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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import zx.azenith.NavItem
import zx.azenith.R

/**
 * Whether the landscape dock is showing its rows.
 *
 * The dock is the pager's chrome, so the pages beside it have to know how much
 * room it takes; the preference outlives any one composition and both sides read
 * the same value.
 */
object NavDockState {
    var expanded by mutableStateOf(true)
}

/**
 * The landscape navigation dock: a full-height panel on the end edge whose
 * destinations are rows, not a bottom bar stacked on its side.
 *
 * Portrait keeps the floating pill bar, which is the right shape under a thumb.
 * Landscape has width a bottom bar cannot use, so the dock spends it on what the
 * bar has no room for: the current destination is named once per row instead of
 * the tab being an icon of unstated meaning, the panel names its own tabs rather
 * than relying on a label that opens only on the selected one, and it carries a
 * search entry point beside them. It is chrome rather than an overlay -- squared
 * to the window edges, elevation 0, hairline on its inner edge -- because a panel
 * that floats reads as a card left on screen rather than as navigation.
 *
 * Landscape only by construction: it is laid out beside the pager instead of over
 * it, so the page narrows rather than being covered.
 */
@Composable
fun NavDock(
    items: List<NavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchRoute: String? = null,
    onSearch: ((String) -> Unit)? = null,
    interactive: Boolean = true,
    blurring: Boolean = false,
    accentPanel: Boolean = false,
    scale: Float = 1f,
    spacing: Float = 1f,
    cornerFraction: Float = 1f,
    labels: Boolean = true,
    hazeState: HazeState? = null
) {
    val scheme = MaterialTheme.colorScheme
    val expanded = NavDockState.expanded
    // The panel animates its own width and the pages animate their matching
    // clearance off the same state and the same spec, so the page's new edge and
    // the panel's frame move as one thing rather than fighting over the strip.
    val panelWidth by animateDpAsState(
        targetValue = if (expanded) NAV_DOCK_WIDTH * scale else NAV_DOCK_HANDLE_WIDTH,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dockWidth"
    )
    // Tinted off the accent when the vibrant preset is on, matching the bar. The
    // blur owns the colour while it draws: two tints stacked cheapen both.
    val panelColor = when {
        blurring -> Color.Transparent
        accentPanel -> scheme.primaryContainer
        else -> scheme.surfaceContainer
    }
    val rowSelectedBg = scheme.primary
    val rowSelectedContent = if (blurring) scheme.primary else scheme.onPrimary
    val rowContent = if (accentPanel) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val rowShape = RoundedCornerShape(percent = (cornerFraction.coerceIn(0f, 1f) * 100).toInt())

    Box(
        modifier = modifier
            .width(panelWidth)
            .then(
                if (blurring && hazeState != null) {
                    Modifier.hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = HazeBlurStyle.Material3(
                            containerColor = scheme.surfaceContainer.copy(alpha = 0.4f)
                        ) { blurRadius(24.dp) }
                    )
                } else {
                    Modifier.background(panelColor)
                }
            )
    ) {
        if (!expanded) {
            // Collapsed, the panel is its own handle -- full height, so the way
            // back is wherever the pointer already is.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .innerEdgeHairline(scheme.outlineVariant.copy(alpha = 0.6f))
                    .navigationBarsPadding()
                    .gamepadFocusOutline(shape = RoundedCornerShape(0.dp))
                    .focusProperties {
                        if (interactive) {
                            right = GamepadFocusNav.pageEntry ?: FocusRequester.Default
                        }
                    }
                    .then(
                        if (interactive) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { NavDockState.expanded = true }
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    // [cd_menu] rather than a new string: it is already translated in
                    // all 84 locales and reads the same to a screen reader.
                    contentDescription = stringResource(R.string.cd_menu),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .innerEdgeHairline(scheme.outlineVariant.copy(alpha = 0.6f))
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = NAV_DOCK_SIDE_PAD * scale,
                    vertical = NAV_DOCK_VERTICAL_PAD * scale
                ),
            verticalArrangement = Arrangement.spacedBy(NAV_DOCK_ROW_GAP * scale * spacing)
        ) {
            // The header is the app's own name and the way back out: one tap
            // hands the panel's width to the page.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NAV_DOCK_HEADER_HEIGHT * scale)
                    .clip(rowShape)
                    .then(
                        if (interactive) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { NavDockState.expanded = false }
                        ) else Modifier
                    )
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    // Points out of the panel, which is the direction the width
                    // goes when it is collapsed. Decorative: the header row itself
                    // is the tap target and the collapse is announced by its state.
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            HorizontalDivider(
                color = scheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            if (onSearch != null && searchRoute != null) {
                DockRow(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.cd_search),
                    selected = false,
                    selectedBg = rowSelectedBg,
                    selectedContent = rowSelectedContent,
                    content = rowContent,
                    interactive = interactive,
                    scale = scale,
                    shape = rowShape,
                    compact = !labels,
                    onClick = { onSearch(searchRoute) }
                )
            }

            items.forEach { item ->
                DockRow(
                    icon = item.icon,
                    title = stringResource(item.labelRes),
                    selected = selectedRoute == item.route,
                    selectedBg = rowSelectedBg,
                    selectedContent = rowSelectedContent,
                    content = rowContent,
                    interactive = interactive,
                    scale = scale,
                    shape = rowShape,
                    compact = !labels,
                    onClick = { onItemSelected(item.route) }
                )
            }
        }
    }
}

