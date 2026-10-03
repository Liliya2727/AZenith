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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

/**
 * The app bar's fade. Under blur the ramp is the blur itself, fading from fully
 * blurred at the bar to fully clear over [fadeHeight], so list content dissolves
 * as it scrolls under the bar rather than being cut off by a colour band. With
 * blur off it falls back to the original colour gradient.
 *
 * [hazeState] must be the state of a `hazeSource` covering the page behind the
 * bar; without one there is nothing to blur and the blur is skipped.
 */
@Composable
fun AppBarFade(
    surface: Color,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isBlurEnabled: Boolean = false,
    fadeHeight: Dp = 96.dp,
) {
    val blurStyle = if (isBlurEnabled && hazeState != null) {
        HazeBlurStyle {
            // Intensities are blur strength per end: 1f fully blurred, 0f untouched.
            progressive(
                HazeProgressive.verticalGradient(
                    startIntensity = 1f,
                    endIntensity = 0f,
                    startY = 0f,
                    endY = fadeHeight.value,
                )
            )
        }
    } else null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (blurStyle != null) {
                    // Painting a tint over blurred pixels would flatten the ramp,
                    // so this path leaves the surface clear and lets blur carry it.
                    Modifier.hazeBlur(input = HazeInput.Sources(hazeState!!), style = blurStyle)
                } else Modifier.background(
                    Brush.verticalGradient(
                        0f to surface,
                        0.55f to surface.copy(alpha = 0.86f),
                        0.8f to surface.copy(alpha = 0.45f),
                        1f to Color.Transparent,
                    )
                )
            )
    )
}