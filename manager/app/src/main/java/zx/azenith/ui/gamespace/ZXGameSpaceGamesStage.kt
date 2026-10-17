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

package zx.azenith.ui.gamespace

import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Token
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.ui.util.getGameWallpaperMode
import zx.azenith.ui.util.setGameWallpaperMode
import zx.azenith.ui.util.getGameWallpaperUri
import zx.azenith.ui.util.setGameWallpaperUri
import zx.azenith.ui.util.saveMediaDirectly
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.MediaBannerRenderer
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.viewmodel.ApplistViewmodel
import zx.azenith.ui.util.getHeaderImage

internal const val HOME_EXPAND_AT = 90

/**
 * Top horizontal ribbon of app and section tiles.
 * - Unselected tiles are compact 54dp rounded squares aligned to the top of the ribbon.
 * - Selected/focused tile expands smoothly to 88dp with a crisp outer focus ring.
 * - The selected item's badge + title sits immediately to the right of the expanded tile,
 *   directly underneath the adjacent compact tiles, matching the ZXGameSpace home layout.
 */
@Composable
internal fun ZXGameSpaceTileRibbon(
    entries: List<RibbonEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onActivate: (RibbonEntry) -> Unit,
    focusRequester: FocusRequester? = null
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val itemPitchPx = with(density) { 62.dp.toPx() }
    val expandedRightBasePx = with(density) { (88.dp + 12.dp).toPx() }

    val carouselPos = remember { Animatable(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(selectedIndex, isDragging) {
        if (!isDragging) {
            carouselPos.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Haptic feedback on each game scrolled/passed
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(carouselPos.value) {
        val currentNearest = carouselPos.value.roundToInt().coerceIn(0, entries.lastIndex.coerceAtLeast(0))
        if (currentNearest != lastHapticIndex) {
            lastHapticIndex = currentNearest
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val visualIndex = carouselPos.value.roundToInt().coerceIn(0, entries.lastIndex.coerceAtLeast(0))
    val activeEntry = entries.getOrElse(visualIndex) { RibbonEntry.Welcome }

    val libraryIndex = entries.indexOf(RibbonEntry.Library)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .pointerInput(entries.size) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        val effectiveMaxIdx = if (currentSelectedIndex >= libraryIndex && libraryIndex >= 0) {
                            entries.lastIndex.coerceAtLeast(0)
                        } else if (libraryIndex >= 0) {
                            libraryIndex
                        } else {
                            entries.lastIndex.coerceAtLeast(0)
                        }
                        val target = carouselPos.value.roundToInt().coerceIn(0, effectiveMaxIdx)
                        if (target != currentSelectedIndex) {
                            currentOnSelect(target)
                        }
                        scope.launch {
                            carouselPos.animateTo(
                                targetValue = target.toFloat(),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val effectiveMaxIdx = if (currentSelectedIndex >= libraryIndex && libraryIndex >= 0) {
                            entries.lastIndex.coerceAtLeast(0)
                        } else if (libraryIndex >= 0) {
                            libraryIndex
                        } else {
                            entries.lastIndex.coerceAtLeast(0)
                        }
                        val nextPos = (carouselPos.value - (dragAmount / itemPitchPx))
                            .coerceIn(0f, effectiveMaxIdx.toFloat())
                        scope.launch {
                            carouselPos.snapTo(nextPos)
                        }
                        val nearest = nextPos.roundToInt().coerceIn(0, effectiveMaxIdx)
                        if (nearest != currentSelectedIndex) {
                            currentOnSelect(nearest)
                        }
                    }
                )
            }
    ) {
        val scrollItems = (carouselPos.value - 2f).coerceAtLeast(0f)
        val totalOffsetPx = -scrollItems * itemPitchPx

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .offset { IntOffset(x = totalOffsetPx.roundToInt(), y = 0) }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .gamepadScreenEntry(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            val showWrapTile = selectedIndex >= libraryIndex && libraryIndex >= 0
            entries.forEachIndexed { index, item ->
                val dist = (carouselPos.value - index).absoluteValue
                val selectionFraction = (1f - dist).coerceIn(0f, 1f)
                val isSelected = index == selectedIndex
                if (item is RibbonEntry.WrapToFirst) {
                    AnimatedVisibility(
                        visible = showWrapTile,
                        enter = fadeIn(motion.defaultEffectsSpec()) + expandHorizontally(motion.defaultSpatialSpec()),
                        exit = fadeOut(motion.defaultEffectsSpec()) + shrinkHorizontally(motion.defaultSpatialSpec())
                    ) {
                        ZXGameSpaceRibbonTile(
                            entry = item,
                            selectionFraction = selectionFraction,
                            selected = isSelected,
                            onFocus = { onSelect(index) },
                            onClick = {
                                if (isSelected) {
                                    onActivate(item)
                                } else {
                                    onSelect(index)
                                }
                            }
                        )
                    }
                } else {
                    ZXGameSpaceRibbonTile(
                        entry = item,
                        selectionFraction = selectionFraction,
                        selected = isSelected,
                        onFocus = { onSelect(index) },
                        onClick = {
                            if (isSelected) {
                                onActivate(item)
                            } else {
                                onSelect(index)
                            }
                        }
                    )
                }
            }
        }

        // Adjacent label sits just right of the expanded tile's lower edge and stays anchored at slot 3.
        val visualSlot = carouselPos.value.coerceIn(0f, 2f)
        val labelOffsetPx = ((visualSlot * itemPitchPx) + expandedRightBasePx).roundToInt()

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
                    // No ENABLED label
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

                RibbonEntry.WrapToFirst -> {
                    Text(
                        text = stringResource(R.string.gamespace_back_to_start),
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
internal fun ZXGameSpaceRibbonTile(
    entry: RibbonEntry,
    selectionFraction: Float,
    selected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    val tileSize = lerp(54.dp, 88.dp, selectionFraction)
    val outerRadius = lerp(16.dp, 24.dp, selectionFraction)
    val innerRadius = lerp(12.dp, 18.dp, selectionFraction)
    val outerShape = RoundedCornerShape(outerRadius)
    val innerShape = RoundedCornerShape(innerRadius)

    val borderColor = scheme.onSurface.copy(alpha = (0.85f * selectionFraction).coerceIn(0f, 1f))
    val borderWidth = lerp(0.dp, 1.5.dp, selectionFraction)
    val tilePadding = lerp(0.dp, 3.dp, selectionFraction)

    Box(
        modifier = modifier
            .size(tileSize)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .gamepadFocusOutline(shape = outerShape, width = 2.dp)
            .clip(outerShape)
            .then(
                if (borderWidth > 0.2.dp) {
                    Modifier.border(width = borderWidth, color = borderColor, shape = outerShape)
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(tilePadding)
    ) {
        Surface(
            shape = innerShape,
            color = scheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxSize()
        ) {
            when (entry) {
                RibbonEntry.Welcome -> WelcomeGlyphTileContent(selectedFraction = selectionFraction)
                is RibbonEntry.Game -> GameTileContent(app = entry.app, selectedFraction = selectionFraction)
                RibbonEntry.Library -> LibraryTileContent(selectedFraction = selectionFraction)
                RibbonEntry.WrapToFirst -> WrapToFirstTileContent(selectedFraction = selectionFraction)
            }
        }
    }
}

/**
 * The signature 2x2 controller symbol quad (Triangle, Circle, Cross, Square) on the Welcome tile.
 */
@Composable
internal fun WelcomeGlyphTileContent(selectedFraction: Float) {
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
        val glyphSpan = lerp(22.dp, 34.dp, selectedFraction)
        Canvas(modifier = Modifier.size(glyphSpan)) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 3f + (1.2f * selectedFraction), cap = StrokeCap.Round)
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
internal fun GameTileContent(app: ApplistViewmodel.AppInfo, selectedFraction: Float) {
    val iconSize = lerp(44.dp, 82.dp, selectedFraction)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppIconImage(app = app, size = iconSize)
    }
}

@Composable
internal fun LibraryTileContent(selectedFraction: Float = 0f) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val iconSize = lerp(24.dp, 34.dp, selectedFraction)
        Icon(
            imageVector = Icons.Rounded.GridView,
            contentDescription = stringResource(R.string.gamespace_library_title),
            tint = scheme.onSurface,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun WrapToFirstTileContent(selectedFraction: Float = 0f) {
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
        val iconSize = lerp(26.dp, 38.dp, selectedFraction)
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = stringResource(R.string.gamespace_back_to_start),
            tint = if (selectedFraction > 0.5f) scheme.primary else scheme.onSurface,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Terminal wrap-around stage that returns the user directly to the home screen.
 */
@Composable
internal fun WrapToFirstStage(
    onReturnToFirst: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    var buttonFocused by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (buttonFocused) 1.05f else 1f,
        animationSpec = motion.defaultSpatialSpec(),
        label = "ReturnBtnScale"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.gamespace_back_to_start),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.gamespace_back_to_start_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .height(64.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                    .onFocusChanged { buttonFocused = it.isFocused }
                    .gamepadFocusOutline(shape = CircleShape, width = 3.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onReturnToFirst)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.gamespace_back_to_start),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Stage 1: the home sheet. The bento wall used to sit in the stage's leftover height and clipped
 * at its edge, so what the normal-mode home reports is carried here instead -- one widget per
 * reading, and the sheet scrolls rather than cutting anything off.
 */
@Composable
internal fun WelcomeHubStage(
    scroll: ScrollState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    profile: String,
    profileValue: String,
    games: List<ApplistViewmodel.AppInfo>,
    allAppsCount: Int,
    refreshHz: Int,
    deviceName: String,
    chipsetName: String,
    onApplyProfile: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectGame: (ApplistViewmodel.AppInfo) -> Unit,
    onLaunchGame: (ApplistViewmodel.AppInfo) -> Unit,
    onOpenLibrary: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    val wordmark = stringResource(R.string.app_name)
    val scope = rememberCoroutineScope()
    val expandedNow = rememberUpdatedState(expanded)
    val notifyExpanded = rememberUpdatedState(onExpandedChange)
    var stageHold by remember { mutableStateOf(false) }

    val stageGate = remember(scroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val fromDrag = source == NestedScrollSource.UserInput
                if (stageHold && fromDrag && available.y < 0f) return available
                if (!expandedNow.value && available.y < 0f) {
                    val projected = scroll.value + available.y
                    if (projected >= HOME_EXPAND_AT) {
                        if (fromDrag) stageHold = true
                        notifyExpanded.value(true)
                        scope.launch { runCatching { scroll.animateScrollTo(0) } }
                        return Offset(0f, projected - HOME_EXPAND_AT)
                    }
                }
                if (expandedNow.value && fromDrag && available.y > 0f && scroll.value <= 0) {
                    stageHold = false
                    notifyExpanded.value(false)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
        }
    }

    val rootGranted by produceState<Boolean?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { RootUtils.isRootGranted() }
    }
    val moduleInstalled by produceState<Boolean?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { RootUtils.isModuleInstalled() }
    }
    val service by RootUtils.observeServiceStatusRes()
        .collectAsState(initial = R.string.status_initializing to "")

    val rootValue = when (rootGranted) {
        true -> stringResource(R.string.root_granted)
        false -> stringResource(R.string.root_not_granted)
        else -> stringResource(R.string.status_checking)
    }
    val moduleValue = when (moduleInstalled) {
        true -> stringResource(service.first)
        false -> stringResource(R.string.module_not_installed)
        else -> stringResource(R.string.status_checking)
    }
    val suspended = stringResource(R.string.status_suspended)
    val pidValue = service.second.ifBlank { suspended }

    val topRoom by animateDpAsState(
        targetValue = if (expanded) 64.dp else 142.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.Dp>(),
        label = "WelcomeTopRoom"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(stageGate)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change ->
                            if (change.changedToDown()) stageHold = false
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topRoom)
                    .clipToBounds(),
                contentAlignment = Alignment.BottomStart
            ) {
                HomeWordmark(
                    scroll = scroll,
                    held = expanded,
                    text = wordmark,
                    deviceName = deviceName,
                    chipsetName = chipsetName
                )
            }

            // Row 1: Core System & Performance Status (4 balanced cards across the width)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = if (rootGranted == true) Icons.Rounded.VerifiedUser else Icons.Rounded.Security,
                    label = stringResource(R.string.root_access),
                    value = rootValue,
                    container = if (rootGranted == false) scheme.errorContainer else scheme.primaryContainer,
                    onContainer = if (rootGranted == false) scheme.onErrorContainer else scheme.onPrimaryContainer
                )
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Memory,
                    label = stringResource(R.string.gamespace_info_module),
                    value = moduleValue,
                    container = if (moduleInstalled == false) scheme.errorContainer else scheme.secondaryContainer,
                    onContainer = if (moduleInstalled == false) scheme.onErrorContainer else scheme.onSecondaryContainer
                )
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Tune,
                    label = stringResource(R.string.gamespace_info_pid),
                    value = pidValue,
                    container = scheme.tertiaryContainer,
                    onContainer = scheme.onTertiaryContainer
                )
                HubInfoTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Token,
                    label = stringResource(R.string.current_profile),
                    value = profile,
                    container = scheme.surfaceContainerHigh,
                    onContainer = scheme.onSurface,
                    badgeText = "CYCLE PROFILE",
                    onClick = {
                        val next = when (profileValue) {
                            "1" -> "2"
                            "2" -> "3"
                            else -> "1"
                        }
                        onApplyProfile(next)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Row 2: Landscape Bento Deck (2-column balanced layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Hardware & System Architecture Deck (55% width)
                HardwareSpecsBentoCard(
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    deviceName = deviceName,
                    chipsetName = chipsetName,
                    refreshHz = refreshHz,
                    onOpenSettings = onOpenSettings
                )

                // Column 2: Gaming Library & Quick Action Deck (45% width)
                GamingLibraryBentoCard(
                    modifier = Modifier.weight(0.9f).fillMaxHeight(),
                    gamesCount = games.size,
                    allAppsCount = allAppsCount,
                    onOpenLibrary = onOpenLibrary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Row 3: Community & Knowledge Links (Side-by-side 50/50 split)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HubLinkTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(R.string.support_us),
                    description = stringResource(R.string.support_us_desc),
                    container = scheme.secondaryContainer,
                    onContainer = scheme.onSecondaryContainer,
                    onClick = { uriHandler.openUri("https://t.me/ZeshArch") }
                )
                HubLinkTile(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    icon = Icons.Rounded.Info,
                    label = stringResource(R.string.learn_more),
                    description = stringResource(R.string.learn_more_desc),
                    container = scheme.tertiaryContainer,
                    onContainer = scheme.onTertiaryContainer,
                    onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** The wordmark at rest: hero size while the sheet is whole. */
@Composable
internal fun HomeWordmark(
    scroll: ScrollState,
    held: Boolean,
    text: String,
    deviceName: String,
    chipsetName: String
) {
    val scheme = MaterialTheme.colorScheme
    val hold by animateFloatAsState(
        targetValue = if (held) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>(),
        label = "HomeWordmarkHold"
    )

    Column(
        modifier = Modifier
            .padding(start = 24.dp, bottom = 12.dp)
            .graphicsLayer {
                val ramp = (scroll.value / HOME_EXPAND_AT.toFloat()).coerceIn(0f, 1f)
                val progress = maxOf(ramp, hold)
                alpha = (1f - progress * 1.25f).coerceIn(0f, 1f)
                val scale = 1f - 0.35f * progress
                scaleX = scale
                scaleY = scale
                translationY = -progress * 28f
                transformOrigin = TransformOrigin(0f, 1f)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    lineHeight = 74.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                ),
                color = scheme.onSurface,
                maxLines = 1
            )
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    text = "GAME SPACE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = scheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = "$deviceName \u00b7 $chipsetName",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )
    }
}

@Composable
internal fun HardwareSpecsBentoCard(
    modifier: Modifier = Modifier,
    deviceName: String,
    chipsetName: String,
    refreshHz: Int,
    onOpenSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = true)
            .clickable(onClick = onOpenSettings),
        shape = shape,
        color = scheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.device_info).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "$deviceName \u00b7 $chipsetName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Speed,
                    label = stringResource(R.string.refreshrates),
                    value = stringResource(R.string.refresh_rate_format, refreshHz.toString())
                )
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Android,
                    label = stringResource(R.string.android_version),
                    value = "Android ${Build.VERSION.RELEASE}"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Memory,
                    label = "ARCH",
                    value = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
                )
                SpecPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Check,
                    label = "SECURITY",
                    value = Build.VERSION.SECURITY_PATCH.ifBlank { "Up to date" }
                )
            }
        }
    }
}

