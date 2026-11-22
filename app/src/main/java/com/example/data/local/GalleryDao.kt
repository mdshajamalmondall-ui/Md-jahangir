package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Query("SELECT * FROM local_photos ORDER BY dateTakenMillis DESC")
    fun observeAllPhotos(): Flow<List<LocalPhotoEntity>>

    @Query("SELECT * FROM local_photos WHERE isFavorite = 1 ORDER BY dateTakenMillis DESC")
    fun observeFavoritePhotos(): Flow<List<LocalPhotoEntity>>

    @Query("SELECT COUNT(*) FROM local_photos")
    suspend fun countPhotos(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: LocalPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhotosIgnoreExisting(photos: List<LocalPhotoEntity>)

    @Query("UPDATE local_photos SET isFavorite = :favorite WHERE id = :photoId")
    suspend fun setFavorite(photoId: String, favorite: Boolean)

    @Query("DELETE FROM local_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: String)

    @Query("DELETE FROM local_photos WHERE id IN (:photoIds)")
    suspend fun deletePhotosByIds(photoIds: List<String>)

    @Query("SELECT * FROM local_albums ORDER BY createdAtMillis DESC")
    fun observeAlbums(): Flow<List<LocalAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: LocalAlbumEntity)

    @Query("DELETE FROM local_albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: String)

    @Query("SELECT * FROM album_photo_cross_ref")
    fun observeAlbumLinks(): Flow<List<AlbumPhotoCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPhotoToAlbum(ref: AlbumPhotoCrossRef)

    @Query("DELETE FROM album_photo_cross_ref WHERE albumId = :albumId AND photoId = :photoId")
    suspend fun removePhotoFromAlbum(albumId: String, photoId: String)

    @Query("DELETE FROM album_photo_cross_ref WHERE albumId = :albumId")
    suspend fun clearAlbumLinks(albumId: String)
}
