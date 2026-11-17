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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Snackbar host shared by the pages.
 *
 * Portrait keeps what each page already had: the expressive Material bar that spans the
 * available width, sitting [portraitBottomPadding] above the bottom edge (the pager pages
 * use this to clear the navigation bar).
 *
 * Landscape needs a smaller bar. The navigation chrome moves off the bottom edge, so the
 * pager's bottom padding would leave the bar floating halfway up the short screen; it drops
 * to a plain margin here instead. The chrome is on the end edge now, so the bar also keeps
 * that clearance -- [landscapeDockClearance] adds the in-app dock for the pager pages, and
 * the system navigation bar is always accounted for. And since the screen is wide but the
 * message is short, the bar hugs its text and drops the stock bar's generous vertical
 * padding, so it reads as a small pill rather than a full-width band.
 */
@Composable
fun ZenithSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    portraitBottomPadding: Dp = 0.dp,
    landscapeDockClearance: Boolean = false
) {
    if (!isLandscapeNav()) {
        SnackbarHost(
            hostState = hostState,
            modifier = modifier.padding(bottom = portraitBottomPadding)
        )
        return
    }

    val navigationBarEnd = WindowInsets.navigationBars.asPaddingValues()
        .calculateRightPadding(LocalLayoutDirection.current)
    val dockClearance = if (landscapeDockClearance) navBarEndClearance() else 0.dp

    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(
            start = 16.dp + safeAreaStartInset(),
            end = 16.dp + navigationBarEnd + dockClearance,
            bottom = 16.dp
        ),
        snackbar = { data -> CompactSnackbar(data) }
    )
}

/**
 * A content-sized snackbar for landscape: the stock one is built to fill the width and keeps
 * 14dp of vertical padding even for a single short line, which is more bar than the screen's
 * height budget can spare next to the dock.
 */
@Composable
private fun CompactSnackbar(data: SnackbarData) {
    val visuals = data.visuals
    Surface(
        shape = SnackbarDefaults.shape,
        color = SnackbarDefaults.color,
        contentColor = SnackbarDefaults.contentColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            visuals.actionLabel?.let { label ->
                TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = label,
                        color = SnackbarDefaults.actionColor
                    )
                }
            }
        }
    }
}
