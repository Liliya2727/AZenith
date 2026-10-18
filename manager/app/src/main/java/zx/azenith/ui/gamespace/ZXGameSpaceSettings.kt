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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
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
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Share
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
internal fun SettingsGeneralPane(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.gamespace_settings_general),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
                Text(
                    text = "Core daemon behavior, system services, and diagnostic utilities",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            statusFeedback?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.show_notifications),
                    summary = "Post status notifications when active profile switches",
                    checked = uiState.profileNotifications,
                    onCheckedChange = { settingsViewModel.setProfileNotifications(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.Vibration,
                    title = stringResource(R.string.tap_haptic_feedback),
                    summary = "Tactile impulse feedback on interactive taps and toggles",
                    checked = uiState.tapHaptic,
                    onCheckedChange = { settingsViewModel.onTapHapticToggle(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.show_toast),
                    summary = "Display brief toast notifications on profile and state switches",
                    checked = uiState.stateToast,
                    onCheckedChange = { settingsViewModel.setShowToast(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.allow_verbose_log),
                    summary = "Write extended debug diagnostics to /data/adb/.config/AZenith",
                    checked = uiState.debugMode,
                    onCheckedChange = { settingsViewModel.setDebugMode(it) }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.disable_auto_mode),
                    summary = "Lock daemon in active mode instead of pausing during screen-off",
                    checked = uiState.autoMode,
                    onCheckedChange = { settingsViewModel.setAutoMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.disable_tweak),
                    summary = stringResource(R.string.disable_tweak_desc),
                    checked = uiState.disableTweak,
                    onCheckedChange = { settingsViewModel.setDisableTweak(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.HourglassEmpty,
                    title = stringResource(R.string.profile_timeout),
                    summary = stringResource(R.string.profile_timeout_desc),
                    checked = uiState.profileTimeout,
                    onCheckedChange = { settingsViewModel.setProfileTimeout(it) }
                )
            }
        }

        Text(
            text = stringResource(R.string.section_diagnostics).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = scheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.RestartAlt,
                title = stringResource(R.string.restart_service),
                summary = stringResource(R.string.restart_service_desc),
                onClick = {
                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service --rerun").submit { res ->
                        statusFeedback = if (res.isSuccess) {
                            context.getString(R.string.toast_restarting_service)
                        } else {
                            "Restart failed"
                        }
                    }
                }
            )
            SettingsActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Save,
                title = stringResource(R.string.save_log),
                summary = stringResource(R.string.save_log_desc),
                onClick = {
                    coroutineScope.launch {
                        val file = dumpDiagnosticLogs(context, saveToDownloads = true)
                        statusFeedback = if (file != null && file.exists()) {
                            context.getString(R.string.toast_log_saved)
                        } else {
                            context.getString(R.string.toast_log_save_fail)
                        }
                    }
                }
            )
        }
    }
}

