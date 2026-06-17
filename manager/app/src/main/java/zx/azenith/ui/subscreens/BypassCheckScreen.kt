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
 
package zx.azenith.ui.subscreens
 
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.util.PropertyUtils
import com.topjohnwu.superuser.CallbackList
import kotlinx.coroutines.launch
import com.topjohnwu.superuser.Shell
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShellOutput(
    val text: String,
    val isCompleted: Boolean = false
)

@Composable
fun BypassChargeCheckScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dialog Handle
    val confirmDialogHandle = rememberConfirmDialog()

    // State Pengaturan Bypass
    var activePath by remember { mutableStateOf("") }
    var availablePaths by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isChargerConnected by remember { mutableStateOf(false) }
    
    // State Proses Diagnosis
    val logs = remember { mutableStateListOf<ShellOutput>() }
    var isRunning by remember { mutableStateOf(false) }
    var hasRunDiagnosis by remember { mutableStateOf(false) }
    
    // Scroll State untuk konsol
    val logScrollState = rememberScrollState()

    // 1. Ambil status properti awal & list path via argumen -bpl ke daemon
    // Ditambah callback onComplete untuk mengeksekusi dialog setelah selesai scan
    fun refreshBypassData(onComplete: ((List<Pair<String, String>>) -> Unit)? = null) {
        activePath = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "UNSUPPORTED")
        
        scope.launch(Dispatchers.IO) {
            val output = Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service -bpl").exec().out
            val parsedList = output.filter { it.contains("FOUND") && !it.contains("NOT FOUND") }.map { line ->
                val cleanLine = line.replace("\u001B\\[[;\\d]*m".toRegex(), "")
                val parts = cleanLine.split("|")
                val name = parts.getOrNull(0)?.trim() ?: "UNKNOWN"
                val path = parts.getOrNull(2)?.trim() ?: ""
                Pair(name, path)
            }
            withContext(Dispatchers.Main) {
                availablePaths = parsedList
                onComplete?.invoke(parsedList)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshBypassData()
    }

    // 2. Real-time Charger Connection Observer
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                isChargerConnected = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                     status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Auto scroll konsol log pas diagnosis jalan
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logScrollState.animateScrollTo(logScrollState.maxValue)
        }
    }

    // 3. Modifier pencegah bocornya scroll konsol ke parent LazyColumn
    val blockParentScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Return "available" artinya kita memakan semua sisa scroll biar gak nyampai ke parent
                return available
            }
        }
    }

    // Fungsi trigger proses diagnosis otomatis bawaan daemon (-cbc)
    fun runCompatibilityCheck() {
        logs.clear()
        isRunning = true
        hasRunDiagnosis = true
        
        scope.launch(Dispatchers.IO) {
            val callbackList = object : CallbackList<String>() {
                override fun onAddElement(line: String) {
                    scope.launch(Dispatchers.Main.immediate) {
                        logs.add(ShellOutput(line, true))
                    }
                }
            }

            val binaryPath = "/data/adb/modules/AZenith/system/bin/sys.azenith-service"
            Shell.cmd("$binaryPath -cbc 2>&1").to(callbackList).submit {
                isRunning = false
                refreshBypassData { paths ->
                    // Memunculkan dialog jika node bypass ditemukan setelah scan selesai
                    if (paths.isNotEmpty()) {
                        scope.launch {
                            val result = confirmDialogHandle.awaitConfirm(
                                title = "Diagnosis Complete",
                                content = "Found working nodes for your device! Do you want to automatically apply the recommended node [${paths.first().first}]?",
                                confirm = "Apply",
                                dismiss = "Dismiss"
                            )
                            if (result == ConfirmResult.Confirmed) {
                                val targetNode = paths.first().first
                                PropertyUtils.set("persist.sys.azenithconf.bypasspath", targetNode)
                                activePath = targetNode
                            }
                        }
                    }
                }
            }
        }
    }

    MaterialExpressiveTheme {
        ConfirmDialogHost(handle = confirmDialogHandle)

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { 
                BypassChgCheckTopAppBar(
                    scrollBehavior = scrollBehavior, 
                    onBack = { navController.popBackStack() }
                ) 
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isRunning && hasRunDiagnosis,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.popBackStack() },
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        icon = { Icon(Icons.Rounded.Check, null) },
                        text = { Text(stringResource(R.string.done)) }
                    )
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // SECTION 1: Current Active Path Information
                item {
                    Text(
                        text = "Current Status",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    ExpressiveList(
                        content = listOf {
                            val isUnsupported = activePath == "UNSUPPORTED" || activePath.isEmpty()
                            ExpressiveListItem(
                                headlineContent = { 
                                    // Animasi perubahan teks saat target berubah
                                    AnimatedContent(targetState = activePath, label = "activePathAnim") { path ->
                                        Text(
                                            text = if (path == "UNSUPPORTED" || path.isEmpty()) "Bypass Charging Disabled" else path,
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    }
                                },
                                supportingContent = { 
                                    Text(if (isUnsupported) "No active bypass node mapped to system properties." else "Active charging gateway node bypass channel.") 
                                },
                                leadingContent = { 
                                    LeadingIcon(
                                        icon = if (isUnsupported) Icons.Rounded.Block else Icons.Rounded.ElectricBolt,
                                        containerColor = if (isUnsupported) colorScheme.error.copy(alpha = 0.12f) else colorScheme.primary.copy(alpha = 0.12f),
                                        contentColor = if (isUnsupported) colorScheme.error else colorScheme.primary
                                    ) 
                                }
                            )
                        }
                    )
                }

                // SECTION 2: Diagnosis System Controller
                item {
                    Text(
                        text = "Gateway Diagnostics",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = colorScheme.surfaceColorAtElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isRunning) "Diagnostic in Progress" else "Compatibility Automated Scan",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    // Animasi perubahan teks deteksi charger / loading
                                    AnimatedContent(
                                        targetState = Triple(isChargerConnected, isRunning, hasRunDiagnosis),
                                        label = "chargerStatusAnim"
                                    ) { (connected, running, _) ->
                                        Text(
                                            text = if (!connected) "Power cable detached. Plug in charger first to unlock diagnostics."
                                                   else if (running) "Probing kernel sysfs power rails... Please stand by."
                                                   else "Safely test your device compatibility and isolate current values.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (!connected) colorScheme.error else colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                AnimatedVisibility(
                                    visible = isRunning,
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    LoadingIndicator(modifier = Modifier.size(28.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        val result = confirmDialogHandle.awaitConfirm(
                                            title = "Start Hardware Test?",
                                            content = "AZenith will perform current drop tests across embedded battery nodes. This safe diagnostics sequence loops for several seconds.",
                                            confirm = "Begin Check",
                                            dismiss = "Cancel"
                                        )
                                        if (result == ConfirmResult.Confirmed) {
                                            runCompatibilityCheck()
                                        }
                                    }
                                },
                                enabled = isChargerConnected && !isRunning,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Launch Compatibility Check")
                            }
                        }
                    }
                }

                // Live Console Terminal Output
                // Animasi saat container konsol dimunculkan/disembunyikan
                item {
                    AnimatedVisibility(
                        visible = logs.isNotEmpty() || isRunning,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .nestedScroll(blockParentScroll), // Cegah bocor scroll ke parent
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C))
                        ) {
                            SelectionContainer {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    // Diganti ke Column + verticalScroll agar terisolasi lebih rapi dibanding LazyColumn di dalam LazyColumn
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(logScrollState)
                                            .horizontalScroll(rememberScrollState())
                                    ) {
                                        logs.forEach { line ->
                                            Text(
                                                text = line.text.parseAsAnsiAnnotatedString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 16.sp
                                                ),
                                                color = Color.White,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 3: Found Available Node Paths
                item {
                    Text(
                        text = "Available Target Nodes (${availablePaths.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    
                    if (availablePaths.isEmpty()) {
                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    headlineContent = { Text("No Compatible Nodes") },
                                    supportingContent = { Text("Run diagnostics above or check if your kernel has charging bypass nodes.") },
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.SearchOff) }
                                )
                            }
                        )
                    } else {
                        ExpressiveList(
                            content = availablePaths.map { pathNode ->
                                {
                                    val isSelected = activePath == pathNode.first
                                    ExpressiveListItemHighlight(
                                        containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                        onClick = {
                                            if (!isRunning) {
                                                scope.launch {
                                                    val result = confirmDialogHandle.awaitConfirm(
                                                        title = "Switch Control Node?",
                                                        content = "Do you want to explicitly force your bypass profile gateway target to [${pathNode.first}]?",
                                                        confirm = "Apply Path",
                                                        dismiss = "Dismiss"
                                                    )
                                                    if (result == ConfirmResult.Confirmed) {
                                                        PropertyUtils.set("persist.sys.azenithconf.bypasspath", pathNode.first)
                                                        activePath = pathNode.first
                                                    }
                                                }
                                            }
                                        },
                                        headlineContent = { 
                                            Text(
                                                text = pathNode.first,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                            ) 
                                        },
                                        supportingContent = { Text(pathNode.second) },
                                        leadingContent = {
                                            LeadingIcon(
                                                icon = Icons.Rounded.FolderOpen,
                                                containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceVariant,
                                                contentColor = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingContent = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = "Active",
                                                    tint = colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BypassChgCheckTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onBack: () -> Unit) {
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
                    text = stringResource(R.string.CompatibilityCheck),
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
