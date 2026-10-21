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

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import zx.azenith.ui.component.gamepadKeyWatch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.tapHaptic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.ThermostatAuto
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WebStories
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.material3.Material3
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.BuildConfig
import zx.azenith.R
import zx.azenith.ui.component.MediaBannerRenderer
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.ScaleConfirmationDialog
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.topjohnwu.superuser.Shell
import zx.azenith.ui.theme.ColorEngine
import zx.azenith.ui.theme.ColorMode
import android.widget.Toast
import zx.azenith.ui.component.AppIconImage
import androidx.compose.ui.platform.LocalConfiguration
import zx.azenith.ui.util.BgmManager
import zx.azenith.ui.util.BgmTrack
import zx.azenith.ui.util.resolveGlobalGameWallpaperMode
import zx.azenith.ui.util.batchResetGameWallpapers
import zx.azenith.ui.util.batchSetGameWallpaper
import zx.azenith.ui.util.batchSetGameWallpaperMode
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.dumpDiagnosticLogs
import zx.azenith.ui.util.getGlobalGameWallpaperMode
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.getHomeWallpaperBlur
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import zx.azenith.ui.util.isHomeWallpaperEnabled
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.saveMediaDirectly
import zx.azenith.ui.util.setGlobalGameWallpaperMode
import zx.azenith.ui.util.setHomeWallpaperBlur
import zx.azenith.ui.util.setHomeWallpaperEnabled
import zx.azenith.ui.viewmodel.ApplistViewmodel
import zx.azenith.ui.viewmodel.SettingsViewModel
import zx.azenith.ui.viewmodel.TweakViewModel
import zx.azenith.ui.gamespace.settings.SettingsGeneralPane
import zx.azenith.ui.gamespace.settings.SettingsTweaksPane
import zx.azenith.ui.gamespace.settings.SettingsAppearancePane


@Composable
internal fun ZXGameSpaceSettingsStage(
    selectedTab: SettingsSidebarTab = SettingsSidebarTab.General,
    onSelectTab: (SettingsSidebarTab) -> Unit = {},
    profile: String = "",
    profileValue: String = "",
    refreshHz: Int = 0,
    deviceName: String = "",
    chipsetName: String = "",
    onApplyProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val density = LocalDensity.current
    val context = LocalContext.current

    Row(
        modifier = modifier.padding(top = 4.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.82f),
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = scheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                )

                val sidebarScrollState = rememberScrollState()
                var sidebarHeightPx by remember { mutableIntStateOf(0) }
                var tabPositions by remember { mutableStateOf(mapOf<SettingsSidebarTab, Pair<Float, Float>>()) }
                val currentPos = tabPositions[selectedTab] ?: (0f to 0f)

                LaunchedEffect(selectedTab, currentPos.first, currentPos.second, sidebarHeightPx) {
                    if (currentPos.second > 0f && sidebarHeightPx > 0) {
                        val targetTop = currentPos.first.roundToInt()
                        val targetBottom = (currentPos.first + currentPos.second).roundToInt()
                        val currentScroll = sidebarScrollState.value
                        if (targetTop < currentScroll) {
                            sidebarScrollState.animateScrollTo(targetTop)
                        } else if (targetBottom > currentScroll + sidebarHeightPx) {
                            sidebarScrollState.animateScrollTo((targetBottom - sidebarHeightPx).coerceAtLeast(0))
                        }
                    }
                }

                val animatedY by animateFloatAsState(
                    targetValue = currentPos.first,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "SidebarChipY"
                )
                val animatedHeight by animateFloatAsState(
                    targetValue = currentPos.second,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "SidebarChipHeight"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onGloballyPositioned { sidebarHeightPx = it.size.height }
                        .verticalScroll(sidebarScrollState)
                ) {
                    if (animatedHeight > 0f) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, animatedY.roundToInt()) }
                                .height(with(density) { animatedHeight.toDp() })
                        ) {}
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsSidebarTab.entries.forEach { tab ->
                            val isSelected = tab == selectedTab
                            val itemShape = RoundedCornerShape(16.dp)
                            val contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurfaceVariant

                            Surface(
                                shape = itemShape,
                                color = Color.Transparent,
                                contentColor = contentColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        val parentY = coordinates.positionInParent().y
                                        val height = coordinates.size.height.toFloat()
                                        if (tabPositions[tab]?.first != parentY || tabPositions[tab]?.second != height) {
                                            tabPositions = tabPositions + (tab to (parentY to height))
                                        }
                                    }
                                    .gamepadFocusOutline(shape = itemShape, register = true)
                                    .clip(itemShape)
                                    .clickable {
                                        AudioUtil.playOk(context)
                                        onSelectTab(tab)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerLow.copy(alpha = 0.72f),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), initialScale = 0.98f))
                        .togetherWith(fadeOut(motion.defaultEffectsSpec()) + scaleOut(motion.defaultSpatialSpec(), targetScale = 1.01f))
                },
                modifier = Modifier.fillMaxSize(),
                label = "SettingsPaneTransition"
            ) { tab ->
                when (tab) {
                    SettingsSidebarTab.General -> SettingsGeneralPane()
                    SettingsSidebarTab.Tweaks -> SettingsTweaksPane(
                        profile = profile,
                        profileValue = profileValue,
                        refreshHz = refreshHz,
                        onApplyProfile = onApplyProfile
                    )
                    SettingsSidebarTab.Appearance -> SettingsAppearancePane()
                    SettingsSidebarTab.Audio -> SettingsAudioPane()
                    SettingsSidebarTab.About -> SettingsAboutPane(
                        deviceName = deviceName,
                        chipsetName = chipsetName
                    )
                }
            }
        }
    }
}


