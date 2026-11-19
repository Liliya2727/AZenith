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

import zx.azenith.ui.theme.azShape


import zx.azenith.ui.navigation.safePopBackStack
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
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

private const val MAX_BYPASS_LOG_LINES = 1500

@Composable
fun BypassChargeCheckScreen(navController: NavController) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val coroutineScope = rememberCoroutineScope()
    val portraitListState = rememberLazyListState()
    val landscapeRightListState = rememberLazyListState()

    val confirmDialogHandle = rememberConfirmDialog()

    RegisterGamepadActions(
        enabled = true,
        onBack = {
            navController.safePopBackStack()
            true
        },
        scrollState = if (isLandscape) landscapeRightListState else portraitListState,
    )


    var activePath by remember { mutableStateOf("") }
    var availablePaths by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    // availablePaths is empty both while the node scan is still running and when
    // it genuinely finds nothing, and the two states show different content, so
    // the scan needs its own flag rather than inferring from an empty list.
    var isLoadingPaths by remember { mutableStateOf(true) }
    var isChargerConnected by remember { mutableStateOf(false) }
    // Node whose action sheet is open. Tapping a node never switches it directly:
    // the sheet offers switching or running a single node test first.
    var sheetNode by remember { mutableStateOf<Pair<String, String>?>(null) }
    

    val logs = remember { mutableStateListOf<ShellOutput>() }
    var isRunning by remember { mutableStateOf(false) }
    var hasRunDiagnosis by remember { mutableStateOf(false) }
    var isConsoleClosed by remember { mutableStateOf(false) }
    

    val logScrollState = rememberScrollState()


    fun refreshBypassData(onComplete: ((List<Pair<String, String>>) -> Unit)? = null) {
        activePath = PropertyUtils.get("persist.sys.azenithconf.bypasspath", "UNSUPPORTED")
        
        scope.launch(Dispatchers.IO) {
            val binaryPath = "/data/adb/modules/AZenith/system/bin/sys.azenith-service"

            // A previous diagnosis can be killed while a charge node is ON: the
            // shell process dies and nobody restores the node. The daemon records
            // that intent on disk, so replay it here and the user does not have
            // to reboot just to charge again.
            val restoreOut = Shell.cmd("$binaryPath -rp").exec().out
            val restored = restoreOut.any { it.contains("Restored a charge node") }

            val output = Shell.cmd("$binaryPath -bpl").exec().out
            val parsedList = output
                .map { it.replace("\\u001B\\[[;\\d]*m".toRegex(), "") }
                .filter { it.contains("FOUND") && !it.contains("NOT FOUND") }
                .map { line ->
                    val parts = line.split("|")
                    val name = parts.getOrNull(0)?.trim() ?: context.getString(R.string.status_unknown)
                    val path = parts.getOrNull(2)?.trim() ?: ""
                    Pair(name, path)
                }
            withContext(Dispatchers.Main) {
                if (restored) {
                    logs.add(
                        ShellOutput(
                            "[INFO] A charge node left ON by an interrupted test was restored.",
                            true
                        )
                    )
                }
                availablePaths = parsedList
                isLoadingPaths = false
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
        if (logs.isNotEmpty()) {
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


    fun applyBypassNode(nodeName: String) {
        PropertyUtils.set("persist.sys.azenithconf.bypasspath", nodeName)
        scope.launch(Dispatchers.IO) {
            val file = SuFile("/data/adb/.config/AZenith/bypasschgconfig/bypasspath")
            SuFileOutputStream.open(file).writer().use { writer ->
                writer.write(nodeName)
            }
        }
        activePath = nodeName
    }

    fun confirmSwitchNode(nodeName: String) {
        scope.launch {
            val result = confirmDialogHandle.awaitConfirm(
                title = context.getString(R.string.dialog_switch_node_title),
                content = context.getString(R.string.dialog_switch_node_content, nodeName),
                confirm = context.getString(R.string.dialog_apply_path),
                dismiss = context.getString(R.string.dialog_dismiss)
            )
            if (result == ConfirmResult.Confirmed) {
                applyBypassNode(nodeName)
            }
        }
    }

    // `-tb` for one node: same console as the full scan, so the per-sample trace
    // and the verdict stay readable afterwards.
    fun runSingleNodeTest(nodeName: String) {
        val excess = logs.size - MAX_BYPASS_LOG_LINES
        if (excess > 0) {
            repeat(excess) { logs.removeAt(0) }
        }
        if (logs.isNotEmpty()) {
            logs.add(ShellOutput("", true))
        }
        val firstNewLog = logs.size

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
            Shell.cmd("$binaryPath -tb \"$nodeName\" 2>&1").to(callbackList).submit {
                isRunning = false
                hasRunDiagnosis = true

                val successRegex = "Node is compatible:\\s*(\\S+)".toRegex()
                // Only this run: an older success line must not be applied again.
                val cleanLogs = logs.drop(firstNewLog)
                    .map { it.text.replace("\u001B\\[[;\\d]*m".toRegex(), "") }
                val passedNode = cleanLogs.firstNotNullOfOrNull { successRegex.find(it)?.groupValues?.get(1) }

                refreshBypassData { }

                if (passedNode != null) {
                    scope.launch {
                        val dialogResult = confirmDialogHandle.awaitConfirm(
                            title = context.getString(R.string.dialog_diagnosis_complete_title),
                            content = context.getString(R.string.dialog_diagnosis_complete_content, passedNode),
                            confirm = context.getString(R.string.dialog_apply),
                            dismiss = context.getString(R.string.dialog_dismiss)
                        )
                        if (dialogResult == ConfirmResult.Confirmed) {
                            applyBypassNode(passedNode)
                        }
                    }
                }
            }
        }
    }

    fun runCompatibilityCheck() {
        // Keep the previous run on screen: a scan takes minutes and the result
        // must stay readable after it finishes. Trim only to bound memory.
        val excess = logs.size - MAX_BYPASS_LOG_LINES
        if (excess > 0) {
            repeat(excess) { logs.removeAt(0) }
        }
        if (logs.isNotEmpty()) {
            logs.add(ShellOutput("", true))
        }
        val firstNewLog = logs.size

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
            Shell.cmd("$binaryPath -cbc 2>&1").to(callbackList).submit { result ->
                isRunning = false
                hasRunDiagnosis = true
    
                val successRegex = "Found working node:\\s*(\\S+)".toRegex()
                // Only this run: an older success line must not be applied again.
                val cleanLogs = logs.drop(firstNewLog)
                    .map { it.text.replace("\u001B\\[[;\\d]*m".toRegex(), "") }
                val successNode = cleanLogs.firstNotNullOfOrNull { successRegex.find(it)?.groupValues?.get(1) }
    
                refreshBypassData { }
    
                if (successNode != null) {
                    scope.launch {
                        val dialogResult = confirmDialogHandle.awaitConfirm(
                            title = context.getString(R.string.dialog_diagnosis_complete_title),
                            content = context.getString(R.string.dialog_diagnosis_complete_content, successNode),
                            confirm = context.getString(R.string.dialog_apply),
                            dismiss = context.getString(R.string.dialog_dismiss)
                        )
                        if (dialogResult == ConfirmResult.Confirmed) {
                            applyBypassNode(successNode)
                        }
                    }
                }
            }
        }
    }

    ConfirmDialogHost(handle = confirmDialogHandle)

    RootAppDialog {
        val node = sheetNode
        AZenithSheet(
            show = node != null,
            onDismiss = { sheetNode = null },
            title = node?.first ?: "",
            subtitle = node?.second,
            style = SheetRowStyle.Action,
            items = listOf(
                SheetItem(
                    label = stringResource(R.string.str_switch_node),
                    summary = stringResource(R.string.str_switch_node_desc),
                    icon = Icons.Rounded.SwapHoriz,
                ),
                SheetItem(
                    label = stringResource(R.string.str_test_this_node),
                    summary = if (isChargerConnected) stringResource(R.string.str_test_this_node_desc)
                              else stringResource(R.string.str_plug_in_charger),
                    icon = Icons.Rounded.PlayArrow,
                    enabled = isChargerConnected && !isRunning,
                ),
            ),
            onItemClick = { index ->
                val target = sheetNode
                if (target != null) {
                    // The sheet hides itself before this runs; the dialog it opens
                    // then owns the screen.
                    if (index == 0) confirmSwitchNode(target.first) else runSingleNodeTest(target.first)
                }
            },
        )
    }


    // Confirmation shared by the portrait button and the landscape one.
    val startCompatibilityCheck: () -> Unit = {
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
    }

    Scaffold(
        modifier = Modifier,
        topBar = { 
            BypassChgCheckTopAppBar(
                onBack = { navController.safePopBackStack() },
            scrollBehavior = scrollBehavior
            ) 
        },
        contentColor = ScaffoldContentColor(),
        containerColor = ScaffoldContainerColor(colorScheme.surface)
    ) { innerPadding ->
        val topPad = innerPadding.calculateTopPadding()
        val bottomPad = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        if (isLandscape) {
            // Landscape keeps the console open on the left -- empty until a run
            // starts -- and puts every control on the right, so the trace and the
            // actions can be read side by side.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BypassConsole(
                    logs = logs,
                    logScrollState = logScrollState,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .padding(top = topPad, bottom = bottomPad)
                        .hazePageSource(),
                    showClose = false
                )

                LazyColumn(
                    state = landscapeRightListState,
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .hazePageSource()
                        .gamepadScreenEntry(scopeKey = "subscreen:bypass_check"),
                    contentPadding = PaddingValues(
                        top = topPad,
                        bottom = bottomPad
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    bypassStatusItems(activePath = activePath)
                    bypassDiagnosticsItems(
                        showConsole = false,
                        logs = logs,
                        logScrollState = logScrollState,
                        isConsoleClosed = isConsoleClosed,
                        onCloseConsole = { isConsoleClosed = true },
                        isChargerConnected = isChargerConnected,
                        isRunning = isRunning,
                        hasRunDiagnosis = hasRunDiagnosis,
                        blockParentScroll = blockParentScroll,
                        onStart = startCompatibilityCheck
                    )
                    bypassNodesItems(
                        activePath = activePath,
                        isLoadingPaths = isLoadingPaths,
                        availablePaths = availablePaths,
                        isRunning = isRunning,
                        onNodeClick = { sheetNode = it }
                    )
                }
            }
        } else {
            LazyColumn(
                state = portraitListState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazePageSource()
                    .gamepadScreenEntry(scopeKey = "subscreen:bypass_check"),
                contentPadding = PaddingValues(
                    top = topPad,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = bottomPad
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                bypassStatusItems(activePath = activePath)
                bypassDiagnosticsItems(
                    showConsole = true,
                    logs = logs,
                    logScrollState = logScrollState,
                    isConsoleClosed = isConsoleClosed,
                    onCloseConsole = { isConsoleClosed = true },
                    isChargerConnected = isChargerConnected,
                    isRunning = isRunning,
                    hasRunDiagnosis = hasRunDiagnosis,
                    blockParentScroll = blockParentScroll,
                    onStart = startCompatibilityCheck
                )
                bypassNodesItems(
                    activePath = activePath,
                    isLoadingPaths = isLoadingPaths,
                    availablePaths = availablePaths,
                    isRunning = isRunning,
                    onNodeClick = { sheetNode = it }
                )
            }
        }
    }
}

/** The active node card at the top of the screen. */
private fun LazyListScope.bypassStatusItems(activePath: String) {
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
                            containerColor = if (isUnsupported) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = if (isUnsupported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ) 
                    }
                )
            }
        )
    }
}

