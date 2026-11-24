package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = OnCyanContainerDark,
    secondary = AmberSignal,
    onSecondary = OnAmberSignal,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = OnAmberContainerDark,
    tertiary = SkyTelemetry,
    onTertiary = OnSkyTelemetry,
    tertiaryContainer = SkyContainerDark,
    onTertiaryContainer = OnSkyContainerDark,
    error = CoralAlert,
    onError = OnCoralAlert,
    errorContainer = CoralContainerDark,
    onErrorContainer = OnCoralContainerDark,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = SlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = SlateSurfaceBright,
    outline = SlateOutline,
    outlineVariant = SlateOutline
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = OnCyanContainerLight,
    secondary = AmberSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = OnAmberContainerLight,
    tertiary = SkyTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = SkyContainerLight,
    onTertiaryContainer = OnSkyContainerLight,
    error = CoralErrorLight,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = OnCoralContainerLight,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    outlineVariant = LightOutline
)

@Immutable
data class StreamGrabSemantics(
    val success: Color,
    val badge360pBg: Color,
    val badge360pText: Color,
    val doodAccent: Color,
    val mixDropAccent: Color
)

val LocalStreamGrabSemantics = staticCompositionLocalOf {
    StreamGrabSemantics(
        success = EmeraldSuccess,
        badge360pBg = CyanContainerDark,
        badge360pText = OnCyanContainerDark,
        doodAccent = ElectricCyan,
        mixDropAccent = AmberSignal
    )
}

private val StreamGrabShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun StreamGrabTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semantics = if (darkTheme) {
        StreamGrabSemantics(
            success = EmeraldSuccess,
            badge360pBg = CyanContainerDark,
            badge360pText = OnCyanContainerDark,
            doodAccent = ElectricCyan,
            mixDropAccent = AmberSignal
        )
    } else {
        StreamGrabSemantics(
            success = CyanPrimaryLight,
            badge360pBg = CyanContainerLight,
            badge360pText = OnCyanContainerLight,
            doodAccent = CyanPrimaryLight,
            mixDropAccent = AmberSecondaryLight
        )
    }

    CompositionLocalProvider(LocalStreamGrabSemantics provides semantics) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StreamGrabTypography,
            shapes = StreamGrabShapes,
            content = content
        )
    }
}
