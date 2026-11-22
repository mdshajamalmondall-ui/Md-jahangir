package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import com.example.auth.GoogleAuthHelper
import com.example.auth.authStateFlow
import com.example.data.local.GalleryPhoto
import com.example.ui.CloudDriveViewModel
import com.example.ui.GalleryViewModel
import com.example.ui.components.ThemeStudioSheet
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.DriveCloudRoute
import com.example.ui.screens.GalleryHomeScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.theme.LuminaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private enum class LuminaTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PHOTOS("photos", R.string.nav_photos, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    ALBUMS("albums", R.string.nav_albums, Icons.Filled.Collections, Icons.Outlined.Collections),
    DRIVE("drive", R.string.nav_drive, Icons.Filled.Cloud, Icons.Outlined.CloudQueue)
}

private data class ViewerSession(
    val photos: List<GalleryPhoto>,
    val initialIndex: Int,
    val slideshow: Boolean,
    val imageLoader: ImageLoader? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuminaAppRoot()
        }
    }
}

@Composable
fun LuminaAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val galleryViewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.factory(context))
    val uiState by galleryViewModel.uiState.collectAsStateWithLifecycle()

    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    LuminaTheme(
        preset = uiState.themePrefs.preset,
        darkTheme = uiState.themePrefs.darkModeEnabled,
        ambientMotionEnabled = uiState.themePrefs.ambientMotionEnabled
    ) {
        val user = currentUser
        if (user != null) {
            key(user.uid) {
                val cloudViewModel: CloudDriveViewModel = viewModel(
                    key = user.uid,
                    factory = CloudDriveViewModel.factory(context, user.uid)
                )
                LuminaScaffoldContent(
                    galleryViewModel = galleryViewModel,
                    cloudViewModel = cloudViewModel,
                    currentUser = user
                )
            }
        } else {
            LuminaScaffoldContent(
                galleryViewModel = galleryViewModel,
                cloudViewModel = null,
                currentUser = null
            )
        }
    }
}

