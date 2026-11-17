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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared duration so a row that sizes to its text can animate in step with it. */
internal const val BlurCrossfadeDurationMillis = 220

/**
 * Text that crossfades through a short blur when its value changes.
 *
 * A row that uses `animateContentSize` already eases the width change, but a plain `Text`
 * still swaps its glyphs in one frame. The outgoing string blurs away while fading out and
 * the incoming one resolves from the same blur radius, so the two changes read as one
 * motion. The blur is per-content: each side of the crossfade owns its own animatable.
 */
@Composable
internal fun BlurCrossfadeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    blurRadius: Dp = 6.dp,
    durationMillis: Int = BlurCrossfadeDurationMillis,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            // Let the text's own bounds travel between the two strings. Without a size
            // transform AnimatedContent jumps to the new width in one frame, and a row
            // that wraps to another line yanks its centred leading icon with it.
            fadeIn(animationSpec = tween(durationMillis)) togetherWith
                fadeOut(animationSpec = tween(durationMillis)) using
                SizeTransform(clip = false) { _, _ -> tween(durationMillis) }
        },
        label = "BlurCrossfadeText"
    ) { target ->
        // AnimatedContent keeps both strings composed for the length of the transition.
        // The one heading out animates towards the blur radius, the one arriving starts
        // there and sharpens -- the same blur, read in opposite directions.
        val arriving = transition.targetState != EnterExitState.PostExit
        val blur = remember {
            Animatable(
                initialValue = if (arriving) blurRadius else 0.dp,
                typeConverter = Dp.VectorConverter
            )
        }
        LaunchedEffect(arriving) {
            blur.animateTo(
                targetValue = if (arriving) 0.dp else blurRadius,
                animationSpec = tween(durationMillis)
            )
        }

        Text(
            text = target,
            modifier = modifier.blur(blur.value),
            color = color,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
            softWrap = softWrap
        )
    }
}
