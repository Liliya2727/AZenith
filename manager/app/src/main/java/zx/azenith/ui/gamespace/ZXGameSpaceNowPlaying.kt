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
import androidx.compose.animation.SizeTransform
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import kotlinx.coroutines.delay
import zx.azenith.R
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.BgmManager
import zx.azenith.ui.util.BgmRepeatMode
import zx.azenith.ui.util.BgmTrack
import zx.azenith.ui.util.fireTapHaptic

/**
 * Responsive layout modes for the NowPlaying dialog depending on scale & screen dimensions.
 */
private enum class NowPlayingLayoutMode {
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
    var pendingExpandOnClose by remember { mutableStateOf(false) }

    LaunchedEffect(songChangeNonce) {
        if (songChangeNonce != lastObservedNonce && songChangeNonce > 0L) {
            lastObservedNonce = songChangeNonce
            if (isPanelOpen) {
                pendingExpandOnClose = true
                displayedTrack = track
            } else {
                if (isExpanded) {
                    isExpanded = false
                    delay(260L)
                }
                displayedTrack = track
                val textLen = maxOf(track.title.length, (track.artist ?: "").length)
                val duration = if (textLen > 18) {
                    (3800L + (textLen - 18) * 160L).coerceIn(4500L, 9500L)
                } else {
                    3800L
                }
                delay(80L)
                isExpanded = true
                delay(duration)
                isExpanded = false
            }
        }
    }

    LaunchedEffect(isPanelOpen) {
        if (!isPanelOpen && pendingExpandOnClose) {
            pendingExpandOnClose = false
            delay(280L)
            val textLen = maxOf(displayedTrack.title.length, (displayedTrack.artist ?: "").length)
            val duration = if (textLen > 18) {
                (3800L + (textLen - 18) * 160L).coerceIn(4500L, 9500L)
            } else {
                3800L
            }
            isExpanded = true
            delay(duration)
            isExpanded = false
        }
    }

    val jacketRotation = rememberGameSpaceClock()

