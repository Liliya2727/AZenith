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

package zx.azenith.ui.subscreens


import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.util.PropertyUtils


@Composable
fun PreferenceTweakScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    
    MaterialExpressiveTheme {        
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { PreferenceTweakTopAppBar(
                scrollBehavior,
                onBack = { navController.popBackStack() }
                ) 
            },
            containerColor = colorScheme.surface
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
                    Spacer(modifier = Modifier.height(16.dp))
                    ExpressiveList(
                        content = listOf {
                            ExpressiveInfoCard(
                                supportingContent = { Text(text = stringResource(R.string.str_apply_add_on_configurations_ta)) },
                                leadingContent = { LeadingIcon(icon = Icons.Filled.Info) },
                                containerColor = colorScheme.surfaceContainerLow,
                                onClick = {}
                            )
                        }
                    )
                }

                item { PrefSectionTitle(stringResource(R.string.section_prefstweaks)) }
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
    
                    if (socType != null && schedTunes != null && distherm != null && dlogcat != null && DTraces != null && waltTunes != null && sflstate != null && jitstate != null && fpsgogedstate != null && malischedstate != null) { 
                        
                        val isMediaTek = socType == "1"
                        val isSnapdragon = socType == "2"

                        ExpressiveList(
                            content = listOf(
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.Tune,
                                        title = stringResource(R.string.sched_tunes),
                                        summary = stringResource(R.string.sched_tunes_desc),
                                        checked = schedTunes!!,
                                        onCheckedChange = { isChecked ->
                                            schedTunes = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.schedtunes", if (isChecked) "1" else "0")
                                            
                                            if (isChecked && waltTunes == true) {
                                                waltTunes = false
                                                PropertyUtils.set("persist.sys.azenithconf.walttunes", "0")
                                            }
                                        }
                                    )
                                },
                                {
                                    Box(modifier = Modifier.alpha(if (isSnapdragon) 1f else 0.4f)) {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.Timeline,
                                            title = stringResource(R.string.walt_tunes),
                                            summary = if (isSnapdragon) stringResource(R.string.walt_tunes_desc) else "This option is only available for Snapdragon devices.",
                                            checked = waltTunes!!,
                                            enabled = isSnapdragon,
                                            onCheckedChange = { isChecked ->
                                                waltTunes = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.walttunes", if (isChecked) "1" else "0")
                                                
                                                if (isChecked && schedTunes == true) {
                                                    schedTunes = false
                                                    PropertyUtils.set("persist.sys.azenithconf.schedtunes", "0")
                                                }
                                            }
                                        )
                                    }
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.Layers,
                                        title = stringResource(R.string.sfl_latency),
                                        summary = stringResource(R.string.sfl_latency_desc),
                                        checked = sflstate!!,
                                        onCheckedChange = { isChecked ->
                                            sflstate = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.SFL", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.Bolt,
                                        title = stringResource(R.string.jit_compilation),
                                        summary = stringResource(R.string.jit_compilation_desc),
                                        checked = jitstate!!,
                                        onCheckedChange = { isChecked ->
                                            jitstate = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.justintime", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.Rounded.TrackChanges,
                                        title = stringResource(R.string.disable_trace),
                                        summary = stringResource(R.string.disable_trace_desc),
                                        checked = DTraces!!,
                                        onCheckedChange = { isChecked ->
                                            DTraces = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.disabletrace", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        icon = Icons.AutoMirrored.Rounded.Notes,
                                        title = stringResource(R.string.disable_logging),
                                        summary = stringResource(R.string.disable_logging_desc),
                                        checked = dlogcat!!,
                                        onCheckedChange = { isChecked ->
                                            dlogcat = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.logd", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    Box(modifier = Modifier.alpha(if (isMediaTek) 1f else 0.4f)) {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.Speed,
                                            title = stringResource(R.string.fpsgo_ged),
                                            summary = if (isMediaTek) stringResource(R.string.fpsgo_ged_desc) else "This option is only available for MediaTek devices.",
                                            checked = fpsgogedstate!!,
                                            enabled = isMediaTek,
                                            onCheckedChange = { isChecked ->
                                                fpsgogedstate = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.fpsged", if (isChecked) "1" else "0")
                                            }
                                        )
                                    }
                                },
                                {
                                    Box(modifier = Modifier.alpha(if (isMediaTek) 1f else 0.4f)) {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.DeveloperBoard,
                                            title = stringResource(R.string.gpu_mali),
                                            summary = if (isMediaTek) stringResource(R.string.gpu_mali_desc) else "This option is only available for MediaTek devices.",
                                            checked = malischedstate!!,
                                            enabled = isMediaTek,
                                            onCheckedChange = { isChecked ->
                                                malischedstate = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.malisched", if (isChecked) "1" else "0")
                                            }
                                        )
                                    }
                                },
                                {
                                    Box(modifier = Modifier.alpha(if (isMediaTek) 1f else 0.4f)) {
                                        ExpressiveSwitchItem(
                                            icon = Icons.Rounded.Thermostat,
                                            title = stringResource(R.string.disable_thermals),
                                            summary = if (isMediaTek) stringResource(R.string.disable_thermals_desc) else "This option is only available for MediaTek devices.",
                                            checked = distherm!!,
                                            enabled = isMediaTek,
                                            onCheckedChange = { isChecked ->
                                                distherm = isChecked
                                                PropertyUtils.set("persist.sys.azenithconf.DThermal", if (isChecked) "1" else "0")
                                            }
                                        )
                                    }
                                }
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
fun PrefSectionTitle(text: String) {
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
        LargeFlexibleTopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.prefs),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
