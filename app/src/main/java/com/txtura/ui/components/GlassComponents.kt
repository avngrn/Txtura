@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma", "BooleanLiteralArgument", "ClarifyingParentheses")

package com.txtura.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.txtura.data.UiColorMode
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.data.WallpaperTexture
import com.txtura.ui.theme.LocalExpressiveAnimations
import com.txtura.ui.theme.LocalGlassPalette
import com.txtura.ui.theme.TxturaHaptics
import com.txtura.ui.theme.TxturaMotion
import com.txtura.ui.theme.WallpaperHarmonyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ambient background with support for:
 * - Default theme ambient glow orbs + Silk Weave texture
 * - 10 curated artistic glassmorphic presets + 10 monochrome Light/Dark accent presets ([WallpaperPreset])
 * - 21 bespoke procedural tactile textures ([WallpaperTexture]) inspired by the "Txtura" name
 * - Custom user-selected gallery photo ([WallpaperConfig.customImageUri]) with a readability scrim
 * - Automatic complementary UI color adaptation and UI transparency provided via [LocalGlassPalette]
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    wallpaperConfig: WallpaperConfig = WallpaperConfig(),
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = LocalGlassPalette.current.isDark
    val isAmoled = LocalGlassPalette.current.isAmoled
    val uiOpacity = LocalGlassPalette.current.uiOpacity
    val uiColorMode = LocalGlassPalette.current.uiColorMode
    val context = LocalContext.current
    val customPath = wallpaperConfig.customImageUri

    val customLoaded by produceState<Pair<android.graphics.Bitmap, Color>?>(
        initialValue = null,
        key1 = customPath,
    ) {
        value = if (customPath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(customPath)
                    val decoded = if (file.exists()) {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                    } else {
                        val uri = customPath.toUri()
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            BitmapFactory.decodeStream(stream, null, opts)
                        }
                    }
                    decoded?.let { bmp ->
                        val dominant = WallpaperHarmonyEngine.extractAndCacheDominantColor(customPath, bmp)
                        bmp to dominant
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    val customBitmap = customLoaded?.first
    val customDominantColor = customLoaded?.second
        ?: WallpaperHarmonyEngine.getCachedCustomColor(customPath)

    val palette = remember(
        isDark,
        isAmoled,
        wallpaperConfig,
        customDominantColor,
        uiOpacity,
        uiColorMode,
    ) {
        WallpaperHarmonyEngine.adaptPalette(
            isDark = isDark,
            isAmoled = isAmoled,
            wallpaperConfig = wallpaperConfig,
            customDominantColor = customDominantColor,
            uiOpacity = uiOpacity,
            uiColorMode = uiColorMode,
        )
    }

    val adaptedColorScheme = remember(palette) {
        WallpaperHarmonyEngine.colorSchemeForPalette(palette)
    }

    val preset = wallpaperConfig.preset
    val isMinimalist = preset.isMinimalist || (uiColorMode == UiColorMode.MINIMALIST)
    val isMono = (preset.isMonochrome || (uiColorMode == UiColorMode.MONOCHROME)) && !isMinimalist
    val activeTexture = wallpaperConfig.resolvedTexture
    val bgBase = when {
        isMinimalist -> palette.backgroundBase
        customBitmap != null -> palette.backgroundBase
        isMono -> palette.backgroundBase
        preset == WallpaperPreset.DEFAULT -> palette.backgroundBase
        !palette.isDark -> lerp(Color(0xFFE2EAF4), Color(preset.primaryOrbHex), 0.18f)
        palette.isAmoled -> Color.Black
        else -> palette.backgroundBase
    }

    // Background blur and smoked frost:
    // When uiOpacity = 1.00 (100% Providno): minimum blur (0dp), crystal-clear view through glass
    // When uiOpacity = 0.00 (0% Providno): maximum frosted blur (22dp) and dense smoked frost
    val frostSolidity = (1.0f - uiOpacity.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    val frostBlurRadius = (frostSolidity * 22f).dp
    val frostVeilColor = when {
        !palette.isDark -> lerp(Color(0xFF94A3B8), palette.accentPrimary.copy(alpha = 1f), 0.22f)
        palette.isAmoled -> Color.Black
        else -> lerp(Color(0xFF1A2538), palette.orbPrimary.copy(alpha = 1f), 0.30f)
    }
    val frostVeilAlpha = (frostSolidity * 0.32f).coerceIn(0.0f, 0.32f)

    val texPrimary = when {
        customBitmap != null -> palette.accentPrimary.copy(alpha = 0.28f)
        isMono -> palette.accentPrimary.copy(alpha = if (palette.isDark) 0.36f else 0.30f)
        else -> Color(preset.accentOrbHex).copy(alpha = if (palette.isDark) 0.38f else 0.32f)
    }
    val texSecondary = when {
        customBitmap != null -> Color.White.copy(alpha = 0.16f)
        isMono -> (if (palette.isDark) Color.White else Color.Black).copy(
            alpha = if (palette.isDark) 0.18f else 0.14f,
        )
        else -> Color(preset.primaryOrbHex).copy(alpha = if (palette.isDark) 0.32f else 0.26f)
    }
    val texAccent = palette.accentPrimary.copy(alpha = if (palette.isDark) 0.46f else 0.38f)
    val shouldDrawTexture = if (customBitmap != null) {
        (wallpaperConfig.texture != WallpaperTexture.AUTO_THEME) &&
            (wallpaperConfig.texture != WallpaperTexture.NONE)
    } else {
        activeTexture != WallpaperTexture.NONE
    }

    CompositionLocalProvider(LocalGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = adaptedColorScheme,
            typography = MaterialTheme.typography,
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgBase),
            ) {
                // 1. Softly blurred ambient backdrop (gallery image or glowing theme orbs)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(
                            radius = frostBlurRadius,
                            edgeTreatment = BlurredEdgeTreatment.Unbounded,
                        ),
                ) {
                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Subtle smoked readability scrim (never bleached white)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    when {
                                        !palette.isDark -> Color(0xFFCBD5E1).copy(alpha = 0.32f)
                                        palette.isAmoled -> Color.Black.copy(alpha = 0.52f)
                                        else -> Color(0xFF182232).copy(alpha = 0.42f)
                                    },
                                ),
                        )
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (isMinimalist) return@Canvas
                            val w = size.width
                            val h = size.height

                            if ((preset == WallpaperPreset.DEFAULT) || isMono) {
                                val topCenter = Offset(w * 0.85f, h * 0.12f)
                                val topRadius = (w * (if (palette.isAmoled) 0.60f else 0.78f)).coerceAtLeast(1f)
                                val topBrush = Brush.radialGradient(
                                    colors = listOf(palette.orbPrimary, Color.Transparent),
                                    center = topCenter,
                                    radius = topRadius,
                                )
                                val bottomCenter = Offset(w * 0.15f, h * 0.78f)
                                val bottomRadius = (w * (if (palette.isAmoled) 0.56f else 0.74f)).coerceAtLeast(1f)
                                val bottomBrush = Brush.radialGradient(
                                    colors = listOf(palette.orbSecondary, Color.Transparent),
                                    center = bottomCenter,
                                    radius = bottomRadius,
                                )
                                drawCircle(brush = topBrush, radius = topRadius, center = topCenter)
                                drawCircle(brush = bottomBrush, radius = bottomRadius, center = bottomCenter)
                            } else {
                                val c1 = Color(preset.primaryOrbHex).copy(
                                    alpha = if (!palette.isDark) 0.28f else if (palette.isAmoled) 0.26f else 0.42f,
                                )
                                val c2 = Color(preset.secondaryOrbHex).copy(
                                    alpha = if (!palette.isDark) 0.24f else if (palette.isAmoled) 0.22f else 0.38f,
                                )
                                val c3 = Color(preset.accentOrbHex).copy(
                                    alpha = if (!palette.isDark) 0.20f else if (palette.isAmoled) 0.18f else 0.32f,
                                )

                                val radiusScale = if (palette.isAmoled) 0.72f else 0.90f
                                val center1 = Offset(w * 0.82f, h * 0.14f)
                                val radius1 = (w * radiusScale).coerceAtLeast(1f)
                                val brush1 = Brush.radialGradient(
                                    colors = listOf(c1, Color.Transparent),
                                    center = center1,
                                    radius = radius1,
                                )

                                val center2 = Offset(w * 0.15f, h * 0.48f)
                                val radius2 = (w * (radiusScale * 0.92f)).coerceAtLeast(1f)
                                val brush2 = Brush.radialGradient(
                                    colors = listOf(c2, Color.Transparent),
                                    center = center2,
                                    radius = radius2,
                                )

                                val center3 = Offset(w * 0.75f, h * 0.86f)
                                val radius3 = (w * (radiusScale * 0.88f)).coerceAtLeast(1f)
                                val brush3 = Brush.radialGradient(
                                    colors = listOf(c3, Color.Transparent),
                                    center = center3,
                                    radius = radius3,
                                )

                                drawCircle(brush = brush1, radius = radius1, center = center1)
                                drawCircle(brush = brush2, radius = radius2, center = center2)
                                drawCircle(brush = brush3, radius = radius3, center = center3)
                            }
                        }
                    }
                }

                // 2. Smoked frosted veil that deepens smoothly in both directions without bleaching
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(frostVeilColor.copy(alpha = frostVeilAlpha)),
                )

                // 3. Crisp Txtura tactile surface texture layer rendered ABOVE the blur filter so
                // fine weaves, crystals, ribbons, and facets stay sharp and always update live
                if (shouldDrawTexture) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawTxturaTexture(
                            texture = activeTexture,
                            primaryColor = texPrimary,
                            secondaryColor = texSecondary,
                            accentColor = texAccent,
                            isCompactPreview = false,
                        )
                    }
                }

                content()
            }
        }
    }
}

