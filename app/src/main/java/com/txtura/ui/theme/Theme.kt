package com.txtura.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import androidx.core.graphics.get
import androidx.core.graphics.scale

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
    val dangerColor: Color = RoseDanger,
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
    accentPrimary = IndigoRoyal,
    accentSecondary = SapphireDeep,
    outgoingBubbleStart = Color(0xDD4338CA),
    outgoingBubbleEnd = Color(0xDD0284C7),
    incomingBubbleSurface = Color(0xEEFFFFFF)
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
    accentPrimary = SapphireAccent,
    accentSecondary = IndigoSoft,
    outgoingBubbleStart = Color(0xCC312E81),
    outgoingBubbleEnd = Color(0xCC0369A1),
    incomingBubbleSurface = Color(0x401E293B)
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
    accentPrimary = SapphireAccent,
    accentSecondary = WisteriaGlass,
    outgoingBubbleStart = Color(0xD91E40AF),
    outgoingBubbleEnd = Color(0xD94338CA),
    incomingBubbleSurface = Color(0x3B161E2E)
)

val LocalGlassPalette = staticCompositionLocalOf { AmoledGlassPalette }

/**
 * Complementary Color Harmony Engine:
 * Adapts UI accents, message bubbles, specular glass borders, and card surfaces so they are
 * color-wheel complementary and visually harmonious with the active wallpaper preset or custom Gallery photo.
 */
object WallpaperHarmonyEngine {

    private val customImageColorCache = ConcurrentHashMap<String, Color>()

    fun getCachedCustomColor(uriString: String?): Color? {
        if (uriString.isNullOrBlank()) return null
        return customImageColorCache[uriString]
    }

    /**
     * Extracts the dominant vibrant color from a user-selected Gallery photo bitmap.
     */
    fun extractAndCacheDominantColor(uriString: String, bitmap: Bitmap): Color {
        customImageColorCache[uriString]?.let { return it }
        return try {
            val small = bitmap.scale(16, 16)
            val hsv = FloatArray(3)
            var sumX = 0.0
            var sumY = 0.0
            var weightSum = 0.0
            var fallbackR = 0f
            var fallbackG = 0f
            var fallbackB = 0f
            val totalPixels = small.width * small.height

            for (y in 0 until small.height) {
                for (x in 0 until small.width) {
                    val pixel = small[x, y]
                    val r = android.graphics.Color.red(pixel) / 255f
                    val g = android.graphics.Color.green(pixel) / 255f
                    val b = android.graphics.Color.blue(pixel) / 255f
                    fallbackR += r
                    fallbackG += g
                    fallbackB += b

                    android.graphics.Color.RGBToHSV(
                        android.graphics.Color.red(pixel),
                        android.graphics.Color.green(pixel),
                        android.graphics.Color.blue(pixel),
                        hsv
                    )
                    val sat = hsv[1]
                    val value = hsv[2]
                    val weight = (sat * 0.75f + value * 0.25f).toDouble()
                    if (sat > 0.12f && value in 0.12f..0.95f) {
                        val rad = Math.toRadians(hsv[0].toDouble())
                        sumX += cos(rad) * weight
                        sumY += sin(rad) * weight
                        weightSum += weight
                    }
                }
            }

            val extracted = if (weightSum > 0.01) {
                val avgHue = ((Math.toDegrees(atan2(sumY, sumX)).toFloat() + 360f) % 360f)
                Color.hsv(avgHue, 0.65f, 0.85f)
            } else {
                Color(
                    red = (fallbackR / totalPixels).coerceIn(0f, 1f),
                    green = (fallbackG / totalPixels).coerceIn(0f, 1f),
                    blue = (fallbackB / totalPixels).coerceIn(0f, 1f)
                )
            }
            customImageColorCache[uriString] = extracted
            extracted
        } catch (_: Exception) {
            SapphireAccent
        }
    }

