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
import android.view.KeyEvent
import zx.azenith.ui.mainscreens.isLauncherIconEnabled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.LocalGamepadFocusEnabled
import zx.azenith.ui.component.gamepadFocusOutline
import zx.azenith.ui.component.gamepadKeyWatch
import zx.azenith.ui.gamespace.AdaptiveSettingsGrid
import zx.azenith.ui.gamespace.BindGameSpaceDialogGamepad
import zx.azenith.ui.gamespace.GameSpaceInputSource
import zx.azenith.ui.gamespace.GameSpaceOverlayState
import zx.azenith.ui.gamespace.SettingsActionCard
import zx.azenith.ui.gamespace.SettingsSwitchCard
import zx.azenith.ui.util.RebootManager
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
    var isSavingLog by remember { mutableStateOf(false) }
    var saveLogBadge by remember { mutableStateOf<String?>(null) }
    var saveLogSummary by remember { mutableStateOf<String?>(null) }
    var pendingDisableTweakValue by remember { mutableStateOf<Boolean?>(null) }
    val scrollState = rememberScrollState()
    zx.azenith.ui.component.RegisterGamepadActions(columnScrollState = scrollState)
    var isLauncherVisible by remember { mutableStateOf(isLauncherIconEnabled(context)) }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded) {
            RebootManager.captureBaselineOnce("disable_tweak", uiState.disableTweak)
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
                    onCheckedChange = { isChecked ->
                        if (RebootManager.wouldRequireReboot("disable_tweak", isChecked)) {
                            pendingDisableTweakValue = isChecked
                        } else {
                            settingsViewModel.setDisableTweak(isChecked)
                        }
                    }
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
            minColumnWidth = 340.dp,
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
                    summary = saveLogSummary ?: stringResource(R.string.save_log_desc),
                    trailingBadge = when {
                        isSavingLog -> "Saving..."
                        else -> saveLogBadge
                    },
                    enabled = !isSavingLog,
                    onClick = {
                        if (isSavingLog) return@SettingsActionCard
                        isSavingLog = true
                        saveLogBadge = "Saving..."
                        coroutineScope.launch {
                            val file = dumpDiagnosticLogs(context, saveToDownloads = true)
                            isSavingLog = false
                            if (file != null) {
                                val msg = context.getString(R.string.toast_log_saved)
                                statusFeedback = msg
                                saveLogBadge = "Saved"
                                saveLogSummary = "$msg (${file.name})"
                            } else {
                                val err = context.getString(R.string.toast_log_save_fail)
                                statusFeedback = err
                                saveLogBadge = "Failed"
                                saveLogSummary = err
                            }
                        }
                    }
                )
            }
        )

        AdaptiveSettingsGrid(
            items = diagnosticCards,
            minColumnWidth = 340.dp,
            spacing = 10.dp
        )
    }

    pendingDisableTweakValue?.let { targetChecked ->
        Dialog(onDismissRequest = { pendingDisableTweakValue = null }) {
            BindGameSpaceDialogGamepad()
            val dialogToken = remember { Any() }
            val confirmRequester = remember { FocusRequester() }
            DisposableEffect(Unit) {
                GameSpaceOverlayState.acquire(dialogToken, "DisableTweakRebootDialog")
                onDispose {
                    GameSpaceOverlayState.release(dialogToken)
                }
            }
            LaunchedEffect(Unit) {
                if (GameSpaceInputSource.isGamepadLast) {
                    for (attempt in 1..12) {
                        withFrameNanos { }
                        if (runCatching { confirmRequester.requestFocus() }.getOrDefault(false)) break
                    }
                }
            }
            val confirmAction = {
                settingsViewModel.setDisableTweak(targetChecked)
                pendingDisableTweakValue = null
            }
            CompositionLocalProvider(LocalGamepadFocusEnabled provides true) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = scheme.surfaceContainerHigh,
                    contentColor = scheme.onSurface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(24.dp))
                        .gamepadKeyWatch()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_B ||
                                 event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ESCAPE)
                            ) {
                                pendingDisableTweakValue = null
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.dialog_reboot_required_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.reboot_required_content),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { pendingDisableTweakValue = null },
                                colors = ButtonDefaults.textButtonColors(),
                                modifier = Modifier.gamepadFocusOutline(
                                    shape = CircleShape,
                                    onActivate = { pendingDisableTweakValue = null }
                                )
                            ) {
                                Text(stringResource(R.string.no), color = scheme.outline)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = confirmAction,
                                modifier = Modifier
                                    .focusRequester(confirmRequester)
                                    .gamepadFocusOutline(
                                        shape = CircleShape,
                                        onActivate = confirmAction
                                    )
                            ) {
                                Text(stringResource(R.string.yes))
                            }
                        }
                    }
                }
            }
        }
    }
}
