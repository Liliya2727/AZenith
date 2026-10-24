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
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Overlay Service hosting the Material 3 Expressive Game Space Sidepanel, Detached Left Rail,
 * Right Widget Drawer, Floating Monitor, Crosshair Reticle, and Floating App Window.
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
    private var crosshairView: ComposeView? = null
    private var hudView: ComposeView? = null
    private var isViewAttached = false
    private var monitorJob: Job? = null

    private lateinit var prefs: SharedPreferences

    private val panelState = ZXSidePanelStateHolder()
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
                AZenithTheme(darkOverride = true) {
                    SidePanelRoot(
                        state = panelState,
                        isLeft = sideSetting == "left",
                        isHandleVisible = isHandleVisible,
                        isExpanded = isPanelExpanded,
                        onExpandRequest = { expandPanel() },
                        onCollapseRequest = { collapsePanel() },
                        onVerticalDragDelta = { delta ->
                            val screenH = resources.displayMetrics.heightPixels
                            val newY = (currentVerticalY + delta).roundToInt().coerceIn(60, screenH - 140)
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

        // Non-touchable in-game crosshair overlay window when sidepanel is collapsed
        val chParams = WindowManager.LayoutParams(
            (120 * resources.displayMetrics.density).roundToInt(),
            (120 * resources.displayMetrics.density).roundToInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        val chView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)
            setContent {
                AZenithTheme(darkOverride = true) {
                    if (isHandleVisible && !isPanelExpanded && panelState.crosshair.enabled) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CrosshairReticleCanvas(crosshair = panelState.crosshair)
                        }
                    }
                }
            }
        }

        // Draggable Floating Monitor HUD window when sidepanel is collapsed
        val hudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (16 * resources.displayMetrics.density).roundToInt()
        }
        val hView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)
            setContent {
                AZenithTheme(darkOverride = true) {
                    if (isHandleVisible && !isPanelExpanded && panelState.floatingHudVisible) {
                        FloatingMonitorPill(
                            fps = panelState.currentFps,
                            cpuLoad = panelState.cpuLoad,
                            gpuLoad = panelState.gpuLoad,
                            onDragDelta = { dx, dy ->
                                panelState.hudOffsetX += dx
                                panelState.hudOffsetY += dy
                                hudParams.x = panelState.hudOffsetX.roundToInt()
                                hudParams.y = ((16 * resources.displayMetrics.density) + panelState.hudOffsetY).roundToInt()
                                runCatching { wm.updateViewLayout(this@apply, hudParams) }
                            }
                        )
                    }
                }
            }
        }

        try {
            wm.addView(chView, chParams)
            wm.addView(hView, hudParams)
            wm.addView(view, handleParams)
            crosshairView = chView
            hudView = hView
            composeView = view
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detachOverlay() {
        if (!isViewAttached) return
        try {
            crosshairView?.let { windowManager?.removeView(it) }
            hudView?.let { windowManager?.removeView(it) }
            composeView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            crosshairView = null
            hudView = null
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
            delay(280)
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
        val targetWidth = (40 * density).roundToInt()
        val targetHeight = (136 * density).roundToInt()

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
                                    if (lastKnownGamePkg.isNotEmpty() && isHandleVisible) {
                                        withContext(Dispatchers.Main) {
                                            isHandleVisible = false
                                            collapsePanel()
                                        }
                                    }
                                }
                            } else {
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
 * Root Composable orchestrating the Material 3 Game Space Sidepanel, Detached Left Rail,
 * Right Widget Drawer, Floating Monitor, Crosshair Overlay, Floating App Window, and Snackbar.
 */
@Composable
fun SidePanelRoot(
    state: ZXSidePanelStateHolder,
    isLeft: Boolean,
    isHandleVisible: Boolean,
    isExpanded: Boolean,
    onExpandRequest: () -> Unit,
    onCollapseRequest: () -> Unit,
    onVerticalDragDelta: (Float) -> Unit,
    onVerticalDragFinish: () -> Unit
) {
    val motion = MaterialTheme.motionScheme
    var clockTime by remember { mutableStateOf("04:05") }

    // Live telemetry simulation (1100ms interval matching gamespace_sidebar.html) + clock update
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (isActive) {
            clockTime = formatter.format(Date())
            state.simulateLiveMetrics()
            delay(1100L)
        }
    }

    // Auto-hide toast after 2200ms
    LaunchedEffect(state.toastSequence) {
        if (state.toastVisible) {
            delay(2200L)
            state.toastVisible = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Outside-tap dismiss layer when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = motion.defaultEffectsSpec()),
            exit = fadeOut(animationSpec = motion.defaultEffectsSpec())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapseRequest
                    )
            )
        }

        // 2. In-game Crosshair Overlay (#crosshairOverlay) when expanded
        AnimatedVisibility(
            visible = isExpanded && state.crosshair.enabled,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CrosshairReticleCanvas(crosshair = state.crosshair)
        }

        // 3. Floating Monitor (#floatingMonitor) when expanded
        AnimatedVisibility(
            visible = isExpanded && state.floatingHudVisible,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .offset {
                    IntOffset(
                        state.hudOffsetX.roundToInt(),
                        state.hudOffsetY.roundToInt()
                    )
                }
        ) {
            FloatingMonitorPill(
                fps = state.currentFps,
                cpuLoad = state.cpuLoad,
                gpuLoad = state.gpuLoad,
                onDragDelta = { dx, dy ->
                    val maxX = screenWidthPx * 0.45f
                    val maxY = screenHeightPx * 0.85f
                    state.hudOffsetX = (state.hudOffsetX + dx).coerceIn(-maxX, maxX)
                    state.hudOffsetY = (state.hudOffsetY + dy).coerceIn(-8f, maxY)
                }
            )
        }

        // 4. Swipe Edge Trigger (#swipeEdgeArea) when collapsed
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
            SwipeEdgeHandle(
                isLeft = isLeft,
                onExpand = onExpandRequest,
                onVerticalDrag = onVerticalDragDelta,
                onVerticalDragFinish = onVerticalDragFinish
            )
        }

        // 5. Sidepanel Root (#sidepanelRoot: Detached Left Rail + Main Sidepanel + Right Widget Drawer)
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> if (isLeft) -fullWidth else fullWidth },
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = motion.defaultEffectsSpec()),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> if (isLeft) -fullWidth else fullWidth },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = motion.defaultEffectsSpec()),
            modifier = Modifier
                .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(
                    start = if (isLeft) 16.dp else 0.dp,
                    end = if (!isLeft) 16.dp else 0.dp
                )
        ) {
            SidepanelWorkspaceRow(
                state = state,
                clockTime = clockTime,
                isLeft = isLeft
            )
        }

        // 6. Floating App Window (#floatingAppWindow - Mock WhatsApp UI)
        val activeFloatingApp = remember(state.floatingAppId) {
            state.allApps.find { it.id == state.floatingAppId }
        }
        AnimatedVisibility(
            visible = isExpanded && activeFloatingApp != null,
            enter = scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + fadeIn(tween(200)),
            exit = scaleOut(
                targetScale = 0.88f,
                animationSpec = tween(180)
            ) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val baseX = (screenWidthPx * 0.15f).roundToInt()
                    val baseY = (screenHeightPx * 0.20f).roundToInt()
                    IntOffset(
                        baseX + state.appWindowOffsetX.roundToInt(),
                        baseY + state.appWindowOffsetY.roundToInt()
                    )
                }
        ) {
            FloatingAppWindow(
                appName = activeFloatingApp?.name ?: "WhatsApp",
                onClose = { state.closeFloatingAppWindow() },
                onDragDelta = { dx, dy ->
                    state.appWindowOffsetX += dx
                    state.appWindowOffsetY += dy
                }
            )
        }

        // 7. Snackbar (#m3Toast)
        M3ToastSnackbar(
            visible = isExpanded && state.toastVisible,
            message = state.toastMessage,
            iconName = state.toastIcon,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

/**
 * Swipe Edge Trigger (#swipeEdgeArea) matching `w-1.5 h-32 rounded-r-full md-surface-high`
 * that expands to `w-2.5` (`10.dp`) and turns `primary` (`#d0bcff`) on active touch/drag.
 */
@Composable
private fun SwipeEdgeHandle(
    isLeft: Boolean,
    onExpand: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var isPressed by remember { mutableStateOf(false) }
    var accumulatedHorizontalDrag by remember { mutableFloatStateOf(0f) }

    val barWidth by animateDpAsState(
        targetValue = if (isPressed) 10.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SwipeEdgeBarWidth"
    )
    val barColor by animateColorAsState(
        targetValue = if (isPressed) scheme.primary else scheme.surfaceContainerHigh.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "SwipeEdgeBarColor"
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
                        val thresholdPx = 24 * density
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
        val pillShape = if (isLeft) {
            RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
        } else {
            RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
        }
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(128.dp)
                .clip(pillShape)
                .background(barColor)
        )
    }
}

