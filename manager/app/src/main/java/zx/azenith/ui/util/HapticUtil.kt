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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration

const val PREF_TAP_HAPTIC = "tap_haptic_feedback"

private const val PREFS = "settings"

/** Default ON, so the app buzzes on a tap unless the user turns it off. */
fun isTapHapticEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_TAP_HAPTIC, true)

/**
 * Live flag shared by the Settings row and the tap hook, so a change takes effect on the
 * next tap without recomposing the screens.
 */
object TapHapticState {
    val enabled = mutableStateOf(true)
}

fun setTapHapticEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_TAP_HAPTIC, enabled).apply()
    TapHapticState.enabled.value = enabled
}

/**
 * Haptic on tap for everything under the modifier.
 *
 * A whole-window modifier rather than per-control: it reaches the clickables this app
 * passes `indication = null` (the nav pills) and the M3 controls that carry their own
 * ripple (Button, IconButton, Switch), which a `LocalIndication` override would miss.
 *
 * A tap, not a touch: it fires on the up event, only when something interactive consumed
 * the gesture. A tap on empty space leaves the up unconsumed and stays silent, and a
 * scroll makes waitForUpOrCancellation return null before any up. A long press is
 * excluded by the touch timeout, which is also what the system uses to pick the long-press
 * feedback instead of a tap.
 *
 * VirtualKey is the tap type; it goes through `View.performHapticFeedback`, so the OS
 * touch-feedback setting silences this too.
 */
fun Modifier.tapHaptic(): Modifier = composed {
    val viewConfiguration = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current
    // A key that never changes: the gesture block reads TapHapticState itself, so toggling
    // the pref must not restart the pointer input and lose an in-flight gesture.
    val hapticKey = remember { Any() }

    this.pointerInput(hapticKey) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val up = waitForUpOrCancellation(PointerEventPass.Final)
            if (up != null &&
                up.changedToUpIgnoreConsumed() &&
                up.isConsumed &&
                TapHapticState.enabled.value &&
                (up.uptimeMillis - down.uptimeMillis) <= viewConfiguration.longPressTimeoutMillis
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            }
        }
    }
}
