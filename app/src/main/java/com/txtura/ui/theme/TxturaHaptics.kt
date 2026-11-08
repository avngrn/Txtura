package com.txtura.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralized, battery-efficient haptic feedback engine for Txtura.
 * Supports:
 * - Rich Android 11+ (API 30+) HapticFeedbackConstants (CONFIRM, REJECT, GESTURE_START, GESTURE_END)
 * - Android 14+ (API 34+) TOGGLE_ON / TOGGLE_OFF and SEGMENT_TICK
 * - High-precision LRA predefined VibrationEffects (EFFECT_CLICK, EFFECT_TICK, EFFECT_HEAVY_CLICK)
 * - Graceful fallback down to API 26 (OneShot micro-pulses) and standard View.performHapticFeedback
 * - Respects the global in-app setting [isEnabled]
 */
object TxturaHaptics {

    @Volatile
    var isEnabled: Boolean = true

    /**
     * Light, crisp click for button taps, tab switching, and tool toggles.
     */
    fun click(view: View? = null, context: Context? = null) {
        if (!isEnabled) return
        val handled = view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) == true
        if (!handled && context != null) {
            vibratePredefined(context, VibrationEffect.EFFECT_CLICK)
        }
    }

    /**
     * Subtle micro-tick for sliders, checkmarks, and item selection.
     */
    fun tick(view: View? = null, context: Context? = null) {
        if (!isEnabled) return
        val feedbackConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        val handled = view?.performHapticFeedback(feedbackConstant) == true
        if (!handled && context != null) {
            vibratePredefined(context, VibrationEffect.EFFECT_TICK)
        }
    }

    /**
     * Satisfying solid feedback for long-press on messages or list items.
     */
    fun longPress(view: View? = null, context: Context? = null) {
        if (!isEnabled) return
        val handled = view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) == true
        if (!handled && context != null) {
            vibratePredefined(context, VibrationEffect.EFFECT_HEAVY_CLICK)
        }
    }

    /**
     * Pleasant confirmation pulse when an SMS is sent, an OTP copied, or action completed.
     */
    fun confirm(view: View? = null, context: Context? = null) {
        if (!isEnabled) return
        val handled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM) == true
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) == true
        }
        if (!handled && context != null) {
            vibratePredefined(context, VibrationEffect.EFFECT_CLICK)
        }
    }

    /**
     * Distinct warning pulse when deleting messages, moving to trash, or rejecting an action.
     */
    fun reject(view: View? = null, context: Context? = null) {
        if (!isEnabled) return
        val handled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.REJECT) == true
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) == true
        }
        if (!handled && context != null) {
            vibrateWaveform(context, longArrayOf(0L, 20L, 40L, 25L), intArrayOf(0, 110, 0, 140))
        }
    }

    /**
     * Tactile toggle snap for switches and state flips.
     */
    fun toggle(view: View? = null, context: Context? = null, isChecked: Boolean = true) {
        if (!isEnabled) return
        val feedbackConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (isChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
        val handled = view?.performHapticFeedback(feedbackConstant) == true
        if (!handled && context != null) {
            vibratePredefined(context, VibrationEffect.EFFECT_TICK)
        }
    }

    /**
     * Satisfying snap when a swipe-to-delete or swipe-to-pin action passes the activation threshold.
     */
    fun swipeSnap(view: View? = null, context: Context? = null) {
        if (!isEnabled) return
        val feedbackConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.GESTURE_END
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        val handled = view?.performHapticFeedback(feedbackConstant) == true
        if (!handled && context != null) {
            vibratePredefined(context, VibrationEffect.EFFECT_CLICK)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun vibratePredefined(
        context: Context,
        effectId: Int
    ) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } catch (_: Exception) {
            // Ignored if device lacks vibrator hardware or permission
        }
    }

    private fun vibrateWaveform(context: Context, timings: LongArray, amplitudes: IntArray) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } catch (_: Exception) {
            // Ignored
        }
    }
}
