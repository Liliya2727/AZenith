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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
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
 * One action the dock offers at its bottom for the page on screen.
 *
 * Data rather than a composable because the page owns its own dialog, sheet and menu
 * state: it hands over what to draw and keeps the state that makes the click mean
 * something.
 */
data class NavDockAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val badgeCount: Int = 0,
    val highlighted: Boolean = false
)

/**
 * The actions of the page currently showing, published by that page.
 *
 * The dock is the pages' ancestor, so a page cannot hand it anything downward; it
 * writes here instead. Only the page the pager is on publishes, so there is a single
 * writer at a time and no ordering question between the four of them.
 */
private object NavDockActions {
    var current: List<NavDockAction> by mutableStateOf(emptyList())
}

/**
 * Hands this page's [actions] to the dock while this page is the one on screen.
 *
 * Visibility comes from [LocalIsCurrentPagerPage] rather than a parameter because the
 * pager composes all four pages and only one of them may be the dock's writer at a
 * time.
 */
@Composable
fun PublishNavDockActions(actions: List<NavDockAction>) {
    val visible = LocalIsCurrentPagerPage.current
    SideEffect {
        if (visible) NavDockActions.current = actions
    }
    DisposableEffect(visible) {
        onDispose { if (visible) NavDockActions.current = emptyList() }
    }
}

/**
 * Whether the enclosing page is the one the pager is showing.
 *
 * Defaults true so a screen outside the pager reads as visible; nothing outside the
 * pager publishes to the dock anyway.
 */
val LocalIsCurrentPagerPage = compositionLocalOf { true }

/**
 * Provides [LocalIsCurrentPagerPage] to a pager page.
 *
 * The pager keeps all four pages composed, so a page cannot tell from its own scope
 * whether it is the one on screen.
 */
@Composable
fun CurrentPagerPage(current: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIsCurrentPagerPage provides current, content = content)
}

/**
 * The landscape navigation dock: a full-height panel on the end edge whose
 * destinations are rows, not a bottom bar stacked on its side.
 *
 * Portrait keeps the floating pill bar, which is the right shape under a thumb.
 * Landscape has width a bottom bar cannot use, so the dock spends it on what the
 * bar has no room for: the destination the pager is on is named at the top, the
 * open row is named on itself rather than a label that appears on the selected tab
 * alone, and the page's own actions -- its filter, its search, its changelog -- sit
 * at the bottom, which is why landscape needs no top app bar. It is chrome rather
 * than an overlay -- squared to the window edges, elevation 0, hairline on its inner
 * edge -- because a panel that floats reads as a card left on screen rather than as
 * navigation.
 *
 * The panel's corner fraction no longer reaches it: the travelling selection pill
 * derives its own corners from the row height, so the rows stay square and that number
 * no longer competes with the rows' geometry.
 *
 * Landscape only by construction: it is laid out beside the pager instead of over
 * it, so the page narrows rather than being covered.
 */
