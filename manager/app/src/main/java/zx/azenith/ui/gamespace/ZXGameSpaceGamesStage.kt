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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OfflineBolt
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
import androidx.compose.material.icons.rounded.Water
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.AZenithSheet
import zx.azenith.ui.component.AppIconImage
import zx.azenith.ui.component.MediaBannerRenderer
import zx.azenith.ui.component.SheetItem
import zx.azenith.ui.component.SheetRowStyle
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadScreenEntry
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.viewmodel.AutoModeBus
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
    appScalePercent: Int = 85,
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
    onOpenLibrary: () -> Unit = {},
    onOpenHomeWallpaper: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    val wordmark = stringResource(R.string.app_name)
    val scope = rememberCoroutineScope()
    val expandedNow = rememberUpdatedState(expanded)
    val notifyExpanded = rememberUpdatedState(onExpandedChange)
    var dragExpandDistance by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val scaleRatio = (100f / appScalePercent.coerceIn(60, 150))
    val expandThresholdPx = with(density) { (32.dp * scaleRatio).coerceIn(28.dp, 54.dp).toPx() }

    LaunchedEffect(expanded) {
        dragExpandDistance = 0f
        if (!expanded && scroll.value != 0) {
            scroll.scrollTo(0)
        }
    }

    val stageGate = remember(scroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val fromDrag = source == NestedScrollSource.UserInput
                if (!expandedNow.value) {
                    if (fromDrag && available.y < 0f) {
                        dragExpandDistance = (dragExpandDistance - available.y).coerceAtLeast(0f)
                        if (dragExpandDistance >= expandThresholdPx) {
                            notifyExpanded.value(true)
                            dragExpandDistance = 0f
                        }
                        return Offset(0f, available.y)
                    }
                    if (fromDrag && available.y > 0f && dragExpandDistance > 0f) {
                        dragExpandDistance = (dragExpandDistance - available.y).coerceAtLeast(0f)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }
                if (expandedNow.value && fromDrag && available.y > 0f && scroll.value <= 0) {
                    notifyExpanded.value(false)
                    dragExpandDistance = 0f
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (expandedNow.value && source == NestedScrollSource.UserInput && available.y > 0f && scroll.value <= 0) {
                    notifyExpanded.value(false)
                    dragExpandDistance = 0f
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                dragExpandDistance = 0f
                if (!expandedNow.value) {
                    if (available.y < -200f) {
                        notifyExpanded.value(true)
                        return available
                    }
                } else {
                    if (available.y > 200f && scroll.value <= 0) {
                        notifyExpanded.value(false)
                        return available
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dragExpandDistance = 0f
                if (expandedNow.value && available.y > 80f && scroll.value <= 0) {
                    notifyExpanded.value(false)
                    return available
                }
                return Velocity.Zero
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

    var autoMode by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            autoMode = PropertyUtils.get("persist.sys.azenithconf.AIenabled")
        }
    }
    LaunchedEffect(Unit) {
        RootUtils.observeAutoMode().collect { mode ->
            autoMode = mode
        }
    }
    LaunchedEffect(Unit) {
        AutoModeBus.changes.collect { mode ->
            autoMode = mode
        }
    }

    val isManualMode = autoMode == "0"
    var showProfileSheet by remember { mutableStateOf(false) }
    LaunchedEffect(isManualMode) {
        if (!isManualMode) showProfileSheet = false
    }

    var pendingProfileValue by remember { mutableStateOf<String?>(null) }
    var isProfileApplying by remember { mutableStateOf(false) }

    LaunchedEffect(profileValue) {
        if (pendingProfileValue != null && profileValue == pendingProfileValue) {
            kotlinx.coroutines.delay(180)
            isProfileApplying = false
            pendingProfileValue = null
        }
    }

    LaunchedEffect(isProfileApplying) {
        if (isProfileApplying) {
            kotlinx.coroutines.delay(4000)
            isProfileApplying = false
            pendingProfileValue = null
        }
    }

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
    val pidBadge = if (service.second.isNotBlank()) {
        stringResource(R.string.pid_format, service.second)
    } else {
        suspended
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(stageGate)
            .pointerInput(Unit) {
                awaitEachGesture {
                    try {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        dragExpandDistance = 0f
                    }
                }
            }
    ) {
        val stageH = maxHeight
        val dynamicCompactH = (stageH * 0.22f).coerceIn(52.dp, 72.dp)
        val restBottomSpacer = (14.dp + 4.dp * (scaleRatio - 0.8f)).coerceIn(12.dp, 20.dp)
        val dynamicGap = (stageH * 0.04f).coerceIn(4.dp, 12.dp)
        val dynamicRestTopRoom = (stageH - dynamicCompactH - dynamicGap - restBottomSpacer)
            .coerceAtLeast(32.dp)

        val topRoom by animateDpAsState(
            targetValue = if (expanded) 12.dp else dynamicRestTopRoom,
            animationSpec = spring(
                dampingRatio = 0.52f,
                stiffness = 240f
            ),
            label = "WelcomeTopRoom"
        )

        val targetProgress = if (expanded) 1f else 0f
        val expandProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = spring(
                dampingRatio = 0.52f,
                stiffness = 240f
            ),
            label = "BentoMorphProgress"
        )

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
                    chipsetName = chipsetName,
                    scaleRatio = scaleRatio,
                    onOpenHomeWallpaper = onOpenHomeWallpaper
                )
            }

            val wordmarkToBentoGap = lerp(dynamicGap, 0.dp, expandProgress)
            if (wordmarkToBentoGap > 0.dp) {
                Spacer(Modifier.height(wordmarkToBentoGap))
            }

            HubBentoGrid(
                expandProgress = expandProgress,
                compactHeight = dynamicCompactH,
                onExpandRequest = {
                    notifyExpanded.value(true)
                },
                moduleInstalled = moduleInstalled,
                moduleValue = moduleValue,
                servicePid = service.second.takeIf { moduleInstalled == true && it.isNotBlank() },
                rootGranted = rootGranted,
                rootValue = rootValue,
                profile = profile,
                profileValue = profileValue,
                isManualMode = isManualMode,
                isProfileApplying = isProfileApplying,
                onProfileClick = if (isManualMode && !isProfileApplying) {
                    { showProfileSheet = true }
                } else null,
                deviceName = deviceName,
                chipsetName = chipsetName,
                refreshHz = refreshHz,
                gamesCount = games.size,
                allAppsCount = allAppsCount,
                onOpenSettings = onOpenSettings,
                onOpenLibrary = onOpenLibrary,
                onSupport = { uriHandler.openUri("https://t.me/ZeshArch") },
                onLearnMore = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
            )

            val bottomSpacer = lerp(restBottomSpacer, 32.dp, expandProgress.coerceIn(0f, 1f))
            Spacer(Modifier.height(bottomSpacer))
        }
    }

    val profileOptions = listOf(
        Triple("1", R.string.Profile_Performance, Icons.Rounded.OfflineBolt),
        Triple("2", R.string.Profile_Balanced, Icons.Rounded.Water),
        Triple("3", R.string.Profile_ECO_mode, Icons.Rounded.EnergySavingsLeaf)
    )
    val profileSummaries = listOf(
        "Maximum CPU/GPU clock frequencies and low-latency governor",
        "Adaptive daily performance and thermal efficiency",
        "Power-saving frequency cap for extended battery life"
    )

    AZenithSheet(
        show = showProfileSheet,
        onDismiss = { showProfileSheet = false },
        title = stringResource(R.string.Profile_Select),
        subtitle = stringResource(R.string.current_profile),
        style = SheetRowStyle.SingleSelect,
        items = profileOptions.mapIndexed { idx, (value, titleRes, icon) ->
            SheetItem(
                label = stringResource(titleRes),
                summary = profileSummaries[idx],
                icon = icon,
                selected = profileValue == value || (value == "2" && profileValue.isBlank())
            )
        },
        onItemClick = { index ->
            if (index in profileOptions.indices) {
                val target = profileOptions[index].first
                if (target != profileValue) {
                    pendingProfileValue = target
                    isProfileApplying = true
                }
                onApplyProfile(target)
            }
            showProfileSheet = false
        }
    )
}

