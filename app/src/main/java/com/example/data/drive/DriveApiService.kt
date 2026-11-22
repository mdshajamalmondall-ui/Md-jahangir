package com.example.data.drive

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@JsonClass(generateAdapter = true)
data class DriveFilesResponse(
    val nextPageToken: String? = null,
    val files: List<DriveFileDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DriveFileDto(
    val id: String,
    val name: String,
    val mimeType: String? = null,
    val thumbnailLink: String? = null,
    val webContentLink: String? = null,
    val size: String? = null,
    val modifiedTime: String? = null,
    val imageMediaMetadata: DriveImageMetadataDto? = null
)

@JsonClass(generateAdapter = true)
data class DriveImageMetadataDto(
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class DriveAboutResponse(
    val user: DriveUserDto? = null,
    val storageQuota: DriveStorageQuotaDto? = null
)

@JsonClass(generateAdapter = true)
data class DriveUserDto(
    val displayName: String? = null,
    val emailAddress: String? = null,
    val photoLink: String? = null
)

@JsonClass(generateAdapter = true)
data class DriveStorageQuotaDto(
    val limit: String? = null,
    val usage: String? = null,
    val usageInDrive: String? = null
)

interface DriveApiService {
    @GET("drive/v3/files")
    suspend fun listImageFiles(
        @Header("Authorization") bearerAuth: String,
        @Query("q") query: String = "mimeType contains 'image/' and trashed = false",
        @Query("fields") fields: String = "nextPageToken, files(id, name, mimeType, thumbnailLink, webContentLink, size, modifiedTime, imageMediaMetadata(width, height))",
        @Query("pageSize") pageSize: Int = 60,
        @Query("orderBy") orderBy: String = "modifiedTime desc",
        @Query("pageToken") pageToken: String? = null
    ): DriveFilesResponse

    @GET("drive/v3/about")
    suspend fun getAbout(
        @Header("Authorization") bearerAuth: String,
        @Query("fields") fields: String = "user(displayName, emailAddress, photoLink), storageQuota(limit, usage, usageInDrive)"
    ): DriveAboutResponse

    @Streaming
    @GET("drive/v3/files/{fileId}")
    suspend fun downloadFile(
        @Header("Authorization") bearerAuth: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): ResponseBody

    @Multipart
    @POST("upload/drive/v3/files")
    suspend fun uploadImage(
        @Header("Authorization") bearerAuth: String,
        @Query("uploadType") uploadType: String = "multipart",
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): DriveFileDto
}
