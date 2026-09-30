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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's single slider implementation, so every slider matches instead of
 * each screen hand-rolling its own.
 *
 * The track is deliberately straight. Material's expressive slider draws a
 * wavy thumb; that is not wanted here, so the thumb is a plain circle and the
 * track is a flat rounded bar.
 *
 * Both the fill and the thumb live in the slider's own track and thumb slots
 * rather than being layered underneath by a separate composable. The slots
 * receive the same [SliderState], so the fill and the thumb are computed from
 * one width and cannot drift apart mid-drag. The track is drawn with
 * drawBehind, so a value change costs a redraw and not a recomposition.
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
    val interactionSource = MutableInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()

    // Material thickens the track under the finger; the spring back to the
    // resting thickness is the part of the interaction that reads as tactile.
    val thickness by animateFloatAsState(
        targetValue = if (pressed) 20f else 14f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "SliderThickness",
    )
    val density = LocalDensity.current
    val trackDp: Dp = with(density) { thickness.dp }
    val thumbDp: Dp = with(density) { (thickness + 10f).dp }

    val state: SliderState = rememberSliderState(
        value = value,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
    )

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(),
        interactionSource = interactionSource,
        track = { sliderState ->
            val fraction = sliderState.value.fractionOf(valueRange)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .drawBehind {
                        val h = trackDp.toPx()
                        val top = (size.height - h) / 2f
                        val radius = CornerRadius(h / 2f)
                        // Resting track.
                        drawRoundRect(
                            color = accent.copy(alpha = 0.16f),
                            topLeft = Offset(0f, top),
                            size = Size(size.width, h),
                            cornerRadius = radius,
                        )
                        // Active portion.
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(0f, top),
                            size = Size(size.width * fraction, h),
                            cornerRadius = radius,
                        )
                    },
            )
        },
        thumb = {
            // Straight thumb: a plain circle, slightly larger under the finger.
            val scale by animateFloatAsState(
                targetValue = if (pressed) 1.12f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "SliderThumbScale",
            )
            Box(
                modifier = Modifier
                    .size(thumbDp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(thumbDp)
                        .drawBehind { drawCircle(color = accent) }
                )
            }
        },
    )
}

/** Where [value] sits inside [range], clamped to 0..1. */
private fun Float.fractionOf(range: ClosedFloatingPointRange<Float>): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f) return 0f
    return ((this - range.start) / span).coerceIn(0f, 1f)
}
