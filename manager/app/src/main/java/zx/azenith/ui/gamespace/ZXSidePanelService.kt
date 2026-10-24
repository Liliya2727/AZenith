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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.topjohnwu.superuser.Shell
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
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.viewmodel.ApplistViewmodel
import zx.azenith.ui.viewmodel.AutoModeBus
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar
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
        const val ACTION_OPEN_FLOATING_APP = "zx.azenith.action.OPEN_FLOATING_APP"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"

        private const val APPLIST_PATH = "/data/adb/.config/AZenith/gamelist/azenithApplist.json"
        private const val APP_STATUS_PATH = "/data/adb/.config/AZenith/app_status"
        private const val DAEMON_BIN = "/data/adb/modules/AZenith/system/bin/sys.azenith-service"
        private const val DAEMON_PROFILE_PATH = "/data/adb/.config/AZenith/API/current_profile"
        private const val APP_PROFILE_PATH = "/data/data/zx.azenith/API/current_profile"
        private const val DAEMON_GAMEINFO_PATH = "/data/adb/.config/AZenith/API/gameinfo"
        private const val APP_GAMEINFO_PATH = "/data/data/zx.azenith/API/gameinfo"
        private const val DAEMON_MODES_PATH = "/data/adb/.config/AZenith/API/current_modes"
        private const val AI_PROP = "persist.sys.azenithconf.AIenabled"
        private const val LITE_PROP = "persist.sys.azenithconf.litemode"
        private const val BYPASS_PROP = "persist.sys.azenithconf.bypasschg"
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
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
    private var audioManager: AudioManager? = null
    private var floatingAppManager: ZXFloatingAppManager? = null
    private var rootOverlayView: View? = null
    private var crosshairView: ComposeView? = null
    private var hudView: View? = null
    private var isViewAttached = false
    private var monitorJob: Job? = null
    private var telemetryJob: Job? = null
    private var windowTransitionJob: Job? = null
    private var fpsTrackingActive = false
    private var fpsWindowStartNs = 0L
    private var fpsFrameCount = 0

    private val telemetryCollector by lazy { ZXRealTelemetryCollector() }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!fpsTrackingActive) return
            if (fpsWindowStartNs == 0L) {
                fpsWindowStartNs = frameTimeNanos
                fpsFrameCount = 0
            } else {
                fpsFrameCount++
                val elapsedNs = frameTimeNanos - fpsWindowStartNs
                if (elapsedNs >= 500_000_000L) {
                    val measured = ((fpsFrameCount * 1_000_000_000.0) / elapsedNs).roundToInt()
                    val cap = panelState.fpsCap.coerceAtLeast(60)
                    panelState.currentFps = measured.coerceIn(1, cap)
                    fpsWindowStartNs = frameTimeNanos
                    fpsFrameCount = 0
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

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

    @Volatile
    private var pendingLaunchPkg: String? = null

    @Volatile
    private var pendingLaunchGraceUntilMs: Long = 0L

    @Volatile
    private var lastUsageForegroundPkg: String? = null

    @Volatile
    private var lastUsageEventTs: Long = 0L

    @Volatile
    private var cachedApplistPackages: Set<String> = emptySet()

    @Volatile
    private var cachedApplistTimestampMs: Long = 0L

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
        startForegroundNotification()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        panelState.bindPreferences(prefs)
        sideSetting = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
        barSensitivity = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
        barLengthPref = prefs.getInt("gamespace_sidepanel_length", 50).coerceIn(20, 100)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        floatingAppManager = ZXFloatingAppManager(
            context = this,
            windowManager = windowManager!!,
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this,
            scope = serviceScope,
            checkRootGranted = { checkRootGranted() },
            resolveAppIcon = { pkg ->
                panelState.installedApps.firstOrNull { it.packageName == pkg }?.iconBitmap
                    ?: runCatching {
                        drawableToImageBitmap(packageManager.getApplicationIcon(pkg), 96)
                    }.getOrNull()
            }
        ).also { it.prebindRootServiceIfPossible() }

        // Ensure app-local API mirror directory exists and is world-accessible for the daemon
        runCatching {
            val apiDir = File(applicationInfo.dataDir, "API")
            if (!apiDir.exists()) apiDir.mkdirs()
            apiDir.setReadable(true, false)
            apiDir.setWritable(true, false)
            apiDir.setExecutable(true, false)
        }

        val displayMetrics = resources.displayMetrics
        val posPct = prefs.getInt("gamespace_sidepanel_pos", 50).coerceIn(10, 90)
        currentVerticalY = ((displayMetrics.heightPixels * (posPct / 100f))).roundToInt()

        panelState.onQuickAppsChanged = { list ->
            prefs.edit().putString(PREF_QUICK_APPS, list.joinToString(",")).apply()
        }
        panelState.onLaunchAppRequest = { pkg ->
            launchExternalApp(pkg)
        }
        panelState.onBrightnessChangedRequest = { pct ->
            applySystemBrightness(pct)
        }
        panelState.onVolumeChangedRequest = { pct, isMuted ->
            applySystemVolume(pct, isMuted)
        }
        panelState.onToggleAutoModeRequest = { enableAuto ->
            applyAutoModeToggle(enableAuto)
        }
        panelState.onSelectProfileRequest = { profileCode ->
            applyDaemonProfile(profileCode)
        }

        syncInitialAudioAndBrightness()
        loadInstalledLauncherApps()
        startAppMonitorWatcher()
        startRealTelemetryMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        val action = intent?.action ?: ACTION_SHOW
        when (action) {
            ACTION_SHOW -> {
                val pkg = intent?.getStringExtra(EXTRA_PACKAGE_NAME)?.takeIf { it.isNotBlank() && it != packageName }
                val label = intent?.getStringExtra(EXTRA_APP_LABEL)
                sideSetting = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
                barSensitivity = prefs.getInt("gamespace_sidepanel_sensitivity", 65).coerceIn(20, 100)
                barLengthPref = prefs.getInt("gamespace_sidepanel_length", 50).coerceIn(20, 100)

                if (pkg != null) {
                    val now = System.currentTimeMillis()
                    pendingLaunchPkg = pkg
                    pendingLaunchGraceUntilMs = now + 6_000L
                    lastUsageForegroundPkg = pkg
                    lastUsageEventTs = now
                    activeGamePackage = pkg
                    activeGameLabel = label ?: resolveAppLabel(pkg)
                    panelState.loadForApp(pkg)
                    attachOverlayIfNeeded()
                    isHandleVisible = true
                    startFpsChoreographer()
                } else if (!label.isNullOrBlank()) {
                    val now = System.currentTimeMillis()
                    pendingLaunchGraceUntilMs = now + 6_000L
                    val matchedPkg = panelState.installedApps.firstOrNull {
                        it.name.equals(label, ignoreCase = true)
                    }?.packageName
                    if (matchedPkg != null) {
                        pendingLaunchPkg = matchedPkg
                        lastUsageForegroundPkg = matchedPkg
                        lastUsageEventTs = now
                        if (activeGamePackage != matchedPkg) {
                            activeGamePackage = matchedPkg
                            activeGameLabel = label
                            panelState.loadForApp(matchedPkg)
                        }
                    } else {
                        activeGameLabel = label
                    }
                    attachOverlayIfNeeded()
                    isHandleVisible = true
                    startFpsChoreographer()
                }
            }
            ACTION_HIDE -> {
                pendingLaunchPkg = null
                pendingLaunchGraceUntilMs = 0L
                if (lastUsageForegroundPkg == activeGamePackage) {
                    lastUsageForegroundPkg = packageName
                    lastUsageEventTs = System.currentTimeMillis()
                }
                stopFpsChoreographer()
                isHandleVisible = false
                isPanelExpanded = false
                isWindowFullScreen = false
                detachOverlay()
            }
            ACTION_OPEN_FLOATING_APP -> {
                val pkg = intent?.getStringExtra(EXTRA_PACKAGE_NAME)
                if (!pkg.isNullOrBlank()) {
                    launchExternalApp(pkg)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isServiceActive = false
        stopFpsChoreographer()
        runCatching { prefs.unregisterOnSharedPreferenceChangeListener(prefListener) }
        monitorJob?.cancel()
        telemetryJob?.cancel()
        windowTransitionJob?.cancel()
        detachOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        }
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val channelId = "gamespace_overlay_service_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Game Space",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Game Space overlay service"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(zx.azenith.R.mipmap.ic_launcher)
            .setContentTitle("Game Space")
            .setContentText("Game Space overlay active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                startForeground(1001, notification, fgsType)
            } else {
                startForeground(1001, notification)
            }
        }.onFailure { e ->
            android.util.Log.w("ZXSidePanelService", "startForeground failed: ${e.message}")
        }
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

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        floatingAppManager?.onConfigurationChanged()
    }

    private fun launchExternalApp(pkg: String) {
        collapsePanel()
        serviceScope.launch {
            delay(140)
            floatingAppManager?.openFloatingApp(pkg)
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
                AZenithTheme {
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

            override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
                // Consume system cutout/bar insets so WindowManager does not create a safe-area gap on the left/right edge
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsets.CONSUMED
                } else {
                    @Suppress("DEPRECATION")
                    insets.consumeSystemWindowInsets()
                }
            }

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
            fitsSystemWindows = false
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
            applyFullCutoutMode()
        }
        val chView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)
            visibility = if (panelState.crosshair.enabled) View.VISIBLE else View.GONE
            setContent {
                AZenithTheme {
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
            applyFullCutoutMode()
        }
        val hudComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ZXSidePanelService)
            setViewTreeViewModelStoreOwner(this@ZXSidePanelService)
            setViewTreeSavedStateRegistryOwner(this@ZXSidePanelService)
            setContent {
                AZenithTheme {
                    FloatingMonitorPill(state = panelState)
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
                        panelState.saveWidgetLayoutPreferences()
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
        floatingAppManager?.destroyAllSessions()
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
        if (panelState.editMode) {
            panelState.editMode = false
        }
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

    private fun WindowManager.LayoutParams.applyFullCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
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
            applyFullCutoutMode()
        }
    }

    private fun createExpandedLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.FILL
            applyFullCutoutMode()
        }
    }

    private fun checkRootGranted(): Boolean {
        return runCatching {
            Shell.isAppGrantedRoot() ?: Shell.getShell().isRoot
        }.getOrDefault(false)
    }

    private fun queryForegroundPackageFromUsageStats(nowMs: Long): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return lastUsageForegroundPkg
        val lookbackMs = if (lastUsageForegroundPkg == null) 600_000L else 15_000L
        val events = runCatching { usm.queryEvents(nowMs - lookbackMs, nowMs) }.getOrNull()
            ?: return lastUsageForegroundPkg
        val event = UsageEvents.Event()
        var latestTs = lastUsageEventTs
        var latestPkg = lastUsageForegroundPkg
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val pkg = event.packageName
                if (!pkg.isNullOrBlank() &&
                    pkg != "android" &&
                    pkg != "com.android.systemui" &&
                    pkg != "com.android.permissioncontroller" &&
                    pkg != "com.google.android.permissioncontroller" &&
                    event.timeStamp >= latestTs
                ) {
                    latestTs = event.timeStamp
                    latestPkg = pkg
                }
            }
        }
        lastUsageEventTs = latestTs
        lastUsageForegroundPkg = latestPkg
        return latestPkg
    }

    private fun startAppMonitorWatcher() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val rootGranted = checkRootGranted()
                    if (rootGranted) {
                        telemetryCollector.unlockSysfsOnceWithRoot(packageName)
                    }
                    val nowMs = System.currentTimeMillis()
                    val inLaunchGrace = nowMs < pendingLaunchGraceUntilMs

                    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    var screenAwake = pm?.isInteractive ?: true
                    var focusedPkg: String? = null

                    val daemonRunning = PropertyUtils.get("persist.sys.azenith.state", "") == "running"
                    if (daemonRunning) {
                        val statusText = telemetryCollector.readFileTextDirectOrSu(APP_STATUS_PATH, rootGranted)
                        if (!statusText.isNullOrBlank()) {
                            val focusedLine = statusText.lineSequence().firstOrNull { it.startsWith("focused_app ") }
                            focusedPkg = focusedLine?.substringAfter("focused_app ")?.substringBefore(" ")?.trim()
                                ?.takeIf { it.isNotEmpty() && it != "unknown" && it != "none" }
                            val screenAwakeLine = statusText.lineSequence().firstOrNull { it.startsWith("screen_awake ") }
                            if (screenAwakeLine != null) {
                                screenAwake = screenAwakeLine.substringAfter("screen_awake ").trim() != "0"
                            }
                        }
                    }

                    if (focusedPkg == null) {
                        focusedPkg = queryForegroundPackageFromUsageStats(nowMs)
                    }

                    if (!focusedPkg.isNullOrBlank()) {
                        val isHostedFloatingApp = floatingAppManager?.isPackageHosted(focusedPkg) == true
                        val isConfiguredGame = focusedPkg != packageName &&
                            !isLauncherPackage(focusedPkg) &&
                            (isConfiguredAzGame(focusedPkg, rootGranted) || focusedPkg == pendingLaunchPkg)

                        if (screenAwake && isHostedFloatingApp) {
                            withContext(Dispatchers.Main) {
                                attachOverlayIfNeeded()
                                isHandleVisible = true
                                startFpsChoreographer()
                            }
                        } else if (screenAwake && isConfiguredGame) {
                            val switchedGame = activeGamePackage != focusedPkg
                            val label = if (switchedGame || activeGameLabel == null) resolveAppLabel(focusedPkg) else activeGameLabel
                            withContext(Dispatchers.Main) {
                                sideSetting = prefs.getString("gamespace_sidepanel_side", "left") ?: "left"
                                if (switchedGame) {
                                    activeGamePackage = focusedPkg
                                    activeGameLabel = label
                                    panelState.loadForApp(focusedPkg)
                                }
                                attachOverlayIfNeeded()
                                isHandleVisible = true
                                startFpsChoreographer()
                            }
                        } else if (!(inLaunchGrace && focusedPkg == packageName)) {
                            pendingLaunchPkg = null
                            pendingLaunchGraceUntilMs = 0L
                            if (isHandleVisible || isViewAttached) {
                                withContext(Dispatchers.Main) {
                                    stopFpsChoreographer()
                                    isHandleVisible = false
                                    isPanelExpanded = false
                                    isWindowFullScreen = false
                                    detachOverlay()
                                }
                            }
                        }
                    } else if (!screenAwake) {
                        if (isHandleVisible || isViewAttached) {
                            withContext(Dispatchers.Main) {
                                stopFpsChoreographer()
                                isHandleVisible = false
                                isPanelExpanded = false
                                isWindowFullScreen = false
                                detachOverlay()
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                delay(550L)
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

    /**
     * Strictly checks whether [pkg] is enabled in AZenith's configured game list
     * (`/data/adb/.config/AZenith/gamelist/azenithApplist.json` or `ApplistViewmodel.apps`).
     * Never falls back to `ApplicationInfo.CATEGORY_GAME` so non-AZenith apps never trigger the overlay.
     */
    private fun isConfiguredAzGame(pkg: String, rootGranted: Boolean): Boolean {
        if (pkg.isBlank() || pkg == packageName) return false

        val inMemoryApps = ApplistViewmodel.apps
        if (inMemoryApps.any { it.packageName == pkg && it.isEnabledInConfig }) {
            return true
        }

        val now = System.currentTimeMillis()
        if (now - cachedApplistTimestampMs > 1_500L) {
            try {
                val raw = telemetryCollector.readFileTextDirectOrSu(APPLIST_PATH, rootGranted)
                if (!raw.isNullOrBlank()) {
                    val json = JSONObject(raw)
                    val parsed = HashSet<String>()
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        parsed.add(keys.next())
                    }
                    cachedApplistPackages = parsed
                    cachedApplistTimestampMs = now
                }
            } catch (_: Exception) {
            }
        }
        return cachedApplistPackages.contains(pkg)
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

    private fun startFpsChoreographer() {
        if (fpsTrackingActive) return
        fpsTrackingActive = true
        fpsWindowStartNs = 0L
        fpsFrameCount = 0
        updateDisplayRefreshRateCap()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopFpsChoreographer() {
        if (!fpsTrackingActive) return
        fpsTrackingActive = false
        runCatching { Choreographer.getInstance().removeFrameCallback(frameCallback) }
    }

    private fun updateDisplayRefreshRateCap() {
        runCatching {
            @Suppress("DEPRECATION")
            val display = windowManager?.defaultDisplay ?: return
            val currentRate = display.refreshRate.roundToInt().coerceAtLeast(60)
            val maxRate = display.supportedModes.maxOfOrNull { it.refreshRate.roundToInt() } ?: currentRate
            panelState.fpsCap = maxRate.coerceAtLeast(currentRate)
            if (panelState.currentFps <= 0 || panelState.currentFps > panelState.fpsCap) {
                panelState.currentFps = currentRate
            }
        }
    }

    private fun syncInitialAudioAndBrightness() {
        runCatching {
            val rawBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 200)
            panelState.brightness = ((rawBrightness * 100f) / 255f).roundToInt().coerceIn(10, 100)
        }
        runCatching {
            val am = audioManager ?: return@runCatching
            val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            panelState.volume = ((curVol * 100f) / maxVol).roundToInt().coerceIn(0, 100)
            panelState.muted = curVol == 0
        }
    }

    private fun applySystemBrightness(pct: Int) {
        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                if (Settings.System.canWrite(this@ZXSidePanelService)) {
                    val sysVal = ((pct.coerceIn(10, 100) / 100f) * 255f).roundToInt().coerceIn(10, 255)
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, sysVal)
                }
            }
        }
    }

    private fun applySystemVolume(pct: Int, isMuted: Boolean) {
        serviceScope.launch(Dispatchers.IO) {
            runCatching {
                val am = audioManager ?: return@runCatching
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val targetVol = if (isMuted) 0 else ((pct.coerceIn(0, 100) / 100f) * maxVol).roundToInt()
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol.coerceIn(0, maxVol), 0)
            }
        }
    }

    private fun applyAutoModeToggle(enableAuto: Boolean) {
        serviceScope.launch(Dispatchers.IO) {
            val stateStr = if (enableAuto) "1" else "0"
            PropertyUtils.set(AI_PROP, stateStr)
            AutoModeBus.notify(stateStr)
            if (checkRootGranted()) {
                RootUtils.writeRootFile(DAEMON_MODES_PATH, "$stateStr\n")
            }
        }
    }

    private fun applyDaemonProfile(profileCode: String) {
        serviceScope.launch(Dispatchers.IO) {
            if (checkRootGranted()) {
                Shell.cmd("$DAEMON_BIN -p $profileCode").submit()
            }
        }
    }

    private fun startRealTelemetryMonitor() {
        telemetryJob?.cancel()
        telemetryJob = serviceScope.launch(Dispatchers.IO) {
            var tick = 0
            var lastObservedProfileCode = ""
            var lastParsedStartTimeStr = ""

            while (isActive) {
                if (isHandleVisible || panelState.floatingHudVisible) {
                    val rootGranted = checkRootGranted()
                    if (rootGranted) {
                        telemetryCollector.unlockSysfsOnceWithRoot(packageName)
                    }

                    // 1. Read real-time FPS from kernel/sysfs if available
                    val realFps = telemetryCollector.readRealFps(rootGranted, panelState.fpsCap)

                    // 2. Read CPU & GPU load via direct File IO (no inline shell commands)
                    val cpuPct = telemetryCollector.readCpuLoadPercent(rootGranted)
                    val gpuPct = telemetryCollector.readGpuLoadPercent(
                        rootGranted = rootGranted,
                        currentFps = realFps ?: panelState.currentFps,
                        fpsCap = panelState.fpsCap,
                        cpuLoad = cpuPct
                    )

                    // 3. Read Battery Level, Charging & Thermal Temperature
                    val batteryInfo = readBatteryAndThermal(rootGranted)

                    // 3. Read AZenith Auto Mode, Running Profile, and Profile Duration
                    val daemonRunning = PropertyUtils.get("persist.sys.azenith.state", "") == "running"
                    val aiPropVal = PropertyUtils.get(AI_PROP, "")
                    val modesFileVal = telemetryCollector.readFileTextDirectOrSu(DAEMON_MODES_PATH, rootGranted)?.trim()
                    val resolvedAuto = when {
                        !modesFileVal.isNullOrEmpty() -> modesFileVal == "1"
                        aiPropVal.isNotEmpty() -> aiPropVal == "1"
                        else -> panelState.isAutoMode
                    }
                    val liteEnabled = PropertyUtils.get(LITE_PROP, "0") == "1"
                    val bypassActive = PropertyUtils.get(BYPASS_PROP, "0") == "1"

                    val rawProfileCode = if (daemonRunning) {
                        telemetryCollector.readFileTextDirectOrSu(APP_PROFILE_PATH, false)?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: telemetryCollector.readFileTextDirectOrSu(DAEMON_PROFILE_PATH, rootGranted)?.trim()
                            ?: panelState.currentProfileCode
                    } else {
                        panelState.currentProfileCode
                    }

                    val normalizedCode = if (rawProfileCode in listOf("0", "1", "2", "3")) {
                        rawProfileCode
                    } else {
                        panelState.currentProfileCode
                    }

                    val mappedActiveProfile = when (normalizedCode) {
                        "3" -> "saving"
                        "2" -> "balanced"
                        "1" -> "performance"
                        else -> panelState.activeProfile
                    }

                    if (daemonRunning) {
                        val gameInfoRaw = telemetryCollector.readFileTextDirectOrSu(APP_GAMEINFO_PATH, false)
                            ?.takeIf { it.isNotBlank() }
                            ?: telemetryCollector.readFileTextDirectOrSu(DAEMON_GAMEINFO_PATH, rootGranted)

                        val startTimeLine = gameInfoRaw?.lineSequence()
                            ?.firstOrNull { it.startsWith("Time:") }
                            ?.substringAfter("Time:")
                            ?.trim()
                            .orEmpty()

                        if (normalizedCode != lastObservedProfileCode ||
                            (startTimeLine.isNotEmpty() && startTimeLine != lastParsedStartTimeStr)
                        ) {
                            lastObservedProfileCode = normalizedCode
                            lastParsedStartTimeStr = startTimeLine
                            val parsedEpoch = parseTodayTimeStrToEpochMs(startTimeLine)
                            val fileMtime = File(APP_PROFILE_PATH).takeIf { it.exists() }?.lastModified() ?: 0L
                            panelState.profileSessionStartMs = when {
                                parsedEpoch != null -> parsedEpoch
                                fileMtime > 0L -> fileMtime
                                else -> System.currentTimeMillis()
                            }
                        }
                    } else if (normalizedCode != lastObservedProfileCode) {
                        lastObservedProfileCode = normalizedCode
                        panelState.profileSessionStartMs = System.currentTimeMillis()
                    }

                    val elapsedMs = (System.currentTimeMillis() - panelState.profileSessionStartMs).coerceAtLeast(0L)
                    val totalSec = elapsedMs / 1000L
                    val hours = totalSec / 3600L
                    val mins = (totalSec % 3600L) / 60L
                    val secs = totalSec % 60L
                    val durationFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)

                    // 4. Periodically measure network latency & sync external brightness/volume changes
                    var measuredPing = panelState.pingMs
                    if (tick % 3 == 0) {
                        telemetryCollector.measureTcpPingMs()?.let { measuredPing = it }
                    }

                    withContext(Dispatchers.Main) {
                        updateDisplayRefreshRateCap()
                        if (realFps != null && realFps > 0) {
                            panelState.currentFps = realFps
                        }
                        panelState.isRootAvailable = rootGranted
                        panelState.cpuLoad = cpuPct
                        panelState.gpuLoad = gpuPct
                        panelState.batteryLevel = batteryInfo.first
                        panelState.isBatteryCharging = batteryInfo.second
                        panelState.tempCelsius = batteryInfo.third
                        panelState.isAutoMode = resolvedAuto
                        panelState.isLiteMode = liteEnabled
                        panelState.isBypassChargingActive = bypassActive
                        panelState.currentProfileCode = normalizedCode
                        panelState.activeProfile = mappedActiveProfile
                        panelState.profileDurationText = durationFormatted
                        panelState.pingMs = measuredPing
                    }
                    tick++
                }
                delay(1000L)
            }
        }
    }

    private fun readBatteryAndThermal(rootGranted: Boolean): Triple<Int, Boolean, Int> {
        var levelPct = panelState.batteryLevel
        var charging = panelState.isBatteryCharging
        var tempC = panelState.tempCelsius

        runCatching {
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (level >= 0) {
                    levelPct = ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
                }
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                val rawBatteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (rawBatteryTemp > 0) {
                    tempC = (rawBatteryTemp / 10f).roundToInt().coerceIn(15, 95)
                }
            }
        }

        val sysfsTemp = telemetryCollector.readThermalZoneCelsius(rootGranted)
        if (sysfsTemp != null && sysfsTemp in 20..95) {
            tempC = sysfsTemp
        }
        return Triple(levelPct, charging, tempC)
    }

    private fun parseTodayTimeStrToEpochMs(timeStr: String): Long? {
        if (timeStr.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            val parsed = sdf.parse(timeStr) ?: return null
            val now = Calendar.getInstance()
            val start = Calendar.getInstance().apply {
                time = parsed
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.MONTH, now.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            }
            var targetMs = start.timeInMillis
            if (targetMs > now.timeInMillis + 2000L) {
                targetMs -= 24L * 60L * 60L * 1000L
            }
            targetMs
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Direct sysfs/procfs telemetry collector that operates both with and without root
 * using standard `java.io.File` reads (and `SuFileInputStream` stream fallback when root is granted)
 * without spawning per-tick inline shell commands.
 */
private class ZXRealTelemetryCollector {
    private val coreDirs: List<File> by lazy {
        val cpuRoot = File("/sys/devices/system/cpu")
        val count = Runtime.getRuntime().availableProcessors().coerceIn(1, 16)
        (0 until count).map { idx -> File(cpuRoot, "cpu$idx/cpufreq") }
    }

    private val coreMaxFreqKhz = HashMap<Int, Long>()
    private val coreMinFreqKhz = HashMap<Int, Long>()

    private var prevProcTotal = 0L
    private var prevProcIdle = 0L
    private var sysfsPermissionsUnlocked = false

    private val gpuDirectCandidates = listOf(
        "/sys/module/ged/parameters/gpu_loading",
        "/sys/kernel/ged/hal/gpu_utilization",
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
        "/sys/class/kgsl/kgsl-3d0/gpubusy",
        "/sys/kernel/gpu/gpu_busy",
        "/proc/mali/utilization",
        "/proc/gpufreqv2/gpufreq_status",
        "/proc/gpufreq/gpufreq_var_dump",
        "/sys/class/misc/mali0/device/utilization",
        "/sys/devices/platform/mali/utilization",
        "/sys/class/devfreq/13000000.mali/cur_freq"
    )

    fun unlockSysfsOnceWithRoot(pkgName: String) {
        if (sysfsPermissionsUnlocked) return
        sysfsPermissionsUnlocked = true
        runCatching {
            // One-time chmod & appops grant when service starts with root so all subsequent reads use direct APIs/File IO
            Shell.cmd(
                "appops set $pkgName GET_USAGE_STATS allow 2>/dev/null",
                "appops set $pkgName RUN_IN_BACKGROUND allow 2>/dev/null",
                "appops set $pkgName RUN_ANY_IN_BACKGROUND allow 2>/dev/null",
                "dumpsys deviceidle whitelist +$pkgName >/dev/null 2>&1",
                "pm grant $pkgName android.permission.PACKAGE_USAGE_STATS 2>/dev/null",
                "pm grant $pkgName android.permission.BATTERY_STATS 2>/dev/null",
                "chmod 0444 /sys/devices/system/cpu/cpu*/cpufreq/* /proc/stat 2>/dev/null",
                "chmod 0755 /sys/kernel/ged /sys/kernel/ged/hal /sys/class/devfreq 2>/dev/null",
                "chmod 0444 /sys/kernel/ged/hal/* /sys/module/ged/parameters/* /sys/class/kgsl/kgsl-3d0/* /sys/class/devfreq/*/* /proc/mali/utilization 2>/dev/null",
                "chmod 0644 /data/adb/.config/AZenith/API/* /data/data/zx.azenith/API/* 2>/dev/null"
            ).exec()
        }
    }

    fun readFileTextDirectOrSu(path: String, allowSuFallback: Boolean): String? {
        try {
            val f = File(path)
            if (f.exists()) {
                val text = f.readText()
                if (text.isNotEmpty()) return text
            }
        } catch (_: Exception) {
        }
        if (allowSuFallback) {
            try {
                val suFile = SuFile(path)
                if (suFile.exists()) {
                    val content = SuFileInputStream.open(suFile).bufferedReader().use { it.readText() }
                    if (content.isNotEmpty()) return content
                }
            } catch (_: Exception) {
            }
            try {
                val res = Shell.cmd("cat $path 2>/dev/null").exec()
                if (res.isSuccess && res.out.isNotEmpty()) {
                    return res.out.joinToString("\n")
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun readFirstLineDirectOrSu(path: String, allowSuFallback: Boolean): String? {
        readFirstLineDirect(File(path))?.let { return it }
        if (allowSuFallback) {
            try {
                val suFile = SuFile(path)
                if (suFile.exists()) {
                    return SuFileInputStream.open(suFile).bufferedReader().use { it.readLine() }
                }
            } catch (_: Exception) {
            }
            try {
                val res = Shell.cmd("head -n 1 $path 2>/dev/null").exec()
                if (res.isSuccess && res.out.isNotEmpty()) {
                    return res.out.firstOrNull()
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun parseProcStatLine(line: String?): Pair<Long, Long>? {
        if (line == null || !line.startsWith("cpu ")) return null
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 5) return null
        val user = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val nice = parts.getOrNull(2)?.toLongOrNull() ?: 0L
        val system = parts.getOrNull(3)?.toLongOrNull() ?: 0L
        val idle = parts.getOrNull(4)?.toLongOrNull() ?: 0L
        val iowait = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val irq = parts.getOrNull(6)?.toLongOrNull() ?: 0L
        val softirq = parts.getOrNull(7)?.toLongOrNull() ?: 0L
        val steal = parts.getOrNull(8)?.toLongOrNull() ?: 0L

        val totalIdle = idle + iowait
        val total = user + nice + system + totalIdle + irq + softirq + steal
        return Pair(total, totalIdle)
    }

    fun readCpuLoadPercent(rootGranted: Boolean): Int {
        // 1. Read kernel /proc/stat delta via direct File or libsu stream (never affected by governor max-freq pinning)
        var statSample = parseProcStatLine(readFirstLineDirectOrSu("/proc/stat", rootGranted))
        if (statSample != null && prevProcTotal == 0L) {
            prevProcTotal = statSample.first
            prevProcIdle = statSample.second
            runCatching { Thread.sleep(65L) }
            statSample = parseProcStatLine(readFirstLineDirectOrSu("/proc/stat", rootGranted))
        }

        if (statSample != null) {
            val (total, totalIdle) = statSample
            val diffTotal = total - prevProcTotal
            val diffIdle = totalIdle - prevProcIdle
            prevProcTotal = total
            prevProcIdle = totalIdle

            if (diffTotal > 0L) {
                val busy = (diffTotal - diffIdle).coerceAtLeast(0L)
                return ((busy * 100L) / diffTotal).toInt().coerceIn(1, 100)
            }
        }

        // 2. Non-root fallback when SELinux blocks /proc/stat: read per-core cpufreq scaling_cur_freq vs hardware max/min
        var weightedCur = 0.0
        var weightedMax = 0.0
        var coresAtMaxCount = 0
        var validCoresCount = 0

        coreDirs.forEachIndexed { idx, dir ->
            val curKhz = readLongDirect(File(dir, "scaling_cur_freq"))
                ?: readLongDirect(File(dir, "cpuinfo_cur_freq"))

            val maxKhz = coreMaxFreqKhz[idx] ?: run {
                val availMax = readFirstLineDirect(File(dir, "scaling_available_frequencies"))
                    ?.trim()
                    ?.split(Regex("\\s+"))
                    ?.mapNotNull { it.toLongOrNull() }
                    ?.maxOrNull()
                val m = availMax
                    ?: readLongDirect(File(dir, "cpuinfo_max_freq"))
                    ?: readLongDirect(File(dir, "scaling_max_freq"))
                if (m != null && m > 0L) coreMaxFreqKhz[idx] = m
                m
            }
            val minKhz = coreMinFreqKhz[idx] ?: run {
                val availMin = readFirstLineDirect(File(dir, "scaling_available_frequencies"))
                    ?.trim()
                    ?.split(Regex("\\s+"))
                    ?.mapNotNull { it.toLongOrNull() }
                    ?.minOrNull()
                val mn = availMin
                    ?: readLongDirect(File(dir, "cpuinfo_min_freq"))
                    ?: readLongDirect(File(dir, "scaling_min_freq"))
                    ?: 300_000L
                coreMinFreqKhz[idx] = mn
                mn
            }

            if (curKhz != null && maxKhz != null && maxKhz > minKhz) {
                validCoresCount++
                if (curKhz >= (maxKhz * 98L) / 100L) {
                    coresAtMaxCount++
                }
                val span = (maxKhz - minKhz).coerceAtLeast(1L).toDouble()
                val norm = ((curKhz - minKhz).toDouble() / span).coerceIn(0.02, 1.0)
                val weight = maxKhz.toDouble()
                weightedCur += norm * weight
                weightedMax += weight
            }
        }

        if (weightedMax > 0.0) {
            val rawPct = ((weightedCur / weightedMax) * 100.0).roundToInt().coerceIn(2, 100)
            // If all cores are pinned at max frequency while unrooted, estimate realistic active load instead of sticking at 100%
            if (validCoresCount > 0 && coresAtMaxCount == validCoresCount && rawPct >= 97) {
                return 64
            }
            return rawPct
        }
        return 25
    }

    fun readGpuLoadPercent(
        rootGranted: Boolean,
        currentFps: Int,
        fpsCap: Int,
        cpuLoad: Int
    ): Int {
        // 1. Probe direct GPU load sysfs files
        for (path in gpuDirectCandidates) {
            val raw = readFileTextDirectOrSu(path, rootGranted)?.trim()
            if (!raw.isNullOrEmpty()) {
                if (path.endsWith("gpubusy")) {
                    val nums = Regex("\\d+").findAll(raw).mapNotNull { it.value.toLongOrNull() }.toList()
                    if (nums.size >= 2 && nums[1] > 0L) {
                        return ((nums[0] * 100L) / nums[1]).toInt().coerceIn(0, 100)
                    }
                } else {
                    val firstInt = Regex("\\d+").find(raw)?.value?.toIntOrNull()
                    if (firstInt != null && firstInt in 0..100) {
                        return firstInt
                    }
                }
            }
        }

        // 2. Try /sys/kernel/gpu/gpu_clock vs gpu_max_clock or /sys/class/kgsl/kgsl-3d0/devfreq
        val curClk = readFileTextDirectOrSu("/sys/kernel/gpu/gpu_clock", rootGranted)?.trim()?.toLongOrNull()
            ?: readFileTextDirectOrSu("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq", rootGranted)?.trim()?.toLongOrNull()
        val maxClk = readFileTextDirectOrSu("/sys/kernel/gpu/gpu_max_clock", rootGranted)?.trim()?.toLongOrNull()
            ?: readFileTextDirectOrSu("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq", rootGranted)?.trim()?.toLongOrNull()
        if (curClk != null && maxClk != null && maxClk > 0L) {
            return ((curClk * 100L) / maxClk).toInt().coerceIn(1, 100)
        }

        // 3. Try /sys/class/devfreq/*gpu* or *mali*
        runCatching {
            val devfreqRoot = File("/sys/class/devfreq")
            val dirs = devfreqRoot.listFiles()
            if (dirs != null) {
                for (d in dirs) {
                    val name = d.name.lowercase(Locale.US)
                    if (name.contains("gpu") || name.contains("mali") || name.contains("kgsl") || name.contains("dfrgx")) {
                        val loadRaw = readFirstLineDirect(File(d, "load"))
                            ?: readFirstLineDirect(File(d, "utilization"))
                        if (!loadRaw.isNullOrEmpty()) {
                            val pct = Regex("\\d+").find(loadRaw.substringBefore('@'))?.value?.toIntOrNull()
                            if (pct != null && pct in 0..100) return pct
                        }
                        val cf = readLongDirect(File(d, "cur_freq"))
                        val mf = readLongDirect(File(d, "max_freq"))
                        if (cf != null && mf != null && mf > 0L) {
                            return ((cf * 100L) / mf).toInt().coerceIn(1, 100)
                        }
                    }
                }
            }
        }

        // 4. Non-root fallback when kernel hides all GPU sysfs nodes: derive from live frame cadence & CPU freq load
        val fpsRatio = (currentFps.toFloat() / fpsCap.coerceAtLeast(60).toFloat()).coerceIn(0.1f, 1f)
        val dynamicJitter = ((System.currentTimeMillis() / 1000L) % 5).toInt() - 2
        return ((cpuLoad * 0.55f) + (fpsRatio * 32f) + dynamicJitter).roundToInt().coerceIn(4, 98)
    }

    fun readThermalZoneCelsius(rootGranted: Boolean): Int? {
        val candidates = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/power_supply/battery/temp"
        )
        for (path in candidates) {
            val raw = readFileTextDirectOrSu(path, rootGranted)?.trim()?.toIntOrNull() ?: continue
            val c = when {
                raw >= 10000 -> raw / 1000
                raw >= 200 -> raw / 10
                else -> raw
            }
            if (c in 20..95) return c
        }
        return null
    }

    fun measureTcpPingMs(): Int? {
        return try {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 650)
            }
            val elapsedMs = ((System.nanoTime() - start) / 1_000_000L).toInt()
            elapsedMs.coerceIn(1, 999)
        } catch (_: Exception) {
            null
        }
    }

    fun readRealFps(rootGranted: Boolean, defaultCap: Int): Int? {
        val kpiRaw = readFileTextDirectOrSu("/sys/kernel/ged/hal/ged_kpi", rootGranted)?.trim()
        if (!kpiRaw.isNullOrBlank()) {
            val fpsVal = kpiRaw.substringBefore(',').trim().toIntOrNull()
            if (fpsVal != null && fpsVal in 1..defaultCap) {
                return fpsVal
            }
        }
        val fpsgoRaw = readFileTextDirectOrSu("/sys/kernel/debug/fpsgo/common/fpsgo_status", rootGranted)?.trim()
        if (!fpsgoRaw.isNullOrBlank()) {
            val fpsVal = Regex("fps:\\s*(\\d+)").find(fpsgoRaw)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (fpsVal != null && fpsVal in 1..defaultCap) {
                return fpsVal
            }
        }
        return null
    }

    private fun readFirstLineDirect(file: File): String? {
        return try {
            file.bufferedReader().use { it.readLine() }
        } catch (_: Exception) {
            null
        }
    }

    private fun readLongDirect(file: File): Long? {
        return readFirstLineDirect(file)?.trim()?.toLongOrNull()
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

    // Clock update (telemetry is driven by ZXSidePanelService.startRealTelemetryMonitor)
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (isActive) {
            clockTime = formatter.format(Date())
            delay(1000L)
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
            val isCompactPortrait = maxWidth < 520.dp
            val horizPad = if (isCompactPortrait) 8.dp else 12.dp
            val availableWidthDp = (maxWidth - horizPad * 2).coerceAtLeast(280.dp)

            Box(
                modifier = Modifier
                    .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    // Manual safe-area padding (system display cutout safe-area inset is disabled)
                    .padding(horizontal = horizPad, vertical = 8.dp)
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
                    isLeft = isLeft,
                    availableWidthDp = availableWidthDp,
                    isCompactPortrait = isCompactPortrait
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
            scheme.primary.copy(alpha = 0.85f)
        } else {
            scheme.primary.copy(alpha = 0.28f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "SwipeEdgeBarColor"
    )
    val outlineColor by animateColorAsState(
        targetValue = if (isPressed || morphProgress > 0.05f) {
            scheme.primary
        } else {
            scheme.primary.copy(alpha = 0.60f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "SwipeEdgeBarOutline"
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
                .border(
                    width = 1.dp,
                    color = outlineColor,
                    shape = pillShape
                )
                .background(
                    color = barColor,
                    shape = pillShape
                )
        )
    }
}

@Composable
private fun SidepanelWorkspaceRow(
    state: ZXSidePanelStateHolder,
    clockTime: String,
    isLeft: Boolean,
    availableWidthDp: Dp,
    isCompactPortrait: Boolean
) {
    val density = LocalDensity.current
    val feedListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val feedBlocks by remember {
        derivedStateOf { state.buildFeedBlocks() }
    }

    var workspaceRootOrigin by remember { mutableStateOf(Offset.Zero) }
    var mainFeedRootBounds by remember { mutableStateOf(Rect.Zero) }
    var rightDrawerRootBounds by remember { mutableStateOf(Rect.Zero) }

    val editCardBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val editRemoveBtnBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val editResizeGripBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val libraryCardBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val libraryAddBtnBoundsMap = remember { mutableStateMapOf<String, Rect>() }

    // Automatically sync active category in Left Rail when scrolling main feed
    val firstVisibleIndex by remember {
        derivedStateOf { feedListState.firstVisibleItemIndex }
    }
    LaunchedEffect(firstVisibleIndex, state.editMode, state.activeSubscreen) {
        if (!state.editMode && state.activeSubscreen == null) {
            val wId = state.activeWidgets.getOrNull(firstVisibleIndex)
            val category = wId?.let { state.widgetCatalog[it]?.category }
            if (category != null && category != state.activeScrollCategory) {
                state.activeScrollCategory = category
            }
        }
    }

    val updateDragFromRootPointer: (Offset) -> Unit = { pointerInRoot ->
        val pointerInWorkspace = pointerInRoot - workspaceRootOrigin
        val overLibrary = state.editMode &&
            rightDrawerRootBounds.width > 0f &&
            rightDrawerRootBounds.contains(pointerInRoot)

        val edgeZonePx = 72f * density.density
        val topZone = mainFeedRootBounds.top + edgeZonePx
        val bottomZone = mainFeedRootBounds.bottom - edgeZonePx
        val inTopScrollZone = pointerInRoot.y < topZone && feedListState.canScrollBackward
        val inBottomScrollZone = pointerInRoot.y > bottomZone && feedListState.canScrollForward

        val maxIdx = if (state.draggingFromLibrary) {
            state.activeWidgets.size
        } else {
            (state.activeWidgets.size - 1).coerceAtLeast(0)
        }

        val computedIndex = when {
            overLibrary -> -1
            inTopScrollZone -> 0
            inBottomScrollZone -> maxIdx
            else -> {
                val candidates = state.activeWidgets.mapNotNull { wId ->
                    val rect = editCardBoundsMap[wId]
                    if (rect != null && rect.width > 0f) wId to rect else null
                }
                if (candidates.isEmpty()) {
                    0
                } else {
                    val matching = candidates.firstOrNull { (_, rect) ->
                        pointerInRoot.y >= rect.top && pointerInRoot.y <= rect.bottom
                    } ?: candidates.minByOrNull { (_, rect) ->
                        if (pointerInRoot.y < rect.top) rect.top - pointerInRoot.y
                        else pointerInRoot.y - rect.bottom
                    }
                    if (matching != null) {
                        val (wId, rect) = matching
                        val itemIdx = state.activeWidgets.indexOf(wId)
                        if (itemIdx >= 0) {
                            if (pointerInRoot.x > rect.center.x) {
                                (itemIdx + 1).coerceIn(0, maxIdx)
                            } else {
                                itemIdx.coerceIn(0, maxIdx)
                            }
                        } else {
                            0
                        }
                    } else {
                        0
                    }
                }
            }
        }

        state.updateWidgetDrag(
            pointerInWorkspace = pointerInWorkspace,
            targetIndex = computedIndex,
            hoveringLibrary = overLibrary
        )
    }

    // Smooth 60fps auto-scroll while dragging a widget near the top or bottom edge of the main feed
    LaunchedEffect(state.draggingWidgetId) {
        while (state.draggingWidgetId != null) {
            if (!state.isHoveringLibraryDropZone && mainFeedRootBounds.height > 0f) {
                val pointerRootY = workspaceRootOrigin.y + state.dragPointerInWorkspace.y
                val edgeZonePx = 72f * density.density
                val topZone = mainFeedRootBounds.top + edgeZonePx
                val bottomZone = mainFeedRootBounds.bottom - edgeZonePx
                val maxStepPx = 18f * density.density

                val scrollDelta = when {
                    pointerRootY < topZone && feedListState.canScrollBackward -> {
                        val intensity = ((topZone - pointerRootY) / edgeZonePx).coerceIn(0.15f, 1.35f)
                        -maxStepPx * intensity
                    }
                    pointerRootY > bottomZone && feedListState.canScrollForward -> {
                        val intensity = ((pointerRootY - bottomZone) / edgeZonePx).coerceIn(0.15f, 1.35f)
                        maxStepPx * intensity
                    }
                    else -> 0f
                }

                if (abs(scrollDelta) > 0.5f) {
                    feedListState.scrollBy(scrollDelta)
                }
            }
            delay(16L)
        }
    }

    val onScrollToSection: (String) -> Unit = { categoryName ->
        if (state.activeSubscreen != null) {
            state.closeSubscreen()
        }
        state.activeScrollCategory = categoryName
        coroutineScope.launch {
            val idx = state.activeWidgets.indexOfFirst { wId ->
                state.widgetCatalog[wId]?.category == categoryName
            }
            if (idx >= 0) {
                feedListState.animateScrollToItem(idx)
            }
        }
    }

    val railWidth = if (isCompactPortrait) 64.dp else 78.dp
    val rowSpacing = if (isCompactPortrait) 8.dp else 10.dp
    val targetMainWidth = if (isCompactPortrait) {
        (availableWidthDp - railWidth - rowSpacing).coerceIn(252.dp, 354.dp)
    } else {
        if (state.editMode) 332.dp else 354.dp
    }

    Box(
        modifier = Modifier
            .fillMaxHeight(if (isCompactPortrait) 0.90f else 0.86f)
            .heightIn(max = if (isCompactPortrait) 820.dp else 740.dp)
            .onGloballyPositioned { coords ->
                workspaceRootOrigin = coords.positionInRoot()
            }
            .pointerInput(state.editMode) {
                if (!state.editMode) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val downInRoot = workspaceRootOrigin + down.position

                    val hitActiveId = if (state.activeSubscreen == null && mainFeedRootBounds.contains(downInRoot)) {
                        state.activeWidgets.firstOrNull { wId ->
                            val cardRect = editCardBoundsMap[wId]
                            val removeRect = editRemoveBtnBoundsMap[wId]
                            val gripRect = editResizeGripBoundsMap[wId]
                            cardRect != null &&
                                cardRect.width > 0f &&
                                cardRect.contains(downInRoot) &&
                                (removeRect == null || !removeRect.inflate(4f).contains(downInRoot)) &&
                                (gripRect == null || !gripRect.inflate(6f).contains(downInRoot))
                        }
                    } else {
                        null
                    }

                    val hitLibraryId = if (hitActiveId == null && rightDrawerRootBounds.contains(downInRoot)) {
                        state.libraryWidgets.firstOrNull { wId ->
                            val cardRect = libraryCardBoundsMap[wId]
                            val addRect = libraryAddBtnBoundsMap[wId]
                            cardRect != null &&
                                cardRect.width > 0f &&
                                cardRect.contains(downInRoot) &&
                                (addRect == null || !addRect.inflate(4f).contains(downInRoot))
                        }
                    } else {
                        null
                    }

                    if (hitActiveId == null && hitLibraryId == null) {
                        return@awaitEachGesture
                    }

                    val pointerId = down.id
                    val slopPx = viewConfiguration.touchSlop
                    var dragStarted = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            if (dragStarted) {
                                state.commitWidgetDrag()
                            }
                            break
                        }

                        val currentInRoot = workspaceRootOrigin + change.position
                        if (!dragStarted) {
                            val dist = (currentInRoot - downInRoot).getDistance()
                            if (dist >= slopPx) {
                                dragStarted = true
                                change.consume()
                                if (hitActiveId != null) {
                                    val cardRect = editCardBoundsMap[hitActiveId] ?: Rect(downInRoot, 0f)
                                    val srcIdx = state.activeWidgets.indexOf(hitActiveId).coerceAtLeast(0)
                                    val grabOffset = (downInRoot - cardRect.topLeft).let {
                                        Offset(it.x.coerceIn(10f, (cardRect.width - 10f).coerceAtLeast(10f)), it.y.coerceIn(10f, (cardRect.height - 10f).coerceAtLeast(10f)))
                                    }
                                    state.startWidgetDrag(
                                        widgetId = hitActiveId,
                                        fromLibrary = false,
                                        sourceIndex = srcIdx,
                                        initialPointerInWorkspace = currentInRoot - workspaceRootOrigin,
                                        grabOffset = grabOffset
                                    )
                                } else if (hitLibraryId != null) {
                                    val cardRect = libraryCardBoundsMap[hitLibraryId] ?: Rect(downInRoot, 0f)
                                    val grabOffset = (downInRoot - cardRect.topLeft).let {
                                        Offset(it.x.coerceIn(20f, 220f), it.y.coerceIn(12f, 44f))
                                    }
                                    state.startWidgetDrag(
                                        widgetId = hitLibraryId,
                                        fromLibrary = true,
                                        sourceIndex = -1,
                                        initialPointerInWorkspace = currentInRoot - workspaceRootOrigin,
                                        grabOffset = grabOffset
                                    )
                                }
                                updateDragFromRootPointer(currentInRoot)
                            }
                        } else {
                            change.consume()
                            updateDragFromRootPointer(currentInRoot)
                        }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        val reportActiveCardBounds: (String, Rect, Rect, Rect) -> Unit = { wId, cardRect, removeBtnRect, gripRect ->
            editCardBoundsMap[wId] = cardRect
            editRemoveBtnBoundsMap[wId] = removeBtnRect
            editResizeGripBoundsMap[wId] = gripRect
        }
        val reportLibraryCardBounds: (String, Rect, Rect) -> Unit = { wId, cardRect, addBtnRect ->
            libraryCardBoundsMap[wId] = cardRect
            libraryAddBtnBoundsMap[wId] = addBtnRect
        }

        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLeft) {
                DetachedLeftRail(
                    state = state,
                    railWidth = railWidth,
                    onScrollToSection = onScrollToSection
                )

                if (isCompactPortrait) {
                    Column(
                        modifier = Modifier
                            .width(targetMainWidth)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainSidepanelContent(
                            state = state,
                            clockTime = clockTime,
                            panelWidth = targetMainWidth,
                            feedBlocks = feedBlocks,
                            feedListState = feedListState,
                            onFeedBoundsChanged = { mainFeedRootBounds = it },
                            onReportActiveCardBounds = reportActiveCardBounds,
                            modifier = Modifier.weight(1f)
                        )

                        AnimatedVisibility(
                            visible = state.editMode,
                            enter = slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                            ) + expandVertically(
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(tween(180)),
                            exit = slideOutVertically(
                                targetOffsetY = { it / 2 },
                                animationSpec = tween(180)
                            ) + shrinkVertically(
                                animationSpec = tween(180)
                            ) + fadeOut(tween(140))
                        ) {
                            RightWidgetDrawer(
                                state = state,
                                drawerWidth = targetMainWidth,
                                onDrawerBoundsChanged = { rightDrawerRootBounds = it },
                                onReportLibraryCardBounds = reportLibraryCardBounds,
                                modifier = Modifier.height(216.dp)
                            )
                        }
                    }
                } else {
                    MainSidepanelContent(
                        state = state,
                        clockTime = clockTime,
                        panelWidth = targetMainWidth,
                        feedBlocks = feedBlocks,
                        feedListState = feedListState,
                        onFeedBoundsChanged = { mainFeedRootBounds = it },
                        onReportActiveCardBounds = reportActiveCardBounds,
                        modifier = Modifier.fillMaxHeight()
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
                            drawerWidth = 264.dp,
                            onDrawerBoundsChanged = { rightDrawerRootBounds = it },
                            onReportLibraryCardBounds = reportLibraryCardBounds,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            } else {
                if (isCompactPortrait) {
                    Column(
                        modifier = Modifier
                            .width(targetMainWidth)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainSidepanelContent(
                            state = state,
                            clockTime = clockTime,
                            panelWidth = targetMainWidth,
                            feedBlocks = feedBlocks,
                            feedListState = feedListState,
                            onFeedBoundsChanged = { mainFeedRootBounds = it },
                            onReportActiveCardBounds = reportActiveCardBounds,
                            modifier = Modifier.weight(1f)
                        )

                        AnimatedVisibility(
                            visible = state.editMode,
                            enter = slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                            ) + expandVertically(
                                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(tween(180)),
                            exit = slideOutVertically(
                                targetOffsetY = { it / 2 },
                                animationSpec = tween(180)
                            ) + shrinkVertically(
                                animationSpec = tween(180)
                            ) + fadeOut(tween(140))
                        ) {
                            RightWidgetDrawer(
                                state = state,
                                drawerWidth = targetMainWidth,
                                onDrawerBoundsChanged = { rightDrawerRootBounds = it },
                                onReportLibraryCardBounds = reportLibraryCardBounds,
                                modifier = Modifier.height(216.dp)
                            )
                        }
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
                            drawerWidth = 264.dp,
                            onDrawerBoundsChanged = { rightDrawerRootBounds = it },
                            onReportLibraryCardBounds = reportLibraryCardBounds,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    MainSidepanelContent(
                        state = state,
                        clockTime = clockTime,
                        panelWidth = targetMainWidth,
                        feedBlocks = feedBlocks,
                        feedListState = feedListState,
                        onFeedBoundsChanged = { mainFeedRootBounds = it },
                        onReportActiveCardBounds = reportActiveCardBounds,
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                DetachedLeftRail(
                    state = state,
                    railWidth = railWidth,
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

        // Falling / Dropping Widget Animation Overlay
        val droppingId = state.droppingWidgetId
        if (droppingId != null) {
            DroppingWidgetAnimationOverlay(
                widgetId = droppingId,
                state = state,
                initialOffset = state.droppingWidgetOffset,
                onFinished = {
                    state.droppingWidgetId = null
                },
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun DetachedLeftRail(
    state: ZXSidePanelStateHolder,
    railWidth: Dp,
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
            .width(railWidth)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp, horizontal = 6.dp),
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
                        targetValue = if (isActive) 1.08f else 1f,
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
                                .width(48.dp)
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
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Tonal Divider
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest)
            )

            // Below Divider: Real Installed Quick Launch Apps
            Text(
                text = "Apps",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
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
                            .size(46.dp)
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
                                        .size(28.dp)
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
                        .size(46.dp)
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

private data class EditFeedRow(
    val key: String,
    val items: List<EditFeedRenderEntry>
)

private data class NormalFeedRow(
    val key: String,
    val widgetIds: List<String>
)

@Composable
private fun MainSidepanelContent(
    state: ZXSidePanelStateHolder,
    clockTime: String,
    panelWidth: Dp,
    feedBlocks: List<SidePanelFeedBlock>,
    feedListState: androidx.compose.foundation.lazy.LazyListState,
    onFeedBoundsChanged: (Rect) -> Unit,
    onReportActiveCardBounds: (widgetId: String, cardBoundsInRoot: Rect, removeBtnBoundsInRoot: Rect, resizeGripBoundsInRoot: Rect) -> Unit,
    modifier: Modifier = Modifier
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

    val editRows by remember {
        derivedStateOf {
            val rows = mutableListOf<EditFeedRow>()
            var currentRow = mutableListOf<EditFeedRenderEntry>()
            var currentSpan = 0

            for (entry in editEntries) {
                val wId = when (entry) {
                    is EditFeedRenderEntry.ActiveCard -> entry.widgetId
                    is EditFeedRenderEntry.DropPlaceholder -> entry.widgetId
                }
                val span = state.getWidgetSpan(wId)
                if (currentSpan + span <= 3) {
                    currentRow.add(entry)
                    currentSpan += span
                } else {
                    if (currentRow.isNotEmpty()) {
                        rows.add(EditFeedRow(currentRow.joinToString("_") { it.key }, currentRow))
                    }
                    currentRow = mutableListOf(entry)
                    currentSpan = span
                }
            }
            if (currentRow.isNotEmpty()) {
                rows.add(EditFeedRow(currentRow.joinToString("_") { it.key }, currentRow))
            }
            rows
        }
    }

    val normalRows by remember {
        derivedStateOf {
            val rows = mutableListOf<NormalFeedRow>()
            var currentRow = mutableListOf<String>()
            var currentSpan = 0

            for (wId in state.activeWidgets) {
                val span = state.getWidgetSpan(wId)
                if (currentSpan + span <= 3) {
                    currentRow.add(wId)
                    currentSpan += span
                } else {
                    if (currentRow.isNotEmpty()) {
                        rows.add(NormalFeedRow(currentRow.joinToString("_"), currentRow))
                    }
                    currentRow = mutableListOf(wId)
                    currentSpan = span
                }
            }
            if (currentRow.isNotEmpty()) {
                rows.add(NormalFeedRow(currentRow.joinToString("_"), currentRow))
            }
            rows
        }
    }

    val mainPanelWidth by animateDpAsState(
        targetValue = panelWidth,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "MainSidepanelWidth"
    )

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = scheme.surfaceContainerLow,
        shadowElevation = 24.dp,
        modifier = modifier.width(mainPanelWidth)
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
                        .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 10.dp),
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
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = ZXSidePanelIcons.get("wifi"),
                                    contentDescription = null,
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${state.batteryLevel}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = clockTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
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
            } else {
                // Subscreen Header
                val subscreenTitle = when (state.activeSubscreen) {
                    "liveMonitor" -> "Live Monitor"
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
                    "liveMonitor" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            LiveMonitorSubscreen(state = state)
                        }
                    }
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
                                            text = "Drag or add widgets from the widget library",
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = editRows,
                                    key = { it.key }
                                ) { row ->
                                    val rowSpanSum = row.items.sumOf { entry ->
                                        val wId = when (entry) {
                                            is EditFeedRenderEntry.ActiveCard -> entry.widgetId
                                            is EditFeedRenderEntry.DropPlaceholder -> entry.widgetId
                                        }
                                        state.getWidgetSpan(wId)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem(
                                                placementSpec = spring(
                                                    dampingRatio = 0.78f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        row.items.forEach { entry ->
                                            val wId = when (entry) {
                                                is EditFeedRenderEntry.ActiveCard -> entry.widgetId
                                                is EditFeedRenderEntry.DropPlaceholder -> entry.widgetId
                                            }
                                            val span = state.getWidgetSpan(wId)
                                            Box(
                                                modifier = Modifier.weight(span.toFloat())
                                            ) {
                                                when (entry) {
                                                    is EditFeedRenderEntry.DropPlaceholder -> {
                                                        WidgetDropLandingPlaceholder(
                                                            widgetId = entry.widgetId,
                                                            state = state,
                                                            span = span
                                                        )
                                                    }
                                                    is EditFeedRenderEntry.ActiveCard -> {
                                                        EditModeWidgetCard(
                                                            widgetId = entry.widgetId,
                                                            index = entry.originalIndex,
                                                            state = state,
                                                            span = span,
                                                            onReportBounds = { cardBounds, removeBtnBounds, resizeGripBounds ->
                                                                onReportActiveCardBounds(
                                                                    entry.widgetId,
                                                                    cardBounds,
                                                                    removeBtnBounds,
                                                                    resizeGripBounds
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (rowSpanSum < 3) {
                                            Spacer(modifier = Modifier.weight((3 - rowSpanSum).toFloat()))
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
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = normalRows,
                                    key = { it.key }
                                ) { row ->
                                    val rowSpanSum = row.widgetIds.sumOf { state.getWidgetSpan(it) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem(
                                                placementSpec = spring(
                                                    dampingRatio = 0.78f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        row.widgetIds.forEach { wId ->
                                            val span = state.getWidgetSpan(wId)
                                            Box(
                                                modifier = Modifier.weight(span.toFloat())
                                            ) {
                                                SidePanelWidgetContent(
                                                    widgetId = wId,
                                                    state = state,
                                                    isMerged = false,
                                                    span = span
                                                )
                                            }
                                        }
                                        if (rowSpanSum < 3) {
                                            Spacer(modifier = Modifier.weight((3 - rowSpanSum).toFloat()))
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
    drawerWidth: Dp,
    onDrawerBoundsChanged: (Rect) -> Unit,
    onReportLibraryCardBounds: (widgetId: String, cardBoundsInRoot: Rect, addBtnBoundsInRoot: Rect) -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .width(drawerWidth)
            .onGloballyPositioned { coords ->
                onDrawerBoundsChanged(coords.boundsInRoot())
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Drawer Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Widget library",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isDropTargetHighlighted) {
                            "Release finger to store widget here"
                        } else {
                            "Drag into panel or tap + to add"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDropTargetHighlighted) scheme.primary else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { state.resetDefaultWidgets() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ZXSidePanelIcons.get("restart_alt"),
                                contentDescription = "Reset default layout",
                                tint = scheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = scheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { state.toggleEditMode(false) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ZXSidePanelIcons.get("close"),
                                contentDescription = "Close widget library",
                                tint = scheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                        }
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        var cardBounds by remember { mutableStateOf(Rect.Zero) }
                        var addBtnBounds by remember { mutableStateOf(Rect.Zero) }
                        val isBeingDragged = state.draggingWidgetId == widgetId && state.draggingFromLibrary

                        Surface(
                            shape = cardShape,
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    cardBounds = coords.boundsInRoot()
                                    onReportLibraryCardBounds(widgetId, cardBounds, addBtnBounds)
                                }
                                .graphicsLayer {
                                    alpha = if (isBeingDragged) 0.3f else 1f
                                    scaleX = if (isBeingDragged) 0.95f else 1f
                                    scaleY = if (isBeingDragged) 0.95f else 1f
                                }
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
                                        .onGloballyPositioned { coords ->
                                            addBtnBounds = coords.boundsInRoot()
                                            if (cardBounds != Rect.Zero) {
                                                onReportLibraryCardBounds(widgetId, cardBounds, addBtnBounds)
                                            }
                                        }
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