@Composable
fun NavDock(
    items: List<NavItem>,
    selectedRoute: String,
    title: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
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
    // The panel draws edge to edge -- it is chrome, and its hairline and blur have to
    // reach the screen edges -- but nothing it lays out may sit under the status bar,
    // the navigation bar, or the display cutout.
    //
    // End-side only. The cutout is top-centre in portrait and becomes a strip on one
    // *side* in landscape, and the panel is docked at the end edge: if the cutout lands
    // on the start edge, it is the page's problem (safeAreaStartInset) and padding the
    // panel for it opens a dead margin on the side facing the page.
    //
    // Deliberately not safeDrawing: that includes the IME, and a navigation panel must
    // not be shoved upward by a keyboard.
    val safeInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End)
    // The panel animates its own width, and the value is deliberately kept as a State
    // rather than read here: it is read in the layout block below, so a frame of the
    // animation invalidates layout alone. Reading it in this body would recompose the
    // panel's whole subtree -- rows, labels, actions -- sixty times a second.
    //
    // M3 expressive's spatial spec is the bouncy one; the effects spec is for colour.
    val panelWidth = animateDpAsState(
        targetValue = if (expanded) NAV_DOCK_WIDTH * scale else NAV_DOCK_HANDLE_WIDTH,
        animationSpec = MotionScheme.expressive().defaultSpatialSpec(),
        label = "dockWidth"
    )
    // Tinted off the accent when the vibrant preset is on, matching the bar. The
    // blur owns the colour while it draws: two tints stacked cheapen both.
    val panelColor = when {
        blurring -> Color.Transparent
        accentPanel -> scheme.primaryContainer
        else -> scheme.surfaceContainer
    }
    // The selection is one pill that travels between rows rather than a fill that
    // switches on and off per row: a tab change should carry the highlight with it.
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }
    val rowHeight = (if (labels) NAV_DOCK_ROW_HEIGHT else NAV_DOCK_ROW_HEIGHT_COMPACT) * scale
    val rowGap = NAV_DOCK_ROW_GAP * scale * spacing
    // The single animated value: where the pill is, in row units. Everything the
    // indicator draws is derived from it, so the travel and the stretch it shows on
    // the way cannot drift apart the way two independent animations would.
    val pillPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dockPillPosition"
    )
    // Under blur the panel is mostly transparent, so an opaque tile would sit on it
    // like a sticker; the alpha is the same treatment the portrait bar uses.
    val pillFill = if (blurring) scheme.primary.copy(alpha = 0.25f) else scheme.primary
    val rowSelectedContent = if (blurring) scheme.primary else scheme.onPrimary
    val rowContent = if (accentPanel) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val rowShape = RoundedCornerShape(percent = (cornerFraction.coerceIn(0f, 1f) * 100).toInt())

    Box(
        modifier = modifier
            // Read in the layout block, not in this body: a frame of the spring then
            // invalidates layout alone, instead of recomposing the panel's rows, labels
            // and action tiles sixty times a second.
            .layout { measurable, constraints ->
                val width = panelWidth.value.roundToPx().coerceIn(0, constraints.maxWidth)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = width, maxWidth = width)
                )
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
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
                    .windowInsetsPadding(safeInsets)
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
                DockIconButton(
                    icon = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.cd_menu),
                    tint = scheme.onSurfaceVariant,
                    interactive = interactive,
                    scale = scale,
                    onClick = { NavDockState.expanded = true }
                )
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .innerEdgeHairline(scheme.outlineVariant.copy(alpha = 0.6f))
                .windowInsetsPadding(safeInsets)
                .padding(horizontal = NAV_DOCK_SIDE_PAD * scale)
                .padding(
                    top = NAV_DOCK_VERTICAL_PAD * scale,
                    bottom = NAV_DOCK_BOTTOM_PAD * scale
                )
        ) {
            // M3 separates a navigation surface's headline from its destinations with
            // type and space rather than a rule, so the header is a name block: the
            // eyebrow is the app, the headline is the page, because landscape has no app
            // bar left to say where you are. The collapse control is a real icon button
            // wearing M3's state layer instead of a bare glyph sitting in a tappable row.
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
                    .padding(start = 8.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DockIconButton(
                    // Points out of the panel, which is the direction the width goes
                    // when it collapses.
                    icon = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.cd_menu),
                    tint = scheme.onSurfaceVariant,
                    interactive = interactive,
                    scale = scale,
                    onClick = { NavDockState.expanded = false }
                )
            }

            // Destinations scroll so a long list or a large font still reaches every
            // row; the actions below stay put, because they are the page's controls
            // rather than part of the list.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(rowGap)
            ) {
                // The travelling selection pill. Drawn inside the scrolling column so it
                // travels with the rows. Its own corner radius is half the row height, which
                // is what makes it a pill at any row height; mid-travel it stretches along
                // the axis it is moving on -- equal at both ends of the trip, so it reads as
                // the pill being carried rather than cut -- and it never fades: a translucent
                // pill in flight would blink instead of slide.
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Only the fill is conditional: a destination that is not in the
                    // panel's own list has no index to select, and the rows must still be
                    // there to walk back out of a subscreen.
                    if (selectedIndex >= 0) {
                        val pillShape = RoundedCornerShape(rowHeight / 2)
                        // Distance to the nearest row, 0 once settled: it is what the stretch
                        // and the fill's height both read, so they can never disagree.
                        val travel = abs(pillPosition - pillPosition.roundToInt())
                        val stretch = (NAV_DOCK_PILL_STRETCH * travel * 2f).dp
                        val pillHeight = rowHeight + stretch
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(pillHeight)
                                .offset {
                                    // Centred on the animated position, so growing the pill
                                    // pushes it out both ways rather than only downward.
                                    val centre = ((rowHeight + rowGap) * pillPosition + rowHeight / 2).toPx()
                                    IntOffset(x = 0, y = (centre - pillHeight.toPx() / 2f).roundToInt())
                                }
                                .clip(pillShape)
                                .background(pillFill)
                        )
                    }
                    // Everything but the fill sits above it, so the pill's edges pass
                    // under the labels instead of over them.
                    //
                    // No second copy of the selected icon/label is drawn on top: that made
                    // the destination's text appear twice, and the duplicate slid across the
                    // panel between rows. The row tints its own content instead, on the
                    // effects spec, so the pill's arrival and the label turning to the
                    // pill's foreground are one motion.
                    Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
                        items.forEachIndexed { index, item ->
                            DockRow(
                                icon = item.icon,
                                title = stringResource(item.labelRes),
                                selected = index == selectedIndex,
                                selectedContent = rowSelectedContent,
                                content = rowContent,
                                interactive = interactive,
                                scale = scale,
                                compact = !labels,
                                onClick = { onItemSelected(item.route) }
                            )
                        }
                    }
                }
            }

            val actions = NavDockActions.current
            if (actions.isNotEmpty()) {
                HorizontalDivider(
                    color = scheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
                DockActionBar(
                    actions = actions,
                    content = rowContent,
                    highlight = scheme.primary,
                    interactive = interactive,
                    shape = rowShape,
                    scale = scale
                )
            }
        }
    }
}

