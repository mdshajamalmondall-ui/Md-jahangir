package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.data.local.GalleryPhoto
import com.example.ui.CloudDriveViewModel
import com.example.ui.CloudUiState
import com.example.ui.DriveLoadState
import com.example.ui.components.GoogleDriveAuthHeroCard
import com.example.ui.components.LuminaPhotoTile
import com.google.firebase.auth.FirebaseUser

@Composable
fun DriveCloudRoute(
    currentUser: FirebaseUser?,
    cloudViewModel: CloudDriveViewModel?,
    gridColumns: Int,
    onAuthSuccess: () -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenDrivePhoto: (List<GalleryPhoto>, Int, ImageLoader?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentUser == null || cloudViewModel == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            GoogleDriveAuthHeroCard(
                onAuthSuccess = onAuthSuccess,
                onAuthError = onShowMessage
            )
        }
        return
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val driveState by cloudViewModel.driveState.collectAsStateWithLifecycle()
    val driveLoader by cloudViewModel.driveImageLoader.collectAsStateWithLifecycle()
    val pendingIntent by cloudViewModel.pendingConsentIntent.collectAsStateWithLifecycle()
    val cloudFavState by cloudViewModel.cloudFavoritesState.collectAsStateWithLifecycle()
    val cloudMsg by cloudViewModel.cloudMessage.collectAsStateWithLifecycle()

    var driveQuery by remember { mutableStateOf("") }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        cloudViewModel.handleDriveConsentResult(res.data)
    }

    LaunchedEffect(pendingIntent) {
        val pi = pendingIntent ?: return@LaunchedEffect
        cloudViewModel.consumePendingConsentIntent()
        consentLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
    }

    LaunchedEffect(cloudMsg) {
        val msg = cloudMsg ?: return@LaunchedEffect
        onShowMessage(msg)
        cloudViewModel.clearCloudMessage()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Account & Drive Status Header
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = (currentUser.displayName ?: currentUser.email ?: "G").first().uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser.displayName ?: "Google Account",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUser.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            GoogleAuthHelper.signOut(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.action_sign_out)
                        )
                    }
                }

                val readyState = driveState as? DriveLoadState.Ready
                readyState?.storage?.let { quota ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val ratio = if (quota.limitBytes > 0L) {
                        (quota.usedBytes.toFloat() / quota.limitBytes.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Google Drive Storage", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${formatGb(quota.usedBytes)} / ${formatGb(quota.limitBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cloud Favorites Quick Strip (Firestore)
        if (cloudFavState is CloudUiState.Success) {
            val favs = (cloudFavState as CloudUiState.Success).data
            if (favs.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFB7185),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cloud Favorites (${favs.size})", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(favs, key = { it.id }) { fav ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = fav.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Drive Photos Content
        when (val state = driveState) {
            DriveLoadState.Disconnected -> {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Load Google Drive Photos", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Grant read & file access to view photos stored in your Gmail's Google Drive and save edited photos back to the cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { if (activity != null) cloudViewModel.connectDrive(activity) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("connect_drive_btn")
                        ) {
                            Icon(Icons.Filled.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_connect_drive), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            DriveLoadState.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Fetching photos from Google Drive…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            is DriveLoadState.Error -> {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Could not load Drive photos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { if (activity != null) cloudViewModel.connectDrive(activity) },
                            modifier = Modifier.testTag("retry_drive_btn")
                        ) {
                            Text("Retry Connection")
                        }
                    }
                }
            }

            is DriveLoadState.Ready -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Drive Photos (${state.photos.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    IconButton(
                        onClick = { cloudViewModel.refreshDrivePhotos() },
                        modifier = Modifier.testTag("refresh_drive_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Drive")
                    }
                }

                OutlinedTextField(
                    value = driveQuery,
                    onValueChange = { driveQuery = it },
                    placeholder = { Text("Search Drive images…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("drive_search_input")
                )

                val filtered = remember(state.photos, driveQuery) {
                    val q = driveQuery.trim().lowercase()
                    if (q.isEmpty()) state.photos else state.photos.filter { it.title.lowercase().contains(q) }
                }

                if (filtered.isEmpty()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            "No image files found in your Google Drive.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filtered, key = { _, item -> item.id }) { idx, photo ->
                            LuminaPhotoTile(
                                photo = photo,
                                isSelected = false,
                                selectionMode = false,
                                imageLoader = driveLoader,
                                onClick = { onOpenDrivePhoto(filtered, idx, driveLoader) },
                                onLongClick = { cloudViewModel.downloadDrivePhotoToLocal(photo) },
                                onToggleFavorite = { cloudViewModel.toggleCloudFavorite(photo, false) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatGb(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return String.format(java.util.Locale.US, "%.2f GB", gb)
}
