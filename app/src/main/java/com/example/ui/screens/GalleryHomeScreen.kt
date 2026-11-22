package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.GalleryPhoto
import com.example.ui.AlbumSummary
import com.example.ui.GalleryFilter
import com.example.ui.GalleryUiState
import com.example.ui.components.LuminaPhotoTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryHomeScreen(
    uiState: GalleryUiState,
    userDisplayName: String?,
    onOpenPhoto: (List<GalleryPhoto>, Int) -> Unit,
    onStartSlideshow: (List<GalleryPhoto>) -> Unit,
    onToggleFavorite: (GalleryPhoto) -> Unit,
    onToggleSelect: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onAddSelectedToAlbum: (String, String) -> Unit,
    onCreateAlbumWithSelected: (String, String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (GalleryFilter) -> Unit,
    onImportPhotos: (List<Uri>) -> Unit,
    onImportFolder: (Uri) -> Unit,
    onOpenThemeStudio: () -> Unit,
    onNavigateToDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchExpanded by remember { mutableStateOf(false) }
    var importMenuExpanded by remember { mutableStateOf(false) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris ->
        if (uris.isNotEmpty()) onImportPhotos(uris)
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onImportFolder(uri)
    }

    val selectionMode = uiState.selectedPhotoIds.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar or Selection Bar
            if (selectionMode) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onClearSelection,
                                modifier = Modifier.testTag("clear_selection_btn")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                            }
                            Text(
                                text = "${uiState.selectedPhotoIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    val selectedList = uiState.filteredPhotos.filter { it.id in uiState.selectedPhotoIds }
                                    if (selectedList.isNotEmpty()) {
                                        onClearSelection()
                                        onStartSlideshow(selectedList)
                                    }
                                },
                                modifier = Modifier.testTag("selected_slideshow_btn")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play slideshow")
                            }
                            IconButton(
                                onClick = { showAddToAlbumDialog = true },
                                modifier = Modifier.testTag("selected_add_album_btn")
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = "Add to album")
                            }
                            IconButton(
                                onClick = onDeleteSelected,
                                modifier = Modifier.testTag("selected_delete_btn")
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete selected")
                            }
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${uiState.photos.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = if (!userDisplayName.isNullOrBlank()) "Synced · $userDisplayName" else "Tap Drive to link Gmail photos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onNavigateToDrive() }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { searchExpanded = !searchExpanded },
                            modifier = Modifier.testTag("toggle_search_btn")
                        ) {
                            Icon(
                                imageVector = if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Search photos"
                            )
                        }
                        IconButton(
                            onClick = {
                                if (uiState.filteredPhotos.isNotEmpty()) {
                                    onStartSlideshow(uiState.filteredPhotos)
                                }
                            },
                            modifier = Modifier.testTag("launch_slideshow_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Slideshow,
                                contentDescription = stringResource(R.string.action_slideshow)
                            )
                        }
                        IconButton(
                            onClick = onOpenThemeStudio,
                            modifier = Modifier.testTag("open_theme_studio_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = stringResource(R.string.cd_theme_studio),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Animated Search Bar
            AnimatedVisibility(
                visible = searchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_photos_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                        .testTag("gallery_search_input")
                )
            }

            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(GalleryFilter.entries) { filter ->
                    val selected = uiState.activeFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(filter.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.filteredPhotos.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No matching photos yet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import photos from your device or link a folder without granting broad storage access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    multiPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("empty_import_photos_btn")
                            ) {
                                Text(stringResource(R.string.action_import_photos))
                            }
                            OutlinedButton(
                                onClick = { folderPicker.launch(null) },
                                modifier = Modifier.testTag("empty_link_folder_btn")
                            ) {
                                Text(stringResource(R.string.action_link_folder))
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(uiState.themePrefs.gridColumns),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = uiState.filteredPhotos,
                        key = { _, item -> item.id }
                    ) { index, photo ->
                        LuminaPhotoTile(
                            photo = photo,
                            isSelected = photo.id in uiState.selectedPhotoIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    onToggleSelect(photo.id)
                                } else {
                                    onOpenPhoto(uiState.filteredPhotos, index)
                                }
                            },
                            onLongClick = { onToggleSelect(photo.id) },
                            onToggleFavorite = { onToggleFavorite(photo) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Import FAB + Dropdown Menu
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { importMenuExpanded = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null) },
                text = { Text("Import") },
                modifier = Modifier.testTag("fab_import")
            )
            DropdownMenu(
                expanded = importMenuExpanded,
                onDismissRequest = { importMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_import_photos)) },
                    leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        importMenuExpanded = false
                        multiPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag("menu_pick_photos")
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_link_folder)) },
                    leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                    onClick = {
                        importMenuExpanded = false
                        folderPicker.launch(null)
                    },
                    modifier = Modifier.testTag("menu_link_folder")
                )
            }
        }
    }

    if (showAddToAlbumDialog) {
        val customAlbums = uiState.albums.filter { !it.isSmart }
        AlertDialog(
            onDismissRequest = { showAddToAlbumDialog = false },
            title = { Text("Add to Album") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customAlbums.forEach { album: AlbumSummary ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAddSelectedToAlbum(album.id, album.name)
                                    showAddToAlbumDialog = false
                                }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(album.name, style = MaterialTheme.typography.titleMedium)
                                Text("${album.photos.size} items", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text("Or create new album") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAlbumName.isNotBlank()) {
                            onCreateAlbumWithSelected(newAlbumName.trim(), "#22D3EE")
                            newAlbumName = ""
                            showAddToAlbumDialog = false
                        }
                    },
                    enabled = newAlbumName.isNotBlank()
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddToAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
