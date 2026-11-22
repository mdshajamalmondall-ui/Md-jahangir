package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class LuminaThemePreset(
    val id: String,
    val displayName: String,
    val tagline: String,
    val previewPrimaryHex: Long,
    val previewSecondaryHex: Long,
    val previewSurfaceHex: Long
) {
    OBSIDIAN_NEON(
        id = "OBSIDIAN_NEON",
        displayName = "Obsidian Neon",
        tagline = "AMOLED midnight with electric cyan & coral",
        previewPrimaryHex = 0xFF22D3EE,
        previewSecondaryHex = 0xFFFB7185,
        previewSurfaceHex = 0xFF090D16
    ),
    TERRACOTTA_WARM(
        id = "TERRACOTTA_WARM",
        displayName = "Warm Terracotta",
        tagline = "Sunlit clay, travertine & amber editorial",
        previewPrimaryHex = 0xFFD95D39,
        previewSecondaryHex = 0xFFE09F3E,
        previewSurfaceHex = 0xFFFAF3EB
    ),
    AURORA_GLASS(
        id = "AURORA_GLASS",
        displayName = "Aurora Glass",
        tagline = "Nordic indigo with emerald & violet glow",
        previewPrimaryHex = 0xFF34D399,
        previewSecondaryHex = 0xFFA78BFA,
        previewSurfaceHex = 0xFF0B1324
    ),
    SAKURA_MIST(
        id = "SAKURA_MIST",
        displayName = "Sakura Mist",
        tagline = "Soft rose quartz & deep plum botanical",
        previewPrimaryHex = 0xFFEC4899,
        previewSecondaryHex = 0xFF8B5CF6,
        previewSurfaceHex = 0xFFFFF5F8
    ),
    SYSTEM_DYNAMIC(
        id = "SYSTEM_DYNAMIC",
        displayName = "Material You",
        tagline = "Adaptive wallpaper palette on Android 12+",
        previewPrimaryHex = 0xFF3B82F6,
        previewSecondaryHex = 0xFF14B8A6,
        previewSurfaceHex = 0xFF111827
    );

    companion object {
        fun fromId(id: String?): LuminaThemePreset =
            entries.firstOrNull { it.id == id } ?: OBSIDIAN_NEON
    }
}

// Obsidian Neon
val ObsidianBgDark = Color(0xFF070A12)
val ObsidianSurfaceDark = Color(0xFF0F1524)
val ObsidianSurfaceVariantDark = Color(0xFF182238)
val ObsidianPrimary = Color(0xFF22D3EE)
val ObsidianOnPrimary = Color(0xFF00242B)
val ObsidianSecondary = Color(0xFFFB7185)
val ObsidianTertiary = Color(0xFF818CF8)

val ObsidianBgLight = Color(0xFFF3F8FC)
val ObsidianSurfaceLight = Color(0xFFFFFFFF)
val ObsidianSurfaceVariantLight = Color(0xFFE2EEF7)
val ObsidianPrimaryLight = Color(0xFF0891B2)

// Warm Terracotta
val TerracottaBgLight = Color(0xFFFBF5EE)
val TerracottaSurfaceLight = Color(0xFFFFFBF7)
val TerracottaSurfaceVariantLight = Color(0xFFF2E5D7)
val TerracottaPrimary = Color(0xFFC84B31)
val TerracottaSecondary = Color(0xFFD99B26)
val TerracottaTertiary = Color(0xFF4A6B5D)

val TerracottaBgDark = Color(0xFF17110E)
val TerracottaSurfaceDark = Color(0xFF221915)
val TerracottaSurfaceVariantDark = Color(0xFF332620)
val TerracottaPrimaryDark = Color(0xFFFF8A65)
val TerracottaSecondaryDark = Color(0xFFF6BD60)

// Aurora Glass
val AuroraBgDark = Color(0xFF08101E)
val AuroraSurfaceDark = Color(0xFF101C31)
val AuroraSurfaceVariantDark = Color(0xFF1A2A47)
val AuroraPrimary = Color(0xFF34D399)
val AuroraSecondary = Color(0xFFA78BFA)
val AuroraTertiary = Color(0xFF38BDF8)

val AuroraBgLight = Color(0xFFF0FDF8)
val AuroraSurfaceLight = Color(0xFFFFFFFF)
val AuroraSurfaceVariantLight = Color(0xFFDCF5EC)
val AuroraPrimaryLight = Color(0xFF059669)

// Sakura Mist
val SakuraBgLight = Color(0xFFFFF6F9)
val SakuraSurfaceLight = Color(0xFFFFFFFF)
val SakuraSurfaceVariantLight = Color(0xFFFCE7F0)
val SakuraPrimary = Color(0xFFDB2777)
val SakuraSecondary = Color(0xFF7C3AED)
val SakuraTertiary = Color(0xFFF59E0B)

val SakuraBgDark = Color(0xFF180C14)
val SakuraSurfaceDark = Color(0xFF24121E)
val SakuraSurfaceVariantDark = Color(0xFF381C2E)
val SakuraPrimaryDark = Color(0xFFF472B6)
val SakuraSecondaryDark = Color(0xFFC4B5FD)
