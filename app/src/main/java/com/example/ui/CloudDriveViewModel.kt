package com.example.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.example.R
import com.example.data.cloud.CloudAlbum
import com.example.data.cloud.CloudFavorite
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.UserPreference
import com.example.data.drive.DriveAuthOutcome
import com.example.data.drive.DriveRepository
import com.example.data.drive.DriveStorageSummary
import com.example.data.local.GalleryDatabase
import com.example.data.local.GalleryPhoto
import com.example.data.local.LocalGalleryRepository
import com.example.data.local.PhotoSourceType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

sealed interface DriveLoadState {
    data object Disconnected : DriveLoadState
    data object Loading : DriveLoadState
    data class Ready(
        val photos: List<GalleryPhoto>,
        val storage: DriveStorageSummary?
    ) : DriveLoadState
    data class Error(val message: String) : DriveLoadState
}

class CloudDriveViewModel(
    private val cloudSyncRepo: CloudSyncRepository,
    private val driveRepo: DriveRepository,
    private val localRepo: LocalGalleryRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _driveAccessToken = MutableStateFlow<String?>(null)
    val driveAccessToken: StateFlow<String?> = _driveAccessToken.asStateFlow()

    private val _driveImageLoader = MutableStateFlow<ImageLoader?>(null)
    val driveImageLoader: StateFlow<ImageLoader?> = _driveImageLoader.asStateFlow()

    private val _driveState = MutableStateFlow<DriveLoadState>(DriveLoadState.Disconnected)
    val driveState: StateFlow<DriveLoadState> = _driveState.asStateFlow()

    private val _pendingConsentIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingConsentIntent: StateFlow<PendingIntent?> = _pendingConsentIntent.asStateFlow()

    private val _cloudMessage = MutableStateFlow<String?>(null)
    val cloudMessage: StateFlow<String?> = _cloudMessage.asStateFlow()

    val userPreferenceState: StateFlow<CloudUiState<UserPreference?>> =
        cloudSyncRepo.observeUserPreference(currentUserId)
            .map<UserPreference?, CloudUiState<UserPreference?>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user preference", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Failed to load cloud preferences"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val cloudFavoritesState: StateFlow<CloudUiState<List<CloudFavorite>>> =
        cloudSyncRepo.observeCloudFavorites(currentUserId)
            .map<List<CloudFavorite>, CloudUiState<List<CloudFavorite>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing cloud favorites", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Failed to observe cloud favorites"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val cloudAlbumsState: StateFlow<CloudUiState<List<CloudAlbum>>> =
        cloudSyncRepo.observeCloudAlbums(currentUserId)
            .map<List<CloudAlbum>, CloudUiState<List<CloudAlbum>>> { CloudUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing cloud albums", rootError)
                emit(CloudUiState.Error(rootError.message ?: "Failed to observe cloud albums"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    fun connectDrive(activity: Activity) {
        viewModelScope.launch {
            _driveState.value = DriveLoadState.Loading
            when (val outcome = driveRepo.requestDriveAuthorization(activity)) {
                is DriveAuthOutcome.Authorized -> {
                    onDriveTokenAcquired(outcome.accessToken)
                }
                is DriveAuthOutcome.NeedsConsent -> {
                    _pendingConsentIntent.value = outcome.pendingIntent
                    _driveState.value = DriveLoadState.Disconnected
                }
                is DriveAuthOutcome.Failed -> {
                    _driveState.value = DriveLoadState.Error(outcome.message)
                }
            }
        }
    }

    fun consumePendingConsentIntent() {
        _pendingConsentIntent.value = null
    }

    fun handleDriveConsentResult(data: Intent?) {
        val tokenRes = driveRepo.extractTokenFromConsentResult(data)
        tokenRes.onSuccess { token ->
            onDriveTokenAcquired(token)
        }.onFailure { err ->
            _driveState.value = DriveLoadState.Error(err.localizedMessage ?: "Drive permission was not granted")
        }
    }

    fun refreshDrivePhotos() {
        val token = _driveAccessToken.value ?: return
        viewModelScope.launch {
            _driveState.value = DriveLoadState.Loading
            val res = driveRepo.fetchDrivePhotos(token)
            res.onSuccess { (photos, summary) ->
                _driveState.value = DriveLoadState.Ready(photos, summary)
            }.onFailure { err ->
                _driveState.value = DriveLoadState.Error(err.localizedMessage ?: "Failed to fetch Google Drive photos")
            }
        }
    }

    private fun onDriveTokenAcquired(token: String) {
        _driveAccessToken.value = token
        _driveImageLoader.value = driveRepo.buildAuthenticatedImageLoader(token)
        refreshDrivePhotos()
    }

    fun downloadDrivePhotoToLocal(photo: GalleryPhoto, onReady: (GalleryPhoto) -> Unit = {}) {
        val token = _driveAccessToken.value ?: return
        val fileId = photo.driveFileId ?: return
        viewModelScope.launch {
            val dl = driveRepo.downloadDrivePhotoToCache(token, fileId, photo.title)
            dl.onSuccess { file ->
                val saved = localRepo.recordSavedPhoto(
                    title = photo.title,
                    uri = android.net.Uri.fromFile(file),
                    width = photo.width,
                    height = photo.height,
                    sizeBytes = file.length(),
                    sourceType = PhotoSourceType.DEVICE,
                    albumName = "Drive Offline"
                )
                _cloudMessage.value = "Downloaded \"${photo.title}\" to device gallery"
                onReady(saved)
            }.onFailure { err ->
                _cloudMessage.value = err.localizedMessage ?: "Failed to download Drive photo"
            }
        }
    }

    fun uploadEditedBytesToDrive(fileName: String, jpegBytes: ByteArray) {
        val token = _driveAccessToken.value
        if (token.isNullOrBlank()) {
            _cloudMessage.value = "Connect Google Drive first to upload edited photos"
            return
        }
        viewModelScope.launch {
            val res = driveRepo.uploadEditedPhotoToDrive(token, fileName, jpegBytes)
            res.onSuccess {
                _cloudMessage.value = "Uploaded \"$fileName\" to Google Drive"
                refreshDrivePhotos()
            }.onFailure { err ->
                _cloudMessage.value = err.localizedMessage ?: "Drive upload failed"
            }
        }
    }

    fun syncThemePreference(
        displayName: String,
        themePreset: String,
        darkModeEnabled: Boolean,
        gridColumns: Int
    ) {
        viewModelScope.launch {
            cloudSyncRepo.saveUserPreference(
                userId = currentUserId,
                displayName = displayName,
                themePreset = themePreset,
                darkModeEnabled = darkModeEnabled,
                gridColumns = gridColumns
            ).onSuccess {
                _cloudMessage.value = "Theme preferences backed up to your Google account"
            }.onFailure { err ->
                _cloudMessage.value = err.localizedMessage ?: "Failed to sync preferences"
            }
        }
    }

    fun toggleCloudFavorite(photo: GalleryPhoto, currentlyCloudFav: Boolean) {
        val favId = photo.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
        viewModelScope.launch {
            val res = if (currentlyCloudFav) {
                cloudSyncRepo.removeCloudFavorite(currentUserId, favId)
            } else {
                cloudSyncRepo.addCloudFavorite(
                    userId = currentUserId,
                    favoriteId = favId,
                    photoKey = photo.id,
                    title = photo.title,
                    sourceType = photo.sourceType.name,
                    driveFileId = photo.driveFileId
                )
            }
            res.onSuccess {
                _cloudMessage.value = if (currentlyCloudFav) "Removed from Cloud Favorites" else "Saved to Cloud Favorites"
            }.onFailure { err ->
                _cloudMessage.value = err.localizedMessage ?: "Failed to update Cloud Favorites"
            }
        }
    }

    fun createCloudAlbum(name: String, accentHex: String, photoKeys: List<String> = emptyList()) {
        viewModelScope.launch {
            cloudSyncRepo.createCloudAlbum(currentUserId, name, accentHex, photoKeys)
                .onSuccess { _cloudMessage.value = "Cloud album \"$name\" synced" }
                .onFailure { _cloudMessage.value = it.localizedMessage ?: "Failed to create cloud album" }
        }
    }

    fun clearCloudMessage() {
        _cloudMessage.value = null
    }

    companion object {
        private const val TAG = "CloudDriveVM"

        fun factory(context: Context, currentUserId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    val db = FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
                    val cloudRepo = CloudSyncRepository(db)
                    val driveRepo = DriveRepository(appCtx)
                    val localRepo = LocalGalleryRepository(appCtx, GalleryDatabase.getInstance(appCtx).galleryDao())
                    return CloudDriveViewModel(cloudRepo, driveRepo, localRepo, currentUserId) as T
                }
            }
    }
}
