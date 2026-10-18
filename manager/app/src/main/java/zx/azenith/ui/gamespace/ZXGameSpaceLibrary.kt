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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package zx.azenith.ui.gamespace

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.delay
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.GamepadFocusNav
import zx.azenith.ui.component.GamepadInput
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.viewmodel.ApplistViewmodel

private const val LIBRARY_STAGGER_MS = 35L
private const val LIBRARY_STAGGER_CAP = 11
// Tiles composed later than this after a filter change are scrolled-in rows: they appear at rest.
private const val LIBRARY_REVEAL_WINDOW_MS = 400L
private val LIBRARY_TILE_HEIGHT = 88.dp
private val LIBRARY_REST_CORNER = 28.dp
private val LIBRARY_ENGAGED_CORNER = 12.dp


/**
 * Stage 3: Full Game Library Grid when the rightmost Library tile is selected.
 */
@Composable
internal fun LibraryGridStage(
    games: List<ApplistViewmodel.AppInfo>,
    allApps: List<ApplistViewmodel.AppInfo>,
    expanded: Boolean,
    onOpenSettings: (ApplistViewmodel.AppInfo) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onNavigateUp: () -> Unit = {},
    loading: Boolean = false
) {
    // Null until the user picks: the default follows whether games exist once the load lands.
    var scopeChoice by remember { mutableStateOf<Boolean?>(null) }
    val showAllApps = scopeChoice ?: games.isEmpty()
    var onlyEnabled by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val expandedNow = rememberUpdatedState(expanded)
    val notifyExpanded = rememberUpdatedState(onExpandedChange)

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > 0 || offset > 60) notifyExpanded.value(true)
            }
    }
    val gridPull = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                if (!expandedNow.value) return Offset.Zero
                if (gridState.firstVisibleItemIndex > 0) return Offset.Zero
                if (gridState.firstVisibleItemScrollOffset > 0) return Offset.Zero
                notifyExpanded.value(false)
                return Offset.Zero
            }
        }
    }

    val baseList = if (showAllApps) allApps else games
    val displayed = remember(baseList, onlyEnabled) {
        if (onlyEnabled) baseList.filter { it.isEnabledInConfig } else baseList
    }
    val revealEpoch = remember(showAllApps, onlyEnabled) { SystemClock.uptimeMillis() }

    Column(modifier = Modifier.fillMaxSize().nestedScroll(gridPull)) {
        LibraryFilterBar(
            showAllApps = showAllApps,
            gamesCount = games.size,
            allAppsCount = allApps.size,
            onlyEnabled = onlyEnabled,
            onScopeChange = { scopeChoice = it },
            onToggleEnabled = { onlyEnabled = !onlyEnabled },
            onNavigateUp = onNavigateUp
        )

        Spacer(Modifier.height(12.dp))

        val paneModifier = Modifier.weight(1f).fillMaxWidth()
        when {
            displayed.isNotEmpty() -> {
                val motion = MaterialTheme.motionScheme
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    state = gridState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    itemsIndexed(displayed, key = { _, app -> app.packageName }) { index, app ->
                        LibraryAppTile(
                            app = app,
                            order = index.coerceAtMost(LIBRARY_STAGGER_CAP),
                            revealEpoch = revealEpoch,
                            onClick = { onOpenSettings(app) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(LIBRARY_TILE_HEIGHT)
                                .animateItem(placementSpec = motion.defaultSpatialSpec())
                        )
                    }
                }
            }

            loading -> LibraryStatePane(
                titleRes = R.string.gamespace_library_loading,
                loading = true,
                modifier = paneModifier
            )

            onlyEnabled && baseList.isNotEmpty() -> LibraryStatePane(
                titleRes = R.string.gamespace_library_no_enabled,
                bodyRes = R.string.gamespace_library_no_enabled_desc,
                icon = Icons.Rounded.Tune,
                actionRes = R.string.gamespace_library_clear_filter,
                onAction = { onlyEnabled = false },
                modifier = paneModifier
            )

            !showAllApps && allApps.isNotEmpty() -> LibraryStatePane(
                titleRes = R.string.gamespace_no_games,
                bodyRes = R.string.gamespace_no_games_desc,
                icon = Icons.Rounded.GridView,
                actionRes = R.string.gamespace_library_browse_all,
                onAction = { scopeChoice = true },
                modifier = paneModifier
            )

            else -> LibraryStatePane(
                titleRes = R.string.gamespace_no_games,
                bodyRes = R.string.gamespace_no_games_desc,
                icon = Icons.Rounded.GridView,
                modifier = paneModifier
            )
        }
    }
}

@Composable
private fun LibraryFilterBar(
    showAllApps: Boolean,
    gamesCount: Int,
    allAppsCount: Int,
    onlyEnabled: Boolean,
    onScopeChange: (Boolean) -> Unit,
    onToggleEnabled: () -> Unit,
    onNavigateUp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    onNavigateUp()
                    true
                } else {
                    false
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Games / All Apps pick one list, so they read as one connected control.
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            LibraryScopeButton(
                checked = !showAllApps,
                icon = Icons.Rounded.SportsEsports,
                label = "${stringResource(R.string.gamespace_tab_games)} ($gamesCount)",
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                onClick = { onScopeChange(false) }
            )
            LibraryScopeButton(
                checked = showAllApps,
                icon = Icons.Rounded.Apps,
                label = "${stringResource(R.string.gamespace_all_apps)} ($allAppsCount)",
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                onClick = { onScopeChange(true) }
            )
        }
        Spacer(Modifier.width(16.dp))
        ExpressiveFilterChip(
            selected = onlyEnabled,
            label = stringResource(R.string.gamespace_tuned),
            leadingIcon = Icons.Rounded.Tune,
            onClick = onToggleEnabled
        )
    }
}

