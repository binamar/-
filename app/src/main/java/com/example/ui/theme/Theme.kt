package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.DarkContrastMode
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.resolveStrings

val LocalMinimalistMode = staticCompositionLocalOf { false }

private val FlowDayLightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = SkySecondary,
    onSecondary = Color.White,
    secondaryContainer = SkyContainerLight,
    onSecondaryContainer = Color(0xFF082F49),
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF451A03),
    error = CoralError,
    onError = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = Color(0xFFFDFEFF),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0)
)

private fun darkColorSchemeFor(contrast: DarkContrastMode) = when (contrast) {
    DarkContrastMode.BALANCED -> darkColorScheme(
        primary = TealPrimaryLight,
        onPrimary = Color(0xFF042F2E),
        primaryContainer = TealContainerDark,
        onPrimaryContainer = Color(0xFFCCFBF1),
        secondary = SkySecondaryLight,
        onSecondary = Color(0xFF082F49),
        secondaryContainer = SkyContainerDark,
        onSecondaryContainer = Color(0xFFE0F2FE),
        tertiary = AmberTertiaryLight,
        onTertiary = Color(0xFF451A03),
        tertiaryContainer = AmberContainerDark,
        onTertiaryContainer = Color(0xFFFEF3C7),
        error = CoralErrorLight,
        onError = Color(0xFF450A0A),
        background = DarkBalancedBg,
        onBackground = DarkBalancedOnSurface,
        surface = DarkBalancedSurface,
        onSurface = DarkBalancedOnSurface,
        surfaceVariant = DarkBalancedSurfaceVariant,
        onSurfaceVariant = DarkBalancedOnSurfaceVariant,
        surfaceContainerLow = Color(0xFF141E2F),
        surfaceContainer = DarkBalancedSurface,
        surfaceContainerHigh = DarkBalancedSurfaceVariant,
        outline = DarkBalancedOutline,
        outlineVariant = Color(0xFF263349)
    )
    DarkContrastMode.HIGH_CONTRAST -> darkColorScheme(
        primary = TealPrimaryLight,
        onPrimary = Color(0xFF021F1E),
        primaryContainer = Color(0xFF0F766E),
        onPrimaryContainer = Color.White,
        secondary = SkySecondaryLight,
        onSecondary = Color(0xFF031B29),
        secondaryContainer = Color(0xFF0369A1),
        onSecondaryContainer = Color.White,
        tertiary = AmberTertiaryLight,
        onTertiary = Color(0xFF270E01),
        tertiaryContainer = Color(0xFFB45309),
        onTertiaryContainer = Color.White,
        error = CoralErrorLight,
        onError = Color(0xFF2A0505),
        background = DarkHighContrastBg,
        onBackground = DarkHighContrastOnSurface,
        surface = DarkHighContrastSurface,
        onSurface = DarkHighContrastOnSurface,
        surfaceVariant = DarkHighContrastSurfaceVariant,
        onSurfaceVariant = DarkHighContrastOnSurfaceVariant,
        surfaceContainerLow = Color(0xFF0C121E),
        surfaceContainer = DarkHighContrastSurface,
        surfaceContainerHigh = DarkHighContrastSurfaceVariant,
        outline = DarkHighContrastOutline,
        outlineVariant = Color(0xFF334155)
    )
    DarkContrastMode.OLED_BLACK -> darkColorScheme(
        primary = TealPrimaryLight,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF0F514A),
        onPrimaryContainer = Color(0xFFE6FFFA),
        secondary = SkySecondaryLight,
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF0A3A57),
        onSecondaryContainer = Color(0xFFE0F2FE),
        tertiary = AmberTertiaryLight,
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF5C2808),
        onTertiaryContainer = Color(0xFFFEF3C7),
        error = CoralErrorLight,
        onError = Color.Black,
        background = DarkOledBg,
        onBackground = DarkOledOnSurface,
        surface = DarkOledSurface,
        onSurface = DarkOledOnSurface,
        surfaceVariant = DarkOledSurfaceVariant,
        onSurfaceVariant = DarkOledOnSurfaceVariant,
        surfaceContainerLow = Color(0xFF06080D),
        surfaceContainer = DarkOledSurface,
        surfaceContainerHigh = DarkOledSurfaceVariant,
        outline = DarkOledOutline,
        outlineVariant = Color(0xFF1F2937)
    )
}

val FlowDayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlowDayTheme(
    preferences: UserPreferences = UserPreferences(),
    content: @Composable () -> Unit
) {
    val isDark = when (preferences.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) {
        darkColorSchemeFor(preferences.darkContrastMode)
    } else {
        FlowDayLightColors
    }

    val strings = resolveStrings(preferences.language)
    val layoutDirection = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = buildFlowDayTypography(
        scale = preferences.fontScale,
        isArabic = strings.isArabic
    )

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides layoutDirection,
        LocalMinimalistMode provides preferences.minimalistMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = FlowDayShapes,
            content = content
        )
    }
}
