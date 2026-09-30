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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

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
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
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
