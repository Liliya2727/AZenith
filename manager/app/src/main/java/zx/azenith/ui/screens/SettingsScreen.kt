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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val aboutDialog = rememberCustomDialog { AboutDialog(it) }

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
            // --- SECTION: PERSONALIZATION ---
            item { SettingsSectionTitle("Personalization") }
            item {
                ExpressiveList(
                    content = listOf {
                        ExpressiveListItem(
                            onClick = { navController.navigate("color_palette") },
                            headlineContent = { Text("Theme") },
                            supportingContent = { Text("Customize colors and accent") },
                            leadingContent = { Icon(Icons.Filled.Palette, null) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                        )
                    }
                )
            }

            // --- SECTION: FEATURES ---
            item { SettingsSectionTitle("Features") }
            item {
                // State nullable untuk mencegah flicker saat load
                var stateToast by remember { mutableStateOf<Boolean?>(null) }
                var autoMode by remember { mutableStateOf<Boolean?>(null) }
                var debugMode by remember { mutableStateOf<Boolean?>(null) }

                // Load state dari system properties
                LaunchedEffect(Unit) {
                    stateToast = Shell.cmd("getprop persist.sys.azenithconf.showtoast").exec().out.firstOrNull()?.trim() == "1"
                    autoMode = Shell.cmd("getprop persist.sys.azenithconf.AIenabled").exec().out.firstOrNull()?.trim() == "0"
                    debugMode = Shell.cmd("getprop persist.sys.azenith.debugmode").exec().out.firstOrNull()?.trim() == "true"
                }

                // Hanya tampilkan jika semua state sudah loaded
                if (stateToast != null && autoMode != null && debugMode != null) {
                    ExpressiveList(
                        content = listOf(
                            {
                                ExpressiveSwitchItem(
                                    icon = Icons.Filled.Notifications,
                                    title = "Show toast notification",
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
                                    title = "Disable auto mode",
                                    checked = autoMode!!,
                                    onCheckedChange = { isChecked ->
                                        autoMode = isChecked
                                        Shell.cmd("setprop persist.sys.azenithconf.AIenabled ${if (isChecked) "0" else "1"}").submit()
                                    }
                                )
                            },
                            {
                                ExpressiveSwitchItem(
                                    icon = Icons.Filled.BugReport,
                                    title = "Allow Daemon to Verbose log",
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
                    // Loading state - tampilkan placeholder atau skeleton
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

            // --- SECTION: OTHERS ---
            item { SettingsSectionTitle("Others") }
            item {
                ExpressiveList(
                    content = listOf(
                        {
                            ExpressiveListItem(
                                onClick = {
                                    Shell.cmd(                               
                                        "/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf restartservice"
                                    ).submit { result ->
                                        if (result.isSuccess) Toast.makeText(context, "Restarting Service", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                headlineContent = { Text("Restart Service") },
                                supportingContent = { Text("Re-initialize Daemon processes") },
                                leadingContent = { Icon(Icons.Filled.RestartAlt, null) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                            )
                        },
                        {
                            ExpressiveListItem(
                                onClick = {
                                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf saveLog").submit { result ->
                                        if (result.isSuccess) Toast.makeText(context, "Log saved to /sdcard/AZenith.log", Toast.LENGTH_LONG).show()
                                    }
                                },
                                headlineContent = { Text("Save Log") },
                                supportingContent = { Text("Export debug logs to /sdcard") },
                                leadingContent = { Icon(Icons.Filled.Save, null) },
                                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                            )
                        }
                    )
                )
            }

            // --- SECTION: ABOUT ---
            item { SettingsSectionTitle("About") }
            item {
                ExpressiveList(
                    content = listOf {
                        ExpressiveListItem(
                            onClick = { aboutDialog.show() },
                            headlineContent = { Text("About AZenith") },
                            supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") },
                            leadingContent = { Icon(Icons.Filled.ContactPage, null) }
                        )
                    }
                )
            }
        }
    }
}

// Mengubah nama agar tidak konflik dengan file lain
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
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
    )
}