/**
 * Renders the 21 bespoke tactile surface textures for Txtura themes, both in full-screen [GlassBackground]
 * and in compact thumbnail preview swatches.
 */
fun DrawScope.drawTxturaTexture(
    texture: WallpaperTexture,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    isCompactPreview: Boolean = false,
) {
    val w = size.width
    val h = size.height
    if ((w <= 1f) || (h <= 1f)) return

    val strokeBase = if (isCompactPreview) 1.35f else 2.15f
    val stepScale = if (isCompactPreview) 0.22f else 1f

    when (texture) {
        WallpaperTexture.AUTO_THEME,
        WallpaperTexture.SILK_WEAVE,
        -> {
            // Diagonal cross-woven silk threads with gentle specular wave modulation
            val step = (42f * stepScale).coerceAtLeast(6f)
            var d = -h
            var idx = 0
            while (d < (w + h)) {
                val col = if (idx % 3 == 0) accentColor else primaryColor
                drawLine(
                    color = col,
                    start = Offset(d, 0f),
                    end = Offset(d + h * 0.65f, h),
                    strokeWidth = strokeBase,
                )
                drawLine(
                    color = secondaryColor,
                    start = Offset(d + h * 0.65f, 0f),
                    end = Offset(d, h),
                    strokeWidth = strokeBase * 0.85f,
                )
                d += step
                idx++
            }
        }

        WallpaperTexture.AURORA_RIBBONS -> {
            // Flowing multi-layered silk wave contours
            val ribbonCount = if (isCompactPreview) 4 else 11
            val segments = if (isCompactPreview) 10 else 28
            for (r in 0 until ribbonCount) {
                val baseY = h * ((r + 0.5f) / ribbonCount)
                val amp = (if (isCompactPreview) 4f else 26f) * (1f + (r % 3) * 0.35f)
                val phase = r * 0.85f
                val path = Path()
                for (s in 0..segments) {
                    val t = s.toFloat() / segments
                    val x = w * t
                    val y = baseY + sin((t * PI * 2.4 + phase)).toFloat() * amp
                    if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = when (r % 3) {
                        0 -> accentColor
                        1 -> primaryColor
                        else -> secondaryColor
                    },
                    style = Stroke(width = strokeBase * (if (r % 2 == 0) 1.35f else 0.9f), cap = StrokeCap.Round)
                )
            }
        }

        WallpaperTexture.VELVET_NAP -> {
            // Crushed velvet nap micro-strokes + diagonal fold sheen + stardust dots
            val rows = if (isCompactPreview) 4 else 16
            val cols = if (isCompactPreview) 5 else 14
            val cellW = w / cols
            val cellH = h / rows
            for (ry in 0 until rows) {
                for (cx in 0 until cols) {
                    val x = (cx + 0.3f + ((ry * 7 + cx * 3) % 5) * 0.1f) * cellW
                    val y = (ry + 0.25f + ((cx * 5 + ry * 2) % 5) * 0.1f) * cellH
                    val angle = 0.45f + sin((ry + cx) * 0.6f) * 0.35f
                    val len = if (isCompactPreview) 5f else 18f
                    drawLine(
                        color = if ((ry + cx) % 3 == 0) accentColor else primaryColor,
                        start = Offset(x, y),
                        end = Offset(x + cos(angle) * len, y + sin(angle) * len),
                        strokeWidth = strokeBase,
                        cap = StrokeCap.Round
                    )
                    if ((ry * cols + cx) % 5 == 0) {
                        drawCircle(
                            color = accentColor,
                            radius = strokeBase * 1.15f,
                            center = Offset(x + cellW * 0.35f, y + cellH * 0.2f)
                        )
                    }
                }
            }
        }

        WallpaperTexture.OCEAN_CAUSTICS -> {
            // Concentric water ripple rings and aquatic caustic wave crests
            val centers = if (isCompactPreview) {
                arrayOf(Offset(w * 0.3f, h * 0.35f), Offset(w * 0.78f, h * 0.72f))
            } else {
                arrayOf(
                    Offset(w * 0.22f, h * 0.24f),
                    Offset(w * 0.78f, h * 0.38f),
                    Offset(w * 0.32f, h * 0.76f),
                    Offset(w * 0.82f, h * 0.84f)
                )
            }
            val ringCount = if (isCompactPreview) 3 else 5
            val maxR = (if (w > h) w else h) * (if (isCompactPreview) 0.55f else 0.42f)
            centers.forEachIndexed { cIdx, center ->
                for (r in 1..ringCount) {
                    val radius = maxR * (r.toFloat() / ringCount)
                    drawCircle(
                        color = if ((cIdx + r) % 2 == 0) primaryColor else accentColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeBase)
                    )
                }
            }
        }

        WallpaperTexture.SAND_DUNES -> {
            // Wind-sculpted desert dune ridges & sandstone strata
            val duneCount = if (isCompactPreview) 4 else 12
            val segments = if (isCompactPreview) 10 else 26
            for (d in 0 until duneCount) {
                val baseY = h * ((d + 0.6f) / duneCount)
                val amp = if (isCompactPreview) 3.5f else 22f
                val path = Path()
                for (s in 0..segments) {
                    val t = s.toFloat() / segments
                    val x = w * t
                    val wave = sin(t * PI * 1.8 + d * 0.7).toFloat() * amp + (t * amp * 0.6f)
                    val y = baseY - wave
                    if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = if (d % 2 == 0) accentColor else primaryColor,
                    style = Stroke(width = strokeBase * (if (d % 2 == 0) 1.4f else 0.9f), cap = StrokeCap.Round)
                )
            }
        }

        WallpaperTexture.ORGANIC_BOTANICAL -> {
            // Botanical leaf venation & natural woodgrain growth curves
            val stemCount = if (isCompactPreview) 3 else 7
            for (i in 0 until stemCount) {
                val xCenter = w * ((i + 0.5f) / stemCount)
                drawLine(
                    color = primaryColor,
                    start = Offset(xCenter, 0f),
                    end = Offset(xCenter + (if (i % 2 == 0) 14f else -14f) * stepScale, h),
                    strokeWidth = strokeBase * 1.2f
                )
                val veins = if (isCompactPreview) 3 else 9
                val span = w / (stemCount * 1.8f)
                for (v in 1..veins) {
                    val vy = h * (v.toFloat() / (veins + 1))
                    drawLine(
                        color = if (v % 2 == 0) accentColor else secondaryColor,
                        start = Offset(xCenter, vy),
                        end = Offset(xCenter + span, vy - span * 0.55f),
                        strokeWidth = strokeBase * 0.9f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = if (v % 2 == 1) accentColor else secondaryColor,
                        start = Offset(xCenter, vy + span * 0.2f),
                        end = Offset(xCenter - span, vy - span * 0.35f),
                        strokeWidth = strokeBase * 0.9f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        WallpaperTexture.CRYSTAL_FACETS -> {
            // Low-poly geometric crystal facet lattice
            val cols = if (isCompactPreview) 3 else 6
            val rows = if (isCompactPreview) 2 else 9
            val cellW = w / cols
            val cellH = h / rows
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x0 = c * cellW
                    val y0 = r * cellH
                    val x1 = x0 + cellW
                    val y1 = y0 + cellH
                    val xm = x0 + cellW * (if ((r + c) % 2 == 0) 0.38f else 0.62f)
                    val ym = y0 + cellH * (if ((r * 3 + c) % 2 == 0) 0.42f else 0.58f)
                    val col = if ((r + c) % 2 == 0) accentColor else primaryColor
                    drawLine(col, Offset(x0, y0), Offset(xm, ym), strokeBase)
                    drawLine(col, Offset(x1, y0), Offset(xm, ym), strokeBase)
                    drawLine(secondaryColor, Offset(x0, y1), Offset(xm, ym), strokeBase * 0.9f)
                    drawLine(col, Offset(x1, y1), Offset(xm, ym), strokeBase)
                    drawLine(secondaryColor, Offset(x0, y0), Offset(x1, y0), strokeBase * 0.75f)
                    drawLine(secondaryColor, Offset(x0, y0), Offset(x0, y1), strokeBase * 0.75f)
                }
            }
        }

        WallpaperTexture.CONSTELLATION_MESH -> {
            // Starfield nodes connected by delicate constellation filaments
            val pts = if (isCompactPreview) {
                arrayOf(
                    Offset(w * 0.16f, h * 0.25f),
                    Offset(w * 0.48f, h * 0.18f),
                    Offset(w * 0.82f, h * 0.32f),
                    Offset(w * 0.28f, h * 0.72f),
                    Offset(w * 0.66f, h * 0.68f),
                    Offset(w * 0.88f, h * 0.82f)
                )
            } else {
                arrayOf(
                    Offset(w * 0.14f, h * 0.11f),
                    Offset(w * 0.38f, h * 0.08f),
                    Offset(w * 0.68f, h * 0.14f),
                    Offset(w * 0.88f, h * 0.24f),
                    Offset(w * 0.22f, h * 0.31f),
                    Offset(w * 0.54f, h * 0.29f),
                    Offset(w * 0.79f, h * 0.44f),
                    Offset(w * 0.15f, h * 0.54f),
                    Offset(w * 0.42f, h * 0.52f),
                    Offset(w * 0.66f, h * 0.62f),
                    Offset(w * 0.26f, h * 0.76f),
                    Offset(w * 0.52f, h * 0.81f),
                    Offset(w * 0.85f, h * 0.79f),
                    Offset(w * 0.36f, h * 0.92f)
                )
            }
            for (i in 0 until pts.size - 1) {
                drawLine(
                    color = primaryColor,
                    start = pts[i],
                    end = pts[i + 1],
                    strokeWidth = strokeBase * 0.85f
                )
                if (i + 3 < pts.size && i % 2 == 0) {
                    drawLine(
                        color = secondaryColor,
                        start = pts[i],
                        end = pts[i + 3],
                        strokeWidth = strokeBase * 0.7f
                    )
                }
            }
            pts.forEachIndexed { idx, p ->
                drawCircle(
                    color = if (idx % 2 == 0) accentColor else primaryColor,
                    radius = strokeBase * (if (idx % 3 == 0) 2.2f else 1.4f),
                    center = p
                )
            }
        }

        WallpaperTexture.DAMASK_JACQUARD -> {
            // Woven diamond jacquard trellis with inner diamond ornament
            val step = (54f * stepScale).coerceAtLeast(9f)
            var d = -h
            while (d < (w + h)) {
                drawLine(primaryColor, Offset(d, 0f), Offset(d + h, h), strokeBase)
                drawLine(primaryColor, Offset(d + h, 0f), Offset(d, h), strokeBase)
                d += step
            }
            val cols = (w / step).toInt().coerceAtLeast(1)
            val rows = (h / step).toInt().coerceAtLeast(1)
            val rSize = step * 0.22f
            for (ry in 0..rows) {
                for (cx in 0..cols) {
                    val cxPos = (cx + 0.5f) * step
                    val cyPos = (ry + 0.5f) * step
                    if (cxPos <= w && cyPos <= h) {
                        val p = Path().apply {
                            moveTo(cxPos, cyPos - rSize)
                            lineTo(cxPos + rSize, cyPos)
                            lineTo(cxPos, cyPos + rSize)
                            lineTo(cxPos - rSize, cyPos)
                            close()
                        }
                        drawPath(p, accentColor, style = Stroke(width = strokeBase))
                    }
                }
            }
        }

        WallpaperTexture.FROST_CRYSTALS -> {
            // Hexagonal 6-pointed ice crystal stars and glacial frost needles
            val cols = if (isCompactPreview) 2 else 4
            val rows = if (isCompactPreview) 2 else 6
            val cellW = w / cols
            val cellH = h / rows
            val armLen = (if (cellW < cellH) cellW else cellH) * 0.28f
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val center = Offset(
                        x = (c + if (r % 2 == 0) 0.38f else 0.62f) * cellW,
                        y = (r + 0.5f) * cellH
                    )
                    for (a in 0 until 6) {
                        val rad = (a * PI / 3.0).toFloat()
                        val end = Offset(center.x + cos(rad) * armLen, center.y + sin(rad) * armLen)
                        drawLine(
                            color = if ((r + c) % 2 == 0) accentColor else primaryColor,
                            start = center,
                            end = end,
                            strokeWidth = strokeBase,
                            cap = StrokeCap.Round
                        )
                        val mid = Offset(center.x + cos(rad) * (armLen * 0.6f), center.y + sin(rad) * (armLen * 0.6f))
                        val branchLen = armLen * 0.32f
                        val b1 = rad + 0.55f
                        val b2 = rad - 0.55f
                        drawLine(
                            color = secondaryColor,
                            start = mid,
                            end = Offset(mid.x + cos(b1) * branchLen, mid.y + sin(b1) * branchLen),
                            strokeWidth = strokeBase * 0.8f
                        )
                        drawLine(
                            color = secondaryColor,
                            start = mid,
                            end = Offset(mid.x + cos(b2) * branchLen, mid.y + sin(b2) * branchLen),
                            strokeWidth = strokeBase * 0.8f
                        )
                    }
                }
            }
        }

        WallpaperTexture.KINTSUGI_MARBLE -> {
            // Natural stone fracture veins with golden/accent kintsugi seams
            val veinCount = if (isCompactPreview) 3 else 7
            val steps = if (isCompactPreview) 6 else 14
            for (v in 0 until veinCount) {
                val path = Path()
                var curX = w * ((v + 0.2f) / veinCount)
                var curY = 0f
                path.moveTo(curX, curY)
                for (s in 1..steps) {
                    curY = h * (s.toFloat() / steps)
                    val shift = sin(s * 1.4f + v * 2.1f) * (if (isCompactPreview) 6f else 34f)
                    curX = (curX + shift).coerceIn(0f, w)
                    path.lineTo(curX, curY)
                }
                drawPath(
                    path = path,
                    color = if (v % 2 == 0) accentColor else primaryColor,
                    style = Stroke(
                        width = strokeBase * (if (v % 2 == 0) 1.55f else 0.95f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        WallpaperTexture.SLATE_STRATA -> {
            // Layered cleft slate stone horizontal/angled strata
            val layers = if (isCompactPreview) 5 else 18
            val segs = if (isCompactPreview) 5 else 12
            for (l in 0 until layers) {
                val baseY = h * ((l + 0.5f) / layers)
                val path = Path()
                for (s in 0..segs) {
                    val x = w * (s.toFloat() / segs)
                    val stepOffset = ((l * 5 + s * 3) % 4 - 1.5f) * (if (isCompactPreview) 1.5f else 6f)
                    val y = baseY + stepOffset
                    if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = if (l % 3 == 0) accentColor else primaryColor,
                    style = Stroke(width = strokeBase)
                )
            }
        }

        WallpaperTexture.BRUSHED_METAL -> {
            // Fine horizontal/diagonal anodized hairline brushed titanium strokes
            val lineCount = if (isCompactPreview) 10 else 44
            for (i in 0 until lineCount) {
                val y = h * (i.toFloat() / lineCount)
                val xStart = ((i * 37) % 10) * (w * 0.04f)
                val xEnd = w - ((i * 23) % 10) * (w * 0.04f)
                drawLine(
                    color = when (i % 4) {
                        0 -> accentColor
                        1, 2 -> primaryColor
                        else -> secondaryColor
                    },
                    start = Offset(xStart, y),
                    end = Offset(xEnd, y + (if (isCompactPreview) 1f else 4f)),
                    strokeWidth = strokeBase * (if (i % 3 == 0) 1.15f else 0.75f),
                    cap = StrokeCap.Round
                )
            }
        }

        WallpaperTexture.LINEN_CANVAS -> {
            // Tactile warp-and-weft woven linen fabric grid
            val step = (24f * stepScale).coerceAtLeast(5.5f)
            var x = step * 0.5f
            var ix = 0
            while (x < w) {
                drawLine(
                    color = if (ix % 4 == 0) accentColor else primaryColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = strokeBase * 0.85f
                )
                x += step
                ix++
            }
            var y = step * 0.5f
            var iy = 0
            while (y < h) {
                drawLine(
                    color = if (iy % 4 == 0) accentColor else secondaryColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = strokeBase * 0.85f
                )
                y += step
                iy++
            }
        }

        WallpaperTexture.FLUTED_GLASS -> {
            // Architectural vertical reeded/fluted glass columns
            val fluteWidth = (28f * stepScale).coerceAtLeast(6f)
            var x = 0f
            var idx = 0
            while (x < w) {
                drawLine(
                    color = if (idx % 2 == 0) accentColor else primaryColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = strokeBase * 1.25f
                )
                drawLine(
                    color = secondaryColor,
                    start = Offset(x + fluteWidth * 0.45f, 0f),
                    end = Offset(x + fluteWidth * 0.45f, h),
                    strokeWidth = strokeBase * 0.65f
                )
                x += fluteWidth
                idx++
            }
        }

        WallpaperTexture.SUEDE_MICROFIBER -> {
            // Cross-brushed suede leather micro-fibers
            val rows = if (isCompactPreview) 5 else 20
            val cols = if (isCompactPreview) 5 else 14
            val cellW = w / cols
            val cellH = h / rows
            val strokeLen = if (isCompactPreview) 4.5f else 15f
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val cx = (c + 0.5f) * cellW
                    val cy = (r + 0.5f) * cellH
                    val dir = if ((r + c) % 2 == 0) 0.55f else -0.55f
                    drawLine(
                        color = if ((r * 3 + c) % 4 == 0) accentColor else primaryColor,
                        start = Offset(cx - cos(dir) * strokeLen * 0.5f, cy - sin(dir) * strokeLen * 0.5f),
                        end = Offset(cx + cos(dir) * strokeLen * 0.5f, cy + sin(dir) * strokeLen * 0.5f),
                        strokeWidth = strokeBase,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        WallpaperTexture.CARBON_TWILL -> {
            // 2x2 diagonal carbon-fiber twill weave blocks
            val cell = (26f * stepScale).coerceAtLeast(6f)
            val cols = (w / cell).toInt() + 1
            val rows = (h / cell).toInt() + 1
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = c * cell
                    val y = r * cell
                    val isDiagonalStep = ((c + r) % 2 == 0)
                    val col = if ((c + r) % 4 == 0) accentColor else if (isDiagonalStep) primaryColor else secondaryColor
                    if (isDiagonalStep) {
                        drawLine(col, Offset(x, y + cell), Offset(x + cell, y), strokeBase * 1.2f)
                        drawLine(col, Offset(x, y + cell * 0.5f), Offset(x + cell * 0.5f, y), strokeBase * 0.9f)
                    } else {
                        drawLine(col, Offset(x, y), Offset(x + cell, y + cell), strokeBase * 1.1f)
                    }
                }
            }
        }

        WallpaperTexture.COTTON_PAPER -> {
            // Handmade cotton rag paper fibers and stippled grain
            val count = if (isCompactPreview) 18 else 95
            val len = if (isCompactPreview) 4f else 14f
            for (i in 0 until count) {
                val px = ((i * 73 + 19) % 97) / 97f * w
                val py = ((i * 41 + 11) % 89) / 89f * h
                val ang = (i * 1.17f)
                drawLine(
                    color = if (i % 3 == 0) accentColor else primaryColor,
                    start = Offset(px, py),
                    end = Offset(px + cos(ang) * len, py + sin(ang) * len),
                    strokeWidth = strokeBase * 0.9f,
                    cap = StrokeCap.Round
                )
                if (i % 2 == 0) {
                    drawCircle(
                        color = secondaryColor,
                        radius = strokeBase * 0.9f,
                        center = Offset((px + len).coerceAtMost(w), py)
                    )
                }
            }
        }

        WallpaperTexture.HERRINGBONE_TWILL -> {
            // Tailored V-shaped chevron herringbone weave
            val colW = (36f * stepScale).coerceAtLeast(8f)
            val rowH = (22f * stepScale).coerceAtLeast(5f)
            val cols = (w / colW).toInt() + 1
            val rows = (h / rowH).toInt() + 1
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x0 = c * colW
                    val y0 = r * rowH
                    val xm = x0 + colW * 0.5f
                    val x1 = x0 + colW
                    val col = if ((r + c) % 3 == 0) accentColor else primaryColor
                    drawLine(col, Offset(x0, y0 + rowH * 0.65f), Offset(xm, y0), strokeBase)
                    drawLine(
                        if (r % 2 == 0) col else secondaryColor,
                        Offset(xm, y0),
                        Offset(x1, y0 + rowH * 0.65f),
                        strokeBase
                    )
                }
            }
        }

        WallpaperTexture.TOPOGRAPHIC_CONTOUR -> {
            // Nested organic topographic elevation isolines
            val rings = if (isCompactPreview) 4 else 11
            val segs = if (isCompactPreview) 16 else 36
            val cx = w * 0.52f
            val cy = h * 0.48f
            val maxRx = w * 0.62f
            val maxRy = h * 0.58f
            for (r in 1..rings) {
                val frac = r.toFloat() / rings
                val path = Path()
                for (s in 0..segs) {
                    val theta = (s.toFloat() / segs) * (PI * 2.0).toFloat()
                    val wobble = 1f + 0.14f * sin(theta * 3f + r * 0.6f) + 0.08f * cos(theta * 5f - r * 0.4f)
                    val x = cx + cos(theta) * maxRx * frac * wobble
                    val y = cy + sin(theta) * maxRy * frac * wobble
                    if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path = path,
                    color = if (r % 3 == 0) accentColor else primaryColor,
                    style = Stroke(width = strokeBase * (if (r % 3 == 0) 1.3f else 0.9f))
                )
            }
        }

        WallpaperTexture.PEBBLED_LEATHER -> {
            // Organic pebbled leather grain cells
            val cell = (34f * stepScale).coerceAtLeast(7.5f)
            val cols = (w / cell).toInt() + 1
            val rows = (h / cell).toInt() + 1
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val shiftX = if (r % 2 == 0) 0f else cell * 0.48f
                    val cx = c * cell + shiftX + cell * 0.5f
                    val cy = r * cell + cell * 0.5f
                    val rx = cell * (0.38f + ((r * 3 + c * 5) % 3) * 0.05f)
                    val ry = cell * (0.34f + ((r * 7 + c * 2) % 3) * 0.05f)
                    drawOval(
                        color = if ((r + c) % 3 == 0) accentColor else primaryColor,
                        topLeft = Offset(cx - rx, cy - ry),
                        size = Size(rx * 2f, ry * 2f),
                        style = Stroke(width = strokeBase * 0.95f)
                    )
                }
            }
        }

        WallpaperTexture.NONE -> Unit
    }
}

