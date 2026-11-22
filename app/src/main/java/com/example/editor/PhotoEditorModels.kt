package com.example.editor

import androidx.compose.ui.graphics.Color

enum class FilterPreset(val label: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    NOIR("Noir"),
    GOLDEN("Golden"),
    CYBERPUNK("Neon"),
    MATTE("Matte"),
    SEPIA("Vintage"),
    ARCTIC("Arctic")
}

enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Original", null),
    SQUARE_1_1("1:1", 1f),
    PORTRAIT_3_4("3:4", 3f / 4f),
    LANDSCAPE_4_3("4:3", 4f / 3f),
    WIDE_16_9("16:9", 16f / 9f)
}

data class NormalizedPoint(val x: Float, val y: Float)

data class DrawStroke(
    val points: List<NormalizedPoint>,
    val colorValue: Long = 0xFF22D3EE,
    val strokeWidthRatio: Float = 0.012f
)

data class PhotoEditState(
    val preset: FilterPreset = FilterPreset.ORIGINAL,
    val brightness: Float = 0f,      // -0.6f .. 0.6f
    val contrast: Float = 1f,        // 0.5f .. 1.8f
    val saturation: Float = 1f,      // 0f .. 2f
    val warmth: Float = 0f,          // -0.5f .. 0.5f
    val fade: Float = 0f,            // 0f .. 0.5f
    val vignette: Float = 0f,        // 0f .. 0.85f
    val rotationQuarterTurns: Int = 0, // 0, 1, 2, 3
    val straightenDegrees: Float = 0f, // -35f .. 35f
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropAspect: CropAspect = CropAspect.FREE,
    val strokes: List<DrawStroke> = emptyList(),
    val textOverlay: String = "",
    val textColorValue: Long = 0xFFFFFFFF
) {
    val isDefault: Boolean
        get() = preset == FilterPreset.ORIGINAL &&
            brightness == 0f &&
            contrast == 1f &&
            saturation == 1f &&
            warmth == 0f &&
            fade == 0f &&
            vignette == 0f &&
            rotationQuarterTurns % 4 == 0 &&
            straightenDegrees == 0f &&
            !flipHorizontal &&
            !flipVertical &&
            cropAspect == CropAspect.FREE &&
            strokes.isEmpty() &&
            textOverlay.isBlank()
}

val EditorInkColors: List<Color> = listOf(
    Color(0xFFFFFFFF),
    Color(0xFF22D3EE),
    Color(0xFFFB7185),
    Color(0xFFFBBF24),
    Color(0xFF34D399),
    Color(0xFFA78BFA),
    Color(0xFF111827)
)