@Composable
private fun LuminaScaffoldContent(
    galleryViewModel: GalleryViewModel,
    cloudViewModel: CloudDriveViewModel?,
    currentUser: FirebaseUser?
) {
    val context = LocalContext.current
    val uiState by galleryViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showThemeStudio by remember { mutableStateOf(false) }
    var activeViewer by remember { mutableStateOf<ViewerSession?>(null) }
    var editingPhoto by remember { mutableStateOf<GalleryPhoto?>(null) }

    val driveToken = cloudViewModel?.driveAccessToken?.collectAsStateWithLifecycle()?.value

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        galleryViewModel.clearStatusMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.navigationBars,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            LuminaTab.entries.forEach { tab ->
                                val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                        ) {
                            LuminaTab.entries.forEach { tab ->
                                val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    modifier = Modifier.testTag("rail_tab_${tab.route}")
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = LuminaTab.PHOTOS.route,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        composable(LuminaTab.PHOTOS.route) {
                            GalleryHomeScreen(
                                uiState = uiState,
                                userDisplayName = currentUser?.displayName ?: currentUser?.email,
                                onOpenPhoto = { list, index ->
                                    activeViewer = ViewerSession(list, index, slideshow = false)
                                },
                                onStartSlideshow = { list ->
                                    activeViewer = ViewerSession(list, 0, slideshow = true)
                                },
                                onToggleFavorite = { photo ->
                                    galleryViewModel.toggleFavorite(photo)
                                    cloudViewModel?.toggleCloudFavorite(photo, photo.isFavorite)
                                },
                                onToggleSelect = galleryViewModel::togglePhotoSelection,
                                onClearSelection = galleryViewModel::clearSelection,
                                onDeleteSelected = galleryViewModel::deleteSelectedPhotos,
                                onAddSelectedToAlbum = galleryViewModel::addSelectedToAlbum,
                                onCreateAlbumWithSelected = { name, hex ->
                                    galleryViewModel.createAlbum(name, hex, uiState.selectedPhotoIds.toList())
                                    cloudViewModel?.createCloudAlbum(name, hex, uiState.selectedPhotoIds.toList())
                                },
                                onSearchQueryChange = galleryViewModel::setSearchQuery,
                                onSelectFilter = galleryViewModel::setActiveFilter,
                                onImportPhotos = galleryViewModel::importPickedPhotos,
                                onImportFolder = galleryViewModel::importSafFolder,
                                onOpenThemeStudio = { showThemeStudio = true },
                                onNavigateToDrive = {
                                    navController.navigate(LuminaTab.DRIVE.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        composable(LuminaTab.ALBUMS.route) {
                            AlbumsScreen(
                                albums = uiState.albums,
                                gridColumns = uiState.themePrefs.gridColumns,
                                onOpenPhoto = { list, index ->
                                    activeViewer = ViewerSession(list, index, slideshow = false)
                                },
                                onStartSlideshow = { list ->
                                    activeViewer = ViewerSession(list, 0, slideshow = true)
                                },
                                onToggleFavorite = { photo ->
                                    galleryViewModel.toggleFavorite(photo)
                                    cloudViewModel?.toggleCloudFavorite(photo, photo.isFavorite)
                                },
                                onCreateAlbum = { name, hex ->
                                    galleryViewModel.createAlbum(name, hex)
                                    cloudViewModel?.createCloudAlbum(name, hex)
                                },
                                onDeleteAlbum = galleryViewModel::deleteAlbum
                            )
                        }

                        composable(LuminaTab.DRIVE.route) {
                            DriveCloudRoute(
                                currentUser = currentUser,
                                cloudViewModel = cloudViewModel,
                                gridColumns = uiState.themePrefs.gridColumns,
                                onAuthSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Signed in with Google")
                                    }
                                },
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                onOpenDrivePhoto = { list, index, loader ->
                                    activeViewer = ViewerSession(list, index, slideshow = false, imageLoader = loader)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen Photo Viewer Overlay
        AnimatedContent(
            targetState = activeViewer,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "viewer_overlay"
        ) { session ->
            if (session != null && editingPhoto == null) {
                PhotoViewerScreen(
                    photos = session.photos,
                    initialIndex = session.initialIndex,
                    initialSlideshow = session.slideshow,
                    slideshowSeconds = uiState.themePrefs.slideshowSeconds,
                    imageLoader = session.imageLoader,
                    onBack = { activeViewer = null },
                    onEditPhoto = { photo -> editingPhoto = photo },
                    onToggleFavorite = { photo ->
                        galleryViewModel.toggleFavorite(photo)
                        cloudViewModel?.toggleCloudFavorite(photo, photo.isFavorite)
                        val updated = session.photos.map {
                            if (it.id == photo.id) it.copy(isFavorite = !it.isFavorite) else it
                        }
                        activeViewer = session.copy(photos = updated)
                    },
                    onDeletePhoto = { photo ->
                        galleryViewModel.deleteSinglePhoto(photo.id)
                        val remaining = session.photos.filterNot { it.id == photo.id }
                        activeViewer = if (remaining.isEmpty()) null else session.copy(photos = remaining)
                    },
                    onDownloadDrivePhoto = cloudViewModel?.let { vm ->
                        { photo -> vm.downloadDrivePhotoToLocal(photo) }
                    }
                )
            }
        }

        // Full-Screen Studio Photo Editor Overlay
        AnimatedContent(
            targetState = editingPhoto,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "editor_overlay"
        ) { photoToEdit ->
            if (photoToEdit != null) {
                PhotoEditorScreen(
                    photo = photoToEdit,
                    driveAccessToken = driveToken,
                    onBack = { editingPhoto = null },
                    onSaveLocal = { bitmap, title, alsoUploadToDrive ->
                        galleryViewModel.saveEditedPhoto(
                            context = context,
                            bitmap = bitmap,
                            baseTitle = title,
                            onSaved = { _, jpegBytes ->
                                if (alsoUploadToDrive && cloudViewModel != null) {
                                    cloudViewModel.uploadEditedBytesToDrive(
                                        fileName = "${title.substringBeforeLast('.')}_lumina_edit.jpg",
                                        jpegBytes = jpegBytes
                                    )
                                }
                                editingPhoto = null
                                activeViewer = null
                            },
                            onError = { errMsg ->
                                scope.launch { snackbarHostState.showSnackbar(errMsg) }
                            }
                        )
                    }
                )
            }
        }

        if (showThemeStudio) {
            ThemeStudioSheet(
                prefs = uiState.themePrefs,
                isSignedIn = currentUser != null,
                onSelectPreset = galleryViewModel::setThemePreset,
                onToggleDarkMode = galleryViewModel::setDarkMode,
                onToggleAmbientMotion = galleryViewModel::setAmbientMotion,
                onSelectGridColumns = galleryViewModel::setGridColumns,
                onSetSlideshowSeconds = galleryViewModel::setSlideshowSeconds,
                onSyncToCloud = cloudViewModel?.let { vm ->
                    {
                        vm.syncThemePreference(
                            displayName = currentUser?.displayName ?: currentUser?.email ?: "Lumina User",
                            themePreset = uiState.themePrefs.preset.id,
                            darkModeEnabled = uiState.themePrefs.darkModeEnabled,
                            gridColumns = uiState.themePrefs.gridColumns
                        )
                    }
                },
                onDismiss = { showThemeStudio = false }
            )
        }
    }
}