/**
 * The page's actions along the dock's bottom edge.
 *
 * One row per three: the panel is narrower than four tap targets abreast, and a
 * wrapping grid keeps the row count honest for a page that offers more.
 */
@Composable
private fun DockActionBar(
    actions: List<NavDockAction>,
    content: Color,
    highlight: Color,
    interactive: Boolean,
    shape: Shape,
    scale: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = NAV_DOCK_ROW_GAP * scale * 2f),
        verticalArrangement = Arrangement.spacedBy(NAV_DOCK_ROW_GAP * scale)
    ) {
        actions.chunked(NAV_DOCK_ACTIONS_PER_ROW).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NAV_DOCK_ROW_GAP * scale)
            ) {
                row.forEach { action ->
                    DockActionButton(
                        action = action,
                        content = content,
                        highlight = highlight,
                        interactive = interactive,
                        shape = shape,
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keep the last row's buttons the width of a full row's.
                repeat(NAV_DOCK_ACTIONS_PER_ROW - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * One action: the page's own control, at the dock's bottom.
 *
 * A tile rather than a row -- an action is not a destination and must not read as a
 * fifth tab -- so it carries no label, and its description lives in [NavDockAction]
 * for the screen reader instead.
 */
@Composable
private fun DockActionButton(
    action: NavDockAction,
    content: Color,
    highlight: Color,
    interactive: Boolean,
    shape: Shape,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val tint = if (action.highlighted) highlight else content
    val background by animateColorAsState(
        targetValue = if (focused || pressed) tint.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = MotionScheme.expressive().defaultEffectsSpec(),
        label = "dockActionBg"
    )

    Box(
        modifier = modifier
            .height(NAV_DOCK_ACTION_HEIGHT * scale)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .gamepadFocusOutline(shape = RoundedCornerShape(percent = 50))
            .focusProperties {
                if (interactive) {
                    val pageEntry = GamepadFocusNav.pageEntry ?: FocusRequester.Default
                    // The actions are the dock's last stop: up is the rows above,
                    // and the way back into the page is sideways.
                    left = pageEntry
                    right = pageEntry
                }
            }
            .then(
                if (interactive) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = action.onClick
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (action.badgeCount > 0) {
            BadgedBox(badge = { Badge { Text(action.badgeCount.toString()) } }) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = tint,
                    modifier = Modifier.size(24.dp * scale)
                )
            }
        } else {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp * scale)
            )
        }
    }
}

/**
 * An icon-only control wearing M3's icon-button state layer.
 *
 * The state layer is what makes an icon read as pressable: a bare glyph in a tappable
 * row has no hover, focus or press feedback of its own, which is why the header's
 * chevron looked like decoration rather than the way out of the panel.
 */
@Composable
private fun DockIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    interactive: Boolean,
    scale: Float,
    shape: Shape = CircleShape,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val layer by animateColorAsState(
        targetValue = if (focused || pressed) tint.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = MotionScheme.expressive().defaultEffectsSpec(),
        label = "dockIconButtonLayer"
    )

    Box(
        modifier = Modifier
            .size(NAV_DOCK_ICON_BUTTON_SIZE * scale)
            .clip(shape)
            .background(layer)
            .gamepadFocusOutline(shape = shape)
            .then(
                if (interactive) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp * scale)
        )
    }
}