@Composable
internal fun SettingsTweaksPane(
    profile: String = "",
    profileValue: String = "",
    refreshHz: Int = 0,
    onApplyProfile: (String) -> Unit = {},
    tweakViewModel: TweakViewModel = viewModel()
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (!tweakViewModel.isUiLoaded) {
            tweakViewModel.loadAllConfiguration(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.nav_tweaks),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
                Text(
                    text = "Kernel governor, scheduling optimizations, and hardware accelerators",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val profiles = listOf(
                Triple("1", R.string.Profile_Performance, R.drawable.ic_performance),
                Triple("2", R.string.Profile_Balanced, R.drawable.ic_balanced),
                Triple("3", R.string.Profile_ECO_mode, R.drawable.ic_eco)
            )
            profiles.forEach { (value, labelRes, iconRes) ->
                val active = profileValue == value || (value == "2" && profileValue.isBlank())
                val shape = RoundedCornerShape(18.dp)
                Surface(
                    shape = shape,
                    color = if (active) scheme.primaryContainer else scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .gamepadFocusOutline(shape = shape, register = true)
                        .clip(shape)
                        .clickable { onApplyProfile(value) }
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
                            modifier = Modifier.size(24.dp)
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.perf_lite_mode),
                    summary = stringResource(R.string.perf_lite_mode_desc),
                    checked = tweakViewModel.liteState ?: false,
                    onCheckedChange = { tweakViewModel.updateLiteMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.RocketLaunch,
                    title = stringResource(R.string.game_preload),
                    summary = stringResource(R.string.game_preload_desc),
                    checked = tweakViewModel.preloadState ?: false,
                    onCheckedChange = { tweakViewModel.updatePreloadMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.CleaningServices,
                    title = stringResource(R.string.memory_killer),
                    summary = stringResource(R.string.memory_killer_desc),
                    checked = tweakViewModel.memKillerState ?: false,
                    onCheckedChange = { tweakViewModel.updateMemoryKiller(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.SwapVerticalCircle,
                    title = stringResource(R.string.app_priority_control),
                    summary = stringResource(R.string.app_priority_control_desc),
                    checked = tweakViewModel.appPriorState ?: false,
                    onCheckedChange = { tweakViewModel.updateAppPriority(it) }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsSwitchCard(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    title = stringResource(R.string.dnd_mode_gaming),
                    summary = stringResource(R.string.dnd_mode_gaming_desc),
                    checked = tweakViewModel.dndState ?: false,
                    onCheckedChange = { tweakViewModel.updateDndMode(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.ContentCut,
                    title = stringResource(R.string.trim_filesystem),
                    summary = stringResource(R.string.trim_filesystem_desc),
                    checked = tweakViewModel.fstrimState ?: false,
                    onCheckedChange = { tweakViewModel.updateFstrim(it) }
                )
                SettingsSwitchCard(
                    icon = Icons.Rounded.ThermostatAuto,
                    title = stringResource(R.string.thermalcore_service),
                    summary = stringResource(R.string.thermalcore_service_desc),
                    checked = tweakViewModel.thermalState ?: false,
                    onCheckedChange = { tweakViewModel.updateThermalCore(it) }
                )

                SettingsActionCard(
                    icon = Icons.Rounded.WebStories,
                    title = stringResource(R.string.refreshrates),
                    summary = "Display refresh rate locked during gaming",
                    trailingBadge = if (refreshHz > 0) "${refreshHz}Hz" else "${tweakViewModel.currentRefreshRate ?: 60}Hz",
                    onClick = {
                        val supported = getSupportedRefreshRatesPicker(context)
                        if (supported.isNotEmpty()) {
                            val curr = (tweakViewModel.currentRefreshRate ?: 60).toString()
                            val currIdx = supported.indexOf(curr)
                            val next = if (currIdx >= 0 && currIdx < supported.lastIndex) supported[currIdx + 1] else supported.first()
                            tweakViewModel.executeSetRefreshRates(next, context)
                        }
                    }
                )
                SettingsActionCard(
                    icon = Icons.Rounded.SettingsSuggest,
                    title = stringResource(R.string.renderengine),
                    summary = "HWUI graphics compositing backend",
                    trailingBadge = tweakViewModel.currentRenderer?.take(10) ?: "Default",
                    onClick = {
                        val current = (tweakViewModel.currentRenderer ?: "default").lowercase()
                        val next = when {
                            current.contains("vk") -> "default"
                            current.contains("gl") -> "skiavk"
                            else -> "skiagl"
                        }
                        tweakViewModel.executeSetRenderer(next, context)
                    }
                )
            }
        }
    }
}



@Composable
internal fun SettingsAppearancePane() {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val systemDark = isSystemInDarkTheme()

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var customWallpaperUri by remember { mutableStateOf(context.getHeaderImage()) }
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch(Dispatchers.IO) {
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
                    context.saveHeaderImage(savedUri)
                    withContext(Dispatchers.Main) {
                        customWallpaperUri = savedUri
                    }
                }
            }
        }
    }

    var sidePanelSide by remember {
        mutableStateOf(prefs.getString("gamespace_sidepanel_side", "left") ?: "left")
    }
    var sidePanelSensitivity by remember {
        mutableIntStateOf(prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100))
    }
    var colorModeValue by remember { mutableIntStateOf(prefs.getInt("color_mode", ColorMode.SYSTEM.value)) }
    var keyColorValue by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }
    var appScalePercent by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150))
    }
    var confirmedScale by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150))
    }
    var pendingScaleConfirm by remember { mutableStateOf<Int?>(null) }
    var wallpaperDimmingIntensity by remember {
        mutableIntStateOf(prefs.getInt("gamespace_wallpaper_dimming_intensity", 75).coerceIn(15, 100))
    }
    var isWallpaperEnabled by remember {
        mutableStateOf(context.isHomeWallpaperEnabled())
    }
    var wallpaperBlur by remember {
        mutableIntStateOf(context.getHomeWallpaperBlur())
    }
    var floatingShapesEnabled by remember {
        mutableStateOf(prefs.getBoolean("gamespace_floating_shapes_enabled", true))
    }

    var colorSpec by remember {
        mutableStateOf(ColorEngine.fromPersisted(prefs.getString("color_spec", null)))
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "header_image_uri" -> {
                    customWallpaperUri = context.getHeaderImage()
                }
                "gamespace_sidepanel_side" -> {
                    sidePanelSide = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
                }
                "gamespace_sidepanel_sensitivity" -> {
                    sidePanelSensitivity = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
                }
                "gamespace_wallpaper_dimming_intensity" -> {
                    wallpaperDimmingIntensity = prefs.getInt("gamespace_wallpaper_dimming_intensity", 75).coerceIn(15, 100)
                }
                "gamespace_wallpaper_enabled", "enable_banner_image" -> {
                    isWallpaperEnabled = context.isHomeWallpaperEnabled()
                }
                "gamespace_wallpaper_blur" -> {
                    wallpaperBlur = context.getHomeWallpaperBlur()
                }
                "gamespace_floating_shapes_enabled" -> {
                    floatingShapesEnabled = prefs.getBoolean("gamespace_floating_shapes_enabled", true)
                }
                "color_mode" -> {
                    colorModeValue = prefs.getInt("color_mode", ColorMode.SYSTEM.value)
                }
                "color_spec" -> {
                    colorSpec = ColorEngine.fromPersisted(prefs.getString("color_spec", null))
                }
                "key_color" -> {
                    keyColorValue = prefs.getInt("key_color", 0)
                }
                "gamespace_app_scale" -> {
                    val updated = prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150)
                    appScalePercent = updated
                    if (pendingScaleConfirm == null) {
                        confirmedScale = updated
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val isDarkPreview = remember(colorModeValue, systemDark) {
        when (ColorMode.fromValue(colorModeValue)) {
            ColorMode.LIGHT -> false
            ColorMode.DARK, ColorMode.DARKAMOLED -> true
            ColorMode.SYSTEM -> systemDark
        }
    }

    val followMonet = keyColorValue == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Customize Side Panel dock & sensitivity, theme mode, interface scaling, and color palettes",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        CustomizeSidePanelSection(
            sidePanelSide = sidePanelSide,
            initialSensitivity = sidePanelSensitivity,
            onSideChanged = {
                sidePanelSide = it
                prefs.edit().putString("gamespace_sidepanel_side", it).apply()
            },
            onSensitivityCommitted = {
                sidePanelSensitivity = it
                prefs.edit().putInt("gamespace_sidepanel_sensitivity", it).apply()
            }
        )

        HomeWallpaperSection(
            customWallpaperUri = customWallpaperUri,
            initialDimming = wallpaperDimmingIntensity,
            initialBlur = wallpaperBlur,
            isWallpaperEnabled = isWallpaperEnabled,
            floatingShapesEnabled = floatingShapesEnabled,
            colorModeValue = colorModeValue,
            onChooseWallpaper = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onResetWallpaper = {
                context.clearHeaderImage()
                customWallpaperUri = null
            },
            onDimmingCommitted = {
                wallpaperDimmingIntensity = it
                prefs.edit().putInt("gamespace_wallpaper_dimming_intensity", it).apply()
            },
            onBlurCommitted = {
                wallpaperBlur = it
                context.setHomeWallpaperBlur(it)
            },
            onWallpaperEnabledChanged = {
                isWallpaperEnabled = it
                context.setHomeWallpaperEnabled(it)
            },
            onFloatingShapesToggled = {
                floatingShapesEnabled = it
                prefs.edit().putBoolean("gamespace_floating_shapes_enabled", it).apply()
            }
        )

        GameWallpaperSection(
            colorModeValue = colorModeValue
        )

        ThemeModeSection(
            colorModeValue = colorModeValue,
            onModeSelected = {
                colorModeValue = it
                prefs.edit().putInt("color_mode", it).apply()
            }
        )

        AppScaleSection(
            initialScale = appScalePercent,
            onScaleCommitted = { next ->
                if (next != confirmedScale) {
                    pendingScaleConfirm = next
                    appScalePercent = next
                    prefs.edit().putInt("gamespace_app_scale", next).apply()
                }
            }
        )

        PaletteSection(
            followMonet = followMonet,
            keyColorValue = keyColorValue,
            isDarkPreview = isDarkPreview,
            colorSpec = colorSpec,
            onFollowMonet = {
                keyColorValue = 0
                prefs.edit().putInt("key_color", 0).apply()
            },
            onSelectPreset = { colorInt ->
                keyColorValue = colorInt
                prefs.edit().putInt("key_color", colorInt).apply()
            }
        )
    }

    pendingScaleConfirm?.let { pendingScale ->
        ScaleConfirmationDialog(
            newScalePercent = pendingScale,
            onConfirm = {
                confirmedScale = pendingScale
                pendingScaleConfirm = null
            },
            onRevert = {
                appScalePercent = confirmedScale
                prefs.edit().putInt("gamespace_app_scale", confirmedScale).apply()
                pendingScaleConfirm = null
            }
        )
    }
}

