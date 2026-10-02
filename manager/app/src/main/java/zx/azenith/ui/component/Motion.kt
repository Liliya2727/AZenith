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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin

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
     * Scrim enter/exit — a plain opacity ramp on the full-screen backdrop.
     *
     * The scrim is deliberately kept on its own transition rather than sharing the dialog's
     * container transform. `AnimatedVisibility` applies its enter/exit to the whole subtree, so
     * putting the card and the scrim in one `AnimatedVisibility` makes the scale pivot resolve
     * against the scrim's fullscreen bounds — the dialog then appears to grow out of the middle
     * of the screen with the overlay attached, instead of from the control that opened it.
     * Callers wrap the card in one `AnimatedVisibility` and the scrim in another.
     */
    fun scrimEnter() = fadeIn(animationSpec = tween(DurationShort4, easing = Emphasized))

    fun scrimExit() = fadeOut(animationSpec = tween(DurationShort4, easing = Emphasized))

    /**
     * Container-transform enter: the dialog grows out of the tap point rather than appearing at
     * its own centre, so it reads as the triggering control expanding into the dialog.
     *
     * [origin] is the tap position already converted into the card's own coordinate space and
     * normalised so (0, 0) is its top-left and (1, 1) its bottom-right. `scaleIn` grows about
     * `transformOrigin`, so moving the pivot to [origin] makes the card expand away from that
     * corner while its final resting position stays exactly where the layout put it.
     *
     * The initial scale is small on purpose: at 0.1 the card is nearly the size of the triggering
     * tile, which is what makes the eye read continuity between the two rather than a pop-up.
     */
    fun cardEnterFrom(origin: Offset) = fadeIn(
        animationSpec = tween(DurationShort4, easing = EmphasizedDecelerate)
    ) + scaleIn(
        initialScale = 0.1f,
        animationSpec = tween(DurationMedium2, easing = EmphasizedDecelerate),
        transformOrigin = TransformOrigin(origin.x, origin.y)
    )

    /** Counterpart to [cardEnterFrom] — collapses back toward the control that opened it. */
    fun cardExitTo(origin: Offset) = fadeOut(
        animationSpec = tween(DurationShort4, easing = EmphasizedAccelerate)
    ) + scaleOut(
        targetScale = 0.1f,
        animationSpec = tween(DurationShort4, easing = EmphasizedAccelerate),
        transformOrigin = TransformOrigin(origin.x, origin.y)
    )
}
