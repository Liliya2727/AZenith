/*
 * Copyright 2026 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zx.azenith.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * Material 3 motion tokens.
 *
 * `androidx.compose.material3.tokens.MotionTokens` carries the same values but is declared
 * `internal` in material3 1.5.0-alpha23, so app code cannot reference it. These are the same
 * numbers, verified against the resolved AAR's class file rather than copied from documentation.
 */
object Motion {
    /** Emphasized decelerate — elements arriving: fast start, gentle settle. */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized accelerate — elements departing: slow start, quick exit. */
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Emphasized — the default for most M3 component state transitions. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Standard — replaces the pre-M3 `FastOutSlowInEasing`. */
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    const val DurationShort4 = 200
    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350

    /**
     * Enter transition for a dialog: scrim fade, container scale-up and a short upward lift,
     * all on one clock so the surface resolves rather than merely appearing.
     *
     * EmphasizedDecelerate finishes ~70% of its travel in the first third of the duration, so
     * the scale and slide are already near their final value by mid-transition — visible motion
     * without a sluggish tail.
     */
    fun dialogEnter() = fadeIn(
        animationSpec = tween(DurationMedium2, easing = EmphasizedDecelerate)
    ) + scaleIn(
        initialScale = 0.86f,
        animationSpec = tween(DurationMedium2, easing = EmphasizedDecelerate)
    ) + slideInVertically(
        initialOffsetY = { it / 8 },
        animationSpec = tween(DurationMedium2, easing = EmphasizedDecelerate)
    )

    /** Exit is always the faster of the pair and always the accelerate curve. */
    fun dialogExit() = fadeOut(
        animationSpec = tween(DurationShort4, easing = EmphasizedAccelerate)
    ) + scaleOut(
        targetScale = 0.86f,
        animationSpec = tween(DurationShort4, easing = EmphasizedAccelerate)
    ) + slideOutVertically(
        targetOffsetY = { it / 12 },
        animationSpec = tween(DurationShort4, easing = EmphasizedAccelerate)
    )
}
