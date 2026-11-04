package com.txtura.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.txtura.data.UiColorMode
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.ui.theme.LocalGlassPalette
import com.txtura.ui.theme.WallpaperHarmonyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Ambient background with support for:
 * - Default theme ambient glow orbs
 * - 10 curated artistic glassmorphic presets + 10 monochrome Light/Dark accent presets ([WallpaperPreset])
 * - Custom user-selected gallery photo ([WallpaperConfig.customImageUri]) with a readability scrim
 * - Automatic complementary UI color adaptation and UI transparency provided via [LocalGlassPalette]
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    wallpaperConfig: WallpaperConfig = WallpaperConfig(),
    content: @Composable BoxScope.() -> Unit,
) {
    val currentPalette = LocalGlassPalette.current
    val isDark = currentPalette.isDark
    val isAmoled = currentPalette.isAmoled
    val uiOpacity = currentPalette.uiOpacity
    val uiColorMode = currentPalette.uiColorMode
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
        uiColorMode
    ) {
        WallpaperHarmonyEngine.adaptPalette(
            isDark = isDark,
            isAmoled = isAmoled,
            wallpaperConfig = wallpaperConfig,
            customDominantColor = customDominantColor,
            uiOpacity = uiOpacity,
            uiColorMode = uiColorMode
        )
    }

    val adaptedColorScheme = remember(palette) {
        WallpaperHarmonyEngine.colorSchemeForPalette(palette)
    }

    val preset = wallpaperConfig.preset
    val isMono = preset.isMonochrome || uiColorMode == UiColorMode.MONOCHROME
    val bgBase = when {
        customBitmap != null -> palette.backgroundBase
        isMono -> palette.backgroundBase
        preset == WallpaperPreset.DEFAULT -> palette.backgroundBase
        !palette.isDark -> Color(preset.primaryOrbHex).copy(alpha = 0.14f)
        else -> Color(preset.baseColorHex)
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
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Subtle readability scrim so glass cards and text remain crisp over any photo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (palette.isDark) Color.Black.copy(alpha = 0.45f)
                                else Color.White.copy(alpha = 0.35f),
                            ),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val w = size.width
                                val h = size.height
                                if (preset == WallpaperPreset.DEFAULT || isMono) {
                                    val topCenter = Offset(w * 0.85f, h * 0.12f)
                                    val topRadius = (w * 0.72f).coerceAtLeast(1f)
                                    val topBrush = Brush.radialGradient(
                                        colors = listOf(palette.orbPrimary, Color.Transparent),
                                        center = topCenter,
                                        radius = topRadius,
                                    )
                                    val bottomCenter = Offset(w * 0.15f, h * 0.78f)
                                    val bottomRadius = (w * 0.68f).coerceAtLeast(1f)
                                    val bottomBrush = Brush.radialGradient(
                                        colors = listOf(palette.orbSecondary, Color.Transparent),
                                        center = bottomCenter,
                                        radius = bottomRadius,
                                    )
                                    onDrawBehind {
                                        drawCircle(brush = topBrush, radius = topRadius, center = topCenter)
                                        drawCircle(brush = bottomBrush, radius = bottomRadius, center = bottomCenter)
                                    }
                                } else {
                                    val c1 = Color(preset.primaryOrbHex).copy(alpha = if (palette.isDark) 0.34f else 0.26f)
                                    val c2 = Color(preset.secondaryOrbHex).copy(alpha = if (palette.isDark) 0.30f else 0.22f)
                                    val c3 = Color(preset.accentOrbHex).copy(alpha = if (palette.isDark) 0.24f else 0.18f)

                                    val center1 = Offset(w * 0.82f, h * 0.14f)
                                    val radius1 = (w * 0.85f).coerceAtLeast(1f)
                                    val brush1 = Brush.radialGradient(
                                        colors = listOf(c1, Color.Transparent),
                                        center = center1,
                                        radius = radius1,
                                    )

                                    val center2 = Offset(w * 0.15f, h * 0.48f)
                                    val radius2 = (w * 0.78f).coerceAtLeast(1f)
                                    val brush2 = Brush.radialGradient(
                                        colors = listOf(c2, Color.Transparent),
                                        center = center2,
                                        radius = radius2,
                                    )

                                    val center3 = Offset(w * 0.75f, h * 0.86f)
                                    val radius3 = (w * 0.75f).coerceAtLeast(1f)
                                    val brush3 = Brush.radialGradient(
                                        colors = listOf(c3, Color.Transparent),
                                        center = center3,
                                        radius = radius3,
                                    )

                                    val hasStars = (preset == WallpaperPreset.STARLIGHT_GALAXY) ||
                                        (preset == WallpaperPreset.VELVET_PLUM)
                                    val starPositions = if (hasStars) {
                                        arrayOf(
                                            Offset(w * 0.18f, h * 0.12f),
                                            Offset(w * 0.64f, h * 0.09f),
                                            Offset(w * 0.88f, h * 0.27f),
                                            Offset(w * 0.32f, h * 0.34f),
                                            Offset(w * 0.12f, h * 0.66f),
                                            Offset(w * 0.77f, h * 0.58f),
                                            Offset(w * 0.45f, h * 0.81f),
                                            Offset(w * 0.89f, h * 0.91f),
                                        )
                                    } else {
                                        emptyArray()
                                    }

                                    onDrawBehind {
                                        drawCircle(brush = brush1, radius = radius1, center = center1)
                                        drawCircle(brush = brush2, radius = radius2, center = center2)
                                        drawCircle(brush = brush3, radius = radius3, center = center3)
                                        starPositions.forEachIndexed { idx, pos ->
                                            drawCircle(
                                                color = Color.White.copy(alpha = if ((idx % 2) == 0) 0.35f else 0.18f),
                                                radius = if ((idx % 3) == 0) 4.5f else 2.8f,
                                                center = pos,
                                            )
                                        }
                                    }
                                }
                            },
                    )
                }
                content()
            }
        }
    }
}

