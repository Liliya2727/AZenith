package zx.azenith.ui.component

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs

/**
 * The shape of the row being composed, so a focus outline can follow the row's
 * own corners.
 */
val LocalRowShape = compositionLocalOf<Shape> { RoundedCornerShape(4.dp) }

/** Remappable gamepad actions (single mapping point). */
enum class GamepadAction {
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    CONFIRM, // A / CENTER
    BACK,    // B
    SEARCH,  // X
    TOP_ACTION, // Y
    PAGE_PREV, // L1
    PAGE_NEXT, // R1
    NONE
}

/** Central mapping from keycode -> action. Edit here to remap later. */
object GamepadKeymap {
    fun map(keyCode: Int): GamepadAction = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> GamepadAction.DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> GamepadAction.DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> GamepadAction.DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadAction.DPAD_RIGHT
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A -> GamepadAction.CONFIRM
        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_ESCAPE -> GamepadAction.BACK
        KeyEvent.KEYCODE_BUTTON_X -> GamepadAction.SEARCH
        KeyEvent.KEYCODE_BUTTON_Y -> GamepadAction.TOP_ACTION
        KeyEvent.KEYCODE_BUTTON_L1 -> GamepadAction.PAGE_PREV
        KeyEvent.KEYCODE_BUTTON_R1 -> GamepadAction.PAGE_NEXT
        else -> GamepadAction.NONE
    }
}

/**
 * D-pad, TV-remote and gamepad state for the whole app.
 */
object GamepadInput {
    /** True while a d-pad / gamepad is driving the UI. A finger going down clears it. */
    var dpadMode: Boolean by mutableStateOf(false)
        private set

    /** A gamepad or d-pad-capable device is attached. */
    var controllerConnected: Boolean by mutableStateOf(false)
        private set

    val active: Boolean get() = dpadMode

    fun onDpadKey() {
        if (!dpadMode) dpadMode = true
    }

    fun onTouch() {
        if (dpadMode) dpadMode = false
    }

    fun onControllerPresence(connected: Boolean) {
        if (controllerConnected != connected) controllerConnected = connected
    }

    fun hasDpadDevice(): Boolean = InputDevice.getDeviceIds().any { id ->
        val device = InputDevice.getDevice(id) ?: return@any false
        if (!device.isEnabled || device.isVirtual) return@any false
        val sources = device.sources
        sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            (sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD && device.isExternal)
    }
}

/** Right stick scroll axis (vertical). Positive = scroll down. */
object GamepadStickScroll {
    var axisY by mutableStateOf(0f)
        private set

    /** Called from Activity.dispatchGenericMotionEvent. */
    fun onAxisY(value: Float) {
        axisY = value
    }
}

/**
 * Registry for the currently active screen's gamepad shortcuts.
 * Only the foreground/active screen should register handlers (use DisposableEffect).
 */
object GamepadShortcuts {
    /** If true, a dialog is considered open by global counter; pages may also use onCloseOverlay. */
    var isDialogOpen: Boolean = false

    // Search
    var hasSearch: Boolean = false
    var isSearchOpen: Boolean = false
    var onSearchOpen: (() -> Unit)? = null
    var onSearchClose: (() -> Unit)? = null
    var onSearchToggle: (() -> Unit)? = null

    // Top action (beside search)
    var hasTopAction: Boolean = false
    var onTopAction: (() -> Unit)? = null

    // Back / overlay close
    /** Try to close an overlay (dialog/sheet) owned by current screen. Return true if consumed. */
    var onCloseOverlay: (() -> Boolean)? = null
    /** Generic back handler; return true if consumed. */
    var onBack: (() -> Boolean)? = null

    // Pager (only active on main pager)
    var isOnMainPager: Boolean = false
    var canPagePrev: Boolean = false
    var canPageNext: Boolean = false
    var onPagePrev: (() -> Unit)? = null
    var onPageNext: (() -> Unit)? = null
}

/**
 * Where the bottom bar hands focus back to the page.
 */
