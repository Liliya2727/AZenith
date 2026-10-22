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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.ThermostatAuto
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.gamespace.AdaptiveSettingsGrid
import zx.azenith.ui.gamespace.SettingsActionCard
import zx.azenith.ui.gamespace.SettingsSwitchCard
import zx.azenith.ui.gamespace.springPressBounce
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.viewmodel.AutoModeBus
import zx.azenith.ui.viewmodel.TweakViewModel

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
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (!tweakViewModel.isUiLoaded) {
            tweakViewModel.loadAllConfiguration(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp),
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
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                color = if (isTarget) scheme.onPrimaryContainer else scheme.primary,
                                                modifier = Modifier.size(18.dp)
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
                        Text(
                            text = "Applying ${when (pendingProfileValue) { "1" -> "Performance"; "3" -> "ECO"; else -> "Balanced" }} profile...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.primary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }
                }
            }
        }

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
                    icon = Icons.Rounded.DoNotDisturbOn,
                    title = stringResource(R.string.dnd_mode_gaming),
                    summary = stringResource(R.string.dnd_mode_gaming_desc),
                    checked = tweakViewModel.dndState ?: false,
                    onCheckedChange = { tweakViewModel.updateDndMode(it) }
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
            },
            {
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
        )

        AdaptiveSettingsGrid(
            items = cards,
            minColumnWidth = 340.dp,
            spacing = 10.dp
        )
    }
}
