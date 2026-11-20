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

package zx.azenith.ui.gamespace

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import zx.azenith.ui.component.BreathingText
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3

/**
 * First-launch gamepad gate: a stationary, edge-to-edge blurred scrim (the same
 * blur recipe the wallpaper settings overlay uses) with a centred glowing prompt
 * whose highlight drifts slowly left-to-right, PS5-style. Any controller button
 * or touch dismisses it.
 */
@Composable
internal fun ZXGameSpacePressAnyButtonOverlay(
    visible: Boolean,
    hazeState: HazeState,
    onAnyInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val scheme = MaterialTheme.colorScheme

    // Slow flowing highlight: one gradient sweeps across the text repeatedly.
    val flow by rememberInfiniteTransition(label = "PressAnyButtonFlow").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PressAnyButtonFlowValue"
    )
    val pulse by rememberInfiniteTransition(label = "PressAnyButtonPulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PressAnyButtonPulseValue"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = HazeBlurStyle.Material3(
                    containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.55f)
                ) {
                    blurRadius(28.dp)
                }
            )
            .background(scheme.scrim.copy(alpha = 0.40f))
            .pointerInput(Unit) {
                // Any touch also dismisses, so a controller-less setup is never stuck.
                detectTapGestures { onAnyInput() }
            },
        contentAlignment = Alignment.Center
    ) {
        BreathingText(
            text = "Press any button",
            modifier = Modifier.drawBehind { drawFlowGlow(flow, pulse, scheme.primary) }
        )
    }
}

/** Draws a soft, slowly drifting glow behind the prompt text. */
private fun DrawScope.drawFlowGlow(flow: Float, pulse: Float, accent: Color) {
    val w = size.width
    val centerX = (flow + 1f) * 0.5f * w
    val glow = Brush.radialGradient(
        colors = listOf(
            accent.copy(alpha = 0.55f * pulse),
            accent.copy(alpha = 0.18f * pulse),
            Color.Transparent
        ),
        center = androidx.compose.ui.geometry.Offset(centerX, size.height / 2f),
        radius = w * 0.75f
    )
    drawRect(glow)
}
