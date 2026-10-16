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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.gamespace

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import zx.azenith.R
import zx.azenith.ui.component.AppIconCache
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.GamepadFocusRoot
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.util.getChipsetName
import zx.azenith.ui.util.getRealDeviceName
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import zx.azenith.ui.viewmodel.ApplistViewmodel

private enum class TopCategory(val labelRes: Int) {
    Games(R.string.gamespace_tab_games),
    System(R.string.gamespace_tab_media)
}

private sealed interface RibbonEntry {
    val key: String

    data object Welcome : RibbonEntry {
        override val key: String = "__welcome__"
    }

    data class Game(val app: ApplistViewmodel.AppInfo) : RibbonEntry {
        override val key: String = app.packageName
    }

    data object Library : RibbonEntry {
        override val key: String = "__library__"
    }
}

/**
 * ZXGameSpace Material 3 Expressive home screen with full touch and controller support.
 */
@Composable
fun ZXGameSpaceHub(
    profile: String,
    profileValue: String = "",
    games: List<ApplistViewmodel.AppInfo>,
    allApps: List<ApplistViewmodel.AppInfo> = games,
    onApplyProfile: (String) -> Unit = {},
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val effectsSpec = motion.defaultEffectsSpec<Float>()

    var category by remember { mutableStateOf(TopCategory.Games) }
    var selectedRibbonIndex by remember { mutableIntStateOf(0) }

    val ribbonEntries = remember(games) {
        buildList {
            add(RibbonEntry.Welcome)
            games.forEach { add(RibbonEntry.Game(it)) }
            add(RibbonEntry.Library)
        }
    }

    val safeIndex = selectedRibbonIndex.coerceIn(0, ribbonEntries.lastIndex)
    val activeEntry = ribbonEntries[safeIndex]

    val clock = rememberClock()
    val battery = rememberBatteryState()
    val ramState = rememberRamState()
    val refreshHz = remember(context) {
        getSupportedRefreshRatesPicker(context).firstOrNull()?.toIntOrNull() ?: 60
    }
    val deviceName = remember(context) { getRealDeviceName(context) }
    val chipsetName = remember(context) { getChipsetName(context) }

    GamepadFocusRoot(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceContainerLowest)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_BUTTON_L1 -> {
                            category = TopCategory.Games
                            true
                        }
                        KeyEvent.KEYCODE_BUTTON_R1 -> {
                            category = TopCategory.System
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Dynamic full-bleed atmospheric backdrop driven by the selected ribbon entry.
        ZXGameSpaceStageBackdrop(
            entry = if (category == TopCategory.Games) activeEntry else RibbonEntry.Welcome
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            ZXGameSpaceTopBar(
                selectedCategory = category,
                clock = clock,
                onSelectCategory = { category = it },
                onSearchClick = {
                    category = TopCategory.Games
                    selectedRibbonIndex = ribbonEntries.lastIndex
                },
                onSettingsClick = {
                    category = if (category == TopCategory.System) TopCategory.Games else TopCategory.System
                },
                onAvatarClick = {
                    val nextProfile = when (profileValue) {
                        "1" -> "2"
                        "2" -> "3"
                        else -> "1"
                    }
                    onApplyProfile(nextProfile)
                },
                onExit = onExit
            )

            Spacer(Modifier.height(6.dp))

            if (category == TopCategory.Games) {
                ZXGameSpaceTileRibbon(
                    entries = ribbonEntries,
                    selectedIndex = safeIndex,
                    onSelect = { selectedRibbonIndex = it },
                    onActivate = { entry ->
                        when (entry) {
                            is RibbonEntry.Game -> onLaunch(entry.app)
                            RibbonEntry.Welcome -> {
                                games.firstOrNull()?.let { selectedRibbonIndex = 1 }
                            }
                            RibbonEntry.Library -> {
                                selectedRibbonIndex = ribbonEntries.lastIndex
                            }
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))

                AnimatedContent(
                    targetState = activeEntry,
                    transitionSpec = {
                        fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec)
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    label = "ZXGameSpaceMainStage"
                ) { entry ->
                    when (entry) {
                        RibbonEntry.Welcome -> WelcomeHubStage(
                            profile = profile,
                            profileValue = profileValue,
                            games = games,
                            allAppsCount = allApps.size.coerceAtLeast(games.size),
                            battery = battery,
                            ramState = ramState,
                            refreshHz = refreshHz,
                            deviceName = deviceName,
                            chipsetName = chipsetName,
                            onApplyProfile = onApplyProfile,
                            onOpenSystem = { category = TopCategory.System },
                            onSelectGame = { game ->
                                val idx = ribbonEntries.indexOfFirst {
                                    it is RibbonEntry.Game && it.app.packageName == game.packageName
                                }
                                if (idx >= 0) selectedRibbonIndex = idx else onLaunch(game)
                            },
                            onLaunchGame = onLaunch
                        )

                        is RibbonEntry.Game -> GameHeroStage(
                            app = entry.app,
                            profile = profile,
                            refreshHz = refreshHz,
                            onLaunch = { onLaunch(entry.app) },
                            onOpenTune = { category = TopCategory.System }
                        )

                        RibbonEntry.Library -> LibraryGridStage(
                            games = games,
                            allApps = allApps,
                            onLaunch = onLaunch
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                SystemControlStage(
                    profile = profile,
                    profileValue = profileValue,
                    battery = battery,
                    ramState = ramState,
                    refreshHz = refreshHz,
                    deviceName = deviceName,
                    chipsetName = chipsetName,
                    onApplyProfile = onApplyProfile,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Full-screen atmospheric background that crossfades between the ZXGameSpace contour/symbol wave
 * and a rich game artwork stage with Material 3 tonal scrims.
 */
@Composable
private fun ZXGameSpaceStageBackdrop(entry: RibbonEntry) {
    val scheme = MaterialTheme.colorScheme

    Crossfade(
        targetState = entry,
        label = "ZXGameSpaceBackdropCrossfade"
    ) { current ->
        Box(modifier = Modifier.fillMaxSize().background(scheme.surfaceContainerLowest)) {
            when (current) {
                is RibbonEntry.Game -> {
                    val bitmap = AppIconCache.get(current.app.packageName)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.72f)
                                .align(Alignment.CenterEnd)
                                .blur(36.dp, BlurredEdgeTreatment.Unbounded)
                                .alpha(0.38f)
                        )
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(320.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = (-72).dp, y = (-18).dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .alpha(0.28f)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.banner_bg),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.28f)
                        )
                    }
                }

                else -> {
                    Image(
                        painter = painterResource(R.drawable.banner_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(0.22f)
                    )
                    ZXGameSpaceContourGlyphCanvas(
                        primaryColor = scheme.primary,
                        tertiaryColor = scheme.tertiary,
                        outlineColor = scheme.outlineVariant
                    )
                }
            }

            // Left-to-right and top-to-bottom Material 3 tonal scrims for guaranteed legibility.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.92f),
                                scheme.surfaceContainerLowest.copy(alpha = 0.58f),
                                scheme.primaryContainer.copy(alpha = 0.14f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.55f),
                                Color.Transparent,
                                scheme.surfaceContainerLowest.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Draws the abstract sweeping contour ribbons and translucent controller symbol geometry seen in the
 * upper-right quadrant of the ZXGameSpace Welcome Hub.
 */
@Composable
private fun ZXGameSpaceContourGlyphCanvas(
    primaryColor: Color,
    tertiaryColor: Color,
    outlineColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Sweeping topographic contour curves across the stage.
        for (i in 0..4) {
            val shift = i * 26f
            val wave = Path().apply {
                moveTo(w * 0.12f, h * 0.52f + shift)
                cubicTo(
                    w * 0.38f, h * 0.24f + shift,
                    w * 0.64f, h * 0.62f - shift,
                    w * 0.98f, h * 0.18f + shift
                )
            }
            drawPath(
                path = wave,
                color = outlineColor.copy(alpha = 0.16f - i * 0.025f),
                style = Stroke(width = 2.2f)
            )
        }

        // Translucent geometric controller symbols in the upper-right quadrant.
        val cx = w * 0.72f
        val cy = h * 0.34f
        val stroke = Stroke(width = 4f, cap = StrokeCap.Round)

        // Circle glyph
        drawCircle(
            color = tertiaryColor.copy(alpha = 0.18f),
            radius = 54f,
            center = Offset(cx + 120f, cy - 28f),
            style = stroke
        )
        // Square/pill glyph
        drawRoundRect(
            color = primaryColor.copy(alpha = 0.18f),
            topLeft = Offset(cx - 88f, cy - 42f),
            size = Size(86f, 86f),
            cornerRadius = CornerRadius(20f, 20f),
            style = stroke
        )
        // Cross glyph
        val crossX = cx + 18f
        val crossY = cy + 24f
        drawLine(
            color = primaryColor.copy(alpha = 0.22f),
            start = Offset(crossX - 36f, crossY - 36f),
            end = Offset(crossX + 36f, crossY + 36f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.22f),
            start = Offset(crossX + 36f, crossY - 36f),
            end = Offset(crossX - 36f, crossY + 36f),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        // Triangle glyph
        val tri = Path().apply {
            moveTo(cx + 195f, cy - 95f)
            lineTo(cx + 245f, cy - 10f)
            lineTo(cx + 145f, cy - 10f)
            close()
        }
        drawPath(
            path = tri,
            color = tertiaryColor.copy(alpha = 0.16f),
            style = stroke
        )
    }
}

/**
 * ZXGameSpace top navigation bar: "Games | System" on the left; Search, Settings, Profile Avatar with live
 * status dot, Exit, and Clock on the right.
 */
@Composable
private fun ZXGameSpaceTopBar(
    selectedCategory: TopCategory,
    clock: String,
    onSelectCategory: (TopCategory) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onExit: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopCategory.entries.forEach { tab ->
                val selected = tab == selectedCategory
                val shape = MaterialTheme.shapes.small
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(shape)
                        .gamepadFocusOutline(shape = shape)
                        .clickable { onSelectCategory(tab) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.gamespace_library_title),
                onClick = onSearchClick
            )
            TopBarIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.gamespace_rail_settings),
                onClick = onSettingsClick
            )

            // Profile avatar with green/primary online status indicator dot (matches Image 2).
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .gamepadFocusOutline(shape = CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.avatar),
                    contentDescription = stringResource(R.string.gamespace_profile),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(30.dp).clip(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(scheme.primary)
                        .border(1.5.dp, scheme.surfaceContainerLowest, CircleShape)
                )
            }

            TopBarIconButton(
                icon = Icons.Rounded.PowerSettingsNew,
                contentDescription = stringResource(R.string.gamespace_rail_exit),
                tint = scheme.onSurfaceVariant,
                onClick = onExit
            )

            Spacer(Modifier.width(4.dp))

            Text(
                text = clock,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                color = scheme.onSurface
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .gamepadFocusOutline(shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * ZXGameSpace cross-media tile ribbon:
 * - Unselected tiles are compact 54dp rounded squares aligned to the top of the ribbon.
 * - Selected/focused tile expands smoothly to 88dp with a crisp outer focus ring.
 * - The selected item's badge + title sits immediately to the right of the expanded tile,
 *   directly underneath the adjacent compact tiles, matching the ZXGameSpace home layout.
 */
@Composable
private fun ZXGameSpaceTileRibbon(
    entries: List<RibbonEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onActivate: (RibbonEntry) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    var selectedRightEdgePx by remember { mutableStateOf(0f) }
    var ribbonWidthPx by remember { mutableStateOf(0f) }

    LaunchedEffect(selectedIndex, entries.size) {
        runCatching { listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0)) }
    }

    val activeEntry = entries.getOrElse(selectedIndex) { RibbonEntry.Welcome }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .onGloballyPositioned { ribbonWidthPx = it.size.width.toFloat() }
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .gamepadScreenEntry(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            contentPadding = PaddingValues(end = 180.dp)
        ) {
            itemsIndexed(entries, key = { _, item -> item.key }) { index, item ->
                val isSelected = index == selectedIndex
                ZXGameSpaceRibbonTile(
                    entry = item,
                    selected = isSelected,
                    onFocus = { onSelect(index) },
                    onClick = {
                        if (isSelected) {
                            onActivate(item)
                        } else {
                            onSelect(index)
                        }
                    },
                    modifier = if (isSelected) {
                        Modifier.onGloballyPositioned { coords ->
                            selectedRightEdgePx = coords.positionInParent().x + coords.size.width
                        }
                    } else {
                        Modifier
                    }
                )
            }
        }

        // Adjacent label sits just right of the expanded tile's lower edge.
        val labelOffsetPx = remember(selectedRightEdgePx, ribbonWidthPx, density) {
            val gapPx = with(density) { 12.dp.toPx() }
            val maxPx = (ribbonWidthPx - with(density) { 220.dp.toPx() }).coerceAtLeast(0f)
            (selectedRightEdgePx + gapPx).coerceIn(0f, maxPx).roundToInt()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(x = labelOffsetPx, y = 0) }
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (activeEntry) {
                RibbonEntry.Welcome -> {
                    Text(
                        text = stringResource(R.string.gamespace_welcome),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }

                is RibbonEntry.Game -> {
                    ZXGameSpacePlatformTag(
                        text = if (activeEntry.app.isEnabledInConfig) {
                            stringResource(R.string.gamespace_tuned).uppercase()
                        } else {
                            "GAME"
                        }
                    )
                    Text(
                        text = activeEntry.app.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                RibbonEntry.Library -> {
                    Text(
                        text = stringResource(R.string.gamespace_library_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ZXGameSpaceRibbonTile(
    entry: RibbonEntry,
    selected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val spatialSpec = motion.defaultSpatialSpec<androidx.compose.ui.unit.Dp>()
    val colorSpec = motion.defaultEffectsSpec<Color>()

    val tileSize by animateDpAsState(
        targetValue = if (selected) 88.dp else 54.dp,
        animationSpec = spatialSpec,
        label = "RibbonTileSize"
    )
    val outerShape = if (selected) MaterialTheme.shapes.large else MaterialTheme.shapes.medium
    val borderColor by animateColorAsState(
        targetValue = if (selected) scheme.onSurface.copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = colorSpec,
        label = "RibbonTileBorder"
    )

    Box(
        modifier = modifier
            .size(tileSize)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .gamepadFocusOutline(shape = outerShape, width = 2.dp)
            .clip(outerShape)
            .border(width = if (selected) 1.5.dp else 0.dp, color = borderColor, shape = outerShape)
            .clickable(onClick = onClick)
            .padding(if (selected) 3.dp else 0.dp)
    ) {
        val innerShape = if (selected) MaterialTheme.shapes.medium else MaterialTheme.shapes.small
        Surface(
            shape = innerShape,
            color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxSize()
        ) {
            when (entry) {
                RibbonEntry.Welcome -> WelcomeGlyphTileContent(selected = selected)
                is RibbonEntry.Game -> GameTileContent(app = entry.app, selected = selected)
                RibbonEntry.Library -> LibraryTileContent()
            }
        }
    }
}

/**
 * The signature 2x2 controller symbol quad (Triangle, Circle, Cross, Square) on the Welcome tile.
 */
@Composable
private fun WelcomeGlyphTileContent(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceContainerHighest,
                        scheme.surfaceContainerLow
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val glyphSpan = if (selected) 34.dp else 22.dp
        Canvas(modifier = Modifier.size(glyphSpan)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = if (selected) 4.2f else 3f, cap = StrokeCap.Round)
            val color = scheme.onSurface

            // Top-Left: Square
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.08f, h * 0.08f),
                size = Size(w * 0.34f, h * 0.34f),
                cornerRadius = CornerRadius(3f, 3f),
                style = stroke
            )
            // Top-Right: Triangle
            val tri = Path().apply {
                moveTo(w * 0.75f, h * 0.08f)
                lineTo(w * 0.93f, h * 0.42f)
                lineTo(w * 0.57f, h * 0.42f)
                close()
            }
            drawPath(path = tri, color = color, style = stroke)

            // Bottom-Left: Cross (X)
            drawLine(
                color = color,
                start = Offset(w * 0.08f, h * 0.58f),
                end = Offset(w * 0.42f, h * 0.92f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(w * 0.42f, h * 0.58f),
                end = Offset(w * 0.08f, h * 0.92f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )

            // Bottom-Right: Circle
            drawCircle(
                color = color,
                radius = w * 0.17f,
                center = Offset(w * 0.75f, h * 0.75f),
                style = stroke
            )
        }
    }
}

@Composable
private fun GameTileContent(app: ApplistViewmodel.AppInfo, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppIconImage(app = app, size = if (selected) 82.dp else 54.dp)
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                scheme.surfaceContainerLowest.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LibraryTileContent() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.GridView,
            contentDescription = stringResource(R.string.gamespace_library_title),
            tint = scheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Stage 1: ZXGameSpace Welcome bento grid (matches Image 1).
 * 3-column bento layout:
 * - Left: Active Profiles card (with 4 circular medallions) + Featured Banner card
 * - Center: Performance & Thermal card + Optimization Trophies card (4 trophy counters + progress bar)
 * - Right: 3 Circular Hardware Rings card (Battery, RAM, Display Hz) + Spotlight card
 */
@Composable
private fun WelcomeHubStage(
    profile: String,
    profileValue: String,
    games: List<ApplistViewmodel.AppInfo>,
    allAppsCount: Int,
    battery: Pair<Int, Boolean>,
    ramState: Pair<Float, String>,
    refreshHz: Int,
    deviceName: String,
    chipsetName: String,
    onApplyProfile: (String) -> Unit,
    onOpenSystem: () -> Unit,
    onSelectGame: (ApplistViewmodel.AppInfo) -> Unit,
    onLaunchGame: (ApplistViewmodel.AppInfo) -> Unit
) {
    val tunedCount = remember(games) { games.count { it.isEnabledInConfig } }
    val featuredGame = games.firstOrNull()
    val spotlightGame = games.getOrNull(1) ?: featuredGame

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper open stage breathing room so the contour waves and symbols show through.
        Spacer(Modifier.weight(0.22f))

        Row(
            modifier = Modifier
                .weight(0.78f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Column 1: Active Profiles + Store/Engine Banner
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActiveProfilesBentoCard(
                    profile = profile,
                    profileValue = profileValue,
                    tunedCount = tunedCount,
                    chipsetName = chipsetName,
                    deviceName = deviceName,
                    onClick = {
                        val next = when (profileValue) {
                            "1" -> "2"
                            "2" -> "3"
                            else -> "1"
                        }
                        onApplyProfile(next)
                    },
                    modifier = Modifier.weight(1.38f).fillMaxWidth()
                )

                EngineBannerBentoCard(
                    featuredGame = featuredGame,
                    deviceName = deviceName,
                    onClick = {
                        if (featuredGame != null) onSelectGame(featuredGame) else onOpenSystem()
                    },
                    modifier = Modifier.weight(0.72f).fillMaxWidth()
                )
            }

            // Column 2: Performance & Thermal + Optimization Trophies
            Column(
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PerfThermalBentoCard(
                    onClick = onOpenSystem,
                    modifier = Modifier.weight(0.76f).fillMaxWidth()
                )

                OptimizationTrophiesBentoCard(
                    profile = profile,
                    profileValue = profileValue,
                    tunedCount = tunedCount,
                    gamesCount = games.size,
                    allAppsCount = allAppsCount,
                    onClick = onOpenSystem,
                    modifier = Modifier.weight(1.34f).fillMaxWidth()
                )
            }

            // Column 3: 3 Circular Hardware Rings + Spotlight Game Card
            Column(
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HardwareRingsBentoCard(
                    battery = battery,
                    ramState = ramState,
                    refreshHz = refreshHz,
                    onClick = onOpenSystem,
                    modifier = Modifier.weight(0.76f).fillMaxWidth()
                )

                SpotlightBentoCard(
                    game = spotlightGame,
                    chipsetName = chipsetName,
                    onClick = {
                        if (spotlightGame != null) onLaunchGame(spotlightGame) else onOpenSystem()
                    },
                    modifier = Modifier.weight(1.34f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ActiveProfilesBentoCard(
    profile: String,
    profileValue: String,
    tunedCount: Int,
    chipsetName: String,
    deviceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.SportsEsports,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.gamespace_active_profiles),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = tunedCount.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
            }

            // 4 circular profile/avatar medallions mirroring the 4 friend avatars in Image 1.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileCircleMedallion(
                    iconRes = R.drawable.ic_performance,
                    active = profileValue == "1",
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer
                )
                ProfileCircleMedallion(
                    iconRes = R.drawable.ic_balanced,
                    active = profileValue == "2" || profileValue.isBlank(),
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                )
                ProfileCircleMedallion(
                    iconRes = R.drawable.ic_eco,
                    active = profileValue == "3",
                    containerColor = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer
                )
                Image(
                    painter = painterResource(R.drawable.avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, scheme.outlineVariant, CircleShape)
                )
            }

            Text(
                text = "$profile · $chipsetName\n$deviceName",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileCircleMedallion(
    iconRes: Int,
    active: Boolean,
    containerColor: Color,
    contentColor: Color
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .size(40.dp)
            .then(
                if (active) Modifier.border(2.dp, scheme.primary, CircleShape)
                else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EngineBannerBentoCard(
    featuredGame: ApplistViewmodel.AppInfo?,
    deviceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh,
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.banner_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.55f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.92f),
                                scheme.surfaceContainerLowest.copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.gamespace_engine_store),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }

                Text(
                    text = featuredGame?.label ?: deviceName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ZXGameSpaceDualBadge(left = "AZENITH", right = "M3")
            }
        }
    }
}

@Composable
private fun PerfThermalBentoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.gamespace_perf_thermal),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }

                Text(
                    text = stringResource(R.string.gamespace_perf_thermal_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { rotationZ = -14f }
                    .border(2.dp, scheme.primary.copy(alpha = 0.6f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizationTrophiesBentoCard(
    profile: String,
    profileValue: String,
    tunedCount: Int,
    gamesCount: Int,
    allAppsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val ratio = remember(tunedCount, gamesCount) {
        if (gamesCount > 0) (tunedCount.toFloat() / gamesCount.toFloat()).coerceIn(0.08f, 1f)
        else 0.33f
    }
    val percent = (ratio * 100f).roundToInt()
    val profileLevel = when (profileValue) {
        "1" -> 3
        "2" -> 2
        "3" -> 1
        else -> 2
    }

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.gamespace_trophies_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.gamespace_total_format, allAppsCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface
                )
            }

            // 4 Trophy-style stat columns (Platinum, Gold, Silver, Bronze tones).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrophyStatColumn(
                    icon = Icons.Rounded.EmojiEvents,
                    tint = scheme.primary,
                    count = tunedCount
                )
                TrophyStatColumn(
                    icon = Icons.Rounded.WorkspacePremium,
                    tint = scheme.tertiary,
                    count = gamesCount
                )
                TrophyStatColumn(
                    icon = Icons.Rounded.Speed,
                    tint = scheme.secondary,
                    count = profileLevel
                )
                TrophyStatColumn(
                    icon = Icons.Rounded.Apps,
                    tint = scheme.onSurfaceVariant,
                    count = allAppsCount
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = scheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = profile.ifBlank { "—" },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                // Bottom progress bar on Material 3 tokens.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(ratio)
                            .clip(CircleShape)
                            .background(scheme.onSurface)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrophyStatColumn(
    icon: ImageVector,
    tint: Color,
    count: Int
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.16f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface
        )
    }
}

/**
 * 3 Circular Hardware Rings Card (mirrors the 3 Controller Battery Rings in Image 1).
 */
@Composable
private fun HardwareRingsBentoCard(
    battery: Pair<Int, Boolean>,
    ramState: Pair<Float, String>,
    refreshHz: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularTelemetryGauge(
                progress = (battery.first / 100f).coerceIn(0.05f, 1f),
                arcColor = scheme.primary,
                icon = Icons.Rounded.SportsEsports,
                subLabel = "${battery.first}%"
            )
            CircularTelemetryGauge(
                progress = ramState.first.coerceIn(0.05f, 1f),
                arcColor = scheme.tertiary,
                icon = Icons.Rounded.Memory,
                subLabel = ramState.second
            )
            CircularTelemetryGauge(
                progress = (refreshHz / 144f).coerceIn(0.25f, 1f),
                arcColor = scheme.secondary,
                icon = Icons.Rounded.Speed,
                subLabel = "${refreshHz}Hz"
            )
        }
    }
}

@Composable
private fun CircularTelemetryGauge(
    progress: Float,
    arcColor: Color,
    icon: ImageVector,
    subLabel: String
) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.surfaceContainerHighest

    Box(
        modifier = Modifier.size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpotlightBentoCard(
    game: ApplistViewmodel.AppInfo?,
    chipsetName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh,
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.schemeillust),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.65f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.72f),
                                Color.Transparent,
                                scheme.surfaceContainerLowest.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.gamespace_spotlight),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = game?.label ?: chipsetName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ZXGameSpaceDualBadge(
                        left = "AZENITH",
                        right = if (game?.isEnabledInConfig == true) "TUNED" else "GAME"
                    )
                }
            }
        }
    }
}

/**
 * Dual-tone split badge mirroring the compact platform tag pill.
 */
@Composable
private fun ZXGameSpaceDualBadge(left: String, right: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .border(1.dp, scheme.outlineVariant, MaterialTheme.shapes.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = scheme.surface,
            modifier = Modifier
                .background(scheme.onSurface)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Text(
            text = right,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = scheme.onSurface,
            modifier = Modifier
                .background(scheme.surfaceContainerHighest)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ZXGameSpacePlatformTag(text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = scheme.onSurface,
        contentColor = scheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Stage 2: ZXGameSpace game hero stage (matches Image 2).
 * - Left hero column: Large title, subtitle, pill "Play Game" button + circular "..." button
 * - Right floating status pills: `[100%]` `[120Hz]`
 * - Bottom peeking cards row: "Optimization Status" card + "AZenith Game Boost" card
 */
@Composable
private fun GameHeroStage(
    app: ApplistViewmodel.AppInfo,
    profile: String,
    refreshHz: Int,
    onLaunch: () -> Unit,
    onOpenTune: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val colorSpec = motion.defaultEffectsSpec<Color>()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Hero Column: Emblem/Title + Subtitle + [Play Game] [...]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppIconImage(app = app, size = 52.dp)
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = buildString {
                        append(
                            stringResource(
                                if (app.isEnabledInConfig) R.string.gamespace_tuned
                                else R.string.gamespace_untuned
                            )
                        )
                        append(" · ")
                        append(app.packageName)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))

                // Play-game pill button plus the circular overflow button.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var playFocused by remember { mutableStateOf(false) }
                    val playBg by animateColorAsState(
                        targetValue = if (playFocused) {
                            scheme.primaryContainer
                        } else {
                            scheme.surfaceContainerHighest.copy(alpha = 0.88f)
                        },
                        animationSpec = colorSpec,
                        label = "PlayBtnBg"
                    )
                    val playFg by animateColorAsState(
                        targetValue = if (playFocused) scheme.onPrimaryContainer else scheme.onSurface,
                        animationSpec = colorSpec,
                        label = "PlayBtnFg"
                    )

                    Surface(
                        shape = CircleShape,
                        color = playBg,
                        contentColor = playFg,
                        modifier = Modifier
                            .width(184.dp)
                            .height(44.dp)
                            .onFocusChanged { playFocused = it.isFocused }
                            .gamepadFocusOutline(shape = CircleShape, width = 2.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onLaunch)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.gamespace_play_game),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    var moreFocused by remember { mutableStateOf(false) }
                    val moreBg by animateColorAsState(
                        targetValue = if (moreFocused) {
                            scheme.secondaryContainer
                        } else {
                            scheme.surfaceContainerHighest.copy(alpha = 0.82f)
                        },
                        animationSpec = colorSpec,
                        label = "MoreBtnBg"
                    )

                    Surface(
                        shape = CircleShape,
                        color = moreBg,
                        contentColor = if (moreFocused) scheme.onSecondaryContainer else scheme.onSurface,
                        modifier = Modifier
                            .size(44.dp)
                            .onFocusChanged { moreFocused = it.isFocused }
                            .gamepadFocusOutline(shape = CircleShape, width = 2.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenTune)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = stringResource(R.string.gamespace_tune),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Right Floating Telemetry Pills (matches [100%] [54h] in Image 2)
            Row(
                modifier = Modifier.padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZXGameSpaceTelemetryPill(
                    icon = Icons.Rounded.BarChart,
                    text = if (app.isEnabledInConfig) "100%" else "STD"
                )
                ZXGameSpaceTelemetryPill(
                    icon = Icons.Rounded.Schedule,
                    text = "${refreshHz}Hz"
                )
            }
        }

        // Bottom Peeking Info Cards Row (matches "Trophies" and "Friends Who Play" in Image 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Bottom Card: Trophies / Optimization Breakdown
            val cardShape = MaterialTheme.shapes.large
            Surface(
                shape = cardShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .gamepadFocusOutline(shape = cardShape)
                    .clip(cardShape)
                    .clickable(onClick = onOpenTune)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InlineTrophyBadge(
                            icon = Icons.Rounded.EmojiEvents,
                            tint = scheme.primary,
                            value = if (app.isEnabledInConfig) "1" else "0"
                        )
                        InlineTrophyBadge(
                            icon = Icons.Rounded.WorkspacePremium,
                            tint = scheme.tertiary,
                            value = profile.ifBlank { "—" }
                        )
                        InlineTrophyBadge(
                            icon = Icons.Rounded.Speed,
                            tint = scheme.secondary,
                            value = "${refreshHz}Hz"
                        )
                        InlineTrophyBadge(
                            icon = Icons.Rounded.Bolt,
                            tint = scheme.onSurfaceVariant,
                            value = "UID ${app.uid}"
                        )
                    }
                    Text(
                        text = stringResource(R.string.gamespace_trophies_title),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = scheme.onSurface
                    )
                }
            }

            // Right Bottom Card: AZenith Game Boost + Decorative Badge Matrix
            Surface(
                shape = cardShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .gamepadFocusOutline(shape = cardShape)
                    .clip(cardShape)
                    .clickable(onClick = onLaunch)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.gamespace_boost_title),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                    }

                    // 3x2 decorative glyph matrix on the right edge (mirrors Image 2 bottom-right card)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(2) { rowIdx ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) { colIdx ->
                                    val alpha = 0.12f + (colIdx + rowIdx) * 0.06f
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(MaterialTheme.shapes.extraSmall)
                                            .border(
                                                1.dp,
                                                scheme.onSurface.copy(alpha = alpha),
                                                MaterialTheme.shapes.extraSmall
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SportsEsports,
                                            contentDescription = null,
                                            tint = scheme.onSurface.copy(alpha = alpha),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZXGameSpaceTelemetryPill(icon: ImageVector, text: String) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = scheme.surfaceContainerLowest.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = Modifier.border(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.5f),
            MaterialTheme.shapes.small
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun InlineTrophyBadge(
    icon: ImageVector,
    tint: Color,
    value: String
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            maxLines = 1
        )
    }
}

/**
 * Stage 3: Full Game Library Grid when the rightmost Library tile is selected.
 * Uses Material 3 Expressive FilterChips (trailing check icon + animateContentSize).
 */
@Composable
private fun LibraryGridStage(
    games: List<ApplistViewmodel.AppInfo>,
    allApps: List<ApplistViewmodel.AppInfo>,
    onLaunch: (ApplistViewmodel.AppInfo) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    var showAllApps by remember { mutableStateOf(games.isEmpty()) }
    var onlyTuned by remember { mutableStateOf(false) }

    val baseList = if (showAllApps) allApps else games
    val displayed = remember(baseList, onlyTuned) {
        if (onlyTuned) baseList.filter { it.isEnabledInConfig } else baseList
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveFilterChip(
                selected = !showAllApps,
                label = "${stringResource(R.string.gamespace_tab_games)} (${games.size})",
                onClick = { showAllApps = false }
            )
            ExpressiveFilterChip(
                selected = showAllApps,
                label = "${stringResource(R.string.gamespace_all_apps)} (${allApps.size})",
                onClick = { showAllApps = true }
            )
            ExpressiveFilterChip(
                selected = onlyTuned,
                label = stringResource(R.string.gamespace_tuned),
                onClick = { onlyTuned = !onlyTuned }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (displayed.isEmpty()) {
            EmptyPane(
                icon = Icons.Rounded.GridView,
                titleRes = R.string.gamespace_no_games,
                bodyRes = R.string.gamespace_no_games_desc,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 148.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(displayed, key = { it.packageName }) { app ->
                    val shape = MaterialTheme.shapes.medium
                    Surface(
                        shape = shape,
                        color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
                        modifier = Modifier
                            .height(64.dp)
                            .gamepadFocusOutline(shape = shape)
                            .clip(shape)
                            .clickable { onLaunch(app) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(app = app, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        if (app.isEnabledInConfig) R.string.gamespace_tuned
                                        else R.string.gamespace_untuned
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (app.isEnabledInConfig) scheme.primary else scheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val colorSpec = motion.defaultEffectsSpec<Color>()
    val spatialSpec = motion.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()

    val container by animateColorAsState(
        targetValue = if (selected) scheme.secondaryContainer else Color.Transparent,
        animationSpec = colorSpec,
        label = "ChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        animationSpec = colorSpec,
        label = "ChipContent"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            AnimatedVisibility(visible = selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = container,
            selectedContainerColor = container,
            labelColor = content,
            selectedLabelColor = content,
            iconColor = content,
            selectedTrailingIconColor = content
        ),
        modifier = Modifier
            .animateContentSize(spatialSpec)
            .gamepadFocusOutline(shape = MaterialTheme.shapes.small)
    )
}

/**
 * Stage 4: System / Performance Control Deck when the top "System" tab is selected.
 */
@Composable
private fun SystemControlStage(
    profile: String,
    profileValue: String,
    battery: Pair<Int, Boolean>,
    ramState: Pair<Float, String>,
    refreshHz: Int,
    deviceName: String,
    chipsetName: String,
    onApplyProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.section_performance).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = scheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SystemProfileCard(
                labelRes = R.string.Profile_Performance,
                iconRes = R.drawable.ic_performance,
                active = profileValue == "1",
                onClick = { onApplyProfile("1") },
                modifier = Modifier.weight(1f)
            )
            SystemProfileCard(
                labelRes = R.string.Profile_Balanced,
                iconRes = R.drawable.ic_balanced,
                active = profileValue == "2" || profileValue.isBlank(),
                onClick = { onApplyProfile("2") },
                modifier = Modifier.weight(1f)
            )
            SystemProfileCard(
                labelRes = R.string.Profile_ECO_mode,
                iconRes = R.drawable.ic_eco,
                active = profileValue == "3",
                onClick = { onApplyProfile("3") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.88f),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.current_profile),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                    Text(
                        text = profile.ifBlank { "—" },
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = scheme.onSurface
                    )
                    Text(
                        text = "$deviceName · $chipsetName",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            HardwareRingsBentoCard(
                battery = battery,
                ramState = ramState,
                refreshHz = refreshHz,
                onClick = {},
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SystemProfileCard(
    labelRes: Int,
    iconRes: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    Surface(
        shape = shape,
        color = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (active) scheme.onPrimaryContainer else scheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Tonal badge over a headline: resting empty state. */
@Composable
internal fun EmptyPane(
    icon: ImageVector,
    titleRes: Int,
    bodyRes: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(280.dp)
            )
        }
    }
}

@Composable
private fun rememberClock(): String {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(10_000)
        }
    }
    return remember(now) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
}

@Composable
private fun rememberBatteryState(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readBattery(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            state = readBattery(context)
            delay(30_000)
        }
    }
    return state
}

private fun readBattery(context: Context): Pair<Int, Boolean> {
    val manager = context.getSystemService(BatteryManager::class.java) ?: return 0 to false
    val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val status = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    return level to charging
}

@Composable
private fun rememberRamState(): Pair<Float, String> {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readRam(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            state = readRam(context)
            delay(15_000)
        }
    }
    return state
}

private fun readRam(context: Context): Pair<Float, String> {
    val am = context.getSystemService(ActivityManager::class.java) ?: return 0.5f to "—"
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    if (info.totalMem <= 0L) return 0.5f to "—"
    val used = (info.totalMem - info.availMem).coerceAtLeast(0L)
    val fraction = (used.toFloat() / info.totalMem.toFloat()).coerceIn(0f, 1f)
    val usedGb = used.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return fraction to String.format(java.util.Locale.US, "%.1fG", usedGb)
}