/** The wordmark at rest: hero size while the sheet is whole. */
@Composable
internal fun HomeWordmark(
    scroll: ScrollState,
    held: Boolean,
    text: String,
    deviceName: String,
    chipsetName: String,
    scaleRatio: Float = 1f,
    onOpenHomeWallpaper: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val hold by animateFloatAsState(
        targetValue = if (held) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = 240f
        ),
        label = "HomeWordmarkHold"
    )

    val wordmarkSizeSp = (36f + 14f * (scaleRatio - 0.7f)).coerceIn(40f, 60f)
    val wordmarkFontSize = wordmarkSizeSp.sp
    val wordmarkLineHeight = (wordmarkSizeSp + 2f).sp
    val badgeTopPad = (2.dp + 4.dp * (scaleRatio - 0.8f)).coerceIn(2.dp, 8.dp)
    val penSize = (26.dp + 6.dp * (scaleRatio - 0.8f)).coerceIn(26.dp, 34.dp)

    Column(
        modifier = Modifier
            .padding(start = 24.dp, bottom = 4.dp)
            .graphicsLayer {
                val progress = hold.coerceIn(0f, 1f)
                alpha = (1f - progress * 1.35f).coerceIn(0f, 1f)
                val scale = 1f - 0.25f * progress
                scaleX = scale
                scaleY = scale
                translationY = -progress * 20f
                transformOrigin = TransformOrigin(0f, 1f)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = wordmarkFontSize,
                    lineHeight = wordmarkLineHeight,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp
                ),
                color = scheme.onSurface,
                maxLines = 1
            )
            Surface(
                shape = CircleShape,
                color = scheme.primaryContainer,
                modifier = Modifier.padding(top = badgeTopPad)
            ) {
                Text(
                    text = "GAME SPACE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    ),
                    color = scheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            val penShape = CircleShape
            Surface(
                shape = penShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                contentColor = scheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = badgeTopPad)
                    .size(penSize)
                    .gamepadFocusOutline(shape = penShape, register = true)
                    .clip(penShape)
                    .clickable(onClick = onOpenHomeWallpaper)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Home Wallpaper",
                        modifier = Modifier.size(penSize * 0.54f)
                    )
                }
            }
        }
        Text(
            text = "$deviceName \u00b7 $chipsetName",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = (11f + 2f * scaleRatio).coerceIn(12f, 15f).sp
            ),
            color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )
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
    battery: BatteryState,
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
    movingForward: Boolean = true,
    onLaunch: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenWallpaperSettings: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val colorSpec = motion.defaultEffectsSpec<Color>()

    // Individual bounce animatables for the bottom bar left info elements
    val iconOffset = remember { Animatable(0f) }
    val iconScale = remember { Animatable(1f) }
    val iconAlpha = remember { Animatable(1f) }

    val titleOffset = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(1f) }

    val subOffset = remember { Animatable(0f) }
    val subAlpha = remember { Animatable(1f) }

    // In-place bounce animatables for action buttons (Play, Pen, Gear)
    val playPageBounce = remember { Animatable(1f) }
    val penPageBounce = remember { Animatable(1f) }
    val gearPageBounce = remember { Animatable(1f) }
    val gearRotation = remember { Animatable(0f) }

    var isFirstLaunch by remember { mutableStateOf(true) }

    LaunchedEffect(app.packageName) {
        if (isFirstLaunch) {
            isFirstLaunch = false
            return@LaunchedEffect
        }
        val sign = if (movingForward) 1f else -1f

        // 1. App Icon: springs with horizontal offset + scale overshoot
        launch {
            iconOffset.snapTo(sign * 42f)
            iconScale.snapTo(0.72f)
            iconAlpha.snapTo(0.2f)
            launch {
                iconAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
            launch {
                iconScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.50f,
                        stiffness = 320f
                    )
                )
            }
            iconOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = 300f
                )
            )
        }

        // 2. App Title: staggered individual spring overshoot
        launch {
            kotlinx.coroutines.delay(25)
            titleOffset.snapTo(sign * 65f)
            titleAlpha.snapTo(0.15f)
            launch {
                titleAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
            titleOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.54f,
                    stiffness = 280f
                )
            )
        }

        // 3. Version badge & package name: staggered individual spring overshoot
        launch {
            kotlinx.coroutines.delay(50)
            subOffset.snapTo(sign * 80f)
            subAlpha.snapTo(0.1f)
            launch {
                subAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 220)
                )
            }
            subOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.56f,
                    stiffness = 260f
                )
            )
        }

        // 4. Action buttons: stay in place (no translation), bounce in place!
        launch {
            playPageBounce.animateTo(
                targetValue = 1.18f,
                animationSpec = spring(
                    dampingRatio = 0.60f,
                    stiffness = 850f
                )
            )
            playPageBounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.42f,
                    stiffness = 260f
                )
            )
        }

        launch {
            kotlinx.coroutines.delay(20)
            penPageBounce.snapTo(0.82f)
            penPageBounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.50f,
                    stiffness = 350f
                )
            )
        }

        launch {
            kotlinx.coroutines.delay(35)
            gearPageBounce.snapTo(0.82f)
            val gearSign = if (movingForward) -1f else 1f
            gearRotation.snapTo(gearSign * 45f)
            launch {
                gearRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.52f,
                        stiffness = 280f
                    )
                )
            }
            gearPageBounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.50f,
                    stiffness = 350f
                )
            )
        }
    }

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
                    Box(
                        modifier = Modifier.graphicsLayer {
                            translationX = iconOffset.value
                            scaleX = iconScale.value
                            scaleY = iconScale.value
                            alpha = iconAlpha.value
                        }
                    ) {
                        AppIconImage(app = app, size = 56.dp)
                    }
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                translationX = titleOffset.value
                                alpha = titleAlpha.value
                            }
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.graphicsLayer {
                                translationX = subOffset.value
                                alpha = subAlpha.value
                            }
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

            val haptic = LocalHapticFeedback.current
            val playInteractionSource = remember { MutableInteractionSource() }
            val isPlayPressed by playInteractionSource.collectIsPressedAsState()
            val playHoldScale = remember { Animatable(1f) }

            LaunchedEffect(isPlayPressed) {
                if (isPlayPressed) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    playHoldScale.animateTo(
                        targetValue = 1.14f,
                        animationSpec = spring(
                            dampingRatio = 0.58f,
                            stiffness = 380f
                        )
                    )
                } else {
                    playHoldScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.38f,
                            stiffness = 240f
                        )
                    )
                }
            }

            val playShadowElevation by animateDpAsState(
                targetValue = if (isPlayPressed) 14.dp else 8.dp,
                animationSpec = motion.defaultSpatialSpec(),
                label = "PlayBtnShadow"
            )
            val playTonalElevation by animateDpAsState(
                targetValue = if (isPlayPressed) 10.dp else 6.dp,
                animationSpec = motion.defaultSpatialSpec(),
                label = "PlayBtnTonal"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                            .graphicsLayer {
                                scaleX = penPageBounce.value
                                scaleY = penPageBounce.value
                            }
                            .onFocusChanged { penFocused = it.isFocused }
                            .gamepadFocusOutline(shape = CircleShape, width = 2.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenWallpaperSettings)
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
                            .graphicsLayer {
                                scaleX = gearPageBounce.value
                                scaleY = gearPageBounce.value
                                rotationZ = gearRotation.value
                            }
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

                val playButtonShape = RoundedCornerShape(22.dp)

                // Expressive colorful Play button with rounded corner square shape (reduced by 25%)
                Surface(
                    shape = playButtonShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    tonalElevation = playTonalElevation,
                    shadowElevation = playShadowElevation,
                    modifier = Modifier
                        .height(81.dp)
                        .graphicsLayer {
                            val combinedScale = playScale * playPageBounce.value * playHoldScale.value
                            scaleX = combinedScale
                            scaleY = combinedScale
                        }
                        .onFocusChanged { playFocused = it.isFocused }
                        .gamepadFocusOutline(shape = playButtonShape, width = 3.dp)
                        .clip(playButtonShape)
                        .clickable(
                            interactionSource = playInteractionSource,
                            indication = ripple(),
                            onClick = onLaunch
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Play",
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
