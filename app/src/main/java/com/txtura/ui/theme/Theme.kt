package com.txtura.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class TxturaThemeMode(val label: String) {
    AMOLED("AMOLED"),
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Auto")
}

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val isAmoled: Boolean,
    val backgroundBase: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val outgoingBubbleStart: Color,
    val outgoingBubbleEnd: Color,
    val incomingBubbleSurface: Color,
    val safeColor: Color = EmeraldSafe,
    val cautionColor: Color = AmberCaution,
    val dangerColor: Color = RoseDanger
)

val LightGlassPalette = GlassPalette(
    isDark = false,
    isAmoled = false,
    backgroundBase = LightBgBase,
    orbPrimary = LightOrbPrimary,
    orbSecondary = LightOrbSecondary,
    glassSurface = LightGlassSurface,
    glassSurfaceElevated = LightGlassSurfaceElevated,
    glassBorderTop = LightGlassBorderTop,
    glassBorderBottom = LightGlassBorderBottom,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    accentPrimary = IndigoElectric,
    accentSecondary = CyanDeep,
    outgoingBubbleStart = Color(0xDD4F46E5),
    outgoingBubbleEnd = Color(0xDD0284C7),
    incomingBubbleSurface = Color(0xD8FFFFFF)
)

val DarkGlassPalette = GlassPalette(
    isDark = true,
    isAmoled = false,
    backgroundBase = DarkBgBase,
    orbPrimary = DarkOrbPrimary,
    orbSecondary = DarkOrbSecondary,
    glassSurface = DarkGlassSurface,
    glassSurfaceElevated = DarkGlassSurfaceElevated,
    glassBorderTop = DarkGlassBorderTop,
    glassBorderBottom = DarkGlassBorderBottom,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    accentPrimary = IndigoSoft,
    accentSecondary = CyanGlow,
    outgoingBubbleStart = Color(0xCC4F46E5),
    outgoingBubbleEnd = Color(0xCC0891B2),
    incomingBubbleSurface = Color(0x28FFFFFF)
)

val AmoledGlassPalette = GlassPalette(
    isDark = true,
    isAmoled = true,
    backgroundBase = AmoledBgBase,
    orbPrimary = AmoledOrbPrimary,
    orbSecondary = AmoledOrbSecondary,
    glassSurface = AmoledGlassSurface,
    glassSurfaceElevated = AmoledGlassSurfaceElevated,
    glassBorderTop = AmoledGlassBorderTop,
    glassBorderBottom = AmoledGlassBorderBottom,
    textPrimary = AmoledTextPrimary,
    textSecondary = AmoledTextSecondary,
    accentPrimary = CyanGlow,
    accentSecondary = VioletGlass,
    outgoingBubbleStart = Color(0xB800B4D8),
    outgoingBubbleEnd = Color(0xB86366F1),
    incomingBubbleSurface = Color(0x1AFFFFFF)
)

val LocalGlassPalette = staticCompositionLocalOf { AmoledGlassPalette }

private val LightColors = lightColorScheme(
    primary = IndigoElectric,
    secondary = CyanDeep,
    tertiary = VioletGlass,
    background = LightBgBase,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary
)

private val DarkColors = darkColorScheme(
    primary = IndigoSoft,
    secondary = CyanGlow,
    tertiary = VioletGlass,
    background = DarkBgBase,
    surface = Color(0xFF111827),
    onPrimary = Color.White,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary
)

private val AmoledColors = darkColorScheme(
    primary = CyanGlow,
    secondary = IndigoSoft,
    tertiary = VioletGlass,
    background = AmoledBgBase,
    surface = AmoledBgBase,
    onPrimary = Color.Black,
    onBackground = AmoledTextPrimary,
    onSurface = AmoledTextPrimary
)

@Composable
fun TxturaTheme(
    themeMode: TxturaThemeMode = TxturaThemeMode.AMOLED,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val palette = when (themeMode) {
        TxturaThemeMode.LIGHT -> LightGlassPalette
        TxturaThemeMode.DARK -> DarkGlassPalette
        TxturaThemeMode.AMOLED -> AmoledGlassPalette
        TxturaThemeMode.SYSTEM -> if (systemDark) AmoledGlassPalette else LightGlassPalette
    }

    val colorScheme = when {
        palette.isAmoled -> AmoledColors
        palette.isDark -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.isDark
                isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
