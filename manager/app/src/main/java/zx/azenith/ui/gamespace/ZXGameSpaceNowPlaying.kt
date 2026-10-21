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

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.BgmManager
import zx.azenith.ui.util.BgmRepeatMode
import zx.azenith.ui.util.BgmTrack
import zx.azenith.ui.util.fireTapHaptic

/**
 * Adaptive layout modes for the NowPlaying dialog depending on available vertical height.
 */
private enum class NowPlayingLayoutMode {
    LARGE,
    EXPANDED,
    COMPACT,
    ULTRA_COMPACT
}

/**
 * Now Playing dot indicator for the Game Space status bar.
 */
@Composable
internal fun NowPlayingDot(
    isPanelOpen: Boolean = false,
    onOpenPanel: () -> Unit,
    onPositioned: (Rect) -> Unit = {}
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val currentTrack by BgmManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by BgmManager.isPlaying.collectAsStateWithLifecycle()
    val positionMsState = BgmManager.playbackPositionMs.collectAsStateWithLifecycle()
    val albumArt by BgmManager.currentAlbumArt.collectAsStateWithLifecycle()
    val isEnabled by BgmManager.isEnabled.collectAsStateWithLifecycle()
    val nowPlayingEnabled by BgmManager.isNowPlayingEnabled.collectAsStateWithLifecycle()
    val songChangeNonce by BgmManager.songChangeNonce.collectAsStateWithLifecycle()

    if (!isEnabled || !nowPlayingEnabled || currentTrack == null) return

    val track = currentTrack ?: return

    var isExpanded by remember { mutableStateOf(false) }
    var displayedTrack by remember { mutableStateOf(track) }
    var lastObservedNonce by remember { mutableStateOf(songChangeNonce) }
    var wasPanelOpen by remember { mutableStateOf(isPanelOpen) }

    LaunchedEffect(songChangeNonce) {
        if (songChangeNonce != lastObservedNonce && songChangeNonce > 0L) {
            lastObservedNonce = songChangeNonce
            if (!isPanelOpen) {
                if (isExpanded) {
                    isExpanded = false
                    delay(220L)
                }
                displayedTrack = track
                val textLen = maxOf(track.title.length, track.displayArtist.length)
                val duration = if (textLen > 18) {
                    (3500L + (textLen - 18) * 150L).coerceIn(4000L, 8000L)
                } else {
                    3500L
                }
                delay(60L)
                isExpanded = true
                delay(duration)
                isExpanded = false
            } else {
                displayedTrack = track
            }
        }
    }

    LaunchedEffect(isPanelOpen) {
        if (isPanelOpen) {
            wasPanelOpen = true
        } else if (wasPanelOpen) {
            wasPanelOpen = false
            displayedTrack = currentTrack ?: track
            // Transform back into topbar in the expanded state with overshoot spring
            isExpanded = true
            val textLen = maxOf(displayedTrack.title.length, displayedTrack.displayArtist.length)
            val duration = if (textLen > 18) {
                (3500L + (textLen - 18) * 150L).coerceIn(4000L, 8000L)
            } else {
                3500L
            }
            delay(duration)
            // After music title and artist is shown, finally collapse to dot
            isExpanded = false
        }
    }

    val jacketRotation = rememberGameSpaceClock()

    AnimatedVisibility(
        visible = !isPanelOpen,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                expandHorizontally(
                    expandFrom = Alignment.End,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) +
                scaleIn(
                    initialScale = 0.75f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
               shrinkHorizontally(
                   shrinkTowards = Alignment.End,
                   animationSpec = spring(
                       dampingRatio = Spring.DampingRatioMediumBouncy,
                       stiffness = Spring.StiffnessMediumLow
                   )
               ) +
               slideOutVertically(
                   targetOffsetY = { it / 2 },
                   animationSpec = spring(
                       dampingRatio = Spring.DampingRatioMediumBouncy,
                       stiffness = Spring.StiffnessMediumLow
                   )
               ) +
               scaleOut(
                   targetScale = 0.75f,
                   animationSpec = spring(
                       dampingRatio = Spring.DampingRatioMediumBouncy,
                       stiffness = Spring.StiffnessMediumLow
                   )
               )
    ) {
        Surface(
            shape = CircleShape,
            color = if (isExpanded) scheme.surfaceContainerHigh else scheme.surfaceContainer,
            modifier = Modifier
                .height(38.dp)
                .clip(CircleShape)
                .onGloballyPositioned { coords ->
                    if (!isPanelOpen && coords.isAttached && coords.size.width > 0 && coords.size.height > 0) {
                        val bounds = coords.boundsInRoot()
                        if (bounds.width > 0f && bounds.height > 0f) {
                            onPositioned(bounds)
                        }
                    }
                }
                .gamepadFocusOutline(shape = CircleShape, register = true)
                .clickable {
                    AudioUtil.playOk(context)
                    onOpenPanel()
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            expandHorizontally(
                                expandFrom = Alignment.End,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) +
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { it / 3 },
                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                           shrinkHorizontally(
                               shrinkTowards = Alignment.End,
                               animationSpec = spring(
                                   dampingRatio = Spring.DampingRatioMediumBouncy,
                                   stiffness = Spring.StiffnessMediumLow
                               )
                           )
                ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.width(128.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = displayedTrack.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    repeatDelayMillis = 1000,
                                    initialDelayMillis = 600,
                                    velocity = 35.dp
                                )
                        )
                        val subtitle = displayedTrack.displayArtist
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 11.sp
                            ),
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    repeatDelayMillis = 1000,
                                    initialDelayMillis = 600,
                                    velocity = 35.dp
                                )
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp)
            ) {
                CircularProgressIndicator(
                    progress = {
                        val duration = track.durationMs.coerceAtLeast(1L)
                        (positionMsState.value.toFloat() / duration).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxSize(),
                    color = scheme.primary.copy(alpha = 0.85f),
                    trackColor = scheme.outlineVariant.copy(alpha = 0.3f),
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round
                )

                AnimatedContent(
                    targetState = albumArt,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                         scaleIn(initialScale = 0.85f, animationSpec = tween(280, easing = FastOutSlowInEasing))) togetherWith
                        (fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                         scaleOut(targetScale = 0.85f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                    },
                    label = "JacketMorph",
                    contentAlignment = Alignment.Center
                ) { art ->
                    if (art != null) {
                        val bitmap = remember(art) { art.asImageBitmap() }
                        Image(
                            bitmap = bitmap,
                            contentDescription = displayedTrack.title,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .graphicsLayer {
                                    rotationZ = (jacketRotation.floatValue * 72f) % 360f
                                },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = displayedTrack.title,
                            tint = scheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    rotationZ = (jacketRotation.floatValue * 72f) % 360f
                                }
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Floating morphing dialog for Now Playing.
 * Morphs smoothly from the status dot using an overshoot spring animation.
 * Adaptively fits and re-layouts dynamically based on screen scale and height.
 * The playlist opens UPWARDS covering controls, preserving vertical space.
 */
@Composable
internal fun NowPlayingPanelOverlay(
    visible: Boolean,
    anchor: Rect? = null,
    hazeState: HazeState? = null,
    onClose: () -> Unit,
    onNavigateToAudioSettings: (() -> Unit)? = null
) {
    BackHandler(enabled = visible, onBack = onClose)

    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val morphProgress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            morphProgress.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 380f
                )
            )
        } else {
            morphProgress.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 420f
                )
            )
        }
    }

    val progress = morphProgress.value
    if (!visible && !morphProgress.isRunning && progress <= 0.005f) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val margin = 12.dp
        val dotTopDp = anchor?.let { with(density) { it.bottom.toDp() } } ?: 54.dp
        val dotCenterXDp = anchor?.let { with(density) { it.center.x.toDp() } } ?: (screenWidth - 32.dp)
        val dotRightDp = anchor?.let { with(density) { it.right.toDp() } } ?: screenWidth

        val dialogTop = (dotTopDp + 8.dp).coerceIn(8.dp, screenHeight - 120.dp)
        val availableHeight = (screenHeight - dialogTop - margin).coerceAtLeast(100.dp)

        // Select adaptive layout mode strictly based on available height in Compose DP
        val layoutMode = when {
            availableHeight < 150.dp -> NowPlayingLayoutMode.ULTRA_COMPACT
            availableHeight < 240.dp -> NowPlayingLayoutMode.COMPACT
            availableHeight < 350.dp -> NowPlayingLayoutMode.EXPANDED
            else -> NowPlayingLayoutMode.LARGE
        }

        val dialogWidth = when (layoutMode) {
            NowPlayingLayoutMode.LARGE -> minOf(360.dp, screenWidth - 24.dp)
            NowPlayingLayoutMode.EXPANDED -> minOf(350.dp, screenWidth - 24.dp)
            NowPlayingLayoutMode.COMPACT -> minOf(340.dp, screenWidth - 24.dp)
            NowPlayingLayoutMode.ULTRA_COMPACT -> minOf(320.dp, screenWidth - 24.dp)
        }

        val idealRight = dotRightDp + 4.dp
        val clampedRight = idealRight.coerceIn(dialogWidth + margin, screenWidth - margin)
        val dialogLeft = clampedRight - dialogWidth

        // Morphing scale and translation origin pivoted at dot
        val tipFractionX = ((dotCenterXDp - dialogLeft) / dialogWidth).coerceIn(0.1f, 0.9f)
        val scale = (0.15f + 0.85f * progress).coerceAtLeast(0.01f)
        val translationY = (1f - progress) * -20f * density.density
        val contentAlpha = (progress / 0.25f).coerceIn(0f, 1f)

        // Fully transparent dismiss layer: Outside click closes the modal without darkening the screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = dialogLeft, y = dialogTop)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerHigh,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .width(dialogWidth)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = contentAlpha
                            transformOrigin = TransformOrigin(tipFractionX, 0f)
                            this.translationY = translationY
                        }
                ) {
                    NowPlayingDialogWindow(
                        layoutMode = layoutMode,
                        dialogWidth = dialogWidth,
                        availableHeight = availableHeight
                    )
                }
            }
        }
    }
}