@Composable
private fun LibraryScopeButton(
    checked: Boolean,
    icon: ImageVector,
    label: String,
    shapes: ToggleButtonShapes,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    ToggleButton(
        checked = checked,
        onCheckedChange = { if (it) onClick() },
        shapes = shapes,
        // secondaryContainer keeps primaryContainer reserved for tuned app tiles.
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurfaceVariant,
            checkedContainerColor = scheme.secondaryContainer,
            checkedContentColor = scheme.onSecondaryContainer
        ),
        modifier = Modifier
            .semantics { role = Role.RadioButton }
            .gamepadFocusOutline(shape = CircleShape)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

@Composable
internal fun ExpressiveFilterChip(
    selected: Boolean,
    label: String,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val colorSpec = motion.defaultEffectsSpec<Color>()
    val spatialSpec = motion.defaultSpatialSpec<IntSize>()
    val isSelected = selected

    val container by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh,
        animationSpec = colorSpec,
        label = "ChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = colorSpec,
        label = "ChipContent"
    )

    // Surface(onClick) pads the touch target to 48dp around the 40dp visual pill.
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = Modifier
            .animateContentSize(spatialSpec)
            .gamepadFocusOutline(shape = CircleShape)
            .semantics { this.selected = isSelected }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
            AnimatedVisibility(visible = selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * One app in the library. Tuned apps carry the accent fill and a per-app Material shape;
 * untuned apps stay on a quiet surface with a circle. Press and d-pad focus squish the tile,
 * tighten its corners and morph the badge.
 */
@Composable
private fun LibraryAppTile(
    app: ApplistViewmodel.AppInfo,
    order: Int,
    revealEpoch: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val tuned = app.isEnabledInConfig

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val dpadFocused = focused && GamepadInput.active

    val press = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "LibraryTilePress"
    )
    val lift = animateFloatAsState(
        targetValue = if (dpadFocused) 1f else 0f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "LibraryTileLift"
    )
    val container = animateColorAsState(
        targetValue = if (tuned) scheme.primaryContainer else scheme.surfaceContainer,
        animationSpec = motion.defaultEffectsSpec(),
        label = "LibraryTileFill"
    )
    val titleColor = if (tuned) scheme.onPrimaryContainer else scheme.onSurface
    val chipContainer = if (tuned) {
        scheme.primary.copy(alpha = 0.18f)
    } else {
        scheme.surfaceContainerHighest.copy(alpha = 0.85f)
    }
    val chipContent = if (tuned) scheme.primary else scheme.onSurfaceVariant

    val enter = remember(revealEpoch) {
        val fresh = SystemClock.uptimeMillis() - revealEpoch < LIBRARY_REVEAL_WINDOW_MS
        Animatable(if (fresh) 0f else 1f)
    }
    val enterSpec = motion.slowSpatialSpec<Float>()
    LaunchedEffect(revealEpoch) {
        if (enter.value < 1f) {
            delay(order * LIBRARY_STAGGER_MS)
            enter.animateTo(1f, enterSpec)
        }
    }

    val engaged = { maxOf(press.value, lift.value) }
    val corner = { lerp(LIBRARY_REST_CORNER, LIBRARY_ENGAGED_CORNER, engaged().coerceIn(0f, 1f)) }

    Box(
        modifier = modifier
            .graphicsLayer {
                val e = enter.value
                val s = (0.88f + 0.12f * e) * (1f - 0.04f * press.value) * (1f + 0.03f * lift.value)
                scaleX = s
                scaleY = s
                alpha = e.coerceIn(0f, 1f)
                translationY = (1f - e) * 24.dp.toPx()
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner())
                clip = true
            }
            .drawWithContent {
                drawRect(container.value)
                drawContent()
            }
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) GamepadFocusNav.publishLastFocused(requester)
            }
            .focusRequester(requester)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = titleColor),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MorphBadge(
                shape = BentoShapes.Circle,
                color = if (tuned) scheme.primary else scheme.surfaceContainerHighest,
                size = 60.dp,
                pressedShape = BentoShapes.Cookie4,
                engaged = engaged,
                spin = 24f
            ) {
                AppIconImage(app = app, size = 38.dp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    shape = CircleShape,
                    color = chipContainer,
                    contentColor = chipContent
                ) {
                    Text(
                        text = stringResource(if (tuned) R.string.gamespace_tuned else R.string.gamespace_untuned),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Loading, filtered-to-nothing and no-games states, each naming its cause and next step. */
@Composable
private fun LibraryStatePane(
    titleRes: Int,
    modifier: Modifier = Modifier,
    bodyRes: Int? = null,
    icon: ImageVector = Icons.Rounded.GridView,
    loading: Boolean = false,
    actionRes: Int? = null,
    onAction: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) {
                LoadingIndicator(modifier = Modifier.size(72.dp))
            } else {
                MorphBadge(
                    shape = BentoShapes.Cookie9,
                    color = scheme.primaryContainer,
                    size = 80.dp
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (bodyRes != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(320.dp)
                )
            }
            if (actionRes != null) {
                Spacer(Modifier.height(20.dp))
                FilledTonalButton(
                    onClick = onAction,
                    modifier = Modifier.gamepadFocusOutline(shape = CircleShape)
                ) {
                    Text(text = stringResource(actionRes))
                }
            }
        }
    }
}