/**
 * Glassmorphic container with translucent multi-stop fill and specular gradient border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevated: Boolean = false,
    accentHighlight: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val baseSurface = if (elevated) {
        LocalGlassPalette.current.glassSurfaceElevated
    } else {
        LocalGlassPalette.current.glassSurface
    }
    val topBorder = accentHighlight?.copy(alpha = 0.55f) ?: LocalGlassPalette.current.glassBorderTop
    val bottomBorder = LocalGlassPalette.current.glassBorderBottom

    val surfaceBrush = remember(baseSurface) {
        Brush.linearGradient(
            colors = listOf(
                baseSurface,
                baseSurface.copy(alpha = (baseSurface.alpha * 0.72f).coerceIn(0.04f, 0.98f)),
            ),
            start = Offset.Zero,
            end = Offset(600f, 600f),
        )
    }

    val borderBrush = remember(topBorder, bottomBorder) {
        Brush.linearGradient(
            colors = listOf(
                topBorder,
                bottomBorder,
            ),
            start = Offset.Zero,
            end = Offset(500f, 500f),
        )
    }

    val isClickable = (onClick != null) || (onLongClick != null)
    val resolvedSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by resolvedSource.collectIsPressedAsState()
    val isExpressive = LocalExpressiveAnimations.current

    val pressScale by animateFloatAsState(
        targetValue = if (isClickable && isPressed) 0.984f else 1.0f,
        animationSpec = if (isExpressive) {
            TxturaMotion.pressSpec(isPressed)
        } else {
            tween(durationMillis = 80)
        },
        label = "glassCardPressScale",
    )

    val clickableModifier = when {
        (onClick != null) && (onLongClick != null) -> {
            Modifier.combinedClickable(
                interactionSource = resolvedSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        }
        onLongClick != null -> {
            Modifier.combinedClickable(
                interactionSource = resolvedSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = {},
                onLongClick = onLongClick,
            )
        }
        onClick != null -> {
            Modifier.clickable(
                interactionSource = resolvedSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick,
            )
        }
        else -> Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                if (isClickable) {
                    scaleX = pressScale
                    scaleY = pressScale
                }
            }
            .clip(shape)
            .background(brush = surfaceBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    tint: Color = LocalGlassPalette.current.textPrimary,
    accentHighlight: Color? = null,
    size: Dp = 42.dp,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isExpressive = LocalExpressiveAnimations.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1.0f,
        animationSpec = if (isExpressive) {
            TxturaMotion.pressSpec(isPressed)
        } else {
            tween(durationMillis = 80)
        },
        label = "glassIconButtonScale",
    )

    GlassCard(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            },
        shape = CircleShape,
        elevated = true,
        accentHighlight = accentHighlight,
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource,
        onClick = {
            TxturaHaptics.click(view, context)
            onClick()
        },
        onLongClick = onLongClick?.let { action ->
            {
                TxturaHaptics.longPress(view, context)
                action()
            }
        },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.Center),
        )
    }
}

/**
 * Subtle, modern glassmorphic slider with a slim capsule groove, soft gradient fill,
 * subtle micro-tick dots, and a refined circular glass-ring thumb that follows the active Txtura palette.
 */