/**
 * Manages the player content and the pull-up playlist sheet that opens UPWARDS.
 */
@Composable
private fun NowPlayingDialogWindow(
    layoutMode: NowPlayingLayoutMode,
    dialogWidth: Dp,
    availableHeight: Dp
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val currentTrack by BgmManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by BgmManager.isPlaying.collectAsStateWithLifecycle()
    val positionMs by BgmManager.playbackPositionMs.collectAsStateWithLifecycle()
    val albumArt by BgmManager.currentAlbumArt.collectAsStateWithLifecycle()
    val playlist by BgmManager.playlist.collectAsStateWithLifecycle()
    val repeatMode by BgmManager.repeatMode.collectAsStateWithLifecycle()
    val isShuffleEnabled by BgmManager.isShuffleEnabled.collectAsStateWithLifecycle()

    val track = currentTrack ?: BgmTrack.DEFAULT
    val durationMs = track.durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    // Pull-up playlist progress (0f = collapsed at bottom, 1f = fully covering dialog)
    val playlistProgress = remember { Animatable(0f) }

    val collapsedHeight = when (layoutMode) {
        NowPlayingLayoutMode.LARGE -> if (availableHeight >= 460.dp) 365.dp else 340.dp
        NowPlayingLayoutMode.EXPANDED -> 230.dp
        NowPlayingLayoutMode.COMPACT -> 138.dp
        NowPlayingLayoutMode.ULTRA_COMPACT -> 94.dp
    }
    val maxExpandedHeight = when (layoutMode) {
        NowPlayingLayoutMode.LARGE -> minOf(availableHeight, 480.dp).coerceAtLeast(collapsedHeight)
        NowPlayingLayoutMode.EXPANDED -> minOf(availableHeight, 360.dp).coerceAtLeast(collapsedHeight)
        NowPlayingLayoutMode.COMPACT -> minOf(availableHeight, 280.dp).coerceAtLeast(collapsedHeight)
        NowPlayingLayoutMode.ULTRA_COMPACT -> minOf(availableHeight, 200.dp).coerceAtLeast(collapsedHeight)
    }

    val currentCardHeight = collapsedHeight + (maxExpandedHeight - collapsedHeight) * playlistProgress.value
    val triggerBarHeight = 44.dp
    val totalTravelDp = (collapsedHeight - triggerBarHeight).coerceAtLeast(1.dp)
    val totalTravelPx = with(density) { totalTravelDp.toPx() }
    val updatedTotalTravelPx by rememberUpdatedState(totalTravelPx)

    Box(
        modifier = Modifier
            .width(dialogWidth)
            .height(currentCardHeight)
            .clip(RoundedCornerShape(28.dp))
    ) {
        // LAYER 1: Player controls underneath
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = triggerBarHeight)
        ) {
            when (layoutMode) {
                NowPlayingLayoutMode.LARGE -> {
                    LargePlayerContent(
                        track = track,
                        albumArt = albumArt,
                        isPlaying = isPlaying,
                        durationMs = durationMs,
                        progress = progress,
                        positionMs = positionMs,
                        repeatMode = repeatMode,
                        isShuffleEnabled = isShuffleEnabled,
                        availableHeight = availableHeight
                    )
                }
                NowPlayingLayoutMode.EXPANDED -> {
                    ExpandedPlayerContent(
                        track = track,
                        albumArt = albumArt,
                        isPlaying = isPlaying,
                        durationMs = durationMs,
                        progress = progress,
                        positionMs = positionMs,
                        repeatMode = repeatMode,
                        isShuffleEnabled = isShuffleEnabled
                    )
                }
                NowPlayingLayoutMode.COMPACT -> {
                    CompactPlayerContent(
                        track = track,
                        isPlaying = isPlaying,
                        durationMs = durationMs,
                        progress = progress,
                        positionMs = positionMs
                    )
                }
                NowPlayingLayoutMode.ULTRA_COMPACT -> {
                    UltraCompactPlayerContent(
                        track = track,
                        isPlaying = isPlaying
                    )
                }
            }
        }

        // LAYER 2: Draggable pull-up Playlist Sheet (opens UPWARDS, covering seekbar & controls)
        val currentSheetOffsetY = (totalTravelPx * (1f - playlistProgress.value)).roundToInt()

        Surface(
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 28.dp,
                bottomEnd = 28.dp
            ),
            color = scheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .height(currentCardHeight)
                .offset { IntOffset(0, currentSheetOffsetY) }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header & Drag Handle (44 dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(triggerBarHeight)
                        .padding(horizontal = 14.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { deltaPx ->
                                val deltaFraction = -deltaPx / updatedTotalTravelPx
                                val nextProgress = (playlistProgress.value + deltaFraction).coerceIn(0f, 1f)
                                coroutineScope.launch {
                                    playlistProgress.snapTo(nextProgress)
                                }
                            },
                            onDragStopped = { velocity ->
                                coroutineScope.launch {
                                    val target = when {
                                        velocity < -350f -> 1f
                                        velocity > 350f -> 0f
                                        else -> if (playlistProgress.value > 0.4f) 1f else 0f
                                    }
                                    playlistProgress.animateTo(
                                        targetValue = target,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    AudioUtil.playOk(context)
                                    val target = if (playlistProgress.value > 0.5f) 0f else 1f
                                    playlistProgress.animateTo(
                                        targetValue = target,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.bgm_now_playing_playlist),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = scheme.onSurface
                        )
                        ExpressiveBadge(
                            text = "${playlist.size}",
                            container = scheme.secondaryContainer,
                            content = scheme.onSecondaryContainer
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Center subtle drag handle pill
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.45f))
                    )

                    Spacer(Modifier.weight(1f))

                    // Right trigger action indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (playlistProgress.value > 0.5f) {
                                stringResource(R.string.bgm_now_playing_title)
                            } else {
                                stringResource(R.string.bgm_view_playlist)
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = scheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (playlistProgress.value > 0.5f) {
                                Icons.Rounded.KeyboardArrowDown
                            } else {
                                Icons.Rounded.KeyboardArrowUp
                            },
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Playlist Track List (visible as sheet is pulled up)
                if (playlistProgress.value > 0.05f) {
                    val listAlpha = ((playlistProgress.value - 0.1f) / 0.9f).coerceIn(0f, 1f)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .graphicsLayer { alpha = listAlpha },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(playlist.size, key = { index -> playlist[index].id }) { index ->
                            val item = playlist[index]
                            val isCurrent = item.id == track.id
                            val itemBg = if (isCurrent) scheme.secondaryContainer.copy(alpha = 0.55f) else Color.Transparent

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = itemBg,
                                contentColor = if (isCurrent) scheme.onSecondaryContainer else scheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            AudioUtil.playOk(context)
                                            BgmManager.selectTrack(context, item)
                                            // Collapse sheet back down to reveal player controls
                                            coroutineScope.launch {
                                                playlistProgress.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isCurrent && isPlaying) {
                                        NowPlayingBars(color = scheme.primary, modifier = Modifier.size(15.dp))
                                    } else {
                                        Icon(
                                            imageVector = if (item.isDefault) Icons.Rounded.AutoAwesome else Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = when {
                                                isCurrent -> scheme.primary
                                                item.isDefault -> scheme.primary.copy(alpha = 0.8f)
                                                else -> scheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp,
                                                lineHeight = 14.sp
                                            ),
                                            color = if (isCurrent) scheme.onSecondaryContainer else scheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.displayArtist,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 12.sp),
                                            color = if (isCurrent) scheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            else scheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (item.formattedDuration.isNotEmpty()) {
                                        Text(
                                            text = item.formattedDuration,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = if (isCurrent) scheme.onSecondaryContainer.copy(alpha = 0.9f)
                                            else scheme.onSurfaceVariant.copy(alpha = 0.75f)
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

// ──────────────────────────────────────────────────────────────────────────────
// Player Contents (Large, Expanded, Compact, Ultra-Compact)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LargePlayerContent(
    track: BgmTrack,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    durationMs: Long,
    progress: Float,
    positionMs: Long,
    repeatMode: BgmRepeatMode,
    isShuffleEnabled: Boolean,
    availableHeight: Dp
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    val artSize = if (availableHeight >= 460.dp) 140.dp else 125.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Album Cover Art
        ExpressiveJacket(
            art = albumArt,
            track = track,
            isPlaying = isPlaying,
            size = artSize,
            onClick = { AudioUtil.playOk(context) }
        )

        Spacer(Modifier.height(8.dp))

        // Title & Artist
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.5.sp,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 1000,
                    initialDelayMillis = 600,
                    velocity = 40.dp
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.displayArtist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                ),
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(6.dp))

        // Squiggly seekbar with timestamps
        var scrubFraction by remember { mutableStateOf<Float?>(null) }
        val shownFraction = scrubFraction ?: progress

        ExpressiveSquigglySeekbar(
            progress = shownFraction,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            onValueChange = { scrubFraction = it },
            onValueChangeFinished = {
                val target = scrubFraction ?: 0f
                scrubFraction = null
                BgmManager.seekTo((target * durationMs).toLong())
                fireTapHaptic(haptic)
            }
        )

        Spacer(Modifier.height(2.dp))

        val elapsedMs = (shownFraction * durationMs).toLong()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = BgmManager.formatTimeMs(elapsedMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = scheme.onSurfaceVariant
            )
            Text(
                text = track.formattedDuration.ifEmpty { "0:00" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))

        // 5-button transport controls
        val repeatIcon = if (repeatMode == BgmRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveToggleButton(
                icon = repeatIcon,
                contentDescription = if (repeatMode == BgmRepeatMode.ONE)
                    stringResource(R.string.bgm_now_playing_repeat_one)
                else stringResource(R.string.bgm_now_playing_repeat_all),
                isActive = repeatMode == BgmRepeatMode.ONE,
                activeColor = scheme.tertiaryContainer,
                activeContent = scheme.onTertiaryContainer,
                inactiveColor = Color.Transparent,
                inactiveContent = scheme.onSurfaceVariant,
                defaultShape = RoundedCornerShape(19.dp),
                activeShape = RoundedCornerShape(12.dp),
                size = 38.dp,
                iconSize = 18.dp,
                rotationDegrees = if (repeatMode == BgmRepeatMode.ONE) 180f else 0f,
                onClick = { BgmManager.toggleRepeatMode(context) }
            )

            ExpressiveNudgeButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.bgm_now_playing_previous),
                nudgeDp = (-4).dp,
                tint = scheme.onSurface,
                container = scheme.surfaceContainerHighest,
                size = 48.dp,
                iconSize = 21.dp,
                defaultShape = RoundedCornerShape(24.dp),
                pressedShape = RoundedCornerShape(12.dp),
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.playPreviousOrRestart(context)
                }
            )

            ExpressivePlayPauseButton(
                isPlaying = isPlaying,
                tint = scheme.onPrimary,
                container = scheme.primary,
                size = if (isPlaying) 68.dp else 56.dp,
                height = 54.dp,
                iconSize = 25.dp,
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.togglePlayPause(context)
                }
            )

            ExpressiveNudgeButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.bgm_now_playing_next),
                nudgeDp = 4.dp,
                tint = scheme.onSurface,
                container = scheme.surfaceContainerHighest,
                size = 48.dp,
                iconSize = 21.dp,
                defaultShape = RoundedCornerShape(24.dp),
                pressedShape = RoundedCornerShape(12.dp),
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.playNext(context)
                }
            )

            ExpressiveToggleButton(
                icon = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.bgm_shuffle),
                isActive = isShuffleEnabled,
                activeColor = scheme.tertiaryContainer,
                activeContent = scheme.onTertiaryContainer,
                inactiveColor = Color.Transparent,
                inactiveContent = scheme.onSurfaceVariant,
                defaultShape = RoundedCornerShape(19.dp),
                activeShape = RoundedCornerShape(12.dp),
                size = 38.dp,
                iconSize = 18.dp,
                rotationDegrees = 0f,
                onClick = { BgmManager.toggleShuffle(context) }
            )
        }
    }
}

