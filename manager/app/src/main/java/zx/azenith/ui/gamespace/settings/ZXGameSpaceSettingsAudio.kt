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

package zx.azenith.ui.gamespace.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.gamespace.SettingsSwitchCard
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.BgmManager
import zx.azenith.ui.viewmodel.SettingsViewModel

@Composable
internal fun ZXGameSpaceSettingsAudio(
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    SettingsAudioPane(
        settingsViewModel = settingsViewModel,
        modifier = modifier
    )
}

@Composable
internal fun SettingsAudioPane(
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scheme = MaterialTheme.colorScheme

    val bgmEnabled by BgmManager.isEnabled.collectAsStateWithLifecycle()
    val bgmVolume by BgmManager.volume.collectAsStateWithLifecycle()
    val playlist by BgmManager.playlist.collectAsStateWithLifecycle()
    val currentTrack by BgmManager.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by BgmManager.isPlaying.collectAsStateWithLifecycle()
    val nowPlayingEnabled by BgmManager.isNowPlayingEnabled.collectAsStateWithLifecycle()
    val isShuffleEnabled by BgmManager.isShuffleEnabled.collectAsStateWithLifecycle()
    val stagedTracks by BgmManager.stagedTracks.collectAsStateWithLifecycle()
    val playbackPositionMs by BgmManager.playbackPositionMs.collectAsStateWithLifecycle()
    val currentAlbumArt by BgmManager.currentAlbumArt.collectAsStateWithLifecycle()

    var isImportingMusic by remember { mutableStateOf(false) }
    var stagingExpanded by remember { mutableStateOf(false) }
    var playlistExpanded by remember { mutableStateOf(false) }
    val stagingScrollState = rememberScrollState()
    val playlistScrollState = rememberScrollState()
    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isInitialComposition = false
    }
    var exitingPlaylistTrackIds by remember { mutableStateOf(setOf<String>()) }
    var exitingStagedTrackIds by remember { mutableStateOf(setOf<String>()) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isImportingMusic = true
            scope.launch {
                try {
                    val count = BgmManager.addTracksFromUris(context, uris)
                    if (count > 0) {
                        Toast.makeText(context, "Added $count track(s)", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isImportingMusic = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.section_audio),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Sound effects, background music, and audio playback settings",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        SettingsSwitchCard(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.sound_effects),
            summary = stringResource(R.string.sound_effects_desc),
            checked = uiState.soundEffects,
            onCheckedChange = { settingsViewModel.setSoundEffects(it) }
        )

        SettingsSwitchCard(
            icon = Icons.Rounded.MusicNote,
            title = stringResource(R.string.bgm_title),
            summary = stringResource(R.string.bgm_desc),
            checked = bgmEnabled,
            onCheckedChange = { BgmManager.setEnabled(context, it) }
        )

        AnimatedVisibility(
            visible = bgmEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val activePlayingTrack = currentTrack
                // Tied to the selected track rather than to playback, so pausing keeps the bar up;
                // only losing the track (removed, nothing left to play) takes it away.
                val isNowPlayingVisible = activePlayingTrack != null

                val nowPlayingSlide = spring<IntSize>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntSize.VisibilityThreshold
                )

                AnimatedVisibility(
                    visible = isNowPlayingVisible,
                    enter = fadeIn(animationSpec = tween(160)) + expandVertically(
                        animationSpec = nowPlayingSlide,
                        expandFrom = Alignment.Top
                    ),
                    exit = fadeOut(animationSpec = tween(160)) + shrinkVertically(
                        animationSpec = nowPlayingSlide,
                        shrinkTowards = Alignment.Top
                    )
                ) {
                    if (activePlayingTrack != null) {
                        val miniPlayerShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = miniPlayerShape,
                            color = scheme.surfaceContainerHigh.copy(alpha = 0.95f),
                            contentColor = scheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .gamepadFocusOutline(shape = miniPlayerShape, register = true)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val albumArt = currentAlbumArt
                                val jacketShape = RoundedCornerShape(10.dp)
                                Surface(
                                    shape = jacketShape,
                                    color = if (activePlayingTrack.isDefault) scheme.tertiaryContainer else scheme.primaryContainer,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(jacketShape)
                                ) {
                                    if (albumArt != null) {
                                        Image(
                                            bitmap = albumArt.asImageBitmap(),
                                            contentDescription = activePlayingTrack.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (activePlayingTrack.isDefault) Icons.Rounded.AutoAwesome else Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = if (activePlayingTrack.isDefault) scheme.onTertiaryContainer else scheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                val displayTitle = activePlayingTrack.title
                                val displayArtist = activePlayingTrack.displayArtist

                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = "—",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = displayArtist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }

                                val currentPosFormatted = BgmManager.formatTimeMs(playbackPositionMs)
                                val totalDurMs = activePlayingTrack.durationMs
                                val totalDurFormatted = if (totalDurMs > 0) {
                                    BgmManager.formatTimeMs(totalDurMs)
                                } else {
                                    activePlayingTrack.formattedDuration.ifEmpty { "0:00" }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = scheme.surfaceContainerHighest.copy(alpha = 0.55f)
                                ) {
                                    Text(
                                        text = "$currentPosFormatted / $totalDurFormatted",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = scheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (playlist.size > 1) {
                                        val prevShape = CircleShape
                                        Surface(
                                            shape = prevShape,
                                            color = Color.Transparent,
                                            contentColor = scheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(prevShape)
                                                .gamepadFocusOutline(shape = prevShape, register = true)
                                                .clickable {
                                                    AudioUtil.playOk(context)
                                                    BgmManager.playPreviousOrRestart(context)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.SkipPrevious,
                                                    contentDescription = "Previous",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    val playShape = CircleShape
                                    Surface(
                                        shape = playShape,
                                        color = scheme.primary,
                                        contentColor = scheme.onPrimary,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(playShape)
                                            .gamepadFocusOutline(shape = playShape, register = true)
                                            .clickable {
                                                AudioUtil.playOk(context)
                                                BgmManager.togglePlayPause(context)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    if (playlist.size > 1) {
                                        val nextShape = CircleShape
                                        Surface(
                                            shape = nextShape,
                                            color = Color.Transparent,
                                            contentColor = scheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(nextShape)
                                                .gamepadFocusOutline(shape = nextShape, register = true)
                                                .clickable {
                                                    AudioUtil.playOk(context)
                                                    BgmManager.playNext(context)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.SkipNext,
                                                    contentDescription = "Next",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val volumeCardShape = RoundedCornerShape(18.dp)
                Surface(
                    shape = volumeCardShape,
                    color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    contentColor = scheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .gamepadFocusOutline(shape = volumeCardShape, register = true)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        var sliderVolume by remember(bgmVolume) { mutableIntStateOf(bgmVolume) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            tint = scheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.bgm_volume),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = scheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.bgm_volume_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "$sliderVolume%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = scheme.primary
                            )
                        }

                        ZenithSlider(
                            value = sliderVolume.toFloat(),
                            onValueChange = { raw ->
                                val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(0, 100)
                                if (snapped != sliderVolume) {
                                    sliderVolume = snapped
                                    BgmManager.setVolume(context, snapped)
                                }
                            },
                            onValueChangeFinished = {
                                BgmManager.setVolume(context, sliderVolume)
                            },
                            valueRange = 0f..100f,
                            steps = 19,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                    }
                }

                // Split Screen: Left = Staging Area, Right = Live Playlist
                val springBouncyFloat = spring<Float>(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                val springBouncyOffset = spring<IntOffset>(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
                val springBouncySize = spring<IntSize>(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val totalWidth = maxWidth
                    val gap = 14.dp
                    val isStacked = stagedTracks.isNotEmpty() && totalWidth < 820.dp
                    val halfWidth = if (totalWidth > gap) (totalWidth - gap) / 2 else totalWidth

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        AnimatedVisibility(
                            visible = stagedTracks.isNotEmpty(),
                            enter = slideInHorizontally(
                                animationSpec = springBouncyOffset,
                                initialOffsetX = { -it }
                            ) + expandHorizontally(
                                animationSpec = springBouncySize,
                                expandFrom = Alignment.Start
                            ) + fadeIn(animationSpec = springBouncyFloat),
                            exit = slideOutHorizontally(
                                animationSpec = springBouncyOffset,
                                targetOffsetX = { -it }
                            ) + shrinkHorizontally(
                                animationSpec = springBouncySize,
                                shrinkTowards = Alignment.Start
                            ) + fadeOut(animationSpec = springBouncyFloat)
                        ) {
                            val stagingCardShape = RoundedCornerShape(18.dp)
                            val stagingModifier = if (isStacked) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier.width(halfWidth)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Surface(
                                    shape = stagingCardShape,
                                    color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    contentColor = scheme.onSurface,
                                    modifier = stagingModifier
                                        .gamepadFocusOutline(shape = stagingCardShape, register = true)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .animateContentSize(animationSpec = springBouncySize),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = scheme.tertiaryContainer,
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Inventory2,
                                                            contentDescription = null,
                                                            tint = scheme.onTertiaryContainer,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stringResource(R.string.bgm_staging_area_title),
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = scheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${stagedTracks.size} unplaced track(s)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = scheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (stagedTracks.isNotEmpty()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = scheme.tertiaryContainer,
                                                        contentColor = scheme.onTertiaryContainer
                                                    ) {
                                                        Text(
                                                            text = "${stagedTracks.size}",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val restoreAllShape = RoundedCornerShape(12.dp)
                                                Surface(
                                                    shape = restoreAllShape,
                                                    color = scheme.tertiaryContainer,
                                                    contentColor = scheme.onTertiaryContainer,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .gamepadFocusOutline(shape = restoreAllShape, register = true)
                                                        .clip(restoreAllShape)
                                                        .clickable {
                                                            AudioUtil.playOk(context)
                                                            val allStaged = stagedTracks.toList()
                                                            allStaged.forEach { track ->
                                                                BgmManager.restoreFromStaging(context, track.id)
                                                            }
                                                            if (playlist.size + allStaged.size >= 3) {
                                                                playlistExpanded = true
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Add,
                                                            contentDescription = stringResource(R.string.bgm_restore_all),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = stringResource(R.string.bgm_restore_all),
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }

                                                val clearAllShape = RoundedCornerShape(12.dp)
                                                Surface(
                                                    shape = clearAllShape,
                                                    color = scheme.surfaceContainerHighest,
                                                    contentColor = scheme.error.copy(alpha = 0.85f),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .gamepadFocusOutline(shape = clearAllShape, register = true)
                                                        .clip(clearAllShape)
                                                        .clickable {
                                                            val toDelete = stagedTracks.toList()
                                                            toDelete.forEach { track ->
                                                                BgmManager.deletePermanently(context, track.id)
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.DeleteOutline,
                                                            contentDescription = stringResource(R.string.bgm_clear_all),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(
                                                            text = stringResource(R.string.bgm_clear_all),
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(
                                            color = scheme.outlineVariant.copy(alpha = 0.4f),
                                            thickness = 1.dp
                                        )

                                        val visibleStaged = if (stagingExpanded) stagedTracks else stagedTracks.take(3)
                                        val isStagingScrollable = stagingExpanded && stagedTracks.size > 5

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (isStagingScrollable) {
                                                        Modifier
                                                            .heightIn(max = 312.dp)
                                                            .verticalScroll(stagingScrollState)
                                                    } else {
                                                        Modifier
                                                    }
                                                ),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            visibleStaged.forEach { track ->
                                                key(track.id) {
                                                    val isExiting = track.id in exitingStagedTrackIds
                                                    AnimatedVisibility(
                                                        visible = !isExiting,
                                                        enter = if (isInitialComposition) EnterTransition.None else {
                                                            slideInHorizontally(
                                                                animationSpec = springBouncyOffset,
                                                                initialOffsetX = { it }
                                                            ) + expandVertically(
                                                                animationSpec = springBouncySize
                                                            ) + fadeIn(animationSpec = springBouncyFloat)
                                                        },
                                                        exit = slideOutHorizontally(
                                                            animationSpec = springBouncyOffset,
                                                            targetOffsetX = { it }
                                                        ) + shrinkVertically(
                                                            animationSpec = springBouncySize
                                                        ) + fadeOut(animationSpec = springBouncyFloat)
                                                    ) {
                                                        val trackItemShape = RoundedCornerShape(12.dp)
                                                        val trackBgColor = if (track.isDefault) {
                                                            scheme.tertiaryContainer.copy(alpha = 0.45f)
                                                        } else {
                                                            scheme.surfaceContainerHighest.copy(alpha = 0.6f)
                                                        }
                                                        val trackContentColor = scheme.onSurface
                                                        val trackMutedColor = if (track.isDefault) {
                                                            scheme.onSurface.copy(alpha = 0.85f)
                                                        } else {
                                                            scheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                        }

                                                        Surface(
                                                            shape = trackItemShape,
                                                            color = trackBgColor,
                                                            contentColor = trackContentColor,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .gamepadFocusOutline(shape = trackItemShape, register = true)
                                                                .clip(trackItemShape)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.size(24.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = if (track.isDefault) Icons.Rounded.AutoAwesome else Icons.Rounded.MusicNote,
                                                                        contentDescription = null,
                                                                        tint = if (track.isDefault) scheme.tertiary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }

                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = track.title,
                                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                                            fontWeight = FontWeight.Medium
                                                                        ),
                                                                        color = scheme.onSurface,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    if (track.isDefault) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Rounded.AutoAwesome,
                                                                                contentDescription = null,
                                                                                tint = trackMutedColor,
                                                                                modifier = Modifier.size(11.dp)
                                                                            )
                                                                            Text(
                                                                                text = stringResource(R.string.bgm_default_track_badge),
                                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                                                color = trackMutedColor,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis,
                                                                                modifier = Modifier.weight(1f, fill = false)
                                                                            )
                                                                        }
                                                                    } else {
                                                                        val subtitle = track.artist ?: track.fileName
                                                                        Text(
                                                                            text = subtitle,
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }
                                                                }

                                                                if (track.formattedDuration.isNotEmpty()) {
                                                                    Text(
                                                                        text = track.formattedDuration,
                                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                                        color = trackMutedColor
                                                                    )
                                                                }

                                                                val restoreShape = CircleShape
                                                                Surface(
                                                                    shape = restoreShape,
                                                                    color = scheme.primaryContainer,
                                                                    contentColor = scheme.onPrimaryContainer,
                                                                    modifier = Modifier
                                                                        .size(32.dp)
                                                                        .gamepadFocusOutline(shape = restoreShape, register = true)
                                                                        .clip(restoreShape)
                                                                        .clickable(enabled = !isExiting) {
                                                                            scope.launch {
                                                                                AudioUtil.playOk(context)
                                                                                exitingStagedTrackIds = exitingStagedTrackIds + track.id
                                                                                delay(320)
                                                                                BgmManager.restoreFromStaging(context, track.id)
                                                                                exitingStagedTrackIds = exitingStagedTrackIds - track.id
                                                                                if (playlist.size >= 3) {
                                                                                    playlistExpanded = true
                                                                                }
                                                                            }
                                                                        }
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(
                                                                            imageVector = Icons.Rounded.Add,
                                                                            contentDescription = stringResource(R.string.bgm_restore_track),
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }
                                                                }

                                                                if (!track.isDefault) {
                                                                    val deleteShape = CircleShape
                                                                    Surface(
                                                                        shape = deleteShape,
                                                                        color = Color.Transparent,
                                                                        contentColor = scheme.error.copy(alpha = 0.8f),
                                                                        modifier = Modifier
                                                                            .size(32.dp)
                                                                            .gamepadFocusOutline(shape = deleteShape, register = true)
                                                                            .clip(deleteShape)
                                                                            .clickable(enabled = !isExiting) {
                                                                                scope.launch {
                                                                                    exitingStagedTrackIds = exitingStagedTrackIds + track.id
                                                                                    delay(320)
                                                                                    BgmManager.deletePermanently(context, track.id)
                                                                                    exitingStagedTrackIds = exitingStagedTrackIds - track.id
                                                                                }
                                                                            }
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Icon(
                                                                                imageVector = Icons.Rounded.DeleteOutline,
                                                                                contentDescription = stringResource(R.string.bgm_delete_permanent),
                                                                                modifier = Modifier.size(18.dp)
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

                                        if (stagedTracks.size > 3) {
                                            val showMoreShape = RoundedCornerShape(10.dp)
                                            Surface(
                                                shape = showMoreShape,
                                                color = scheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                                contentColor = scheme.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .gamepadFocusOutline(shape = showMoreShape, register = true)
                                                    .clip(showMoreShape)
                                                    .clickable {
                                                        stagingExpanded = !stagingExpanded
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = stringResource(if (stagingExpanded) R.string.bgm_show_less else R.string.bgm_show_more),
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = if (stagingExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!isStacked) {
                                    Spacer(Modifier.width(gap))
                                }
                            }
                        }

                        val playlistCardShape = RoundedCornerShape(18.dp)
                        val playlistModifier = if (isStacked) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.weight(1f)
                        }

                        Surface(
                            shape = playlistCardShape,
                            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            contentColor = scheme.onSurface,
                            modifier = playlistModifier
                                .gamepadFocusOutline(shape = playlistCardShape, register = true)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .animateContentSize(animationSpec = springBouncySize),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = scheme.primaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                                    contentDescription = null,
                                                    tint = scheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.bgm_playlist_title),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = scheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = when {
                                                    playlist.isEmpty() -> "No active tracks"
                                                    playlist.size <= 1 -> "Single track looping"
                                                    isShuffleEnabled -> "${playlist.size} tracks • Shuffle mode"
                                                    else -> "${playlist.size} tracks • Sequential loop"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = scheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (playlist.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = scheme.primaryContainer,
                                                contentColor = scheme.onPrimaryContainer
                                            ) {
                                                Text(
                                                    text = "${playlist.size}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val shuffleBtnShape = RoundedCornerShape(12.dp)
                                        Surface(
                                            shape = shuffleBtnShape,
                                            color = if (isShuffleEnabled) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                            contentColor = if (isShuffleEnabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .gamepadFocusOutline(shape = shuffleBtnShape, register = true)
                                                .clip(shuffleBtnShape)
                                                .clickable {
                                                    BgmManager.toggleShuffle(context)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Shuffle,
                                                    contentDescription = stringResource(R.string.bgm_shuffle),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.bgm_shuffle),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        val addBtnShape = RoundedCornerShape(12.dp)
                                        Surface(
                                            shape = addBtnShape,
                                            color = scheme.primary,
                                            contentColor = scheme.onPrimary,
                                            modifier = Modifier
                                                .weight(1f)
                                                .gamepadFocusOutline(shape = addBtnShape, register = true)
                                                .clip(addBtnShape)
                                                .clickable {
                                                    audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.bgm_add_track),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = scheme.outlineVariant.copy(alpha = 0.4f),
                                    thickness = 1.dp
                                )

                                if (playlist.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp, horizontal = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                                    contentDescription = null,
                                                    tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Playlist is empty",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = scheme.onSurface
                                        )
                                        Text(
                                            text = "Restore music from staging or tap Add Music",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    val visiblePlaylist = if (playlistExpanded) playlist else playlist.take(3)
                                    val isPlaylistScrollable = playlistExpanded && playlist.size > 5

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isPlaylistScrollable) {
                                                    Modifier
                                                        .heightIn(max = 312.dp)
                                                        .verticalScroll(playlistScrollState)
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        visiblePlaylist.forEach { track ->
                                            key(track.id) {
                                                val isExiting = track.id in exitingPlaylistTrackIds
                                                AnimatedVisibility(
                                                    visible = !isExiting,
                                                    enter = if (isInitialComposition) EnterTransition.None else {
                                                        slideInHorizontally(
                                                            animationSpec = springBouncyOffset,
                                                            initialOffsetX = { -it }
                                                        ) + expandVertically(
                                                            animationSpec = springBouncySize
                                                        ) + fadeIn(animationSpec = springBouncyFloat)
                                                    },
                                                    exit = slideOutHorizontally(
                                                        animationSpec = springBouncyOffset,
                                                        targetOffsetX = { -it }
                                                    ) + shrinkVertically(
                                                        animationSpec = springBouncySize
                                                    ) + fadeOut(animationSpec = springBouncyFloat)
                                                ) {
                                                    val isActive = track.id == currentTrack?.id
                                                    val trackItemShape = RoundedCornerShape(12.dp)
                                                    val realIndex = playlist.indexOfFirst { it.id == track.id }
                                                    val trackBgColor = when {
                                                        isActive -> scheme.secondaryContainer
                                                        track.isDefault -> scheme.tertiaryContainer.copy(alpha = 0.45f)
                                                        else -> scheme.surfaceContainerHighest.copy(alpha = 0.6f)
                                                    }
                                                    val trackContentColor = if (isActive) scheme.onSecondaryContainer else scheme.onSurface
                                                    // onTertiaryContainer is a mid tone and lands at 2:1 on its own container, so the theme row takes the surface ink instead.
                                                    val trackControlTint = when {
                                                        isActive -> scheme.onSecondaryContainer
                                                        track.isDefault -> scheme.onSurface
                                                        else -> scheme.onSurfaceVariant
                                                    }

                                                    Surface(
                                                        shape = trackItemShape,
                                                        color = trackBgColor,
                                                        contentColor = trackContentColor,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .gamepadFocusOutline(shape = trackItemShape, register = true)
                                                            .clip(trackItemShape)
                                                            .clickable(enabled = !isExiting) {
                                                                BgmManager.selectTrack(context, track)
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.size(24.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (isActive && isPlaying) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.GraphicEq,
                                                                        contentDescription = stringResource(R.string.bgm_playing),
                                                                        tint = scheme.primary,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                } else if (isActive) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.PlayArrow,
                                                                        contentDescription = stringResource(R.string.bgm_stopped),
                                                                        tint = scheme.primary,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                } else if (track.isDefault) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.AutoAwesome,
                                                                        contentDescription = null,
                                                                        tint = scheme.tertiary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                } else {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.MusicNote,
                                                                        contentDescription = null,
                                                                        tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }

                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = track.title,
                                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                                                    ),
                                                                    color = if (isActive) scheme.onSecondaryContainer else scheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                if (track.isDefault) {
                                                                    Row(
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Rounded.AutoAwesome,
                                                                            contentDescription = null,
                                                                            tint = trackControlTint.copy(alpha = 0.85f),
                                                                            modifier = Modifier.size(11.dp)
                                                                        )
                                                                        Text(
                                                                            text = stringResource(R.string.bgm_default_track_badge),
                                                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                                            color = trackControlTint.copy(alpha = 0.85f),
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis,
                                                                            modifier = Modifier.weight(1f, fill = false)
                                                                        )
                                                                    }
                                                                } else {
                                                                    val subtitle = track.artist ?: track.fileName
                                                                    Text(
                                                                        text = subtitle,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = if (isActive) scheme.onSecondaryContainer.copy(alpha = 0.75f) else scheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }

                                                            if (track.formattedDuration.isNotEmpty()) {
                                                                Text(
                                                                    text = track.formattedDuration,
                                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                                                    color = trackControlTint.copy(alpha = 0.85f)
                                                                )
                                                            }

                                                            if (playlist.size > 1 && realIndex in playlist.indices && !isExiting) {
                                                                if (realIndex > 0) {
                                                                    Surface(
                                                                        shape = CircleShape,
                                                                        color = Color.Transparent,
                                                                        modifier = Modifier
                                                                            .size(28.dp)
                                                                            .clip(CircleShape)
                                                                            .clickable {
                                                                                BgmManager.moveTrack(context, realIndex, realIndex - 1)
                                                                            }
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Icon(
                                                                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                                                                contentDescription = stringResource(R.string.bgm_move_up),
                                                                                tint = trackControlTint,
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                                if (realIndex < playlist.lastIndex) {
                                                                    Surface(
                                                                        shape = CircleShape,
                                                                        color = Color.Transparent,
                                                                        modifier = Modifier
                                                                            .size(28.dp)
                                                                            .clip(CircleShape)
                                                                            .clickable {
                                                                                BgmManager.moveTrack(context, realIndex, realIndex + 1)
                                                                            }
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Icon(
                                                                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                                                                contentDescription = stringResource(R.string.bgm_move_down),
                                                                                tint = trackControlTint,
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            val deleteShape = CircleShape
                                                            Surface(
                                                                shape = deleteShape,
                                                                color = Color.Transparent,
                                                                contentColor = trackControlTint.copy(alpha = 0.9f),
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .gamepadFocusOutline(shape = deleteShape, register = true)
                                                                    .clip(deleteShape)
                                                                    .clickable(enabled = !isExiting) {
                                                                        scope.launch {
                                                                            exitingPlaylistTrackIds = exitingPlaylistTrackIds + track.id
                                                                            delay(320)
                                                                            BgmManager.removeTrack(context, track.id)
                                                                            exitingPlaylistTrackIds = exitingPlaylistTrackIds - track.id
                                                                        }
                                                                    }
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.RemoveCircleOutline,
                                                                        contentDescription = stringResource(R.string.bgm_unstage_track),
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (playlist.size > 3) {
                                        val showMoreShape = RoundedCornerShape(10.dp)
                                        Surface(
                                            shape = showMoreShape,
                                            color = scheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                            contentColor = scheme.primary,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .gamepadFocusOutline(shape = showMoreShape, register = true)
                                                .clip(showMoreShape)
                                                .clickable {
                                                    playlistExpanded = !playlistExpanded
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(if (playlistExpanded) R.string.bgm_show_less else R.string.bgm_show_more),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (playlistExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }

                if (playlist.size > 1 || playlist.any { !it.isDefault } || stagedTracks.isNotEmpty()) {
                    val resetBtnShape = RoundedCornerShape(14.dp)
                    Surface(
                        shape = resetBtnShape,
                        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        contentColor = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .gamepadFocusOutline(shape = resetBtnShape, register = true)
                            .clip(resetBtnShape)
                            .clickable {
                                BgmManager.resetPlaylistToDefault(context)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bgm_reset_playlist),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }


                SettingsSwitchCard(
                    icon = Icons.Rounded.GraphicEq,
                    title = stringResource(R.string.bgm_now_playing_title),
                    summary = stringResource(R.string.bgm_now_playing_desc),
                    checked = nowPlayingEnabled,
                    onCheckedChange = { BgmManager.setNowPlayingEnabled(context, it) }
                )
            }
        }
    }

    if (isImportingMusic) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.5.dp,
                        color = scheme.primary
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.bgm_importing_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.bgm_importing_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
