package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.GalleryPhoto
import com.example.editor.BitmapProcessor
import com.example.editor.CropAspect
import com.example.editor.DrawStroke
import com.example.editor.EditorInkColors
import com.example.editor.FilterPreset
import com.example.editor.NormalizedPoint
import com.example.editor.PhotoEditState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class EditorTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FILTERS("Filters", Icons.Filled.AutoFixHigh),
    ADJUST("Adjust", Icons.Filled.Tune),
    TRANSFORM("Transform", Icons.Filled.Crop),
    MARKUP("Markup", Icons.Filled.Brush)
}

@Composable
fun PhotoEditorScreen(
    photo: GalleryPhoto,
    driveAccessToken: String?,
    onBack: () -> Unit,
    onSaveLocal: (Bitmap, String, Boolean) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var sourceBitmap by remember(photo.id) { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember(photo.id) { mutableStateOf<Bitmap?>(null) }
    var loadError by remember(photo.id) { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var editState by remember(photo.id) { mutableStateOf(PhotoEditState()) }
    val undoStack = remember(photo.id) { mutableStateListOf<PhotoEditState>() }
    var activeTab by remember { mutableIntStateOf(0) }
    var compareOriginal by remember { mutableStateOf(false) }

    var activeInkColor by remember { mutableStateOf(EditorInkColors[1]) }
    var strokeRatio by remember { mutableFloatStateOf(0.014f) }
    val activeDragPoints = remember { mutableStateListOf<NormalizedPoint>() }

    fun pushEdit(transform: (PhotoEditState) -> PhotoEditState) {
        undoStack.add(editState)
        if (undoStack.size > 25) undoStack.removeAt(0)
        editState = transform(editState)
    }

    LaunchedEffect(photo.id, driveAccessToken) {
        val loaded = BitmapProcessor.loadBitmapForEditor(
            context = context,
            photo = photo,
            driveAccessToken = driveAccessToken,
            maxDimension = 1280
        )
        if (loaded != null) {
            sourceBitmap = loaded
            previewBitmap = loaded
            loadError = null
        } else {
            loadError = "Unable to decode photo for editing."
        }
    }

    LaunchedEffect(sourceBitmap, editState) {
        val src = sourceBitmap ?: return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            BitmapProcessor.renderEditedBitmap(src, editState)
        }
        previewBitmap = rendered
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Editor Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = Color.White)
                }
                Text("Lumina Studio", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        if (undoStack.isNotEmpty()) {
                            editState = undoStack.removeAt(undoStack.lastIndex)
                        }
                    },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier.testTag("editor_undo_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (undoStack.isNotEmpty()) Color.White else Color.Gray
                    )
                }
                IconButton(
                    onClick = {
                        if (!editState.isDefault) {
                            undoStack.add(editState)
                            editState = PhotoEditState()
                        }
                    },
                    enabled = !editState.isDefault,
                    modifier = Modifier.testTag("editor_reset_btn")
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Reset edits",
                        tint = if (!editState.isDefault) Color.White else Color.Gray
                    )
                }
                Button(
                    onClick = {
                        val finalBmp = previewBitmap ?: return@Button
                        isSaving = true
                        onSaveLocal(finalBmp, photo.title, false)
                    },
                    enabled = previewBitmap != null && !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("editor_save_btn")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }

        // Canvas Preview Area
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            when {
                loadError != null -> {
                    Text(loadError!!, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodyLarge)
                }
                previewBitmap == null -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                else -> {
                    val displayBmp = if (compareOriginal) sourceBitmap ?: previewBitmap!! else previewBitmap!!
                    val drawMode = EditorTab.entries[activeTab] == EditorTab.MARKUP

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF05070B))
                    ) {
                        val imgBitmap = remember(displayBmp) { displayBmp.asImageBitmap() }
                        Image(
                            bitmap = imgBitmap,
                            contentDescription = "Edited photo preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (drawMode) {
                                        Modifier.pointerInput(activeInkColor, strokeRatio) {
                                            detectDragGestures(
                                                onDragStart = { start ->
                                                    activeDragPoints.clear()
                                                    activeDragPoints.add(
                                                        NormalizedPoint(
                                                            (start.x / size.width).coerceIn(0f, 1f),
                                                            (start.y / size.height).coerceIn(0f, 1f)
                                                        )
                                                    )
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    activeDragPoints.add(
                                                        NormalizedPoint(
                                                            (change.position.x / size.width).coerceIn(0f, 1f),
                                                            (change.position.y / size.height).coerceIn(0f, 1f)
                                                        )
                                                    )
                                                },
                                                onDragEnd = {
                                                    if (activeDragPoints.isNotEmpty()) {
                                                        val newStroke = DrawStroke(
                                                            points = activeDragPoints.toList(),
                                                            colorValue = activeInkColor.toArgb().toLong() and 0xFFFFFFFFL,
                                                            strokeWidthRatio = strokeRatio
                                                        )
                                                        pushEdit { it.copy(strokes = it.strokes + newStroke) }
                                                        activeDragPoints.clear()
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        )

                        // Compare Original hold pill
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            compareOriginal = true
                                            tryAwaitRelease()
                                            compareOriginal = false
                                        }
                                    )
                                }
                                .testTag("editor_compare_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Filled.Compare, contentDescription = stringResource(R.string.cd_compare_original), tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (compareOriginal) "Original" else "Compare",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        if (!driveAccessToken.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .clickable(enabled = !isSaving) {
                                        val finalBmp = previewBitmap ?: return@clickable
                                        isSaving = true
                                        onSaveLocal(finalBmp, photo.title, true)
                                    }
                                    .testTag("editor_save_drive_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save + Drive", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tool Controls Panel
        Surface(
            color = Color(0xFF121723),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    EditorTab.entries.forEachIndexed { idx, tab ->
                        Tab(
                            selected = activeTab == idx,
                            onClick = { activeTab = idx },
                            text = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("editor_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                when (EditorTab.entries[activeTab]) {
                    EditorTab.FILTERS -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(FilterPreset.entries) { preset ->
                                val selected = editState.preset == preset
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF1C2333),
                                    border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { pushEdit { it.copy(preset = preset) } }
                                        .testTag("filter_preset_${preset.name.lowercase()}")
                                ) {
                                    Text(
                                        text = preset.label,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    EditorTab.ADJUST -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(196.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            AdjustmentSlider("Brightness", editState.brightness, -0.6f..0.6f) { v ->
                                editState = editState.copy(brightness = v)
                            }
                            AdjustmentSlider("Contrast", editState.contrast, 0.5f..1.8f) { v ->
                                editState = editState.copy(contrast = v)
                            }
                            AdjustmentSlider("Saturation", editState.saturation, 0f..2f) { v ->
                                editState = editState.copy(saturation = v)
                            }
                            AdjustmentSlider("Warmth", editState.warmth, -0.5f..0.5f) { v ->
                                editState = editState.copy(warmth = v)
                            }
                            AdjustmentSlider("Matte Fade", editState.fade, 0f..0.5f) { v ->
                                editState = editState.copy(fade = v)
                            }
                            AdjustmentSlider("Vignette", editState.vignette, 0f..0.85f) { v ->
                                editState = editState.copy(vignette = v)
                            }
                        }
                    }

                    EditorTab.TRANSFORM -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { pushEdit { it.copy(rotationQuarterTurns = it.rotationQuarterTurns - 1) } },
                                    modifier = Modifier.weight(1f).testTag("editor_rotate_left")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left", modifier = Modifier.size(18.dp))
                                }
                                OutlinedButton(
                                    onClick = { pushEdit { it.copy(rotationQuarterTurns = it.rotationQuarterTurns + 1) } },
                                    modifier = Modifier.weight(1f).testTag("editor_rotate_right")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Right", modifier = Modifier.size(18.dp))
                                }
                                OutlinedButton(
                                    onClick = { pushEdit { it.copy(flipHorizontal = !it.flipHorizontal) } },
                                    modifier = Modifier.weight(1f).testTag("editor_flip_h")
                                ) {
                                    Icon(Icons.Filled.Flip, contentDescription = "Flip Horizontal", modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AdjustmentSlider("Straighten (${editState.straightenDegrees.roundToInt()}°)", editState.straightenDegrees, -35f..35f) { v ->
                                editState = editState.copy(straightenDegrees = v)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(CropAspect.entries) { aspect ->
                                    FilterChip(
                                        selected = editState.cropAspect == aspect,
                                        onClick = { pushEdit { it.copy(cropAspect = aspect) } },
                                        label = { Text(aspect.label) },
                                        modifier = Modifier.testTag("crop_aspect_${aspect.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    EditorTab.MARKUP -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EditorInkColors.forEach { ink ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(ink)
                                                .border(
                                                    width = if (activeInkColor == ink) 2.5.dp else 1.dp,
                                                    color = if (activeInkColor == ink) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                                                    shape = CircleShape
                                                )
                                                .clickable { activeInkColor = ink }
                                        )
                                    }
                                }
                                if (editState.strokes.isNotEmpty()) {
                                    OutlinedButton(onClick = { pushEdit { it.copy(strokes = emptyList()) } }) {
                                        Text("Clear Ink", color = Color.White)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = editState.textOverlay,
                                onValueChange = { txt -> editState = editState.copy(textOverlay = txt.take(40)) },
                                label = { Text("Add Caption / Watermark on Photo") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_text_overlay_input")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(
                text = String.format(java.util.Locale.US, "%.2f", value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}
