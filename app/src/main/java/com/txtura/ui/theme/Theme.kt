@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma", "ClarifyingParentheses")

package com.txtura.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.txtura.data.UiColorMode
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class TxturaThemeMode(val label: String) {
    SYSTEM("Auto"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED")
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
    val sendButtonColor: Color = accentPrimary,
    val uiOpacity: Float = 0.50f,
    val uiColorMode: UiColorMode = UiColorMode.COLORFUL,
) {
    val popupContainerColor: Color
        get() {
            val accentRgb = accentPrimary.copy(alpha = 1f)
            val baseTone = when {
                !isDark -> lerp(
                    Color(0xFFE4ECF5),
                    accentRgb,
                    0.05f
                )
                isAmoled -> lerp(
                    Color(0xFF06080D),
                    accentRgb,
                    0.04f
                )
                else -> lerp(
                    Color(0xFF141C28),
                    accentRgb,
                    0.05f
                )
            }
            // Frosted glass translucency that scales smoothly with uiOpacity:
            // 100% Providno -> crystal-clear translucent glass (~0.40f)
            // 50% Providno (Podrazumevano) -> refined, balanced frosted glass (~0.64f)
            // 0% Providno (Najzamućenije) -> dense smoked obsidian glass (~0.88f)
            val solidity = (1.0f - uiOpacity.coerceIn(0f, 1f)).coerceIn(0f, 1f)
            val minAlpha = if (isDark) 0.40f else 0.46f
            val maxAlpha = if (isDark) 0.88f else 0.90f
            val targetAlpha = minAlpha + ((maxAlpha - minAlpha) * solidity)
            return baseTone.copy(alpha = targetAlpha)
        }
}

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
    incomingBubbleSurface = Color(0xCCD5E0EE),
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
    incomingBubbleSurface = Color(0x401E293B),
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
    incomingBubbleSurface = Color(0x3B161E2E),
)

val LocalGlassPalette = staticCompositionLocalOf { AmoledGlassPalette }
val LocalExpressiveAnimations = staticCompositionLocalOf { true }

