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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveSwitchItem
import zx.azenith.ui.util.RootUtils
import kotlin.system.exitProcess
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner


@Composable
fun GetStartedScreen(navController: NavController) {
    var currentPage by remember { mutableIntStateOf(0) }
    var rootAccessGranted by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingRoot by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var isLauncherVisible by remember { mutableStateOf(isLauncherIconEnabled(context)) }
    var stateToast by remember { mutableStateOf(true) }
    var autoMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val toastOut = Shell.cmd("getprop persist.sys.azenithconf.showtoast").exec().out.firstOrNull()?.trim()
        if (toastOut == "0") stateToast = false

        val aiOut = Shell.cmd("getprop persist.sys.azenithconf.AIenabled").exec().out.firstOrNull()?.trim()
        if (aiOut == "0") autoMode = true
    }

    var isFinalizing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val totalPages = 4

    val canGoNext = when (currentPage) {
        1 -> rootAccessGranted == true
        else -> true
    }

    LaunchedEffect(currentPage) {
        if (currentPage == 1 && rootAccessGranted == null) {
            isCheckingRoot = true
            delay(500)
            rootAccessGranted = RootUtils.requestRootAccess()
            isCheckingRoot = false
        }
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, currentPage) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && currentPage == 1 && rootAccessGranted != true) {
                isCheckingRoot = true
                coroutineScope.launch {
                    delay(300)
                    rootAccessGranted = RootUtils.requestRootAccess()
                    isCheckingRoot = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isFinalizing) 0f else 1f,
        animationSpec = tween(300),
        label = "contentAlpha"
    )

    Scaffold(
        bottomBar = {
            // Kita bungkus dengan Box berukuran statis agar tinggi BottomBar tidak mendadak 0
            Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 24.dp)) {
                AnimatedVisibility(
                    visible = !isFinalizing,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(300)), // Dipercepat agar sinkron
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (currentPage > 0) {
                            Button(
                                onClick = { if (currentPage > 0) currentPage-- },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Text("Back", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                if (!canGoNext) return@Button
                                
                                if (currentPage < totalPages - 1) {
                                    currentPage++
                                } else {
                                    isFinalizing = true
                                    coroutineScope.launch {
                                        RootUtils.isModuleInstalled()
                                        delay(2000)

                                        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                        prefs.edit().putBoolean("has_completed_get_started", true).commit()

                                        Shell.cmd("su -c am start -S -n zx.azenith/.MainActivity").exec()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canGoNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (canGoNext) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text(
                                text = if (currentPage == totalPages - 1) "Let's Go" else "Next",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                // Hapus padding innerPadding agar animasi loading bisa benar-benar di tengah absolut layar
        ) {
            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // Padding dipindah ke dalam konten saja
                    .padding(bottom = 20.dp)
                    .alpha(contentAlpha),
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) + fadeOut()
                    } else {
                        slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "GetStartedSlideAnimation"
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Welcome to",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "AZenith",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 56.sp 
                                )
                            }
                        }
                        1 -> {
                            Text(
                                text = "Let's grant AZenith a root access.",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(40.dp))

                            Button(
                                onClick = {
                                    isCheckingRoot = true
                                    coroutineScope.launch {
                                        delay(500)
                                        rootAccessGranted = RootUtils.requestRootAccess()
                                        isCheckingRoot = false
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            ) {
                                if (isCheckingRoot) {
                                    LoadingIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        "Check Environment",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            AnimatedVisibility(
                                visible = rootAccessGranted != null,
                                enter = fadeIn() + expandVertically()
                            ) {
                                Surface(
                                    color = if (rootAccessGranted == true)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (rootAccessGranted == true) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                            contentDescription = null,
                                            tint = if (rootAccessGranted == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = if (rootAccessGranted == true) "Root access granted! You're good to go." else "Root access denied. AZenith needs root to work.",
                                            color = if (rootAccessGranted == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = "Let's adjust a few basic settings.",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(32.dp))

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
                                                val newState = if (isChecked) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                                pkg.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
                                            }
                                        )
                                    },
                                    {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Filled.Notifications,
                                            title = stringResource(R.string.show_toast),
                                            checked = stateToast,
                                            onCheckedChange = { isChecked ->
                                                stateToast = isChecked
                                                Shell.cmd("su -c setprop persist.sys.azenithconf.showtoast ${if (isChecked) "1" else "0"}").submit()
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
                                                val state = if (isChecked) "0" else "1"
                                                Shell.cmd(
                                                    "su -c setprop persist.sys.azenithconf.AIenabled $state",
                                                    "su -c \"echo $state > /data/adb/.config/AZenith/API/current_modes\""
                                                ).submit()
                                            }
                                        )
                                    }
                                )
                            )
                        }
                        3 -> {
                            Text(
                                text = "You're all set!",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AZenith is now ready to go!.",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isFinalizing,
                enter = fadeIn(tween(500, delayMillis = 200)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(),
                // Memastikan posisinya absolut di tengah layar (mengabaikan Scaffold padding)
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LoadingIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Setting up AZenith...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
