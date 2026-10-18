/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.ui.util

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration

const val PREF_TAP_HAPTIC = "tap_haptic_feedback"

private const val PREFS = "settings"

/** Detents a continuous slider is divided into, so a drag ticks instead of buzzing every frame. */
private const val CONTINUOUS_TICKS = 100

/** Default ON, so the app buzzes on a tap unless the user turns it off. */
fun isTapHapticEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_TAP_HAPTIC, true)

/**
 * Live flag shared by the Settings row and the haptic hooks, so a change takes effect on the
 * next gesture without recomposing the screens.
 */
object TapHapticState {
    val enabled = mutableStateOf(true)

    /**
     * Set by a control that owns its own feedback for this gesture, so the window hook stands down.
     * Only meaningful within one gesture: the hook clears it on the next press, so a value that is
     * never read (a gesture that ends in a drag) cannot leak into a later tap.
     */
    var suppressTap = false
    var suppressSound = false

    fun init(context: Context) {
        enabled.value = isTapHapticEnabled(context)
    }
}

fun setTapHapticEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_TAP_HAPTIC, enabled).apply()
    TapHapticState.enabled.value = enabled
}

/** One pulse, honouring the toggle. VirtualKey is the tap type and respects the OS setting. */
fun fireTapHaptic(haptic: HapticFeedback) {
    if (TapHapticState.enabled.value) {
        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
    }
}

/**
 * Confirmation pulse for the toggle that turns haptics on: the switch is off while this runs, so
 * every other hook is gated out and the user would otherwise feel nothing at the moment of
 * enabling. Runs through the window decor view, with vibrator fallback so it reliably pulses.
 */
fun fireConfirmHaptic(context: Context) {
    val decorView = (context as? android.app.Activity)?.window?.decorView
    val performed = decorView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) == true
    if (!performed) {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val effect = try {
                    android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
                } catch (_: Exception) {
                    android.os.VibrationEffect.createOneShot(25L, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val attrs = android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_TOUCH)
                    vibrator?.vibrate(effect, attrs)
                } else {
                    vibrator?.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25L)
            }
        } catch (_: Exception) { }
    }
}

/**
 * Tap haptic for everything under the modifier.
 *
 * A whole-window modifier rather than per-control: it reaches the clickables this app passes
 * `indication = null` (the nav pills) and the M3 controls that carry their own ripple (Button,
 * IconButton, Switch), which a `LocalIndication` override would miss.
 *
 * The event is read at [PointerEventPass.Final], the last pass of an event. That is the whole
 * trick: `Final` is the only pass where a descendant's consumption is already visible, so
 * `isConsumed` there means "a control took this press". Reading it earlier (or asking
 * `waitForUpOrCancellation` for the up) returns nothing on this app, because `clickable` and
 * `toggleable` consume both the down and the up.
 *
 * Firing on a consumed up is what keeps a tap on empty space and a scroll silent, and the
 * long-press guard leaves a hold to the system's long-press feedback. The toggle is sampled at
 * the press rather than at the release, so turning the setting off still feels the press that
 * did it.
 */
fun Modifier.tapHaptic(): Modifier = composed {
    val viewConfiguration = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            // Any leftover from a gesture that never produced a release is dropped here.
            TapHapticState.suppressTap = false
            TapHapticState.suppressSound = false
            val enabledAtPress = TapHapticState.enabled.value
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis

            var up: PointerInputChange? = null
            var sawDownEvent = false
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Final)
                    .changes.firstOrNull { it.id == down.id } ?: break

                if (!change.pressed) {
                    up = change
                    break
                }
                // The down event arrives already consumed by whichever control took it; only a
                // later event being consumed means the gesture became a scroll or a drag.
                if (sawDownEvent && change.isConsumed) break
                if (change.uptimeMillis - down.uptimeMillis > longPressTimeout) break
                sawDownEvent = true
            }

            // Read here, not at the press: the control's own handler runs in the Main pass, which
            // is before this Final pass, so a flag it sets for this gesture is visible now.
            val released = up
            val suppressedTap = TapHapticState.suppressTap
            val suppressedSound = TapHapticState.suppressSound
            if (released != null && released.isConsumed) {
                if (enabledAtPress && !suppressedTap) {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                }
                if (AudioUtil.isGameHubActive && !suppressedSound) {
                    AudioUtil.playOk(context)
                }
            }
        }
    }
}

/** Slot index of [value] on a slider's scale, so a drag ticks per detent and not per frame. */
internal fun sliderTick(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
): Int {
    val span = range.endInclusive - range.start
    if (span <= 0f) return 0
    val divisions = if (steps > 0) steps + 1 else CONTINUOUS_TICKS
    return ((value - range.start) / span * divisions).toInt()
}
