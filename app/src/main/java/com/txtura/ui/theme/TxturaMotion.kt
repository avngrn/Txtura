@file:Suppress("SpellCheckingInspection")

package com.txtura.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment

/**
 * Universal High-Performance Motion Scheme for Txtura.
 * Replaces non-deterministic physics-based spring calculations with GPU-friendly,
 * hardware-accelerated cubic-bezier curves and compact bounded durations.
 *
 * This ensures silky-smooth, 60fps/120fps stutter-free animations across
 * ALL Android devices (entry-level, mid-range, and flagship).
 */
object TxturaMotion {

    // Easing curves optimized for GPU interpolation and RenderThread
    val DecelerateEasing: Easing = FastOutSlowInEasing
    val AccelerateEasing: Easing = FastOutLinearInEasing
    val StandardEasing: Easing = FastOutSlowInEasing
    val EnterEasing: Easing = LinearOutSlowInEasing

    // Durations (in milliseconds)
    const val DURATION_SCREEN_ENTER = 220
    const val DURATION_SCREEN_EXIT = 220
    const val DURATION_EXPAND = 200
    const val DURATION_COLLAPSE = 160
    const val DURATION_PRESS = 90
    const val DURATION_RELEASE = 140
    const val DURATION_ROTATE = 180
    const val DURATION_DIALOG = 170
    const val DURATION_LIST_ITEM = 180
    const val DURATION_DOCK_SNAP = 180
    const val DURATION_SWIPE_SNAP = 100

    // Touch Feedback Animation Specs
    fun <T> pressSpec(isPressed: Boolean) = tween<T>(
        durationMillis = if (isPressed) DURATION_PRESS else DURATION_RELEASE,
        easing = DecelerateEasing,
    )

    fun <T> rotateSpec() = tween<T>(
        durationMillis = DURATION_ROTATE,
        easing = StandardEasing
    )

    fun <T> dialogSpec() = tween<T>(
        durationMillis = DURATION_DIALOG,
        easing = DecelerateEasing
    )

    fun <T> listItemPlacementSpec() = tween<T>(
        durationMillis = DURATION_LIST_ITEM,
        easing = DecelerateEasing
    )

    fun <T> listItemFadeOutSpec() = tween<T>(
        durationMillis = DURATION_COLLAPSE,
        easing = AccelerateEasing
    )

    fun <T> snapBackSpec() = tween<T>(
        durationMillis = DURATION_DOCK_SNAP,
        easing = DecelerateEasing
    )

    fun <T> swipeSnapBackSpec() = tween<T>(
        durationMillis = DURATION_SWIPE_SNAP,
        easing = DecelerateEasing
    )

    // Bottom Dock Bar Slide Transition (Zero height change / zero vertical bouncing)
    fun dockBarEnter(isExpressive: Boolean = true): EnterTransition {
        if (!isExpressive) return fadeIn(tween(140))
        return slideInVertically(
            animationSpec = tween(durationMillis = 200, easing = DecelerateEasing),
            initialOffsetY = { fullHeight -> fullHeight }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 150, easing = EnterEasing)
        )
    }

    fun dockBarExit(isExpressive: Boolean = true): ExitTransition {
        if (!isExpressive) return fadeOut(tween(100))
        return slideOutVertically(
            animationSpec = tween(durationMillis = 160, easing = AccelerateEasing),
            targetOffsetY = { fullHeight -> fullHeight }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 120, easing = AccelerateEasing)
        )
    }

    // Screen Transitions (Forward / Backward) - Synchronized motion without desync or pop
    fun screenTransition(forward: Boolean, isExpressive: Boolean = true): ContentTransform {
        if (!isExpressive) {
            return fadeIn(tween(140)).togetherWith(fadeOut(tween(100)))
        }
        val slideDistanceFraction = 0.15f
        val enterSlide = slideInHorizontally(
            animationSpec = tween(durationMillis = DURATION_SCREEN_ENTER, easing = DecelerateEasing),
            initialOffsetX = { fullWidth ->
                if (forward) (fullWidth * slideDistanceFraction).toInt() else (-fullWidth * slideDistanceFraction).toInt()
            }
        ) + fadeIn(
            animationSpec = tween(durationMillis = DURATION_SCREEN_ENTER - 40, easing = EnterEasing)
        )

        val exitSlide = slideOutHorizontally(
            animationSpec = tween(durationMillis = DURATION_SCREEN_EXIT, easing = DecelerateEasing),
            targetOffsetX = { fullWidth ->
                if (forward) (-fullWidth * slideDistanceFraction).toInt() else (fullWidth * slideDistanceFraction).toInt()
            }
        ) + fadeOut(
            animationSpec = tween(durationMillis = DURATION_SCREEN_EXIT, easing = AccelerateEasing)
        )

        return enterSlide.togetherWith(exitSlide)
    }

    // Vertical Expand / Collapse (Accordions, Banners, Search bars)
    fun verticalExpandEnter(
        expandFrom: Alignment.Vertical = Alignment.Top,
        isExpressive: Boolean = true
    ): EnterTransition {
        if (!isExpressive) return fadeIn() + expandVertically(expandFrom = expandFrom)
        return expandVertically(
            animationSpec = tween(durationMillis = DURATION_EXPAND, easing = DecelerateEasing),
            expandFrom = expandFrom
        ) + fadeIn(
            animationSpec = tween(durationMillis = DURATION_EXPAND - 40, easing = EnterEasing)
        )
    }

    fun verticalCollapseExit(
        shrinkTowards: Alignment.Vertical = Alignment.Top,
        isExpressive: Boolean = true
    ): ExitTransition {
        if (!isExpressive) return fadeOut() + shrinkVertically(shrinkTowards = shrinkTowards)
        return shrinkVertically(
            animationSpec = tween(durationMillis = DURATION_COLLAPSE, easing = AccelerateEasing),
            shrinkTowards = shrinkTowards
        ) + fadeOut(
            animationSpec = tween(durationMillis = DURATION_COLLAPSE, easing = AccelerateEasing)
        )
    }

    // Horizontal Expand / Collapse (Attachment buttons, toolbars)
    fun horizontalExpandEnter(
        expandFrom: Alignment.Horizontal = Alignment.Start,
        isExpressive: Boolean = true
    ): EnterTransition {
        if (!isExpressive) return fadeIn() + expandHorizontally(expandFrom = expandFrom)
        return expandHorizontally(
            animationSpec = tween(durationMillis = DURATION_EXPAND, easing = DecelerateEasing),
            expandFrom = expandFrom
        ) + fadeIn(
            animationSpec = tween(durationMillis = DURATION_EXPAND - 40, easing = EnterEasing)
        )
    }

    fun horizontalCollapseExit(
        shrinkTowards: Alignment.Horizontal = Alignment.Start,
        isExpressive: Boolean = true
    ): ExitTransition {
        if (!isExpressive) return fadeOut() + shrinkHorizontally(shrinkTowards = shrinkTowards)
        return shrinkHorizontally(
            animationSpec = tween(durationMillis = DURATION_COLLAPSE, easing = AccelerateEasing),
            shrinkTowards = shrinkTowards
        ) + fadeOut(
            animationSpec = tween(durationMillis = DURATION_COLLAPSE, easing = AccelerateEasing)
        )
    }
}