    Surface(
        shape = CircleShape,
        color = if (isExpanded) scheme.surfaceContainerHigh else scheme.surfaceContainer,
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
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
                               dampingRatio = Spring.DampingRatioNoBouncy,
                               stiffness = Spring.StiffnessMedium
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
                        val subtitle = displayedTrack.artist ?: displayedTrack.fileName
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

/**
 * Floating morphing dialog for Now Playing.
 * Morphs smoothly from the status dot using an overshoot spring animation.
 * Adaptively fits and re-layouts dynamically based on screen scale and height.
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
    val config = LocalConfiguration.current

    val morphProgress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            morphProgress.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 380f
                )
            )
        } else {
            morphProgress.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = 0.95f,
                    stiffness = 700f
                )
            )
        }
    }

    val progress = morphProgress.value
    if (!visible && progress <= 0.005f) return

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.35f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "NowPlayingScrim"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isScaleConstrained = density.fontScale > 1.15f || config.densityDpi >= 440

        // Determine dialog width and position based on screen constraints
        val dialogWidth = when {
            screenWidth < 360.dp -> screenWidth - 24.dp
            isScaleConstrained -> minOf(330.dp, screenWidth - 24.dp)
            else -> minOf(350.dp, screenWidth - 32.dp)
        }

        // Anchor-based positioning
        val margin = 12.dp
        val dotTopDp = anchor?.let { with(density) { it.bottom.toDp() } } ?: 54.dp
        val dotCenterXDp = anchor?.let { with(density) { it.center.x.toDp() } } ?: (screenWidth - 32.dp)
        val dotRightDp = anchor?.let { with(density) { it.right.toDp() } } ?: screenWidth

        val dialogTop = (dotTopDp + 8.dp).coerceIn(8.dp, screenHeight - 140.dp)
        val idealRight = dotRightDp + 4.dp
        val clampedRight = idealRight.coerceIn(dialogWidth + margin, screenWidth - margin)
        val dialogLeft = clampedRight - dialogWidth
        val availableHeight = (screenHeight - dialogTop - margin).coerceAtLeast(140.dp)

        // Select adaptive layout mode based on remaining height and screen density
        val layoutMode = when {
            availableHeight < 240.dp || screenHeight < 360.dp -> NowPlayingLayoutMode.ULTRA_COMPACT
            availableHeight < 380.dp || isScaleConstrained -> NowPlayingLayoutMode.COMPACT
            else -> NowPlayingLayoutMode.EXPANDED
        }

        // Morphing scale and translation origin
        val tipFractionX = ((dotCenterXDp - dialogLeft) / dialogWidth).coerceIn(0.1f, 0.9f)
        val scale = 0.15f + 0.85f * progress
        val translationY = (1f - progress) * -24f * density.density
        val contentAlpha = (progress / 0.28f).coerceIn(0f, 1f)

        // Background scrim dismiss layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.scrim.copy(alpha = scrimAlpha))
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
                    shape = RoundedCornerShape(32.dp),
                    color = scheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .width(dialogWidth)
                        .heightIn(max = availableHeight)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeBlur(
                                    input = HazeInput.Sources(hazeState),
                                    style = HazeBlurStyle.Material3(
                                        containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.92f)
                                    ) {
                                        blurRadius(28.dp)
                                    }
                                )
                            } else Modifier
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = contentAlpha
                            transformOrigin = TransformOrigin(tipFractionX, 0f)
                            this.translationY = translationY
                        }
                ) {
                    NowPlayingAdaptiveContent(
                        layoutMode = layoutMode,
                        dialogWidth = dialogWidth,
                        availableHeight = availableHeight,
                        onClose = onClose,
                        onNavigateToAudioSettings = onNavigateToAudioSettings
                    )
                }
            }
        }
    }
}

/**
 * Renders Now Playing content according to the calculated [NowPlayingLayoutMode].
 */
@Composable
private fun NowPlayingAdaptiveContent(
    layoutMode: NowPlayingLayoutMode,
    dialogWidth: Dp,
    availableHeight: Dp,
    onClose: () -> Unit,
    onNavigateToAudioSettings: (() -> Unit)?
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

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

    var isPlaylistExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .wrapContentHeight(unbounded = false)
    ) {
        when (layoutMode) {
            NowPlayingLayoutMode.EXPANDED -> {
                // Header: Album cover, title + artist, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveJacket(
                        art = albumArt,
                        track = track,
                        isPlaying = isPlaying,
                        isPlaylistExpanded = isPlaylistExpanded,
                        onClick = { AudioUtil.playOk(context) }
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                lineHeight = 22.sp
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
                            text = track.artist ?: track.fileName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            ),
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    ExpressiveCloseButton(
                        tint = scheme.onSurfaceVariant,
                        container = scheme.surfaceContainerHighest,
                        onClick = {
                            AudioUtil.playCancel(context)
                            onClose()
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Seek bar with squiggly waveform
                AnimatedVisibility(
                    visible = !isPlaylistExpanded,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.60f, stiffness = 420f),
                        expandFrom = Alignment.Top
                    ) + fadeIn(tween(160)),
                    exit = shrinkVertically(
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(tween(120))
                ) {
                    Column {
                        var scrubFraction by remember { mutableStateOf<Float?>(null) }
                        val shownFraction = scrubFraction ?: progress

                        ExpressiveSquigglySeekbar(
                            progress = shownFraction,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            onValueChange = { scrubFraction = it },
                            onValueChangeFinished = {
                                val target = scrubFraction ?: 0f
                                scrubFraction = null
                                BgmManager.seekTo((target * durationMs).toLong())
                                fireTapHaptic(haptic)
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        val elapsedMs = (shownFraction * durationMs).toLong()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = BgmManager.formatTimeMs(elapsedMs),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = track.formattedDuration.ifEmpty { "0:00" },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = scheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Full 5-button transport controls
                        val repeatIcon = if (repeatMode == BgmRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
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
                                defaultShape = RoundedCornerShape(21.dp),
                                activeShape = RoundedCornerShape(12.dp),
                                size = 42.dp,
                                iconSize = 20.dp,
                                rotationDegrees = if (repeatMode == BgmRepeatMode.ONE) 180f else 0f,
                                onClick = { BgmManager.toggleRepeatMode(context) }
                            )

                            ExpressiveNudgeButton(
                                icon = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.bgm_now_playing_previous),
                                nudgeDp = (-4).dp,
                                tint = scheme.onSurface,
                                container = scheme.surfaceContainerHighest,
                                size = 52.dp,
                                iconSize = 22.dp,
                                defaultShape = RoundedCornerShape(26.dp),
                                pressedShape = RoundedCornerShape(14.dp),
                                onClick = {
                                    AudioUtil.playOk(context)
                                    BgmManager.playPreviousOrRestart(context)
                                }
                            )

                            ExpressivePlayPauseButton(
                                isPlaying = isPlaying,
                                tint = scheme.onPrimary,
                                container = scheme.primary,
                                size = if (isPlaying) 76.dp else 64.dp,
                                height = 64.dp,
                                iconSize = 28.dp,
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
                                size = 52.dp,
                                iconSize = 22.dp,
                                defaultShape = RoundedCornerShape(26.dp),
                                pressedShape = RoundedCornerShape(14.dp),
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
                                defaultShape = RoundedCornerShape(21.dp),
                                activeShape = RoundedCornerShape(12.dp),
                                size = 42.dp,
                                iconSize = 20.dp,
                                rotationDegrees = 0f,
                                onClick = { BgmManager.toggleShuffle(context) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Bottom playlist pull-up trigger
                PlaylistTriggerBar(
                    playlist = playlist,
                    isPlaylistExpanded = isPlaylistExpanded,
                    onExpand = { isPlaylistExpanded = true },
                    onNavigateToAudioSettings = onNavigateToAudioSettings
                )

                Spacer(Modifier.height(6.dp))

                PlaylistExpandedSection(
                    visible = isPlaylistExpanded,
                    playlist = playlist,
                    track = track,
                    isPlaying = isPlaying,
                    onCollapse = { isPlaylistExpanded = false },
                    maxListHeight = (availableHeight - 250.dp).coerceIn(120.dp, 280.dp)
                )
            }

            NowPlayingLayoutMode.COMPACT -> {
                // Adaptive Compact: One-line music name & artist with huge Pause + Next on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveCloseButton(
                        tint = scheme.onSurfaceVariant,
                        container = scheme.surfaceContainerHighest,
                        size = 36.dp,
                        iconSize = 18.dp,
                        onClick = {
                            AudioUtil.playCancel(context)
                            onClose()
                        }
                    )

                    Spacer(Modifier.width(10.dp))

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
                        Text(
                            text = track.artist ?: track.fileName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            ),
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Huge Pause and Next button on the right
                    ExpressivePlayPauseButton(
                        isPlaying = isPlaying,
                        tint = scheme.onPrimary,
                        container = scheme.primary,
                        size = if (isPlaying) 58.dp else 52.dp,
                        height = 48.dp,
                        iconSize = 24.dp,
                        onClick = {
                            AudioUtil.playOk(context)
                            BgmManager.togglePlayPause(context)
                        }
                    )

                    Spacer(Modifier.width(6.dp))

                    ExpressiveNudgeButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.bgm_now_playing_next),
                        nudgeDp = 4.dp,
                        tint = scheme.onSurface,
                        container = scheme.surfaceContainerHighest,
                        size = 46.dp,
                        iconSize = 22.dp,
                        defaultShape = RoundedCornerShape(23.dp),
                        pressedShape = RoundedCornerShape(12.dp),
                        onClick = {
                            AudioUtil.playOk(context)
                            BgmManager.playNext(context)
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Seek bar below this one-line layout
                var scrubFraction by remember { mutableStateOf<Float?>(null) }
                val shownFraction = scrubFraction ?: progress

                ExpressiveSquigglySeekbar(
                    progress = shownFraction,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
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

                Spacer(Modifier.height(10.dp))

                // Below is the hidden playlist and can be pulled up if user wants to see
                PlaylistTriggerBar(
                    playlist = playlist,
                    isPlaylistExpanded = isPlaylistExpanded,
                    onExpand = { isPlaylistExpanded = true },
                    onNavigateToAudioSettings = onNavigateToAudioSettings
                )

                Spacer(Modifier.height(4.dp))

                PlaylistExpandedSection(
                    visible = isPlaylistExpanded,
                    playlist = playlist,
                    track = track,
                    isPlaying = isPlaying,
                    onCollapse = { isPlaylistExpanded = false },
                    maxListHeight = (availableHeight - 190.dp).coerceIn(100.dp, 220.dp)
                )
            }

            NowPlayingLayoutMode.ULTRA_COMPACT -> {
                // Ultra compact (extreme density or narrow view): minimal single-line bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveCloseButton(
                        tint = scheme.onSurfaceVariant,
                        container = scheme.surfaceContainerHighest,
                        size = 32.dp,
                        iconSize = 16.dp,
                        onClick = {
                            AudioUtil.playCancel(context)
                            onClose()
                        }
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 1000,
                                initialDelayMillis = 600,
                                velocity = 35.dp
                            )
                    )

                    Spacer(Modifier.width(8.dp))

                    ExpressivePlayPauseButton(
                        isPlaying = isPlaying,
                        tint = scheme.onPrimary,
                        container = scheme.primary,
                        size = 46.dp,
                        height = 40.dp,
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
                        size = 40.dp,
                        iconSize = 18.dp,
                        defaultShape = RoundedCornerShape(20.dp),
                        pressedShape = RoundedCornerShape(10.dp),
                        onClick = {
                            AudioUtil.playOk(context)
                            BgmManager.playNext(context)
                        }
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Shared: Playlist trigger bar & expandable section
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistTriggerBar(
    playlist: List<BgmTrack>,
    isPlaylistExpanded: Boolean,
    onExpand: () -> Unit,
    onNavigateToAudioSettings: (() -> Unit)?
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val triggerBarModifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(scheme.surfaceContainerHighest)
        .padding(horizontal = 14.dp)
        .expressiveBounceClickable(pressedScale = 0.94f, onClick = {
            AudioUtil.playOk(context)
            onExpand()
        })
        .pointerInput(Unit) {
            detectTapGestures {
                AudioUtil.playOk(context)
                onExpand()
            }
        }

    Row(
        modifier = triggerBarModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.bgm_now_playing_playlist),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = scheme.onSurface
            )
            ExpressiveBadge(
                text = "${playlist.size}",
                container = scheme.secondaryContainer,
                content = scheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.bgm_view_playlist),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = scheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PlaylistExpandedSection(
    visible: Boolean,
    playlist: List<BgmTrack>,
    track: BgmTrack,
    isPlaying: Boolean,
    onCollapse: () -> Unit,
    maxListHeight: Dp
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 400f),
            expandFrom = Alignment.Top
        ) + fadeIn(tween(160)),
        exit = shrinkVertically(
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
            shrinkTowards = Alignment.Top
        ) + fadeOut(tween(120))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bgm_now_playing_playlist),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = scheme.onSurface
                    )
                    ExpressiveBadge(
                        text = "${playlist.size}",
                        container = scheme.surfaceContainerHighest,
                        content = scheme.onSurfaceVariant
                    )
                }

                ExpressiveViewPlaylistToggle(
                    isExpanded = true,
                    onClick = {
                        AudioUtil.playOk(context)
                        onCollapse()
                    },
                    container = scheme.secondaryContainer,
                    content = scheme.onSecondaryContainer
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .heightIn(min = 90.dp, max = maxListHeight)
                    .animateContentSize(
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f)
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(playlist.size, key = { index -> playlist[index].id }) { index ->
                    val item = playlist[index]
                    val isCurrent = item.id == track.id
                    val isFirst = index == 0
                    val isLast = index == playlist.size - 1
                    val itemBg = when {
                        isCurrent -> scheme.secondaryContainer.copy(alpha = 0.65f)
                        item.isDefault -> scheme.tertiaryContainer.copy(alpha = 0.45f)
                        else -> Color.Transparent
                    }
                    val itemShape = when {
                        isCurrent && isFirst -> RoundedCornerShape(
                            topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp
                        )
                        !isCurrent && isLast -> RoundedCornerShape(
                            topStart = 8.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 20.dp
                        )
                        else -> RoundedCornerShape(8.dp)
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val rowScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = spring(dampingRatio = 0.50f, stiffness = 500f),
                        label = "QueueRowPress"
                    )
                    Surface(
                        shape = itemShape,
                        color = itemBg,
                        contentColor = if (isCurrent) scheme.onSecondaryContainer else scheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = rowScale
                                scaleY = rowScale
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = {
                                    AudioUtil.playOk(context)
                                    BgmManager.selectTrack(context, item)
                                    onCollapse()
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
                                        item.isDefault -> scheme.tertiary
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
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = if (isCurrent) scheme.onSecondaryContainer else scheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.isDefault) stringResource(R.string.bgm_default_track_badge)
                                    else item.artist ?: item.fileName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
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

// ──────────────────────────────────────────────────────────────────────────────
// Expressive Atoms (Jacket, Close Button, Squiggly Seekbar, Controls)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpressiveJacket(
    art: Bitmap?,
    track: BgmTrack,
    isPlaying: Boolean,
    isPlaylistExpanded: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val jacketCorner by animateDpAsState(
        targetValue = if (isPlaying) 18.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "JacketCorner"
    )
    val jacketScale by animateFloatAsState(
        targetValue = if (isPlaylistExpanded) 0.76f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "JacketScale"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "JacketPress"
    )

    Surface(
        shape = RoundedCornerShape(jacketCorner),
        color = scheme.surfaceContainerHighest,
        modifier = Modifier
            .size(if (isPlaylistExpanded) 52.dp else 66.dp)
            .graphicsLayer { scaleX = jacketScale * pressScale; scaleY = jacketScale * pressScale }
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
                    tint = if (track.isDefault) scheme.onTertiaryContainer else scheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveCloseButton(
    tint: Color,
    container: Color,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 20.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "CloseCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "CloseScale"
    )

    Surface(
        shape = RoundedCornerShape(corner),
        color = container,
        contentColor = tint,
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Material 3 Expressive squiggly seekbar.
 * Draws a playful sinusoidal wave on the active side and a straight rail on the inactive side.
 * A neat vertical pill acts as the scrubbing thumb.
 */
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
        targetValue = if (isDragging) 28.dp else 20.dp,
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
            .height(30.dp)
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
        val gapPx = 8.dp.toPx()
        val wavelengthPx = 22.dp.toPx()
        val amplitudePx = amplitudeDp.toPx()
        val centerY = size.height / 2f

        val usableWidth = (size.width - thumbWidthPx).coerceAtLeast(1f)
        val thumbCenterX = (thumbWidthPx / 2f) + (usableWidth * currentFraction)

        // 1. Active squiggly wave (left of vertical-bar thumb)
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

        // 2. Inactive flat track + end-stop dot (right of vertical-bar thumb)
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

        // 3. Vertical bar thumb
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
        targetValue = if (isPlaying) 20.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "PlayPauseCorner"
    )
    val pressedCorner by animateDpAsState(
        targetValue = 14.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "PlayPausePressedCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
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
            .expressiveBounceClickable(pressedScale = 0.85f, onClick = onClick)
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
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
private fun ExpressiveViewPlaylistToggle(
    isExpanded: Boolean,
    onClick: () -> Unit,
    container: Color,
    content: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "ViewPlaylistScale"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .expressiveBounceClickable(pressedScale = 0.86f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!isExpanded) {
                Text(
                    text = stringResource(R.string.bgm_view_playlist),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
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
