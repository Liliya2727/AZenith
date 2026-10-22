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

package zx.azenith.ui.gamespace.settings

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.ThermostatAuto
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WebStories
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.superuser.Shell
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.ZenithSlider
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.gamespace.AdaptiveSettingsGrid
import zx.azenith.ui.gamespace.SettingsActionCard
import zx.azenith.ui.gamespace.SettingsSwitchCard
import zx.azenith.ui.gamespace.springPressBounce
import zx.azenith.ui.subscreens.ColorPreset
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RebootManager
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.util.getChipsetVendor
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
import zx.azenith.ui.viewmodel.AutoModeBus
import zx.azenith.ui.viewmodel.TweakViewModel

internal enum class TweaksSubTab {
    Main,
    BypassCharging,
    Governors,
    Preferenced,
    FpsGo,
    DisplayColor
}

private val blockLargeCorner = 22.dp
private val blockSmallCorner = 4.dp

private val blockTopShape = RoundedCornerShape(
    topStart = blockLargeCorner,
    topEnd = blockLargeCorner,
    bottomStart = blockSmallCorner,
    bottomEnd = blockSmallCorner
)
private val blockMiddleShape = RoundedCornerShape(blockSmallCorner)
private val blockBottomShape = RoundedCornerShape(
    topStart = blockSmallCorner,
    topEnd = blockSmallCorner,
    bottomStart = blockLargeCorner,
    bottomEnd = blockLargeCorner
)
private val blockSingleShape = RoundedCornerShape(blockLargeCorner)

@Composable
internal fun ZXGameSpaceSettingsTweaks(
    profile: String = "",
    profileValue: String = "",
    refreshHz: Int = 0,
    onApplyProfile: (String) -> Unit = {},
    tweakViewModel: TweakViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    SettingsTweaksPane(
        profile = profile,
        profileValue = profileValue,
        refreshHz = refreshHz,
        onApplyProfile = onApplyProfile,
        tweakViewModel = tweakViewModel,
        modifier = modifier
    )
}

@Composable
internal fun SettingsTweaksPane(
    profile: String = "",
    profileValue: String = "",
    refreshHz: Int = 0,
    onApplyProfile: (String) -> Unit = {},
    tweakViewModel: TweakViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSubTab by remember { mutableStateOf(TweaksSubTab.Main) }

    LaunchedEffect(Unit) {
        if (!tweakViewModel.isUiLoaded) {
            tweakViewModel.loadAllConfiguration(context)
        }
    }

    BackHandler(enabled = currentSubTab != TweaksSubTab.Main) {
        AudioUtil.playCancel(context)
        currentSubTab = TweaksSubTab.Main
    }

    AnimatedContent(
        targetState = currentSubTab,
        transitionSpec = {
            if (targetState != TweaksSubTab.Main) {
                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn())
                    .togetherWith(slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut())
            }
        },
        modifier = modifier.fillMaxSize(),
        label = "TweaksSubTabTransition"
    ) { subTab ->
        when (subTab) {
            TweaksSubTab.Main -> {
                TweaksMainSubPane(
                    profile = profile,
                    profileValue = profileValue,
                    refreshHz = refreshHz,
                    onApplyProfile = onApplyProfile,
                    tweakViewModel = tweakViewModel,
                    onNavigateToSubTab = { target ->
                        AudioUtil.playOk(context)
                        currentSubTab = target
                    }
                )
            }
            TweaksSubTab.BypassCharging -> {
                BypassChargingSubPane(
                    onBackClick = {
                        AudioUtil.playCancel(context)
                        currentSubTab = TweaksSubTab.Main
                    }
                )
            }
            TweaksSubTab.Governors -> {
                GovernorsSubPane(
                    tweakViewModel = tweakViewModel,
                    onBackClick = {
                        AudioUtil.playCancel(context)
                        currentSubTab = TweaksSubTab.Main
                    }
                )
            }
            TweaksSubTab.Preferenced -> {
                PreferencedTweaksSubPane(
                    onBackClick = {
                        AudioUtil.playCancel(context)
                        currentSubTab = TweaksSubTab.Main
                    }
                )
            }
            TweaksSubTab.FpsGo -> {
                FpsGoSubPane(
                    onBackClick = {
                        AudioUtil.playCancel(context)
                        currentSubTab = TweaksSubTab.Main
                    }
                )
            }
            TweaksSubTab.DisplayColor -> {
                DisplayColorSubPane(
                    onBackClick = {
                        AudioUtil.playCancel(context)
                        currentSubTab = TweaksSubTab.Main
                    }
                )
            }
        }
    }
}