@Composable
private fun SidepanelWorkspaceRow(
    state: ZXSidePanelStateHolder,
    clockTime: String,
    isLeft: Boolean
) {
    val feedListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val feedBlocks by remember {
        derivedStateOf { state.buildFeedBlocks() }
    }

    // Automatically sync active category in Left Rail when scrolling main feed
    val firstVisibleIndex by remember {
        derivedStateOf { feedListState.firstVisibleItemIndex }
    }
    LaunchedEffect(firstVisibleIndex, state.editMode, state.activeSubscreen) {
        if (!state.editMode && state.activeSubscreen == null) {
            val block = feedBlocks.getOrNull(firstVisibleIndex)
            if (block != null && block.category != state.activeScrollCategory) {
                state.activeScrollCategory = block.category
            }
        }
    }

    val onScrollToSection: (String) -> Unit = { categoryName ->
        if (state.activeSubscreen != null) {
            state.closeSubscreen()
        }
        state.activeScrollCategory = categoryName
        coroutineScope.launch {
            if (state.editMode) {
                val idx = state.activeWidgets.indexOfFirst { wId ->
                    state.widgetCatalog[wId]?.category == categoryName
                }
                if (idx >= 0) {
                    feedListState.animateScrollToItem(idx)
                }
            } else {
                val idx = feedBlocks.indexOfFirst { it.category == categoryName }
                if (idx >= 0) {
                    feedListState.animateScrollToItem(idx)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxHeight(0.86f)
            .heightIn(max = 740.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLeft) {
            DetachedLeftRail(
                state = state,
                onScrollToSection = onScrollToSection
            )

            MainSidepanelContent(
                state = state,
                clockTime = clockTime,
                feedBlocks = feedBlocks,
                feedListState = feedListState
            )

            AnimatedVisibility(
                visible = state.editMode,
                enter = slideInHorizontally(
                    initialOffsetX = { it / 2 },
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                ) + expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it / 2 },
                    animationSpec = tween(200)
                ) + shrinkHorizontally(
                    animationSpec = tween(200)
                ) + fadeOut(tween(150))
            ) {
                RightWidgetDrawer(state = state)
            }
        } else {
            AnimatedVisibility(
                visible = state.editMode,
                enter = slideInHorizontally(
                    initialOffsetX = { -it / 2 },
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
                ) + expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it / 2 },
                    animationSpec = tween(200)
                ) + shrinkHorizontally(
                    animationSpec = tween(200)
                ) + fadeOut(tween(150))
            ) {
                RightWidgetDrawer(state = state)
            }

            MainSidepanelContent(
                state = state,
                clockTime = clockTime,
                feedBlocks = feedBlocks,
                feedListState = feedListState
            )

            DetachedLeftRail(
                state = state,
                onScrollToSection = onScrollToSection
            )
        }
    }
}

