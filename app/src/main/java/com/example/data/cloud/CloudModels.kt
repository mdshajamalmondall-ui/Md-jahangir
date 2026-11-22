package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserPreference(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val themePreset: String = "OBSIDIAN_NEON",
    val darkModeEnabled: Boolean = true,
    val gridColumns: Long = 3L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudFavorite(
    @DocumentId val id: String = "",
    val userId: String = "",
    val photoKey: String = "",
    val title: String = "",
    val sourceType: String = "CURATED",
    val driveFileId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAlbum(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val accentHex: String = "#22D3EE",
    val photoKeys: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