@Composable
internal fun SpecPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun GamingLibraryBentoCard(
    modifier: Modifier = Modifier,
    gamesCount: Int,
    allAppsCount: Int,
    onOpenLibrary: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = true)
            .clickable(onClick = onOpenLibrary),
        shape = shape,
        color = scheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.tertiaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = scheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.gamespace_library_title).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.tertiary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "$gamesCount ${stringResource(R.string.gamespace_info_games).lowercase()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainerHighest
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.gamespace_info_games).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$gamesCount Tuned",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.surfaceContainerHighest
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.gamespace_info_apps).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$allAppsCount Apps",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = scheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.gamespace_library_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun HubInfoTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    container: Color,
    onContainer: Color,
    badgeText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = onClick != null)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = shape,
        color = container
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeText != null) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = onContainer.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun HubLinkTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    description: String,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .gamepadFocusOutline(shape = shape, register = true)
            .clickable(onClick = onClick),
        shape = shape,
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = onContainer.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = onContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = onContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun ActiveProfilesBentoCard(
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
        color = scheme.surfaceContainerHigh,
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
internal fun ProfileCircleMedallion(
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
internal fun EngineBannerBentoCard(
    featuredGame: ApplistViewmodel.AppInfo?,
    deviceName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    val context = LocalContext.current
    val customWallpaperUri = remember { context.getHeaderImage() }

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh,
        modifier = modifier
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (customWallpaperUri != null) {
                MediaBannerRenderer(
                    uriString = customWallpaperUri,
                    modifier = Modifier.fillMaxSize().alpha(0.70f)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.banner_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.55f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                scheme.surfaceContainerLowest.copy(alpha = 0.82f),
                                scheme.surfaceContainerLowest.copy(alpha = 0.20f)
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
internal fun PerfThermalBentoCard(
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
internal fun OptimizationTrophiesBentoCard(
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
        color = scheme.surfaceContainerHigh,
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

            // 4 Trophy-style stat columns
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
internal fun TrophyStatColumn(
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

@Composable
internal fun HardwareRingsBentoCard(
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
        color = scheme.surfaceContainerHigh,
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
internal fun CircularTelemetryGauge(
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
internal fun SpotlightBentoCard(
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
                        right = stringResource(
                            if (game?.isEnabledInConfig == true) R.string.gamespace_tuned
                            else R.string.gamespace_untuned
                        ).uppercase()
                    )
                }
            }
        }
    }
}

/**
 * Stage 2: ZXGameSpace game hero stage.
 * - Left hero column: icon, title, tuned/untuned line with the package name
 * - Far right corner: Play and App Settings
 */
@Composable
internal fun GameHeroStage(
    app: ApplistViewmodel.AppInfo,
    onLaunch: () -> Unit,
    onOpenAppSettings: () -> Unit
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
                .weight(1f)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val versionName = app.packageInfo.versionName?.takeIf { it.isNotBlank() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppIconImage(app = app, size = 56.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (versionName != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.secondaryContainer,
                                    contentColor = scheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = "v$versionName",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            var showWallpaperDialog by remember { mutableStateOf(false) }

            if (showWallpaperDialog) {
                GameWallpaperDialog(
                    app = app,
                    onDismiss = { showWallpaperDialog = false }
                )
            }

            var penFocused by remember { mutableStateOf(false) }
            val penBg by animateColorAsState(
                targetValue = if (penFocused) scheme.secondaryContainer else scheme.surfaceContainerHigh,
                animationSpec = colorSpec,
                label = "PenBtnBg"
            )

            var gearFocused by remember { mutableStateOf(false) }
            val gearBg by animateColorAsState(
                targetValue = if (gearFocused) scheme.secondaryContainer else scheme.surfaceContainerHigh,
                animationSpec = colorSpec,
                label = "GearBtnBg"
            )

            var playFocused by remember { mutableStateOf(false) }
            val playScale by animateFloatAsState(
                targetValue = if (playFocused) 1.05f else 1f,
                animationSpec = motion.defaultSpatialSpec(),
                label = "PlayBtnScale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Two-stack of circular action buttons: Pen (wallpaper) & Gear (settings)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: Pen icon (Personalize game wallpaper)
                    Surface(
                        shape = CircleShape,
                        color = penBg,
                        contentColor = if (penFocused) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(34.dp)
                            .onFocusChanged { penFocused = it.isFocused }
                            .gamepadFocusOutline(shape = CircleShape, width = 2.dp)
                            .clip(CircleShape)
                            .clickable { showWallpaperDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Personalize Wallpaper",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Bottom: Original app settings gear icon
                    Surface(
                        shape = CircleShape,
                        color = gearBg,
                        contentColor = if (gearFocused) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(34.dp)
                            .onFocusChanged { gearFocused = it.isFocused }
                            .gamepadFocusOutline(shape = CircleShape, width = 2.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenAppSettings)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.gamespace_app_settings),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                val playButtonShape = RoundedCornerShape(28.dp)

                // Enlarged expressive colorful Play button (50% larger with rounded corner square shape)
                Surface(
                    shape = playButtonShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .height(108.dp)
                        .graphicsLayer {
                            scaleX = playScale
                            scaleY = playScale
                        }
                        .onFocusChanged { playFocused = it.isFocused }
                        .gamepadFocusOutline(shape = playButtonShape, width = 3.dp)
                        .clip(playButtonShape)
                        .clickable(onClick = onLaunch)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 42.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Expressive Dialog allowing user to personalize the game wallpaper.
 * Offers Default Game Art, Follow Home Wallpaper, and Custom Wallpaper (with MediaPicker).
 */
@Composable
internal fun GameWallpaperDialog(
    app: ApplistViewmodel.AppInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var selectedMode by remember { mutableStateOf(context.getGameWallpaperMode(app.packageName)) }
    var customUri by remember { mutableStateOf(context.getGameWallpaperUri(app.packageName)) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            scope.launch(Dispatchers.IO) {
                val mimeType = context.contentResolver.getType(sourceUri) ?: ""
                val isVideo = mimeType.startsWith("video/")
                val extension = when {
                    isVideo -> "mp4"
                    mimeType == "image/gif" -> "gif"
                    mimeType == "image/png" -> "png"
                    mimeType == "image/webp" -> "webp"
                    else -> "jpg"
                }
                val savedUri = context.saveMediaDirectly(sourceUri, extension)
                if (savedUri != null) {
                    context.setGameWallpaperUri(app.packageName, savedUri)
                    context.setGameWallpaperMode(app.packageName, "custom")
                    withContext(Dispatchers.Main) {
                        customUri = savedUri
                        selectedMode = "custom"
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Game Wallpaper",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = scheme.onSurface
                        )
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHighest,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Select stage background style when viewing this game:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )

                // Option 1: Default Game Artwork (App icon & blur)
                val isDefault = selectedMode == "default"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDefault) scheme.primaryContainer else scheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            selectedMode = "default"
                            context.setGameWallpaperMode(app.packageName, "default")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDefault) scheme.primary else scheme.surfaceContainerHighest,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    tint = if (isDefault) scheme.onPrimary else scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Game Art",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDefault) scheme.onPrimaryContainer else scheme.onSurface
                            )
                            Text(
                                text = "Dynamic atmospheric background with game icon",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDefault) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = if (isDefault) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isDefault) scheme.primary else scheme.outlineVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Option 2: Follow Home Wallpaper
                val isFollowHome = selectedMode == "follow_home"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isFollowHome) scheme.primaryContainer else scheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            selectedMode = "follow_home"
                            context.setGameWallpaperMode(app.packageName, "follow_home")
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isFollowHome) scheme.primary else scheme.surfaceContainerHighest,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = if (isFollowHome) scheme.onPrimary else scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Follow Home Wallpaper",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isFollowHome) scheme.onPrimaryContainer else scheme.onSurface
                            )
                            Text(
                                text = "Match the background wallpaper set on the Home tab",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFollowHome) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = if (isFollowHome) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isFollowHome) scheme.primary else scheme.outlineVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Option 3: Custom Wallpaper
                val isCustom = selectedMode == "custom"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isCustom) scheme.primaryContainer else scheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            if (customUri != null) {
                                selectedMode = "custom"
                                context.setGameWallpaperMode(app.packageName, "custom")
                            } else {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCustom) scheme.primary else scheme.surfaceContainerHighest,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = if (isCustom) scheme.onPrimary else scheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Wallpaper",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCustom) scheme.onPrimaryContainer else scheme.onSurface
                                )
                                Text(
                                    text = if (customUri != null) "Personalized photo, GIF, or video" else "Choose an image or video for this game",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCustom) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = if (isCustom) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isCustom) scheme.primary else scheme.outlineVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (customUri != null) {
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                MediaBannerRenderer(
                                    uriString = customUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Change")
                                }
                                TextButton(
                                    onClick = {
                                        context.setGameWallpaperUri(app.packageName, null)
                                        customUri = null
                                        selectedMode = "default"
                                        context.setGameWallpaperMode(app.packageName, "default")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = scheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reset", color = scheme.error)
                                }
                            }
                        }
                    }
                }

                // Done button
                Surface(
                    shape = CircleShape,
                    color = scheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = scheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

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
    onNavigateUp: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    var showAllApps by remember { mutableStateOf(games.isEmpty()) }
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

    Column(modifier = Modifier.fillMaxSize().nestedScroll(gridPull)) {
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveFilterChip(
                selected = !showAllApps,
                label = "${stringResource(R.string.gamespace_tab_games)} (${games.size})",
                leadingIcon = Icons.Rounded.SportsEsports,
                onClick = { showAllApps = false }
            )
            ExpressiveFilterChip(
                selected = showAllApps,
                label = "${stringResource(R.string.gamespace_all_apps)} (${allApps.size})",
                leadingIcon = Icons.Rounded.Apps,
                onClick = { showAllApps = true }
            )
            ExpressiveFilterChip(
                selected = onlyEnabled,
                label = stringResource(R.string.gamespace_tuned),
                leadingIcon = Icons.Rounded.Tune,
                onClick = { onlyEnabled = !onlyEnabled }
            )
        }

        Spacer(Modifier.height(10.dp))

        if (displayed.isEmpty()) {
            EmptyPane(
                icon = Icons.Rounded.GridView,
                titleRes = R.string.gamespace_no_games,
                bodyRes = R.string.gamespace_no_games_desc,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            val motion = MaterialTheme.motionScheme
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                items(displayed, key = { it.packageName }) { app ->
                    val shape = RoundedCornerShape(20.dp)
                    Surface(
                        shape = shape,
                        color = scheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                            .animateItem(placementSpec = motion.defaultSpatialSpec())
                            .gamepadFocusOutline(shape = shape)
                            .clip(shape)
                            .clickable { onOpenSettings(app) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = scheme.surfaceContainerLow,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppIconImage(app = app, size = 42.dp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                if (app.isEnabledInConfig) {
                                    Surface(
                                        shape = CircleShape,
                                        color = scheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(scheme.primary)
                                            )
                                            Text(
                                                text = stringResource(R.string.gamespace_tuned),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = scheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.gamespace_untuned),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
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
    val spatialSpec = motion.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()

    val container by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = colorSpec,
        label = "ChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        animationSpec = colorSpec,
        label = "ChipContent"
    )
    val shape = RoundedCornerShape(14.dp)

    Surface(
        shape = shape,
        color = container,
        contentColor = content,
        modifier = Modifier
            .animateContentSize(spatialSpec)
            .gamepadFocusOutline(shape = shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
            AnimatedVisibility(visible = selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