@Composable
private fun TweaksMainSubPane(
    profile: String,
    profileValue: String,
    refreshHz: Int,
    onApplyProfile: (String) -> Unit,
    tweakViewModel: TweakViewModel,
    onNavigateToSubTab: (TweaksSubTab) -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    var showRefreshRateDialog by remember { mutableStateOf(false) }
    var showRendererDialog by remember { mutableStateOf(false) }

    val socVendor = remember { getChipsetVendor(context) }
    val isMediaTek = socVendor == "mediatek"

    val bypassEnabled by produceState(false) {
        value = withContext(Dispatchers.IO) {
            PropertyUtils.get("persist.sys.azenithconf.bypasschg", "0") == "1"
        }
    }

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

    var pendingProfileValue by remember { mutableStateOf<String?>(null) }
    var isProfileApplying by remember { mutableStateOf(false) }

    LaunchedEffect(profileValue) {
        if (pendingProfileValue != null && profileValue == pendingProfileValue) {
            delay(180)
            isProfileApplying = false
            pendingProfileValue = null
        }
    }

    LaunchedEffect(isProfileApplying) {
        if (isProfileApplying) {
            delay(4000)
            isProfileApplying = false
            pendingProfileValue = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                color = scheme.onSurfaceVariant.copy(alpha = 0.82f)
            )
        }

        // --- 1. Profile Section ---
        if (!isManualMode) {
            val autoCardShape = RoundedCornerShape(22.dp)
            Surface(
                shape = autoCardShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Auto Mode",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = scheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = when (profileValue) {
                                        "1" -> stringResource(R.string.Profile_Performance)
                                        "3" -> stringResource(R.string.Profile_ECO_mode)
                                        else -> stringResource(R.string.Profile_Balanced)
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = scheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Turn on 'Disable auto mode' in General settings to manually change performance profiles.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                            color = scheme.onSurfaceVariant.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        } else {
            val profiles = listOf(
                Triple("1", R.string.Profile_Performance, R.drawable.ic_performance),
                Triple("2", R.string.Profile_Balanced, R.drawable.ic_balanced),
                Triple("3", R.string.Profile_ECO_mode, R.drawable.ic_eco)
            )
            val currentTarget = pendingProfileValue ?: if (profileValue.isBlank()) "2" else profileValue

            var positions by remember { mutableStateOf(mapOf<String, Pair<Float, Float>>()) }
            val currentPillPos = positions[currentTarget] ?: (0f to 0f)

            val animatedPillX by animateFloatAsState(
                targetValue = currentPillPos.first,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "ProfilePillX"
            )
            val animatedPillWidth by animateFloatAsState(
                targetValue = currentPillPos.second,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "ProfilePillWidth"
            )

            val containerShape = RoundedCornerShape(22.dp)
            val pillItemShape = RoundedCornerShape(18.dp)

            Surface(
                shape = containerShape,
                color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        if (animatedPillWidth > 0f) {
                            Surface(
                                shape = pillItemShape,
                                color = scheme.primaryContainer,
                                modifier = Modifier
                                    .offset { IntOffset(animatedPillX.roundToInt(), 0) }
                                    .width(with(density) { animatedPillWidth.toDp() })
                                    .fillMaxHeight()
                            ) {}
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            profiles.forEach { (value, labelRes, iconRes) ->
                                val isTarget = currentTarget == value
                                val isApplyingThis = isProfileApplying && pendingProfileValue == value

                                val iconScale by animateFloatAsState(
                                    targetValue = if (isTarget) 1.08f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "ProfileIconScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .onGloballyPositioned { coordinates ->
                                            val parentX = coordinates.positionInParent().x
                                            val width = coordinates.size.width.toFloat()
                                            if (positions[value]?.first != parentX || positions[value]?.second != width) {
                                                positions = positions + (value to (parentX to width))
                                            }
                                        }
                                        .clip(pillItemShape)
                                        .springPressBounce(pressedScale = 0.95f)
                                        .clickable {
                                            if (!isProfileApplying && currentTarget != value) {
                                                AudioUtil.playOk(context)
                                                pendingProfileValue = value
                                                isProfileApplying = true
                                                onApplyProfile(value)
                                            }
                                        }
                                        .gamepadFocusOutline(shape = pillItemShape, register = true),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (isApplyingThis) {
                                            LoadingIndicator(
                                                color = if (isTarget) scheme.onPrimaryContainer else scheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(iconRes),
                                                contentDescription = null,
                                                tint = if (isTarget) scheme.onPrimaryContainer else scheme.primary,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .graphicsLayer {
                                                        scaleX = iconScale
                                                        scaleY = iconScale
                                                    }
                                            )
                                        }
                                        Text(
                                            text = stringResource(labelRes),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isTarget) scheme.onPrimaryContainer else scheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isProfileApplying,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LoadingIndicator(
                                color = scheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Applying ${when (pendingProfileValue) { "1" -> "Performance"; "3" -> "ECO"; else -> "Balanced" }} profile...",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = scheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Quick Optimizations ---
        TweaksSectionHeader(stringResource(R.string.section_performance))

        val cards = listOf<@Composable () -> Unit>(
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.perf_lite_mode),
                    summary = stringResource(R.string.perf_lite_mode_desc),
                    checked = tweakViewModel.liteState ?: false,
                    onCheckedChange = { tweakViewModel.updateLiteMode(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.RocketLaunch,
                    title = stringResource(R.string.game_preload),
                    summary = stringResource(R.string.game_preload_desc),
                    checked = tweakViewModel.preloadState ?: false,
                    onCheckedChange = { tweakViewModel.updatePreloadMode(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    title = stringResource(R.string.dnd_mode_gaming),
                    summary = stringResource(R.string.dnd_mode_gaming_desc),
                    checked = tweakViewModel.dndState ?: false,
                    onCheckedChange = { tweakViewModel.updateDndMode(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.ContentCut,
                    title = stringResource(R.string.trim_filesystem),
                    summary = stringResource(R.string.trim_filesystem_desc),
                    checked = tweakViewModel.fstrimState ?: false,
                    onCheckedChange = { tweakViewModel.updateFstrim(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.CleaningServices,
                    title = stringResource(R.string.memory_killer),
                    summary = stringResource(R.string.memory_killer_desc),
                    checked = tweakViewModel.memKillerState ?: false,
                    onCheckedChange = { tweakViewModel.updateMemoryKiller(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.ThermostatAuto,
                    title = stringResource(R.string.thermalcore_service),
                    summary = stringResource(R.string.thermalcore_service_desc),
                    checked = tweakViewModel.thermalState ?: false,
                    onCheckedChange = { tweakViewModel.updateThermalCore(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.SwapVerticalCircle,
                    title = stringResource(R.string.app_priority_control),
                    summary = stringResource(R.string.app_priority_control_desc),
                    checked = tweakViewModel.appPriorState ?: false,
                    onCheckedChange = { tweakViewModel.updateAppPriority(it) }
                )
            }
        )

        AdaptiveSettingsGrid(
            items = cards,
            minColumnWidth = 340.dp,
            spacing = 10.dp
        )

        // --- 3. Display & Rendering Subsystems ---
        TweaksSectionHeader("Display & Rendering")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(blockLargeCorner)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsActionCard(
                icon = Icons.Rounded.WebStories,
                title = stringResource(R.string.refreshrates),
                summary = "Display refresh rate switcher and live rate locking",
                trailingBadge = "${tweakViewModel.currentRefreshRate ?: refreshHz} Hz",
                onClick = { showRefreshRateDialog = true },
                shape = blockTopShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.SettingsSuggest,
                title = stringResource(R.string.renderengine),
                summary = "HWUI 2D graphics hardware compositing pipeline",
                trailingBadge = (tweakViewModel.currentRenderer ?: "Default").uppercase(),
                onClick = { showRendererDialog = true },
                shape = blockMiddleShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.color_scheme),
                summary = "Display surface color calibration and hardware saturation",
                trailingBadge = "KCAL",
                onClick = { onNavigateToSubTab(TweaksSubTab.DisplayColor) },
                shape = blockBottomShape
            )
        }

        // --- 4. Advanced Hardware Subsystems ---
        TweaksSectionHeader(stringResource(R.string.section_addons))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(blockLargeCorner)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsActionCard(
                icon = Icons.Rounded.Cable,
                title = stringResource(R.string.bcharging),
                summary = stringResource(R.string.bcharging_desc),
                trailingBadge = if (bypassEnabled) "Active" else null,
                onClick = { onNavigateToSubTab(TweaksSubTab.BypassCharging) },
                shape = blockTopShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.gov_settings),
                summary = stringResource(R.string.gov_settingsdesc),
                onClick = { onNavigateToSubTab(TweaksSubTab.Governors) },
                shape = blockMiddleShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.prefs),
                summary = stringResource(R.string.prefsdesc),
                onClick = { onNavigateToSubTab(TweaksSubTab.Preferenced) },
                shape = if (isMediaTek) blockMiddleShape else blockBottomShape
            )

            if (isMediaTek) {
                SettingsActionCard(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.str_fpsgo_settings),
                    summary = stringResource(R.string.str_fpsgo_desc),
                    trailingBadge = "MediaTek",
                    onClick = { onNavigateToSubTab(TweaksSubTab.FpsGo) },
                    shape = blockBottomShape
                )
            }
        }
    }

    if (showRefreshRateDialog) {
        val options = remember(context) { getSupportedRefreshRatesPicker(context) }
        val currentRateStr = (tweakViewModel.currentRefreshRate ?: refreshHz).toString()
        val selectedIndex = options.indexOfFirst { it.equals(currentRateStr, ignoreCase = true) }.coerceAtLeast(0)
        ChoiceSelectionDialog(
            title = stringResource(R.string.RefreshRatePicker_Select),
            options = options.map { context.getString(R.string.refresh_rate_format, it) },
            selectedIndex = selectedIndex,
            onSelect = { idx ->
                options.getOrNull(idx)?.let { rate ->
                    tweakViewModel.executeSetRefreshRates(rate, context)
                }
            },
            onDismiss = { showRefreshRateDialog = false }
        )
    }

    if (showRendererDialog) {
        val rendererOptions = listOf(
            context.getString(R.string.Renderer_Default) to "default",
            context.getString(R.string.renderer_skia_vk) to "skiavk",
            context.getString(R.string.renderer_skia_vk_threaded) to "skiavkthreaded",
            context.getString(R.string.renderer_skiagl) to "skiagl",
            context.getString(R.string.renderer_skiagl_threaded) to "skiaglthreaded",
            context.getString(R.string.renderer_opengl_es) to "opengl",
            context.getString(R.string.renderer_opengl_es_threaded) to "openglthreaded",
            context.getString(R.string.Renderer_Vulkan) to "vulkan"
        )
        val currentRenderer = tweakViewModel.currentRenderer ?: "default"
        val selectedIndex = rendererOptions.indexOfFirst { it.second.equals(currentRenderer, ignoreCase = true) }.coerceAtLeast(0)
        ChoiceSelectionDialog(
            title = stringResource(R.string.Renderer_Select),
            options = rendererOptions.map { it.first },
            selectedIndex = selectedIndex,
            onSelect = { idx ->
                rendererOptions.getOrNull(idx)?.let { (_, reason) ->
                    tweakViewModel.executeSetRenderer(reason, context)
                }
            },
            onDismiss = { showRendererDialog = false }
        )
    }
}

// ---------------------------------------------------------------------------
// Subpane 1: Bypass Charging
// ---------------------------------------------------------------------------
@Composable
private fun BypassChargingSubPane(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var bypassPath by remember { mutableStateOf("") }
    var bypassState by remember { mutableStateOf(false) }
    var thresholdValue by remember { mutableFloatStateOf(20f) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    var batteryCapacity by remember { mutableIntStateOf(0) }
    var batteryTempC by remember { mutableFloatStateOf(0f) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            bypassPath = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "")
            bypassState = PropertyUtils.get("persist.sys.azenithconf.bypasschg", "0") == "1"
            val rawThreshold = PropertyUtils.get("persist.sys.azenithconf.bypasschgthreshold", "20")
            thresholdValue = rawThreshold.toFloatOrNull()?.coerceIn(20f, 100f) ?: 20f

            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent != null) {
                    val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (rawLevel >= 0 && scale > 0) {
                        batteryCapacity = (rawLevel * 100) / scale
                    }
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                    if (rawTemp != Int.MIN_VALUE && rawTemp > -500 && rawTemp < 1500) {
                        batteryTempC = rawTemp / 10f
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val bypassBottomCorner by animateDpAsState(
        targetValue = if (bypassState) blockSmallCorner else blockLargeCorner,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BypassBottomCorner"
    )
    val masterSwitchShape = RoundedCornerShape(
        topStart = blockLargeCorner,
        topEnd = blockLargeCorner,
        bottomStart = bypassBottomCorner,
        bottomEnd = bypassBottomCorner
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TweaksSubTabHeader(
            title = stringResource(R.string.bcharging),
            subtitle = "Power device directly from adapter without battery degradation",
            onBackClick = onBackClick
        )

        // Live Telemetry Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CAPACITY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$batteryCapacity%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TEMPERATURE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (batteryTempC > 0f) "${batteryTempC}°C" else "--",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "POWER STATE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (isCharging) "Plugged In" else "Discharging",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCharging) scheme.primary else scheme.onSurface
                    )
                }
            }
        }

        TweaksSectionHeader("Bypass Charging Controls")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(blockLargeCorner)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsSwitchCard(
                icon = Icons.Rounded.BatteryChargingFull,
                title = stringResource(R.string.enable_bypass_charge),
                summary = stringResource(R.string.enable_bypass_charge_desc),
                checked = bypassState,
                onCheckedChange = { isChecked ->
                    bypassState = isChecked
                    val strVal = if (isChecked) "1" else "0"
                    PropertyUtils.set("persist.sys.azenithconf.bypasschg", strVal)
                    scope.launch(Dispatchers.IO) {
                        RootUtils.writeRootFile("/data/adb/.config/AZenith/bypasschgconfig/bypasschg", "$strVal\n")
                    }
                },
                shape = masterSwitchShape
            )

            AnimatedVisibility(
                visible = bypassState,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BypassThresholdSliderCard(
                        threshold = thresholdValue,
                        onThresholdChange = { thresholdValue = it },
                        onThresholdChangeFinished = { finalVal ->
                            val roundVal = finalVal.roundToInt()
                            PropertyUtils.set("persist.sys.azenithconf.bypasschgthreshold", roundVal.toString())
                            scope.launch(Dispatchers.IO) {
                                RootUtils.writeRootFile("/data/adb/.config/AZenith/bypasschgconfig/bypasschgthreshold", "$roundVal\n")
                            }
                        },
                        shape = blockMiddleShape
                    )

                    SettingsActionCard(
                        icon = Icons.Rounded.Cable,
                        title = "Hardware Bypass Path",
                        summary = bypassPath.ifEmpty { "Auto-detected charging switch sysfs node" },
                        trailingBadge = if (bypassPath.isNotBlank()) "Node Bound" else "Auto",
                        onClick = {},
                        shape = blockMiddleShape
                    )

                    SettingsActionCard(
                        icon = Icons.Rounded.Power,
                        title = "Test Hardware Compatibility",
                        summary = testFeedback ?: "Validate charging cutoff control against system kernel",
                        trailingBadge = if (isTesting) "Testing..." else "Run Test",
                        onClick = {
                            if (!isTesting) {
                                isTesting = true
                                testFeedback = "Executing hardware probe..."
                                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service --testbypass").submit { res ->
                                    isTesting = false
                                    testFeedback = if (res.isSuccess) {
                                        val output = res.out.joinToString(" ").trim()
                                        if (output.isNotBlank()) output else "Hardware test passed"
                                    } else {
                                        "Bypass test failed or unsupported"
                                    }
                                }
                            }
                        },
                        shape = blockBottomShape
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Subpane 2: Governors & Schedulers
// ---------------------------------------------------------------------------
@Composable
private fun GovernorsSubPane(
    tweakViewModel: TweakViewModel,
    onBackClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    var choiceDialogTitle by remember { mutableStateOf<String?>(null) }
    var choiceDialogOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var choiceDialogSelectedIndex by remember { mutableIntStateOf(0) }
    var choiceDialogOnSelect by remember { mutableStateOf<((Int) -> Unit)?>(null) }

    val govs = tweakViewModel.availableGovernors ?: emptyList()
    val ioScheds = tweakViewModel.availableIOSchedulers ?: emptyList()
    val maliGovs = tweakViewModel.availableMaliGovernors ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TweaksSubTabHeader(
            title = stringResource(R.string.gov_settings),
            subtitle = "CPU frequency scaling, I/O schedulers, and GPU driver policies",
            onBackClick = onBackClick
        )

        // --- CPU Scaling Section ---
        TweaksSectionHeader(stringResource(R.string.default_cpu_gov))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(blockLargeCorner)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FreqOffsetSliderCard(
                initialIndex = tweakViewModel.freqOffsetIndex ?: 0f,
                labels = tweakViewModel.offsetLabels,
                onSaved = { tweakViewModel.saveFreqOffset(it) },
                shape = blockTopShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.default_cpu_gov),
                summary = stringResource(R.string.default_cpu_gov_desc),
                trailingBadge = tweakViewModel.defaultGovIndex?.let { govs.getOrNull(it) } ?: "Auto",
                onClick = {
                    if (govs.isNotEmpty()) {
                        choiceDialogTitle = "Default CPU Governor"
                        choiceDialogOptions = govs
                        choiceDialogSelectedIndex = tweakViewModel.defaultGovIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updateDefaultGovernor(it) }
                    }
                },
                shape = blockMiddleShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Bolt,
                title = stringResource(R.string.performance_cpu_gov),
                summary = stringResource(R.string.performance_cpu_gov_desc),
                trailingBadge = tweakViewModel.performanceGovIndex?.let { govs.getOrNull(it) } ?: "Auto",
                onClick = {
                    if (govs.isNotEmpty()) {
                        choiceDialogTitle = "Performance CPU Governor"
                        choiceDialogOptions = govs
                        choiceDialogSelectedIndex = tweakViewModel.performanceGovIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updatePerformanceGovernor(it) }
                    }
                },
                shape = blockMiddleShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.powersave_cpu_gov),
                summary = stringResource(R.string.powersave_cpu_gov_desc),
                trailingBadge = tweakViewModel.powersaveGovIndex?.let { govs.getOrNull(it) } ?: "Auto",
                onClick = {
                    if (govs.isNotEmpty()) {
                        choiceDialogTitle = "Powersave CPU Governor"
                        choiceDialogOptions = govs
                        choiceDialogSelectedIndex = tweakViewModel.powersaveGovIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updatePowersaveGovernor(it) }
                    }
                },
                shape = blockBottomShape
            )
        }

        // --- I/O Schedulers Section ---
        TweaksSectionHeader(stringResource(R.string.io_settings))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(blockLargeCorner)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsActionCard(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.balanced_io_scheduler),
                summary = "Storage queue scheduler applied in Balanced profile",
                trailingBadge = tweakViewModel.balancedIOIndex?.let { ioScheds.getOrNull(it) } ?: "Auto",
                onClick = {
                    if (ioScheds.isNotEmpty()) {
                        choiceDialogTitle = "Balanced I/O Scheduler"
                        choiceDialogOptions = ioScheds
                        choiceDialogSelectedIndex = tweakViewModel.balancedIOIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updateBalancedIO(it) }
                    }
                },
                shape = blockTopShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Bolt,
                title = stringResource(R.string.performance_io_scheduler),
                summary = "Storage queue scheduler applied in Performance profile",
                trailingBadge = tweakViewModel.performanceIOIndex?.let { ioScheds.getOrNull(it) } ?: "Auto",
                onClick = {
                    if (ioScheds.isNotEmpty()) {
                        choiceDialogTitle = "Performance I/O Scheduler"
                        choiceDialogOptions = ioScheds
                        choiceDialogSelectedIndex = tweakViewModel.performanceIOIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updatePerformanceIO(it) }
                    }
                },
                shape = blockMiddleShape
            )

            SettingsActionCard(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.powersave_io_scheduler),
                summary = "Storage queue scheduler applied in Powersave profile",
                trailingBadge = tweakViewModel.powersaveIOIndex?.let { ioScheds.getOrNull(it) } ?: "Auto",
                onClick = {
                    if (ioScheds.isNotEmpty()) {
                        choiceDialogTitle = "Powersave I/O Scheduler"
                        choiceDialogOptions = ioScheds
                        choiceDialogSelectedIndex = tweakViewModel.powersaveIOIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updatePowersaveIO(it) }
                    }
                },
                shape = blockBottomShape
            )
        }

        // --- Mali GPU Governors (If Available) ---
        if (tweakViewModel.isMaliGpuAvailable == true && maliGovs.isNotEmpty()) {
            TweaksSectionHeader("Mali GPU Governors")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(blockLargeCorner)),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SettingsActionCard(
                    icon = Icons.Rounded.DeveloperBoard,
                    title = stringResource(R.string.balanced_mali_gov),
                    summary = "Mali driver governor applied in Balanced profile",
                    trailingBadge = tweakViewModel.balancedMaliGovIndex?.let { maliGovs.getOrNull(it) } ?: "Auto",
                    onClick = {
                        choiceDialogTitle = "Balanced Mali GPU Governor"
                        choiceDialogOptions = maliGovs
                        choiceDialogSelectedIndex = tweakViewModel.balancedMaliGovIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updateBalancedMaliGov(it) }
                    },
                    shape = blockTopShape
                )

                SettingsActionCard(
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(R.string.performance_mali_gov),
                    summary = "Mali driver governor applied in Performance profile",
                    trailingBadge = tweakViewModel.performanceMaliGovIndex?.let { maliGovs.getOrNull(it) } ?: "Auto",
                    onClick = {
                        choiceDialogTitle = "Performance Mali GPU Governor"
                        choiceDialogOptions = maliGovs
                        choiceDialogSelectedIndex = tweakViewModel.performanceMaliGovIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updatePerformanceMaliGov(it) }
                    },
                    shape = blockMiddleShape
                )

                SettingsActionCard(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.powersave_mali_gov),
                    summary = "Mali driver governor applied in Powersave profile",
                    trailingBadge = tweakViewModel.powersaveMaliGovIndex?.let { maliGovs.getOrNull(it) } ?: "Auto",
                    onClick = {
                        choiceDialogTitle = "Powersave Mali GPU Governor"
                        choiceDialogOptions = maliGovs
                        choiceDialogSelectedIndex = tweakViewModel.powersaveMaliGovIndex ?: 0
                        choiceDialogOnSelect = { tweakViewModel.updatePowersaveMaliGov(it) }
                    },
                    shape = blockBottomShape
                )
            }
        }
    }

    choiceDialogTitle?.let { title ->
        ChoiceSelectionDialog(
            title = title,
            options = choiceDialogOptions,
            selectedIndex = choiceDialogSelectedIndex,
            onSelect = { choiceDialogOnSelect?.invoke(it) },
            onDismiss = { choiceDialogTitle = null }
        )
    }
}

