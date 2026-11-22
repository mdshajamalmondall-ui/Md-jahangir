package com.example.data.drive

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import coil.ImageLoader
import com.example.data.local.GalleryPhoto
import com.example.data.local.PhotoSourceType
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsConsent(val pendingIntent: PendingIntent) : DriveAuthOutcome
    data class Failed(val message: String) : DriveAuthOutcome
}

data class DriveStorageSummary(
    val usedBytes: Long,
    val limitBytes: Long,
    val accountEmail: String?
)

class DriveRepository(private val context: Context) {
    private val driveScopes = listOf(
        Scope("https://www.googleapis.com/auth/drive.readonly"),
        Scope("https://www.googleapis.com/auth/drive.file")
    )

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val baseOkHttp = OkHttpClient.Builder().build()

    private val apiService: DriveApiService = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(baseOkHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DriveApiService::class.java)

    suspend fun requestDriveAuthorization(activity: Activity): DriveAuthOutcome {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(driveScopes)
                .build()
            val result = Identity.getAuthorizationClient(activity)
                .authorize(request)
                .await()
            val pending = result.pendingIntent
            val token = result.accessToken
            when {
                result.hasResolution() && pending != null -> DriveAuthOutcome.NeedsConsent(pending)
                !token.isNullOrBlank() -> DriveAuthOutcome.Authorized(token)
                else -> DriveAuthOutcome.Failed("No access token returned by Google Play Services")
            }
        } catch (e: Exception) {
            DriveAuthOutcome.Failed(e.localizedMessage ?: "Failed to authorize Google Drive")
        }
    }

    fun extractTokenFromConsentResult(data: Intent?): Result<String> {
        return try {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = authResult.accessToken
            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(IllegalStateException("Google Drive authorization returned an empty token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildAuthenticatedImageLoader(accessToken: String): ImageLoader {
        val authedClient = baseOkHttp.newBuilder()
            .addInterceptor { chain ->
                val req = chain.request()
                val host = req.url.host
                val newReq = if (host.contains("googleapis.com") || host.contains("googleusercontent.com")) {
                    req.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                } else req
                chain.proceed(newReq)
            }
            .build()
        return ImageLoader.Builder(context)
            .okHttpClient(authedClient)
            .crossfade(true)
            .build()
    }

    suspend fun fetchDrivePhotos(accessToken: String): Result<Pair<List<GalleryPhoto>, DriveStorageSummary?>> =
        withContext(Dispatchers.IO) {
            try {
                val bearer = "Bearer $accessToken"
                val filesResp = apiService.listImageFiles(bearerAuth = bearer)
                val aboutResp = runCatching { apiService.getAbout(bearerAuth = bearer) }.getOrNull()

                val photos = filesResp.files.map { dto ->
                    val mediaUrl = "https://www.googleapis.com/drive/v3/files/${dto.id}?alt=media"
                    GalleryPhoto(
                        id = "drive_${dto.id}",
                        title = dto.name,
                        uriString = mediaUrl,
                        mimeType = dto.mimeType ?: "image/jpeg",
                        width = dto.imageMediaMetadata?.width ?: 0,
                        height = dto.imageMediaMetadata?.height ?: 0,
                        sizeBytes = dto.size?.toLongOrNull() ?: 0L,
                        dateTakenMillis = parseIsoTime(dto.modifiedTime),
                        isFavorite = false,
                        sourceType = PhotoSourceType.DRIVE,
                        driveFileId = dto.id,
                        driveThumbnailUrl = dto.thumbnailLink?.replace("=s220", "=s720") ?: mediaUrl,
                        albumName = "Google Drive",
                        tags = listOf("drive", "cloud")
                    )
                }

                val summary = aboutResp?.storageQuota?.let { quota ->
                    DriveStorageSummary(
                        usedBytes = quota.usage?.toLongOrNull() ?: 0L,
                        limitBytes = quota.limit?.toLongOrNull() ?: 15L * 1024 * 1024 * 1024,
                        accountEmail = aboutResp.user?.emailAddress
                    )
                }
                Result.success(photos to summary)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun downloadDrivePhotoToCache(accessToken: String, fileId: String, fileName: String): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val body = apiService.downloadFile("Bearer $accessToken", fileId)
                val cacheDir = File(context.cacheDir, "drive_downloads").apply { mkdirs() }
                val safeName = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
                val outFile = File(cacheDir, "${fileId}_$safeName")
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(outFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun uploadEditedPhotoToDrive(
        accessToken: String,
        fileName: String,
        jpegBytes: ByteArray
    ): Result<DriveFileDto> = withContext(Dispatchers.IO) {
        try {
            val safeName = if (fileName.endsWith(".jpg", ignoreCase = true)) fileName else "$fileName.jpg"
            val metadataJson = """{"name":"$safeName","mimeType":"image/jpeg"}"""
            val metadataBody = metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            val fileBody = jpegBytes.toRequestBody("image/jpeg".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", safeName, fileBody)
            val created = apiService.uploadImage(
                bearerAuth = "Bearer $accessToken",
                metadata = metadataBody,
                file = filePart
            )
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIsoTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(raw)?.time ?: System.currentTimeMillis()
        }.getOrElse { System.currentTimeMillis() }
    }
}