@Composable
fun GlassModernSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    tickFractions: List<Float> = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
) {
    val palette = LocalGlassPalette.current
    val view = LocalView.current
    val context = LocalContext.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val normalized = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    var lastHapticStep by remember { mutableIntStateOf(-1) }
    var isDragging by remember { mutableStateOf(value = false) }
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.18f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "glassSliderThumbScale",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val pad = 14.dp.toPx()
                    val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                    val frac = ((offset.x - pad) / usable).coerceIn(0f, 1f)
                    val step = (frac * 20f).toInt()
                    if (step != lastHapticStep) {
                        lastHapticStep = step
                        TxturaHaptics.tick(view, context)
                    }
                    currentOnValueChange(valueRange.start + frac * span)
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val pad = 14.dp.toPx()
                        val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                        val frac = ((offset.x - pad) / usable).coerceIn(0f, 1f)
                        val step = (frac * 20f).toInt()
                        if (step != lastHapticStep) {
                            lastHapticStep = step
                            TxturaHaptics.tick(view, context)
                        }
                        currentOnValueChange(valueRange.start + frac * span)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                ) { change, _ ->
                    change.consume()
                    val pad = 14.dp.toPx()
                    val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                    val frac = ((change.position.x - pad) / usable).coerceIn(0f, 1f)
                    val step = (frac * 20f).toInt()
                    if (step != lastHapticStep) {
                        lastHapticStep = step
                        TxturaHaptics.tick(view, context)
                    }
                    currentOnValueChange(valueRange.start + frac * span)
                }
            },
    ) {
        val trackStart = 14.dp.toPx()
        val trackEnd = (size.width - trackStart).coerceAtLeast(trackStart + 1f)
        val trackWidth = trackEnd - trackStart
        val centerY = size.height * 0.5f
        val trackHeight = 5.dp.toPx()
        val trackRadius = CornerRadius(trackHeight * 0.5f, trackHeight * 0.5f)
        val thumbX = trackStart + trackWidth * normalized

        // 1. Subtle recessed glass groove (inactive track)
        val grooveColor = if (palette.isDark) {
            palette.backgroundBase.copy(alpha = 0.58f)
        } else {
            Color(0xFF94A3B8).copy(alpha = 0.28f)
        }
        drawRoundRect(
            color = grooveColor,
            topLeft = Offset(trackStart, centerY - trackHeight * 0.5f),
            size = Size(trackWidth, trackHeight),
            cornerRadius = trackRadius,
        )
        drawRoundRect(
            color = palette.glassBorderBottom.copy(alpha = 0.45f),
            topLeft = Offset(trackStart, centerY - trackHeight * 0.5f),
            size = Size(trackWidth, trackHeight),
            cornerRadius = trackRadius,
            style = Stroke(width = 0.8.dp.toPx()),
        )

        // 2. Active gradient fill + soft ambient halo
        val activeWidth = (thumbX - trackStart).coerceAtLeast(trackHeight)
        val haloHeight = 9.dp.toPx()
        drawRoundRect(
            color = palette.accentPrimary.copy(alpha = if (isDragging) 0.22f else 0.13f),
            topLeft = Offset(trackStart, centerY - haloHeight * 0.5f),
            size = Size(activeWidth, haloHeight),
            cornerRadius = CornerRadius(haloHeight * 0.5f, haloHeight * 0.5f),
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    palette.accentSecondary.copy(alpha = 0.65f),
                    palette.accentPrimary.copy(alpha = 0.95f),
                ),
                startX = trackStart,
                endX = thumbX.coerceAtLeast(trackStart + 1f),
            ),
            topLeft = Offset(trackStart, centerY - trackHeight * 0.5f),
            size = Size(activeWidth, trackHeight),
            cornerRadius = trackRadius,
        )

        // 3. Subtle micro-tick markers
        val dotRadius = 1.3.dp.toPx()
        tickFractions.forEach { tick ->
            val tx = trackStart + trackWidth * tick.coerceIn(0f, 1f)
            val isCovered = tx <= thumbX
            drawCircle(
                color = if (isCovered) {
                    Color.White.copy(alpha = 0.70f)
                } else {
                    palette.textSecondary.copy(alpha = 0.35f)
                },
                radius = dotRadius,
                center = Offset(tx, centerY),
            )
        }

        // 4. Sleek glass-ring thumb
        val thumbRadius = 7.5.dp.toPx() * thumbScale
        val thumbCenter = Offset(thumbX, centerY)

        // Outer soft glow
        drawCircle(
            color = palette.accentPrimary.copy(alpha = if (isDragging) 0.30f else 0.18f),
            radius = thumbRadius * 1.48f,
            center = thumbCenter,
        )
        // Frosted inner core
        drawCircle(
            color = palette.popupContainerColor,
            radius = thumbRadius,
            center = thumbCenter,
        )
        // Specular accent ring
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(palette.accentPrimary, palette.accentSecondary),
                start = Offset(thumbX - thumbRadius, centerY - thumbRadius),
                end = Offset(thumbX + thumbRadius, centerY + thumbRadius),
            ),
            radius = thumbRadius,
            center = thumbCenter,
            style = Stroke(width = 1.8.dp.toPx()),
        )
        // Luminous center point
        drawCircle(
            color = palette.accentPrimary,
            radius = 2.2.dp.toPx() * thumbScale,
            center = thumbCenter,
        )
    }
}