object GamepadFocusNav {
    var pageEntry: FocusRequester? by mutableStateOf(null)
        private set

    var lastEntry: FocusRequester? by mutableStateOf(null)
        private set

    fun publishPageEntry(requester: FocusRequester?) {
        if (pageEntry !== requester) pageEntry = requester
    }

    fun publishLastEntry(requester: FocusRequester?) {
        if (lastEntry !== requester) lastEntry = requester
    }

    private val focusHistory = ArrayDeque<FocusRequester>()

    fun publishLastFocused(requester: FocusRequester) {
        focusHistory.remove(requester)
        focusHistory.addLast(requester)
        while (focusHistory.size > 8) focusHistory.removeFirst()
    }

    fun restoreFocus(): Boolean {
        val candidates = focusHistory.toList().asReversed() + listOfNotNull(lastEntry, pageEntry)
        return candidates.any { runCatching { it.requestFocus() }.getOrDefault(false) }
    }
}

@Composable
fun Modifier.gamepadFocusOutline(
    shape: Shape = LocalRowShape.current,
    width: Dp = 2.dp,
    register: Boolean = false,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val alpha by animateFloatAsState(
        targetValue = if (focused && GamepadInput.active) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "GamepadFocusOutline"
    )
    return this
        .focusable()
        .onFocusChanged {
            focused = it.isFocused
            if (it.isFocused && register) GamepadFocusNav.publishLastFocused(requester)
        }
        .then(if (register) focusRequester(requester) else Modifier)
        .then(
            if (alpha > 0.01f) {
                Modifier.border(
                    width = width,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
}

@Composable
fun Modifier.gamepadScreenEntry(): Modifier {
    val requester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(GamepadInput.dpadMode) {
        GamepadFocusNav.publishLastEntry(requester)
        if (!GamepadInput.dpadMode) return@LaunchedEffect
        repeat(8) {
            delay(150)
            if (entered) return@LaunchedEffect
            val focused = runCatching { requester.requestFocus() }.getOrDefault(false) ||
                runCatching { focusManager.moveFocus(FocusDirection.Next) }.getOrDefault(false) ||
                runCatching { focusManager.moveFocus(FocusDirection.Enter) }.getOrDefault(false)
            if (focused) return@LaunchedEffect
        }
    }
    return this
        .onFocusChanged { if (it.hasFocus) entered = true }
        .focusRequester(requester)
        .focusGroup()
}

/**
 * Handles gamepad shortcut keys at the window/content level.
 * - KeyDown repeatCount==0 triggers actions.
 * - Also consumes matching KeyUp for those shortcut keys to avoid double-fire.
 * - Sets dpadMode for any mapped gamepad key.
 */
fun Modifier.gamepadKeyWatch(): Modifier = this.onPreviewKeyEvent { event ->
    val keyCode = event.nativeKeyEvent.keyCode
    val action = GamepadKeymap.map(keyCode)
    if (action == GamepadAction.NONE) return@onPreviewKeyEvent false

    // Always mark dpad/gamepad mode on key events
    if (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp) {
        val wasActive = GamepadInput.active
        GamepadInput.onDpadKey()
        if (!wasActive && event.type == KeyEventType.KeyDown) {
            // Try to enter content focus on first gamepad press
            GamepadFocusNav.restoreFocus()
        }
    }

    if (event.type == KeyEventType.KeyUp) {
        // Consume up for shortcut keys we handle to prevent double firing
        return@onPreviewKeyEvent when (action) {
            GamepadAction.BACK, GamepadAction.SEARCH, GamepadAction.TOP_ACTION,
            GamepadAction.PAGE_PREV, GamepadAction.PAGE_NEXT -> true
            else -> false
        }
    }

    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    if (event.nativeKeyEvent.repeatCount != 0) return@onPreviewKeyEvent false

    val dialogOpen = GamepadShortcuts.isDialogOpen
    when (action) {
        GamepadAction.BACK -> {
            // Priority: close overlay (dialog/sheet) -> close search -> custom back -> pop handled by caller
            val closedOverlay = GamepadShortcuts.onCloseOverlay?.invoke() == true
            if (closedOverlay) return@onPreviewKeyEvent true
            if (GamepadShortcuts.isSearchOpen) {
                GamepadShortcuts.onSearchClose?.invoke()
                return@onPreviewKeyEvent true
            }
            val handled = GamepadShortcuts.onBack?.invoke() == true
            return@onPreviewKeyEvent handled
        }
        GamepadAction.SEARCH -> {
            if (dialogOpen) return@onPreviewKeyEvent true
            if (GamepadShortcuts.hasSearch) {
                if (GamepadShortcuts.onSearchToggle != null) {
                    GamepadShortcuts.onSearchToggle?.invoke()
                } else if (GamepadShortcuts.isSearchOpen) {
                    GamepadShortcuts.onSearchClose?.invoke()
                } else {
                    GamepadShortcuts.onSearchOpen?.invoke()
                }
                return@onPreviewKeyEvent true
            }
            return@onPreviewKeyEvent true // consume even if nothing
        }
        GamepadAction.TOP_ACTION -> {
            if (dialogOpen) return@onPreviewKeyEvent true
            if (GamepadShortcuts.hasTopAction) {
                GamepadShortcuts.onTopAction?.invoke()
                return@onPreviewKeyEvent true
            }
            return@onPreviewKeyEvent true
        }
        GamepadAction.PAGE_PREV -> {
            if (dialogOpen) return@onPreviewKeyEvent true
            if (GamepadShortcuts.isOnMainPager && GamepadShortcuts.canPagePrev) {
                GamepadShortcuts.onPagePrev?.invoke()
                return@onPreviewKeyEvent true
            }
            return@onPreviewKeyEvent true
        }
        GamepadAction.PAGE_NEXT -> {
            if (dialogOpen) return@onPreviewKeyEvent true
            if (GamepadShortcuts.isOnMainPager && GamepadShortcuts.canPageNext) {
                GamepadShortcuts.onPageNext?.invoke()
                return@onPreviewKeyEvent true
            }
            return@onPreviewKeyEvent true
        }
        GamepadAction.CONFIRM -> {
            // Let Compose handle A/Enter for clicks/focus activation; just ensure dpadMode set.
            return@onPreviewKeyEvent false
        }
        else -> return@onPreviewKeyEvent false
    }
}

@Composable
fun GamepadFocusRoot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    LaunchedEffect(Unit) {
        while (true) {
            GamepadInput.onControllerPresence(GamepadInput.hasDpadDevice())
            delay(3000)
        }
    }

    val activeDialogCount = LocalActiveDialogCount.current
    GamepadShortcuts.isDialogOpen = activeDialogCount.value > 0

    Box(
        modifier = modifier
            .gamepadKeyWatch()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) GamepadInput.onTouch()
                    }
                }
            }
    ) {
        content()
    }
}

@Composable
fun ProvideRowShape(shape: Shape, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRowShape provides shape) { content() }
}

/** Scrolls a LazyList using the right stick axisY from GamepadStickScroll. */
@Composable
fun GamepadStickScrollEffect(
    listState: LazyListState,
    deadzone: Float = 0.15f,
    speedPxPerSecond: Float = 2200f,
    onScrollDelta: ((Float) -> Unit)? = null,
) {
    LaunchedEffect(listState, onScrollDelta) {
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = (frameNanos - lastNanos) / 1_000_000_000f
                lastNanos = frameNanos
                val value = GamepadStickScroll.axisY
                if (abs(value) > deadzone) {
                    GamepadInput.onDpadKey()
                    val magnitude = (abs(value) - deadzone) / (1f - deadzone)
                    val delta = kotlin.math.sign(value) * magnitude * speedPxPerSecond * dt
                    onScrollDelta?.invoke(delta)
                    this.launch {
                        runCatching { listState.scrollBy(delta) }
                    }
                } else {
                    lastNanos = frameNanos
                }
            }
        }
    }
}