@Composable
private fun DetachedLeftRail(
    state: ZXSidePanelStateHolder,
    onScrollToSection: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val navItems = remember {
        listOf(
            Triple("overview", "Basic", "speed"),
            Triple("quickTools", "Tools", "grid_view"),
            Triple("antiInterference", "DND", "do_not_disturb_on"),
            Triple("addons", "Add-ons", "auto_fix_high")
        )
    }

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = scheme.surfaceContainerLow,
        shadowElevation = 24.dp,
        modifier = Modifier
            .width(84.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Section Scroll Shortcuts
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                navItems.forEach { (catId, label, iconName) ->
                    val isActive = state.activeScrollCategory == catId
                    val pillScale by animateFloatAsState(
                        targetValue = if (isActive) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                        label = "NavRailPillScale"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (isActive) scheme.secondaryContainer else scheme.surfaceContainerLow.copy(alpha = 0f),
                        animationSpec = tween(durationMillis = 250),
                        label = "NavRailPillColor"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (isActive) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 250),
                        label = "NavRailIconColor"
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (isActive) scheme.onSurface else scheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 250),
                        label = "NavRailLabelColor"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onScrollToSection(catId) }
                            )
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(30.dp)
                                .graphicsLayer {
                                    scaleX = pillScale
                                    scaleY = pillScale
                                }
                                .clip(CircleShape)
                                .background(pillColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = ZXSidePanelIcons.get(iconName),
                                contentDescription = label,
                                tint = iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            // Tonal Divider (w-10 h-0.5 rounded-full md-surface-highest my-3)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest)
            )

            // Below Divider: Floating Apps List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(
                        items = state.quickApps,
                        key = { it }
                    ) { appId ->
                        val app = state.allApps.find { it.id == appId } ?: return@items
                        val isOpen = state.floatingAppId == app.id
                        val btnScale by animateFloatAsState(
                            targetValue = if (isOpen) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                            label = "QuickAppScale"
                        )
                        val containerColor by animateColorAsState(
                            targetValue = if (isOpen) scheme.primary else scheme.surfaceContainerHigh,
                            animationSpec = tween(durationMillis = 250),
                            label = "QuickAppContainer"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isOpen) scheme.onPrimary else scheme.onSurface,
                            animationSpec = tween(durationMillis = 250),
                            label = "QuickAppContent"
                        )
                        val shape = RoundedCornerShape(16.dp)

                        Surface(
                            shape = shape,
                            color = containerColor,
                            contentColor = contentColor,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = btnScale
                                    scaleY = btnScale
                                }
                                .clip(shape)
                                .clickable { state.toggleFloatingAppWindow(app.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(app.icon),
                                    contentDescription = app.name,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Add App (+) Button
                    item(key = "add_app_btn") {
                        val shape = RoundedCornerShape(16.dp)
                        Surface(
                            shape = shape,
                            color = scheme.surfaceContainerHigh,
                            contentColor = scheme.onSurface,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer { alpha = 0.7f }
                                .clip(shape)
                                .clickable { state.openSubscreen("appsManager") }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("add"),
                                    contentDescription = "Manage floating apps",
                                    tint = scheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainSidepanelContent(
    state: ZXSidePanelStateHolder,
    clockTime: String,
    feedBlocks: List<SidePanelFeedBlock>,
    feedListState: androidx.compose.foundation.lazy.LazyListState
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = scheme.surfaceContainerLow,
        shadowElevation = 24.dp,
        modifier = Modifier
            .width(360.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header vs Sub-Screen Header
            if (state.activeSubscreen == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("sports_esports"),
                                    contentDescription = null,
                                    tint = scheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Game Space",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("wifi"),
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "82%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                                Text(
                                    text = clockTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Toggle Floating Monitor Button
                        val hudBtnContainer by animateColorAsState(
                            targetValue = if (state.floatingHudVisible) scheme.secondaryContainer else scheme.surfaceContainerHigh,
                            animationSpec = tween(250),
                            label = "HudBtnContainer"
                        )
                        val hudBtnContent by animateColorAsState(
                            targetValue = if (state.floatingHudVisible) scheme.onSecondaryContainer else scheme.onSurface,
                            animationSpec = tween(250),
                            label = "HudBtnContent"
                        )
                        Surface(
                            shape = CircleShape,
                            color = hudBtnContainer,
                            contentColor = hudBtnContent,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { state.toggleFloatingHud() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("monitoring"),
                                    contentDescription = "Toggle floating monitor",
                                    tint = hudBtnContent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Toggle Edit Mode Button
                        val editBtnContainer by animateColorAsState(
                            targetValue = if (state.editMode) scheme.primary else scheme.surfaceContainerHigh,
                            animationSpec = tween(250),
                            label = "EditBtnContainer"
                        )
                        val editBtnContent by animateColorAsState(
                            targetValue = if (state.editMode) scheme.onPrimary else scheme.onSurface,
                            animationSpec = tween(250),
                            label = "EditBtnContent"
                        )
                        Surface(
                            shape = CircleShape,
                            color = editBtnContainer,
                            contentColor = editBtnContent,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { state.toggleEditMode() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get(if (state.editMode) "check" else "edit"),
                                    contentDescription = "Customize and reorder widgets",
                                    tint = editBtnContent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Subscreen Header
                val subscreenTitle = when (state.activeSubscreen) {
                    "crosshair" -> "Crosshair settings"
                    "display" -> "Color Scheme"
                    "appsManager" -> "Manage Apps"
                    else -> "Settings"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { state.closeSubscreen() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("arrow_back"),
                                    contentDescription = "Back to Game Space",
                                    tint = scheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = subscreenTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { state.resetCurrentSubscreen() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = ZXSidePanelIcons.get("restart_alt"),
                                contentDescription = "Reset to default",
                                tint = scheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurface
                            )
                        }
                    }
                }
            }

            // Scrollable Widgets Feed / Subscreens
            AnimatedContent(
                targetState = state.activeSubscreen,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(
                            initialOffsetX = { 48 },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(tween(200))) togetherWith fadeOut(tween(150))
                    } else {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "MainFeedSubscreenTransition"
            ) { currentSubscreen ->
                when (currentSubscreen) {
                    "crosshair" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            CrosshairSubscreen(state = state)
                        }
                    }
                    "display" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            DisplaySubscreen(state = state)
                        }
                    }
                    "appsManager" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            AppsManagerSubscreen(state = state)
                        }
                    }
                    else -> {
                        if (state.activeWidgets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                M3WidgetCard(
                                    contentPadding = PaddingValues(32.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = ZXSidePanelIcons.get("widgets"),
                                            contentDescription = null,
                                            tint = scheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Text(
                                            text = "No widgets on home screen",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = scheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Add widgets from the right panel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = scheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else if (state.editMode) {
                            LazyColumn(
                                state = feedListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = state.activeWidgets,
                                    key = { _, wId -> "edit_$wId" }
                                ) { index, widgetId ->
                                    EditModeWidgetCard(
                                        widgetId = widgetId,
                                        index = index,
                                        totalCount = state.activeWidgets.size,
                                        state = state
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = feedListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = feedBlocks,
                                    key = { block ->
                                        when (block) {
                                            is SidePanelFeedBlock.Single -> block.widgetId
                                            is SidePanelFeedBlock.Merged -> block.groupIds.joinToString(",")
                                        }
                                    }
                                ) { block ->
                                    when (block) {
                                        is SidePanelFeedBlock.Single -> {
                                            SidePanelWidgetContent(
                                                widgetId = block.widgetId,
                                                state = state,
                                                isMerged = false
                                            )
                                        }
                                        is SidePanelFeedBlock.Merged -> {
                                            val isSliders = block.mergeGroup == "sliders"
                                            M3WidgetCard(
                                                contentPadding = if (isSliders) PaddingValues(16.dp) else PaddingValues(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(if (isSliders) 14.dp else 12.dp)
                                            ) {
                                                block.groupIds.forEach { wId ->
                                                    SidePanelWidgetContent(
                                                        widgetId = wId,
                                                        state = state,
                                                        isMerged = true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RightWidgetDrawer(
    state: ZXSidePanelStateHolder
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = scheme.surfaceContainerLow,
        shadowElevation = 24.dp,
        modifier = Modifier
            .width(290.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Drawer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Widget library",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Drag to main panel or tap + to add",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { state.resetDefaultWidgets() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("restart_alt"),
                            contentDescription = "Reset default layout",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Library Widgets Container
            if (state.libraryWidgets.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = scheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("check_circle"),
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "All widgets active",
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Drag any widget here to store it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.libraryWidgets,
                        key = { it }
                    ) { widgetId ->
                        val meta = state.widgetCatalog[widgetId] ?: return@items
                        val cardShape = RoundedCornerShape(24.dp)

                        Surface(
                            shape = cardShape,
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .clickable { state.addWidgetFromLibrary(widgetId) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = scheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = ZXSidePanelIcons.get(meta.icon),
                                                contentDescription = null,
                                                tint = scheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = meta.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = scheme.primary,
                                    contentColor = scheme.onPrimary,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { state.addWidgetFromLibrary(widgetId) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = ZXSidePanelIcons.get("add"),
                                            contentDescription = "Add to Game Space",
                                            tint = scheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
