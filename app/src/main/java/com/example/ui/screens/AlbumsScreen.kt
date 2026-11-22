package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.GalleryPhoto
import com.example.ui.AlbumSummary
import com.example.ui.components.LuminaPhotoTile

private val AlbumAccentOptions = listOf("#22D3EE", "#FB7185", "#34D399", "#A78BFA", "#FBBF24")

@Composable
fun AlbumsScreen(
    albums: List<AlbumSummary>,
    gridColumns: Int,
    onOpenPhoto: (List<GalleryPhoto>, Int) -> Unit,
    onStartSlideshow: (List<GalleryPhoto>) -> Unit,
    onToggleFavorite: (GalleryPhoto) -> Unit,
    onCreateAlbum: (String, String) -> Unit,
    onDeleteAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeAlbumId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumName by remember { mutableStateOf("") }
    var selectedAccent by remember { mutableStateOf(AlbumAccentOptions.first()) }

    val activeAlbum = albums.firstOrNull { it.id == activeAlbumId }

    if (activeAlbum != null) {
        BackHandler { activeAlbumId = null }
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { activeAlbumId = null },
                        modifier = Modifier.testTag("album_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                    Column {
                        Text(activeAlbum.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${activeAlbum.photos.size} photos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    if (activeAlbum.photos.isNotEmpty()) {
                        IconButton(onClick = { onStartSlideshow(activeAlbum.photos) }) {
                            Icon(Icons.Filled.Slideshow, contentDescription = stringResource(R.string.action_slideshow))
                        }
                    }
                    if (!activeAlbum.isSmart) {
                        IconButton(
                            onClick = {
                                onDeleteAlbum(activeAlbum.id)
                                activeAlbumId = null
                            },
                            modifier = Modifier.testTag("delete_album_btn")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete album")
                        }
                    }
                }
            }

            if (activeAlbum.photos.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Long-press photos in the gallery to add them here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(activeAlbum.photos, key = { _, p -> p.id }) { idx, photo ->
                        LuminaPhotoTile(
                            photo = photo,
                            isSelected = false,
                            selectionMode = false,
                            onClick = { onOpenPhoto(activeAlbum.photos, idx) },
                            onLongClick = {},
                            onToggleFavorite = { onToggleFavorite(photo) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text("Albums", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Smart collections & custom folders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 165.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(albums, key = { it.id }) { album ->
                    val accent = runCatching { Color(android.graphics.Color.parseColor(album.accentHex)) }
                        .getOrElse { MaterialTheme.colorScheme.primary }
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { activeAlbumId = album.id }
                            .animateItem()
                            .testTag("album_card_${album.id}")
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.15f)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(accent.copy(alpha = 0.28f), MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                    )
                            ) {
                                val cover = album.photos.firstOrNull()
                                if (cover?.drawableResId != null) {
                                    Image(
                                        painter = painterResource(id = cover.drawableResId),
                                        contentDescription = album.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (cover != null) {
                                    AsyncImage(
                                        model = cover.uriString,
                                        contentDescription = album.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Collections,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .align(Alignment.Center)
                                    )
                                }

                                Surface(
                                    color = Color.Black.copy(alpha = 0.56f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "${album.photos.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("New Album") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("fab_new_album")
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Album") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = albumName,
                        onValueChange = { albumName = it },
                        label = { Text("Album Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_album_name_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AlbumAccentOptions.forEach { hex ->
                            val c = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedAccent = hex }
                            ) {
                                if (selectedAccent == hex) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateAlbum(albumName.trim(), selectedAccent)
                        albumName = ""
                        showCreateDialog = false
                    },
                    enabled = albumName.isNotBlank(),
                    modifier = Modifier.testTag("confirm_create_album_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