/**
 * Complementary Color Harmony Engine:
 * Adapts UI accents, message bubbles, specular glass borders, and card surfaces so they are
 * color-wheel complementary and visually harmonious with the active wallpaper preset or custom Gallery photo.
 * For Monochrome themes, the base strictly follows Light vs Dark/AMOLED mode and applies 1 of 10 accent colors.
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
                        hsv,
                    )
                    val sat = hsv[1]
                    val value = hsv[2]
                    val weight = ((sat * 0.75f) + (value * 0.25f)).toDouble()
                    val isVibrant = (sat > 0.12f) && (value >= 0.12f) && (value <= 0.95f)
                    if (isVibrant) {
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
                    blue = (fallbackB / totalPixels).coerceIn(0f, 1f),
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
     * For the 10 Monochrome themes, provides 10 distinct accent colors adapted to Light vs Dark mode.
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

            // 10 Monochrome Themes: Light/Dark mode base + 1 distinct accent color
            WallpaperPreset.MONO_SLATE -> if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
            WallpaperPreset.MONO_SAPPHIRE -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            WallpaperPreset.MONO_SAGE -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
            WallpaperPreset.MONO_CHAMPAGNE -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
            WallpaperPreset.MONO_PLUM -> if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
            WallpaperPreset.MONO_GRAPHITE -> if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
            WallpaperPreset.MONO_ROSE -> if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
            WallpaperPreset.MONO_NAVY -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
            WallpaperPreset.MONO_SMOKE -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
            WallpaperPreset.MONO_ESPRESSO -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
            WallpaperPreset.ALL_BLACK -> Color.White
            WallpaperPreset.ALL_WHITE -> Color.Black
        }
    }

    private fun presetSecondaryComplementaryAccent(preset: WallpaperPreset, isDark: Boolean): Color {
        if (preset.isMonochrome) {
            // Monochrome mode uses a single accent color family
            val primary = presetComplementaryAccent(preset, isDark)
            return if (isDark) lerp(primary, Color.White, 0.15f) else lerp(primary, Color.Black, 0.15f)
        }
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
            else -> presetComplementaryAccent(preset, isDark)
        }
    }

    /**
     * Maps `uiOpacity` (0.00 = 0% providno / najzamucenije, 1.00 = 100% potpuno providno staklo):
     * - 1.00 (100% Providno): ultra-transparent, crystal-clear glass, minimal surfaces and blur
     * - 0.00 (0% Providno / Najzamucenije): deep frosted obsidian/slate smoked glass with rich blur and opacity
     * - 0.50 (Podrazumevano): balanced elegant frosted glass
     */
    private fun applyOpacityMultiplier(palette: GlassPalette, uiOpacity: Float, uiColorMode: UiColorMode): GlassPalette {
        val clamped = uiOpacity.coerceIn(0.0f, 1.0f)
        val frostDepthTone = when {
            (palette.uiColorMode == UiColorMode.MINIMALIST) && !palette.isDark -> Color(0xFFF4F4F5)
            (palette.uiColorMode == UiColorMode.MINIMALIST) && palette.isAmoled -> Color(0xFF000000)
            (palette.uiColorMode == UiColorMode.MINIMALIST) && palette.isDark -> Color(0xFF1A202C)
            !palette.isDark -> lerp(Color(0xFFCBD5E1), palette.accentPrimary.copy(alpha = 1f), 0.18f)
            palette.isAmoled -> lerp(Color(0xFF030508), palette.orbPrimary.copy(alpha = 1f), 0.20f)
            else -> lerp(Color(0xFF1E293B), palette.orbPrimary.copy(alpha = 1f), 0.34f)
        }

        // solidity: 1.0 when uiOpacity is 0.0 (najzamucenije / najpunije), 0.0 when uiOpacity is 1.0 (potpuno providno)
        val solidity = (1.0f - clamped).coerceIn(0f, 1f)

        val baseSurfRgb = lerp(palette.glassSurface.copy(alpha = 1f), frostDepthTone, 0.30f * solidity)
        val baseElevRgb = lerp(palette.glassSurfaceElevated.copy(alpha = 1f), frostDepthTone, 0.26f * solidity)
        val baseIncRgb = lerp(palette.incomingBubbleSurface.copy(alpha = 1f), frostDepthTone, 0.28f * solidity)

        val surfAlpha = if (palette.isDark) {
            lerpFloat(0.18f, 0.88f, solidity)
        } else {
            lerpFloat(0.36f, 0.92f, solidity)
        }

        val elevAlpha = if (palette.isDark) {
            lerpFloat(0.24f, 0.94f, solidity)
        } else {
            lerpFloat(0.42f, 0.96f, solidity)
        }

        val incAlpha = if (palette.isDark) {
            lerpFloat(0.20f, 0.90f, solidity)
        } else {
            lerpFloat(0.38f, 0.94f, solidity)
        }

        val bubbleAlphaFactor = lerpFloat(0.65f, 0.98f, solidity)

        return palette.copy(
            glassSurface = baseSurfRgb.copy(alpha = surfAlpha),
            glassSurfaceElevated = baseElevRgb.copy(alpha = elevAlpha),
            incomingBubbleSurface = baseIncRgb.copy(alpha = incAlpha),
            outgoingBubbleStart = palette.outgoingBubbleStart.copy(alpha = bubbleAlphaFactor),
            outgoingBubbleEnd = palette.outgoingBubbleEnd.copy(alpha = bubbleAlphaFactor),
            uiOpacity = clamped,
            uiColorMode = uiColorMode
        )
    }

    private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
        return start + ((stop - start) * fraction.coerceIn(0f, 1f))
    }

    /**
     * Produces a canonical, idempotent [GlassPalette] adapted to be complementary to [wallpaperConfig],
     * respecting [uiColorMode] and [uiOpacity].
     */
    fun adaptPalette(
        isDark: Boolean,
        isAmoled: Boolean,
        wallpaperConfig: WallpaperConfig,
        customDominantColor: Color? = getCachedCustomColor(wallpaperConfig.customImageUri),
        uiOpacity: Float = 0.50f,
        uiColorMode: UiColorMode = UiColorMode.COLORFUL
    ): GlassPalette {
        val canonicalBase = when {
            !isDark -> LightGlassPalette
            isAmoled -> AmoledGlassPalette
            else -> DarkGlassPalette
        }

        // 1. Custom Gallery Photo Wallpaper -> user-chosen accent color OR computed split-complementary HSV palette
        if (wallpaperConfig.isCustomGalleryImage) {
            val chosenAccentHex = wallpaperConfig.customAccentColorHex
            if (chosenAccentHex != null) {
                val accent = Color(chosenAccentHex)
                val bubbleAlpha = if (isAmoled) 0.84f else 0.88f
                val adapted = canonicalBase.copy(
                    orbPrimary = accent.copy(alpha = 0.28f),
                    orbSecondary = accent.copy(alpha = 0.18f),
                    glassBorderTop = accent.copy(alpha = if (isDark) 0.50f else 0.68f),
                    glassBorderBottom = accent.copy(alpha = if (isDark) 0.18f else 0.30f),
                    accentPrimary = accent,
                    accentSecondary = accent.copy(alpha = 0.85f),
                    sendButtonColor = accent,
                    outgoingBubbleStart = accent.copy(alpha = bubbleAlpha),
                    outgoingBubbleEnd = accent.copy(alpha = bubbleAlpha * 0.92f)
                )
                return applyOpacityMultiplier(adapted, uiOpacity, uiColorMode)
            }

            if (customDominantColor != null) {
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
                lerp(canonicalBase.glassSurface, customDominantColor.copy(alpha = canonicalBase.glassSurface.alpha), 0.16f)
            }
            val tintedElevated = if (isDark) {
                lerp(canonicalBase.glassSurfaceElevated, accentPrimary.copy(alpha = canonicalBase.glassSurfaceElevated.alpha), 0.18f)
            } else {
                lerp(canonicalBase.glassSurfaceElevated, accentPrimary.copy(alpha = canonicalBase.glassSurfaceElevated.alpha), 0.14f)
            }
            val tintedIncoming = if (isDark) {
                lerp(canonicalBase.incomingBubbleSurface, customDominantColor.copy(alpha = 0.24f), 0.30f)
            } else {
                lerp(canonicalBase.incomingBubbleSurface, customDominantColor.copy(alpha = 0.76f), 0.16f)
            }

            val adapted = canonicalBase.copy(
                orbPrimary = customDominantColor.copy(alpha = 0.32f),
                orbSecondary = accentPrimary.copy(alpha = 0.26f),
                glassSurface = tintedSurface,
                glassSurfaceElevated = tintedElevated,
                glassBorderTop = accentPrimary.copy(alpha = if (isDark) 0.44f else 0.65f),
                glassBorderBottom = customDominantColor.copy(alpha = if (isDark) 0.18f else 0.32f),
                accentPrimary = accentPrimary,
                accentSecondary = accentSecondary,
                sendButtonColor = accentPrimary,
                outgoingBubbleStart = bubbleStart,
                outgoingBubbleEnd = bubbleEnd,
                incomingBubbleSurface = tintedIncoming
            )
            return applyOpacityMultiplier(adapted, uiOpacity, uiColorMode)
        }
        }

        // 2. Built-in Wallpaper Preset
        val preset = wallpaperConfig.preset

        // Minimalist Themes (Pure pitch-black AMOLED / Dark Slate & Pure clean white)
        if (preset == WallpaperPreset.ALL_BLACK) {
            val adapted = if (isAmoled) {
                canonicalBase.copy(
                    isDark = true,
                    isAmoled = true,
                    backgroundBase = Color.Black,
                    orbPrimary = Color.Transparent,
                    orbSecondary = Color.Transparent,
                    glassSurface = Color(0xFF0A0A0C),
                    glassSurfaceElevated = Color(0xFF141418),
                    glassBorderTop = Color.White.copy(alpha = 0.24f),
                    glassBorderBottom = Color.White.copy(alpha = 0.08f),
                    textPrimary = Color.White,
                    textSecondary = Color(0xFFA1A1AA),
                    accentPrimary = Color.White,
                    accentSecondary = Color(0xFFD4D4D8),
                    dangerColor = RoseDanger,
                    safeColor = EmeraldSafe,
                    sendButtonColor = Color.White,
                    outgoingBubbleStart = Color(0xFF27272A),
                    outgoingBubbleEnd = Color(0xFF18181B),
                    incomingBubbleSurface = Color(0xFF101012),
                    uiOpacity = uiOpacity,
                    uiColorMode = UiColorMode.MINIMALIST
                )
            } else {
                canonicalBase.copy(
                    isDark = true,
                    isAmoled = false,
                    backgroundBase = Color(0xFF161B24),
                    orbPrimary = Color.Transparent,
                    orbSecondary = Color.Transparent,
                    glassSurface = Color(0xFF222A38),
                    glassSurfaceElevated = Color(0xFF2C3648),
                    glassBorderTop = Color.White.copy(alpha = 0.28f),
                    glassBorderBottom = Color.White.copy(alpha = 0.12f),
                    textPrimary = Color(0xFFF1F5F9),
                    textSecondary = Color(0xFFCBD5E1),
                    accentPrimary = Color.White,
                    accentSecondary = Color(0xFFE2E8F0),
                    dangerColor = RoseDanger,
                    safeColor = EmeraldSafe,
                    sendButtonColor = Color.White,
                    outgoingBubbleStart = Color(0xFF334155),
                    outgoingBubbleEnd = Color(0xFF1E293B),
                    incomingBubbleSurface = Color(0xFF242D3C),
                    uiOpacity = uiOpacity,
                    uiColorMode = UiColorMode.MINIMALIST
                )
            }
            return applyOpacityMultiplier(adapted, uiOpacity, UiColorMode.MINIMALIST)
        }

        if (preset == WallpaperPreset.ALL_WHITE) {
            val adapted = canonicalBase.copy(
                isDark = false,
                isAmoled = false,
                backgroundBase = Color.White,
                orbPrimary = Color.Transparent,
                orbSecondary = Color.Transparent,
                glassSurface = Color(0xFFF4F4F5),
                glassSurfaceElevated = Color(0xFFE4E4E7),
                glassBorderTop = Color.Black.copy(alpha = 0.16f),
                glassBorderBottom = Color.Black.copy(alpha = 0.06f),
                textPrimary = Color.Black,
                textSecondary = Color(0xFF71717A),
                accentPrimary = Color.Black,
                accentSecondary = Color(0xFF3F3F46),
                dangerColor = RoseDanger,
                safeColor = EmeraldSafe,
                sendButtonColor = Color.Black,
                outgoingBubbleStart = Color(0xFFE4E4E7),
                outgoingBubbleEnd = Color(0xFFD4D4D8),
                incomingBubbleSurface = Color(0xFFF4F4F5),
                uiOpacity = uiOpacity,
                uiColorMode = UiColorMode.MINIMALIST
            )
            return applyOpacityMultiplier(adapted, uiOpacity, UiColorMode.MINIMALIST)
        }

        val isMonochromeTheme = preset.isMonochrome || (uiColorMode == UiColorMode.MONOCHROME)

        if (isMonochromeTheme) {
            val activeMonoPreset = if (preset.isMonochrome) preset else WallpaperPreset.MONO_SLATE
            val singleAccent = presetComplementaryAccent(activeMonoPreset, isDark)
            val secondaryTone = presetSecondaryComplementaryAccent(activeMonoPreset, isDark)

            // Monochrome base strictly follows Light vs Dark (slate-charcoal) vs AMOLED (pitch-black OLED)
            val monoBgBase = when {
                !isDark -> Color(0xFFE4EBF3)
                isAmoled -> Color(0xFF000000)
                else -> Color(0xFF182130)
            }

            val surfaceBaseAlpha = canonicalBase.glassSurface.alpha
            val elevatedBaseAlpha = canonicalBase.glassSurfaceElevated.alpha

            val monoSurface = when {
                !isDark -> lerp(Color(0xFFD5DFED), singleAccent, 0.14f).copy(alpha = surfaceBaseAlpha)
                isAmoled -> lerp(Color(0xFF080A0F), singleAccent, 0.10f).copy(alpha = surfaceBaseAlpha)
                else -> lerp(Color(0xFF243044), singleAccent, 0.16f).copy(alpha = surfaceBaseAlpha)
            }
            val monoElevated = when {
                !isDark -> lerp(Color(0xFFDCE6F4), singleAccent, 0.18f).copy(alpha = elevatedBaseAlpha)
                isAmoled -> lerp(Color(0xFF0F131C), singleAccent, 0.15f).copy(alpha = elevatedBaseAlpha)
                else -> lerp(Color(0xFF2E3C54), singleAccent, 0.22f).copy(alpha = elevatedBaseAlpha)
            }
            val monoIncoming = when {
                !isDark -> lerp(Color(0xFFD8E2F0), singleAccent, 0.14f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
                isAmoled -> lerp(Color(0xFF0A0D14), singleAccent, 0.12f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
                else -> lerp(Color(0xFF263348), singleAccent, 0.18f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
            }

            val bubbleAlpha = if (isAmoled) 0.84f else 0.90f
            val bubbleBase = if (isDark) {
                lerp(singleAccent, if (isAmoled) Color(0xFF090D16) else Color(0xFF1E293B), 0.35f)
            } else {
                singleAccent
            }

            val adapted = canonicalBase.copy(
                backgroundBase = monoBgBase,
                orbPrimary = singleAccent.copy(alpha = if (isAmoled) 0.18f else if (isDark) 0.28f else 0.18f),
                orbSecondary = singleAccent.copy(alpha = if (isAmoled) 0.12f else if (isDark) 0.20f else 0.12f),
                glassSurface = monoSurface,
                glassSurfaceElevated = monoElevated,
                glassBorderTop = singleAccent.copy(alpha = if (isDark) 0.48f else 0.55f),
                glassBorderBottom = singleAccent.copy(alpha = if (isDark) 0.18f else 0.22f),
                accentPrimary = singleAccent,
                accentSecondary = secondaryTone,
                sendButtonColor = singleAccent,
                outgoingBubbleStart = bubbleBase.copy(alpha = bubbleAlpha),
                outgoingBubbleEnd = lerp(bubbleBase, Color.Black, 0.18f).copy(alpha = bubbleAlpha),
                incomingBubbleSurface = monoIncoming
            )
            return applyOpacityMultiplier(adapted, uiOpacity, uiColorMode)
        }

        if (preset == WallpaperPreset.DEFAULT) {
            return applyOpacityMultiplier(canonicalBase, uiOpacity, uiColorMode)
        }

        val bgPrimary = Color(preset.primaryOrbHex)
        val bgSecondary = Color(preset.secondaryOrbHex)
        val compPrimary = presetComplementaryAccent(preset, isDark)
        val compSecondary = presetSecondaryComplementaryAccent(preset, isDark)

        val bubbleAlpha = if (isAmoled) 0.82f else 0.88f
        val bubbleStart = lerp(compPrimary, bgSecondary, 0.25f).copy(alpha = bubbleAlpha)
        val bubbleEnd = lerp(compSecondary, bgPrimary, 0.30f).copy(alpha = bubbleAlpha)

        val surfaceBaseAlpha = canonicalBase.glassSurface.alpha
        val elevatedBaseAlpha = canonicalBase.glassSurfaceElevated.alpha

        // Distinct surface depth: Pitch-black obsidian in AMOLED, rich midnight-slate in Dark, smoked quartz in Light
        val tintedSurface = when {
            !isDark -> lerp(Color(0xFFD4DFEE), bgPrimary, 0.22f).copy(alpha = surfaceBaseAlpha)
            isAmoled -> lerp(Color(0xFF070A10), bgPrimary, 0.18f).copy(alpha = surfaceBaseAlpha)
            else -> lerp(Color(0xFF233046), bgPrimary, 0.28f).copy(alpha = surfaceBaseAlpha)
        }
        val tintedElevated = when {
            !isDark -> lerp(Color(0xFFDBE5F3), compPrimary, 0.18f).copy(alpha = elevatedBaseAlpha)
            isAmoled -> lerp(Color(0xFF0E131D), compPrimary, 0.16f).copy(alpha = elevatedBaseAlpha)
            else -> lerp(Color(0xFF2D3E58), compPrimary, 0.24f).copy(alpha = elevatedBaseAlpha)
        }
        val tintedIncoming = when {
            !isDark -> lerp(Color(0xFFD6E1EF), bgPrimary, 0.18f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
            isAmoled -> lerp(Color(0xFF090D14), bgPrimary, 0.20f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
            else -> lerp(Color(0xFF25344B), bgPrimary, 0.28f).copy(alpha = canonicalBase.incomingBubbleSurface.alpha)
        }

        val adaptedBgBase = when {
            !isDark -> lerp(Color(0xFFE3EAF4), bgPrimary, 0.16f)
            isAmoled -> Color.Black
            else -> lerp(Color(0xFF182234), Color(preset.baseColorHex), 0.45f)
        }

        val adapted = canonicalBase.copy(
            backgroundBase = adaptedBgBase,
            orbPrimary = bgPrimary.copy(alpha = if (isAmoled) 0.22f else if (isDark) 0.36f else 0.24f),
            orbSecondary = bgSecondary.copy(alpha = if (isAmoled) 0.18f else if (isDark) 0.30f else 0.20f),
            glassSurface = tintedSurface,
            glassSurfaceElevated = tintedElevated,
            glassBorderTop = compPrimary.copy(alpha = if (isDark) 0.48f else 0.65f),
            glassBorderBottom = bgPrimary.copy(alpha = if (isDark) 0.22f else 0.32f),
            accentPrimary = compPrimary,
            accentSecondary = compSecondary,
            sendButtonColor = compPrimary,
            outgoingBubbleStart = bubbleStart,
            outgoingBubbleEnd = bubbleEnd,
            incomingBubbleSurface = tintedIncoming
        )
        return applyOpacityMultiplier(adapted, uiOpacity, uiColorMode)
    }

    fun colorSchemeForPalette(palette: GlassPalette): ColorScheme {
        val popupSurface = palette.popupContainerColor
        return when {
            palette.isAmoled -> darkColorScheme(
                primary = palette.accentPrimary,
                secondary = palette.accentSecondary,
                tertiary = WisteriaGlass,
                background = palette.backgroundBase,
                surface = popupSurface,
                surfaceVariant = popupSurface,
                surfaceContainer = popupSurface,
                surfaceContainerHigh = popupSurface,
                surfaceContainerHighest = popupSurface,
                onPrimary = Color.Black,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary
            )
            palette.isDark -> darkColorScheme(
                primary = palette.accentPrimary,
                secondary = palette.accentSecondary,
                tertiary = WisteriaGlass,
                background = palette.backgroundBase,
                surface = popupSurface,
                surfaceVariant = popupSurface,
                surfaceContainer = popupSurface,
                surfaceContainerHigh = popupSurface,
                surfaceContainerHighest = popupSurface,
                onPrimary = Color.Black,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary
            )
            else -> lightColorScheme(
                primary = palette.accentPrimary,
                secondary = palette.accentSecondary,
                tertiary = WisteriaGlass,
                background = palette.backgroundBase,
                surface = popupSurface,
                surfaceVariant = popupSurface,
                surfaceContainer = popupSurface,
                surfaceContainerHigh = popupSurface,
                surfaceContainerHighest = popupSurface,
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
    uiOpacity: Float = 0.50f,
    uiColorMode: UiColorMode = UiColorMode.COLORFUL,
    expressiveAnimations: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val basePalette = when (themeMode) {
        TxturaThemeMode.SYSTEM -> if (systemDark) DarkGlassPalette else LightGlassPalette
        TxturaThemeMode.LIGHT -> LightGlassPalette
        TxturaThemeMode.DARK -> DarkGlassPalette
        TxturaThemeMode.AMOLED -> AmoledGlassPalette
    }

    val customPath = wallpaperConfig.customImageUri
    val customDominantColor by produceState(
        initialValue = WallpaperHarmonyEngine.getCachedCustomColor(customPath),
        key1 = customPath
    ) {
        if (customPath.isNullOrBlank()) {
            value = null
        } else {
            val cached = WallpaperHarmonyEngine.getCachedCustomColor(customPath)
            if (cached != null) {
                value = cached
            } else {
                value = withContext(Dispatchers.IO) {
                    try {
                        val file = File(customPath)
                        val decoded = if (file.exists()) {
                            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                            BitmapFactory.decodeFile(file.absolutePath, opts)
                        } else {
                            val uri = customPath.toUri()
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                                BitmapFactory.decodeStream(stream, null, opts)
                            }
                        }
                        decoded?.let { bmp ->
                            WallpaperHarmonyEngine.extractAndCacheDominantColor(customPath, bmp)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    val adaptedPalette = remember(basePalette, wallpaperConfig, customDominantColor, uiOpacity, uiColorMode) {
        WallpaperHarmonyEngine.adaptPalette(
            isDark = basePalette.isDark,
            isAmoled = basePalette.isAmoled,
            wallpaperConfig = wallpaperConfig,
            customDominantColor = customDominantColor,
            uiOpacity = uiOpacity,
            uiColorMode = uiColorMode
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

    val adaptiveMetrics = rememberAdaptiveMetrics()

    CompositionLocalProvider(
        LocalGlassPalette provides adaptedPalette,
        LocalAdaptiveMetrics provides adaptiveMetrics,
        LocalExpressiveAnimations provides expressiveAnimations
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