@Composable
private fun ExpandedPlayerContent(
    track: BgmTrack,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    durationMs: Long,
    progress: Float,
    positionMs: Long,
    repeatMode: BgmRepeatMode,
    isShuffleEnabled: Boolean
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 4.dp)
    ) {
        // Header: Album cover + title + artist (NO X close button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveJacket(
                art = albumArt,
                track = track,
                isPlaying = isPlaying,
                onClick = { AudioUtil.playOk(context) }
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1000,
                        initialDelayMillis = 600,
                        velocity = 40.dp
                    )
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.displayArtist,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    ),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Squiggly seekbar with timestamps
        var scrubFraction by remember { mutableStateOf<Float?>(null) }
        val shownFraction = scrubFraction ?: progress

        ExpressiveSquigglySeekbar(
            progress = shownFraction,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            onValueChange = { scrubFraction = it },
            onValueChangeFinished = {
                val target = scrubFraction ?: 0f
                scrubFraction = null
                BgmManager.seekTo((target * durationMs).toLong())
                fireTapHaptic(haptic)
            }
        )

        Spacer(Modifier.height(2.dp))

        val elapsedMs = (shownFraction * durationMs).toLong()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = BgmManager.formatTimeMs(elapsedMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = scheme.onSurfaceVariant
            )
            Text(
                text = track.formattedDuration.ifEmpty { "0:00" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))

        // 5-button transport controls
        val repeatIcon = if (repeatMode == BgmRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveToggleButton(
                icon = repeatIcon,
                contentDescription = if (repeatMode == BgmRepeatMode.ONE)
                    stringResource(R.string.bgm_now_playing_repeat_one)
                else stringResource(R.string.bgm_now_playing_repeat_all),
                isActive = repeatMode == BgmRepeatMode.ONE,
                activeColor = scheme.tertiaryContainer,
                activeContent = scheme.onTertiaryContainer,
                inactiveColor = Color.Transparent,
                inactiveContent = scheme.onSurfaceVariant,
                defaultShape = RoundedCornerShape(19.dp),
                activeShape = RoundedCornerShape(12.dp),
                size = 38.dp,
                iconSize = 18.dp,
                rotationDegrees = if (repeatMode == BgmRepeatMode.ONE) 180f else 0f,
                onClick = { BgmManager.toggleRepeatMode(context) }
            )

            ExpressiveNudgeButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.bgm_now_playing_previous),
                nudgeDp = (-4).dp,
                tint = scheme.onSurface,
                container = scheme.surfaceContainerHighest,
                size = 48.dp,
                iconSize = 21.dp,
                defaultShape = RoundedCornerShape(24.dp),
                pressedShape = RoundedCornerShape(12.dp),
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.playPreviousOrRestart(context)
                }
            )

            ExpressivePlayPauseButton(
                isPlaying = isPlaying,
                tint = scheme.onPrimary,
                container = scheme.primary,
                size = if (isPlaying) 68.dp else 56.dp,
                height = 54.dp,
                iconSize = 25.dp,
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.togglePlayPause(context)
                }
            )

            ExpressiveNudgeButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.bgm_now_playing_next),
                nudgeDp = 4.dp,
                tint = scheme.onSurface,
                container = scheme.surfaceContainerHighest,
                size = 48.dp,
                iconSize = 21.dp,
                defaultShape = RoundedCornerShape(24.dp),
                pressedShape = RoundedCornerShape(12.dp),
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.playNext(context)
                }
            )

            ExpressiveToggleButton(
                icon = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.bgm_shuffle),
                isActive = isShuffleEnabled,
                activeColor = scheme.tertiaryContainer,
                activeContent = scheme.onTertiaryContainer,
                inactiveColor = Color.Transparent,
                inactiveContent = scheme.onSurfaceVariant,
                defaultShape = RoundedCornerShape(19.dp),
                activeShape = RoundedCornerShape(12.dp),
                size = 38.dp,
                iconSize = 18.dp,
                rotationDegrees = 0f,
                onClick = { BgmManager.toggleShuffle(context) }
            )
        }
    }
}

