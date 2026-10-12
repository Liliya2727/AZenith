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

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zx.azenith.ui.component.NAV_DOCK_HANDLE_WIDTH
import zx.azenith.ui.component.NAV_DOCK_WIDTH
import zx.azenith.ui.component.NavDockState

/**
 * Clearance a pager page keeps on the end edge for the navigation bar.
 *
 * The bar runs along the bottom in portrait, which [navBarBottomClearance] covers. In
 * landscape it becomes a dock at the end edge, so the clearance moves to that axis and
 * the bottom one shrinks to a plain margin.
 *
 * This is page padding rather than padding on the nav graph: the pages have to keep
 * their own bounds so the dock's blur still has a source to sample behind it.
 *
 * Animated off [NavDockState] with the same spec the panel animates its own width with,
 * so collapsing the dock hands the strip back to the page as the panel closes instead of
 * leaving the page narrower than the thing beside it.
 */
@Composable
fun navBarEndClearance(): Dp {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val dockWidth by animateDpAsState(
        targetValue = when {
            !landscape -> 0.dp
            NavDockState.expanded -> NAV_DOCK_WIDTH
            else -> NAV_DOCK_HANDLE_WIDTH
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navEndClearance"
    )
    return if (landscape) dockWidth + NAV_DOCK_PAGE_MARGIN else 0.dp
}

/**
 * The margin a page keeps between itself and the dock, on top of the dock's own
 * width. Small: the dock already carries a hairline seam, so the page does not need
 * the full margin it keeps on the screen's outer edges.
 */
private val NAV_DOCK_PAGE_MARGIN = 16.dp

/**
 * Bottom clearance a pager page keeps for the navigation bar, [navBarInset] being the
 * gesture bar's own inset.
 */
@Composable
fun navBarBottomClearance(navBarInset: Dp): Dp =
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        16.dp
    } else {
        110.dp + navBarInset
    }
