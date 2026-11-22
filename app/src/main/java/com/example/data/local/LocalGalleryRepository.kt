package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class LocalGalleryRepository(
    private val context: Context,
    private val dao: GalleryDao
) {
    private val curatedDrawables: Map<String, Int> = mapOf(
        "img_sample_aurora_1795353848171" to R.drawable.img_sample_aurora_1795353848171,
        "img_sample_architecture_1795353847506" to R.drawable.img_sample_architecture_1795353847506,
        "img_sample_botanical_1795353848143" to R.drawable.img_sample_botanical_1795353848143,
        "img_sample_coastline_1795353848446" to R.drawable.img_sample_coastline_1795353848446
    )

    val photosFlow: Flow<List<GalleryPhoto>> = dao.observeAllPhotos().map { list ->
        list.map { it.toDomain() }
    }

    val albumsFlow: Flow<List<LocalAlbumEntity>> = dao.observeAlbums()
    val albumLinksFlow: Flow<List<AlbumPhotoCrossRef>> = dao.observeAlbumLinks()

    suspend fun seedCuratedShowcaseIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countPhotos() > 0) return@withContext
        val now = System.currentTimeMillis()
        val curated = listOf(
            LocalPhotoEntity(
                id = "curated_aurora",
                title = "Nordic Aurora Fjord",
                uriString = "android.resource://${context.packageName}/${R.drawable.img_sample_aurora_1795353848171}",
                drawableResName = "img_sample_aurora_1795353848171",
                mimeType = "image/jpeg",
                width = 1200,
                height = 1600,
                sizeBytes = 640_000L,
                dateTakenMillis = now - 3_600_000L,
                isFavorite = true,
                sourceType = PhotoSourceType.CURATED.name,
                albumName = "Curated Showcase",
                tagsCsv = "aurora,night,nordic,landscape"
            ),
            LocalPhotoEntity(
                id = "curated_architecture",
                title = "Sunlit Terracotta Arch",
                uriString = "android.resource://${context.packageName}/${R.drawable.img_sample_architecture_1795353847506}",
                drawableResName = "img_sample_architecture_1795353847506",
                mimeType = "image/jpeg",
                width = 1200,
                height = 1600,
                sizeBytes = 590_000L,
                dateTakenMillis = now - 7_200_000L,
                isFavorite = false,
                sourceType = PhotoSourceType.CURATED.name,
                albumName = "Curated Showcase",
                tagsCsv = "architecture,warm,terracotta,courtyard"
            ),
            LocalPhotoEntity(
                id = "curated_botanical",
                title = "Prism Dew Monstera",
                uriString = "android.resource://${context.packageName}/${R.drawable.img_sample_botanical_1795353848143}",
                drawableResName = "img_sample_botanical_1795353848143",
                mimeType = "image/jpeg",
                width = 1600,
                height = 1200,
                sizeBytes = 520_000L,
                dateTakenMillis = now - 10_800_000L,
                isFavorite = true,
                sourceType = PhotoSourceType.CURATED.name,
                albumName = "Curated Showcase",
                tagsCsv = "botanical,macro,green,dew"
            ),
            LocalPhotoEntity(
                id = "curated_coastline",
                title = "Volcanic Basalt Cove",
                uriString = "android.resource://${context.packageName}/${R.drawable.img_sample_coastline_1795353848446}",
                drawableResName = "img_sample_coastline_1795353848446",
                mimeType = "image/jpeg",
                width = 1600,
                height = 1200,
                sizeBytes = 610_000L,
                dateTakenMillis = now - 14_400_000L,
                isFavorite = false,
                sourceType = PhotoSourceType.CURATED.name,
                albumName = "Curated Showcase",
                tagsCsv = "coast,ocean,sunset,aerial"
            )
        )
        dao.insertPhotosIgnoreExisting(curated)
        dao.insertAlbum(
            LocalAlbumEntity(
                id = "album_showcase",
                name = "Curated Showcase",
                accentHex = "#22D3EE",
                createdAtMillis = now
            )
        )
        curated.forEach { photo ->
            dao.addPhotoToAlbum(AlbumPhotoCrossRef(albumId = "album_showcase", photoId = photo.id))
        }
    }

    suspend fun importPickedUris(uris: List<Uri>, albumName: String = "Imported Photos"): Int =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) return@withContext 0
            val entities = uris.mapIndexed { idx, uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val meta = queryUriMetadata(uri)
                LocalPhotoEntity(
                    id = "device_${uri.toString().hashCode().toUInt()}_${meta.first.hashCode().toUInt()}",
                    title = meta.first,
                    uriString = uri.toString(),
                    mimeType = meta.third,
                    sizeBytes = meta.second,
                    dateTakenMillis = System.currentTimeMillis() - idx * 10L,
                    isFavorite = false,
                    sourceType = PhotoSourceType.DEVICE.name,
                    albumName = albumName,
                    tagsCsv = "device,imported"
                )
            }
            dao.insertPhotosIgnoreExisting(entities)
            entities.size
        }

    suspend fun importSafFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val folderName = docId.substringAfterLast('/').substringAfterLast(':').ifBlank { "Device Folder" }
        val imported = mutableListOf<LocalPhotoEntity>()

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeIdx) ?: ""
                if (!mime.startsWith("image/")) continue
                val childDocId = cursor.getString(idIdx)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                val name = cursor.getString(nameIdx) ?: "Photo"
                val size = cursor.getLong(sizeIdx)
                val mod = cursor.getLong(modIdx).takeIf { it > 0L } ?: System.currentTimeMillis()
                imported += LocalPhotoEntity(
                    id = "folder_${childUri.toString().hashCode().toUInt()}",
                    title = name,
                    uriString = childUri.toString(),
                    mimeType = mime,
                    sizeBytes = size,
                    dateTakenMillis = mod,
                    isFavorite = false,
                    sourceType = PhotoSourceType.DEVICE.name,
                    albumName = folderName,
                    tagsCsv = "folder,$folderName"
                )
            }
        }

        if (imported.isNotEmpty()) {
            dao.insertPhotosIgnoreExisting(imported)
            val albumId = "album_folder_${folderName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"
            dao.insertAlbum(LocalAlbumEntity(id = albumId, name = folderName, accentHex = "#34D399"))
            imported.forEach { dao.addPhotoToAlbum(AlbumPhotoCrossRef(albumId = albumId, photoId = it.id)) }
        }
        imported.size
    }

    suspend fun recordSavedPhoto(
        title: String,
        uri: Uri,
        width: Int,
        height: Int,
        sizeBytes: Long,
        sourceType: PhotoSourceType = PhotoSourceType.EDITED,
        albumName: String = "Lumina Edits"
    ): GalleryPhoto = withContext(Dispatchers.IO) {
        val entity = LocalPhotoEntity(
            id = "saved_${UUID.randomUUID()}",
            title = title,
            uriString = uri.toString(),
            mimeType = "image/jpeg",
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            dateTakenMillis = System.currentTimeMillis(),
            isFavorite = false,
            sourceType = sourceType.name,
            albumName = albumName,
            tagsCsv = if (sourceType == PhotoSourceType.EDITED) "edited,lumina" else "drive,downloaded"
        )
        dao.insertPhoto(entity)
        entity.toDomain()
    }

    suspend fun toggleFavorite(photoId: String, current: Boolean) {
        dao.setFavorite(photoId, !current)
    }

    suspend fun deletePhotos(photoIds: List<String>) {
        dao.deletePhotosByIds(photoIds)
    }

    suspend fun createAlbum(name: String, accentHex: String, initialPhotoIds: List<String> = emptyList()) {
        val albumId = "album_${System.currentTimeMillis()}"
        dao.insertAlbum(LocalAlbumEntity(id = albumId, name = name.trim(), accentHex = accentHex))
        initialPhotoIds.forEach { photoId ->
            dao.addPhotoToAlbum(AlbumPhotoCrossRef(albumId = albumId, photoId = photoId))
        }
    }

    suspend fun addPhotosToAlbum(albumId: String, photoIds: List<String>) {
        photoIds.forEach { photoId ->
            dao.addPhotoToAlbum(AlbumPhotoCrossRef(albumId = albumId, photoId = photoId))
        }
    }

    suspend fun deleteAlbum(albumId: String) {
        dao.clearAlbumLinks(albumId)
        dao.deleteAlbum(albumId)
    }

    private fun queryUriMetadata(uri: Uri): Triple<String, Long, String> {
        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Photo"
        var size = 0L
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameCol >= 0) displayName = cursor.getString(nameCol) ?: displayName
                if (sizeCol >= 0) size = cursor.getLong(sizeCol)
            }
        }
        return Triple(displayName, size, mime)
    }

    private fun LocalPhotoEntity.toDomain(): GalleryPhoto {
        val parsedSource = PhotoSourceType.entries.firstOrNull { it.name == sourceType } ?: PhotoSourceType.DEVICE
        return GalleryPhoto(
            id = id,
            title = title,
            uriString = uriString,
            drawableResId = drawableResName?.let { curatedDrawables[it] },
            mimeType = mimeType,
            width = width,
            height = height,
            sizeBytes = sizeBytes,
            dateTakenMillis = dateTakenMillis,
            isFavorite = isFavorite,
            sourceType = parsedSource,
            albumName = albumName,
            tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
    }
}