/**
 * One row: icon above/left of the name, with no fill of its own.
 *
 * The selection is drawn by the travelling pill behind these, so a row never paints
 * its own background -- it only says where its content sits.
 */
@Composable
private fun DockRow(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    selectedContent: Color,
    content: Color,
    interactive: Boolean,
    scale: Float,
    shape: Shape? = null,
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
            focused || pressed -> content.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        animationSpec = MotionScheme.expressive().defaultEffectsSpec(),
        label = "dockRowBg"
    )
    // A row under the pill is drawn on the pill, so its content takes the pill's own
    // foreground -- animated rather than switched, because the pill takes a moment to
    // arrive: flipping it instantly would leave the arriving label unreadable on the
    // panel and then readable again.
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContent else content,
        animationSpec = MotionScheme.expressive().defaultEffectsSpec(),
        label = "dockRowContent"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((if (compact) NAV_DOCK_ROW_HEIGHT_COMPACT else NAV_DOCK_ROW_HEIGHT) * scale)
            // The row's own feedback is shaped like the selection pill, not a rectangle:
            // a focus or press tint with different corners lands on the pill visibly
            // mismatched.
            .background(background, RoundedCornerShape(percent = 50))
            .gamepadFocusOutline(shape = RoundedCornerShape(percent = 50))
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
        horizontalArrangement = Arrangement.spacedBy(12.dp * scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp * scale)
        )
        if (!compact) {
            Text(
                text = title,
                // M3's navigation label role, at its own weight: bodyMedium plus a
                // manual SemiBold was inventing a type style the system already has.
                style = MaterialTheme.typography.labelLarge,
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

/**
 * Panel metrics, on M3's 8 dp spacing grid: margins, tile sizes and the row rhythm stay
 * related to each other instead of drifting, and the panel can be scaled and still read
 * as a navigation surface.
 */
private val NAV_DOCK_SIDE_PAD = 12.dp
private val NAV_DOCK_VERTICAL_PAD = 8.dp
private val NAV_DOCK_ROW_GAP = 2.dp
private val NAV_DOCK_HEADER_HEIGHT = 56.dp
private val NAV_DOCK_ROW_HEIGHT = 56.dp
private val NAV_DOCK_ROW_HEIGHT_COMPACT = 48.dp
private val NAV_DOCK_ROW_PAD = 12.dp
private val NAV_DOCK_ICON_BUTTON_SIZE = 48.dp
private val NAV_DOCK_ACTION_HEIGHT = 48.dp

/**
 * Bottom margin the panel keeps below its own content, on top of the navigation bar
 * inset. Small: the inset already clears the gesture bar or the three-button row, so a
 * larger number here reads as the panel being pushed off the screen edge.
 */
private val NAV_DOCK_BOTTOM_PAD = 4.dp

/**
 * How far the travelling pill stretches along its axis of travel at the midpoint
 * between two rows, in dp. Small on purpose: enough to read as the selection being
 * carried rather than cut, without the pill visibly changing size.
 */
private const val NAV_DOCK_PILL_STRETCH = 6f

/**
 * Actions per row at the dock's bottom: three abreast fit the panel's inner width,
 * a fourth does not.
 */
private const val NAV_DOCK_ACTIONS_PER_ROW = 3