@Composable
internal fun SettingsAudioPane(
    settingsViewModel: SettingsViewModel = viewModel()
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
        modifier = Modifier
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
                            Row(verticalAlignment = Alignment.Top) {
                                val stagingCardShape = RoundedCornerShape(18.dp)
                                Surface(
                                    shape = stagingCardShape,
                                    color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                                    contentColor = scheme.onSurface,
                                    modifier = Modifier
                                        .width(halfWidth)
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
                                Spacer(Modifier.width(gap))
                            }
                        }

                        val playlistCardShape = RoundedCornerShape(18.dp)
                        Surface(
                            shape = playlistCardShape,
                            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                            contentColor = scheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
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

@Composable
internal fun SettingsAboutPane(
    deviceName: String = "",
    chipsetName: String = ""
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    var showLicensesDialog by remember { mutableStateOf(false) }
    var showLibrariesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.section_about),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Project information, credits, open-source licenses, and community links",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        // 1. Hero App Info & Community Links Bento Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = rememberDrawablePainter(
                            context.applicationInfo.loadIcon(context.packageManager)
                        ),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.surfaceVariant)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.str_version_buildconfig_version_na, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (deviceName.isNotBlank() || chipsetName.isNotBlank()) {
                            Text(
                                text = listOf(deviceName, chipsetName).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val btnShape = RoundedCornerShape(16.dp)
                    Surface(
                        shape = btnShape,
                        color = scheme.primaryContainer,
                        contentColor = scheme.onPrimaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .gamepadFocusOutline(shape = btnShape, register = true)
                            .clip(btnShape)
                            .clickable { uriHandler.openUri("https://t.me/ArchHavenDisc") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_telegram),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.str_support_group),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = btnShape,
                        color = scheme.surfaceContainerHighest,
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .gamepadFocusOutline(shape = btnShape, register = true)
                            .clip(btnShape)
                            .clickable { uriHandler.openUri("https://t.me/ZeshArch") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_telegram),
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.str_update_channel),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Open Source Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.str_open_source).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = scheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            SettingsActionCard(
                icon = Icons.Rounded.Gavel,
                title = stringResource(R.string.str_licenses),
                summary = stringResource(R.string.str_apache_license_2),
                trailingBadge = "Apache 2.0",
                onClick = { showLicensesDialog = true }
            )

            SettingsActionCard(
                icon = Icons.Rounded.LibraryBooks,
                title = stringResource(R.string.str_libraries_used),
                summary = stringResource(R.string.str_libraries_hint),
                onClick = { showLibrariesDialog = true }
            )

            SettingsActionCard(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.str_source_code),
                summary = stringResource(R.string.str_view_the_source_code_on_github),
                onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
            )
        }

        // 3. Maintainer Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.str_maintainer).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = scheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            DeveloperCreditCard(
                avatarRes = R.drawable.avatar_liliya,
                name = stringResource(R.string.str_liliya),
                role = stringResource(R.string.str_creator_maintainer),
                githubUrl = "https://github.com/Liliya2727",
                telegramUrl = "https://t.me/Zexshia",
                onOpenUrl = { uriHandler.openUri(it) }
            )
        }

        // 4. Collaborators Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.str_collaborators).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = scheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            DeveloperCreditCard(
                avatarRes = R.drawable.avatar_xia,
                name = stringResource(R.string.str_rianixia),
                role = stringResource(R.string.str_co_maintainer),
                githubUrl = "https://github.com/ryanistr",
                telegramUrl = "https://t.me/rianixia",
                onOpenUrl = { uriHandler.openUri(it) }
            )

            DeveloperCreditCard(
                avatarRes = R.drawable.avatar_kanao,
                name = stringResource(R.string.str_kanaochar),
                role = stringResource(R.string.str_co_maintainer),
                githubUrl = "https://github.com/kanaodnd",
                telegramUrl = "https://t.me/kanaochar",
                onOpenUrl = { uriHandler.openUri(it) }
            )
        }
    }

    if (showLicensesDialog) {
        Dialog(
            onDismissRequest = { showLicensesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .padding(16.dp)
                    .tapHaptic()
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = scheme.primaryContainer,
                                contentColor = scheme.onPrimaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Gavel,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.str_apache_license_2),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.str_license_copyright),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showLicensesDialog = false }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.str_project_license_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.str_view_full_license),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith/blob/main/LICENSE") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                contentDescription = stringResource(R.string.cd_github),
                                modifier = Modifier.size(18.dp),
                                tint = scheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.str_view_on_github),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    SettingsActionCard(
                        icon = Icons.Rounded.LibraryBooks,
                        title = stringResource(R.string.str_libraries_used),
                        summary = stringResource(R.string.str_libraries_hint),
                        onClick = {
                            showLicensesDialog = false
                            showLibrariesDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showLibrariesDialog) {
        val libraries by produceLibraries(R.raw.aboutlibraries)
        Dialog(
            onDismissRequest = { showLibrariesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = scheme.surfaceContainerHigh,
                contentColor = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .fillMaxHeight(0.86f)
                    .tapHaptic()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
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
                                        imageVector = Icons.Rounded.LibraryBooks,
                                        contentDescription = null,
                                        tint = scheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.str_libraries_used),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.str_libraries_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showLibrariesDialog = false }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (libraries == null) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            LibrariesContainer(
                                libraries = libraries,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperCreditCard(
    avatarRes: Int,
    name: String,
    role: String,
    githubUrl: String,
    telegramUrl: String,
    onOpenUrl: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconBtnShape = RoundedCornerShape(12.dp)
                Surface(
                    shape = iconBtnShape,
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(40.dp)
                        .gamepadFocusOutline(shape = iconBtnShape, register = true)
                        .clip(iconBtnShape)
                        .clickable { onOpenUrl(githubUrl) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github),
                            contentDescription = stringResource(R.string.cd_github),
                            modifier = Modifier.size(22.dp),
                            tint = scheme.primary
                        )
                    }
                }

                Surface(
                    shape = iconBtnShape,
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier
                        .size(40.dp)
                        .gamepadFocusOutline(shape = iconBtnShape, register = true)
                        .clip(iconBtnShape)
                        .clickable { onOpenUrl(telegramUrl) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = stringResource(R.string.cd_telegram),
                            modifier = Modifier.size(20.dp),
                            tint = scheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable {
                val newVal = !checked
                if (newVal) {
                    AudioUtil.playOk(context)
                } else {
                    AudioUtil.playCancel(context)
                }
                onCheckedChange(newVal)
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (checked) scheme.primaryContainer else scheme.surfaceContainerHighest,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            GameSpaceSwitch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

/** Material 3 Switch with checkmark and X icon morphing in the thumb for GameSpace. */
@Composable
internal fun GameSpaceSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val handleCheckedChange: ((Boolean) -> Unit)? = if (onCheckedChange != null) {
        { newVal ->
            if (newVal) {
                AudioUtil.playOk(context)
            } else {
                AudioUtil.playCancel(context)
            }
            onCheckedChange(newVal)
        }
    } else null

    Switch(
        checked = checked,
        onCheckedChange = handleCheckedChange,
        enabled = enabled,
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(140)) + scaleIn(animationSpec = tween(140), initialScale = 0.5f))
                        .togetherWith(fadeOut(animationSpec = tween(90)) + scaleOut(animationSpec = tween(90), targetScale = 0.5f))
                },
                label = "GameSpaceSwitchThumb"
            ) { isChecked ->
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (enabled) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = scheme.surfaceContainerHighest,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Composable
internal fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailingBadge: String? = null
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable {
                AudioUtil.playOk(context)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = scheme.secondaryContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = scheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!trailingBadge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
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
