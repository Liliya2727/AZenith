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

package zx.azenith.ui.gamespace

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.ui.theme.AZenithTheme
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

enum class FloatingSizeState {
    LARGE,
    NORMAL,
    SMALL,
    MINI,
    MINIMIZED
}

class ZXFloatingAppSession(
    val sessionId: Int,
    val packageName: String,
    val iconBitmap: ImageBitmap?,
    initialSizeState: FloatingSizeState = FloatingSizeState.NORMAL,
    initialX: Int = 48,
    initialY: Int = 96,
    initialWidthPx: Int = 540,
    initialHeightPx: Int = 920
) {
    var sizeState by mutableStateOf(initialSizeState)
    var windowX by mutableIntStateOf(initialX)
    var windowY by mutableIntStateOf(initialY)
    var widthPx by mutableIntStateOf(initialWidthPx)
    var heightPx by mutableIntStateOf(initialHeightPx)
    var isResizing by mutableStateOf(false)
    var isDragging by mutableStateOf(false)
    var isPendingMinimizeToIcon by mutableStateOf(false)
    var resizeModeHint by mutableStateOf<String?>(null)
    var lastResizeModeKey: String? = null
    var lastResizeTier: FloatingSizeState? = null
    var hasLaunchedApp = false
    var lastActiveTimestampMs by mutableLongStateOf(System.currentTimeMillis())
    var isClosing: Boolean = false

    var virtualDisplayId: Int = -1
    var localVirtualDisplay: VirtualDisplay? = null
    var renderSurface: Surface? = null
    var hostView: FrameLayout? = null
    var layoutParams: WindowManager.LayoutParams? = null
}

class ZXFloatingAppManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val scope: CoroutineScope,
    private val checkRootGranted: () -> Boolean,
    private val resolveAppIcon: (String) -> ImageBitmap?
) {
    companion object {
        private const val MAX_FLOATING_WINDOWS = 2
        private const val MAX_MINIMIZED_ICONS = 2
        const val VIRTUAL_PORTRAIT_WIDTH = 720
        const val VIRTUAL_PORTRAIT_HEIGHT = 1280
        const val VIRTUAL_PORTRAIT_DPI = 320
        const val PORTRAIT_ASPECT_RATIO = 9f / 16f

        private const val GESTURE_NONE = 0
        private const val GESTURE_TOPBAR_PENDING = 1
        private const val GESTURE_TOPBAR_DRAG = 2
        private const val GESTURE_MINI_MOVE = 3
        private const val GESTURE_RESIZE_LEFT = 4
        private const val GESTURE_RESIZE_RIGHT = 5
    }

    init {
        prebindRootServiceIfPossible()
    }

    val sessions = mutableStateListOf<ZXFloatingAppSession>()

    var stackSide by mutableStateOf("right")
    var stackY by mutableIntStateOf(260)
    var isStackExpanded by mutableStateOf(false)
    var isHostForegroundPackage: ((String) -> Boolean)? = null

    private var nextSessionId = 1
    private var rootClient: ZXFloatingDisplayRootClient? = null
    private var rootBindingInFlight = false
    private var minimizedStackHostView: FrameLayout? = null
    private var minimizedStackParams: WindowManager.LayoutParams? = null
    private var snapAnimator: ValueAnimator? = null

    private val rootConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            rootBindingInFlight = false
            if (service != null) {
                rootClient = ZXFloatingDisplayRootClient(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rootBindingInFlight = false
            rootClient = null
        }
    }

    fun isPackageHosted(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        return sessions.any { it.packageName == pkg }
    }

    fun prebindRootServiceIfPossible() {
        scope.launch(Dispatchers.IO) {
            if (checkRootGranted()) {
                withContext(Dispatchers.Main) {
                    bindRootServiceIfNeeded()
                }
            }
        }
    }

    private fun bindRootServiceIfNeeded() {
        if (rootClient?.isBinderAlive == true || rootBindingInFlight) return
        rootBindingInFlight = true
        runCatching {
            val intent = Intent(context, ZXFloatingAppRootService::class.java)
            RootService.bind(intent, rootConnection)
        }.onFailure {
            rootBindingInFlight = false
        }
    }

    fun openFloatingApp(pkg: String) {
        if (pkg.isBlank() || pkg == context.packageName || isHostForegroundPackage?.invoke(pkg) == true) return
        scope.launch(Dispatchers.Main) {
            val existing = sessions.firstOrNull { it.packageName == pkg }
            if (existing != null) {
                if (existing.sizeState == FloatingSizeState.MINIMIZED) {
                    restoreSessionFromIcon(existing, targetSize = FloatingSizeState.NORMAL)
                } else {
                    if (existing.sizeState == FloatingSizeState.MINI) {
                        applySizePreset(existing, FloatingSizeState.NORMAL)
                    }
                    bringSessionToFront(existing)
                }
                return@launch
            }

            enforceWindowCapacityBeforeAdding()

            val (normW, normH) = computeDimensionsForState(FloatingSizeState.NORMAL)
            val dm = context.resources.displayMetrics
            val openWindowsCount = sessions.count { it.sizeState != FloatingSizeState.MINIMIZED }
            val cascadeOffset = (openWindowsCount * (26 * dm.density)).roundToInt()
            val startX = (((dm.widthPixels - normW) / 2) + cascadeOffset)
                .coerceIn(12, (dm.widthPixels - normW - 12).coerceAtLeast(12))
            val startY = (((dm.heightPixels - normH) / 2) + cascadeOffset)
                .coerceIn(12, (dm.heightPixels - normH - 12).coerceAtLeast(12))

            val session = ZXFloatingAppSession(
                sessionId = nextSessionId++,
                packageName = pkg,
                iconBitmap = resolveAppIcon(pkg),
                initialSizeState = FloatingSizeState.NORMAL,
                initialX = startX,
                initialY = startY,
                initialWidthPx = normW,
                initialHeightPx = normH
            )
            sessions.add(session)
            attachSessionWindow(session)
            syncMinimizedStackWindow()
        }
    }

    private fun enforceWindowCapacityBeforeAdding() {
        val activeWindows = sessions
            .filter { it.sizeState != FloatingSizeState.MINIMIZED }
            .sortedBy { it.lastActiveTimestampMs }

        if (activeWindows.size >= MAX_FLOATING_WINDOWS) {
            val oldestWindow = activeWindows.first()
            minimizeSessionToIcon(oldestWindow)
        }
    }

    fun minimizeSessionToIcon(session: ZXFloatingAppSession) {
        val currentMinimized = sessions
            .filter { it.sizeState == FloatingSizeState.MINIMIZED && it.sessionId != session.sessionId }
            .sortedBy { it.lastActiveTimestampMs }

        // Enforce max 2 minimized icons at once
        if (currentMinimized.size >= MAX_MINIMIZED_ICONS) {
            val oldestMin = currentMinimized.first()
            closeSession(oldestMin)
        }

        // First minimized icon determines initial stack side; second icon automatically follows it
        if (currentMinimized.isEmpty()) {
            val dm = context.resources.displayMetrics
            val windowCenterX = session.windowX + (session.widthPx / 2)
            stackSide = if (windowCenterX < dm.widthPixels / 2) "left" else "right"
            stackY = session.windowY.coerceIn(
                (48 * dm.density).roundToInt(),
                (dm.heightPixels - (96 * dm.density).roundToInt()).coerceAtLeast((48 * dm.density).roundToInt())
            )
        }

        isStackExpanded = false
        session.sizeState = FloatingSizeState.MINIMIZED
        session.lastActiveTimestampMs = System.currentTimeMillis()
        updateSessionWindowLayout(session)
        syncMinimizedStackWindow()
    }

    fun restoreSessionFromIcon(
        session: ZXFloatingAppSession,
        targetSize: FloatingSizeState = FloatingSizeState.SMALL
    ) {
        val otherActiveWindows = sessions
            .filter { it.sizeState != FloatingSizeState.MINIMIZED && it.sessionId != session.sessionId }
            .sortedBy { it.lastActiveTimestampMs }

        // Maintain max 2 floating windows by swapping oldest open window into the icon stack
        if (otherActiveWindows.size >= MAX_FLOATING_WINDOWS) {
            val oldestOpen = otherActiveWindows.first()
            oldestOpen.sizeState = FloatingSizeState.MINIMIZED
            oldestOpen.lastActiveTimestampMs = System.currentTimeMillis()
            updateSessionWindowLayout(oldestOpen)
        }

        isStackExpanded = false
        val (targetW, targetH) = computeDimensionsForState(targetSize)
        val dm = context.resources.displayMetrics
        session.sizeState = targetSize
        session.widthPx = targetW
        session.heightPx = targetH
        session.windowX = session.windowX.coerceIn(8, (dm.widthPixels - targetW - 8).coerceAtLeast(8))
        session.windowY = session.windowY.coerceIn(8, (dm.heightPixels - targetH - 8).coerceAtLeast(8))
        session.lastActiveTimestampMs = System.currentTimeMillis()

        updateSessionWindowLayout(session)
        bringSessionToFront(session)
        val displayId = session.virtualDisplayId
        if (displayId > 0 && !session.hasLaunchedApp) {
            session.hasLaunchedApp = true
            rootClient?.launchAppOnDisplay(session.packageName, displayId)
        }
        syncMinimizedStackWindow()
    }

    fun expandSessionToFullscreen(session: ZXFloatingAppSession) {
        val pkg = session.packageName
        closeSession(session)
        scope.launch(Dispatchers.Main) {
            delay(80)
            runCatching {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (launchIntent != null) {
                    val opts = ActivityOptions.makeBasic().apply {
                        launchDisplayId = 0
                    }
                    context.startActivity(launchIntent, opts.toBundle())
                }
            }
        }
    }

    fun closeSession(session: ZXFloatingAppSession) {
        session.isClosing = true
        sessions.remove(session)
        releaseSessionDisplay(session)
        session.hostView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        session.hostView = null
        session.layoutParams = null
        if (sessions.count { it.sizeState == FloatingSizeState.MINIMIZED } <= 1) {
            isStackExpanded = false
        }
        syncMinimizedStackWindow()
    }

    fun destroyAllSessions() {
        snapAnimator?.cancel()
        val copy = sessions.toList()
        sessions.clear()
        copy.forEach { session ->
            releaseSessionDisplay(session)
            session.hostView?.let { view ->
                runCatching { windowManager.removeView(view) }
            }
            session.hostView = null
            session.layoutParams = null
        }
        minimizedStackHostView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        minimizedStackHostView = null
        minimizedStackParams = null
    }

    fun onConfigurationChanged() {
        val dm = context.resources.displayMetrics
        sessions.forEach { session ->
            if (session.sizeState != FloatingSizeState.MINIMIZED) {
                val (w, h) = computeDimensionsForState(session.sizeState)
                session.widthPx = w
                session.heightPx = h
                session.windowX = session.windowX.coerceIn(8, (dm.widthPixels - w - 8).coerceAtLeast(8))
                session.windowY = session.windowY.coerceIn(8, (dm.heightPixels - h - 8).coerceAtLeast(8))
                updateSessionWindowLayout(session)
            }
        }
        snapMinimizedStackToEdge(animate = false)
    }

    fun computeDimensionsForState(state: FloatingSizeState): Pair<Int, Int> {
        val dm = context.resources.displayMetrics
        val density = dm.density
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels
        val isLandscapeScreen = screenW > screenH

        val availW = (screenW - (24 * density)).coerceAtLeast(200 * density)
        val availH = (screenH - (24 * density)).coerceAtLeast(200 * density)

        val borderPx = (4f * density).roundToInt()
        val topBarPx = if (state == FloatingSizeState.MINI) 0 else (34f * density).roundToInt()

        val contentH = when (state) {
            FloatingSizeState.MINI -> {
                if (isLandscapeScreen) {
                    (availH * 0.36f).coerceIn(140 * density, 220 * density)
                } else {
                    ((availW * 0.36f) / PORTRAIT_ASPECT_RATIO).coerceIn(140 * density, 220 * density)
                }
            }
            FloatingSizeState.SMALL -> {
                if (isLandscapeScreen) {
                    (availH * 0.52f).coerceIn(200 * density, 340 * density)
                } else {
                    ((availW * 0.52f) / PORTRAIT_ASPECT_RATIO).coerceIn(200 * density, 350 * density)
                }
            }
            FloatingSizeState.NORMAL -> {
                if (isLandscapeScreen) {
                    (availH * 0.72f).coerceIn(280 * density, 450 * density)
                } else {
                    ((availW * 0.70f) / PORTRAIT_ASPECT_RATIO).coerceIn(280 * density, 470 * density)
                }
            }
            FloatingSizeState.LARGE -> {
                if (isLandscapeScreen) {
                    (availH * 0.88f).coerceIn(340 * density, 550 * density)
                } else {
                    ((availW * 0.85f) / PORTRAIT_ASPECT_RATIO).coerceIn(340 * density, 570 * density)
                }
            }
            FloatingSizeState.MINIMIZED -> return Pair(1, 1)
        }

        val contentW = (contentH * PORTRAIT_ASPECT_RATIO).roundToInt()
        val totalW = contentW + borderPx
        val totalH = contentH.roundToInt() + borderPx + topBarPx

        return Pair(totalW, totalH)
    }

    fun applySizePreset(session: ZXFloatingAppSession, targetState: FloatingSizeState) {
        if (targetState == FloatingSizeState.MINIMIZED) {
            minimizeSessionToIcon(session)
            return
        }
        val dm = context.resources.displayMetrics
        val (newW, newH) = computeDimensionsForState(targetState)
        val centerX = session.windowX + session.widthPx / 2
        session.sizeState = targetState
        session.widthPx = newW
        session.heightPx = newH
        session.windowX = (centerX - newW / 2).coerceIn(6, (dm.widthPixels - newW - 6).coerceAtLeast(6))
        session.windowY = session.windowY.coerceIn(6, (dm.heightPixels - newH - 6).coerceAtLeast(6))
        updateSessionWindowLayout(session)
    }

    fun bringSessionToFront(session: ZXFloatingAppSession) {
        session.lastActiveTimestampMs = System.currentTimeMillis()
        if (sessions.contains(session)) {
            sessions.remove(session)
            sessions.add(session)
        }
        val host = session.hostView ?: return
        val lp = session.layoutParams ?: return
        runCatching {
            windowManager.updateViewLayout(host, lp)
        }
    }

    private fun attachSessionWindow(session: ZXFloatingAppSession) {
        val lp = WindowManager.LayoutParams(
            session.widthPx,
            session.heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = session.windowX
            y = session.windowY
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            setContent {
                AZenithTheme {
                    FloatingAppWindowContent(
                        session = session,
                        manager = this@ZXFloatingAppManager
                    )
                }
            }
        }

        val container = object : FrameLayout(context) {
            private var gestureMode = GESTURE_NONE
            private var downRawX = 0f
            private var downRawY = 0f
            private var startWinX = 0
            private var startWinY = 0
            private var startWinW = 0
            private var startWinH = 0
            private val density = resources.displayMetrics.density
            private val touchSlopPx = 8f * density

            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                if (session.sizeState == FloatingSizeState.MINIMIZED) {
                    return false
                }
                val cornerZonePx = 30f * density
                val topBarHeightPx = 34f * density

                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        session.lastActiveTimestampMs = System.currentTimeMillis()
                        bringSessionToFront(session)
                        downRawX = ev.rawX
                        downRawY = ev.rawY
                        startWinX = session.windowX
                        startWinY = session.windowY
                        startWinW = session.widthPx
                        startWinH = session.heightPx

                        val lx = ev.x
                        val ly = ev.y
                        val w = width.toFloat().coerceAtLeast(1f)
                        val h = height.toFloat().coerceAtLeast(1f)

                        if (ly >= h - cornerZonePx && lx <= cornerZonePx) {
                            gestureMode = GESTURE_RESIZE_LEFT
                            session.isResizing = true
                            return true
                        } else if (ly >= h - cornerZonePx && lx >= w - cornerZonePx) {
                            gestureMode = GESTURE_RESIZE_RIGHT
                            session.isResizing = true
                            return true
                        } else if (session.sizeState == FloatingSizeState.MINI) {
                            // MINI mode contents are non-tappable; dragging anywhere moves the square box
                            gestureMode = GESTURE_MINI_MOVE
                            return true
                        } else if (ly <= topBarHeightPx) {
                            gestureMode = GESTURE_TOPBAR_PENDING
                            super.dispatchTouchEvent(ev)
                            return true
                        } else {
                            gestureMode = GESTURE_NONE
                            return super.dispatchTouchEvent(ev)
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = ev.rawX - downRawX
                        val dy = ev.rawY - downRawY

                        when (gestureMode) {
                            GESTURE_RESIZE_LEFT -> {
                                resizeSessionAbsolute(
                                    session = session,
                                    startX = startWinX,
                                    startY = startWinY,
                                    startW = startWinW,
                                    startH = startWinH,
                                    dxPx = dx,
                                    dyPx = dy,
                                    fromLeftCorner = true
                                )
                                return true
                            }
                            GESTURE_RESIZE_RIGHT -> {
                                resizeSessionAbsolute(
                                    session = session,
                                    startX = startWinX,
                                    startY = startWinY,
                                    startW = startWinW,
                                    startH = startWinH,
                                    dxPx = dx,
                                    dyPx = dy,
                                    fromLeftCorner = false
                                )
                                return true
                            }
                            GESTURE_MINI_MOVE -> {
                                if (!session.isDragging && hypot(dx, dy) > touchSlopPx) {
                                    session.isDragging = true
                                }
                                if (session.isDragging) {
                                    moveSessionAbsolute(
                                        session = session,
                                        targetX = startWinX + dx.roundToInt(),
                                        targetY = startWinY + dy.roundToInt()
                                    )
                                }
                                return true
                            }
                            GESTURE_TOPBAR_PENDING -> {
                                if (hypot(dx, dy) > touchSlopPx) {
                                    val cancelEv = MotionEvent.obtain(ev).apply {
                                        action = MotionEvent.ACTION_CANCEL
                                    }
                                    super.dispatchTouchEvent(cancelEv)
                                    gestureMode = GESTURE_TOPBAR_DRAG
                                    session.isDragging = true
                                    moveSessionAbsolute(
                                        session = session,
                                        targetX = startWinX + dx.roundToInt(),
                                        targetY = startWinY + dy.roundToInt()
                                    )
                                } else {
                                    super.dispatchTouchEvent(ev)
                                }
                                return true
                            }
                            GESTURE_TOPBAR_DRAG -> {
                                moveSessionAbsolute(
                                    session = session,
                                    targetX = startWinX + dx.roundToInt(),
                                    targetY = startWinY + dy.roundToInt()
                                )
                                return true
                            }
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        when (gestureMode) {
                            GESTURE_RESIZE_LEFT, GESTURE_RESIZE_RIGHT -> {
                                session.isResizing = false
                                gestureMode = GESTURE_NONE
                                if (ev.action == MotionEvent.ACTION_CANCEL) {
                                    session.isPendingMinimizeToIcon = false
                                }
                                settleSessionResize(session)
                                return true
                            }
                            GESTURE_MINI_MOVE -> {
                                session.isDragging = false
                                gestureMode = GESTURE_NONE
                                return true
                            }
                            GESTURE_TOPBAR_DRAG -> {
                                session.isDragging = false
                                gestureMode = GESTURE_NONE
                                return true
                            }
                            GESTURE_TOPBAR_PENDING -> {
                                gestureMode = GESTURE_NONE
                                super.dispatchTouchEvent(ev)
                                return true
                            }
                        }
                    }
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            elevation = 0f
            outlineProvider = null
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            addView(
                composeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        session.hostView = container
        session.layoutParams = lp
        runCatching {
            windowManager.addView(container, lp)
        }
    }

    fun updateSessionWindowLayout(session: ZXFloatingAppSession) {
        val host = session.hostView ?: return
        val lp = session.layoutParams ?: return
        if (session.sizeState == FloatingSizeState.MINIMIZED) {
            // Keep 1x1 invisible surface alive so TextureView & VirtualDisplay are never destroyed while minimized
            lp.width = 1
            lp.height = 1
            lp.x = -10
            lp.y = -10
            lp.alpha = 0f
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            lp.width = session.widthPx
            lp.height = session.heightPx
            lp.x = session.windowX
            lp.y = session.windowY
            lp.alpha = 1f
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
        }
        runCatching {
            windowManager.updateViewLayout(host, lp)
        }
    }

    fun moveSessionAbsolute(session: ZXFloatingAppSession, targetX: Int, targetY: Int) {
        val dm = context.resources.displayMetrics
        val maxX = (dm.widthPixels - session.widthPx - 6).coerceAtLeast(6)
        val maxY = (dm.heightPixels - session.heightPx - 6).coerceAtLeast(6)
        val clampedX = targetX.coerceIn(6, maxX)
        val clampedY = targetY.coerceIn(6, maxY)
        if (clampedX != session.windowX || clampedY != session.windowY) {
            session.windowX = clampedX
            session.windowY = clampedY
            updateSessionWindowLayout(session)
        }
    }

    fun resizeSessionAbsolute(
        session: ZXFloatingAppSession,
        startX: Int,
        startY: Int,
        startW: Int,
        startH: Int,
        dxPx: Float,
        dyPx: Float,
        fromLeftCorner: Boolean
    ) {
        val dm = context.resources.displayMetrics
        val density = dm.density
        val (_, miniH) = computeDimensionsForState(FloatingSizeState.MINI)
        val (_, smallH) = computeDimensionsForState(FloatingSizeState.SMALL)
        val (_, normalH) = computeDimensionsForState(FloatingSizeState.NORMAL)
        val (_, largeH) = computeDimensionsForState(FloatingSizeState.LARGE)

        val signedDx = if (fromLeftCorner) -dxPx else dxPx
        val deltaH = (signedDx * 0.45f + dyPx * 0.85f).roundToInt()
        val rawTargetH = startH + deltaH
        val minAllowableH = (miniH * 0.70f).roundToInt()
        val maxAllowableH = (largeH * 1.15f).roundToInt()
        val targetH = rawTargetH.coerceIn(minAllowableH, maxAllowableH)

        // Shrinking past 86% of mini height visually indicates intent to minimize to icon.
        // It does NOT minimize immediately: it displays the icon preview.
        // If user enlarges it again, isPendingMinimizeToIcon cancels!
        val iconThresholdH = (miniH * 0.86f).roundToInt()
        val isShrunkToIcon = targetH < iconThresholdH
        session.isPendingMinimizeToIcon = isShrunkToIcon

        // Smooth continuous scaling during drag (discrete magnetic snap commits on release)
        val effectiveH = targetH
        val targetTier = if (isShrunkToIcon) FloatingSizeState.MINI else resolveNearestSizeTier(effectiveH)

        // Haptic tick feedback when snapping between mode boundaries
        val currentModeKey = if (isShrunkToIcon) "ICON" else targetTier.name
        if (session.lastResizeModeKey != currentModeKey) {
            session.lastResizeModeKey = currentModeKey
            session.hostView?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        session.resizeModeHint = if (isShrunkToIcon) {
            "Release to minimize"
        } else {
            when (targetTier) {
                FloatingSizeState.MINI -> "Mini window"
                FloatingSizeState.SMALL -> "Small window"
                FloatingSizeState.NORMAL -> "Standard window"
                FloatingSizeState.LARGE -> "Expanded window"
                else -> "Floating window"
            }
        }

        val borderPx = (4f * density).roundToInt()
        val topBarPx = if (targetTier == FloatingSizeState.MINI || isShrunkToIcon) 0 else (34f * density).roundToInt()
        val contentH = (effectiveH - borderPx - topBarPx).coerceAtLeast((72 * density).roundToInt())
        val contentW = (contentH * PORTRAIT_ASPECT_RATIO).roundToInt()
        val targetW = contentW + borderPx

        session.widthPx = targetW
        session.heightPx = effectiveH

        session.windowX = if (fromLeftCorner) {
            (startX + (startW - targetW)).coerceIn(6, (dm.widthPixels - targetW - 6).coerceAtLeast(6))
        } else {
            startX.coerceIn(6, (dm.widthPixels - targetW - 6).coerceAtLeast(6))
        }
        session.windowY = startY.coerceIn(6, (dm.heightPixels - effectiveH - 6).coerceAtLeast(6))
        session.sizeState = targetTier
        updateSessionWindowLayout(session)
    }

    private var lastModeToast: Toast? = null

    private fun showModeToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            lastModeToast?.cancel()
            val t = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            lastModeToast = t
            t.show()
        }
    }

    fun settleSessionResize(session: ZXFloatingAppSession) {
        val willMinimize = session.isPendingMinimizeToIcon
        session.isPendingMinimizeToIcon = false
        session.resizeModeHint = null
        session.lastResizeModeKey = null

        val hapticConstant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        session.hostView?.performHapticFeedback(hapticConstant)

        if (willMinimize) {
            showModeToast("Minimized to icon")
            minimizeSessionToIcon(session)
        } else {
            val settledTier = resolveNearestSizeTier(session.heightPx)
            applySizePreset(session, settledTier)
            val label = when (settledTier) {
                FloatingSizeState.MINI -> "Mini window"
                FloatingSizeState.SMALL -> "Small window"
                FloatingSizeState.NORMAL -> "Standard window"
                FloatingSizeState.LARGE -> "Expanded window"
                else -> "Floating window"
            }
            showModeToast(label)
        }
    }

    private fun resolveNearestSizeTier(currentHeightPx: Int): FloatingSizeState {
        val (_, miniH) = computeDimensionsForState(FloatingSizeState.MINI)
        val (_, smallH) = computeDimensionsForState(FloatingSizeState.SMALL)
        val (_, normalH) = computeDimensionsForState(FloatingSizeState.NORMAL)
        val (_, largeH) = computeDimensionsForState(FloatingSizeState.LARGE)

        val midMiniSmall = (miniH + smallH) / 2
        val midSmallNormal = (smallH + normalH) / 2
        val midNormalLarge = (normalH + largeH) / 2

        return when {
            currentHeightPx < midMiniSmall -> FloatingSizeState.MINI
            currentHeightPx < midSmallNormal -> FloatingSizeState.SMALL
            currentHeightPx < midNormalLarge -> FloatingSizeState.NORMAL
            else -> FloatingSizeState.LARGE
        }
    }

    fun bindSessionSurface(session: ZXFloatingAppSession, surfaceTexture: SurfaceTexture) {
        surfaceTexture.setDefaultBufferSize(VIRTUAL_PORTRAIT_WIDTH, VIRTUAL_PORTRAIT_HEIGHT)
        val surface = Surface(surfaceTexture)
        session.renderSurface = surface

        scope.launch(Dispatchers.IO) {
            val hasRoot = checkRootGranted()
            if (hasRoot && rootClient == null) {
                withContext(Dispatchers.Main) {
                    bindRootServiceIfNeeded()
                }
                var waitTicks = 0
                while (rootClient == null && waitTicks < 35) {
                    delay(100)
                    waitTicks++
                }
            }

            val client = rootClient
            if (session.virtualDisplayId > 0) {
                if (client != null && client.isBinderAlive) {
                    client.setSurface(session.virtualDisplayId, surface)
                } else {
                    session.localVirtualDisplay?.surface = surface
                }
                return@launch
            }

            var createdDisplayId = -1
            if (client != null && client.isBinderAlive) {
                createdDisplayId = client.createVirtualDisplay(
                    name = "AZenithFloat_${session.sessionId}",
                    width = VIRTUAL_PORTRAIT_WIDTH,
                    height = VIRTUAL_PORTRAIT_HEIGHT,
                    dpi = VIRTUAL_PORTRAIT_DPI,
                    surface = surface
                )
                if (createdDisplayId > 0) {
                    session.virtualDisplayId = createdDisplayId
                    if (!session.hasLaunchedApp) {
                        session.hasLaunchedApp = true
                        client.launchAppOnDisplay(session.packageName, createdDisplayId)
                    }
                }
            }

            if (createdDisplayId <= 0) {
                withContext(Dispatchers.Main) {
                    runCatching {
                        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                        // Private virtual display with no OWN_CONTENT_ONLY restrictions
                        val vd = dm.createVirtualDisplay(
                            "AZenithFloat_${session.sessionId}",
                            VIRTUAL_PORTRAIT_WIDTH,
                            VIRTUAL_PORTRAIT_HEIGHT,
                            VIRTUAL_PORTRAIT_DPI,
                            surface,
                            0
                        )
                        if (vd != null) {
                            session.localVirtualDisplay = vd
                            val displayId = vd.display.displayId
                            session.virtualDisplayId = displayId
                            val launchIntent = context.packageManager
                                .getLaunchIntentForPackage(session.packageName)
                            val component = launchIntent?.component?.flattenToShortString()

                            if (component != null && hasRoot && !session.hasLaunchedApp) {
                                session.hasLaunchedApp = true
                                scope.launch(Dispatchers.IO) {
                                    runCatching {
                                        Shell.cmd(
                                            "cmd window set-ignore-orientation-request -d $displayId true",
                                            "cmd window set-fix-to-user-rotation -d $displayId enabled",
                                            "cmd window user-rotation -d $displayId lock 0",
                                            "cmd activity start-activity --user 0 --display $displayId --windowingMode 1 --activity-reorder-to-front -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10000000 -n $component"
                                        ).exec()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun unbindSessionSurface(session: ZXFloatingAppSession) {
        if (!session.isClosing && sessions.contains(session)) {
            return
        }
        val id = session.virtualDisplayId
        if (id > 0) {
            rootClient?.setSurface(id, null)
            runCatching { session.localVirtualDisplay?.surface = null }
        }
        runCatching { session.renderSurface?.release() }
        session.renderSurface = null
    }

    private fun releaseSessionDisplay(session: ZXFloatingAppSession) {
        val id = session.virtualDisplayId
        session.virtualDisplayId = -1
        if (id > 0) {
            rootClient?.releaseDisplay(id)
        }
        runCatching { session.localVirtualDisplay?.release() }
        session.localVirtualDisplay = null
        runCatching { session.renderSurface?.release() }
        session.renderSurface = null
    }

    fun dispatchTouchToVirtualDisplay(
        session: ZXFloatingAppSession,
        viewWidth: Int,
        viewHeight: Int,
        rawEvent: MotionEvent
    ) {
        if (session.sizeState == FloatingSizeState.MINI ||
            session.sizeState == FloatingSizeState.MINIMIZED
        ) {
            return
        }
        val displayId = session.virtualDisplayId
        val client = rootClient
        if (displayId <= 0 || client == null || !client.isBinderAlive) return

        val bufferW = VIRTUAL_PORTRAIT_WIDTH.toFloat()
        val bufferH = VIRTUAL_PORTRAIT_HEIGHT.toFloat()

        val scaleX = viewWidth.toFloat() / bufferW
        val scaleY = viewHeight.toFloat() / bufferH
        val scale = minOf(scaleX, scaleY).coerceAtLeast(0.001f)

        val contentW = bufferW * scale
        val contentH = bufferH * scale
        val offsetX = (viewWidth - contentW) / 2f
        val offsetY = (viewHeight - contentH) / 2f

        val pointerCount = rawEvent.pointerCount
        val props = Array(pointerCount) { idx ->
            MotionEvent.PointerProperties().also { rawEvent.getPointerProperties(idx, it) }
        }
        val coords = Array(pointerCount) { idx ->
            MotionEvent.PointerCoords().also {
                rawEvent.getPointerCoords(idx, it)
                val mappedX = ((rawEvent.getX(idx) - offsetX) / scale).coerceIn(0f, bufferW - 1f)
                val mappedY = ((rawEvent.getY(idx) - offsetY) / scale).coerceIn(0f, bufferH - 1f)
                it.x = mappedX
                it.y = mappedY
            }
        }

        val mapped = MotionEvent.obtain(
            rawEvent.downTime,
            rawEvent.eventTime,
            rawEvent.action,
            pointerCount,
            props,
            coords,
            rawEvent.metaState,
            rawEvent.buttonState,
            rawEvent.xPrecision,
            rawEvent.yPrecision,
            0,
            rawEvent.edgeFlags,
            InputDevice.SOURCE_TOUCHSCREEN,
            rawEvent.flags
        )
        try {
            if (client != null && client.isBinderAlive) {
                client.injectMotionEvent(displayId, mapped)
            } else {
                val actionStr = when (rawEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> "DOWN"
                    MotionEvent.ACTION_MOVE -> "MOVE"
                    MotionEvent.ACTION_UP -> "UP"
                    MotionEvent.ACTION_CANCEL -> "CANCEL"
                    else -> null
                }
                if (actionStr != null) {
                    val mappedX = coords[0].x.toInt()
                    val mappedY = coords[0].y.toInt()
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            Shell.cmd("input -d $displayId motionevent $actionStr $mappedX $mappedY").exec()
                        }
                    }
                }
            }
        } finally {
            runCatching { mapped.recycle() }
        }
    }

    fun dispatchBackToSession(session: ZXFloatingAppSession) {
        val displayId = session.virtualDisplayId
        if (displayId > 0) {
            val client = rootClient
            if (client != null && client.isBinderAlive) {
                client.injectBack(displayId)
            } else {
                scope.launch(Dispatchers.IO) {
                    runCatching { Shell.cmd("input -d $displayId keyevent 4").exec() }
                }
            }
        }
    }

    private fun syncMinimizedStackWindow() {
        val hasMinimized = sessions.any { it.sizeState == FloatingSizeState.MINIMIZED }
        if (!hasMinimized) {
            minimizedStackHostView?.visibility = View.GONE
            return
        }

        if (minimizedStackHostView == null) {
            val dm = context.resources.displayMetrics
            val bubbleW = (48 * dm.density).roundToInt()
            val initialX = if (stackSide == "left") 0 else (dm.widthPixels - bubbleW)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = initialX
                y = stackY
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }

            val composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                setContent {
                    AZenithTheme {
                        MinimizedIconStackOverlay(manager = this@ZXFloatingAppManager)
                    }
                }
            }

            val host = object : FrameLayout(context) {
                private var downRawX = 0f
                private var downRawY = 0f
                private var startParamX = 0
                private var startParamY = 0
                private var draggingStack = false
                private val touchSlopPx = 10f * resources.displayMetrics.density

                override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                    super.onSizeChanged(w, h, oldw, oldh)
                    if (!draggingStack && w > 0 && w != oldw && !isStackExpanded) {
                        post { snapMinimizedStackToEdge(animate = false) }
                    }
                }

                override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                    val p = minimizedStackParams ?: return super.dispatchTouchEvent(ev)
                    when (ev.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            snapAnimator?.cancel()
                            downRawX = ev.rawX
                            downRawY = ev.rawY
                            startParamX = p.x
                            startParamY = p.y
                            draggingStack = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = ev.rawX - downRawX
                            val dy = ev.rawY - downRawY
                            if (!draggingStack && hypot(dx, dy) > touchSlopPx && !isStackExpanded) {
                                draggingStack = true
                            }
                            if (draggingStack) {
                                val dmMetrics = resources.displayMetrics
                                val maxX = (dmMetrics.widthPixels - width).coerceAtLeast(0)
                                val maxY = (dmMetrics.heightPixels - height).coerceAtLeast(0)
                                p.x = (startParamX + dx.roundToInt()).coerceIn(0, maxX)
                                p.y = (startParamY + dy.roundToInt()).coerceIn(0, maxY)
                                stackY = p.y
                                runCatching { windowManager.updateViewLayout(this, p) }
                                return true
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (draggingStack) {
                                draggingStack = false
                                snapMinimizedStackToEdge(animate = true)
                                return true
                            }
                        }
                    }
                    return super.dispatchTouchEvent(ev)
                }
            }.apply {
                elevation = 0f
                outlineProvider = null
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
                addView(
                    composeView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            minimizedStackParams = params
            minimizedStackHostView = host
            runCatching { windowManager.addView(host, params) }
        } else {
            minimizedStackHostView?.visibility = View.VISIBLE
        }

        snapMinimizedStackToEdge(animate = false)
    }

    fun snapMinimizedStackToEdge(animate: Boolean) {
        val host = minimizedStackHostView ?: return
        val params = minimizedStackParams ?: return
        val dm = context.resources.displayMetrics

        val minimizedCount = sessions.count { it.sizeState == FloatingSizeState.MINIMIZED }
        if (isStackExpanded && minimizedCount > 1) {
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.x = 0
            params.y = 0
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            runCatching { windowManager.updateViewLayout(host, params) }
            return
        }

        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val viewW = (32 * dm.density).roundToInt()
        val viewH = (32 * dm.density).roundToInt()

        val centerX = params.x + viewW / 2
        stackSide = if (centerX < dm.widthPixels / 2) "left" else "right"
        val targetX = if (stackSide == "left") {
            0
        } else {
            (dm.widthPixels - viewW).coerceAtLeast(0)
        }
        val targetY = stackY.coerceIn(
            (16 * dm.density).roundToInt(),
            (dm.heightPixels - viewH - (16 * dm.density).roundToInt()).coerceAtLeast((16 * dm.density).roundToInt())
        )
        stackY = targetY
        params.y = targetY

        snapAnimator?.cancel()
        if (animate && abs(params.x - targetX) > 2) {
            val startX = params.x
            snapAnimator = ValueAnimator.ofInt(startX, targetX).apply {
                duration = 180L
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    params.x = anim.animatedValue as Int
                    runCatching { windowManager.updateViewLayout(host, params) }
                }
                start()
            }
        } else {
            params.x = targetX
            runCatching { windowManager.updateViewLayout(host, params) }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun FloatingAppWindowContent(
    session: ZXFloatingAppSession,
    manager: ZXFloatingAppManager
) {
    val scheme = MaterialTheme.colorScheme
    val isMini = session.sizeState == FloatingSizeState.MINI
    val isPendingIcon = session.isPendingMinimizeToIcon
    val cornerRadius by animateDpAsState(
        targetValue = if (isPendingIcon) 26.dp else if (isMini) 18.dp else 22.dp,
        label = "cornerRadius"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isPendingIcon -> scheme.primary
            session.isResizing || session.isDragging -> scheme.primary.copy(alpha = 0.85f)
            else -> scheme.outlineVariant.copy(alpha = 0.50f)
        },
        animationSpec = tween(150),
        label = "floatingBorderColor"
    )
    val borderThickness by animateDpAsState(
        targetValue = if (isPendingIcon) 2.5.dp else 2.dp,
        label = "floatingBorderThickness"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(scheme.surfaceContainerLowest)
            .border(borderThickness, borderColor, RoundedCornerShape(cornerRadius))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar (shown in SMALL, NORMAL, and LARGE; hidden in MINI square box mode or pending icon)
            if (!isMini && !isPendingIcon) {
                FloatingWindowTopBar(
                    onFullScreen = { manager.expandSessionToFullscreen(session) },
                    onClose = { manager.closeSession(session) }
                )
            }

            // Hosted App Portrait VirtualDisplay Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(scheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                var hasDrawnFrame by remember { mutableStateOf(false) }

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (hasDrawnFrame) 1f else 0f),
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            isOpaque = false
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {
                                    surface.setDefaultBufferSize(
                                        ZXFloatingAppManager.VIRTUAL_PORTRAIT_WIDTH,
                                        ZXFloatingAppManager.VIRTUAL_PORTRAIT_HEIGHT
                                    )
                                    manager.bindSessionSurface(session, surface)
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    surface: SurfaceTexture,
                                    width: Int,
                                    height: Int
                                ) {
                                    surface.setDefaultBufferSize(
                                        ZXFloatingAppManager.VIRTUAL_PORTRAIT_WIDTH,
                                        ZXFloatingAppManager.VIRTUAL_PORTRAIT_HEIGHT
                                    )
                                }

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                    manager.unbindSessionSurface(session)
                                    hasDrawnFrame = false
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                                    if (!hasDrawnFrame) {
                                        hasDrawnFrame = true
                                    }
                                }
                            }

                            setOnTouchListener { v, event ->
                                if (session.sizeState == FloatingSizeState.MINI || session.isPendingMinimizeToIcon) {
                                    false
                                } else {
                                    manager.dispatchTouchToVirtualDisplay(
                                        session = session,
                                        viewWidth = v.width,
                                        viewHeight = v.height,
                                        rawEvent = event
                                    )
                                    true
                                }
                            }
                        }
                    },
                    update = { tv ->
                        tv.surfaceTexture?.setDefaultBufferSize(
                            ZXFloatingAppManager.VIRTUAL_PORTRAIT_WIDTH,
                            ZXFloatingAppManager.VIRTUAL_PORTRAIT_HEIGHT
                        )
                    }
                )

                // Placeholder / Loading before first frame is drawn (prevents black/blank screen or recursive mirror)
                if (!hasDrawnFrame) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.surfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        if (session.iconBitmap != null) {
                            Image(
                                bitmap = session.iconBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Pending Minimize to Icon Visual Preview Overlay
        AnimatedVisibility(
            visible = isPendingIcon,
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.scrim.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    border = BorderStroke(2.dp, scheme.primary),
                    modifier = Modifier.size(54.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (session.iconBitmap != null) {
                            Image(
                                bitmap = session.iconBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                tint = scheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Resizing Mode Feedback Pill (Centered above bottom edge)
        AnimatedVisibility(
            visible = session.isResizing && !isPendingIcon && session.resizeModeHint != null,
            enter = fadeIn(animationSpec = tween(100)),
            exit = fadeOut(animationSpec = tween(100)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = scheme.surfaceContainerHighest.copy(alpha = 0.94f),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = session.resizeModeHint.orEmpty(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun applyPortraitTransform(textureView: TextureView, viewW: Int, viewH: Int) {
    if (viewW <= 0 || viewH <= 0) return
    textureView.surfaceTexture?.setDefaultBufferSize(
        ZXFloatingAppManager.VIRTUAL_PORTRAIT_WIDTH,
        ZXFloatingAppManager.VIRTUAL_PORTRAIT_HEIGHT
    )
}

@Composable
private fun FloatingWindowTopBar(
    onFullScreen: () -> Unit,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.90f))
            .padding(horizontal = 8.dp)
    ) {
        // Left control: Fullscreen
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarIconButton(onClick = onFullScreen) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Center drag handle pill
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(scheme.outline.copy(alpha = 0.45f))
        )

        // Right control: Close (minimize is triggered by resizing smaller into icon mode)
        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            TopBarIconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = scheme.error,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun TopBarIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(scheme.surfaceContainerHighest.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun MinimizedIconStackOverlay(manager: ZXFloatingAppManager) {
    val scheme = MaterialTheme.colorScheme
    val minimized = manager.sessions.filter { it.sizeState == FloatingSizeState.MINIMIZED }
    if (minimized.isEmpty()) return

    val isExpanded = manager.isStackExpanded && minimized.size > 1
    val isRightSide = manager.stackSide == "right"

    val dockShape = if (isRightSide) {
        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 0.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 18.dp, bottomEnd = 18.dp)
    }

    val dockContent = @Composable {
        Box(
            modifier = Modifier
                .clip(dockShape)
                .background(scheme.surfaceContainerHigh.copy(alpha = 0.96f))
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.40f), dockShape)
                .padding(
                    start = if (isRightSide) 6.dp else 2.dp,
                    end = if (isRightSide) 2.dp else 6.dp,
                    top = 4.dp,
                    bottom = 4.dp
                )
        ) {
            if (minimized.size == 1) {
                val single = minimized.first()
                MinimizedAppIconTile(
                    iconBitmap = single.iconBitmap,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            manager.restoreSessionFromIcon(single, targetSize = FloatingSizeState.SMALL)
                        }
                )
            } else if (!isExpanded) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            manager.isStackExpanded = true
                            manager.snapMinimizedStackToEdge(animate = false)
                        }
                ) {
                    val backSession = minimized[1]
                    val frontSession = minimized[0]

                    MinimizedAppIconTile(
                        iconBitmap = backSession.iconBitmap,
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                    )
                    MinimizedAppIconTile(
                        iconBitmap = frontSession.iconBitmap,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopStart)
                            .border(1.5.dp, scheme.surfaceContainerHigh, RoundedCornerShape(10.dp))
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    minimized.take(2).forEach { item ->
                        MinimizedAppIconTile(
                            iconBitmap = item.iconBitmap,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    manager.isStackExpanded = false
                                    manager.restoreSessionFromIcon(item, targetSize = FloatingSizeState.SMALL)
                                }
                        )
                    }
                }
            }
        }
    }

    if (isExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        manager.isStackExpanded = false
                        manager.snapMinimizedStackToEdge(animate = true)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(if (isRightSide) Alignment.TopEnd else Alignment.TopStart)
                    .offset { IntOffset(0, manager.stackY) }
            ) {
                dockContent()
            }
        }
    } else {
        dockContent()
    }
}

@Composable
private fun MinimizedAppIconTile(
    iconBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceContainerHighest)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