@Composable
private fun CustomizeSidePanelSection(
    sidePanelSide: String,
    initialSensitivity: Int,
    onSideChanged: (String) -> Unit,
    onSensitivityCommitted: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var sliderSensitivity by remember(initialSensitivity) { mutableIntStateOf(initialSensitivity) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Customize Side Panel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Floating pull-out bar pinned to screen edge when launching an app",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Column: Pinned Edge Position
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pinned Edge Position",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val sideOptions = listOf(
                            "left" to "Left Side",
                            "right" to "Right Side"
                        )
                        sideOptions.forEach { (sideKey, title) ->
                            val selected = sidePanelSide == sideKey
                            val cornerPercent by animateIntAsState(
                                targetValue = if (selected) 28 else 18,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "SidePanelEdgeCorner"
                            )
                            val cardShape = RoundedCornerShape(percent = cornerPercent)
                            Surface(
                                shape = cardShape,
                                color = if (selected) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .gamepadFocusOutline(shape = cardShape, register = true)
                                    .clip(cardShape)
                                    .clickable { onSideChanged(sideKey) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = scheme.surfaceContainerLow,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .align(
                                                        if (sideKey == "left") Alignment.CenterStart
                                                        else Alignment.CenterEnd
                                                    )
                                                    .width(5.dp)
                                                    .height(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (selected) scheme.primary else scheme.onSurfaceVariant
                                                    )
                                            )
                                            Icon(
                                                imageVector = if (sideKey == "left") {
                                                    Icons.Rounded.ChevronRight
                                                } else {
                                                    Icons.AutoMirrored.Rounded.ArrowBack
                                                },
                                                contentDescription = null,
                                                tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .align(
                                                        if (sideKey == "left") Alignment.CenterStart
                                                        else Alignment.CenterEnd
                                                    )
                                                    .padding(
                                                        start = if (sideKey == "left") 8.dp else 0.dp,
                                                        end = if (sideKey == "right") 8.dp else 0.dp
                                                    )
                                                    .size(16.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Column: Swipe-In Sensitivity
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Swipe-In Sensitivity",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "${sliderSensitivity}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = scheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    "Less Sensitive" to 35,
                                    "Balanced" to 65,
                                    "More Sensitive" to 90
                                )
                                presets.forEach { (label, targetVal) ->
                                    val selected = when (targetVal) {
                                        35 -> sliderSensitivity <= 45
                                        90 -> sliderSensitivity >= 80
                                        else -> sliderSensitivity in 46..79
                                    }
                                    val pillRadius by animateIntAsState(
                                        targetValue = if (selected) 50 else 26,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "SensPresetRadius"
                                    )
                                    val pillShape = RoundedCornerShape(percent = pillRadius)
                                    Surface(
                                        shape = pillShape,
                                        color = if (selected) scheme.primary else scheme.surfaceContainerLow,
                                        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .gamepadFocusOutline(shape = pillShape, register = true)
                                            .clip(pillShape)
                                            .clickable {
                                                sliderSensitivity = targetVal
                                                onSensitivityCommitted(targetVal)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            ZenithSlider(
                                value = sliderSensitivity.toFloat(),
                                onValueChange = { raw ->
                                    val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(20, 100)
                                    if (snapped != sliderSensitivity) {
                                        sliderSensitivity = snapped
                                    }
                                },
                                onValueChangeFinished = {
                                    onSensitivityCommitted(sliderSensitivity)
                                },
                                valueRange = 20f..100f,
                                steps = 15,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeWallpaperSection(
    customWallpaperUri: String?,
    initialDimming: Int,
    initialBlur: Int,
    isWallpaperEnabled: Boolean,
    floatingShapesEnabled: Boolean,
    colorModeValue: Int,
    onChooseWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onDimmingCommitted: (Int) -> Unit,
    onBlurCommitted: (Int) -> Unit,
    onWallpaperEnabledChanged: (Boolean) -> Unit,
    onFloatingShapesToggled: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    var sliderDimming by remember(initialDimming) { mutableIntStateOf(initialDimming) }
    var sliderBlur by remember(initialBlur) { mutableIntStateOf(initialBlur) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Wallpaper,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Home Wallpaper",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Customize stage and hub background wallpaper",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isWallpaperEnabled) "Enabled" else "Off",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isWallpaperEnabled) scheme.primary else scheme.onSurfaceVariant
                    )
                    GameSpaceSwitch(
                        checked = isWallpaperEnabled,
                        onCheckedChange = onWallpaperEnabledChanged
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visual Preview Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.surfaceContainerLowest,
                    modifier = Modifier
                        .weight(0.48f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!isWallpaperEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(scheme.surfaceContainerLowest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Wallpaper",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            if (customWallpaperUri != null) {
                                MediaBannerRenderer(
                                    uriString = customWallpaperUri,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (sliderBlur > 0) Modifier.blur(sliderBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.banner_bg),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (sliderBlur > 0) Modifier.blur(sliderBlur.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
                                )
                            }
                            val previewDimScale = (sliderDimming / 75f).coerceIn(0.15f, 1.45f)
                            val isAmoled = colorModeValue == ColorMode.DARKAMOLED.value
                            val previewBaseDim = if (isAmoled) Color.Black else scheme.surfaceContainerLowest

                            // GameSpace horizontal gradient dimming
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colorStops = arrayOf(
                                                0.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.88f else 0.85f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                0.35f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.84f else 0.80f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                0.68f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.44f else 0.38f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                1.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.22f else 0.18f) * previewDimScale).coerceIn(0f, 0.98f))
                                            )
                                        )
                                    )
                            )
                            // GameSpace vertical vignette
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.32f else 0.28f) * previewDimScale).coerceIn(0f, 0.98f)),
                                                0.45f to Color.Transparent,
                                                1.00f to previewBaseDim.copy(alpha = ((if (isAmoled) 0.48f else 0.42f) * previewDimScale).coerceIn(0f, 0.98f))
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // Action Buttons Column (Symmetrical 110.dp matching the preview height)
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .height(110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chooseShape = RoundedCornerShape(16.dp)
                    Surface(
                        shape = chooseShape,
                        color = scheme.primary,
                        contentColor = scheme.onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .gamepadFocusOutline(shape = chooseShape, register = true)
                            .clip(chooseShape)
                            .clickable(onClick = onChooseWallpaper)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (customWallpaperUri != null) "Change Wallpaper" else "Choose Wallpaper",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = customWallpaperUri != null,
                        enter = fadeIn(motion.defaultEffectsSpec()) + expandVertically(motion.defaultSpatialSpec()),
                        exit = fadeOut(motion.defaultEffectsSpec()) + shrinkVertically(motion.defaultSpatialSpec())
                    ) {
                        val resetShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = resetShape,
                            color = scheme.surfaceContainerHighest,
                            contentColor = scheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .gamepadFocusOutline(shape = resetShape, register = true)
                                .clip(resetShape)
                                .clickable(onClick = onResetWallpaper)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = scheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Reset to Default",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.error,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Dimming Intensity Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dimming Intensity",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Adjust wallpaper gradient dimming opacity",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$sliderDimming%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                }

                ZenithSlider(
                    value = sliderDimming.toFloat(),
                    onValueChange = { raw ->
                        val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(15, 100)
                        if (snapped != sliderDimming) {
                            sliderDimming = snapped
                        }
                    },
                    onValueChangeFinished = {
                        onDimmingCommitted(sliderDimming)
                    },
                    valueRange = 15f..100f,
                    steps = 16,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }

            // Wallpaper Blur Slider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wallpaper Blur",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Adjust background depth-of-field blur",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (sliderBlur == 0) "Sharp (0 dp)" else "$sliderBlur dp",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                }

                ZenithSlider(
                    value = sliderBlur.toFloat(),
                    onValueChange = { raw ->
                        val snapped = raw.roundToInt().coerceIn(0, 25)
                        if (snapped != sliderBlur) {
                            sliderBlur = snapped
                        }
                    },
                    onValueChangeFinished = {
                        onBlurCommitted(sliderBlur)
                    },
                    valueRange = 0f..25f,
                    steps = 24,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }

            // Floating Shapes Toggle Card
            val shapesToggleShape = RoundedCornerShape(18.dp)
            Surface(
                shape = shapesToggleShape,
                color = if (floatingShapesEnabled) scheme.surfaceContainerHighest.copy(alpha = 0.65f) else scheme.surfaceContainerHighest.copy(alpha = 0.35f),
                contentColor = scheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .gamepadFocusOutline(shape = shapesToggleShape, register = true)
                    .clip(shapesToggleShape)
                    .clickable { onFloatingShapesToggled(!floatingShapesEnabled) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (floatingShapesEnabled) scheme.primaryContainer else scheme.surfaceContainerHighest,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (floatingShapesEnabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Floating Shapes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Material 3 shapes hovering & morphing on homescreen",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                    GameSpaceSwitch(
                        checked = floatingShapesEnabled,
                        onCheckedChange = onFloatingShapesToggled
                    )
                }
            }
        }
    }
}

