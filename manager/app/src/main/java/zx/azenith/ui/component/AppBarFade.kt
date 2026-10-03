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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur

/** Mirrors the `expressive_blur_ui` pref; provided once at the app root. */
val LocalAppBlurEnabled = compositionLocalOf { false }

/**
 * Scaffold background for a screen with an app-bar fade.
 *
 * The fade can only reveal what is behind it, so the Scaffold has to go clear
 * while blur is on. Pair with [ScaffoldContentColor] -- contentColorFor() maps a
 * transparent container to Color.Black, which recolours every label in the tree.
 */
@Composable
fun ScaffoldContainerColor(surface: Color): Color =
    if (LocalAppBlurEnabled.current) Color.Transparent else surface

/** Content colour to pair with [ScaffoldContainerColor]; never falls back to black. */
@Composable
fun ScaffoldContentColor(): Color = MaterialTheme.colorScheme.onSurface

/**
 * Marks the scrolling content as the blur source for the app bar above it.
 *
 * The source has to be the list, not the screen: a source wrapping the whole
 * screen also encloses its own app bar, so the bar ends up inside its own blur
 * input and samples itself instead of the content behind it.
 */
@Composable
fun Modifier.hazePageSource(): Modifier {
    val hazeState = LocalAppHazeState.current
    return if (LocalAppBlurEnabled.current && hazeState != null) {
        hazeSource(hazeState)
    } else this
}

/**
 * The app bar's top fade, applied to the app bar's own node rather than to a child.
 *
 * Under blur the ramp is applied twice over the same span: haze's progressive style
 * ramps the blur out, and a colour gradient ramps a little surface tint with it. The
 * blur alone leaves flat-looking bars on light surfaces, so the tint supplies the
 * contrast the colour-only version had -- but at low alpha, or it hides the blur.
 *
 * [overlap] is the fraction of the bar covered by scrolled content, so the blur is
 * skipped at rest: an unoverlapped bar has nothing behind it and sampling it just
 * produces noise. This only shows if the surfaces behind the bar are also transparent.
 *
 * With blur off it falls back to the opaque colour gradient.
 */
@Composable
fun Modifier.appBarFade(surface: Color, overlap: Float): Modifier {
    val hazeState = LocalAppHazeState.current
    if (!LocalAppBlurEnabled.current || hazeState == null || overlap <= 0f) {
        return background(
            Brush.verticalGradient(
                0f to surface,
                0.4f to surface.copy(alpha = 0.9f),
                0.5f to surface.copy(alpha = 0.8f),
                0.6f to surface.copy(alpha = 0.7f),
                0.7f to surface.copy(alpha = 0.5f),
                0.8f to surface.copy(alpha = 0.4f),
                0.9f to surface.copy(alpha = 0.3f),
                1f to Color.Transparent,
            )
        )
    }

    val style = HazeBlurStyle {
        // progressive() only rescales an existing blur -- without a radius the
        // ramp has nothing to fade.
        blurRadius(32.dp)
        // Intensities are blur strength per end: 1f fully blurred, 0f untouched.
        // startY/endY are PIXELS inside this node, not dp -- Infinity is the
        // library's own default for "span the whole node".
        progressive(
            HazeProgressive.verticalGradient(
                easing = LinearEasing,
                startIntensity = 1f,
                endIntensity = 0f,
            )
        )
    }

    // Tint first, blur second: haze reads the source from the window, and the tint
    // has to be part of what gets blurred or it sits on top as a flat wash.
    return this
        .background(
            Brush.verticalGradient(
                0f to surface.copy(alpha = 0.55f * overlap),
                0.45f to surface.copy(alpha = 0.35f * overlap),
                0.75f to surface.copy(alpha = 0.12f * overlap),
                1f to Color.Transparent,
            )
        )
        .hazeBlur(input = HazeInput.Sources(hazeState), style = style)
}