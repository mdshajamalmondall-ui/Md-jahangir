package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing cloud sync.")
        if (expectedUserId != null && uid != expectedUserId) {
            throw IllegalStateException("Signed-in user changed; refusing cross-user operation.")
        }
        return uid
    }

    fun observeUserPreference(userId: String): Flow<UserPreference?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserPreference::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserPreference(
        userId: String,
        displayName: String,
        themePreset: String,
        darkModeEnabled: Boolean,
        gridColumns: Int
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val safeName = displayName.trim().ifBlank { "Lumina User" }.take(80)
            val safeCols = gridColumns.coerceIn(2, 4).toLong()
            val docRef = db.collection("users").document(uid)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "themePreset" to themePreset,
                        "darkModeEnabled" to darkModeEnabled,
                        "gridColumns" to safeCols,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "themePreset" to themePreset,
                        "darkModeEnabled" to darkModeEnabled,
                        "gridColumns" to safeCols,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeCloudFavorites(userId: String): Flow<List<CloudFavorite>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/cloudFavorites"
        emitAll(
            db.collection("users").document(uid).collection("cloudFavorites")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudFavorite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addCloudFavorite(
        userId: String,
        favoriteId: String,
        photoKey: String,
        title: String,
        sourceType: String,
        driveFileId: String?
    ): Result<Unit> {
        val safeId = favoriteId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "fav_item" }
        val path = "users/$userId/cloudFavorites/$safeId"
        return try {
            val uid = requireUserId(userId)
            val payload = mapOf(
                "userId" to uid,
                "photoKey" to photoKey.take(500),
                "title" to title.trim().ifBlank { "Photo" }.take(120),
                "sourceType" to sourceType,
                "driveFileId" to driveFileId?.take(128),
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.collection("users").document(uid)
                .collection("cloudFavorites").document(safeId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeCloudFavorite(userId: String, favoriteId: String): Result<Unit> {
        val safeId = favoriteId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val path = "users/$userId/cloudFavorites/$safeId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid)
                .collection("cloudFavorites").document(safeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeCloudAlbums(userId: String): Flow<List<CloudAlbum>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/cloudAlbums"
        emitAll(
            db.collection("users").document(uid).collection("cloudAlbums")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CloudAlbum::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createCloudAlbum(userId: String, name: String, accentHex: String, photoKeys: List<String> = emptyList()): Result<Unit> {
        val albumId = "album_${System.currentTimeMillis()}"
        val path = "users/$userId/cloudAlbums/$albumId"
        return try {
            val uid = requireUserId(userId)
            val safeName = name.trim().take(60)
            require(safeName.isNotEmpty()) { "Album name cannot be empty" }
            val payload = mapOf(
                "userId" to uid,
                "name" to safeName,
                "accentHex" to accentHex.take(16),
                "photoKeys" to photoKeys.take(100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("cloudAlbums").document(albumId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