/**
 * Theme-aware glassmorphic popup AlertDialog that automatically adapts its surface,
 * specular border, and typography colors to the active [LocalGlassPalette].
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    accentBorderColor: Color? = null,
) {
    val palette = LocalGlassPalette.current
    val dialogShape = RoundedCornerShape(26.dp)
    val topBorder = accentBorderColor?.copy(alpha = 0.65f) ?: palette.glassBorderTop
    val borderBrush = remember(topBorder, palette.glassBorderBottom) {
        Brush.linearGradient(
            colors = listOf(topBorder, palette.glassBorderBottom),
            start = Offset.Zero,
            end = Offset(700f, 700f),
        )
    }

    val baseSurface = palette.popupContainerColor
    val surfaceBrush = remember(baseSurface) {
        Brush.linearGradient(
            colors = listOf(
                baseSurface,
                baseSurface.copy(alpha = (baseSurface.alpha * 0.82f).coerceIn(0.04f, 0.98f)),
            ),
            start = Offset.Zero,
            end = Offset(600f, 600f),
        )
    }

    val isExpressive = LocalExpressiveAnimations.current
    var isDialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isDialogVisible = true }
    val dialogScale by animateFloatAsState(
        targetValue = if (isDialogVisible) 1.0f else 0.93f,
        animationSpec = if (isExpressive) {
            TxturaMotion.dialogSpec()
        } else {
            tween(durationMillis = 100)
        },
        label = "glassDialogScale",
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = dialogShape,
        containerColor = Color.Transparent,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        tonalElevation = 0.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = dialogScale
                scaleY = dialogScale
            }
            .clip(dialogShape)
            .background(brush = surfaceBrush, shape = dialogShape)
            .border(
                width = 1.2.dp,
                brush = borderBrush,
                shape = dialogShape,
            ),
    )
}

/**
 * Circular glass icon button that displays a popup tooltip message when long-pressed (held)
 * and triggers [onClick] when tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTooltipIconButton(
    icon: ImageVector,
    tooltipText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalGlassPalette.current.accentPrimary,
    pressedTint: Color = tint,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    enableTooltip: Boolean = true,
) {
    val palette = LocalGlassPalette.current
    val glassSurfaceElevated = palette.glassSurfaceElevated
    val glassBorderTop = palette.glassBorderTop
    val glassBorderBottom = palette.glassBorderBottom
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val activeTint = if (isPressed) pressedTint else tint

    @Suppress("DEPRECATION")
    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = {
            val tooltipShape = RoundedCornerShape(12.dp)
            PlainTooltip(
                containerColor = palette.popupContainerColor,
                contentColor = palette.textPrimary,
                shape = tooltipShape,
                modifier = Modifier.border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(glassBorderTop, glassBorderBottom)),
                    shape = tooltipShape
                )
            ) {
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        },
        state = tooltipState,
    ) {
        val surfaceBrush = remember(glassSurfaceElevated) {
            Brush.linearGradient(
                colors = listOf(
                    glassSurfaceElevated,
                    glassSurfaceElevated.copy(alpha = (glassSurfaceElevated.alpha * 0.72f).coerceIn(0.05f, 0.95f)),
                ),
            )
        }
        val borderBrush = remember(glassBorderTop, glassBorderBottom) {
            Brush.linearGradient(
                colors = listOf(glassBorderTop, glassBorderBottom),
            )
        }

        val view = LocalView.current
        val context = LocalContext.current
        val isExpressive = LocalExpressiveAnimations.current
        val buttonScale by animateFloatAsState(
            targetValue = if (isPressed) 0.91f else 1.0f,
            animationSpec = if (isExpressive) {
                TxturaMotion.pressSpec(isPressed)
            } else {
                tween(durationMillis = 80)
            },
            label = "glassTooltipButtonScale",
        )

        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .clip(CircleShape)
                .background(brush = surfaceBrush, shape = CircleShape)
                .border(width = 1.dp, brush = borderBrush, shape = CircleShape)
                .then(
                    if (enableTooltip) {
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = {
                                TxturaHaptics.click(view, context)
                                onClick()
                            },
                            onLongClick = {
                                TxturaHaptics.tick(view, context)
                                scope.launch { tooltipState.show() }
                            },
                        )
                    } else {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.foundation.LocalIndication.current,
                            onClick = {
                                TxturaHaptics.click(view, context)
                                onClick()
                            }
                        )
                    }
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltipText,
                tint = activeTint,
                modifier = Modifier
                    .size(iconSize)
                    .align(Alignment.Center),
            )
        }
    }
}

/**
 * Text component that automatically detects if its content did not fit in the available space
 * (`hasVisualOverflow`) and displays the full text in a themed glass popup when the user holds
 * their touch (long-presses) on the truncated text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverflowTooltipText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = 1,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val palette = LocalGlassPalette.current
    var isOverflowing by remember(text) { mutableStateOf(value = false) }
    val tooltipState = rememberTooltipState()

    @Suppress("DEPRECATION")
    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()

    TooltipBox(
        positionProvider = positionProvider,
        enableUserInput = isOverflowing,
        tooltip = {
            if (isOverflowing) {
                val tooltipShape = RoundedCornerShape(14.dp)
                PlainTooltip(
                    containerColor = palette.popupContainerColor,
                    contentColor = palette.textPrimary,
                    shape = tooltipShape,
                    modifier = Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(palette.glassBorderTop, palette.glassBorderBottom)
                        ),
                        shape = tooltipShape,
                    ),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        },
        state = tooltipState,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            style = style,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow,
            onTextLayout = { layoutResult ->
                isOverflowing = layoutResult.hasVisualOverflow
            },
        )
    }
}

/**
 * Lightweight, zero-dependency vector icons tailored for Txtura SMS/MMS UI.
 */
