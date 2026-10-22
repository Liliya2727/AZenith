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

import android.content.ComponentName
import android.content.pm.PackageManager
import zx.azenith.ui.mainscreens.isLauncherIconEnabled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.gamespace.AdaptiveSettingsGrid
import zx.azenith.ui.gamespace.SettingsActionCard
import zx.azenith.ui.gamespace.SettingsSwitchCard
import zx.azenith.ui.util.dumpDiagnosticLogs
import zx.azenith.ui.viewmodel.SettingsViewModel

@Composable
internal fun ZXGameSpaceSettingsGeneral(
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    SettingsGeneralPane(
        settingsViewModel = settingsViewModel,
        modifier = modifier
    )
}

@Composable
internal fun SettingsGeneralPane(
    settingsViewModel: SettingsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var statusFeedback by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var isLauncherVisible by remember { mutableStateOf(isLauncherIconEnabled(context)) }
    val scheme = MaterialTheme.colorScheme

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

        val cards = listOf<@Composable () -> Unit>(
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.show_notifications),
                    summary = "Post status notifications when active profile switches",
                    checked = uiState.profileNotifications,
                    onCheckedChange = { settingsViewModel.setProfileNotifications(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.disable_auto_mode),
                    summary = "Lock daemon in active mode instead of pausing during screen-off",
                    checked = uiState.autoMode,
                    onCheckedChange = { settingsViewModel.setAutoMode(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Vibration,
                    title = stringResource(R.string.tap_haptic_feedback),
                    summary = "Tactile impulse feedback on interactive taps and toggles",
                    checked = uiState.tapHaptic,
                    onCheckedChange = { settingsViewModel.onTapHapticToggle(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.disable_tweak),
                    summary = stringResource(R.string.disable_tweak_desc),
                    checked = uiState.disableTweak,
                    onCheckedChange = { settingsViewModel.setDisableTweak(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.show_toast),
                    summary = "Display brief toast notifications on profile and state switches",
                    checked = uiState.stateToast,
                    onCheckedChange = { settingsViewModel.setShowToast(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.HourglassEmpty,
                    title = stringResource(R.string.profile_timeout),
                    summary = stringResource(R.string.profile_timeout_desc),
                    checked = uiState.profileTimeout,
                    onCheckedChange = { settingsViewModel.setProfileTimeout(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.allow_verbose_log),
                    summary = "Write extended debug diagnostics to /data/adb/.config/AZenith",
                    checked = uiState.debugMode,
                    onCheckedChange = { settingsViewModel.setDebugMode(it) }
                )
            },
            {
                SettingsSwitchCard(
                    icon = Icons.Rounded.AddHome,
                    title = stringResource(R.string.show_icon),
                    summary = "Show the AZenith launcher icon on the home screen",
                    checked = isLauncherVisible,
                    onCheckedChange = { isChecked ->
                        isLauncherVisible = isChecked
                        val pkg = context.packageManager
                        val componentName = ComponentName(context.packageName, "${context.packageName}.Launcher")
                        val newState = if (isChecked) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        }
                        pkg.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
                    }
                )
            }
        )

        AdaptiveSettingsGrid(
            items = cards,
            minColumnWidth = 260.dp,
            spacing = 10.dp
        )

        Text(
            text = stringResource(R.string.section_diagnostics).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            ),
            color = scheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        val diagnosticCards = listOf<@Composable () -> Unit>(
            {
                SettingsActionCard(
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
            },
            {
                SettingsActionCard(
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
        )

        AdaptiveSettingsGrid(
            items = diagnosticCards,
            minColumnWidth = 260.dp,
            spacing = 10.dp
        )
    }
}
