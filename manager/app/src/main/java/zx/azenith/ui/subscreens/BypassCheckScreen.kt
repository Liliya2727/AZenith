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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileOutputStream
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.*
import zx.azenith.ui.util.PropertyUtils


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


    val confirmDialogHandle = rememberConfirmDialog()


    var activePath by remember { mutableStateOf("") }
    var availablePaths by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isChargerConnected by remember { mutableStateOf(false) }
    

    val logs = remember { mutableStateListOf<ShellOutput>() }
    var isRunning by remember { mutableStateOf(false) }
    var hasRunDiagnosis by remember { mutableStateOf(false) }
    var isConsoleClosed by remember { mutableStateOf(false) }
    

    val logScrollState = rememberScrollState()


    fun refreshBypassData(onComplete: ((List<Pair<String, String>>) -> Unit)? = null) {
        activePath = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "UNSUPPORTED")
        
        scope.launch(Dispatchers.IO) {
            val output = Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service -bpl").exec().out
            val parsedList = output.filter { it.contains("FOUND") && !it.contains("NOT FOUND") }.map { line ->
                val cleanLine = line.replace("\u001B\\[[;\\d]*m".toRegex(), "")
                val parts = cleanLine.split("|")
                val name = parts.getOrNull(0)?.trim() ?: context.getString(R.string.status_unknown)
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


    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && !isRunning) {
            logScrollState.animateScrollTo(logScrollState.maxValue)
        }
    }


    val blockParentScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available
            }
        }
    }


    fun runCompatibilityCheck() {
        logs.clear()
        isRunning = true
        hasRunDiagnosis = false
        isConsoleClosed = false
        
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
                hasRunDiagnosis = true
                refreshBypassData { paths ->

                    if (paths.isNotEmpty()) {
                        scope.launch {
                            val result = confirmDialogHandle.awaitConfirm(
                                title = context.getString(R.string.dialog_diagnosis_complete_title),
                                content = context.getString(R.string.dialog_diagnosis_complete_content, paths.first().first),
                                confirm = context.getString(R.string.dialog_apply),
                                dismiss = context.getString(R.string.dialog_dismiss)
                            )
                            if (result == ConfirmResult.Confirmed) {
                                val targetNode = paths.first().first
                                PropertyUtils.set("persist.sys.azenithconf.bypasspath", targetNode)
                                withContext(Dispatchers.IO) {
                                    val file = SuFile("/data/adb/.config/AZenith/bypasschgconfig/bypasspath")
                                    SuFileOutputStream.open(file).writer().use { writer ->
                                        writer.write(targetNode)
                                    }
                                }
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
                
                item {
                    BypassCheckTitle(text = stringResource(R.string.section_current_status))
                        
                    ExpressiveList(
                        content = listOf {
                            val isUnsupported = activePath == "UNSUPPORTED" || activePath.isEmpty()
                            ExpressiveListItem(
                                headlineContent = { 
                                    AnimatedContent(targetState = activePath, label = "activePathAnim") { path ->
                                        Text(
                                            text = if (path == "UNSUPPORTED" || path.isEmpty()) stringResource(R.string.str_no_active_nodes) else path,
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    }
                                },
                                supportingContent = { 
                                    Text(if (isUnsupported) stringResource(R.string.str_no_active_nodes_desc) else stringResource(R.string.str_active_bypass_node)) 
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
                item {
                    Column {
                        BypassCheckTitle(text = stringResource(R.string.section_diagnostics))
                        
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AnimatedContent(
                                                targetState = isRunning,
                                                label = "scanIconAnim"
                                            ) { running ->
                                                LeadingIcon(
                                                    icon = if (running) Icons.Rounded.Memory else Icons.AutoMirrored.Rounded.ManageSearch,
                                                    contentDescription = stringResource(R.string.cd_scan_icon)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = if (isRunning) stringResource(R.string.str_diagnostic_in_progress_title) else stringResource(R.string.str_scan_nodes),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }


                                        Spacer(modifier = Modifier.height(6.dp))
                                        AnimatedContent(
                                            targetState = Triple(isChargerConnected, isRunning, hasRunDiagnosis),
                                            label = "chargerStatusAnim"
                                        ) { (connected, running, _) ->
                                            Text(
                                                text = if (!connected) stringResource(R.string.str_plug_in_charger)
                                                       else if (running) stringResource(R.string.str_checking_current)
                                                       else stringResource(R.string.str_safely_test),
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
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                            color = colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val result = confirmDialogHandle.awaitConfirm(
                                                title = context.getString(R.string.dialog_start_hw_test_title),
                                                content = context.getString(R.string.dialog_start_hw_test_content),
                                                confirm = context.getString(R.string.dialog_begin_check),
                                                dismiss = context.getString(R.string.dialog_cancel)
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
                                    Text(stringResource(R.string.str_launch_compatibility_check))
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !isRunning && hasRunDiagnosis && logs.isNotEmpty() && !isConsoleClosed,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {

                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp)
                                        .nestedScroll(blockParentScroll), 
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141C))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        SelectionContainer {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp)
                                                    .padding(top = 28.dp)
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
                                        
                                        IconButton(
                                            onClick = { isConsoleClosed = true },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.str_close_logs),
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { 
                    BypassCheckTitle(text = stringResource(R.string.str_available_nodes, availablePaths.size))
                
                    if (availablePaths.isEmpty()) {
                        ExpressiveList(
                            content = listOf {
                                ExpressiveListItem(
                                    headlineContent = { Text(stringResource(R.string.str_no_compatible_nodes)) },
                                    supportingContent = { Text(stringResource(R.string.str_run_diagnostics_above_or_check)) },
                                    leadingContent = { LeadingIcon(icon = Icons.Rounded.SearchOff) }
                                )
                            }
                        )
                    } else {
                        ExpressiveList(
                            content = availablePaths.map { pathNode ->
                                {
                                    val isSelected = activePath == pathNode.first
                                    


                                    val textScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.08f else 1.0f,
                                        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
                                        label = "textScaleAnim"
                                    )

                                    ExpressiveListItemHighlight(
                                        containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                        onClick = {
                                            if (!isRunning) {
                                                scope.launch {
                                                    val result = confirmDialogHandle.awaitConfirm(
                                                        title = context.getString(R.string.dialog_switch_node_title),
                                                        content = context.getString(R.string.dialog_switch_node_content, pathNode.first),
                                                        confirm = context.getString(R.string.dialog_apply_path),
                                                        dismiss = context.getString(R.string.dialog_dismiss)
                                                    )
                                                    if (result == ConfirmResult.Confirmed) {
                                                        PropertyUtils.set("persist.sys.azenithconf.bypasspath", pathNode.first)
                                                        withContext(Dispatchers.IO) {
                                                            val file = SuFile("/data/adb/.config/AZenith/bypasschgconfig/bypasspath")
                                                            SuFileOutputStream.open(file).writer().use { writer ->
                                                                writer.write(pathNode.first)
                                                            }
                                                        }
                                                        activePath = pathNode.first
                                                    }
                                                }
                                            }
                                        },
                                        headlineContent = { 
                                            Text(
                                                text = pathNode.first,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                                                modifier = Modifier.graphicsLayer {
                                                    scaleX = textScale
                                                    scaleY = textScale
                                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                                }
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

                                            AnimatedVisibility(
                                                visible = isSelected,
                                                enter = scaleIn(tween(durationMillis = 200, easing = LinearOutSlowInEasing)) + fadeIn(tween(200)),
                                                exit = scaleOut(tween(durationMillis = 150)) + fadeOut(tween(150))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
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
fun BypassCheckTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 12.dp,
            end = 12.dp,
            bottom = 8.dp
        )
    )
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
