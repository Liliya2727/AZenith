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
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
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
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.systemGestureExclusion
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Overlay Service hosting the Material 3 Expressive Game Space Sidepanel, Detached Left Rail,
 * Right Widget Drawer, Floating Monitor, and Crosshair Reticle.
 */
class ZXSidePanelService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW = "zx.azenith.action.SIDE_PANEL_SHOW"
        const val ACTION_HIDE = "zx.azenith.action.SIDE_PANEL_HIDE"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"

        private const val APPLIST_PATH = "/data/adb/.config/AZenith/gamelist/azenithApplist.json"
        private const val APP_STATUS_PATH = "/data/adb/.config/AZenith/app_status"
        private const val PREF_QUICK_APPS = "gamespace_sidepanel_quick_apps"

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
    private var rootOverlayView: View? = null
    private var crosshairView: ComposeView? = null
    private var hudView: View? = null
    private var isViewAttached = false
    private var monitorJob: Job? = null
    private var windowTransitionJob: Job? = null

    private lateinit var prefs: SharedPreferences

    private val panelState = ZXSidePanelStateHolder()
    private var isPanelExpanded by mutableStateOf(false)
    private var isWindowFullScreen by mutableStateOf(false)
    private var isHandleVisible by mutableStateOf(false)
    private var isHandlePressed by mutableStateOf(false)
    private var sideSetting by mutableStateOf("left")
    private var barSensitivity by mutableIntStateOf(65)
    private var barLengthPref by mutableIntStateOf(50)
    private var activeGamePackage by mutableStateOf<String?>(null)
    private var activeGameLabel by mutableStateOf<String?>(null)
    private var currentVerticalY by mutableIntStateOf(0)

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "gamespace_sidepanel_side" -> {
                sideSetting = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
                updateHandleWindowIfCollapsed()
            }
            "gamespace_sidepanel_sensitivity" -> {
                barSensitivity = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
            }
            "gamespace_sidepanel_length" -> {
                barLengthPref = prefs.getInt("gamespace_sidepanel_length", 50).coerceIn(20, 100)
                updateHandleWindowIfCollapsed()
            }
        }
    }

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
        barSensitivity = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
        barLengthPref = prefs.getInt("gamespace_sidepanel_length", 50).coerceIn(20, 100)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val displayMetrics = resources.displayMetrics
        val posPct = prefs.getInt("gamespace_sidepanel_pos", 50).coerceIn(10, 90)
        currentVerticalY = ((displayMetrics.heightPixels * (posPct / 100f))).roundToInt()

        panelState.onQuickAppsChanged = { list ->
            prefs.edit().putString(PREF_QUICK_APPS, list.joinToString(",")).apply()
        }
        panelState.onLaunchAppRequest = { pkg ->
            launchExternalApp(pkg)
        }

        loadInstalledLauncherApps()
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
                barSensitivity = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
                barLengthPref = prefs.getInt("gamespace_sidepanel_length", 50).coerceIn(20, 100)
                attachOverlayIfNeeded()
                isHandleVisible = true
            }
            ACTION_HIDE -> {
                isHandleVisible = false
                isPanelExpanded = false
                isWindowFullScreen = false
                detachOverlay()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isServiceActive = false
        runCatching { prefs.unregisterOnSharedPreferenceChangeListener(prefListener) }
        monitorJob?.cancel()
        windowTransitionJob?.cancel()
        detachOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun computeHandleBarHeightDp(): Float {
        val dm = resources.displayMetrics
        val minScreenDp = min(dm.widthPixels, dm.heightPixels) / dm.density
        val baseDp = (minScreenDp * 0.145f).coerceIn(40f, 76f)
        val scale = 0.55f + (barLengthPref.coerceIn(20, 100) / 100f) * 0.90f
        return (baseDp * scale).coerceIn(32f, 112f)
    }

    private fun updateHandleWindowIfCollapsed() {
        if (!isViewAttached || isWindowFullScreen) return
        val view = rootOverlayView ?: return
        val wm = windowManager ?: return
        runCatching {
            wm.updateViewLayout(view, createHandleLayoutParams(sideSetting == "left", currentVerticalY))
        }
    }

    private fun launchExternalApp(pkg: String) {
        val appLabel = panelState.installedApps.find { it.packageName == pkg }?.name ?: pkg
        collapsePanel()
        serviceScope.launch {
            delay(120)
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    panelState.showToast("Cannot launch $appLabel", "close")
                }
            } catch (_: Exception) {
                panelState.showToast("Failed to launch $appLabel", "close")
            }
        }
    }

    private fun loadInstalledLauncherApps() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val pm = packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                val seenPackages = HashSet<String>()
                val loaded = ArrayList<SidePanelAppItem>()

                for (info in resolveInfos) {
                    val pkg = info.activityInfo?.packageName ?: continue
                    if (pkg == packageName || !seenPackages.add(pkg)) continue
                    val label = runCatching { info.loadLabel(pm).toString().trim() }
                        .getOrDefault(pkg)
                        .ifEmpty { pkg }
                    val iconBmp = runCatching {
                        drawableToImageBitmap(info.loadIcon(pm), 96)
                    }.getOrNull()
                    loaded.add(
                        SidePanelAppItem(
                            packageName = pkg,
                            name = label,
                            iconBitmap = iconBmp
                        )
                    )
                }

                loaded.sortBy { it.name.lowercase(Locale.getDefault()) }

                val preferredKeywords = listOf(
                    "whatsapp", "discord", "telegram", "chrome", "browser",
                    "youtube", "spotify", "music", "files", "calculator"
                )
                val defaults = loaded
                    .filter { app ->
                        preferredKeywords.any { kw ->
                            app.packageName.contains(kw, ignoreCase = true) ||
                                app.name.contains(kw, ignoreCase = true)
                        }
                    }
                    .take(3)
                    .map { it.packageName }
                    .ifEmpty { loaded.take(2).map { it.packageName } }

                val savedRaw = prefs.getString(PREF_QUICK_APPS, null)
                val validPkgs = loaded.map { it.packageName }.toSet()
                val initialQuick = if (savedRaw == null) {
                    defaults
                } else {
                    savedRaw.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && validPkgs.contains(it) }
                }

                withContext(Dispatchers.Main) {
                    panelState.installedApps.clear()
                    panelState.installedApps.addAll(loaded)
                    panelState.defaultQuickApps = defaults
                    panelState.quickApps.clear()
                    panelState.quickApps.addAll(initialQuick)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable, sizePx: Int): androidx.compose.ui.graphics.ImageBitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bmp.asImageBitmap()
    }

    private fun attachOverlayIfNeeded() {
        if (isViewAttached) return
        if (!Settings.canDrawOverlays(this)) return

        val wm = windowManager ?: return
        val isLeft = sideSetting == "left"
        val handleParams = createHandleLayoutParams(isLeft, currentVerticalY)

        val panelComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)

            setContent {
                AZenithTheme(darkOverride = true) {
                    val barHeightDp = computeHandleBarHeightDp()
                    SidePanelRoot(
                        state = panelState,
                        isLeft = sideSetting == "left",
                        isHandleVisible = isHandleVisible,
                        isHandlePressed = isHandlePressed,
                        isExpanded = isPanelExpanded,
                        isWindowFullScreen = isWindowFullScreen,
                        barHeightDp = barHeightDp,
                        currentVerticalY = currentVerticalY,
                        onCollapseRequest = { collapsePanel() },
                        onHudVisibilityChanged = { visible ->
                            hudView?.visibility = if (visible) View.VISIBLE else View.GONE
                        },
                        onCrosshairVisibilityChanged = { visible ->
                            crosshairView?.visibility = if (visible) View.VISIBLE else View.GONE
                        }
                    )
                }
            }
        }

        // Wrapper FrameLayout that enforces system gesture exclusion on the edge handle bar
        // and processes screen-coordinate (rawX/rawY) swipe-in & vertical-reposition gestures
        val mainContainer = object : FrameLayout(this) {
            private var downRawX = 0f
            private var downRawY = 0f
            private var startVertY = 0
            private var gestureMode = 0 // 0 = undecided, 1 = triggered expand, 2 = vertical drag

            override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
                super.onLayout(changed, left, top, right, bottom)
                updateGestureExclusion()
            }

            fun updateGestureExclusion() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (!isWindowFullScreen && width > 0 && height > 0) {
                        systemGestureExclusionRects = listOf(
                            android.graphics.Rect(0, 0, width, height)
                        )
                    } else {
                        systemGestureExclusionRects = emptyList()
                    }
                }
            }

            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                // When collapsed into the edge bar, handle touch directly in screen coordinates
                // so inward swipes immediately open the sidepanel without triggering system Back or notifications.
                if (!isWindowFullScreen && !isPanelExpanded) {
                    val dm = resources.displayMetrics
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downRawX = ev.rawX
                            downRawY = ev.rawY
                            startVertY = currentVerticalY
                            gestureMode = 0
                            isHandlePressed = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dxInward = if (sideSetting == "left") {
                                ev.rawX - downRawX
                            } else {
                                downRawX - ev.rawX
                            }
                            val dy = ev.rawY - downRawY
                            val triggerDp = (20f - (barSensitivity / 100f) * 14f).coerceIn(5f, 18f)
                            val triggerPx = triggerDp * dm.density

                            if (gestureMode == 0) {
                                if (dxInward >= triggerPx && dxInward >= abs(dy) * 0.45f) {
                                    gestureMode = 1
                                    isHandlePressed = false
                                    expandPanel()
                                    return true
                                } else if (abs(dy) >= 10f * dm.density && abs(dy) > dxInward * 1.35f) {
                                    gestureMode = 2
                                }
                            }

                            if (gestureMode == 2) {
                                val barPx = (computeHandleBarHeightDp() * dm.density).roundToInt()
                                val screenH = dm.heightPixels
                                val newY = (startVertY + dy).roundToInt()
                                    .coerceIn(36, (screenH - barPx - 36).coerceAtLeast(36))
                                if (newY != currentVerticalY) {
                                    currentVerticalY = newY
                                    if (isViewAttached && !isWindowFullScreen) {
                                        runCatching {
                                            wm.updateViewLayout(
                                                this,
                                                createHandleLayoutParams(sideSetting == "left", currentVerticalY)
                                            )
                                        }
                                    }
                                }
                            }
                            return true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isHandlePressed = false
                            if (gestureMode == 2) {
                                val screenH = dm.heightPixels.coerceAtLeast(1)
                                val pct = ((currentVerticalY.toFloat() / screenH) * 100)
                                    .roundToInt()
                                    .coerceIn(10, 90)
                                prefs.edit().putInt("gamespace_sidepanel_pos", pct).apply()
                            } else if (gestureMode == 0 && ev.actionMasked == MotionEvent.ACTION_UP) {
                                val totalDist = abs(ev.rawX - downRawX) + abs(ev.rawY - downRawY)
                                if (totalDist < 12f * dm.density) {
                                    expandPanel()
                                }
                            }
                            gestureMode = 0
                            return true
                        }
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)
            addView(
                panelComposeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        // Single non-touchable Crosshair overlay window (persists whether sidepanel is open or collapsed)
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
            visibility = if (panelState.crosshair.enabled) View.VISIBLE else View.GONE
            setContent {
                AZenithTheme(darkOverride = true) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CrosshairReticleCanvas(crosshair = panelState.crosshair)
                    }
                }
            }
        }

        // Single draggable Floating System Monitor window (persists whether sidepanel is open or collapsed)
        val hudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = panelState.hudOffsetX.roundToInt()
            y = ((16 * resources.displayMetrics.density) + panelState.hudOffsetY).roundToInt()
        }
        val hudComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)
            setContent {
                AZenithTheme(darkOverride = true) {
                    FloatingMonitorPill(
                        fps = panelState.currentFps,
                        cpuLoad = panelState.cpuLoad,
                        gpuLoad = panelState.gpuLoad
                    )
                }
            }
        }
        val hView = object : FrameLayout(this) {
            private var startRawX = 0f
            private var startRawY = 0f
            private var origX = 0
            private var origY = 0

            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startRawX = ev.rawX
                        startRawY = ev.rawY
                        origX = hudParams.x
                        origY = hudParams.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dm = resources.displayMetrics
                        val halfW = ((dm.widthPixels - width).coerceAtLeast(0)) / 2
                        val maxY = (dm.heightPixels - height - 8).coerceAtLeast(8)
                        val newX = (origX + (ev.rawX - startRawX)).roundToInt().coerceIn(-halfW, halfW)
                        val newY = (origY + (ev.rawY - startRawY)).roundToInt().coerceIn(8, maxY)
                        if (newX != hudParams.x || newY != hudParams.y) {
                            hudParams.x = newX
                            hudParams.y = newY
                            panelState.hudOffsetX = newX.toFloat()
                            panelState.hudOffsetY = newY - (16 * dm.density)
                            if (isViewAttached) {
                                runCatching { wm.updateViewLayout(this, hudParams) }
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        return true
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)
            visibility = if (panelState.floatingHudVisible) View.VISIBLE else View.GONE
            addView(
                hudComposeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        try {
            wm.addView(mainContainer, handleParams)
            wm.addView(chView, chParams)
            wm.addView(hView, hudParams)
            rootOverlayView = mainContainer
            crosshairView = chView
            hudView = hView
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
            rootOverlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            crosshairView = null
            hudView = null
            rootOverlayView = null
            isViewAttached = false
            isWindowFullScreen = false
        }
    }

    private fun expandPanel() {
        if (isPanelExpanded) return
        val view = rootOverlayView ?: return
        val wm = windowManager ?: return
        windowTransitionJob?.cancel()
        try {
            isWindowFullScreen = true
            wm.updateViewLayout(view, createExpandedLayoutParams())
            windowTransitionJob = serviceScope.launch {
                // Allow WindowManager 1 frame to expand surface before starting smooth morph animation
                delay(16)
                isPanelExpanded = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun collapsePanel() {
        if (!isPanelExpanded && !isWindowFullScreen) return
        panelState.cancelWidgetDrag()
        isPanelExpanded = false
        windowTransitionJob?.cancel()
        windowTransitionJob = serviceScope.launch {
            // Wait for the morph-to-bar spring animation to settle before shrinking window bounds
            delay(330)
            if (!isPanelExpanded && isViewAttached) {
                val view = rootOverlayView ?: return@launch
                val wm = windowManager ?: return@launch
                try {
                    isWindowFullScreen = false
                    wm.updateViewLayout(
                        view,
                        createHandleLayoutParams(sideSetting == "left", currentVerticalY)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createHandleLayoutParams(isLeft: Boolean, yPx: Int): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val barHeightDp = computeHandleBarHeightDp()
        val targetWidth = (24 * density).roundToInt()
        val targetHeight = ((barHeightDp + 16f) * density).roundToInt()

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
 * Root Composable orchestrating the smooth morph transition between the Edge Bar (`SwipeEdgeHandle`)
 * and the expanded Game Space Workspace (`SidepanelWorkspaceRow`).
 */
@Composable
fun SidePanelRoot(
    state: ZXSidePanelStateHolder,
    isLeft: Boolean,
    isHandleVisible: Boolean,
    isHandlePressed: Boolean,
    isExpanded: Boolean,
    isWindowFullScreen: Boolean,
    barHeightDp: Float,
    currentVerticalY: Int,
    onCollapseRequest: () -> Unit,
    onHudVisibilityChanged: (Boolean) -> Unit,
    onCrosshairVisibilityChanged: (Boolean) -> Unit
) {
    var clockTime by remember { mutableStateOf("04:05") }

    // Sync single floating HUD and crosshair window visibilities regardless of sidepanel expand/collapse state
    LaunchedEffect(isHandleVisible, state.floatingHudVisible) {
        onHudVisibilityChanged(isHandleVisible && state.floatingHudVisible)
    }

    LaunchedEffect(isHandleVisible, state.crosshair.enabled) {
        onCrosshairVisibilityChanged(isHandleVisible && state.crosshair.enabled)
    }

    // Live telemetry simulation + clock update
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

    // Smooth morph progress between 0f (Edge Bar) and 1f (Expanded Sidepanel)
    val openProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 340f
        ),
        label = "SidePanelMorphProgress"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // 1. Outside-tap dismiss scrim when expanded
        if (openProgress > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (openProgress * 0.35f).coerceIn(0f, 0.35f)
                    }
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapseRequest
                    )
            )
        }

        // 2. Swipe Edge Bar (#swipeEdgeArea) that smoothly morphs into/from the sidepanel at currentVerticalY
        val barVisibilityProgress = (1f - (openProgress * 2.2f)).coerceIn(0f, 1f)
        if (isHandleVisible && barVisibilityProgress > 0.01f) {
            SwipeEdgeHandle(
                isLeft = isLeft,
                isPressed = isHandlePressed,
                barHeightDp = barHeightDp,
                morphProgress = 1f - barVisibilityProgress,
                modifier = Modifier
                    .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                    .offset {
                        // When full-screen, position at currentVerticalY so it never jumps when switching window layout
                        IntOffset(0, if (isWindowFullScreen) currentVerticalY else 0)
                    }
            )
        }

        // 3. Sidepanel Workspace Row (morphs out of / back into the exact vertical position of the edge bar)
        if (openProgress > 0.01f) {
            val pivotYFraction = if (isWindowFullScreen) {
                ((currentVerticalY + 40f) / screenHeightPx).coerceIn(0.12f, 0.88f)
            } else {
                0.5f
            }

            Box(
                modifier = Modifier
                    .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(
                        start = if (isLeft) 16.dp else 0.dp,
                        end = if (!isLeft) 16.dp else 0.dp
                    )
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (isLeft) 0f else 1f,
                            pivotFractionY = pivotYFraction
                        )
                        val eased = openProgress.coerceIn(0f, 1f)
                        scaleX = 0.14f + 0.86f * eased
                        scaleY = 0.20f + 0.80f * eased
                        val slidePx = (1f - eased) * 28.dp.toPx()
                        translationX = if (isLeft) -slidePx else slidePx
                        alpha = (eased * 1.35f).coerceIn(0f, 1f)
                    }
            ) {
                SidepanelWorkspaceRow(
                    state = state,
                    clockTime = clockTime,
                    isLeft = isLeft
                )
            }
        }

        // 4. Snackbar (#m3Toast)
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
 * Compact, dynamically scaled Swipe Edge Bar that excludes system back gestures
 * and smoothly stretches/condenses as the sidepanel opens and closes.
 */
@Composable
private fun SwipeEdgeHandle(
    isLeft: Boolean,
    isPressed: Boolean,
    barHeightDp: Float,
    morphProgress: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    val barWidth by animateDpAsState(
        targetValue = if (isPressed || morphProgress > 0.05f) 7.dp else 4.5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SwipeEdgeBarWidth"
    )
    val barColor by animateColorAsState(
        targetValue = if (isPressed || morphProgress > 0.05f) {
            scheme.primary
        } else {
            scheme.onSurface.copy(alpha = 0.42f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "SwipeEdgeBarColor"
    )

    val dynamicHeight = (barHeightDp * (1f + morphProgress * 0.45f)).dp

    Box(
        modifier = modifier
            .width(24.dp)
            .height((barHeightDp + 16f).dp)
            .systemGestureExclusion(),
        contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        val pillShape = if (isLeft) {
            RoundedCornerShape(topEnd = 999.dp, bottomEnd = 999.dp)
        } else {
            RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp)
        }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = (1f - morphProgress).coerceIn(0f, 1f)
                    val shiftPx = morphProgress * 10.dp.toPx()
                    translationX = if (isLeft) shiftPx else -shiftPx
                }
                .width(barWidth)
                .height(dynamicHeight)
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

    var workspaceRootOrigin by remember { mutableStateOf(Offset.Zero) }
    var mainFeedRootBounds by remember { mutableStateOf(Rect.Zero) }
    var rightDrawerRootBounds by remember { mutableStateOf(Rect.Zero) }

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

    // Auto-scroll main feed while dragging a widget near the top or bottom edge
    LaunchedEffect(state.draggingWidgetId, state.dragPointerInWorkspace) {
        while (state.draggingWidgetId != null && !state.isHoveringLibraryDropZone) {
            val pointerRootY = workspaceRootOrigin.y + state.dragPointerInWorkspace.y
            val topZone = mainFeedRootBounds.top + 48f
            val bottomZone = mainFeedRootBounds.bottom - 48f
            if (mainFeedRootBounds.height > 0f) {
                if (pointerRootY < topZone && feedListState.firstVisibleItemIndex > 0) {
                    feedListState.animateScrollToItem(
                        (feedListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                    )
                } else if (pointerRootY > bottomZone) {
                    val nextIdx = (feedListState.firstVisibleItemIndex + 1)
                        .coerceAtMost(state.activeWidgets.size)
                    feedListState.animateScrollToItem(nextIdx)
                }
            }
            delay(180)
        }
    }

    val updateDragFromRootPointer: (Offset) -> Unit = { pointerInRoot ->
        val pointerInWorkspace = pointerInRoot - workspaceRootOrigin
        val overLibrary = state.editMode &&
            rightDrawerRootBounds.width > 0f &&
            rightDrawerRootBounds.contains(pointerInRoot)

        val relY = pointerInRoot.y - mainFeedRootBounds.top
        val visibleItems = feedListState.layoutInfo.visibleItemsInfo
        val maxIdx = if (state.draggingFromLibrary) {
            state.activeWidgets.size
        } else {
            (state.activeWidgets.size - 1).coerceAtLeast(0)
        }

        val computedIndex = if (visibleItems.isEmpty()) {
            0
        } else {
            val aboveItem = visibleItems.firstOrNull { item ->
                relY < (item.offset + item.size / 2f)
            }
            if (aboveItem != null) {
                aboveItem.index.coerceIn(0, maxIdx)
            } else {
                (visibleItems.last().index + if (state.draggingFromLibrary) 1 else 0)
                    .coerceIn(0, maxIdx)
            }
        }

        state.updateWidgetDrag(
            pointerInWorkspace = pointerInWorkspace,
            targetIndex = computedIndex,
            hoveringLibrary = overLibrary
        )
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

    Box(
        modifier = Modifier
            .fillMaxHeight(0.86f)
            .heightIn(max = 740.dp)
            .onGloballyPositioned { coords ->
                workspaceRootOrigin = coords.positionInRoot()
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
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
                    feedListState = feedListState,
                    onFeedBoundsChanged = { mainFeedRootBounds = it },
                    onStartActiveWidgetDrag = { wId, srcIdx, grabOffset, cardOriginInRoot ->
                        val startPointerInWorkspace = (cardOriginInRoot + grabOffset) - workspaceRootOrigin
                        state.startWidgetDrag(
                            widgetId = wId,
                            fromLibrary = false,
                            sourceIndex = srcIdx,
                            initialPointerInWorkspace = startPointerInWorkspace,
                            grabOffset = grabOffset
                        )
                    },
                    onDragWidgetMove = updateDragFromRootPointer,
                    onEndWidgetDrag = { state.commitWidgetDrag() },
                    onCancelWidgetDrag = { state.cancelWidgetDrag() }
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
                    RightWidgetDrawer(
                        state = state,
                        onDrawerBoundsChanged = { rightDrawerRootBounds = it },
                        onStartLibraryWidgetDrag = { wId, grabOffset, cardOriginInRoot ->
                            val startPointerInWorkspace = (cardOriginInRoot + grabOffset) - workspaceRootOrigin
                            state.startWidgetDrag(
                                widgetId = wId,
                                fromLibrary = true,
                                sourceIndex = -1,
                                initialPointerInWorkspace = startPointerInWorkspace,
                                grabOffset = grabOffset
                            )
                        },
                        onDragWidgetMove = updateDragFromRootPointer,
                        onEndWidgetDrag = { state.commitWidgetDrag() },
                        onCancelWidgetDrag = { state.cancelWidgetDrag() }
                    )
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
                    RightWidgetDrawer(
                        state = state,
                        onDrawerBoundsChanged = { rightDrawerRootBounds = it },
                        onStartLibraryWidgetDrag = { wId, grabOffset, cardOriginInRoot ->
                            val startPointerInWorkspace = (cardOriginInRoot + grabOffset) - workspaceRootOrigin
                            state.startWidgetDrag(
                                widgetId = wId,
                                fromLibrary = true,
                                sourceIndex = -1,
                                initialPointerInWorkspace = startPointerInWorkspace,
                                grabOffset = grabOffset
                            )
                        },
                        onDragWidgetMove = updateDragFromRootPointer,
                        onEndWidgetDrag = { state.commitWidgetDrag() },
                        onCancelWidgetDrag = { state.cancelWidgetDrag() }
                    )
                }

                MainSidepanelContent(
                    state = state,
                    clockTime = clockTime,
                    feedBlocks = feedBlocks,
                    feedListState = feedListState,
                    onFeedBoundsChanged = { mainFeedRootBounds = it },
                    onStartActiveWidgetDrag = { wId, srcIdx, grabOffset, cardOriginInRoot ->
                        val startPointerInWorkspace = (cardOriginInRoot + grabOffset) - workspaceRootOrigin
                        state.startWidgetDrag(
                            widgetId = wId,
                            fromLibrary = false,
                            sourceIndex = srcIdx,
                            initialPointerInWorkspace = startPointerInWorkspace,
                            grabOffset = grabOffset
                        )
                    },
                    onDragWidgetMove = updateDragFromRootPointer,
                    onEndWidgetDrag = { state.commitWidgetDrag() },
                    onCancelWidgetDrag = { state.cancelWidgetDrag() }
                )

                DetachedLeftRail(
                    state = state,
                    onScrollToSection = onScrollToSection
                )
            }
        }

        // Floating Picked-Up Widget Mockup Overlay during Drag-and-Drop
        val activeDragId = state.draggingWidgetId
        if (activeDragId != null) {
            DraggedWidgetMockupOverlay(
                widgetId = activeDragId,
                state = state,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (state.dragPointerInWorkspace.x - state.dragGrabOffset.x).roundToInt(),
                            (state.dragPointerInWorkspace.y - state.dragGrabOffset.y).roundToInt()
                        )
                    }
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
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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

            // Tonal Divider
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest)
            )

            // Below Divider: Real Installed Quick Launch Apps
            Text(
                text = "Apps",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.quickApps.forEach { pkgName ->
                    val app = state.installedApps.find { it.packageName == pkgName } ?: return@forEach
                    val shape = RoundedCornerShape(16.dp)

                    Surface(
                        shape = shape,
                        color = scheme.surfaceContainerHigh,
                        contentColor = scheme.onSurface,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(shape)
                            .clickable { state.launchQuickApp(app.packageName) }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap,
                                    contentDescription = app.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("grid_view"),
                                    contentDescription = app.name,
                                    tint = scheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Add App (+) Button
                val addShape = RoundedCornerShape(16.dp)
                Surface(
                    shape = addShape,
                    color = scheme.surfaceContainerHigh,
                    contentColor = scheme.onSurface,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { alpha = 0.7f }
                        .clip(addShape)
                        .clickable { state.openSubscreen("appsManager") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ZXSidePanelIcons.get("add"),
                            contentDescription = "Manage quick launch apps",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private sealed interface EditFeedRenderEntry {
    val key: String

    data class ActiveCard(
        val widgetId: String,
        val originalIndex: Int
    ) : EditFeedRenderEntry {
        override val key: String = "edit_card_$widgetId"
    }

    data class DropPlaceholder(
        val widgetId: String
    ) : EditFeedRenderEntry {
        override val key: String = "edit_drop_placeholder_$widgetId"
    }
}

@Composable
private fun MainSidepanelContent(
    state: ZXSidePanelStateHolder,
    clockTime: String,
    feedBlocks: List<SidePanelFeedBlock>,
    feedListState: androidx.compose.foundation.lazy.LazyListState,
    onFeedBoundsChanged: (Rect) -> Unit,
    onStartActiveWidgetDrag: (widgetId: String, index: Int, grabOffset: Offset, cardOriginInRoot: Offset) -> Unit,
    onDragWidgetMove: (pointerInRoot: Offset) -> Unit,
    onEndWidgetDrag: () -> Unit,
    onCancelWidgetDrag: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    // Build dynamic Edit Mode entries that visualize the live drop landing slot while dragging
    val editEntries by remember {
        derivedStateOf {
            val dragId = state.draggingWidgetId
            if (dragId == null) {
                state.activeWidgets.mapIndexed { idx, wId ->
                    EditFeedRenderEntry.ActiveCard(wId, idx)
                }
            } else if (state.draggingFromLibrary) {
                val base = state.activeWidgets.mapIndexed { idx, wId ->
                    EditFeedRenderEntry.ActiveCard(wId, idx)
                }.toMutableList<EditFeedRenderEntry>()
                if (!state.isHoveringLibraryDropZone) {
                    val targetIdx = state.dropTargetIndex.coerceIn(0, base.size)
                    base.add(targetIdx, EditFeedRenderEntry.DropPlaceholder(dragId))
                }
                base
            } else {
                // Reordering an existing active widget: remove from sourceIndex and show landing placeholder at dropTargetIndex
                val remaining = mutableListOf<EditFeedRenderEntry.ActiveCard>()
                state.activeWidgets.forEachIndexed { idx, wId ->
                    if (wId != dragId) {
                        remaining.add(EditFeedRenderEntry.ActiveCard(wId, idx))
                    }
                }
                val result = remaining.toMutableList<EditFeedRenderEntry>()
                if (!state.isHoveringLibraryDropZone) {
                    val targetIdx = state.dropTargetIndex.coerceIn(0, result.size)
                    result.add(targetIdx, EditFeedRenderEntry.DropPlaceholder(dragId))
                }
                result
            }
        }
    }

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
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        onFeedBoundsChanged(coords.boundsInRoot())
                    },
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
                        if (state.activeWidgets.isEmpty() && state.draggingWidgetId == null) {
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
                                            text = "Drag or add widgets from the right panel",
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
                                userScrollEnabled = state.draggingWidgetId == null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = editEntries,
                                    key = { it.key }
                                ) { entry ->
                                    when (entry) {
                                        is EditFeedRenderEntry.DropPlaceholder -> {
                                            WidgetDropLandingPlaceholder(
                                                widgetId = entry.widgetId,
                                                state = state,
                                                modifier = Modifier.animateItem(
                                                    placementSpec = spring(
                                                        dampingRatio = 0.78f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            )
                                        }
                                        is EditFeedRenderEntry.ActiveCard -> {
                                            EditModeWidgetCard(
                                                widgetId = entry.widgetId,
                                                index = entry.originalIndex,
                                                state = state,
                                                onStartDrag = { grabOffset, cardOriginInRoot ->
                                                    onStartActiveWidgetDrag(
                                                        entry.widgetId,
                                                        entry.originalIndex,
                                                        grabOffset,
                                                        cardOriginInRoot
                                                    )
                                                },
                                                onDragMove = onDragWidgetMove,
                                                onEndDrag = onEndWidgetDrag,
                                                onCancelDrag = onCancelWidgetDrag,
                                                modifier = Modifier.animateItem(
                                                    placementSpec = spring(
                                                        dampingRatio = 0.78f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            )
                                        }
                                    }
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
    state: ZXSidePanelStateHolder,
    onDrawerBoundsChanged: (Rect) -> Unit,
    onStartLibraryWidgetDrag: (widgetId: String, grabOffset: Offset, cardOriginInRoot: Offset) -> Unit,
    onDragWidgetMove: (pointerInRoot: Offset) -> Unit,
    onEndWidgetDrag: () -> Unit,
    onCancelWidgetDrag: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDropTargetHighlighted = state.draggingWidgetId != null &&
        !state.draggingFromLibrary &&
        state.isHoveringLibraryDropZone

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (isDropTargetHighlighted) {
            scheme.secondaryContainer.copy(alpha = 0.35f)
        } else {
            scheme.surfaceContainerLow
        },
        border = if (isDropTargetHighlighted) {
            BorderStroke(2.dp, scheme.primary)
        } else {
            null
        },
        shadowElevation = 24.dp,
        modifier = Modifier
            .width(290.dp)
            .fillMaxHeight()
            .onGloballyPositioned { coords ->
                onDrawerBoundsChanged(coords.boundsInRoot())
            }
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
                        text = if (isDropTargetHighlighted) {
                            "Release finger to store widget here"
                        } else {
                            "Drag into panel or tap + to add"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDropTargetHighlighted) scheme.primary else scheme.onSurfaceVariant
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
                            text = "Drag any active widget here to store it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    userScrollEnabled = state.draggingWidgetId == null,
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
                        var itemRootOrigin by remember { mutableStateOf(Offset.Zero) }
                        var lastPointerInRoot by remember { mutableStateOf(Offset.Zero) }
                        val isBeingDragged = state.draggingWidgetId == widgetId && state.draggingFromLibrary

                        Surface(
                            shape = cardShape,
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    itemRootOrigin = coords.positionInRoot()
                                }
                                .graphicsLayer {
                                    alpha = if (isBeingDragged) 0.3f else 1f
                                    scaleX = if (isBeingDragged) 0.95f else 1f
                                    scaleY = if (isBeingDragged) 0.95f else 1f
                                }
                                .clip(cardShape)
                                .pointerInput(widgetId) {
                                    detectDragGestures(
                                        onDragStart = { localOffset ->
                                            val startInRoot = itemRootOrigin + localOffset
                                            lastPointerInRoot = startInRoot
                                            onStartLibraryWidgetDrag(widgetId, localOffset, itemRootOrigin)
                                        },
                                        onDragEnd = { onEndWidgetDrag() },
                                        onDragCancel = { onCancelWidgetDrag() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            lastPointerInRoot += dragAmount
                                            onDragWidgetMove(lastPointerInRoot)
                                        }
                                    )
                                }
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

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = meta.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = scheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Drag to place or tap +",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
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