/** The diagnostics card, with the console below it in portrait. */
private fun LazyListScope.bypassDiagnosticsItems(
    showConsole: Boolean,
    logs: List<ShellOutput>,
    logScrollState: ScrollState,
    isConsoleClosed: Boolean,
    onCloseConsole: () -> Unit,
    isChargerConnected: Boolean,
    isRunning: Boolean,
    hasRunDiagnosis: Boolean,
    blockParentScroll: NestedScrollConnection,
    onStart: () -> Unit
) {
    item {
        Column {
            BypassCheckTitle(text = stringResource(R.string.section_diagnostics))

            Surface(
                shape = azShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
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
                                    color = if (!connected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStart,
                        enabled = isChargerConnected && !isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = azShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.str_launch_compatibility_check))
                    }
                }
            }

            if (showConsole) {
                AnimatedVisibility(
                    visible = logs.isNotEmpty() && !isConsoleClosed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {

                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        BypassConsole(
                            logs = logs,
                            logScrollState = logScrollState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .nestedScroll(blockParentScroll),
                            showClose = true,
                            onClose = onCloseConsole
                        )
                    }
                }
            }
        }
    }
}

/** The list of detected charge nodes. */
private fun LazyListScope.bypassNodesItems(
    activePath: String,
    isLoadingPaths: Boolean,
    availablePaths: List<Pair<String, String>>,
    isRunning: Boolean,
    onNodeClick: (Pair<String, String>) -> Unit
) {
    item { 
        BypassCheckTitle(text = stringResource(R.string.str_available_nodes, availablePaths.size))

        if (isLoadingPaths) {
            SkeletonSettingsList(rowCount = 2)
        } else if (availablePaths.isEmpty()) {
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
                        val colorScheme = MaterialTheme.colorScheme
                        val isSelected = activePath == pathNode.first



                        val textScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
                            label = "textScaleAnim"
                        )

                        ExpressiveListItemHighlight(
                            containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                            onClick = {
                                // Tapping opens the action sheet; switching is
                                // only ever done from an explicit choice there.
                                if (!isRunning) {
                                    onNodeClick(pathNode)
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

/**
 * The console card. Landscape pins it open in its own pane, so [showClose] is
 * false there and the close button is left out.
 */
@Composable
private fun BypassConsole(
    logs: List<ShellOutput>,
    logScrollState: ScrollState,
    modifier: Modifier = Modifier,
    showClose: Boolean = true,
    onClose: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = azShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .padding(top = if (showClose) 28.dp else 0.dp)
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

            if (showClose) {
                IconButton(
                    onClick = onClose,
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
fun BypassChgCheckTopAppBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .gamepadNotFocusable()
            .appBarFade(colorScheme.surface, scrollBehavior.state.overlappedFraction)
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.CompatibilityCheck),
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.focusProperties { canFocus = false }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },       
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
    }
}
