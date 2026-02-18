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
 
 
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.screens

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import zx.azenith.R
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.rounded.*
import zx.azenith.ui.component.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.topjohnwu.superuser.Shell
import androidx.navigation.NavController
import android.content.Context
import android.view.WindowManager
import zx.azenith.ui.util.PropertyUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast

@Composable
fun PreferenceTweakScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    MaterialExpressiveTheme {        
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { PreferenceTweakTopAppBar(
                scrollBehavior,
                onBack = { navController.popBackStack() }
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
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            ) {
                item {
                    var socType by remember { mutableStateOf<String?>(null) }
                    var schedTunes by remember { mutableStateOf<Boolean?>(null) }
                    var sflstate by remember { mutableStateOf<Boolean?>(null) }
                    var jitstate by remember { mutableStateOf<Boolean?>(null) }
                    var fpsgogedstate by remember { mutableStateOf<Boolean?>(null) }
                    var malischedstate by remember { mutableStateOf<Boolean?>(null) }
                    var waltTunes by remember { mutableStateOf<Boolean?>(null) }
                    var DTraces by remember { mutableStateOf<Boolean?>(null) }
                    var dlogcat by remember { mutableStateOf<Boolean?>(null) }
                    var distherm by remember { mutableStateOf<Boolean?>(null) }
    
                    LaunchedEffect(Unit) {
                        socType = PropertyUtils.get("persist.sys.azenithdebug.soctype")
                        schedTunes = PropertyUtils.get("persist.sys.azenithconf.schedtunes") == "1"
                        sflstate = PropertyUtils.get("persist.sys.azenithconf.SFL") == "1"
                        jitstate = PropertyUtils.get("persist.sys.azenithconf.justintime") == "1"
                        fpsgogedstate = PropertyUtils.get("persist.sys.azenithconf.fpsged") == "1"
                        malischedstate = PropertyUtils.get("persist.sys.azenithconf.malisched") == "1"
                        waltTunes = PropertyUtils.get("persist.sys.azenithconf.walttunes") == "1"
                        DTraces = PropertyUtils.get("persist.sys.azenithconf.disabletrace") == "1"
                        dlogcat = PropertyUtils.get("persist.sys.azenithconf.logd") == "1"
                        distherm = PropertyUtils.get("persist.sys.azenithconf.DThermal") == "1"
                    }
    
                    // Pastikan socType juga tidak null sebelum nampilin list
                    if (socType != null && schedTunes != null && distherm != null && dlogcat != null && DTraces != null && waltTunes != null && sflstate != null && jitstate != null && fpsgogedstate != null && malischedstate != null) { 
                        ExpressiveList(
                            modifier = Modifier.padding(top = 16.dp),
                            content = listOfNotNull( // Menggunakan listOfNotNull agar item conditional bisa masuk
                                {
                                    ExpressiveListItemHighlight(
                                        headlineContent = { Text(stringResource(R.string.section_prefstweaks)) },
                                        leadingContent = { Icon(Icons.Filled.AddToPhotos, null) },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                                        onClick = {}
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "Sched Tunes",
                                        summary = "Optimize CPU frequency scaling for all CPU clusters (Schedutil/Schedhorizon)",
                                        checked = schedTunes!!,
                                        onCheckedChange = { isChecked ->
                                            schedTunes = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.schedtunes", if (isChecked) "1" else "0")
                                            
                                            // Logic: Jika Sched aktif, matikan Walt
                                            if (isChecked && waltTunes == true) {
                                                waltTunes = false
                                                PropertyUtils.set("persist.sys.azenithconf.walttunes", "0")
                                            }
                                        }
                                    )
                                },
                                // Tampilkan Walt Tunes hanya jika soctype == 2
                                if (socType == "2") {
                                    {
                                        ExpressiveSwitchItem(
                                            title = "Walt Governor Tunes",
                                            summary = "Optimize WALT governor parameters for faster CPU scheduling",
                                            checked = waltTunes!!,
                                            onCheckedChange = { isChecked ->
                                                waltTunes = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.walttunes", if (isChecked) "1" else "0")
                                                
                                                // Logic: Jika Walt aktif, matikan Sched
                                                if (isChecked && schedTunes == true) {
                                                    schedTunes = false
                                                    PropertyUtils.set("persist.sys.azenithconf.schedtunes", "0")
                                                }
                                            }
                                        )
                                    }
                                } else null,
                                {
                                    ExpressiveSwitchItem(
                                        title = "SurfaceFlinger Latency",
                                        summary = "Tweak SurfaceFlinger to reduce screen latency and improve responsiveness.",
                                        checked = sflstate!!,
                                        onCheckedChange = { isChecked ->
                                            sflstate = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.SFL", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "JIT Compilation (Just in Time)",
                                        summary = "Improve application performance by enabling JIT compilation",
                                        checked = jitstate!!,
                                        onCheckedChange = { isChecked ->
                                            jitstate = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.justintime", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "Disable Trace",
                                        summary = "Disable kernel tracing and android framework tracing",
                                        checked = DTraces!!,
                                        onCheckedChange = { isChecked ->
                                            DTraces = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.disabletrace", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "Disable System Logging",
                                        summary = "Disable system logging services to reduce background activity",
                                        checked = dlogcat!!,
                                        onCheckedChange = { isChecked ->
                                            dlogcat = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.logd", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                // Tampilkan item khusus MTK jika soctype == 1
                                if (socType == "1") {
                                    {
                                        ExpressiveSwitchItem(
                                            title = "FPSGO and GED Parameters",
                                            summary = "Apply optimized value of FPSGO and GED Parameter",
                                            checked = fpsgogedstate!!,
                                            onCheckedChange = { isChecked ->
                                                fpsgogedstate = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.fpsged", if (isChecked) "1" else "0")
                                            }
                                        )
                                    }
                                } else null,
                                if (socType == "1") {
                                    {
                                        ExpressiveSwitchItem(
                                            title = "GPU Mali Scheduling",
                                            summary = "Force the GPU to serialize job processing to improve predictability",
                                            checked = malischedstate!!,
                                            onCheckedChange = { isChecked ->
                                                malischedstate = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.malisched", if (isChecked) "1" else "0")
                                            }
                                        )
                                    }
                                } else null,
                                if (socType == "1") {
                                    {
                                        ExpressiveSwitchItem(
                                            title = "Disable Thermals",
                                            summary = "Kill thermal service",
                                            checked = distherm!!,
                                            onCheckedChange = { isChecked ->
                                                distherm = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.DThermal", if (isChecked) "1" else "0")
                                            }
                                        )
                                    }
                                } else null
                            )
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PreferenceTweakTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.prefs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
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