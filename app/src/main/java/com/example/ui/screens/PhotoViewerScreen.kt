package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.GalleryPhoto
import com.example.data.local.PhotoSourceType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photos: List<GalleryPhoto>,
    initialIndex: Int,
    initialSlideshow: Boolean,
    slideshowSeconds: Int,
    imageLoader: ImageLoader? = null,
    onBack: () -> Unit,
    onEditPhoto: (GalleryPhoto) -> Unit,
    onToggleFavorite: (GalleryPhoto) -> Unit,
    onDeletePhoto: (GalleryPhoto) -> Unit,
    onDownloadDrivePhoto: ((GalleryPhoto) -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    if (photos.isEmpty()) {
        onBack()
        return
    }

    val safeInitial = initialIndex.coerceIn(0, photos.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { photos.size })
    var chromeVisible by remember { mutableStateOf(!initialSlideshow) }
    var slideshowPlaying by remember { mutableStateOf(initialSlideshow) }
    var showInfoSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentPhoto = photos.getOrElse(pagerState.currentPage) { photos.first() }

    LaunchedEffect(slideshowPlaying, photos.size, slideshowSeconds) {
        while (slideshowPlaying && photos.size > 1) {
            delay(slideshowSeconds * 1000L)
            val next = (pagerState.currentPage + 1) % photos.size
            pagerState.animateScrollToPage(
                page = next,
                animationSpec = tween(durationMillis = 700)
            )
        }
    }

    val kenBurnsTransition = rememberInfiniteTransition(label = "ken_burns")
    val kenBurnsScale by kenBurnsTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = photos[page]
            var scale by remember(item.id) { mutableFloatStateOf(1f) }
            var offset by remember(item.id) { mutableStateOf(Offset.Zero) }

            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                val newScale = (scale * zoomChange).coerceIn(1f, 4.5f)
                scale = newScale
                offset = if (newScale > 1.02f) offset + panChange else Offset.Zero
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTapGestures(
                            onTap = { chromeVisible = !chromeVisible },
                            onDoubleTap = {
                                if (scale > 1.1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.4f
                                }
                            }
                        )
                    }
                    .then(
                        if (scale > 1.01f) Modifier.transformable(state = transformState)
                        else Modifier.transformable(state = transformState)
                    )
            ) {
                val activeScale = if (slideshowPlaying) kenBurnsScale else scale
                val imgMod = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = activeScale
                        scaleY = activeScale
                        translationX = offset.x
                        translationY = offset.y
                    }

                if (item.drawableResId != null) {
                    Image(
                        painter = painterResource(id = item.drawableResId),
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = imgMod
                    )
                } else {
                    val request = remember(item.uriString) {
                        ImageRequest.Builder(context)
                            .data(item.uriString)
                            .crossfade(true)
                            .build()
                    }
                    if (imageLoader != null) {
                        AsyncImage(
                            model = request,
                            imageLoader = imageLoader,
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = imgMod
                        )
                    } else {
                        AsyncImage(
                            model = request,
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = imgMod
                        )
                    }
                }
            }
        }

        // Top Bar
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("viewer_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = currentPhoto.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} of ${photos.size} · ${currentPhoto.albumName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { slideshowPlaying = !slideshowPlaying },
                        modifier = Modifier.testTag("viewer_slideshow_toggle")
                    ) {
                        Icon(
                            imageVector = if (slideshowPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Toggle Slideshow",
                            tint = if (slideshowPlaying) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier.testTag("viewer_info_btn")
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = "Photo details", tint = Color.White)
                    }
                }
            }
        }

        // Bottom Floating Action Bar
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = Color(0xFF121826).copy(alpha = 0.9f),
                shape = CircleShape,
                tonalElevation = 8.dp
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = { onToggleFavorite(currentPhoto) },
                        modifier = Modifier.testTag("viewer_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (currentPhoto.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (currentPhoto.isFavorite) Color(0xFFFB7185) else Color.White
                        )
                    }

                    IconButton(
                        onClick = { onEditPhoto(currentPhoto) },
                        modifier = Modifier.testTag("viewer_edit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (currentPhoto.sourceType == PhotoSourceType.DRIVE && onDownloadDrivePhoto != null) {
                        IconButton(
                            onClick = { onDownloadDrivePhoto(currentPhoto) },
                            modifier = Modifier.testTag("viewer_download_drive_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Save offline copy",
                                tint = Color.White
                            )
                        }
                    } else if (currentPhoto.uriString.startsWith("content://")) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = currentPhoto.mimeType
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPhoto.uriString))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share photo"))
                            },
                            modifier = Modifier.testTag("viewer_share_btn")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share photo", tint = Color.White)
                        }
                    }

                    if (currentPhoto.sourceType != PhotoSourceType.DRIVE) {
                        IconButton(
                            onClick = {
                                onDeletePhoto(currentPhoto)
                                if (photos.size <= 1) onBack()
                            },
                            modifier = Modifier.testTag("viewer_delete_btn")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove photo", tint = Color(0xFFFCA5A5))
                        }
                    }
                }
            }
        }
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text("Photo Details", style = MaterialTheme.typography.titleLarge)
                InfoRow("Title", currentPhoto.title)
                InfoRow("Source", currentPhoto.sourceType.name)
                InfoRow("Album", currentPhoto.albumName)
                InfoRow(
                    "Captured / Modified",
                    SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
                        .format(Date(currentPhoto.dateTakenMillis))
                )
                if (currentPhoto.width > 0 && currentPhoto.height > 0) {
                    InfoRow("Resolution", "${currentPhoto.width} × ${currentPhoto.height} px")
                }
                if (currentPhoto.sizeBytes > 0L) {
                    InfoRow("File Size", "${currentPhoto.sizeBytes / 1024} KB")
                }
                if (currentPhoto.tags.isNotEmpty()) {
                    InfoRow("Tags", currentPhoto.tags.joinToString(", "))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
