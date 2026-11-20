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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

/**
 * Decorative uppercase label that breathes between 0.4 and 1.0 alpha. Used for
 * the gamepad hint ("PRESS ANY BUTTON"). Purely visual — no focus or click.
 *
 * Alpha is applied via [graphicsLayer] so the animation runs in the draw phase
 * instead of recomposing the text every frame.
 */
@Composable
fun BreathingText(
    text: String,
    modifier: Modifier = Modifier
) {
    val alpha by rememberInfiniteTransition(label = "BreathingText").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingTextAlpha"
    )

    Text(
        text = text.uppercase(),
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp
    )
}

@Preview
@Composable
private fun BreathingTextPreview() {
    MaterialTheme {
        BreathingText(text = "Press any button")
    }
}
