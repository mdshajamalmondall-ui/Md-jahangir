package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PhotoSourceType {
    CURATED,
    DEVICE,
    EDITED,
    DRIVE
}

data class GalleryPhoto(
    val id: String,
    val title: String,
    val uriString: String,
    val drawableResId: Int? = null,
    val mimeType: String = "image/jpeg",
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val dateTakenMillis: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val sourceType: PhotoSourceType = PhotoSourceType.CURATED,
    val driveFileId: String? = null,
    val driveThumbnailUrl: String? = null,
    val albumName: String = "Camera Roll",
    val tags: List<String> = emptyList()
)

@Entity(tableName = "local_photos")
data class LocalPhotoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uriString: String,
    val drawableResName: String? = null,
    val mimeType: String = "image/jpeg",
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val dateTakenMillis: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val sourceType: String = PhotoSourceType.DEVICE.name,
    val albumName: String = "Library",
    val tagsCsv: String = ""
)

@Entity(tableName = "local_albums")
data class LocalAlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val accentHex: String = "#22D3EE",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "album_photo_cross_ref",
    primaryKeys = ["albumId", "photoId"]
)
data class AlbumPhotoCrossRef(
    val albumId: String,
    val photoId: String,
    val addedAtMillis: Long = System.currentTimeMillis()
)
