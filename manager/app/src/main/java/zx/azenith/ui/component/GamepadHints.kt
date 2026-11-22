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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zx.azenith.R

private val HintSize = 18.dp
// A badge sits at the action icon's lower-left corner, small enough to read as a
// controller hint rather than a second button.
private val BadgeSize = 13.dp

/**
 * True while a controller is attached or has been used. The hints are pure
 * decoration for the d-pad user, so they never show up in a touch-only session.
 */
@Composable
private fun hintsVisible(): Boolean =
    GamepadInput.controllerConnected || GamepadInput.dpadMode

/** L1 / R1 beside the navigation. [left] picks which shoulder is shown. */
@Composable
fun GamepadShoulderHint(
    left: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = HintSize,
) {
    if (!hintsVisible()) return
    Icon(
        painter = painterResource(if (left) R.drawable.ic_gp_l1 else R.drawable.ic_gp_r1),
        contentDescription = null,
        modifier = modifier.size(size),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    )
}

/**
 * A small controller badge pinned to the lower-left of one action icon, so the
 * glyph reads as belonging to that button instead of floating in the top bar.
 * The hint is decorative — it never takes focus or intercepts a click.
 */
@Composable
fun GamepadButtonBadge(
    glyphRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = BadgeSize,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        content()
        if (hintsVisible()) {
            Icon(
                painter = painterResource(glyphRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-1).dp, y = 1.dp)
                    .size(size),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            )
        }
    }
}

/**
 * [topAction] picks the glyph: Triangle/Y for the page's top action (backup,
 * changelog, reboot), Square/X for search.
 */
@Composable
fun GamepadActionBadge(
    topAction: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GamepadButtonBadge(
        glyphRes = if (topAction) R.drawable.ic_gp_triangle else R.drawable.ic_gp_square,
        modifier = modifier,
        content = content,
    )
}

/** Select / View — opens search in GameSpace. */
@Composable
fun GamepadSelectBadge(
    modifier: Modifier = Modifier,
    size: Dp = BadgeSize,
    content: @Composable () -> Unit,
) {
    GamepadButtonBadge(R.drawable.ic_gp_select, modifier, size, content)
}

/** R2 — opens quick settings in GameSpace. */
@Composable
fun GamepadR2Badge(
    modifier: Modifier = Modifier,
    size: Dp = BadgeSize,
    content: @Composable () -> Unit,
) {
    GamepadButtonBadge(R.drawable.ic_gp_r2, modifier, size, content)
}

/** L2 — opens now playing in GameSpace. */
@Composable
fun GamepadL2Badge(
    modifier: Modifier = Modifier,
    size: Dp = BadgeSize,
    content: @Composable () -> Unit,
) {
    GamepadButtonBadge(R.drawable.ic_gp_l2, modifier, size, content)
}