    /**
     * Returns the primary complementary UI accent color for a given [WallpaperPreset],
     * used both by the live theme and by the preset preview swatches.
     */
    fun presetComplementaryAccent(preset: WallpaperPreset, isDark: Boolean): Color {
        return when (preset) {
            WallpaperPreset.DEFAULT -> if (isDark) SapphireAccent else IndigoRoyal
            WallpaperPreset.AURORA_BOREALIS -> if (isDark) Color(0xFFF472B6) else Color(0xFFE11D48)
            WallpaperPreset.VELVET_PLUM -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            WallpaperPreset.MIDNIGHT_OCEAN -> if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
            WallpaperPreset.SUNSET_HORIZON -> if (isDark) Color(0xFF14B8A6) else Color(0xFF0D9488)
            WallpaperPreset.EMERALD_FOREST -> if (isDark) Color(0xFFFB7185) else Color(0xFFBE123C)
            WallpaperPreset.ROYAL_AMETHYST -> if (isDark) Color(0xFFFACC15) else Color(0xFFCA8A04)
            WallpaperPreset.STARLIGHT_GALAXY -> if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
            WallpaperPreset.CRIMSON_VELVET -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
            WallpaperPreset.ARCTIC_FROST -> if (isDark) Color(0xFFFDA4AF) else Color(0xFFE11D48)
            WallpaperPreset.OBSIDIAN_GOLD -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

            // 10 Refined Monochrome Presets (Tonal single-color accents)
            WallpaperPreset.MONO_SLATE -> if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
            WallpaperPreset.MONO_SAPPHIRE -> if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
            WallpaperPreset.MONO_SAGE -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
            WallpaperPreset.MONO_CHAMPAGNE -> if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
            WallpaperPreset.MONO_PLUM -> if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
            WallpaperPreset.MONO_GRAPHITE -> if (isDark) Color(0xFFA3A3A3) else Color(0xFF404040)
            WallpaperPreset.MONO_ROSE -> if (isDark) Color(0xFFF472B6) else Color(0xFFBE185D)
            WallpaperPreset.MONO_NAVY -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
            WallpaperPreset.MONO_SMOKE -> if (isDark) Color(0xFFA8A29E) else Color(0xFF57534E)
            WallpaperPreset.MONO_ESPRESSO -> if (isDark) Color(0xFFFB923C) else Color(0xFF9A3412)
        }
    }