@Composable
private fun CompactPlayerContent(
    track: BgmTrack,
    isPlaying: Boolean,
    durationMs: Long,
    progress: Float,
    positionMs: Long
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
    ) {
        // One-line title + artist on left, huge Pause + Next on right (NO X close button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 19.sp
                    ),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1000,
                        initialDelayMillis = 600,
                        velocity = 35.dp
                    )
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = track.displayArtist,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            // Pause button on the right
            ExpressivePlayPauseButton(
                isPlaying = isPlaying,
                tint = scheme.onPrimary,
                container = scheme.primary,
                size = if (isPlaying) 54.dp else 46.dp,
                height = 42.dp,
                iconSize = 22.dp,
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.togglePlayPause(context)
                }
            )

            Spacer(Modifier.width(6.dp))

            // Next button
            ExpressiveNudgeButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.bgm_now_playing_next),
                nudgeDp = 4.dp,
                tint = scheme.onSurface,
                container = scheme.surfaceContainerHighest,
                size = 42.dp,
                iconSize = 20.dp,
                defaultShape = RoundedCornerShape(21.dp),
                pressedShape = RoundedCornerShape(10.dp),
                onClick = {
                    AudioUtil.playOk(context)
                    BgmManager.playNext(context)
                }
            )
        }

        Spacer(Modifier.height(5.dp))

        // Seekbar below this one-line layout
        var scrubFraction by remember { mutableStateOf<Float?>(null) }
        val shownFraction = scrubFraction ?: progress

        ExpressiveSquigglySeekbar(
            progress = shownFraction,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            onValueChange = { scrubFraction = it },
            onValueChangeFinished = {
                val target = scrubFraction ?: 0f
                scrubFraction = null
                BgmManager.seekTo((target * durationMs).toLong())
                fireTapHaptic(haptic)
            }
        )

        Spacer(Modifier.height(1.dp))

        val elapsedMs = (shownFraction * durationMs).toLong()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = BgmManager.formatTimeMs(elapsedMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = scheme.onSurfaceVariant
            )
            Text(
                text = track.formattedDuration.ifEmpty { "0:00" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UltraCompactPlayerContent(
    track: BgmTrack,
    isPlaying: Boolean
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    repeatDelayMillis = 1000,
                    initialDelayMillis = 600,
                    velocity = 35.dp
                )
            )
            Text(
                text = track.displayArtist,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        ExpressivePlayPauseButton(
            isPlaying = isPlaying,
            tint = scheme.onPrimary,
            container = scheme.primary,
            size = 44.dp,
            height = 38.dp,
            iconSize = 20.dp,
            onClick = {
                AudioUtil.playOk(context)
                BgmManager.togglePlayPause(context)
            }
        )

        Spacer(Modifier.width(6.dp))

        ExpressiveNudgeButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.bgm_now_playing_next),
            nudgeDp = 3.dp,
            tint = scheme.onSurface,
            container = scheme.surfaceContainerHighest,
            size = 38.dp,
            iconSize = 18.dp,
            defaultShape = RoundedCornerShape(19.dp),
            pressedShape = RoundedCornerShape(10.dp),
            onClick = {
                AudioUtil.playOk(context)
                BgmManager.playNext(context)
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Expressive Atoms
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpressiveJacket(
    art: Bitmap?,
    track: BgmTrack,
    isPlaying: Boolean,
    size: Dp = 56.dp,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val isLarge = size > 80.dp
    val defaultCorner = if (isLarge) 30.dp else 28.dp
    val activeCorner = if (isLarge) 20.dp else 16.dp
    val jacketCorner by animateDpAsState(
        targetValue = if (isPlaying) activeCorner else defaultCorner,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "JacketCorner"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "JacketPress"
    )

    Surface(
        shape = RoundedCornerShape(jacketCorner),
        color = scheme.surfaceContainerHighest,
        shadowElevation = if (isLarge) 6.dp else 0.dp,
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        val bitmap = art?.let { remember(it) { it.asImageBitmap() } }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (track.isDefault) Icons.Rounded.AutoAwesome else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = if (track.isDefault) scheme.tertiary else scheme.primary,
                    modifier = Modifier.size(if (isLarge) size * 0.40f else 24.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveSquigglySeekbar(
    progress: Float,
    isPlaying: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    val currentFraction = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)

    val amplitudeDp by animateDpAsState(
        targetValue = if (isPlaying && !isDragging) 3.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f),
        label = "WaveAmplitude"
    )
    val thumbWidthDp by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.dp,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 600f),
        label = "ThumbWidth"
    )
    val thumbHeightDp by animateDpAsState(
        targetValue = if (isDragging) 26.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 600f),
        label = "ThumbHeight"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "WavePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseShift"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(newFraction)
                    onValueChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(dragProgress)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(dragProgress)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished()
                    }
                )
            }
    ) {
        val strokeWidthPx = 4.dp.toPx()
        val thumbWidthPx = thumbWidthDp.toPx()
        val thumbHeightPx = thumbHeightDp.toPx()
        val gapPx = 7.dp.toPx()
        val wavelengthPx = 22.dp.toPx()
        val amplitudePx = amplitudeDp.toPx()
        val centerY = size.height / 2f

        val usableWidth = (size.width - thumbWidthPx).coerceAtLeast(1f)
        val thumbCenterX = (thumbWidthPx / 2f) + (usableWidth * currentFraction)

        // Active squiggly wave (left of vertical-bar thumb)
        val activeEndX = (thumbCenterX - thumbWidthPx / 2f - gapPx).coerceAtLeast(0f)
        if (activeEndX > 0f) {
            val wavePath = Path()
            val stepPx = 2f
            var x = 0f
            while (x <= activeEndX) {
                val angularFreq = (2f * Math.PI / wavelengthPx).toFloat()
                val y = centerY + amplitudePx * kotlin.math.sin(angularFreq * x - phase)
                if (x == 0f) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                x += stepPx
            }
            drawPath(
                path = wavePath,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Inactive flat track + end-stop dot (right of vertical-bar thumb)
        val inactiveStartX = (thumbCenterX + thumbWidthPx / 2f + gapPx).coerceAtMost(size.width)
        val trackEndX = size.width - (strokeWidthPx / 2f)
        if (inactiveStartX < trackEndX) {
            drawLine(
                color = inactiveColor,
                start = Offset(inactiveStartX, centerY),
                end = Offset(trackEndX, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = activeColor,
                radius = 2.dp.toPx(),
                center = Offset(trackEndX, centerY)
            )
        }

        // Vertical bar thumb
        drawRect(
            color = activeColor,
            topLeft = Offset(
                x = thumbCenterX - thumbWidthPx / 2f,
                y = centerY - thumbHeightPx / 2f
            ),
            size = Size(width = thumbWidthPx, height = thumbHeightPx)
        )
    }
}

@Composable
private fun Modifier.expressiveBounceClickable(
    pressedScale: Float = 0.88f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "ExpressiveBounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
private fun ExpressiveToggleButton(
    icon: ImageVector,
    contentDescription: String?,
    isActive: Boolean,
    activeColor: Color,
    activeContent: Color,
    inactiveColor: Color,
    inactiveContent: Color,
    defaultShape: Shape,
    activeShape: Shape,
    size: Dp,
    iconSize: Dp,
    rotationDegrees: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 500f),
        label = "ToggleScale"
    )
    val rotation by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
        label = "ToggleRotation"
    )

    val resolvedShape = when {
        isPressed -> RoundedCornerShape(12.dp)
        isActive -> activeShape
        else -> defaultShape
    }

    Surface(
        shape = resolvedShape,
        color = if (isActive) activeColor else inactiveColor,
        contentColor = if (isActive) activeContent else inactiveContent,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .expressiveBounceClickable(pressedScale = 0.80f, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isActive) activeContent else inactiveContent,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
private fun ExpressiveNudgeButton(
    icon: ImageVector,
    contentDescription: String?,
    nudgeDp: Dp,
    tint: Color,
    container: Color,
    size: Dp,
    iconSize: Dp,
    defaultShape: Shape,
    pressedShape: Shape,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "NudgeScale"
    )
    val nudgeTranslationX by animateFloatAsState(
        targetValue = if (isPressed) nudgeDp.value * LocalDensity.current.density else 0f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "NudgeX"
    )

    Surface(
        shape = if (isPressed) pressedShape else defaultShape,
        color = container,
        contentColor = tint,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = nudgeTranslationX
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .expressiveBounceClickable(pressedScale = 0.84f, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun ExpressivePlayPauseButton(
    isPlaying: Boolean,
    tint: Color,
    container: Color,
    size: Dp,
    height: Dp,
    iconSize: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = if (isPlaying) 18.dp else 28.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "PlayPauseCorner"
    )
    val pressedCorner by animateDpAsState(
        targetValue = 13.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "PlayPausePressedCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "PlayPauseScale"
    )

    val resolvedCorner = if (isPressed) pressedCorner else corner

    Surface(
        shape = RoundedCornerShape(resolvedCorner),
        color = container,
        contentColor = tint,
        modifier = Modifier
            .width(size)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .expressiveBounceClickable(pressedScale = 0.86f, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize)
        ) {
            Crossfade(
                targetState = isPlaying,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 650f),
                label = "PlayPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing)
                        stringResource(R.string.bgm_now_playing_pause)
                    else stringResource(R.string.bgm_now_playing_play),
                    tint = tint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveBadge(
    text: String,
    container: Color,
    content: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun NowPlayingBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "EqBars")
    val bar1 = transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(400, easing = LinearEasing), RepeatMode.Reverse
        ), label = "bar1"
    )
    val bar2 = transition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            tween(350, easing = LinearEasing), RepeatMode.Reverse
        ), label = "bar2"
    )
    val bar3 = transition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = LinearEasing), RepeatMode.Reverse
        ), label = "bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { barState ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = barState.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}
