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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapVerticalCircle
import androidx.compose.material.icons.rounded.ThermostatAuto
import androidx.compose.material.icons.rounded.WebStories
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import zx.azenith.R
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.gamespace.AdaptiveSettingsGrid
import zx.azenith.ui.gamespace.SettingsActionCard
import zx.azenith.ui.gamespace.SettingsSwitchCard
import zx.azenith.ui.gamespace.springPressBounce
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.getSupportedRefreshRatesPicker
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

        val profiles = listOf(
            Triple("1", R.string.Profile_Performance, R.drawable.ic_performance),
            Triple("2", R.string.Profile_Balanced, R.drawable.ic_balanced),
            Triple("3", R.string.Profile_ECO_mode, R.drawable.ic_eco)
        )
        val activeValue = if (profileValue.isBlank()) "2" else profileValue

        var positions by remember { mutableStateOf(mapOf<String, Pair<Float, Float>>()) }
        val currentPillPos = positions[activeValue] ?: (0f to 0f)

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
        Surface(
            shape = containerShape,
            color = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                if (animatedPillWidth > 0f) {
                    val pillShape = RoundedCornerShape(18.dp)
                    Surface(
                        shape = pillShape,
                        color = scheme.primaryContainer,
                        modifier = Modifier
                            .offset { IntOffset(animatedPillX.roundToInt(), 0) }
                            .width(with(density) { animatedPillWidth.toDp() })
                            .height(52.dp)
                    ) {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    profiles.forEach { (value, labelRes, iconRes) ->
                        val active = activeValue == value
                        val pillItemShape = RoundedCornerShape(18.dp)
                        val iconScale by animateFloatAsState(
                            targetValue = if (active) 1.08f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "ProfileIconScale"
                        )

                        Surface(
                            shape = pillItemShape,
                            color = Color.Transparent,
                            contentColor = if (active) scheme.onPrimaryContainer else scheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coordinates ->
                                    val parentX = coordinates.positionInParent().x
                                    val width = coordinates.size.width.toFloat()
                                    if (positions[value]?.first != parentX || positions[value]?.second != width) {
                                        positions = positions + (value to (parentX to width))
                                    }
                                }
                                .gamepadFocusOutline(shape = pillItemShape, register = true)
                                .clip(pillItemShape)
                                .springPressBounce(pressedScale = 0.95f)
                                .clickable {
                                    AudioUtil.playOk(context)
                                    onApplyProfile(value)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = if (active) scheme.onPrimaryContainer else scheme.primary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                Text(
                                    text = stringResource(labelRes),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }
                        }
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
            minColumnWidth = 260.dp,
            spacing = 10.dp
        )
    }
}
