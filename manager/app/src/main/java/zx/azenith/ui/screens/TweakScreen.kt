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
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.SolidColor

                
@Composable
fun TweakScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val listState = rememberLazyListState()
    
    var currentRenderer by remember { mutableStateOf<String?>(null) }
    var currentRefreshRate by remember { mutableStateOf<Int?>(null) } 
    
    val scope = rememberCoroutineScope()
    
    // Fungsi pembantu untuk refresh data UI
    val updateUiData = {
        // Ambil Renderer
        currentRenderer = PropertyUtils.get("persist.sys.azenithconf.renderer", "Default")
        
        // Ambil Refresh Rate
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        currentRefreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.refreshRate.toInt()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate.toInt()
        }
    }
    
    var availableIOSchedulers by remember { mutableStateOf<List<String>>(emptyList()) }
    var balancedIOIndex by remember { mutableStateOf(0) }
    var performanceIOIndex by remember { mutableStateOf(0) }
    var powersaveIOIndex by remember { mutableStateOf(0) }

    // Fungsi untuk load data I/O Scheduler
    val loadIOSchedulers = {
        scope.launch {
            // 1. Cari Block Device yang valid (looping seperti di JS)
            val candidates = listOf("mmcblk0", "mmcblk1", "sda", "sdb", "sdc")
            var validBlock = ""
            
            for (block in candidates) {
                if (Shell.cmd("test -e /sys/block/$block/queue/scheduler").exec().isSuccess) {
                    validBlock = block
                    break
                }
            }

            if (validBlock.isNotEmpty()) {
                // 2. Baca scheduler dari device tersebut
                val result = Shell.cmd("cat /sys/block/$validBlock/queue/scheduler").exec()
                if (result.isSuccess) {
                    // Output biasanya "[none] mq-deadline kyber". Kita perlu bersihkan kurung []
                    val rawOut = result.out.firstOrNull() ?: ""
                    // Regex untuk ambil kata-katanya saja (membuang [] dan spasi)
                    val schedulers = rawOut.replace("[", "").replace("]", "").trim().split("\\s+".toRegex())
                    availableIOSchedulers = schedulers

                    // 3. Ambil nilai Balanced IO (Custom -> Fallback ke Default)
                    val currentBal = PropertyUtils.get("persist.sys.azenith.custom_default_balanced_IO").ifEmpty {
                        PropertyUtils.get("persist.sys.azenith.default_balanced_IO")
                    }
                    balancedIOIndex = schedulers.indexOf(currentBal).coerceAtLeast(0)

                    // 4. Ambil nilai Performance IO
                    val currentPerf = PropertyUtils.get("persist.sys.azenith.custom_performance_IO")
                    performanceIOIndex = schedulers.indexOf(currentPerf).coerceAtLeast(0)

                    // 5. Ambil nilai Powersave IO
                    val currentEco = PropertyUtils.get("persist.sys.azenith.custom_powersave_IO")
                    powersaveIOIndex = schedulers.indexOf(currentEco).coerceAtLeast(0)
                }
            }
        }
    }
    
    var availableGovernors by remember { mutableStateOf<List<String>>(emptyList()) }
    var defaultGovIndex by remember { mutableStateOf(0) }
    var powersaveGovIndex by remember { mutableStateOf(0) }
    
    // Fungsi untuk load data governor
    val loadGovernors = {
        scope.launch {
            // 1. Ambil list governor dari file sistem
            val result = Shell.cmd("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors").exec()
            if (result.isSuccess) {
                val govs = result.out.firstOrNull()?.trim()?.split("\\s+".toRegex()) ?: emptyList()
                availableGovernors = govs
                
                // 2. Ambil nilai default gov saat ini
                val currentDefault = PropertyUtils.get("persist.sys.azenith.custom_default_cpu_gov").ifEmpty {
                    PropertyUtils.get("persist.sys.azenith.default_cpu_gov")
                }
                defaultGovIndex = govs.indexOf(currentDefault).coerceAtLeast(0)
    
                // 3. Ambil nilai powersave gov saat ini
                val currentPowersave = PropertyUtils.get("persist.sys.azenith.custom_powersave_cpu_gov")
                powersaveGovIndex = govs.indexOf(currentPowersave).coerceAtLeast(0)
            }
        }
    }
    
    val initialOffset = remember {
        val saved = PropertyUtils.get("persist.sys.azenithconf.freqoffset", "Disabled")
        when (saved) {
            "90" -> 1f; "80" -> 2f; "70" -> 3f; "60" -> 4f; "50" -> 5f; "40" -> 6f; else -> 0f
        }
    }
    
    var freqOffsetIndex by remember { mutableStateOf(initialOffset) }
    val offsetLabels = listOf("Disabled", "90%", "80%", "70%", "60%", "50%", "40%")
    
        
    // Panggil loadGovernors di LaunchedEffect
    LaunchedEffect(Unit) {
        updateUiData()
        loadIOSchedulers() 
        loadGovernors()
    }
    
    // Load data awal
    LaunchedEffect(Unit) {
        updateUiData()
    }

    var showRendererDialog by remember { mutableStateOf(false) }
    var showRefreshRateDialog by remember { mutableStateOf(false) }
    
    RendererDialog(
        show = showRendererDialog,
        onDismiss = { showRendererDialog = false },
        onRenderer = { rendererReason ->
            scope.launch {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrender $rendererReason").submit()
                // 2. Langsung update UI setelah perintah dikirim
                delay(1000)
                updateUiData() 
                Toast.makeText(context, "Applied: $rendererReason", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
    RefreshRatePickerDialog(
        show = showRefreshRateDialog,
        onDismiss = { showRefreshRateDialog = false },
        onRefreshRatePicker = { refreshrateReason ->
            scope.launch {
                Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setrefreshrates $refreshrateReason").submit()
                // 3. Langsung update UI setelah perintah dikirim
                delay(1000)
                updateUiData()
                Toast.makeText(context, "Applied Selected Refresh Rate", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
    MaterialExpressiveTheme {        
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { TweakScreenTopAppBar(scrollBehavior) },
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
                             
                item {
                
                    val colorScheme = MaterialTheme.colorScheme
                    var liteState by remember { mutableStateOf<Boolean?>(null) }
                    
                    LaunchedEffect(Unit) {
                        liteState = PropertyUtils.get("persist.sys.azenithconf.cpulimit") == "1"
                    }
                    
                    // Animasi untuk progress bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = freqOffsetIndex / 6f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                        label = "OffsetProgress"
                    )
                    
                    if (liteState != null) {
                        ExpressiveList(
                            content = listOf(
                                {
                                    // Item ini akan punya warna background sendiri (misal: Primary Container)
                                    ExpressiveListItemHighlight(
                                        headlineContent = { Text(stringResource(R.string.section_CPUSettings)) },
                                        leadingContent = { Icon(Icons.Filled.DeveloperBoard, null) },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                                        onClick = {}
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "Perf Lite Mode",
                                        summary = "Reduce heating in performance profile by limiting CPU Frequencies",
                                        checked = liteState!!,
                                        onCheckedChange = { isChecked ->
                                            liteState = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.cpulimit", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    // Default CPU Governor
                                    ExpressiveDropdownItem(
                                        title = "Default CPU Governor",
                                        summary = "Governor used in Balanced Profiles",
                                        items = availableGovernors,
                                        selectedIndex = defaultGovIndex,
                                        onItemSelected = { index ->
                                            val selectedGov = availableGovernors[index]
                                            defaultGovIndex = index
                                            scope.launch {
                                                // Set property
                                                PropertyUtils.set("persist.sys.azenith.custom_default_cpu_gov", selectedGov)
                                                
                                                // Cek profile aktif (seperti logic JS)
                                                val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
                                                if (currentProfile == "2") {
                                                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov $selectedGov").submit()
                                                }
                                                Toast.makeText(context, "Default Governor set to $selectedGov", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                                {                                    
                                    // Powersave CPU Governor
                                    ExpressiveDropdownItem(
                                        
                                        title = "Powersave CPU Governor",
                                        summary = "Governor used in Powersave Profiles",
                                        items = availableGovernors,
                                        selectedIndex = powersaveGovIndex,
                                        onItemSelected = { index ->
                                            val selectedGov = availableGovernors[index]
                                            powersaveGovIndex = index
                                            scope.launch {
                                                // Set property
                                                PropertyUtils.set("persist.sys.azenith.custom_powersave_cpu_gov", selectedGov)
                                                
                                                // Cek profile aktif (seperti logic JS: "3" untuk powersave)
                                                val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
                                                if (currentProfile == "3") {
                                                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsgov $selectedGov").submit()
                                                }
                                                Toast.makeText(context, "Powersave Governor set to $selectedGov", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                                {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Frequency Offset",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Limit max freq by percentage",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.outline
                                                )
                                            }
                                            
                                            Surface(
                                                color = if (freqOffsetIndex.roundToInt() == 0) colorScheme.surfaceVariant else colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = offsetLabels[freqOffsetIndex.roundToInt()],
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (freqOffsetIndex.roundToInt() == 0) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    
                                        Spacer(modifier = Modifier.height(24.dp))
                                    
                                        // Progress Bar
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colorScheme.surfaceContainerHighest)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedProgress)
                                                    .height(8.dp)
                                                    .align(Alignment.CenterStart)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(colorScheme.primary.copy(alpha = 0.6f), colorScheme.primary)
                                                        )
                                                    )
                                            )
                                        }
                                    
                                        Spacer(modifier = Modifier.height(4.dp))
                                    
                                        Slider(
                                            value = freqOffsetIndex,
                                            onValueChange = { newValue -> 
                                                freqOffsetIndex = newValue
                                            },
                                            onValueChangeFinished = {
                                                val index = freqOffsetIndex.roundToInt()
                                                // Perbaikan: Jika index 0, set ke "Disabled"
                                                val propValue = if (index == 0) "Disabled" else offsetLabels[index].replace("%", "")
                                                
                                                PropertyUtils.set("persist.sys.azenithconf.freqoffset", propValue)
                                            },
                                            valueRange = 0f..6f,
                                            steps = 5, 
                                            colors = SliderDefaults.colors(
                                                thumbColor = colorScheme.primary,
                                                activeTrackColor = Color.Transparent,
                                                inactiveTrackColor = Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth().height(32.dp)
                                        )
                                    
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Disabled", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
                                            Text("40%", style = MaterialTheme.typography.labelSmall, color = colorScheme.outline)
                                        }
                                    }
                                }
                            )
                        )
                    } else {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                item {
                    if (availableIOSchedulers.isNotEmpty()) {
                        ExpressiveList(
                            modifier = Modifier.padding(top = 16.dp),
                            content = listOf(
                                {
                                    ExpressiveListItemHighlight(
                                        headlineContent = { Text("I/O Settings") },
                                        leadingContent = { Icon(Icons.Rounded.Storage, null) }, // Icon header tetap ada biar rapi
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        onClick = {}
                                    )
                                },
                                {
                                    // Balanced I/O
                                    ExpressiveDropdownItem(
                                         // Sesuai request: Gausah pake icon
                                        title = "Balanced I/O Scheduler",
                                        summary = "Scheduler used in Balanced Profiles",
                                        items = availableIOSchedulers,
                                        selectedIndex = balancedIOIndex,
                                        onItemSelected = { index ->
                                            val selectedIO = availableIOSchedulers[index]
                                            balancedIOIndex = index
                                            scope.launch {
                                                // Set property
                                                PropertyUtils.set("persist.sys.azenith.custom_default_balanced_IO", selectedIO)
                                                
                                                // Cek profile aktif (2 = Balanced)
                                                val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
                                                if (currentProfile == "2") {
                                                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").submit()
                                                }
                                                Toast.makeText(context, "Balanced I/O set to $selectedIO", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                                {
                                    // Performance I/O
                                    ExpressiveDropdownItem(
                                         // Tanpa icon
                                        title = "Performance I/O Scheduler",
                                        summary = "Scheduler used in Performance Profiles",
                                        items = availableIOSchedulers,
                                        selectedIndex = performanceIOIndex,
                                        onItemSelected = { index ->
                                            val selectedIO = availableIOSchedulers[index]
                                            performanceIOIndex = index
                                            scope.launch {
                                                PropertyUtils.set("persist.sys.azenith.custom_performance_IO", selectedIO)
                                                
                                                // Cek profile aktif (1 = Performance)
                                                val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
                                                if (currentProfile == "1") {
                                                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").submit()
                                                }
                                                Toast.makeText(context, "Performance I/O set to $selectedIO", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                                {
                                    // Powersave I/O
                                    ExpressiveDropdownItem(
                                         // Tanpa icon
                                        title = "Powersave I/O Scheduler",
                                        summary = "Scheduler used in Powersave Profiles",
                                        items = availableIOSchedulers,
                                        selectedIndex = powersaveIOIndex,
                                        onItemSelected = { index ->
                                            val selectedIO = availableIOSchedulers[index]
                                            powersaveIOIndex = index
                                            scope.launch {
                                                PropertyUtils.set("persist.sys.azenith.custom_powersave_IO", selectedIO)
                                                
                                                // Cek profile aktif (3 = Powersave/Eco)
                                                val currentProfile = Shell.cmd("cat /data/adb/.config/AZenith/API/current_profile").exec().out.firstOrNull()?.trim()
                                                if (currentProfile == "3") {
                                                    Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setsIO $selectedIO").submit()
                                                }
                                                Toast.makeText(context, "Powersave I/O set to $selectedIO", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            )
                        )
                    }
                }
                
                item {
                    // State nullable
                    var preloadState by remember { mutableStateOf<Boolean?>(null) }
                    var memKillerState by remember { mutableStateOf<Boolean?>(null) }
                    var appPriorState by remember { mutableStateOf<Boolean?>(null) }
                    var dndState by remember { mutableStateOf<Boolean?>(null) }
                    var fstrimState by remember { mutableStateOf<Boolean?>(null) }
    
                    // Load state system properties
                    LaunchedEffect(Unit) {
                                        
                        // Ambil semua state tweak
                        preloadState = PropertyUtils.get("persist.sys.azenithconf.APreload") == "1"
                        memKillerState = PropertyUtils.get("persist.sys.azenithconf.clearbg") == "1"
                        appPriorState = PropertyUtils.get("persist.sys.azenithconf.iosched") == "1"
                        dndState = PropertyUtils.get("persist.sys.azenithconf.dnd") == "1"
                        fstrimState = PropertyUtils.get("persist.sys.azenithconf.fstrim") == "1"
                    }
    
                    
                    if (preloadState != null && memKillerState != null && appPriorState != null && dndState != null && fstrimState != null) { 
                        ExpressiveList(
                            modifier = Modifier.padding(top = 16.dp),
                            content = listOf(
                                {
                                    // Item ini akan punya warna background sendiri (misal: Primary Container)
                                    ExpressiveListItemHighlight(
                                        headlineContent = { Text(stringResource(R.string.section_additionalsettings)) },
                                        leadingContent = { Icon(Icons.Filled.Gamepad, null) },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                                        onClick = {}
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "Game Preload",
                                        summary = "Preload libraries at game start",
                                        checked = preloadState!!,
                                        onCheckedChange = { isChecked ->
                                            preloadState = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.APreload", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "Memory Killer",
                                        summary = "Clear ram usage at gamestart",
                                        checked = memKillerState!!,
                                        onCheckedChange = { isChecked ->
                                            memKillerState = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.clearbg", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "App Priority Control",
                                        summary = "Increase running game I/O scheduling priority in Performance profiles",
                                        checked = appPriorState!!,
                                        onCheckedChange = { isChecked ->
                                            appPriorState = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.iosched", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "DND Mode on Gaming",
                                        summary = "Enable DND in Performance Profile",
                                        checked = dndState!!,
                                        onCheckedChange = { isChecked ->
                                            dndState = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.dnd", if (isChecked) "1" else "0")
                                        }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "Trim Filesystem Partition",
                                        summary = "Trim unused blocks in system partitions to Increase I/O Performance",
                                        checked = fstrimState!!,
                                        onCheckedChange = { isChecked ->
                                            fstrimState = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.fstrim", if (isChecked) "1" else "0")
                                        }
                                    )
                                }
                            )
                        )
                    } else {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                if (currentRenderer != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExpressiveTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.WebStories,
                                label = stringResource(R.string.refreshrates),
                                value = "${currentRefreshRate}Hz",
                                showArrow = true,
                                highlight = false
                            ) {
                                showRefreshRateDialog = true
                            }
                
                            ExpressiveTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.SettingsSuggest,
                                label = stringResource(R.string.renderengine),
                                value = currentRenderer!!.uppercase(),
                                showArrow = true,
                                highlight = false
                            ) {
                                showRendererDialog = true
                            }
                        }
                    }
                }

                item {
                    var thermalstate by remember { mutableStateOf<Boolean?>(null) }
    
                    // Load state system properties
                    LaunchedEffect(Unit) {
                                        
                        // Ambil semua state tweak
                        thermalstate = PropertyUtils.get("persist.sys.azenithconf.thermalcore") == "1"
                    }
                    
                    if (thermalstate != null) {
                        ExpressiveList(
                            modifier = Modifier.padding(top = 16.dp),
                            content = listOf(
                                {
                                    // Item ini akan punya warna background sendiri (misal: Primary Container)
                                    ExpressiveListItemHighlight(
                                        headlineContent = { Text(stringResource(R.string.section_addons)) },
                                        leadingContent = { Icon(Icons.Filled.Extension, null) },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                                        onClick = {}
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            navController.navigate("bypasschg")
                                        },
                                        headlineContent = { Text(stringResource(R.string.bcharging)) },
                                        supportingContent = { Text(stringResource(R.string.bcharging_desc)) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            navController.navigate("colorscheme")
                                        },
                                        headlineContent = { Text(stringResource(R.string.schemecolor)) },
                                        supportingContent = { Text(stringResource(R.string.schemecolordesc)) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveListItem(
                                        onClick = {
                                            navController.navigate("preferenced")
                                        },
                                        headlineContent = { Text(stringResource(R.string.prefs)) },
                                        supportingContent = { Text(stringResource(R.string.prefsdesc)) },
                                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )
                                },
                                {
                                    ExpressiveSwitchItem(
                                        title = "ThermalCore Service",
                                        summary = "Automatically manage system thermal based on device behavior, prevent phone getting too hot in daily use while maintaining device stability",
                                        checked = thermalstate!!,
                                        onCheckedChange = { isChecked ->
                                            thermalstate = isChecked
                                            PropertyUtils.set("persist.sys.azenithconf.thermalcore", if (isChecked) "1" else "0")
                                            Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-utilityconf setthermalcore ${if (isChecked) "1" else "0"}").submit()
                                        }
                                    )
                                }
                            )
                        )
                    } else {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }                        
            }
        }
    }
}

@Composable
fun ExpressiveTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean,
    showArrow: Boolean = false, // Parameter baru
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .padding(top = 16.dp),
        color = if (highlight) colorScheme.secondaryContainer else colorScheme.surfaceColorAtElevation(1.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Column {
                Icon(icon, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
            }
            
            if (showArrow) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd).size(20.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TweakScreenTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
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
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Tweaks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