    private fun presetSecondaryComplementaryAccent(preset: WallpaperPreset, isDark: Boolean): Color {
        return when (preset) {
            WallpaperPreset.DEFAULT -> if (isDark) WisteriaGlass else SapphireDeep
            WallpaperPreset.AURORA_BOREALIS -> if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
            WallpaperPreset.VELVET_PLUM -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
            WallpaperPreset.MIDNIGHT_OCEAN -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
            WallpaperPreset.SUNSET_HORIZON -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            WallpaperPreset.EMERALD_FOREST -> if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA)
            WallpaperPreset.ROYAL_AMETHYST -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
            WallpaperPreset.STARLIGHT_GALAXY -> if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
            WallpaperPreset.CRIMSON_VELVET -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            WallpaperPreset.ARCTIC_FROST -> if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706)
            WallpaperPreset.OBSIDIAN_GOLD -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)

            // 10 Refined Monochrome Presets
            WallpaperPreset.MONO_SLATE -> if (isDark) Color(0xFF64748B) else Color(0xFF334155)
            WallpaperPreset.MONO_SAPPHIRE -> if (isDark) Color(0xFF3B82F6) else Color(0xFF1E3A8A)
            WallpaperPreset.MONO_SAGE -> if (isDark) Color(0xFF22C55E) else Color(0xFF166534)
            WallpaperPreset.MONO_CHAMPAGNE -> if (isDark) Color(0xFFF59E0B) else Color(0xFF78350F)
            WallpaperPreset.MONO_PLUM -> if (isDark) Color(0xFFA855F7) else Color(0xFF581C87)
            WallpaperPreset.MONO_GRAPHITE -> if (isDark) Color(0xFF737373) else Color(0xFF262626)
            WallpaperPreset.MONO_ROSE -> if (isDark) Color(0xFFE11D48) else Color(0xFF831843)
            WallpaperPreset.MONO_NAVY -> if (isDark) Color(0xFF60A5FA) else Color(0xFF172554)
            WallpaperPreset.MONO_SMOKE -> if (isDark) Color(0xFF78716C) else Color(0xFF44403C)
            WallpaperPreset.MONO_ESPRESSO -> if (isDark) Color(0xFFEA580C) else Color(0xFF7C2D12)
        }
    }

    /**
     * Produces a canonical, idempotent [GlassPalette] adapted to be complementary to [wallpaperConfig].
     */
    fun adaptPalette(
        isDark: Boolean,
        isAmoled: Boolean,
        wallpaperConfig: WallpaperConfig,
        customDominantColor: Color? = getCachedCustomColor(wallpaperConfig.customImageUri)
    ): GlassPalette {
        val canonicalBase = when {
            !isDark -> LightGlassPalette
            isAmoled -> AmoledGlassPalette
            else -> DarkGlassPalette
        }

        // 1. Custom Gallery Photo Wallpaper -> compute true 180° & 145° split-complementary HSV palette
        if (wallpaperConfig.isCustomGalleryImage && customDominantColor != null) {
            val hsv = FloatArray(3)
            val argb = customDominantColor.toArgb()
            android.graphics.Color.RGBToHSV(
                android.graphics.Color.red(argb),
                android.graphics.Color.green(argb),
                android.graphics.Color.blue(argb),
                hsv
            )
            val bgHue = hsv[0]
            val compHue = (bgHue + 180f) % 360f
            val splitHue1 = (bgHue + 145f) % 360f
            val splitHue2 = (bgHue + 215f) % 360f

            val accentPrimary = if (isDark) {
                Color.hsv(compHue, 0.68f, 0.92f)
            } else {
                Color.hsv(compHue, 0.74f, 0.68f)
            }
            val accentSecondary = if (isDark) {
                Color.hsv(splitHue1, 0.65f, 0.90f)
            } else {
                Color.hsv(splitHue1, 0.70f, 0.64f)
            }
            val bubbleAlpha = if (isAmoled) 0.82f else 0.88f
            val bubbleStart = Color.hsv(compHue, 0.68f, 0.72f).copy(alpha = bubbleAlpha)
            val bubbleEnd = Color.hsv(splitHue2, 0.70f, 0.66f).copy(alpha = bubbleAlpha)

            val tintedSurface = if (isDark) {
                lerp(canonicalBase.glassSurface, customDominantColor.copy(alpha = canonicalBase.glassSurface.alpha), 0.22f)
            } else {
                lerp(canonicalBase.glassSurface, customDominantColor.copy(alpha = canonicalBase.glassSurface.alpha), 0.12f)
            }
            val tintedElevated = if (isDark) {
                lerp(canonicalBase.glassSurfaceElevated, accentPrimary.copy(alpha = canonicalBase.glassSurfaceElevated.alpha), 0.18f)
            } else {
                lerp(canonicalBase.glassSurfaceElevated, accentPrimary.copy(alpha = canonicalBase.glassSurfaceElevated.alpha), 0.10f)
            }
            val tintedIncoming = if (isDark) {
                lerp(canonicalBase.incomingBubbleSurface, customDominantColor.copy(alpha = 0.24f), 0.30f)
            } else {
                lerp(canonicalBase.incomingBubbleSurface, customDominantColor.copy(alpha = 0.88f), 0.12f)
            }

            return canonicalBase.copy(
                orbPrimary = customDominantColor.copy(alpha = 0.32f),
                orbSecondary = accentPrimary.copy(alpha = 0.26f),
                glassSurface = tintedSurface,
                glassSurfaceElevated = tintedElevated,
                glassBorderTop = accentPrimary.copy(alpha = if (isDark) 0.44f else 0.65f),
                glassBorderBottom = customDominantColor.copy(alpha = if (isDark) 0.18f else 0.32f),
                accentPrimary = accentPrimary,
                accentSecondary = accentSecondary,
                outgoingBubbleStart = bubbleStart,
                outgoingBubbleEnd = bubbleEnd,
                incomingBubbleSurface = tintedIncoming
            )
        }

        // 2. Built-in Wallpaper Preset
        val preset = wallpaperConfig.preset
        if (preset == WallpaperPreset.DEFAULT) {
            return canonicalBase
        }

        val bgPrimary = Color(preset.primaryOrbHex)
        val bgSecondary = Color(preset.secondaryOrbHex)
        val compPrimary = presetComplementaryAccent(preset, isDark)
        val compSecondary = presetSecondaryComplementaryAccent(preset, isDark)

        val bubbleAlpha = if (isAmoled) 0.82f else 0.88f
        val bubbleStart = if (preset.isMonochrome) {
            Color(preset.primaryOrbHex).copy(alpha = bubbleAlpha)
        } else {
            lerp(compPrimary, bgSecondary, 0.25f).copy(alpha = bubbleAlpha)
        }
        val bubbleEnd = if (preset.isMonochrome) {
            Color(preset.secondaryOrbHex).copy(alpha = bubbleAlpha)
        } else {
            lerp(compSecondary, bgPrimary, 0.30f).copy(alpha = bubbleAlpha)
        }

        val surfaceBaseAlpha = canonicalBase.glassSurface.alpha
        val elevatedBaseAlpha = canonicalBase.glassSurfaceElevated.alpha

        val tintedSurface = if (isDark) {
            lerp(Color.White, bgPrimary, 0.25f).copy(alpha = surfaceBaseAlpha)
        } else {
            lerp(Color.White, bgPrimary, 0.10f).copy(alpha = surfaceBaseAlpha)
        }
        val tintedElevated = if (isDark) {
            lerp(Color.White, compPrimary, 0.20f).copy(alpha = elevatedBaseAlpha)
        } else {
            lerp(Color.White, compPrimary, 0.08f).copy(alpha = elevatedBaseAlpha)
        }
        val tintedIncoming = if (isDark) {
            lerp(Color.White, bgPrimary, 0.28f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
        } else {
            lerp(Color.White, bgPrimary, 0.09f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
        }

        return canonicalBase.copy(
            backgroundBase = if (isDark) Color(preset.baseColorHex) else canonicalBase.backgroundBase,
            orbPrimary = bgPrimary.copy(alpha = if (isDark) 0.30f else 0.24f),
            orbSecondary = bgSecondary.copy(alpha = if (isDark) 0.26f else 0.20f),
            glassSurface = tintedSurface,
            glassSurfaceElevated = tintedElevated,
            glassBorderTop = compPrimary.copy(alpha = if (isDark) 0.45f else 0.65f),
            glassBorderBottom = bgPrimary.copy(alpha = if (isDark) 0.20f else 0.32f),
            accentPrimary = compPrimary,
            accentSecondary = compSecondary,
            outgoingBubbleStart = bubbleStart,
            outgoingBubbleEnd = bubbleEnd,
            incomingBubbleSurface = tintedIncoming
        )
    }

    fun colorSchemeForPalette(palette: GlassPalette): ColorScheme {
        return when {
            palette.isAmoled -> darkColorScheme(
                primary = palette.accentPrimary,
                secondary = palette.accentSecondary,
                tertiary = WisteriaGlass,
                background = palette.backgroundBase,
                surface = palette.backgroundBase,
                onPrimary = Color.Black,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary
            )
            palette.isDark -> darkColorScheme(
                primary = palette.accentPrimary,
                secondary = palette.accentSecondary,
                tertiary = WisteriaGlass,
                background = palette.backgroundBase,
                surface = Color(0xFF111827),
                onPrimary = Color.Black,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary
            )
            else -> lightColorScheme(
                primary = palette.accentPrimary,
                secondary = palette.accentSecondary,
                tertiary = WisteriaGlass,
                background = palette.backgroundBase,
                surface = Color.White,
                onPrimary = Color.White,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary
            )
        }
    }
}

@Composable
fun TxturaTheme(
    themeMode: TxturaThemeMode = TxturaThemeMode.AMOLED,
    wallpaperConfig: WallpaperConfig = WallpaperConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val basePalette = when (themeMode) {
        TxturaThemeMode.LIGHT -> LightGlassPalette
        TxturaThemeMode.DARK -> DarkGlassPalette
        TxturaThemeMode.AMOLED -> AmoledGlassPalette
        TxturaThemeMode.SYSTEM -> if (systemDark) AmoledGlassPalette else LightGlassPalette
    }

    val adaptedPalette = remember(basePalette, wallpaperConfig) {
        WallpaperHarmonyEngine.adaptPalette(
            isDark = basePalette.isDark,
            isAmoled = basePalette.isAmoled,
            wallpaperConfig = wallpaperConfig
        )
    }

    val colorScheme = remember(adaptedPalette) {
        WallpaperHarmonyEngine.colorSchemeForPalette(adaptedPalette)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !adaptedPalette.isDark
                isAppearanceLightNavigationBars = !adaptedPalette.isDark
            }
        }
    }

    CompositionLocalProvider(LocalGlassPalette provides adaptedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
