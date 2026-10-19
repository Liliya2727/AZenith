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

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import zx.azenith.R
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.BgmManager
import zx.azenith.ui.util.BgmRepeatMode
import zx.azenith.ui.util.BgmTrack
import zx.azenith.ui.util.tapHaptic

/**
 * Now Playing dot indicator for the Game Space status bar.
 * Shows album art or a rotating music note with circular progress.
 * Expands briefly on song change to show title + artist, then collapses.
 * On click, opens the [NowPlayingPanel].
 */
@Composable
internal fun NowPlayingDot(
    isPanelOpen: Boolean = false,
    onOpenPanel: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val currentTrack by BgmManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by BgmManager.isPlaying.collectAsStateWithLifecycle()
    val positionMs by BgmManager.playbackPositionMs.collectAsStateWithLifecycle()
    val albumArt by BgmManager.currentAlbumArt.collectAsStateWithLifecycle()
    val isEnabled by BgmManager.isEnabled.collectAsStateWithLifecycle()
    val nowPlayingEnabled by BgmManager.isNowPlayingEnabled.collectAsStateWithLifecycle()
    val songChangeNonce by BgmManager.songChangeNonce.collectAsStateWithLifecycle()

    if (!isEnabled || !nowPlayingEnabled || currentTrack == null) return

    val track = currentTrack ?: return
    val durationMs = track.durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    var isExpanded by remember { mutableStateOf(false) }
    var displayedTrack by remember { mutableStateOf(track) }
    var lastObservedNonce by remember { mutableStateOf(songChangeNonce) }
    var pendingExpandOnClose by remember { mutableStateOf(false) }

    // Collapse first on track change while expanded to prevent abrupt metadata swap
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

    val infiniteTransition = rememberInfiniteTransition(label = "NowPlayingRotation")
    val jacketRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "JacketRotation"
    )

    AnimatedVisibility(
        visible = !isPanelOpen,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
               scaleOut(
                   targetScale = 0.8f,
                   animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
               )
    ) {
        Surface(
            shape = CircleShape,
            color = scheme.surfaceContainerHigh,
            modifier = Modifier
                .height(38.dp)
                .clip(CircleShape)
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
                // Overshoot spring animates container and content on expansion
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
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
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

                // Album art / rotating cover jacket with circular progress on the RIGHT
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(30.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = scheme.primary.copy(alpha = 0.8f),
                        trackColor = scheme.outlineVariant.copy(alpha = 0.3f),
                        strokeWidth = 2.dp,
                        strokeCap = StrokeCap.Round
                    )

                    // Cross-morph between cover images and fallback icons
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
                            Image(
                                bitmap = art.asImageBitmap(),
                                contentDescription = displayedTrack.title,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .then(if (isPlaying) Modifier.rotate(jacketRotation) else Modifier),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = displayedTrack.title,
                                tint = scheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .then(if (isPlaying) Modifier.rotate(jacketRotation) else Modifier)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen overlay with scrim and a floating side panel for Now Playing controls.
 * Slides in from the right matching the existing QuickSettings pattern.
 */
@Composable
internal fun NowPlayingPanelOverlay(
    visible: Boolean,
    hazeState: HazeState? = null,
    onClose: () -> Unit,
    onNavigateToAudioSettings: (() -> Unit)? = null
) {
    BackHandler(enabled = visible, onBack = onClose)

    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    val currentTrack by BgmManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by BgmManager.isPlaying.collectAsStateWithLifecycle()
    val positionMs by BgmManager.playbackPositionMs.collectAsStateWithLifecycle()
    val albumArt by BgmManager.currentAlbumArt.collectAsStateWithLifecycle()
    val playlist by BgmManager.playlist.collectAsStateWithLifecycle()
    val repeatMode by BgmManager.repeatMode.collectAsStateWithLifecycle()
    val isShuffleEnabled by BgmManager.isShuffleEnabled.collectAsStateWithLifecycle()
    val volume by BgmManager.volume.collectAsStateWithLifecycle()

    val track = currentTrack ?: BgmTrack.DEFAULT
    val durationMs = track.durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    // Scrim
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = fadeOut(animationSpec = motion.defaultEffectsSpec())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.scrim.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    // Panel
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(animationSpec = motion.defaultEffectsSpec()),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 24.dp, top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerLow,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeBlur(
                                input = HazeInput.Sources(hazeState),
                                style = HazeBlurStyle.Material3(
                                    containerColor = scheme.surfaceContainerLow.copy(alpha = 0.85f)
                                ) {
                                    blurRadius(28.dp)
                                }
                            )
                        } else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.bgm_now_playing_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = scheme.onSurface
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                                .clickable {
                                    AudioUtil.playCancel(context)
                                    onClose()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Album art or large icon
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        val artContent = albumArt
                        if (artContent != null) {
                            Image(
                                bitmap = artContent.asImageBitmap(),
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.surfaceContainerHighest,
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = scheme.primaryContainer,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = scheme.onPrimaryContainer,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Track info
                    AnimatedContent(
                        targetState = track,
                        transitionSpec = {
                            (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                    fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "TrackInfo"
                    ) { t ->
                        Column {
                            Text(
                                text = t.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = t.artist ?: t.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Seek bar
                    Column {
                        ZenithSlider(
                            value = progress,
                            onValueChange = { frac ->
                                val seekMs = (frac * durationMs).toLong()
                                BgmManager.seekTo(seekMs)
                            },
                            onValueChangeFinished = {},
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = BgmManager.formatTimeMs(positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = track.formattedDuration,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Media controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Repeat
                        val repeatIcon = if (repeatMode == BgmRepeatMode.ONE)
                            Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                        val repeatTint = if (repeatMode == BgmRepeatMode.ONE)
                            scheme.primary else scheme.onSurfaceVariant

                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                                .clickable {
                                    BgmManager.toggleRepeatMode(context)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = if (repeatMode == BgmRepeatMode.ONE)
                                        stringResource(R.string.bgm_now_playing_repeat_one)
                                    else stringResource(R.string.bgm_now_playing_repeat_all),
                                    tint = repeatTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Previous
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                                .clickable {
                                    AudioUtil.playOk(context)
                                    BgmManager.playPreviousOrRestart(context)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = stringResource(R.string.bgm_now_playing_previous),
                                    tint = scheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Play/Pause
                        Surface(
                            shape = CircleShape,
                            color = scheme.primaryContainer,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                                .clickable {
                                    AudioUtil.playOk(context)
                                    BgmManager.togglePlayPause(context)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying)
                                        Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying)
                                        stringResource(R.string.bgm_now_playing_pause)
                                    else stringResource(R.string.bgm_now_playing_play),
                                    tint = scheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Next
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                                .clickable {
                                    AudioUtil.playOk(context)
                                    BgmManager.playNext(context)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = stringResource(R.string.bgm_now_playing_next),
                                    tint = scheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Shuffle
                        val shuffleTint = if (isShuffleEnabled)
                            scheme.primary else scheme.onSurfaceVariant
                        Surface(
                            shape = CircleShape,
                            color = if (isShuffleEnabled) scheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .gamepadFocusOutline(shape = CircleShape, register = true)
                                .clickable {
                                    BgmManager.toggleShuffle(context)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = stringResource(R.string.bgm_shuffle),
                                    tint = shuffleTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Playlist Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.bgm_now_playing_playlist),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                            Text(
                                text = "(${playlist.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        if (onNavigateToAudioSettings != null) {
                            val viewBtnShape = RoundedCornerShape(10.dp)
                            Surface(
                                shape = viewBtnShape,
                                color = scheme.secondaryContainer.copy(alpha = 0.75f),
                                contentColor = scheme.onSecondaryContainer,
                                modifier = Modifier
                                    .clip(viewBtnShape)
                                    .gamepadFocusOutline(shape = viewBtnShape, register = true)
                                    .clickable {
                                        AudioUtil.playOk(context)
                                        onClose()
                                        onNavigateToAudioSettings()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.bgm_view_playlist),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(playlist.size, key = { index -> playlist[index].id }) { index ->
                            val item = playlist[index]
                            val isCurrent = item.id == track.id
                            val itemShape = RoundedCornerShape(12.dp)
                            Surface(
                                shape = itemShape,
                                color = if (isCurrent) scheme.secondaryContainer else Color.Transparent,
                                contentColor = if (isCurrent) scheme.onSecondaryContainer else scheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(itemShape)
                                    .gamepadFocusOutline(shape = itemShape, register = true)
                                    .clickable {
                                        AudioUtil.playOk(context)
                                        BgmManager.selectTrack(context, item)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isCurrent && isPlaying) {
                                        NowPlayingBars(
                                            color = scheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = if (isCurrent) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isCurrent) scheme.onSecondaryContainer else scheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sub = item.artist ?: item.fileName
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (isCurrent) scheme.onSecondaryContainer.copy(alpha = 0.75f) else scheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (item.formattedDuration.isNotEmpty()) {
                                        Text(
                                            text = item.formattedDuration,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = if (isCurrent) scheme.onSecondaryContainer.copy(alpha = 0.85f) else scheme.onSurfaceVariant.copy(alpha = 0.7f)
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

/**
 * Animated equalizer bars for the currently playing track in the playlist.
 */
@Composable
private fun NowPlayingBars(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "EqBars")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(400, easing = LinearEasing), RepeatMode.Reverse
        ), label = "bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            tween(350, easing = LinearEasing), RepeatMode.Reverse
        ), label = "bar2"
    )
    val bar3 by transition.animateFloat(
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
        listOf(bar1, bar2, bar3).forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}
