/*
 * Copyright 2026 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.round
import zx.azenith.ui.util.fireTapHaptic
import zx.azenith.ui.util.sliderTick

/**
 * The app's single slider implementation, so every slider matches instead of
 * each screen hand-rolling its own.
 *
 * This is the stock Material 3 slider with no custom track or thumb slot, which
 * is what makes it the current design rather than the 2023 one. The library
 * draws a wide bar handle with a gap in the active track and a stop indicator
 * under the finger, and widens all three while pressed; supplying a slot
 * replaces that whole treatment, so this deliberately passes none.
 *
 * The track is straight. The wavy treatment in this library belongs to the
 * progress indicators, not the slider, so the stock component already satisfies
 * that without a custom shape.
 *
 * Only the colour is customised, via the [SliderDefaults.colors] arguments, so
 * screens that tint a slider keep working while the geometry stays Material's.
 */
@Composable
internal fun ZenithSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val haptic = LocalHapticFeedback.current
    // Buzzing from onValueChange would fire once per frame of a drag, which reads as a rattle
    // rather than as detents. Tick on the index instead, so there is exactly one pulse per step.
    var lastTick by remember { mutableIntStateOf(Int.MIN_VALUE) }

    Box(
        modifier = modifier.pointerInput(enabled, valueRange, steps) {
            // Material's own slider seeks only after the finger passes touch slop, so a
            // plain tap on the track highlights it and leaves the thumb where it was.
            // This is an ancestor of the Slider, so it still sees the event after the
            // child consumes it, and it only fires when nothing was dragged -- every
            // real drag is already handled by the slider itself.
            if (!enabled) return@pointerInput
            awaitEachGesture {
                // Main pass: Material's Slider consumes the down, so an unconsumed
                // wait on Initial would never fire for us.
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Main
                )
                val reach = THUMB_RADIUS.toPx()
                val usable = size.width - reach * 2f
                if (usable <= 0f) return@awaitEachGesture
                val raw = valueRange.start + (down.position.x - reach) / usable *
                    (valueRange.endInclusive - valueRange.start)
                val snapped = snapToTick(raw, valueRange, steps)
                val tapSlop = TAP_SLOP.toPx()
                // Only a press that never became a drag seeks: the slider itself owns
                // real drags, and it consumed the movement this branch never sees.
                var dragged = false
                var pointer = down.id
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
                        ?: break
                    if (!change.pressed) break
                    if ((change.position - down.position).getDistance() > tapSlop) {
                        dragged = true
                    }
                }
                if (!dragged) {
                    if (snapped != value) onValueChange(snapped)
                    onValueChangeFinished()
                    fireTapHaptic(haptic)
                }
            }
        }
    ) {
        Slider(
            value = value,
            onValueChange = { raw ->
                val tick = sliderTick(raw, valueRange, steps)
                if (tick != lastTick) {
                    lastTick = tick
                    fireTapHaptic(haptic)
                }
                onValueChange(raw)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                activeTickColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.24f),
            ),
        )
    }
}

/** Half the M3 slider thumb (HandleWidth 44dp); the usable track is inset by this. */
private val THUMB_RADIUS = 22.dp

/** Movement past this means the press became a drag, which the slider handles itself. */
private val TAP_SLOP = 12.dp

/** Nearest tick of the slider's own scale: `steps` dividers means `steps + 1` ticks. */
private fun snapToTick(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f || steps <= 0) return value.coerceIn(range.start, range.endInclusive)
    val ticks = steps + 1
    val snapped = round((value - range.start) / span * ticks) / ticks
    return (range.start + snapped * span).coerceIn(range.start, range.endInclusive)
}