/**
 * Glassmorphic container with translucent multi-stop fill and specular gradient border.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevated: Boolean = false,
    accentHighlight: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalGlassPalette.current
    val baseSurface = if (elevated) palette.glassSurfaceElevated else palette.glassSurface
    val topBorder = accentHighlight?.copy(alpha = 0.55f) ?: palette.glassBorderTop

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

    val borderBrush = remember(topBorder, palette.glassBorderBottom) {
        Brush.linearGradient(
            colors = listOf(
                topBorder,
                palette.glassBorderBottom,
            ),
            start = Offset.Zero,
            end = Offset(500f, 500f),
        )
    }

    val clickableModifier = when {
        onClick != null && onLongClick != null -> Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
        onLongClick != null -> Modifier.combinedClickable(
            onClick = {},
            onLongClick = onLongClick
        )
        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }

    Box(
        modifier = modifier
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
    tint: Color = LocalGlassPalette.current.textPrimary,
    accentHighlight: Color? = null,
    size: Dp = 42.dp,
) {
    GlassCard(
        modifier = modifier.size(size),
        shape = CircleShape,
        elevated = true,
        accentHighlight = accentHighlight,
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
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
    size: Dp = 44.dp,
) {
    val palette = LocalGlassPalette.current
    val isDark = palette.isDark
    val glassSurfaceElevated = palette.glassSurfaceElevated
    val glassBorderTop = palette.glassBorderTop
    val glassBorderBottom = palette.glassBorderBottom
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    @Suppress("DEPRECATION")
    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = {
            PlainTooltip(
                containerColor = if (isDark) Color(0xEE1E293B) else Color(0xEE0F172A),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.labelMedium,
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

        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(brush = surfaceBrush, shape = CircleShape)
                .border(width = 1.dp, brush = borderBrush, shape = CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        scope.launch { tooltipState.show() }
                    },
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltipText,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.Center),
            )
        }
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

    val SparklesTheme: ImageVector by lazy {
        ImageVector.Builder("SparklesTheme", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 3f)
                lineTo(14.5f, 9.5f)
                lineTo(21f, 12f)
                lineTo(14.5f, 14.5f)
                lineTo(12f, 21f)
                lineTo(9.5f, 14.5f)
                lineTo(3f, 12f)
                lineTo(9.5f, 9.5f)
                close()
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

    val Pin: ImageVector by lazy {
        ImageVector.Builder("Pin", 24.dp, 24.dp, 24f, 24f).apply {
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

    val CloudUpload: ImageVector by lazy {
        ImageVector.Builder("CloudUpload", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16f, 16f)
                lineTo(12f, 12f)
                lineTo(8f, 16f)
                moveTo(12f, 12f)
                lineTo(12f, 21f)
                moveTo(20.39f, 18.39f)
                arcTo(horizontalEllipseRadius = 5f, verticalEllipseRadius = 5f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 18f, y1 = 9f)
                arcTo(horizontalEllipseRadius = 7f, verticalEllipseRadius = 7f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 5f, y1 = 13f)
                arcTo(horizontalEllipseRadius = 4f, verticalEllipseRadius = 4f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6f, y1 = 21f)
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

    val CloudSync: ImageVector by lazy {
        ImageVector.Builder("CloudSync", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19.35f, 10.04f)
                arcTo(horizontalEllipseRadius = 7.5f, verticalEllipseRadius = 7.5f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 5f, y1 = 12f)
                arcTo(horizontalEllipseRadius = 6f, verticalEllipseRadius = 6f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6f, y1 = 20f)
                lineTo(19f, 20f)
                arcTo(horizontalEllipseRadius = 5f, verticalEllipseRadius = 5f, theta = 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 19.35f, y1 = 10.04f)
                close()
            }
        }.build()
    }
}
