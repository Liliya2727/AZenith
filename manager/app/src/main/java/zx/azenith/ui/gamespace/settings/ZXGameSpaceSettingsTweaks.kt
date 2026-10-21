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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import zx.azenith.R
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.gamespace.SettingsActionCard
import zx.azenith.ui.gamespace.SettingsSwitchCard
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
