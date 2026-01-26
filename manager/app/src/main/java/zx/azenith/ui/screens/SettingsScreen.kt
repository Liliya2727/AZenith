package zx.azenith.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val aboutDialog = rememberCustomDialog { AboutDialog(it) }
    val restartToastText = stringResource(R.string.toast_restarting_service)
    val logSavedToastText = stringResource(R.string.toast_log_saved)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { SettingsScreenTopAppBar(scrollBehavior) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            item { SettingsSectionTitle(stringResource(R.string.section_personalization)) }
            item {
                ExpressiveList(
                    content = listOf {
                        ExpressiveListItem(
                            onClick = { navController.navigate("color_palette") },
                            headlineContent = { Text(stringResource(R.string.theme)) },
                            supportingContent = { Text(stringResource(R.string.theme_desc)) },
                            leadingContent = { Icon(Icons.Filled.Palette, null) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                        )
                    }
                )
            }

           item { SettingsSectionTitle(stringResource(R.string.section_features)) }
            item {
                
                var stateToast by remember { mutableStateOf(false) }
                var autoMode by remember { mutableStateOf(false) }
                var debugMode by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    stateToast = Shell.cmd("getprop persist.sys.azenithconf.showtoast").exec().out.firstOrNull()?.trim() == "1"
                    autoMode = Shell.cmd("getprop persist.sys.azenithconf.AIenabled").exec().out.firstOrNull()?.trim() == "0"
                    debugMode = Shell.cmd("getprop persist.sys.azenith.debugmode").exec().out.firstOrNull()?.trim() == "true"
                }

                ExpressiveList(
                    content = listOf(
                        {
                            ExpressiveSwitchItem(
                                icon = Icons.Filled.Notifications,
                                title = stringResource(R.string.show_toast),
                                checked = stateToast,
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
                                checked = autoMode,
                                onCheckedChange = { isChecked ->
                                    autoMode = isChecked
                                    Shell.cmd("setprop persist.sys.azenithconf.AIenabled ${if (isChecked) "0" else "1"}").submit()
                                }
                            )
                        },
                        {
                            ExpressiveSwitchItem(
                                icon = Icons.Filled.BugReport,
                                title = stringResource(R.string.allow_verbose_log),
                                checked = debugMode,
                                onCheckedChange = { isChecked ->
                                    debugMode = isChecked
                                    Shell.cmd("setprop persist.sys.azenith.debugmode ${if (isChecked) "true" else "false"}").submit()
                                }
                            )
                        }
                    )
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.section_others)) }
            item {
                ExpressiveList(
                    content = listOf(
                        {
                            ExpressiveListItem(
                                onClick = {
                                    Shell.cmd(                               
                                        "/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf restartservice"
                                    ).submit { result ->
                                        if (result.isSuccess) Toast.makeText(
                                            context,
                                            restartToastText,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                headlineContent = { Text(stringResource(R.string.restart_service)) },
                                supportingContent = { Text(stringResource(R.string.restart_service_desc)) },
                                leadingContent = { Icon(Icons.Filled.RestartAlt, null) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                            )
                        },
                        {
                            ExpressiveListItem(
                                onClick = {
                                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf saveLog").submit { result ->
                                        if (result.isSuccess) Toast.makeText(
                                            context,
                                            logSavedToastText,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                headlineContent = { Text(stringResource(R.string.save_log)) },
                                supportingContent = { Text(stringResource(R.string.save_log_desc)) },
                                leadingContent = { Icon(Icons.Filled.Save, null) },
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
                            leadingContent = { Icon(Icons.Filled.ContactPage, null) }
                        )
                    }
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
    )
}
