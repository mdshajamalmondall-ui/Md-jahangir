package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

val LuminaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp)
)

@Composable
fun LuminaTheme(
    preset: LuminaThemePreset = LuminaThemePreset.OBSIDIAN_NEON,
    darkTheme: Boolean = isSystemInDarkTheme(),
    ambientMotionEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val targetScheme: ColorScheme = when (preset) {
        LuminaThemePreset.OBSIDIAN_NEON -> if (darkTheme) {
            darkColorScheme(
                primary = ObsidianPrimary,
                onPrimary = ObsidianOnPrimary,
                primaryContainer = Color(0xFF0A3A4A),
                onPrimaryContainer = Color(0xFFB5F4FF),
                secondary = ObsidianSecondary,
                onSecondary = Color(0xFF3F0010),
                secondaryContainer = Color(0xFF4A1827),
                onSecondaryContainer = Color(0xFFFFD9E0),
                tertiary = ObsidianTertiary,
                background = ObsidianBgDark,
                onBackground = Color(0xFFF1F5F9),
                surface = ObsidianSurfaceDark,
                onSurface = Color(0xFFF1F5F9),
                surfaceVariant = ObsidianSurfaceVariantDark,
                onSurfaceVariant = Color(0xFFA3B3CC),
                outline = Color(0xFF2E3D59)
            )
        } else {
            lightColorScheme(
                primary = ObsidianPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFCFFAFE),
                onPrimaryContainer = Color(0xFF063B4B),
                secondary = Color(0xFFE11D48),
                onSecondary = Color.White,
                tertiary = Color(0xFF4F46E5),
                background = ObsidianBgLight,
                onBackground = Color(0xFF0F172A),
                surface = ObsidianSurfaceLight,
                onSurface = Color(0xFF0F172A),
                surfaceVariant = ObsidianSurfaceVariantLight,
                onSurfaceVariant = Color(0xFF475569)
            )
        }

        LuminaThemePreset.TERRACOTTA_WARM -> if (darkTheme) {
            darkColorScheme(
                primary = TerracottaPrimaryDark,
                onPrimary = Color(0xFF3E1105),
                primaryContainer = Color(0xFF5A2313),
                onPrimaryContainer = Color(0xFFFFDBD0),
                secondary = TerracottaSecondaryDark,
                onSecondary = Color(0xFF362200),
                tertiary = Color(0xFF81B29A),
                background = TerracottaBgDark,
                onBackground = Color(0xFFF7EDE2),
                surface = TerracottaSurfaceDark,
                onSurface = Color(0xFFF7EDE2),
                surfaceVariant = TerracottaSurfaceVariantDark,
                onSurfaceVariant = Color(0xFFCBB8AC)
            )
        } else {
            lightColorScheme(
                primary = TerracottaPrimary,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFDFD6),
                onPrimaryContainer = Color(0xFF4A1608),
                secondary = TerracottaSecondary,
                onSecondary = Color(0xFF2E1E00),
                tertiary = TerracottaTertiary,
                background = TerracottaBgLight,
                onBackground = Color(0xFF281A14),
                surface = TerracottaSurfaceLight,
                onSurface = Color(0xFF281A14),
                surfaceVariant = TerracottaSurfaceVariantLight,
                onSurfaceVariant = Color(0xFF6B5347)
            )
        }

        LuminaThemePreset.AURORA_GLASS -> if (darkTheme) {
            darkColorScheme(
                primary = AuroraPrimary,
                onPrimary = Color(0xFF002819),
                primaryContainer = Color(0xFF093B2C),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = AuroraSecondary,
                onSecondary = Color(0xFF1F1147),
                tertiary = AuroraTertiary,
                background = AuroraBgDark,
                onBackground = Color(0xFFECFDF5),
                surface = AuroraSurfaceDark,
                onSurface = Color(0xFFECFDF5),
                surfaceVariant = AuroraSurfaceVariantDark,
                onSurfaceVariant = Color(0xFF9BB4CC)
            )
        } else {
            lightColorScheme(
                primary = AuroraPrimaryLight,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF064E3B),
                secondary = Color(0xFF7C3AED),
                tertiary = Color(0xFF0284C7),
                background = AuroraBgLight,
                onBackground = Color(0xFF0F2922),
                surface = AuroraSurfaceLight,
                onSurface = Color(0xFF0F2922),
                surfaceVariant = AuroraSurfaceVariantLight,
                onSurfaceVariant = Color(0xFF3E6359)
            )
        }

        LuminaThemePreset.SAKURA_MIST -> if (darkTheme) {
            darkColorScheme(
                primary = SakuraPrimaryDark,
                onPrimary = Color(0xFF450724),
                primaryContainer = Color(0xFF5D1336),
                onPrimaryContainer = Color(0xFFFFD8E9),
                secondary = SakuraSecondaryDark,
                onSecondary = Color(0xFF25124D),
                tertiary = Color(0xFFFBBF24),
                background = SakuraBgDark,
                onBackground = Color(0xFFFDF2F8),
                surface = SakuraSurfaceDark,
                onSurface = Color(0xFFFDF2F8),
                surfaceVariant = SakuraSurfaceVariantDark,
                onSurfaceVariant = Color(0xFFD6B4C8)
            )
        } else {
            lightColorScheme(
                primary = SakuraPrimary,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFCE7F3),
                onPrimaryContainer = Color(0xFF6B0F3A),
                secondary = SakuraSecondary,
                tertiary = SakuraTertiary,
                background = SakuraBgLight,
                onBackground = Color(0xFF2E1220),
                surface = SakuraSurfaceLight,
                onSurface = Color(0xFF2E1220),
                surfaceVariant = SakuraSurfaceVariantLight,
                onSurfaceVariant = Color(0xFF754C61)
            )
        }

        LuminaThemePreset.SYSTEM_DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) darkColorScheme(primary = ObsidianPrimary) else lightColorScheme(primary = ObsidianPrimaryLight)
            }
        }
    }

    val animSpec = tween<Color>(durationMillis = 420)
    val primary by animateColorAsState(targetScheme.primary, animSpec, label = "primary")
    val onPrimary by animateColorAsState(targetScheme.onPrimary, animSpec, label = "onPrimary")
    val primaryContainer by animateColorAsState(targetScheme.primaryContainer, animSpec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(targetScheme.onPrimaryContainer, animSpec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(targetScheme.secondary, animSpec, label = "secondary")
    val background by animateColorAsState(targetScheme.background, animSpec, label = "background")
    val onBackground by animateColorAsState(targetScheme.onBackground, animSpec, label = "onBackground")
    val surface by animateColorAsState(targetScheme.surface, animSpec, label = "surface")
    val onSurface by animateColorAsState(targetScheme.onSurface, animSpec, label = "onSurface")
    val surfaceVariant by animateColorAsState(targetScheme.surfaceVariant, animSpec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(targetScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant")

    val animatedScheme = targetScheme.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = Typography,
        shapes = LuminaShapes
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedAmbientBackdrop(
                enabled = ambientMotionEnabled,
                primary = animatedScheme.primary,
                secondary = animatedScheme.secondary,
                background = animatedScheme.background
            )
            content()
        }
    }
}

@Composable
fun AnimatedAmbientBackdrop(
    enabled: Boolean,
    primary: Color,
    secondary: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    val phase = if (enabled) {
        val transition = rememberInfiniteTransition(label = "ambient_backdrop")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        value
    } else {
        0.5f
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = background)
        val w = size.width
        val h = size.height
        val orb1Center = Offset(
            x = w * (0.22f + 0.12f * cos(phase)),
            y = h * (0.14f + 0.06f * sin(phase))
        )
        val orb2Center = Offset(
            x = w * (0.82f + 0.10f * sin(phase)),
            y = h * (0.78f + 0.08f * cos(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent),
                center = orb1Center,
                radius = w * 0.72f
            ),
            center = orb1Center,
            radius = w * 0.72f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.14f), Color.Transparent),
                center = orb2Center,
                radius = w * 0.68f
            ),
            center = orb2Center,
            radius = w * 0.68f
        )
    }
}