/**
 * One row: icon, name, and the fill that marks the open one.
 *
 * A destination and an action are the same row here -- the only difference is
 * whether it can be selected, which is what keeps the search entry point from
 * looking like a fifth tab.
 */
@Composable
private fun DockRow(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    selectedBg: Color,
    selectedContent: Color,
    content: Color,
    interactive: Boolean,
    scale: Float,
    shape: Shape,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // The ring the gamepad helper draws is the real focus affordance; this tint
    // is what keeps the row readable for a pointer user who never sees it.
    val focused by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = when {
            selected -> selectedBg
            focused || pressed -> content.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = MotionScheme.expressive().defaultEffectsSpec(),
        label = "dockRowBg"
    )
    val contentColor = if (selected) selectedContent else content

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((if (compact) NAV_DOCK_ROW_HEIGHT_COMPACT else NAV_DOCK_ROW_HEIGHT) * scale)
            .clip(shape)
            .background(background)
            .gamepadFocusOutline(shape = shape)
            .focusProperties {
                if (interactive) {
                    // A dock's up/down belong to its own rows; the way out is
                    // sideways, back into the page.
                    val pageEntry = GamepadFocusNav.pageEntry ?: FocusRequester.Default
                    left = pageEntry
                    right = pageEntry
                }
            }
            .then(
                if (interactive) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(horizontal = NAV_DOCK_ROW_PAD * scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp * scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            // Under the bar's 24 dp: here the icon sits beside its label rather
            // than above it, and a full-size one crowds the row.
            modifier = Modifier.size(20.dp * scale)
        )
        if (!compact) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * The panel's inner-edge hairline.
 *
 * A panel on the same surface as the page needs a seam or the two read as one
 * shape. Drawn rather than an element so it costs the layout nothing.
 */
private fun Modifier.innerEdgeHairline(color: Color): Modifier = this.drawBehind {
    val line = 1.dp.toPx()
    drawRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = Size(line, size.height)
    )
}

/**
 * The panel's width.
 *
 * M3's rail spends 80 dp on icon-and-indicator items with the label under them.
 * Naming every destination beside its icon needs the rest: at 88 dp the label
 * would get about 26 dp of room and every tab would ellipsise to three
 * characters. 136 dp leaves ~74 dp for the label -- "Settings" and its
 * translations fit, and it is still under a fifth of a landscape screen.
 */
val NAV_DOCK_WIDTH = 136.dp
val NAV_DOCK_HANDLE_WIDTH = 40.dp

/**
 * End clearance the pages keep for the dock -- its own width plus the margin the
 * page already keeps on every other edge.
 */
val NAV_DOCK_CLEARANCE = NAV_DOCK_WIDTH + 24.dp

private val NAV_DOCK_SIDE_PAD = 8.dp
private val NAV_DOCK_VERTICAL_PAD = 14.dp
private val NAV_DOCK_ROW_GAP = 2.dp
private val NAV_DOCK_HEADER_HEIGHT = 44.dp
private val NAV_DOCK_ROW_HEIGHT = 52.dp
private val NAV_DOCK_ROW_HEIGHT_COMPACT = 44.dp
private val NAV_DOCK_ROW_PAD = 8.dp
