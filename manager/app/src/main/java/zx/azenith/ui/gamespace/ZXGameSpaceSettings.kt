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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.ThermostatAuto
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WebStories
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import com.materialkolor.rememberDynamicColorScheme
import com.topjohnwu.superuser.Shell
import zx.azenith.ui.theme.ColorEngine
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.util.clearHeaderImage
import zx.azenith.ui.util.dumpDiagnosticLogs
import zx.azenith.ui.util.getHeaderImage
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import zx.azenith.ui.util.saveHeaderImage
import zx.azenith.ui.util.saveMediaDirectly
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

    Row(
        modifier = modifier.padding(top = 4.dp, bottom = 12.dp),
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

                var tabPositions by remember { mutableStateOf(mapOf<SettingsSidebarTab, Pair<Float, Float>>()) }
                val currentPos = tabPositions[selectedTab] ?: (0f to 0f)

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
                        modifier = Modifier.fillMaxWidth(),
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
                                    .clickable { onSelectTab(tab) }
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
    var sliderSensitivity by remember(sidePanelSensitivity) {
        mutableIntStateOf(sidePanelSensitivity)
    }
    var colorModeValue by remember { mutableIntStateOf(prefs.getInt("color_mode", ColorMode.SYSTEM.value)) }
    var keyColorValue by remember { mutableIntStateOf(prefs.getInt("key_color", 0)) }
    var appScalePercent by remember {
        mutableIntStateOf(prefs.getInt("gamespace_app_scale", 85).coerceIn(60, 150))
    }
    var sliderScalePercent by remember(appScalePercent) {
        mutableIntStateOf(appScalePercent)
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
                    val updated = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
                    sidePanelSensitivity = updated
                    sliderSensitivity = updated
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val currentColorSpec = remember {
        ColorEngine.fromPersisted(prefs.getString("color_spec", null))
    }

    val isDarkPreview = when (ColorMode.fromValue(colorModeValue)) {
        ColorMode.LIGHT -> false
        ColorMode.DARK, ColorMode.DARKAMOLED -> true
        ColorMode.SYSTEM -> systemDark
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

        // 0. Customize Side Panel (Bento layout alignment)
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
                                Triple("left", "Left Side", "Pull out from left edge"),
                                Triple("right", "Right Side", "Pull out from right edge")
                            )
                            sideOptions.forEach { (sideKey, title, subtitle) ->
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
                                        .clickable {
                                            sidePanelSide = sideKey
                                            prefs.edit().putString("gamespace_sidepanel_side", sideKey).apply()
                                        }
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

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (selected) {
                                                    scheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                } else {
                                                    scheme.onSurfaceVariant
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
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
                                text = when {
                                    sliderSensitivity <= 45 -> "Less Sensitive · ${sliderSensitivity}%"
                                    sliderSensitivity >= 80 -> "More Sensitive · ${sliderSensitivity}%"
                                    else -> "Balanced · ${sliderSensitivity}%"
                                },
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
                                                    sidePanelSensitivity = targetVal
                                                    prefs.edit().putInt("gamespace_sidepanel_sensitivity", targetVal).apply()
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
                                        if (sliderSensitivity != sidePanelSensitivity) {
                                            sidePanelSensitivity = sliderSensitivity
                                            prefs.edit().putInt("gamespace_sidepanel_sensitivity", sliderSensitivity).apply()
                                        }
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

        // 1. Home Wallpaper (Stage & Hub Wallpaper Customization)
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

                    Surface(
                        shape = CircleShape,
                        color = if (customWallpaperUri != null) scheme.primaryContainer else scheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (customWallpaperUri != null) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = scheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = if (customWallpaperUri != null) "Custom Wallpaper" else "Default Wallpaper",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (customWallpaperUri != null) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                            )
                        }
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
                            if (customWallpaperUri != null) {
                                MediaBannerRenderer(
                                    uriString = customWallpaperUri,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.banner_bg),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f))
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (customWallpaperUri != null) "Custom Stage Wallpaper" else "Default Theme Wallpaper",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Action Buttons Column
                    Column(
                        modifier = Modifier.weight(0.52f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chooseShape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = chooseShape,
                            color = scheme.primary,
                            contentColor = scheme.onPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .gamepadFocusOutline(shape = chooseShape, register = true)
                                .clip(chooseShape)
                                .clickable {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (customWallpaperUri != null) "Change Wallpaper" else "Choose Wallpaper",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }

                        if (customWallpaperUri != null) {
                            val resetShape = RoundedCornerShape(16.dp)
                            Surface(
                                shape = resetShape,
                                color = scheme.surfaceContainerHighest,
                                contentColor = scheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .gamepadFocusOutline(shape = resetShape, register = true)
                                    .clip(resetShape)
                                    .clickable {
                                        context.clearHeaderImage()
                                        customWallpaperUri = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = scheme.error
                                    )
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
            }
        }

        // 2. Theme Mode (Auto / Light / Dark / AMOLED)
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
                                .clickable {
                                    colorModeValue = value
                                    prefs.edit().putInt("color_mode", value).apply()
                                }
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

        // 3. App Scale (60% .. 150%, step 5%, default 85%, applies on finger lift)
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
                                .clickable(enabled = appScalePercent > 60) {
                                    val next = (appScalePercent - 5).coerceIn(60, 150)
                                    appScalePercent = next
                                    sliderScalePercent = next
                                    prefs.edit().putInt("gamespace_app_scale", next).apply()
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
                                    appScalePercent = 85
                                    sliderScalePercent = 85
                                    prefs.edit().putInt("gamespace_app_scale", 85).apply()
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
                                .clickable(enabled = appScalePercent < 150) {
                                    val next = (appScalePercent + 5).coerceIn(60, 150)
                                    appScalePercent = next
                                    sliderScalePercent = next
                                    prefs.edit().putInt("gamespace_app_scale", next).apply()
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
                        if (sliderScalePercent != appScalePercent) {
                            appScalePercent = sliderScalePercent
                            prefs.edit().putInt("gamespace_app_scale", sliderScalePercent).apply()
                        }
                    },
                    valueRange = 60f..150f,
                    steps = 17,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }
        }

        // 4. App Theme Palette (Follow Monet vs Existing Preset Seed Colors)
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
                                "Following system Monet dynamic wallpaper palette"
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
                                .clickable {
                                    keyColorValue = 0
                                    prefs.edit().putInt("key_color", 0).apply()
                                }
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
                                    text = "Follow Monet",
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
                                        keyColorValue = firstPreset
                                        prefs.edit().putInt("key_color", firstPreset).apply()
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
                        items = GAMESPACE_PRESET_KEY_COLORS,
                        key = { it.first }
                    ) { (colorInt, name) ->
                        GameSpacePaletteSwatch(
                            seedColor = Color(colorInt),
                            label = name,
                            selected = keyColorValue == colorInt,
                            isDark = isDarkPreview,
                            colorSpec = currentColorSpec,
                            onClick = {
                                keyColorValue = colorInt
                                prefs.edit().putInt("key_color", colorInt).apply()
                            }
                        )
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
    colorSpec: ColorEngine,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val swatchScheme = if (seedColor == Color.Unspecified) {
        val baseScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = baseScheme.primary,
            isDark = isDark,
            specVersion = colorSpec.librarySpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            specVersion = colorSpec.librarySpec
        )
    }

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
        color = if (selected) swatchScheme.primaryContainer else swatchScheme.surfaceContainerHighest,
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
                color = if (selected) swatchScheme.onPrimaryContainer else swatchScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.section_about),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface
            )
            Text(
                text = "Project information, architecture specifications, and community links",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = rememberDrawablePainter(
                                context.applicationInfo.loadIcon(context.packageManager)
                            ),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(scheme.primaryContainer)
                        )

                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = scheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.str_version_buildconfig_version_na, BuildConfig.VERSION_NAME),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = scheme.primary
                            )
                            Text(
                                text = "$deviceName · $chipsetName",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "COMMUNITY & CODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = scheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Code,
                            title = "GitHub",
                            summary = "Source repository",
                            onClick = { uriHandler.openUri("https://github.com/Liliya2727/AZenith") }
                        )
                        SettingsActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Share,
                            title = "Telegram",
                            summary = "Discussion group",
                            onClick = { uriHandler.openUri("https://t.me/ArchHavenDisc") }
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.str_maintainer).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = scheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.avatar_liliya),
                            contentDescription = stringResource(R.string.str_liliya),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.str_liliya),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.str_creator_maintainer),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = stringResource(R.string.str_open_source).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = scheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.str_licenses) + " · Apache 2.0",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Built with Material 3 Expressive, Android NDK, Kotlin Coroutines & Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
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
            .clickable { onCheckedChange(!checked) }
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

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.onPrimary,
                    checkedTrackColor = scheme.primary,
                    uncheckedThumbColor = scheme.outline,
                    uncheckedTrackColor = scheme.surfaceContainerHighest
                )
            )
        }
    }
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
            .clickable(onClick = onClick)
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
