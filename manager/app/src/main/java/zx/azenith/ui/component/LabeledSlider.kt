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

package zx.azenith.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zx.azenith.R
import kotlin.math.abs
import kotlin.math.round

/**
 * Icon + label + value readout over the slider.
 *
 * [commitOnRelease] exists for sliders whose value re-lays out the whole app.
 * Text scale rewrites the density of every screen, so driving it from
 * [onValueChange] means each frame of the drag re-measures the entire
 * composition and the thumb moves under a lagging finger. With the flag set the
 * thumb and readout still follow the finger from local state, and the caller is
 * only told the new value once the finger lifts -- one recomposition per drag
 * instead of one per frame.
 */
@Composable
internal fun LabeledSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    snapTo: Float? = null,
    commitOnRelease: Boolean = false,
    resetDefault: Float? = null,
    valueTextOf: @Composable (Float) -> String = { valueText },
    onValueChange: (Float) -> Unit
) {
    var dragValue by remember(value) { mutableFloatStateOf(value) }
    val shown = if (commitOnRelease) dragValue else value
    val shownText = if (commitOnRelease) valueTextOf(dragValue) else valueText
    // A tap-to-seek reports its value and finishes within one event, before Compose
    // recomposes, so [shown] would still hold the pre-tap value and the finish would
    // overwrite the seek. Track the newest reported value synchronously instead.
    val latest = remember { mutableFloatStateOf(value) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallLeadingIcon(icon = icon)
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                shownText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            // Same restore affordance the gradient and banner-corner sliders use,
            // so a slider that has been dragged knows its way back.
            if (resetDefault != null) {
                IconButton(
                    onClick = {
                        // These sliders commit on release, so the thumb and readout are
                        // driven from local state; a reset that skipped it would snap
                        // the value back on the next drag.
                        latest.floatValue = resetDefault
                        if (commitOnRelease) dragValue = resetDefault
                        onValueChange(resetDefault)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Restore,
                        contentDescription = stringResource(R.string.reset),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        ZenithSlider(
            value = shown,
            onValueChange = { raw ->
                latest.floatValue = raw
                if (commitOnRelease) {
                    dragValue = raw
                } else {
                    onValueChange(raw)
                }
            },
            onValueChangeFinished = {
                val pulled = snapTo?.let { snapNear(latest.floatValue, it, valueRange) }
                    ?: latest.floatValue.toTick(valueRange, steps)
                if (commitOnRelease) dragValue = pulled
                onValueChange(pulled)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Nearest tick of a slider's own scale: `steps` dividers means `steps + 1` ticks. */
private fun Float.toTick(range: ClosedFloatingPointRange<Float>, steps: Int): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f || steps <= 0) return this
    val ticks = steps + 1
    val snapped = round((this - range.start) / span * ticks) / ticks
    return (range.start + snapped * span).coerceIn(range.start, range.endInclusive)
}

/** Magnetic snap near target, fraction-of-track window. */
private fun snapNear(value: Float, target: Float, range: ClosedFloatingPointRange<Float>): Float {
    val window = (range.endInclusive - range.start) * SNAP_WINDOW
    return if (abs(value - target) <= window) target else value
}

private const val SNAP_WINDOW = 0.06f

/**
 * The corner sliders name their zones rather than printing a number: what a
 * fraction of the surface height means is not something the user can hold in their
 * head. [center] is the slider's rest point, and the bands are measured outward
 * from it in whichever direction the track actually has room for -- the bar dial
 * rests at full round, so its upper band would fall off the end.
 */
@Composable
internal fun cornerLabel(fraction: Float, center: Float): String = when {
    fraction > center + CORNER_LABEL_BAND -> stringResource(R.string.pers_roundness_round)
    fraction < center - CORNER_LABEL_BAND -> stringResource(R.string.pers_roundness_sharp)
    else -> stringResource(R.string.pers_roundness_default)
}

private const val CORNER_LABEL_BAND = 0.12f