@Composable
private fun GameWallpaperSection(
    colorModeValue: Int
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    val motion = MaterialTheme.motionScheme
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        if (ApplistViewmodel.apps.isEmpty()) {
            ApplistViewmodel().loadApps(context)
        }
    }

    val installed = ApplistViewmodel.apps
    val targetGames = remember(installed) {
        val configured = installed.filter { it.isEnabledInConfig || it.isRecommended }
        if (configured.isNotEmpty()) configured else installed.filter { !it.isSystem }
    }

    var selectedMode by remember(targetGames) {
        mutableStateOf(context.resolveGlobalGameWallpaperMode(targetGames.map { it.packageName }))
    }

    DisposableEffect(prefs, targetGames) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "gamespace_global_game_wallpaper_mode" || key?.startsWith("game_wallpaper_") == true) {
                selectedMode = context.resolveGlobalGameWallpaperMode(targetGames.map { it.packageName })
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var showBatchDialog by remember { mutableStateOf(false) }
    var pendingBatchUri by remember { mutableStateOf<String?>(null) }

    val batchImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            coroutineScope.launch(Dispatchers.IO) {
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
                    withContext(Dispatchers.Main) {
                        pendingBatchUri = savedUri
                        showBatchDialog = true
                    }
                }
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Game Wallpaper",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Default background wallpaper mode and batch artwork across games",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            GameWallpaperOptionRow(
                title = "Default",
                subtitle = "Dynamic artwork generated from each game's icon",
                icon = Icons.Rounded.AutoAwesome,
                isSelected = selectedMode == "default",
                onClick = {
                    selectedMode = "default"
                    context.setGlobalGameWallpaperMode("default")
                    val pkgs = targetGames.map { it.packageName }
                    if (pkgs.isNotEmpty()) {
                        context.batchSetGameWallpaperMode(pkgs, "default")
                    }
                }
            )

            GameWallpaperOptionRow(
                title = "Follow Home Wallpaper",
                subtitle = "All games mirror the current Home tab wallpaper",
                icon = Icons.Rounded.Sync,
                isSelected = selectedMode == "follow_home",
                onClick = {
                    selectedMode = "follow_home"
                    context.setGlobalGameWallpaperMode("follow_home")
                    val pkgs = targetGames.map { it.packageName }
                    if (pkgs.isNotEmpty()) {
                        context.batchSetGameWallpaperMode(pkgs, "follow_home")
                    }
                }
            )

            GameWallpaperOptionRow(
                title = "Custom",
                subtitle = "Configure wallpaper individually per games",
                icon = Icons.Rounded.Palette,
                isSelected = selectedMode == "custom",
                onClick = {
                    selectedMode = "custom"
                    context.setGlobalGameWallpaperMode("custom")
                }
            )

            AnimatedVisibility(
                visible = selectedMode == "custom",
                enter = fadeIn(motion.defaultEffectsSpec()) + expandVertically(motion.defaultSpatialSpec()),
                exit = fadeOut(motion.defaultEffectsSpec()) + shrinkVertically(motion.defaultSpatialSpec())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val batchShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = batchShape,
                            color = scheme.primary,
                            contentColor = scheme.onPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .gamepadFocusOutline(shape = batchShape, register = true)
                                .clip(batchShape)
                                .clickable {
                                    batchImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Choose Wallpaper",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }

                        val resetShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = resetShape,
                            color = scheme.surfaceContainerHighest,
                            contentColor = scheme.error,
                            modifier = Modifier
                                .gamepadFocusOutline(shape = resetShape, register = true)
                                .clip(resetShape)
                                .clickable {
                                    val pkgs = targetGames.map { it.packageName }
                                    if (pkgs.isNotEmpty()) {
                                        context.batchResetGameWallpapers(pkgs)
                                        Toast.makeText(context, "Reset all game wallpapers to default", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = scheme.error
                                    )
                                    Text(
                                        text = "Reset All",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = scheme.error,
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

    if (showBatchDialog && pendingBatchUri != null) {
        ApplyGameWallpaperDialog(
            uri = pendingBatchUri!!,
            games = targetGames,
            onDismiss = {
                showBatchDialog = false
                pendingBatchUri = null
            },
            onApply = { selectedPackages ->
                if (selectedPackages.isNotEmpty()) {
                    context.batchSetGameWallpaper(selectedPackages, pendingBatchUri!!)
                    Toast.makeText(
                        context,
                        "Applied wallpaper to ${selectedPackages.size} game${if (selectedPackages.size > 1) "s" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showBatchDialog = false
                pendingBatchUri = null
            }
        )
    }
}

@Composable
private fun GameWallpaperOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        shape = cardShape,
        color = if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.5f),
        contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .gamepadFocusOutline(shape = cardShape, register = true)
            .clip(cardShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) scheme.primary else scheme.surfaceContainerHighest,
                contentColor = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ApplyGameWallpaperDialog(
    uri: String,
    games: List<ApplistViewmodel.AppInfo>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var applyToAll by remember { mutableStateOf(true) }
    var selectedSet by remember { mutableStateOf(games.map { it.packageName }.toSet()) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
            modifier = (if (isLandscape) {
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.96f)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            } else {
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
                    .padding(12.dp)
            })
                .tapHaptic()
                .gamepadKeyWatch()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                         event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ESCAPE)
                    ) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = if (isLandscape) 10.dp else 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.primaryContainer,
                            contentColor = scheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Apply Wallpaper",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Choose either for all games or selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }

                // Middle Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Left Pane: preview & option toggles
                            Column(
                                modifier = Modifier
                                    .width(220.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scheme.surfaceContainerLowest,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    MediaBannerRenderer(
                                        uriString = uri,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                val allShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = allShape,
                                    color = if (applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    contentColor = if (applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(allShape)
                                        .clickable { applyToAll = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = "All Games (${games.size})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Apply to all library games",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (applyToAll) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                val selectShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = selectShape,
                                    color = if (!applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    contentColor = if (!applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(selectShape)
                                        .clickable { applyToAll = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (!applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (!applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = "Select Games (${selectedSet.size})",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Choose specific games",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (!applyToAll) scheme.onPrimaryContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Pane: Game list or All Games confirmation
                            if (applyToAll) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = scheme.surfaceContainerLow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = scheme.primaryContainer,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = scheme.primary
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "All ${games.size} games will receive this artwork",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = scheme.onSurface
                                        )
                                        Text(
                                            text = "Every game stage will showcase this wallpaper",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            items(games) { game ->
                                                AppIconImage(app = game, size = 26.dp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedSet.size} of ${games.size} selected",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = scheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(
                                                onClick = { selectedSet = games.map { it.packageName }.toSet() },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text("Select All", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(
                                                onClick = { selectedSet = emptySet() },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text("Deselect All", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(games) { game ->
                                            val isChecked = selectedSet.contains(game.packageName)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isChecked) scheme.primaryContainer.copy(alpha = 0.35f) else scheme.surfaceContainerLow,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        selectedSet = if (isChecked) {
                                                            selectedSet - game.packageName
                                                        } else {
                                                            selectedSet + game.packageName
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    AppIconImage(app = game, size = 26.dp)
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = game.label,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                            color = scheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = game.packageName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = scheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            selectedSet = if (checked) {
                                                                selectedSet + game.packageName
                                                            } else {
                                                                selectedSet - game.packageName
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Portrait layout
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = scheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                MediaBannerRenderer(
                                    uriString = uri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val allShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = allShape,
                                    color = if (applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                    contentColor = if (applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(allShape)
                                        .clickable { applyToAll = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "All Games (${games.size})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                val selectShape = RoundedCornerShape(12.dp)
                                Surface(
                                    shape = selectShape,
                                    color = if (!applyToAll) scheme.primaryContainer else scheme.surfaceContainerHighest,
                                    contentColor = if (!applyToAll) scheme.onPrimaryContainer else scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(selectShape)
                                        .clickable { applyToAll = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (!applyToAll) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (!applyToAll) scheme.primary else scheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Select Games",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            if (!applyToAll) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${selectedSet.size} of ${games.size} selected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { selectedSet = games.map { it.packageName }.toSet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Select All", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(
                                            onClick = { selectedSet = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Deselect All", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(games) { game ->
                                        val isChecked = selectedSet.contains(game.packageName)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isChecked) scheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    selectedSet = if (isChecked) {
                                                        selectedSet - game.packageName
                                                    } else {
                                                        selectedSet + game.packageName
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                AppIconImage(app = game, size = 32.dp)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = game.label,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = scheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = game.packageName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = scheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedSet = if (checked) {
                                                            selectedSet + game.packageName
                                                        } else {
                                                            selectedSet - game.packageName
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All ${games.size} games will receive this artwork",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    val targetCount = if (applyToAll) games.size else selectedSet.size
                    Button(
                        onClick = {
                            val targetList = if (applyToAll) games.map { it.packageName } else selectedSet.toList()
                            onApply(targetList)
                        },
                        enabled = targetCount > 0,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Apply ($targetCount)")
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSection(
    colorModeValue: Int,
    onModeSelected: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (colorModeValue == ColorMode.DARKAMOLED.value) {
                            "AMOLED mode draws pure black backgrounds where possible"
                        } else {
                            "Auto, Light, and Dark follow Monet dynamic surface tones"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = when (ColorMode.fromValue(colorModeValue)) {
                            ColorMode.SYSTEM -> "Auto"
                            ColorMode.LIGHT -> "Light"
                            ColorMode.DARK -> "Dark"
                            ColorMode.DARKAMOLED -> "AMOLED"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val modes = listOf(
                    Triple("Auto", ColorMode.SYSTEM.value, Icons.Rounded.BrightnessAuto),
                    Triple("Light", ColorMode.LIGHT.value, Icons.Rounded.LightMode),
                    Triple("Dark", ColorMode.DARK.value, Icons.Rounded.DarkMode),
                    Triple("AMOLED", ColorMode.DARKAMOLED.value, Icons.Rounded.Android)
                )
                modes.forEach { (name, value, icon) ->
                    val selected = colorModeValue == value
                    val pillRadius by animateIntAsState(
                        targetValue = if (selected) 50 else 28,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "ThemeModePillRadius"
                    )
                    val shape = RoundedCornerShape(percent = pillRadius)
                    Surface(
                        shape = shape,
                        color = if (selected) scheme.primary else scheme.surfaceContainerHighest,
                        contentColor = if (selected) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .gamepadFocusOutline(shape = shape, register = true)
                            .clip(shape)
                            .clickable { onModeSelected(value) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) scheme.onPrimary else scheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppScaleSection(
    initialScale: Int,
    onScaleCommitted: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var sliderScalePercent by remember(initialScale) { mutableIntStateOf(initialScale) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = null,
                                tint = scheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "App Scale",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "60% downscale to 150% upscale",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stepBtnShape = RoundedCornerShape(12.dp)
                    Surface(
                        shape = stepBtnShape,
                        color = scheme.surfaceContainerHighest,
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .size(34.dp)
                            .gamepadFocusOutline(shape = stepBtnShape, register = true)
                            .clip(stepBtnShape)
                            .clickable(enabled = sliderScalePercent > 60) {
                                val next = (sliderScalePercent - 5).coerceIn(60, 150)
                                sliderScalePercent = next
                                onScaleCommitted(next)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease scale",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (sliderScalePercent == 85) scheme.primaryContainer else scheme.primary,
                        contentColor = if (sliderScalePercent == 85) scheme.onPrimaryContainer else scheme.onPrimary,
                        modifier = Modifier
                            .gamepadFocusOutline(shape = CircleShape, register = true)
                            .clip(CircleShape)
                            .clickable {
                                sliderScalePercent = 85
                                onScaleCommitted(85)
                            }
                    ) {
                        Text(
                            text = "${sliderScalePercent}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = stepBtnShape,
                        color = scheme.surfaceContainerHighest,
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .size(34.dp)
                            .gamepadFocusOutline(shape = stepBtnShape, register = true)
                            .clip(stepBtnShape)
                            .clickable(enabled = sliderScalePercent < 150) {
                                val next = (sliderScalePercent + 5).coerceIn(60, 150)
                                sliderScalePercent = next
                                onScaleCommitted(next)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase scale",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            ZenithSlider(
                value = sliderScalePercent.toFloat(),
                onValueChange = { raw ->
                    val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(60, 150)
                    sliderScalePercent = snapped
                },
                onValueChangeFinished = {
                    onScaleCommitted(sliderScalePercent)
                },
                valueRange = 60f..150f,
                steps = 17,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}

@Composable
private fun PaletteSection(
    followMonet: Boolean,
    keyColorValue: Int,
    isDarkPreview: Boolean,
    colorSpec: ColorEngine = ColorEngine.Default,
    onFollowMonet: () -> Unit,
    onSelectPreset: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val initialVisibleCount = 6
    val isSelectionInOverflow = remember(keyColorValue) {
        GAMESPACE_PRESET_KEY_COLORS.drop(initialVisibleCount).any { it.first == keyColorValue }
    }
    var showAllPresets by remember { mutableStateOf(isSelectionInOverflow) }
    val effectiveShowAll = showAllPresets || isSelectionInOverflow
    val displayedPresets = if (effectiveShowAll) {
        GAMESPACE_PRESET_KEY_COLORS
    } else {
        GAMESPACE_PRESET_KEY_COLORS.take(initialVisibleCount)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Theme Palette",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )
                    Text(
                        text = if (followMonet) {
                            "Following system dynamic wallpaper palette"
                        } else {
                            val presetLabel = GAMESPACE_PRESET_KEY_COLORS.firstOrNull { it.first == keyColorValue }?.second ?: "Custom"
                            "Using $presetLabel preset color seed from Personalization"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val monetPillShape = RoundedCornerShape(percent = if (followMonet) 50 else 28)
                    Surface(
                        shape = monetPillShape,
                        color = if (followMonet) scheme.primary else scheme.surfaceContainerHighest,
                        contentColor = if (followMonet) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .gamepadFocusOutline(shape = monetPillShape, register = true)
                            .clip(monetPillShape)
                            .clickable(onClick = onFollowMonet)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Dynamic",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    val presetPillShape = RoundedCornerShape(percent = if (!followMonet) 50 else 28)
                    Surface(
                        shape = presetPillShape,
                        color = if (!followMonet) scheme.primary else scheme.surfaceContainerHighest,
                        contentColor = if (!followMonet) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .gamepadFocusOutline(shape = presetPillShape, register = true)
                            .clip(presetPillShape)
                            .clickable {
                                if (keyColorValue == 0) {
                                    val firstPreset = GAMESPACE_PRESET_KEY_COLORS.first().first
                                    onSelectPreset(firstPreset)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Preset Color",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = displayedPresets,
                    key = { it.first }
                ) { (colorInt, name) ->
                    GameSpacePaletteSwatch(
                        seedColor = Color(colorInt),
                        label = name,
                        selected = keyColorValue == colorInt,
                        isDark = isDarkPreview,
                        colorSpec = colorSpec,
                        onClick = { onSelectPreset(colorInt) }
                    )
                }

                if (!effectiveShowAll) {
                    item(key = "load_more_presets") {
                        val shape = RoundedCornerShape(percent = 24)
                        Surface(
                            shape = shape,
                            color = scheme.surfaceContainerHighest,
                            modifier = Modifier
                                .width(84.dp)
                                .height(112.dp)
                                .gamepadFocusOutline(shape = shape, register = true)
                                .clip(shape)
                                .clickable { showAllPresets = true }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Load more presets",
                                            tint = scheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "+${GAMESPACE_PRESET_KEY_COLORS.size - initialVisibleCount} More",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else if (!isSelectionInOverflow) {
                    item(key = "show_less_presets") {
                        val shape = RoundedCornerShape(percent = 24)
                        Surface(
                            shape = shape,
                            color = scheme.surfaceContainerHighest,
                            modifier = Modifier
                                .width(84.dp)
                                .height(112.dp)
                                .gamepadFocusOutline(shape = shape, register = true)
                                .clip(shape)
                                .clickable { showAllPresets = false }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = scheme.surfaceContainerHigh,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.ExpandLess,
                                            contentDescription = "Show less presets",
                                            tint = scheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Show Less",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.onSurfaceVariant,
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
internal fun GameSpacePaletteSwatch(
    seedColor: Color,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    colorSpec: ColorEngine = ColorEngine.Default,
    onClick: () -> Unit
) {
    val currentScheme = MaterialTheme.colorScheme
    val swatchScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        specVersion = colorSpec.librarySpec
    )

    val cornerPercent by animateIntAsState(
        targetValue = if (selected) 36 else 24,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SwatchCornerPercent"
    )
    val shape = RoundedCornerShape(percent = cornerPercent)

    Surface(
        shape = shape,
        color = if (selected) swatchScheme.primaryContainer else currentScheme.surfaceContainerHighest,
        modifier = Modifier
            .width(84.dp)
            .gamepadFocusOutline(shape = shape, register = true)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(46.dp)) {
                    drawArc(
                        color = swatchScheme.primary,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchScheme.tertiaryContainer,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = swatchScheme.secondaryContainer,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }

                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = swatchScheme.primary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = swatchScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(swatchScheme.primary)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (selected) swatchScheme.onPrimaryContainer else currentScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SettingsAudioPane(
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scheme = MaterialTheme.colorScheme

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
                text = "Sound effects, interaction feedback, and audio behavior",
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
