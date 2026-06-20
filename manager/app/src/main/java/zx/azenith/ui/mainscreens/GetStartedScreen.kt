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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlin.system.exitProcess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.ExpressiveList
import zx.azenith.ui.component.ExpressiveSwitchItem
import zx.azenith.ui.util.RootUtils


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
            Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 24.dp)) {
                AnimatedVisibility(
                    visible = !isFinalizing,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(300)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(totalPages) { index ->
                                val isSelected = index == currentPage
                                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "indicatorWidth")
                                
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .height(8.dp)
                                        .width(width)
                                        .background(color, CircleShape)
                                )
                            }
                        }

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
                                    Text(stringResource(R.string.cd_back), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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

                                            Shell.cmd("su -c pm grant zx.azenith android.permission.READ_EXTERNAL_STORAGE && su -c pm grant zx.azenith android.permission.POST_NOTIFICATIONS && su -c pm grant zx.azenith android.permission.READ_MEDIA_IMAGES && su -c am start -S -n zx.azenith/.MainActivity").exec()
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
                                    text = if (currentPage == totalPages - 1) stringResource(R.string.lets_go) else stringResource(R.string.next),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
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
        ) {
            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(120.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(28.dp).fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.str_welcome_to),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.displayLarge, 
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        1 -> {
                            Text(
                                text = stringResource(R.string.str_let_s_grant_azenith_a_root_acc),
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
                                        stringResource(R.string.str_check_environment),
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                                text = if (rootAccessGranted == true) stringResource(R.string.str_root_access_granted) else stringResource(R.string.str_root_access_denied),
                                                color = if (rootAccessGranted == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    
                                    // --- 4. TEKS PERINGATAN BILA ROOT GAGAL ---
                                    if (rootAccessGranted == false) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Akses Root diperlukan untuk melanjutkan.", // Pertimbangkan untuk dipindah ke strings.xml
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = stringResource(R.string.str_let_s_adjust_a_few_basic_setti),
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
                            // --- 5. PENAMBAHAN IKON SELESAI PADA HALAMAN TERAKHIR ---
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = stringResource(R.string.str_you_re_all_set),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.str_azenith_is_now_ready_to_go),
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
                            stringResource(R.string.setting_up),
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
