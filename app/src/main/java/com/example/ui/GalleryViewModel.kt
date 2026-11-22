package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlbumPhotoCrossRef
import com.example.data.local.GalleryDatabase
import com.example.data.local.GalleryPhoto
import com.example.data.local.LocalAlbumEntity
import com.example.data.local.LocalGalleryRepository
import com.example.data.local.PhotoSourceType
import com.example.data.local.ThemePreferencesStore
import com.example.data.local.ThemeUiPreferences
import com.example.editor.BitmapProcessor
import com.example.ui.theme.LuminaThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GalleryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    DEVICE("Device"),
    EDITED("Edited"),
    CURATED("Showcase")
}

data class AlbumSummary(
    val id: String,
    val name: String,
    val accentHex: String,
    val photos: List<GalleryPhoto>,
    val isSmart: Boolean = false
)

data class GalleryUiState(
    val photos: List<GalleryPhoto> = emptyList(),
    val filteredPhotos: List<GalleryPhoto> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: GalleryFilter = GalleryFilter.ALL,
    val selectedPhotoIds: Set<String> = emptySet(),
    val themePrefs: ThemeUiPreferences = ThemeUiPreferences(),
    val statusMessage: String? = null
)

class GalleryViewModel(
    private val repository: LocalGalleryRepository,
    private val themeStore: ThemePreferencesStore
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(GalleryFilter.ALL)
    private val selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    private val statusMessage = MutableStateFlow<String?>(null)

    private data class FilterTuple(
        val query: String,
        val filter: GalleryFilter,
        val selected: Set<String>,
        val message: String?
    )

    private val filterTupleFlow = combine(
        searchQuery,
        activeFilter,
        selectedPhotoIds,
        statusMessage
    ) { q, f, s, msg -> FilterTuple(q, f, s, msg) }

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.photosFlow,
        repository.albumsFlow,
        repository.albumLinksFlow,
        themeStore.preferencesFlow,
        filterTupleFlow
    ) { photos, albums, links, prefs, tuple ->
        val q = tuple.query.trim().lowercase()
        val filtered = photos.filter { photo ->
            val matchesFilter = when (tuple.filter) {
                GalleryFilter.ALL -> true
                GalleryFilter.FAVORITES -> photo.isFavorite
                GalleryFilter.DEVICE -> photo.sourceType == PhotoSourceType.DEVICE
                GalleryFilter.EDITED -> photo.sourceType == PhotoSourceType.EDITED
                GalleryFilter.CURATED -> photo.sourceType == PhotoSourceType.CURATED
            }
            val matchesSearch = q.isEmpty() ||
                photo.title.lowercase().contains(q) ||
                photo.albumName.lowercase().contains(q) ||
                photo.tags.any { it.lowercase().contains(q) }
            matchesFilter && matchesSearch
        }

        val photoMap = photos.associateBy { it.id }
        val linksByAlbum = links.groupBy { it.albumId }
        val customAlbums = albums.map { album ->
            val albumPhotos = linksByAlbum[album.id].orEmpty().mapNotNull { photoMap[it.photoId] }
            AlbumSummary(
                id = album.id,
                name = album.name,
                accentHex = album.accentHex,
                photos = albumPhotos,
                isSmart = false
            )
        }

        val smartAlbums = listOf(
            AlbumSummary(
                id = "smart_favorites",
                name = "Favorites",
                accentHex = "#FB7185",
                photos = photos.filter { it.isFavorite },
                isSmart = true
            ),
            AlbumSummary(
                id = "smart_edited",
                name = "Lumina Edits",
                accentHex = "#A78BFA",
                photos = photos.filter { it.sourceType == PhotoSourceType.EDITED },
                isSmart = true
            ),
            AlbumSummary(
                id = "smart_device",
                name = "Imported Media",
                accentHex = "#34D399",
                photos = photos.filter { it.sourceType == PhotoSourceType.DEVICE },
                isSmart = true
            )
        )

        GalleryUiState(
            photos = photos,
            filteredPhotos = filtered,
            albums = smartAlbums + customAlbums,
            searchQuery = tuple.query,
            activeFilter = tuple.filter,
            selectedPhotoIds = tuple.selected,
            themePrefs = prefs,
            statusMessage = tuple.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = GalleryUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedCuratedShowcaseIfEmpty()
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setActiveFilter(filter: GalleryFilter) {
        activeFilter.value = filter
    }

    fun togglePhotoSelection(photoId: String) {
        selectedPhotoIds.update { current ->
            if (photoId in current) current - photoId else current + photoId
        }
    }

    fun clearSelection() {
        selectedPhotoIds.value = emptySet()
    }

    fun toggleFavorite(photo: GalleryPhoto) {
        viewModelScope.launch {
            repository.toggleFavorite(photo.id, photo.isFavorite)
        }
    }

    fun importPickedPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val count = repository.importPickedUris(uris)
            statusMessage.value = "Imported $count photo${if (count == 1) "" else "s"} to gallery"
        }
    }

    fun importSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            val count = repository.importSafFolder(treeUri)
            statusMessage.value = if (count > 0) {
                "Linked folder with $count photo${if (count == 1) "" else "s"}"
            } else {
                "No image files found in selected folder"
            }
        }
    }

    fun deleteSelectedPhotos() {
        val ids = selectedPhotoIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deletePhotos(ids)
            selectedPhotoIds.value = emptySet()
            statusMessage.value = "Removed ${ids.size} item${if (ids.size == 1) "" else "s"}"
        }
    }

    fun deleteSinglePhoto(photoId: String) {
        viewModelScope.launch {
            repository.deletePhotos(listOf(photoId))
            statusMessage.value = "Photo removed from gallery"
        }
    }

    fun createAlbum(name: String, accentHex: String, photoIds: List<String> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createAlbum(name, accentHex, photoIds)
            selectedPhotoIds.value = emptySet()
            statusMessage.value = "Created album \"$name\""
        }
    }

    fun addSelectedToAlbum(albumId: String, albumName: String) {
        val ids = selectedPhotoIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.addPhotosToAlbum(albumId, ids)
            selectedPhotoIds.value = emptySet()
            statusMessage.value = "Added ${ids.size} photo${if (ids.size == 1) "" else "s"} to $albumName"
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            repository.deleteAlbum(albumId)
            statusMessage.value = "Album deleted"
        }
    }

    fun setThemePreset(preset: LuminaThemePreset) {
        viewModelScope.launch { themeStore.setThemePreset(preset) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { themeStore.setDarkMode(enabled) }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch { themeStore.setGridColumns(columns) }
    }

    fun setAmbientMotion(enabled: Boolean) {
        viewModelScope.launch { themeStore.setAmbientMotion(enabled) }
    }

    fun setSlideshowSeconds(seconds: Int) {
        viewModelScope.launch { themeStore.setSlideshowSeconds(seconds) }
    }

    fun saveEditedPhoto(
        context: Context,
        bitmap: Bitmap,
        baseTitle: String,
        onSaved: (GalleryPhoto, ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val saveRes = BitmapProcessor.saveEditedBitmap(context, bitmap, baseTitle)
            saveRes.onSuccess { saved ->
                val recorded = repository.recordSavedPhoto(
                    title = "${baseTitle.substringBeforeLast('.')} (Edited)",
                    uri = saved.uri,
                    width = saved.width,
                    height = saved.height,
                    sizeBytes = saved.sizeBytes,
                    sourceType = PhotoSourceType.EDITED,
                    albumName = "Lumina Edits"
                )
                statusMessage.value = "Saved edited photo to Pictures/Lumina"
                onSaved(recorded, saved.jpegBytes)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Failed to save edited photo")
            }
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appCtx = context.applicationContext
                val db = GalleryDatabase.getInstance(appCtx)
                val repo = LocalGalleryRepository(appCtx, db.galleryDao())
                val prefs = ThemePreferencesStore(appCtx)
                return GalleryViewModel(repo, prefs) as T
            }
        }
    }
}
