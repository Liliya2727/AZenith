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

package zx.azenith.ui.gamespace

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import zx.azenith.ui.theme.AZenithTheme
import zx.azenith.ui.util.RootUtils
import kotlin.math.roundToInt

/**
 * Overlay Service hosting the in-game AZenith Side Panel (Game Booster).
 *
 * Provides a resting floating edge handle that respects configured side and vertical
 * position, expanding into a pure Material 3 Expressive blank booster panel upon inward swipe.
 */
class ZXSidePanelService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW = "zx.azenith.action.SIDE_PANEL_SHOW"
        const val ACTION_HIDE = "zx.azenith.action.SIDE_PANEL_HIDE"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"

        private const val APPLIST_PATH = "/data/adb/.config/AZenith/gamelist/azenithApplist.json"
        private const val APP_STATUS_PATH = "/data/adb/.config/AZenith/app_status"

        @Volatile
        var isServiceActive = false
            private set

        fun show(context: Context, packageName: String? = null, appLabel: String? = null) {
            try {
                val intent = Intent(context, ZXSidePanelService::class.java).apply {
                    action = ACTION_SHOW
                    packageName?.let { putExtra(EXTRA_PACKAGE_NAME, it) }
                    appLabel?.let { putExtra(EXTRA_APP_LABEL, it) }
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("ZXSidePanelService", "Failed to start service", e)
            }
        }

        fun hide(context: Context) {
            if (!isServiceActive) return
            try {
                val intent = Intent(context, ZXSidePanelService::class.java).apply {
                    action = ACTION_HIDE
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.e("ZXSidePanelService", "Failed to hide service", e)
            }
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var isViewAttached = false
    private var monitorJob: Job? = null

    private lateinit var prefs: SharedPreferences

    // Reactive UI state
    private var isPanelExpanded by mutableStateOf(false)
    private var isHandleVisible by mutableStateOf(false)
    private var sideSetting by mutableStateOf("left")
    private var activeGamePackage by mutableStateOf<String?>(null)
    private var activeGameLabel by mutableStateOf<String?>(null)
    private var currentVerticalY by mutableIntStateOf(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        sideSetting = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val displayMetrics = resources.displayMetrics
        val posPct = prefs.getInt("gamespace_sidepanel_pos", 50).coerceIn(10, 90)
        currentVerticalY = ((displayMetrics.heightPixels * (posPct / 100f))).roundToInt()

        startAppMonitorWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_SHOW
        when (action) {
            ACTION_SHOW -> {
                val pkg = intent?.getStringExtra(EXTRA_PACKAGE_NAME)
                val label = intent?.getStringExtra(EXTRA_APP_LABEL)
                if (pkg != null) activeGamePackage = pkg
                if (label != null) {
                    activeGameLabel = label
                } else if (pkg != null) {
                    activeGameLabel = resolveAppLabel(pkg)
                }
                sideSetting = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
                attachOverlayIfNeeded()
                isHandleVisible = true
            }
            ACTION_HIDE -> {
                isHandleVisible = false
                isPanelExpanded = false
                detachOverlay()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isServiceActive = false
        monitorJob?.cancel()
        detachOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun attachOverlayIfNeeded() {
        if (isViewAttached) return
        if (!Settings.canDrawOverlays(this)) return

        val wm = windowManager ?: return
        val isLeft = sideSetting == "left"
        val handleParams = createHandleLayoutParams(isLeft, currentVerticalY)

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)

            setContent {
                AZenithTheme {
                    SidePanelRoot(
                        isLeft = sideSetting == "left",
                        isHandleVisible = isHandleVisible,
                        isExpanded = isPanelExpanded,
                        gameLabel = activeGameLabel ?: activeGamePackage ?: "Active Game",
                        onExpandRequest = { expandPanel() },
                        onCollapseRequest = { collapsePanel() },
                        onVerticalDragDelta = { delta ->
                            val screenH = resources.displayMetrics.heightPixels
                            val newY = (currentVerticalY + delta).roundToInt().coerceIn(60, screenH - 120)
                            if (newY != currentVerticalY) {
                                currentVerticalY = newY
                                if (!isPanelExpanded && isViewAttached) {
                                    wm.updateViewLayout(this, createHandleLayoutParams(sideSetting == "left", currentVerticalY))
                                }
                            }
                        },
                        onVerticalDragFinish = {
                            val screenH = resources.displayMetrics.heightPixels.coerceAtLeast(1)
                            val pct = ((currentVerticalY.toFloat() / screenH) * 100).roundToInt().coerceIn(10, 90)
                            prefs.edit().putInt("gamespace_sidepanel_pos", pct).apply()
                        }
                    )
                }
            }
        }

        try {
            wm.addView(view, handleParams)
            composeView = view
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detachOverlay() {
        if (!isViewAttached) return
        try {
            composeView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            composeView = null
            isViewAttached = false
        }
    }

    private fun expandPanel() {
        if (isPanelExpanded) return
        val view = composeView ?: return
        val wm = windowManager ?: return
        try {
            wm.updateViewLayout(view, createExpandedLayoutParams())
            isPanelExpanded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun collapsePanel() {
        if (!isPanelExpanded) return
        isPanelExpanded = false
        serviceScope.launch {
            delay(240)
            if (!isPanelExpanded && isViewAttached) {
                val view = composeView ?: return@launch
                val wm = windowManager ?: return@launch
                try {
                    wm.updateViewLayout(view, createHandleLayoutParams(sideSetting == "left", currentVerticalY))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createHandleLayoutParams(isLeft: Boolean, yPx: Int): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val targetWidth = (28 * density).roundToInt()
        val targetHeight = (96 * density).roundToInt()

        return WindowManager.LayoutParams(
            targetWidth,
            targetHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or (if (isLeft) Gravity.START else Gravity.END)
            y = yPx
            x = 0
        }
    }

    private fun createExpandedLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
        }
    }

    private fun startAppMonitorWatcher() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch(Dispatchers.IO) {
            var lastStatusRaw = ""
            var lastKnownGamePkg = ""
            while (isActive) {
                try {
                    val statusText = RootUtils.readRootFile(APP_STATUS_PATH)
                    if (statusText != null && statusText != lastStatusRaw) {
                        lastStatusRaw = statusText
                        val focusedLine = statusText.lineSequence().firstOrNull { it.startsWith("focused_app ") }
                        val pkg = focusedLine?.substringAfter("focused_app ")?.substringBefore(" ")?.trim()

                        if (!pkg.isNullOrBlank() && pkg != "unknown" && pkg != "none") {
                            if (!isLauncherPackage(pkg)) {
                                val isGame = isRecognizedGame(pkg)
                                if (isGame) {
                                    lastKnownGamePkg = pkg
                                    val label = resolveAppLabel(pkg)
                                    withContext(Dispatchers.Main) {
                                        sideSetting = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
                                        activeGamePackage = pkg
                                        activeGameLabel = label
                                        attachOverlayIfNeeded()
                                        isHandleVisible = true
                                    }
                                } else if (pkg != packageName) {
                                    // User switched from a game to an ordinary app
                                    if (lastKnownGamePkg.isNotEmpty() && isHandleVisible) {
                                        withContext(Dispatchers.Main) {
                                            isHandleVisible = false
                                            collapsePanel()
                                        }
                                    }
                                }
                            } else {
                                // Returned to launcher
                                if (isHandleVisible) {
                                    withContext(Dispatchers.Main) {
                                        isHandleVisible = false
                                        collapsePanel()
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                delay(1200)
            }
        }
    }

    private fun isLauncherPackage(pkg: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolve = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolve?.activityInfo?.packageName == pkg
        } catch (_: Exception) {
            false
        }
    }

    private fun isRecognizedGame(pkg: String): Boolean {
        // 1. Check AZenith configured gamelist
        try {
            val file = SuFile(APPLIST_PATH)
            if (file.exists()) {
                val content = SuFileInputStream.open(file).bufferedReader().use { it.readText() }
                if (content.isNotBlank() && JSONObject(content).has(pkg)) {
                    return true
                }
            }
        } catch (_: Exception) {
        }

        // 2. Fallback to package manager CATEGORY_GAME
        return try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            @Suppress("DEPRECATION")
            (appInfo.category == ApplicationInfo.CATEGORY_GAME || (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0)
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveAppLabel(pkg: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            info.loadLabel(pm).toString()
        } catch (_: Exception) {
            pkg
        }
    }
}

/**
 * Root Composable orchestrating the resting floating edge handle and expanded booster sheet.
 */
@Composable
private fun SidePanelRoot(
    isLeft: Boolean,
    isHandleVisible: Boolean,
    isExpanded: Boolean,
    gameLabel: String,
    onExpandRequest: () -> Unit,
    onCollapseRequest: () -> Unit,
    onVerticalDragDelta: (Float) -> Unit,
    onVerticalDragFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        // 1. Scrim overlay for expanded booster panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = motion.defaultEffectsSpec()),
            exit = fadeOut(animationSpec = motion.defaultEffectsSpec())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.scrim.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapseRequest
                    )
            )
        }

        // 2. Resting Floating Edge Handle (The "Bar")
        AnimatedVisibility(
            visible = isHandleVisible && !isExpanded,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> if (isLeft) -fullWidth else fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> if (isLeft) -fullWidth else fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        ) {
            FloatingHandle(
                isLeft = isLeft,
                onExpand = onExpandRequest,
                onVerticalDrag = onVerticalDragDelta,
                onVerticalDragFinish = onVerticalDragFinish
            )
        }

        // 3. Expanded Blank Material 3 Expressive Booster Panel Sheet
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> if (isLeft) -fullWidth else fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> if (isLeft) -fullWidth else fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec())
        ) {
            BlankBoosterPanelSheet(
                isLeft = isLeft,
                gameLabel = gameLabel,
                onClose = onCollapseRequest
            )
        }
    }
}

/**
 * Material 3 Expressive Floating Edge Handle pinned to Left or Right side.
 */
@Composable
private fun FloatingHandle(
    isLeft: Boolean,
    onExpand: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var isPressed by remember { mutableStateOf(false) }
    var accumulatedHorizontalDrag by remember { mutableFloatStateOf(0f) }

    val handleWidth by animateDpAsState(
        targetValue = if (isPressed) 7.dp else 5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "HandleWidth"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isPressed = true },
                    onDragEnd = {
                        isPressed = false
                        onVerticalDragFinish()
                    },
                    onDragCancel = {
                        isPressed = false
                        onVerticalDragFinish()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        onVerticalDrag(dragAmount)
                    }
                )
            }
            .pointerInput(isLeft) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isPressed = true
                        accumulatedHorizontalDrag = 0f
                    },
                    onDragEnd = {
                        isPressed = false
                        accumulatedHorizontalDrag = 0f
                    },
                    onDragCancel = {
                        isPressed = false
                        accumulatedHorizontalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedHorizontalDrag += dragAmount
                        // Inward swipe detection
                        val thresholdPx = 28 * density
                        val inward = if (isLeft) {
                            accumulatedHorizontalDrag > thresholdPx
                        } else {
                            accumulatedHorizontalDrag < -thresholdPx
                        }
                        if (inward) {
                            onExpand()
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand
            ),
        contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        // Aesthetic tactile pill resting exactly on the edge
        Surface(
            shape = if (isLeft) {
                RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            },
            color = scheme.primary,
            modifier = Modifier
                .width(handleWidth)
                .height(48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(scheme.onPrimary.copy(alpha = 0.85f))
                )
            }
        }
    }
}

/**
 * Pure Material 3 Expressive Blank Booster Panel Sheet.
 * Displays a clean booster header and placeholder surface, with strictly NO glassmorphism and NO widget outlines.
 */
@Composable
private fun BlankBoosterPanelSheet(
    isLeft: Boolean,
    gameLabel: String,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sheetShape = if (isLeft) {
        RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    } else {
        RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    }

    Surface(
        shape = sheetShape,
        color = scheme.surfaceContainerLow,
        modifier = Modifier
            .width(340.dp)
            .fillMaxHeight()
            .padding(vertical = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.RocketLaunch,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AZenith Booster",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Running: $gameLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Material 3 Expressive Blank Booster Workspace Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHigh,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Game Booster Active",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onSurface
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Real-time FPS overlay, performance tuning, and game tools will appear in this workspace.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(Modifier.height(18.dp))

                    // Performance Status Pill
                    Surface(
                        shape = CircleShape,
                        color = scheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PERFORMANCE ENGINE ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                color = scheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
