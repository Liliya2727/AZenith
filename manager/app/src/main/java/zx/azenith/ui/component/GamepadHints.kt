/*
 * Copyright (C) 2026-2027 Zexshia
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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import zx.azenith.R

private val HintSize = 16.dp

/**
 * True while a controller is attached or has been used. The hints are pure
 * decoration for the d-pad user, so they never show up in a touch-only session.
 */
@Composable
private fun hintsVisible(): Boolean =
    GamepadInput.controllerConnected || GamepadInput.dpadMode

/** L1 / R1 beside the navigation. [left] picks which shoulder is shown. */
@Composable
fun GamepadShoulderHint(left: Boolean, modifier: Modifier = Modifier) {
    if (!hintsVisible()) return
    Icon(
        painter = painterResource(if (left) R.drawable.ic_gp_l1 else R.drawable.ic_gp_r1),
        contentDescription = null,
        modifier = modifier.size(HintSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    )
}

/**
 * Triangle / Square for the top bar's action area (top action and search).
 * Both are decorative — they are not clickable and never take focus.
 */
@Composable
fun GamepadFaceHints(modifier: Modifier = Modifier) {
    if (!hintsVisible()) return
    val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_gp_triangle),
            contentDescription = null,
            modifier = Modifier.size(HintSize),
            tint = tint,
        )
        Icon(
            painter = painterResource(R.drawable.ic_gp_square),
            contentDescription = null,
            modifier = Modifier.size(HintSize),
            tint = tint,
        )
    }
}
