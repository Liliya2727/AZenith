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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Whether the window is in landscape, where the dock is the navigation chrome.
 *
 * The pages branch on this to drop their top app bar: landscape has the vertical room
 * a phone does not, and the bar's actions move to the dock instead of disappearing.
 */
@Composable
fun isLandscapeNav(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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
 * Animated, so the page's rows reflow with the panel instead of taking their new width
 * in one jump -- but only for the page on screen. The pager keeps all four pages
 * measured at once, so a read here is a read by every one of them: animating it
 * unconditionally re-measured four lists per frame and collapsing visibly stuttered.
 * Restricting it to the page the user is looking at is what makes the reflow
 * affordable; the off-screen pages snap, where nobody can see it.
 */
@Composable
fun navBarEndClearance(): Dp {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (!landscape) return 0.dp
    val target = if (NavDockState.expanded) NAV_DOCK_WIDTH else NAV_DOCK_HANDLE_WIDTH
    val isCurrent = LocalIsCurrentPagerPage.current
    val animated by animateDpAsState(
        targetValue = target,
        animationSpec = MotionScheme.expressive().defaultSpatialSpec(),
        label = "navEndClearance"
    )
    return (if (isCurrent) animated else target) + NAV_DOCK_PAGE_MARGIN
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
 *
 * Both branches add the inset: it is real reserved space on the bottom edge either way.
 */
@Composable
fun navBarBottomClearance(navBarInset: Dp): Dp =
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        16.dp + navBarInset
    } else {
        110.dp + navBarInset
    }

/**
 * Start-edge clearance a page keeps for the display cutout.
 *
 * Zero in portrait, where the cutout sits inside the status bar strip the page already
 * avoids. In landscape the cutout moves to a side edge, at the middle of the screen
 * height -- exactly where a list row sits -- so the page has to clear it explicitly.
 * Reading it as start rather than left keeps right-to-left layouts correct, since the
 * inset itself is reported per physical side.
 */
@Composable
fun safeAreaStartInset(): Dp =
    WindowInsets.displayCutout.asPaddingValues()
        .calculateLeftPadding(LocalLayoutDirection.current)
