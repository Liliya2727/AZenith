/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.mainscreens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import android.content.pm.PackageManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.content.ComponentName
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import zx.azenith.BuildConfig
import zx.azenith.R
import zx.azenith.ui.component.*
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.LargeFlexibleTopAppBar
import kotlinx.coroutines.launch

fun isLauncherIconEnabled(context: Context): Boolean {
    val pkg = context.packageManager
    val componentName = ComponentName(context.packageName, "${context.packageName}.Launcher")
    val state = pkg.getComponentEnabledSetting(componentName)
    return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
}

@Composable
fun SettingsScreen(navController: NavController) {
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val aboutDialog = rememberCustomDialog { AboutDialog(it) }
    
    val restartToastText = stringResource(R.string.toast_restarting_service)
    val logSavedToastText = stringResource(R.string.toast_log_saved)
    
    var isLauncherVisible by rememberSaveable { 
        mutableStateOf(isLauncherIconEnabled(context)) 
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val uninstallDialog = rememberConfirmDialog(
        onConfirm = {
            Shell.cmd("sh /data/adb/modules/AZenith/uninstall.sh").submit()
        },
        onDismiss = {}
    )
    MaterialExpressiveTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { SettingsScreenTopAppBar(scrollBehavior) },
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(
                        bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) 
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 110.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                
                // 👇 Gabung Header dan Theme dalam satu ExpressiveList
                item {
                    Spacer(modifier = Modifier.height(16.dp)) // Jarak dari TopAppBar
                    
                    ExpressiveList(
                        content = listOf(
                            {
                                // Item 1 (Paling Atas, akan dapat topShape otomatis)
                                AppInfoHeaderContent()
                            },
                            {
                                // Item 2 (Paling Bawah, akan dapat bottomShape otomatis)
                                ExpressiveListItem(
                                    onClick = { navController.navigate("color_palette") },
                                    headlineContent = { Text(stringResource(R.string.theme)) },
                                    supportingContent = { Text(stringResource(R.string.theme_desc)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.Palette) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    )
                }
    
                item { SettingsSectionTitle(stringResource(R.string.section_features)) }
                
                item {
                    var stateToast by remember { mutableStateOf<Boolean?>(null) }
                    var autoMode by remember { mutableStateOf<Boolean?>(null) }
                    var debugMode by remember { mutableStateOf<Boolean?>(null) }
    
                    LaunchedEffect(Unit) {
                        stateToast = Shell.cmd("getprop persist.sys.azenithconf.showtoast").exec().out.firstOrNull()?.trim() == "1"
                        autoMode = Shell.cmd("getprop persist.sys.azenithconf.AIenabled").exec().out.firstOrNull()?.trim() == "0"
                        debugMode = Shell.cmd("getprop persist.sys.azenith.debugmode").exec().out.firstOrNull()?.trim() == "true"
                    }
    
                    if (stateToast != null && autoMode != null && debugMode != null) {
                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Filled.Notifications,
                                        title = stringResource(R.string.show_toast),
                                        checked = stateToast!!,
                                        onCheckedChange = { isChecked ->
                                            stateToast = isChecked
                                            Shell.cmd("setprop persist.sys.azenithconf.showtoast ${if (isChecked) "1" else "0"}").submit()
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Filled.Assistant,
                                        title = stringResource(R.string.disable_auto_mode),
                                        checked = autoMode!!,
                                        onCheckedChange = { isChecked ->
                                            autoMode = isChecked
                                            val state = if (isChecked) "0" else "1"
                                            
                                            Shell.cmd(
                                                "setprop persist.sys.azenithconf.AIenabled $state",
                                                "echo $state > /data/adb/.config/AZenith/API/current_modes"
                                            ).submit()
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Filled.BugReport,
                                        title = stringResource(R.string.allow_verbose_log),
                                        checked = debugMode!!,
                                        onCheckedChange = { isChecked ->
                                            debugMode = isChecked
                                            Shell.cmd("setprop persist.sys.azenith.debugmode ${if (isChecked) "true" else "false"}").submit()
                                        }
                                    )
                                }
                            )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
    
                item { SettingsSectionTitle(stringResource(R.string.section_others)) }
                item {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveSwitchItem(
                                    icon = Icons.Rounded.AddHome,
                                    title = stringResource(R.string.show_icon),
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
                            },
                            {
                                ExpressiveListItem(
                                    onClick = {
                                        Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf restartservice").submit { result ->
                                            if (result.isSuccess) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(restartToastText)
                                                }
                                            }
                                        }
                                    },
                                    headlineContent = { Text(stringResource(R.string.restart_service)) },
                                    supportingContent = { Text(stringResource(R.string.restart_service_desc)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.RestartAlt) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    onClick = {
                                        Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf saveLog").submit { result ->
                                            if (result.isSuccess) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(logSavedToastText)
                                                }
                                            }
                                        }
                                    },
                                    headlineContent = { Text(stringResource(R.string.save_log)) },
                                    supportingContent = { Text(stringResource(R.string.save_log_desc)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.Save) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            },
                            {
                                ExpressiveListItem(
                                    onClick = {
                                        uninstallDialog.showConfirm(
                                            title = context.getString(R.string.uninstall),
                                            content = context.getString(R.string.uninstall_confirm_content),
                                            confirm = context.getString(R.string.yes),
                                            dismiss = context.getString(R.string.no)
                                        )
                                    },
                                    headlineContent = { Text(stringResource(R.string.uninstall)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Filled.Delete) },
                                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                )
                            }
                        )
                    )
                }
    
                item { SettingsSectionTitle(stringResource(R.string.section_about)) }
                item {
                    ExpressiveList(
                        content = listOf {
                            ExpressiveListItem(
                                onClick = { aboutDialog.show() },
                                headlineContent = { Text(stringResource(R.string.about_azenith)) },
                                supportingContent = {
                                    Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
                                },
                                leadingContent = { LeadingIcon(icon = Icons.Filled.ContactPage) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
fun SettingsScreenTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
    val colorScheme = MaterialTheme.colorScheme

    val smoothGradient = Brush.verticalGradient(
        0.0f to colorScheme.surface,
        0.4f to colorScheme.surface.copy(alpha = 0.9f),
        0.5f to colorScheme.surface.copy(alpha = 0.8f),
        0.6f to colorScheme.surface.copy(alpha = 0.7f),
        0.7f to colorScheme.surface.copy(alpha = 0.5f),
        0.8f to colorScheme.surface.copy(alpha = 0.4f),
        0.9f to colorScheme.surface.copy(alpha = 0.3f),
        1.0f to Color.Transparent 
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(smoothGradient)
            .statusBarsPadding()
    ) {
        LargeFlexibleTopAppBar(
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.settings),
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