object TxturaIcons {

    val Search: ImageVector by lazy {
        ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(11f, 19f)
                arcTo(
                    horizontalEllipseRadius = 8f,
                    verticalEllipseRadius = 8f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    x1 = 11f,
                    y1 = 3f,
                )
                arcTo(
                    horizontalEllipseRadius = 8f,
                    verticalEllipseRadius = 8f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    x1 = 11f,
                    y1 = 19f,
                )
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }.build()
    }

    val Send: ImageVector by lazy {
        ImageVector.Builder("Send", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(22f, 2f)
                lineTo(11f, 13f)
                moveTo(22f, 2f)
                lineTo(15f, 22f)
                lineTo(11f, 13f)
                lineTo(2f, 9f)
                close()
            }
        }.build()
    }

    val Forward: ImageVector by lazy {
        ImageVector.Builder("Forward", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 14f)
                lineTo(20f, 9f)
                lineTo(15f, 4f)
                moveTo(4f, 20f)
                lineTo(4f, 13f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8f, 9f)
                lineTo(20f, 9f)
            }
        }.build()
    }

    val HighlightStar: ImageVector by lazy {
        ImageVector.Builder("HighlightStar", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 2f)
                lineTo(15.09f, 8.26f)
                lineTo(22f, 9.27f)
                lineTo(17f, 14.14f)
                lineTo(18.18f, 21.02f)
                lineTo(12f, 17.77f)
                lineTo(5.82f, 21.02f)
                lineTo(7f, 14.14f)
                lineTo(2f, 9.27f)
                lineTo(8.91f, 8.26f)
                close()
            }
        }.build()
    }

    val Block: ImageVector by lazy {
        ImageVector.Builder("Block", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 21f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 21f)
                moveTo(5.7f, 5.7f)
                lineTo(18.3f, 18.3f)
            }
        }.build()
    }

    val SelectAll: ImageVector by lazy {
        ImageVector.Builder("SelectAll", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 4f)
                lineTo(18f, 4f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 6f)
                lineTo(20f, 17f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 19f)
                lineTo(7f, 19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 17f)
                lineTo(5f, 6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 4f)
                close()
                moveTo(9f, 11.5f)
                lineTo(11.5f, 14f)
                lineTo(16f, 9.5f)
                moveTo(3f, 9f)
                lineTo(3f, 19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5f, 21f)
                lineTo(15f, 21f)
            }
        }.build()
    }

    val DeselectAll: ImageVector by lazy {
        ImageVector.Builder("DeselectAll", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 4f)
                lineTo(18f, 4f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 6f)
                lineTo(20f, 17f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 19f)
                lineTo(7f, 19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 17f)
                lineTo(5f, 6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 4f)
                close()
                moveTo(3f, 9f)
                lineTo(3f, 19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 5f, 21f)
                lineTo(15f, 21f)
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder("Check", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20f, 6f)
                lineTo(9f, 17f)
                lineTo(4f, 12f)
            }
        }.build()
    }

    val AttachImage: ImageVector by lazy {
        ImageVector.Builder("AttachImage", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 3f)
                lineTo(19f, 3f)
                arcTo(
                    horizontalEllipseRadius = 2f,
                    verticalEllipseRadius = 2f,
                    theta = 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 21f,
                    y1 = 5f,
                )
                lineTo(21f, 19f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19f, y1 = 21f)
                lineTo(5f, 21f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3f, y1 = 19f)
                lineTo(3f, 5f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5f, y1 = 3f)
                close()
                moveTo(8.5f, 10f)
                arcTo(horizontalEllipseRadius = 1.5f, verticalEllipseRadius = 1.5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 8.5f, y1 = 7f)
                arcTo(horizontalEllipseRadius = 1.5f, verticalEllipseRadius = 1.5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 8.5f, y1 = 10f)
                moveTo(21f, 15f)
                lineTo(16f, 10f)
                lineTo(5f, 21f)
            }
        }.build()
    }

    val ShieldCheck: ImageVector by lazy {
        ImageVector.Builder("ShieldCheck", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 22f)
                curveTo(12f, 22f, 20f, 18f, 20f, 12f)
                lineTo(20f, 5f)
                lineTo(12f, 2f)
                lineTo(4f, 5f)
                lineTo(4f, 12f)
                curveTo(4f, 18f, 12f, 22f, 12f, 22f)
                close()
                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15f, 10f)
            }
        }.build()
    }

    val Warning: ImageVector by lazy {
        ImageVector.Builder("Warning", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.29f, 3.86f)
                lineTo(1.82f, 18f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 3.54f, y1 = 21f)
                lineTo(20.46f, 21f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 22.18f, y1 = 18f)
                lineTo(13.71f, 3.86f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 10.29f, y1 = 3.86f)
                close()
                moveTo(12f, 9f)
                lineTo(12f, 13f)
                moveTo(12f, 17f)
                lineTo(12.01f, 17f)
            }
        }.build()
    }

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder("ArrowBack", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19f, 12f)
                lineTo(5f, 12f)
                moveTo(12f, 19f)
                lineTo(5f, 12f)
                lineTo(12f, 5f)
            }
        }.build()
    }

    val Plus: ImageVector by lazy {
        ImageVector.Builder("Plus", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder("Close", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }.build()
    }

    val CheckDouble: ImageVector by lazy {
        ImageVector.Builder("CheckDouble", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 6f)
                lineTo(7f, 17f)
                lineTo(2f, 12f)
                moveTo(22f, 10f)
                lineTo(14.5f, 17.5f)
                lineTo(13f, 16f)
            }
        }.build()
    }

    val SparklesTheme: ImageVector
        get() = ImageVector.Builder("SparklesTheme", 24.dp, 24.dp, 24f, 24f).apply {
            // Artist color palette outer contour with thumb cutout
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 2.5f)
                curveTo(6.75f, 2.5f, 2.5f, 6.75f, 2.5f, 12f)
                curveTo(2.5f, 17.25f, 6.75f, 21.5f, 12f, 21.5f)
                curveTo(13.65f, 21.5f, 14.9f, 20.3f, 14.9f, 18.7f)
                curveTo(14.9f, 17.95f, 14.6f, 17.3f, 14.15f, 16.8f)
                curveTo(13.7f, 16.3f, 13.45f, 15.7f, 13.45f, 15.0f)
                curveTo(13.45f, 13.6f, 14.6f, 12.5f, 16.0f, 12.5f)
                horizontalLineTo(17.6f)
                curveTo(19.75f, 12.5f, 21.5f, 10.75f, 21.5f, 8.6f)
                curveTo(21.5f, 5.1f, 17.25f, 2.5f, 12f, 2.5f)
                close()
            }
            // Four paint color wells on the palette
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
            ) {
                // Dot 1 (bottom-left)
                moveTo(6.6f, 11.8f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6.6f, 14.3f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6.6f, 11.8f)
                close()
                // Dot 2 (top-left)
                moveTo(8.3f, 7.1f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8.3f, 9.6f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8.3f, 7.1f)
                close()
                // Dot 3 (top-center)
                moveTo(12.8f, 5.7f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12.8f, 8.2f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12.8f, 5.7f)
                close()
                // Dot 4 (top-right)
                moveTo(16.9f, 7.6f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16.9f, 10.1f)
                arcTo(1.25f, 1.25f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16.9f, 7.6f)
                close()
            }
        }.build()

    val CreditCard: ImageVector by lazy {
        ImageVector.Builder("CreditCard", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 6f)
                lineTo(21f, 6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 23f, 8f)
                lineTo(23f, 18f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 20f)
                lineTo(3f, 20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 18f)
                lineTo(1f, 8f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 6f)
                close()
                moveTo(1f, 10f)
                lineTo(23f, 10f)
                moveTo(5f, 15f)
                lineTo(9f, 15f)
            }
        }.build()
    }

    val CopyKey: ImageVector by lazy {
        ImageVector.Builder("CopyKey", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 9f)
                lineTo(20f, 9f)
                lineTo(20f, 20f)
                lineTo(9f, 20f)
                close()
                moveTo(5f, 15f)
                lineTo(4f, 15f)
                lineTo(4f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 5f)
            }
        }.build()
    }

    val PinOutline: ImageVector by lazy {
        ImageVector.Builder("PinOutline", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 17f)
                lineTo(12f, 22f)
                moveTo(5f, 17f)
                lineTo(19f, 17f)
                lineTo(17f, 11f)
                lineTo(17f, 5f)
                lineTo(7f, 5f)
                lineTo(7f, 11f)
                close()
            }
        }.build()
    }

    val Pin: ImageVector get() = PinOutline

    val PinFilled: ImageVector by lazy {
        ImageVector.Builder("PinFilled", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.White),
            ) {
                moveTo(7f, 4f)
                lineTo(17f, 4f)
                lineTo(17f, 11f)
                lineTo(19f, 16f)
                lineTo(5f, 16f)
                lineTo(7f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 16f)
                lineTo(12f, 22f)
            }
        }.build()
    }

    val PinOff: ImageVector by lazy {
        ImageVector.Builder("PinOff", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 17f)
                lineTo(12f, 22f)
                moveTo(5f, 17f)
                lineTo(19f, 17f)
                lineTo(17f, 11f)
                lineTo(17f, 5f)
                lineTo(7f, 5f)
                lineTo(7f, 11f)
                close()
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }.build()
    }

    val Delete: ImageVector by lazy {
        ImageVector.Builder("Delete", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 6f)
                lineTo(21f, 6f)
                moveTo(19f, 6f)
                lineTo(19f, 20f)
                lineTo(5f, 20f)
                lineTo(5f, 6f)
                moveTo(8f, 6f)
                lineTo(8f, 4f)
                lineTo(16f, 4f)
                lineTo(16f, 6f)
            }
        }.build()
    }

    /**
     * Classic Gear / Cogwheel (Zupčanik) icon for Settings.
     */
    val SettingsGear: ImageVector by lazy {
        ImageVector.Builder("SettingsGear", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Inner hub circle
                moveTo(12f, 15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 9f)
                arcTo(horizontalEllipseRadius = 3f, verticalEllipseRadius = 3f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 15f)
                // Outer ring & 8 gear teeth rays
                moveTo(12f, 18.5f)
                arcTo(horizontalEllipseRadius = 6.5f, verticalEllipseRadius = 6.5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 5.5f)
                arcTo(horizontalEllipseRadius = 6.5f, verticalEllipseRadius = 6.5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 18.5f)
                // Top & bottom teeth
                moveTo(12f, 2f)
                lineTo(12f, 5.2f)
                moveTo(12f, 18.8f)
                lineTo(12f, 22f)
                // Left & right teeth
                moveTo(2f, 12f)
                lineTo(5.2f, 12f)
                moveTo(18.8f, 12f)
                lineTo(22f, 12f)
                // 4 diagonal teeth
                moveTo(4.93f, 4.93f)
                lineTo(7.2f, 7.2f)
                moveTo(16.8f, 16.8f)
                lineTo(19.07f, 19.07f)
                moveTo(19.07f, 4.93f)
                lineTo(16.8f, 7.2f)
                moveTo(7.2f, 16.8f)
                lineTo(4.93f, 19.07f)
            }
        }.build()
    }

    /**
     * Phone handset icon for direct calling via the default Dialer app.
     */
    val PhoneCall: ImageVector by lazy {
        ImageVector.Builder("PhoneCall", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(22f, 16.92f)
                lineTo(22f, 19.92f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19.82f, 21.92f)
                curveTo(10.6f, 21.09f, 2.91f, 13.4f, 2.08f, 4.18f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 4.08f, y1 = 2f)
                lineTo(7.08f, 2f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 9.08f, y1 = 3.72f)
                lineTo(9.8f, 7.2f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 9.25f, y1 = 9.01f)
                lineTo(7.55f, 10.71f)
                curveTo(9.0f, 13.3f, 10.7f, 15.0f, 13.29f, 16.45f)
                lineTo(14.99f, 14.75f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 16.8f, y1 = 14.2f)
                lineTo(20.28f, 14.92f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 22f, y1 = 16.92f)
                close()
            }
        }.build()
    }

    /**
     * Contact / Person icon for opening or picking contact details in the default Contacts app.
     */
    val ContactPerson: ImageVector by lazy {
        ImageVector.Builder("ContactPerson", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20f, 21f)
                lineTo(20f, 19f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 16f, y1 = 15f)
                lineTo(8f, 15f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 4f, y1 = 19f)
                lineTo(4f, 21f)
                moveTo(12f, 11f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 3f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 11f)
            }
        }.build()
    }

    /**
     * Multiple Contacts / Users icon for multi-recipient broadcast sending.
     */
    val MultipleContacts: ImageVector by lazy {
        ImageVector.Builder("MultipleContacts", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Primary person
                moveTo(17f, 21f)
                lineTo(17f, 19f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 13f, y1 = 15f)
                lineTo(7f, 15f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 3f, y1 = 19f)
                lineTo(3f, 21f)
                moveTo(10f, 11f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 10f, y1 = 3f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 10f, y1 = 11f)
                // Secondary person behind
                moveTo(21f, 21f)
                lineTo(21f, 19f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 18f, y1 = 15.5f)
                moveTo(16f, 3.13f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 16f, y1 = 10.87f)
            }
        }.build()
    }

    /**
     * Restore / Undo arrow icon for restoring messages from Deleted Messages (Trash).
     */
    val Restore: ImageVector by lazy {
        ImageVector.Builder("Restore", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 7f)
                lineTo(3f, 13f)
                lineTo(9f, 13f)
                moveTo(3.5f, 13f)
                arcTo(horizontalEllipseRadius = 8.5f, verticalEllipseRadius = 8.5f, theta = 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 6f, y1 = 18.5f)
            }
        }.build()
    }

    /**
     * Horizontal left-right swipe arrows icon for Swipe Gesture settings.
     */
    val SwipeHoriz: ImageVector by lazy {
        ImageVector.Builder("SwipeHoriz", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Right arrow
                moveTo(4f, 8f)
                lineTo(20f, 8f)
                moveTo(16f, 4f)
                lineTo(20f, 8f)
                lineTo(16f, 12f)
                // Left arrow
                moveTo(20f, 16f)
                lineTo(4f, 16f)
                moveTo(8f, 12f)
                lineTo(4f, 16f)
                lineTo(8f, 20f)
            }
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder("ChevronRight", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 6f)
                lineTo(15f, 12f)
                lineTo(9f, 18f)
            }
        }.build()
    }

    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder("ChevronDown", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }.build()
    }

    val CloudDownload: ImageVector by lazy {
        ImageVector.Builder("CloudDownload", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 17f)
                lineTo(12f, 21f)
                lineTo(16f, 17f)
                moveTo(12f, 12f)
                lineTo(12f, 21f)
                moveTo(20.39f, 18.39f)
                arcTo(horizontalEllipseRadius = 5f, verticalEllipseRadius = 5f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 18f, y1 = 9f)
                arcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 5f, y1 = 13f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6f, y1 = 21f)
            }
        }.build()
    }

    val FolderBackup: ImageVector by lazy {
        ImageVector.Builder("FolderBackup", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 20f)
                lineTo(20f, 20f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 22f, y1 = 18f)
                lineTo(22f, 8f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 20f, y1 = 6f)
                lineTo(11.5f, 6f)
                lineTo(9.5f, 4f)
                lineTo(4f, 4f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 2f, y1 = 6f)
                lineTo(2f, 18f)
                arcTo(horizontalEllipseRadius = 2f, verticalEllipseRadius = 2f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 4f, y1 = 20f)
                close()
                moveTo(12f, 11f)
                lineTo(12f, 16f)
                moveTo(9.5f, 13.5f)
                lineTo(12f, 11f)
                lineTo(14.5f, 13.5f)
            }
        }.build()
    }

    /**
     * Vertical 3-dots (More Options) icon for overflow dropdown menu in chat header,
     * geometrically centered at x = 12f, y = 5f / 12f / 19f inside the 24x24 viewport.
     */
    val MoreVert: ImageVector by lazy {
        ImageVector.Builder("MoreVert", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
            ) {
                val r = 1.65f
                for (cy in listOf(5.5f, 12f, 18.5f)) {
                    moveTo(12f, cy - r)
                    arcTo(
                        horizontalEllipseRadius = r,
                        verticalEllipseRadius = r,
                        theta = 0f,
                        isMoreThanHalf = true,
                        isPositiveArc = true,
                        x1 = 12f,
                        y1 = cy + r,
                    )
                    arcTo(
                        horizontalEllipseRadius = r,
                        verticalEllipseRadius = r,
                        theta = 0f,
                        isMoreThanHalf = true,
                        isPositiveArc = true,
                        x1 = 12f,
                        y1 = cy - r,
                    )
                    close()
                }
            }
        }.build()
    }

    /**
     * Circular clock/timer icon used for scheduling and countdown timers.
     */
    val ClockTimer: ImageVector by lazy {
        ImageVector.Builder("ClockTimer", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                arcTo(
                    horizontalEllipseRadius = 9f,
                    verticalEllipseRadius = 9f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    x1 = 12f,
                    y1 = 21f,
                )
                arcTo(
                    horizontalEllipseRadius = 9f,
                    verticalEllipseRadius = 9f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    x1 = 12f,
                    y1 = 3f,
                )
                close()
                moveTo(12f, 7.5f)
                lineTo(12f, 12f)
                lineTo(15.2f, 14.2f)
            }
        }.build()
    }

    val PermissionsKey: ImageVector by lazy {
        ImageVector.Builder("PermissionsKey", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Circular key bow at (7.5, 15.5) with radius 4.5
                moveTo(7.5f, 11f)
                arcTo(
                    horizontalEllipseRadius = 4.5f,
                    verticalEllipseRadius = 4.5f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    x1 = 7.5f,
                    y1 = 20f,
                )
                arcTo(
                    horizontalEllipseRadius = 4.5f,
                    verticalEllipseRadius = 4.5f,
                    theta = 0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    x1 = 7.5f,
                    y1 = 11f,
                )
                close()
                // Key shaft from bow edge to top-right
                moveTo(10.68f, 12.32f)
                lineTo(21f, 2f)
                // Key teeth
                moveTo(18f, 5f)
                lineTo(20.5f, 7.5f)
                moveTo(15f, 8f)
                lineTo(17.5f, 10.5f)
            }
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder("Info", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 21f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 21f)
                close()
                moveTo(12f, 8f)
                lineTo(12f, 8.2f)
                moveTo(12f, 11.5f)
                lineTo(12f, 16.5f)
            }
        }.build()
    }

    val Mail: ImageVector by lazy {
        ImageVector.Builder("Mail", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Envelope outline
                moveTo(3f, 6.5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 4.5f)
                lineTo(19f, 4.5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 6.5f)
                lineTo(21f, 17.5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 19.5f)
                lineTo(5f, 19.5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 17.5f)
                close()
                // Envelope flap
                moveTo(3f, 6.5f)
                lineTo(12f, 13f)
                lineTo(21f, 6.5f)
            }
        }.build()
    }

    val ArrowUp: ImageVector by lazy {
        ImageVector.Builder("ArrowUp", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 19f)
                lineTo(12f, 5f)
                moveTo(5f, 12f)
                lineTo(12f, 5f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    val ArrowDown: ImageVector by lazy {
        ImageVector.Builder("ArrowDown", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(12f, 19f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    val ChatBubble: ImageVector by lazy {
        ImageVector.Builder("ChatBubble", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 11.5f)
                arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = false, 5.5f, 17f)
                lineTo(3f, 21f)
                lineTo(7f, 18.5f)
                arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 21f, 11.5f)
                close()
            }
        }.build()
    }

    val EyePreview: ImageVector by lazy {
        ImageVector.Builder("EyePreview", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(1f, 12f)
                arcTo(11f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 23f, 12f)
                arcTo(11f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 12f)
                close()
                moveTo(12f, 15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 9f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 15f)
                close()
            }
        }.build()
    }

    val Link: ImageVector by lazy {
        ImageVector.Builder("Link", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 13f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 17f, 13f)
                lineTo(20f, 10f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 13f, 3f)
                lineTo(11f, 5f)
                moveTo(14f, 11f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 11f)
                lineTo(4f, 14f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 11f, 21f)
                lineTo(13f, 19f)
            }
        }.build()
    }

    val Microphone: ImageVector by lazy {
        ImageVector.Builder("Microphone", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Mic capsule
                moveTo(9f, 6f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 6f)
                lineTo(15f, 11f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 11f)
                close()
                // Mic cradle
                moveTo(5f, 10f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19f, 10f)
                // Stem and base
                moveTo(12f, 17f)
                lineTo(12f, 21f)
                moveTo(8f, 21f)
                lineTo(16f, 21f)
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder("Play", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 5.5f)
                lineTo(19f, 12f)
                lineTo(8f, 18.5f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder("Pause", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 6f)
                lineTo(8f, 18f)
                moveTo(16f, 6f)
                lineTo(16f, 18f)
            }
        }.build()
    }

    val CalendarEvent: ImageVector by lazy {
        ImageVector.Builder("CalendarEvent", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 6f)
                lineTo(21f, 18f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 20f)
                lineTo(5f, 20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 18f)
                lineTo(3f, 6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 4f)
                close()
                moveTo(8f, 2f)
                lineTo(8f, 5f)
                moveTo(16f, 2f)
                lineTo(16f, 5f)
                moveTo(3f, 9f)
                lineTo(21f, 9f)
                moveTo(8f, 14f)
                lineTo(11f, 16.5f)
                lineTo(16f, 12f)
            }
        }.build()
    }

    val DocumentFile: ImageVector by lazy {
        ImageVector.Builder("DocumentFile", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 2f)
                lineTo(6f, 2f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, 4f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 22f)
                lineTo(18f, 22f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 20f, 20f)
                lineTo(20f, 8f)
                lineTo(14f, 2f)
                close()
                moveTo(14f, 2f)
                lineTo(14f, 8f)
                lineTo(20f, 8f)
                moveTo(8f, 13f)
                lineTo(16f, 13f)
                moveTo(8f, 17f)
                lineTo(13f, 17f)
            }
        }.build()
    }

    val NotificationBell: ImageVector by lazy {
        ImageVector.Builder("NotificationBell", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 8f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 8f)
                curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                lineTo(21f, 17f)
                curveTo(21f, 17f, 18f, 15f, 18f, 8f)
                close()
                moveTo(13.73f, 21f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 10.27f, 21f)
            }
        }.build()
    }

    val WallpaperLandscape: ImageVector by lazy {
        ImageVector.Builder("WallpaperLandscape", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 5f)
                lineTo(21f, 5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 23f, 7f)
                lineTo(23f, 17f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 19f)
                lineTo(3f, 19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 17f)
                lineTo(1f, 7f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 5f)
                close()
                moveTo(6.5f, 9.5f)
                arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6.5f, 6.5f)
                arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6.5f, 9.5f)
                moveTo(1f, 16f)
                lineTo(7f, 11f)
                lineTo(13f, 16f)
                moveTo(11f, 14.5f)
                lineTo(15f, 11f)
                lineTo(23f, 17f)
            }
        }.build()
    }

    val CellularNetwork: ImageVector by lazy {
        ImageVector.Builder("CellularNetwork", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 18f)
                lineTo(4f, 20f)
                moveTo(9f, 14f)
                lineTo(9f, 20f)
                moveTo(14f, 10f)
                lineTo(14f, 20f)
                moveTo(19f, 6f)
                lineTo(19f, 20f)
            }
        }.build()
    }

    val BusinessBuilding: ImageVector by lazy {
        ImageVector.Builder("BusinessBuilding", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 21f)
                lineTo(20f, 21f)
                moveTo(6f, 21f)
                lineTo(6f, 5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8f, 3f)
                lineTo(16f, 3f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 5f)
                lineTo(18f, 21f)
                moveTo(9f, 7f)
                lineTo(11f, 7f)
                moveTo(13f, 7f)
                lineTo(15f, 7f)
                moveTo(9f, 11f)
                lineTo(11f, 11f)
                moveTo(13f, 11f)
                lineTo(15f, 11f)
                moveTo(9f, 15f)
                lineTo(11f, 15f)
                moveTo(13f, 15f)
                lineTo(15f, 15f)
            }
        }.build()
    }

    val SecurityLock: ImageVector by lazy {
        ImageVector.Builder("SecurityLock", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 11f)
                lineTo(19f, 11f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 13f)
                lineTo(21f, 20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 22f)
                lineTo(5f, 22f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 20f)
                lineTo(3f, 13f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 11f)
                close()
                moveTo(7f, 11f)
                lineTo(7f, 7f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 7f)
                lineTo(17f, 11f)
                moveTo(12f, 15f)
                lineTo(12f, 18f)
            }
        }.build()
    }
}



