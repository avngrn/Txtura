package com.txtura.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive layout metrics dynamically calculated from the device's physical screen
 * resolution, aspect ratio, and pixel density.
 *
 * Ensures all UI elements, margins, and bottom bars scale harmoniously and remain
 * positioned at the optimal visual height on physical phones (including devices
 * where navigation bar / gesture hints are hidden) and emulators alike.
 */
@Immutable
data class AdaptiveMetrics(
    val scaleFactor: Float,
    val widthScale: Float,
    val heightScale: Float,
    val densityMultiplier: Float,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isCompactWidth: Boolean,
    val isCompactHeight: Boolean,
    val isTabletOrFoldable: Boolean,
    // Bottom Dock Bar & Action Bar Dimensions
    val bottomDockHeight: Dp,
    val centerActionSize: Dp,
    val centerActionIconSize: Dp,
    val dockIconButtonSize: Dp,
    val dockIconInnerSize: Dp,
    val dockCornerRadius: Dp,
    // Adaptive Safe Bottom Clearance (guarantees floating look on physical phones)
    val safeBottomClearance: Dp,
    // General Layout Dimensions
    val topHeaderHeight: Dp,
    val searchBarHeight: Dp,
    val cardPaddingHorizontal: Dp,
    val cardPaddingVertical: Dp,
)

val LocalAdaptiveMetrics = staticCompositionLocalOf {
    AdaptiveMetrics(
        scaleFactor = 1.0f,
        widthScale = 1.0f,
        heightScale = 1.0f,
        densityMultiplier = 2.75f,
        screenWidthDp = 392.dp,
        screenHeightDp = 840.dp,
        isCompactWidth = false,
        isCompactHeight = false,
        isTabletOrFoldable = false,
        bottomDockHeight = 64.dp,
        centerActionSize = 52.dp,
        centerActionIconSize = 28.dp,
        dockIconButtonSize = 46.dp,
        dockIconInnerSize = 24.dp,
        dockCornerRadius = 26.dp,
        safeBottomClearance = 20.dp,
        topHeaderHeight = 44.dp,
        searchBarHeight = 48.dp,
        cardPaddingHorizontal = 14.dp,
        cardPaddingVertical = 10.dp,
    )
}

@Composable
fun rememberAdaptiveMetrics(): AdaptiveMetrics {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val navBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    val gesturesPadding = WindowInsets.systemGestures.asPaddingValues().calculateBottomPadding()
    val imePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val widthDp = configuration.screenWidthDp.toFloat()
    val heightDp = configuration.screenHeightDp.toFloat()

    return remember(
        widthDp,
        heightDp,
        configuration.densityDpi,
        density.density,
        navBarsPadding,
        safeDrawingPadding,
        gesturesPadding,
        imePadding
    ) {
        // Baseline design target: 392dp width x 840dp height (~1080x2310 @ 2.75x or 411x891 @ 2.625x)
        val widthScale = (widthDp / 392f).coerceIn(0.88f, 1.22f)
        val heightScale = (heightDp / 840f).coerceIn(0.88f, 1.20f)
        val scale = ((widthScale * 0.65f) + (heightScale * 0.35f)).coerceIn(0.88f, 1.20f)

        val effectiveNav = maxOf(navBarsPadding, safeDrawingPadding)

        // On physical phones where the user has hidden the gesture bar/indicator (or OEM returns 0 inset),
        // effectiveNav is 0dp or < 12dp. On emulators and devices with navigation buttons or visible gesture pills,
        // effectiveNav is 24dp - 48dp.
        // We ensure a minimum physical clearance of ~20dp - 24dp so the bottom dock bar NEVER sits
        // flat against the bottom glass bezel and looks identical in elevation to the simulator.
        val safeBottomClearance = when {
            imePadding > 0.dp -> 4.dp
            effectiveNav < 12.dp -> {
                val gestureLift = gesturesPadding.value.coerceIn(16f, 24f)
                (gestureLift * heightScale).dp + (6f * heightScale).dp
            }
            else -> (6f * heightScale).dp
        }

        AdaptiveMetrics(
            scaleFactor = scale,
            widthScale = widthScale,
            heightScale = heightScale,
            densityMultiplier = density.density,
            screenWidthDp = widthDp.dp,
            screenHeightDp = heightDp.dp,
            isCompactWidth = widthDp < 360f,
            isCompactHeight = heightDp < 700f,
            isTabletOrFoldable = widthDp >= 600f,
            bottomDockHeight = (64f * scale).coerceIn(58f, 72f).dp,
            centerActionSize = (52f * scale).coerceIn(46f, 58f).dp,
            centerActionIconSize = (28f * scale).coerceIn(24f, 32f).dp,
            dockIconButtonSize = (46f * scale).coerceIn(40f, 50f).dp,
            dockIconInnerSize = (24f * scale).coerceIn(20f, 26f).dp,
            dockCornerRadius = (26f * scale).coerceIn(22f, 30f).dp,
            safeBottomClearance = safeBottomClearance,
            topHeaderHeight = (44f * scale).coerceIn(40f, 50f).dp,
            searchBarHeight = (48f * scale).coerceIn(42f, 54f).dp,
            cardPaddingHorizontal = (14f * scale).coerceIn(10f, 18f).dp,
            cardPaddingVertical = (10f * scale).coerceIn(8f, 14f).dp
        )
    }
}