// ---------------------------------------------------------------------------
// Subpane 3: Preferenced Tweaks
// ---------------------------------------------------------------------------
@Composable
private fun PreferencedTweaksSubPane(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val socVendor = remember { getChipsetVendor(context) }
    val isMediaTek = socVendor == "mediatek"
    val isSnapdragon = socVendor == "qualcomm"

    var schedTunes by remember { mutableStateOf(false) }
    var waltTunes by remember { mutableStateOf(false) }
    var sflState by remember { mutableStateOf(false) }
    var jitState by remember { mutableStateOf(false) }
    var disableTrace by remember { mutableStateOf(false) }
    var disableLogd by remember { mutableStateOf(false) }
    var maliSched by remember { mutableStateOf(false) }
    var disableThermal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            schedTunes = PropertyUtils.get("persist.sys.azenithconf.schedtunes", "0") == "1"
            waltTunes = PropertyUtils.get("persist.sys.azenithconf.walttunes", "0") == "1"
            sflState = PropertyUtils.get("persist.sys.azenithconf.SFL", "0") == "1"
            jitState = PropertyUtils.get("persist.sys.azenithconf.justintime", "0") == "1"
            disableTrace = PropertyUtils.get("persist.sys.azenithconf.disabletrace", "0") == "1"
            disableLogd = PropertyUtils.get("persist.sys.azenithconf.logd", "0") == "1"
            maliSched = PropertyUtils.get("persist.sys.azenithconf.malisched", "0") == "1"
            disableThermal = PropertyUtils.get("persist.sys.azenithconf.DThermal", "0") == "1"

            RebootManager.captureBaselineOnce("pref_schedtunes", schedTunes)
            RebootManager.captureBaselineOnce("pref_walttunes", waltTunes)
            RebootManager.captureBaselineOnce("pref_SFL", sflState)
            RebootManager.captureBaselineOnce("pref_justintime", jitState)
            RebootManager.captureBaselineOnce("pref_disabletrace", disableTrace)
            RebootManager.captureBaselineOnce("pref_logd", disableLogd)
            RebootManager.captureBaselineOnce("pref_malisched", maliSched)
            RebootManager.captureBaselineOnce("pref_DThermal", disableThermal)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TweaksSubTabHeader(
            title = stringResource(R.string.prefs),
            subtitle = "Kernel energy models, latency mitigations, and runtime tunables",
            onBackClick = onBackClick
        )

        TweaksSectionHeader(stringResource(R.string.section_prefstweaks))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(blockLargeCorner)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsSwitchCard(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.sched_tunes),
                summary = stringResource(R.string.sched_tunes_desc),
                checked = schedTunes,
                onCheckedChange = { isChecked ->
                    schedTunes = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.schedtunes", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_schedtunes", isChecked)
                    if (isChecked && waltTunes) {
                        waltTunes = false
                        PropertyUtils.set("persist.sys.azenithconf.walttunes", "0")
                        RebootManager.checkAgainstBaseline("pref_walttunes", false)
                    }
                },
                shape = blockTopShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.Timeline,
                title = stringResource(R.string.walt_tunes),
                summary = if (isSnapdragon) stringResource(R.string.walt_tunes_desc) else stringResource(R.string.str_unavailable_snapdragon),
                checked = waltTunes,
                enabled = isSnapdragon,
                onCheckedChange = { isChecked ->
                    waltTunes = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.walttunes", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_walttunes", isChecked)
                    if (isChecked && schedTunes) {
                        schedTunes = false
                        PropertyUtils.set("persist.sys.azenithconf.schedtunes", "0")
                        RebootManager.checkAgainstBaseline("pref_schedtunes", false)
                    }
                },
                shape = blockMiddleShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.sfl_latency),
                summary = stringResource(R.string.sfl_latency_desc),
                checked = sflState,
                onCheckedChange = { isChecked ->
                    sflState = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.SFL", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_SFL", isChecked)
                },
                shape = blockMiddleShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.Bolt,
                title = stringResource(R.string.jit_compilation),
                summary = stringResource(R.string.jit_compilation_desc),
                checked = jitState,
                onCheckedChange = { isChecked ->
                    jitState = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.justintime", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_justintime", isChecked)
                },
                shape = blockMiddleShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.TrackChanges,
                title = stringResource(R.string.disable_trace),
                summary = stringResource(R.string.disable_trace_desc),
                checked = disableTrace,
                onCheckedChange = { isChecked ->
                    disableTrace = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.disabletrace", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_disabletrace", isChecked)
                },
                shape = blockMiddleShape
            )

            SettingsSwitchCard(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                title = stringResource(R.string.disable_logging),
                summary = stringResource(R.string.disable_logging_desc),
                checked = disableLogd,
                onCheckedChange = { isChecked ->
                    disableLogd = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.logd", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_logd", isChecked)
                },
                shape = blockMiddleShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.DeveloperBoard,
                title = stringResource(R.string.gpu_mali),
                summary = if (isMediaTek) stringResource(R.string.gpu_mali_desc) else stringResource(R.string.str_unavailable_mediatek),
                checked = maliSched,
                enabled = isMediaTek,
                onCheckedChange = { isChecked ->
                    maliSched = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.malisched", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_malisched", isChecked)
                },
                shape = blockMiddleShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.Thermostat,
                title = stringResource(R.string.disable_thermals),
                summary = if (isMediaTek) stringResource(R.string.disable_thermals_desc) else stringResource(R.string.str_unavailable_mediatek),
                checked = disableThermal,
                enabled = isMediaTek,
                onCheckedChange = { isChecked ->
                    disableThermal = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.DThermal", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_DThermal", isChecked)
                },
                shape = blockBottomShape
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Subpane 4: MediaTek FPSGO & GED
// ---------------------------------------------------------------------------
@Composable
private fun FpsGoSubPane(
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    var fpsgoState by remember { mutableStateOf(false) }
    var fpsgedState by remember { mutableStateOf(false) }
    var sflState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            fpsgoState = PropertyUtils.get("persist.sys.azenithconf.usefpsgo", "0") == "1"
            fpsgedState = PropertyUtils.get("persist.sys.azenithconf.fpsged", "0") == "1"
            sflState = PropertyUtils.get("persist.sys.azenithconf.SFL", "0") == "1"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TweaksSubTabHeader(
            title = stringResource(R.string.str_fpsgo_settings),
            subtitle = "MediaTek kernel frame rate governor and event driver",
            onBackClick = onBackClick
        )

        TweaksSectionHeader("Engine & Governor")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(blockLargeCorner)),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsSwitchCard(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.str_use_fpsgo_title),
                summary = stringResource(R.string.str_use_fpsgo_summary),
                checked = fpsgoState,
                onCheckedChange = { isChecked ->
                    fpsgoState = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.usefpsgo", if (isChecked) "1" else "0")
                },
                shape = blockTopShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.fpsgo_ged),
                summary = stringResource(R.string.fpsgo_ged_desc),
                checked = fpsgedState,
                onCheckedChange = { isChecked ->
                    fpsgedState = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.fpsged", if (isChecked) "1" else "0")
                },
                shape = blockMiddleShape
            )

            SettingsSwitchCard(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.sfl_latency),
                summary = stringResource(R.string.sfl_latency_desc),
                checked = sflState,
                onCheckedChange = { isChecked ->
                    sflState = isChecked
                    PropertyUtils.set("persist.sys.azenithconf.SFL", if (isChecked) "1" else "0")
                    RebootManager.checkAgainstBaseline("pref_SFL", isChecked)
                },
                shape = blockBottomShape
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Subpane 5: Display Color Calibration
// ---------------------------------------------------------------------------
@Composable
private fun DisplayColorSubPane(
    onBackClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    var selectedPreset by remember { mutableStateOf(ColorPreset.DEFAULT) }
    var satMultiplier by remember { mutableFloatStateOf(1000f) }

    LaunchedEffect(Unit) {
        val raw = withContext(Dispatchers.IO) {
            PropertyUtils.get("persist.sys.azenithconf.schemeconfig", "")
        }
        if (raw.isNotEmpty()) {
            val parts = raw.split(" ").mapNotNull { it.toFloatOrNull() }
            if (parts.size >= 4) {
                satMultiplier = parts[3].coerceIn(500f, 2000f)
            }
        }
        selectedPreset = ColorPreset.entries.firstOrNull { preset ->
            preset != ColorPreset.CUSTOM && (preset.s - satMultiplier) < 10f && (satMultiplier - preset.s) < 10f
        } ?: ColorPreset.CUSTOM
    }

    val applyPreset: (ColorPreset) -> Unit = { preset ->
        selectedPreset = preset
        if (preset != ColorPreset.CUSTOM) {
            satMultiplier = preset.s
            Shell.cmd("service call SurfaceFlinger 1022 f ${preset.s / 1000f}").submit()
            val config = "${preset.r.toInt()} ${preset.g.toInt()} ${preset.b.toInt()} ${preset.s.toInt()}"
            PropertyUtils.set("persist.sys.azenithconf.schemeconfig", config)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TweaksSubTabHeader(
            title = stringResource(R.string.color_scheme),
            subtitle = "Display surface saturation profiles and hardware KCAL calibration",
            onBackClick = onBackClick
        )

        TweaksSectionHeader("Saturation Presets")

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    ColorPreset.DEFAULT,
                    ColorPreset.VIVID,
                    ColorPreset.WARM,
                    ColorPreset.COOL,
                    ColorPreset.AMOLED
                ).forEach { preset ->
                    val isSelected = selectedPreset == preset
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) scheme.primaryContainer else Color.Transparent,
                        contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { applyPreset(preset) }
                            .gamepadFocusOutline(shape = RoundedCornerShape(14.dp), register = true)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(preset.labelRes),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        TweaksSectionHeader("Saturation Multiplier")

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Surface Saturation",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Direct SurfaceFlinger compositor color matrix boost",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.82f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = scheme.primaryContainer
                    ) {
                        Text(
                            text = String.format("%.2fx", satMultiplier / 1000f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                ZenithSlider(
                    value = satMultiplier,
                    onValueChange = {
                        satMultiplier = it
                        selectedPreset = ColorPreset.CUSTOM
                        Shell.cmd("service call SurfaceFlinger 1022 f ${it / 1000f}").submit()
                    },
                    onValueChangeFinished = {
                        val config = "1000 1000 1000 ${satMultiplier.toInt()}"
                        PropertyUtils.set("persist.sys.azenithconf.schemeconfig", config)
                    },
                    valueRange = 500f..2000f,
                    steps = 14
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared Components & Dialogs
// ---------------------------------------------------------------------------
@Composable
private fun TweaksSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun TweaksSubTabHeader(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val backShape = CircleShape

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = backShape,
            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
            contentColor = scheme.onSurface,
            modifier = Modifier
                .size(42.dp)
                .clip(backShape)
                .gamepadFocusOutline(shape = backShape, register = true)
                .clickable(onClick = onBackClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FreqOffsetSliderCard(
    initialIndex: Float,
    labels: List<String>,
    onSaved: (Float) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp)
) {
    val scheme = MaterialTheme.colorScheme
    var sliderValue by remember(initialIndex) { mutableFloatStateOf(initialIndex) }
    val currentIndex = sliderValue.roundToInt().coerceIn(0, (labels.size - 1).coerceAtLeast(0))
    val displayLabel = if (labels.isNotEmpty()) labels[currentIndex] else "Disabled"

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .gamepadFocusOutline(shape = shape, register = true)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.secondaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = scheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.freq_offset),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "CPU frequency throttling headroom offset",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.82f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentIndex == 0) scheme.surfaceContainerHighest else scheme.primaryContainer
                ) {
                    Text(
                        text = if (currentIndex == 0) stringResource(R.string.disabled) else displayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (currentIndex == 0) scheme.onSurfaceVariant else scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            ZenithSlider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onSaved(sliderValue) },
                valueRange = 0f..(labels.size - 1).coerceAtLeast(1).toFloat(),
                steps = (labels.size - 2).coerceAtLeast(0)
            )
        }
    }
}

@Composable
private fun BypassThresholdSliderCard(
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    onThresholdChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp)
) {
    val scheme = MaterialTheme.colorScheme
    var sliderValue by remember(threshold) { mutableFloatStateOf(threshold) }

    Surface(
        shape = shape,
        color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        contentColor = scheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .gamepadFocusOutline(shape = shape, register = true)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                                imageVector = Icons.Rounded.BatteryChargingFull,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Bypass Cutoff Threshold",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Target battery capacity to engage bypass charging",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.82f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scheme.primaryContainer
                ) {
                    Text(
                        text = "${sliderValue.roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            ZenithSlider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    onThresholdChange(it)
                },
                onValueChangeFinished = { onThresholdChangeFinished(sliderValue) },
                valueRange = 20f..100f,
                steps = 15
            )
        }
    }
}

@Composable
private fun ChoiceSelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEachIndexed { index, option ->
                        val isSelected = index == selectedIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) scheme.primaryContainer else Color.Transparent,
                            contentColor = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelect(index)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
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
}
