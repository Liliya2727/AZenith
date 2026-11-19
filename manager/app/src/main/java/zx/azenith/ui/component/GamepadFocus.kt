package zx.azenith.ui.component

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * The shape of the row being composed, so a focus outline can follow the row's
 * own corners.
 */
val LocalRowShape = compositionLocalOf<Shape> { RoundedCornerShape(4.dp) }

/**
 * True while the main pager is the screen on top of the nav stack.
 *
 * The pager's pages stay composed even when a subscreen is pushed over them, so
 * `LocalIsCurrentPagerPage` alone would let a hidden page keep answering
 * shortcut keys and scroll requests. Screens combine both before registering.
 */
val LocalMainPagerActive = compositionLocalOf { true }

/** Index of the pager page a screen is composed as; -1 outside the pager. */
val LocalPagerPageIndex = compositionLocalOf { -1 }

/** Active focus scope key for the current screen (e.g. "page:0" or per-screen id). */
val LocalGamepadFocusScope = compositionLocalOf<String?> { null }

/**
 * Every action a controller button can ask the UI to take.
 *
 * This enum is the vocabulary; [GamepadKeymap] is the only place a physical
 * keycode is turned into one of these, so remapping a button later means
 * editing that one `when` and nothing else.
 */
enum class GamepadAction {
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    CONFIRM,      // Cross / A          -> click or toggle the focused row
    BACK,         // Circle / B         -> close overlay -> close search -> pop
    SEARCH,       // Square / X         -> toggle search
    TOP_ACTION,   // Triangle / Y       -> toggle the action beside search
    PAGE_PREV,    // L1                 -> previous pager page
    PAGE_NEXT,    // R1                 -> next pager page
    NONE
}

/**
 * The single keycode -> action map.
 *
 * Android's standard gamepad keycodes are used, and the keyboard equivalents
 * (arrows, Enter, Escape) are aliases so a TV remote or a keyboard behaves the
 * same way. Remap a button by changing its line here.
 */
object GamepadKeymap {
    fun map(keyCode: Int): GamepadAction = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> GamepadAction.DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> GamepadAction.DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> GamepadAction.DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadAction.DPAD_RIGHT

        // Cross / A. D-pad centre and Enter are the keyboard aliases, so a
        // focused row can also be activated from a remote.
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> GamepadAction.CONFIRM

        // Circle / B.
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_ESCAPE -> GamepadAction.BACK

        // Square / X.
        KeyEvent.KEYCODE_BUTTON_X -> GamepadAction.SEARCH

        // Triangle / Y.
        KeyEvent.KEYCODE_BUTTON_Y -> GamepadAction.TOP_ACTION

        KeyEvent.KEYCODE_BUTTON_L1 -> GamepadAction.PAGE_PREV
        KeyEvent.KEYCODE_BUTTON_R1 -> GamepadAction.PAGE_NEXT
        else -> GamepadAction.NONE
    }

    /** KeyUps that have to be swallowed so a handled shortcut cannot fire twice. */
    fun consumesKeyUp(action: GamepadAction): Boolean = when (action) {
        GamepadAction.BACK,
        GamepadAction.SEARCH,
        GamepadAction.TOP_ACTION,
        GamepadAction.PAGE_PREV,
        GamepadAction.PAGE_NEXT,
        GamepadAction.CONFIRM -> true
        else -> false
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

/**
 * Right stick scroll (vertical). Positive = scroll down.
 *
 * Values below [REST_SNAP] are treated as rest, so a worn stick that never
 * quite returns to zero cannot make the list creep.
 */
object GamepadStickScroll {
    const val DEADZONE = 0.15f
    private const val REST_SNAP = 0.04f

    var axisY by mutableStateOf(0f)
        private set

    /** Called from the Activity's dispatchGenericMotionEvent. */
    fun onAxisY(value: Float) {
        axisY = if (abs(value) < REST_SNAP) 0f else value
    }

    /** Centres the axis, e.g. when the app is paused or a controller detaches. */
    fun reset() {
        if (axisY != 0f) axisY = 0f
    }
}

/**
 * Left stick as a D-pad. Only the vertical axis moves focus: rows are the only
 * focus targets, and horizontal input is left to the focused slider.
 *
 * A direction has to cross [THRESHOLD] to fire, so resting noise never moves
 * focus, and it repeats only after a delay while the stick is held.
 */
object GamepadStickNav {
    private const val THRESHOLD = 0.55f
    private const val REPEAT_MS = 260L

    var focusManager: FocusManager? = null

    private var lastDy = 0
    private var nextRepeatAt = 0L

    /** Move focus one step in [direction]. */
    fun moveFocus(direction: FocusDirection): Boolean =
        runCatching { focusManager?.moveFocus(direction) == true }.getOrDefault(false)

    fun moveFocusNext(): Boolean = moveFocus(FocusDirection.Next)

    fun onStick(x: Float, y: Float) {
        val dy = if (abs(y) > THRESHOLD) if (y > 0f) 1 else -1 else 0
        if (dy == 0) {
            reset()
            return
        }

        val now = SystemClock.uptimeMillis()
        if (dy == lastDy && now < nextRepeatAt) return

        GamepadInput.onDpadKey()
        moveFocus(if (dy > 0) FocusDirection.Down else FocusDirection.Up)

        lastDy = dy
        nextRepeatAt = now + REPEAT_MS
    }

    fun reset() {
        lastDy = 0
        nextRepeatAt = 0L
    }
}

/**
 * Registry for the *currently active screen's* gamepad behaviour.
 *
 * The pager keeps all four pages composed, so a screen cannot rely on its own
 * composition to know it is the one on screen. Screens register through
 * [RegisterGamepadActions] while they are active and drop their registration in
 * `onDispose`; the map is keyed by a per-screen token so a page that goes out of
 * date can never clear a newer page's handlers.
 *
 * Pager-level behaviour (L1/R1 and the global back fallback) is published once
 * by the main screen, not by the pages.
 */
object GamepadShortcuts {
    /** One screen's handlers. Mutated in place by [RegisterGamepadActions]. */
    class Actions {
        var hasSearch: Boolean = false
        var isSearchOpen: Boolean = false
        var onSearchToggle: (() -> Unit)? = null
        var onSearchClose: (() -> Unit)? = null

        var hasTopAction: Boolean = false
        var isTopActionOpen: Boolean = false
        /** Implemented as a toggle by the screen (open when closed, close when open). */
        var onTopAction: (() -> Unit)? = null

        /** Close a dialog/sheet owned by this screen. Returns true when something closed. */
        var onCloseOverlay: (() -> Boolean)? = null
        /** Screen-specific back, e.g. a search screen popping itself. */
        var onBack: (() -> Boolean)? = null

        /** The screen's list, scrolled by the right stick post-frame. */
        var scrollState: LazyListState? = null
        var shiftableScrollState: ScrollState? = null
        /** Also called with the per-frame delta, so app bars can collapse/expand. */
        var onScrollDelta: ((Float) -> Unit)? = null
        var scrollDeadzone: Float = GamepadStickScroll.DEADZONE
        var scrollSpeedPxPerSecond: Float = 2200f
    }

    private var currentToken: Any? = null
    private var currentActions: Actions? = null

    val actions: Actions? get() = currentActions

    fun register(token: Any, actions: Actions) {
        currentToken = token
        currentActions = actions
    }

    fun unregister(token: Any) {
        if (currentToken === token) {
            currentToken = null
            currentActions = null
        }
    }

    // --- Global state, published by the root / main screen -------------------

    /** True while any dialog (in-composition or its own window) is up. */
    var isDialogOpen: Boolean = false

    /** Main pager state, only meaningful while [isOnMainPager]. */
    var isOnMainPager: Boolean = false
    var pageIndex: Int = 0
    var pageCount: Int = 0
    /** Ask the main screen to animate to a page; null outside the pager. */
    var onPageTarget: ((Int) -> Unit)? = null
    /** Last-resort Circle: pop the current subscreen. Returns true when handled. */
    var onGlobalBack: (() -> Boolean)? = null

    // --- Focused row activation ---------------------------------------------

    private var activationToken: Any? = null
    private var activation: (() -> Unit)? = null

    /** True when focus sits on an outlined content row / switch / slider / button. */
    val hasRowFocus: Boolean get() = activationToken != null

    /** True when the root gamepad container has any focus (for restore heuristics). */
    var rootHasFocus: Boolean by mutableStateOf(false)

    fun publishActivation(token: Any, callback: (() -> Unit)?) {
        activationToken = token
        activation = callback
        GamepadFocusNav.publishLastFocusedToken(token)
    }

    fun clearActivation(token: Any) {
        if (activationToken === token) {
            activationToken = null
            activation = null
        }
    }

    /** Invoke the focused row's click, if it has one. */
    fun activateFocused(): Boolean {
        val callback = activation ?: return false
        callback()
        return true
    }
}

/**
 * Edge-aware horizontal D-pad handling for a focused row.
 *
 * Directions marked blocked are consumed by [gamepadKeyWatch] so the pager does
 * not react and focus does not jump past the row; others pass through to the
 * focus system (or a slider).
 */
enum class HorizontalNav {
    BLOCK_BOTH,
    BLOCK_LEFT,
    BLOCK_RIGHT,
    PASS_BOTH
}

/** Saved focus identity for a scope so it can be restored after recomposition. */
data class SavedFocus(val key: String, val index: Int? = null)

/**
 * Where the bottom bar and the pages hand focus back to content.
 *
 * Page entries are kept per pager index, because the pager keeps every page
 * composed: a single "entry" requester would always point at whichever page
 * happened to compose last.
 */
object GamepadFocusNav {
    var pageEntry: FocusRequester? by mutableStateOf(null)
        private set

    var lastEntry: FocusRequester? by mutableStateOf(null)
        private set

    /** Index of the pager page on screen; -1 when not on the pager. */
    var currentPage: Int = -1

    fun publishPageEntry(page: Int, requester: FocusRequester) {
        if (page >= 0) pageEntries[page] = requester else publishLastEntry(requester)
    }

    fun clearPageEntry(page: Int, requester: FocusRequester) {
        if (page >= 0) {
            if (pageEntries[page] === requester) pageEntries.remove(page)
        } else if (lastEntry === requester) {
            lastEntry = null
        }
    }

    fun publishLastEntry(requester: FocusRequester?) {
        if (lastEntry !== requester) lastEntry = requester
        if (requester != null) pageEntry = requester
    }

    private val pageEntries = HashMap<Int, FocusRequester>()

    /** True when the focused node is a slider, which owns D-pad left/right. */
    var focusedSlider: Boolean = false
        private set

    fun publishFocusedSlider(focused: Boolean) {
        focusedSlider = focused
    }

    /** Horizontal D-pad policy published by the focused row (edge-aware). */
    var horizontalNav: HorizontalNav = HorizontalNav.BLOCK_BOTH
        private set
    private var horizontalNavOwner: Any? = null

    fun publishHorizontalNav(token: Any, nav: HorizontalNav) {
        horizontalNavOwner = token
        horizontalNav = nav
    }

    fun clearHorizontalNav(token: Any) {
        if (horizontalNavOwner === token) {
            horizontalNavOwner = null
            horizontalNav = HorizontalNav.BLOCK_BOTH
        }
    }

    fun isHorizontalBlocked(left: Boolean): Boolean = when (horizontalNav) {
        HorizontalNav.BLOCK_BOTH -> true
        HorizontalNav.BLOCK_LEFT -> left
        HorizontalNav.BLOCK_RIGHT -> !left
        HorizontalNav.PASS_BOTH -> false
    }

    /** Last focused row key per scope ("page:<index>" or standalone screen scope). */
    val lastKeyByScope = mutableStateMapOf<String, String>()
    private val lastIndexByScope = HashMap<String, Int>()
    private val pendingRestoreScopes = mutableStateMapOf<String, Boolean>()

    fun currentScope(): String? =
        if (currentPage >= 0) "page:$currentPage" else lastEntryScope

    /** Scope associated with the active standalone screen entry, if any. */
    var lastEntryScope: String? = null

    fun saveFocusKey(scope: String, key: String, index: Int?) {
        lastKeyByScope[scope] = key
        if (index != null) lastIndexByScope[scope] = index else lastIndexByScope.remove(scope)
    }

    fun savedKey(scope: String): String? = lastKeyByScope[scope]
    fun savedIndex(scope: String): Int? = lastIndexByScope[scope]

    fun markRestorePending(scope: String) {
        pendingRestoreScopes[scope] = true
    }

    fun clearRestorePending(scope: String) {
        pendingRestoreScopes.remove(scope)
    }

    fun isRestorePending(scope: String): Boolean = pendingRestoreScopes[scope] == true

    /**
     * Restore a saved focus key for [scope]. If the matching node is not yet
     * composed, scroll the active list to the saved index (when known) and mark
     * the scope pending so the node can claim focus once composed.
     */
    fun restoreSavedFocus(scope: String): Boolean {
        val key = lastKeyByScope[scope] ?: return false
        markRestorePending(scope)
        val index = lastIndexByScope[scope]
        if (index != null) {
            val ls = GamepadShortcuts.actions?.scrollState
            if (ls != null) {
                MainScope().launch { runCatching { ls.scrollToItem(index) } }
            }
        }
        // A composed matching node will claim focus via gamepadFocusOutline.
        return false
    }

    private val focusHistory = ArrayDeque<FocusRequester>()

    fun publishLastFocused(requester: FocusRequester) {
        publishLastFocusedToken(requester)
    }

    fun publishLastFocusedToken(token: Any) {
        if (token is FocusRequester) {
            focusHistory.remove(token)
            focusHistory.addLast(token)
            while (focusHistory.size > 8) focusHistory.removeFirst()
        }
    }

    /**
     * Focus the first row of [page] after a pager switch.
     *
     * Requests the page's list container and then steps into it, so the outline
     * lands on the first content row rather than on the invisible container.
     */
    fun requestPageEntry(page: Int): Boolean {
        val requester = pageEntries[page]
        val focused = requester != null &&
            runCatching { requester.requestFocus() }.getOrDefault(false)
        if (!focused) return restoreFocus()
        GamepadStickNav.moveFocus(FocusDirection.Next) ||
            GamepadStickNav.moveFocus(FocusDirection.Down) ||
            GamepadStickNav.moveFocus(FocusDirection.Enter)
        return true
    }

    /**
     * Re-seed focus after a page swap, a dialog closing or a rotation.
     *
     * Prefers the requested page's entry, then the last focused row, then the
     * last screen entry, and finally lets the focus system pick any candidate.
     */
    fun forget(requester: FocusRequester) {
        focusHistory.remove(requester)
        if (lastEntry === requester) lastEntry = null
        if (pageEntry === requester) pageEntry = null
        pageEntries.values.remove(requester)
    }

    fun restoreFocus(): Boolean {
        val scope = currentScope()
        val currentPageEntry = pageEntries[currentPage]
        val candidates = focusHistory.toList().asReversed() +
            listOfNotNull(currentPageEntry, lastEntry, pageEntry)
        val restored = candidates.any { runCatching { it.requestFocus() }.getOrDefault(false) }
        if (restored) {
            if (scope != null) clearRestorePending(scope)
            return true
        }
        if (scope != null && lastKeyByScope.containsKey(scope)) {
            restoreSavedFocus(scope)
            return true
        }
        return GamepadStickNav.moveFocusNext()
    }
}

/**
 * Makes a container (the navbar, the landscape dock) and everything inside it
 * unreachable by D-pad. Applied to the bar's root node, which prunes the whole
 * subtree from focus search.
 */
fun Modifier.gamepadNotFocusable(): Modifier = this.focusProperties { canFocus = false }

/**
 * The gamepad focus ring and activation for a content row, switch, slider or
 * dialog button.
 *
 * The ring only ever draws while [GamepadInput.active] is true — i.e. after a
 * real gamepad/d-pad key press — so a touch session never shows it.
 *
 * [onActivate] is what Cross runs while this node is focused. Compose's
 * `clickable` does not know BUTTON_A, so the focused node publishes its click
 * here and [gamepadKeyWatch] invokes it.
 *
 * [slider] marks the node as owning D-pad left/right, so the global handler
 * leaves those keys for the slider itself.
 */
@Composable
fun Modifier.gamepadFocusOutline(
    shape: Shape = LocalRowShape.current,
    width: Dp = 2.dp,
    register: Boolean = false,
    slider: Boolean = false,
    horizontalNav: HorizontalNav = HorizontalNav.BLOCK_BOTH,
    focusKey: String? = null,
    focusIndex: Int? = null,
    onActivate: (() -> Unit)? = null,
): Modifier {
    val requester = remember { FocusRequester() }
    val activationState = rememberUpdatedState(onActivate)
    val horizontalNavState = rememberUpdatedState(horizontalNav)
    val pagerPage = LocalPagerPageIndex.current
    val scope = if (pagerPage >= 0) "page:$pagerPage" else GamepadFocusNav.lastEntryScope
    val focusKeyState = rememberUpdatedState(focusKey)
    val focusIndexState = rememberUpdatedState(focusIndex)
    var focused by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (focused && GamepadInput.active) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "GamepadFocusOutline"
    )
    DisposableEffect(requester) {
        onDispose { GamepadFocusNav.forget(requester) }
    }

    if (scope != null && focusKey != null) {
        LaunchedEffect(scope, focusKey, GamepadFocusNav.savedKey(scope), GamepadFocusNav.isRestorePending(scope)) {
            val saved = GamepadFocusNav.savedKey(scope)
            if (saved == focusKey && GamepadFocusNav.isRestorePending(scope)) {
                delay(16)
                val got = runCatching { requester.requestFocus() }.getOrDefault(false)
                if (got) GamepadFocusNav.clearRestorePending(scope)
            }
        }
    }

    return this
        .onFocusChanged { focusState ->
            focused = focusState.isFocused
            if (focusState.isFocused) {
                if (register) GamepadFocusNav.publishLastFocused(requester)
                if (scope != null) {
                    val key = focusKeyState.value
                    if (key != null) {
                        GamepadFocusNav.saveFocusKey(scope, key, focusIndexState.value)
                        GamepadFocusNav.clearRestorePending(scope)
                    }
                }
                GamepadShortcuts.publishActivation(requester) { activationState.value?.invoke() }
                GamepadFocusNav.publishHorizontalNav(requester, horizontalNavState.value)
                if (slider) GamepadFocusNav.publishFocusedSlider(true)
            } else {
                if (slider) GamepadFocusNav.publishFocusedSlider(false)
                GamepadFocusNav.clearHorizontalNav(requester)
                GamepadShortcuts.clearActivation(requester)
            }
        }
        .focusRequester(requester)
        .focusable()
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

/**
 * Marks a list/screen container as an entry point for focus.
 *
 * [page] is the pager index for the four main pages, or -1 for a standalone
 * screen. Only the page that is currently on screen requests focus, because the
 * pager keeps all four composed at once.
 */
@Composable
fun Modifier.gamepadScreenEntry(page: Int = -1, scopeKey: String? = null): Modifier {
    val requester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var entered by remember { mutableStateOf(false) }
    val standaloneScope = remember { "screen:${System.identityHashCode(Any())}" }
    val scope = scopeKey ?: if (page >= 0) "page:$page" else standaloneScope
    val pagerActive = LocalMainPagerActive.current
    val isActiveScreen = if (page >= 0) (pagerActive && page == GamepadFocusNav.currentPage) else true

    DisposableEffect(page, requester, scope) {
        GamepadFocusNav.publishPageEntry(page, requester)
        if (page < 0) GamepadFocusNav.lastEntryScope = scope
        onDispose { GamepadFocusNav.clearPageEntry(page, requester) }
    }

    LaunchedEffect(isActiveScreen) {
        if (!isActiveScreen) entered = false
    }

    LaunchedEffect(
        GamepadInput.dpadMode,
        GamepadInput.controllerConnected,
        pagerActive,
        page,
        GamepadFocusNav.currentPage
    ) {
        GamepadFocusNav.publishLastEntry(requester)
        if (page < 0) GamepadFocusNav.lastEntryScope = scope
        val canSeed = (GamepadInput.dpadMode || GamepadInput.controllerConnected) && isActiveScreen
        if (!canSeed) return@LaunchedEffect
        // Try to restore the last focused content row first (important when returning
        // from subscreens/dialogs). Prefer saved key for this scope.
        delay(120)
        if (entered) return@LaunchedEffect
        if (GamepadFocusNav.restoreFocus()) return@LaunchedEffect
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
        .focusRequester(requester)
        .focusable()
        .onFocusChanged { focusState ->
            entered = focusState.hasFocus
        }
        .focusGroup()
}

/**
 * Handles every gamepad shortcut key at the window/content level.
 *
 * - KeyDown with repeatCount == 0 triggers actions; KeyUp of a handled shortcut
 *   is consumed so nothing fires twice.
 * - Every mapped key sets dpadMode, which is what turns the focus ring on.
 * - While a dialog is open only Circle is live; it closes the dialog.
 */
fun Modifier.gamepadKeyWatch(): Modifier = this.onPreviewKeyEvent { event ->
    val keyCode = event.nativeKeyEvent.keyCode
    val action = GamepadKeymap.map(keyCode)
    if (action == GamepadAction.NONE) return@onPreviewKeyEvent false

    // Any real gamepad/d-pad key means the user is on a controller.
    GamepadInput.onDpadKey()

    if (event.type == KeyEventType.KeyUp) {
        return@onPreviewKeyEvent GamepadKeymap.consumesKeyUp(action)
    }
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    if (event.nativeKeyEvent.repeatCount != 0) return@onPreviewKeyEvent false

    // Nothing focused yet (fresh page, rotation, a dialog just closed): seed
    // focus before this press is interpreted.
    if (!GamepadShortcuts.rootHasFocus &&
        action in setOf(
            GamepadAction.DPAD_UP, GamepadAction.DPAD_DOWN,
            GamepadAction.DPAD_LEFT, GamepadAction.DPAD_RIGHT,
            GamepadAction.CONFIRM
        )
    ) {
        GamepadFocusNav.restoreFocus()
    }

    val dialogOpen = GamepadShortcuts.isDialogOpen
    val shortcuts = GamepadShortcuts.actions

    when (action) {
        // Circle. Priority: dialog/sheet (overlay) -> open search -> subscreen -> nothing.
        GamepadAction.BACK -> {
            // Always try the screen's overlay closer first (sheets, in-composition
            // dialogs, etc.), then search, then screen/global back.
            val closedOverlay = shortcuts?.onCloseOverlay?.invoke() == true
            if (closedOverlay) return@onPreviewKeyEvent true
            if (dialogOpen) {
                GamepadShortcuts.onGlobalBack?.invoke()
                return@onPreviewKeyEvent true
            }
            if (shortcuts?.isSearchOpen == true) {
                val close = shortcuts.onSearchClose ?: shortcuts.onSearchToggle
                close?.invoke()
                return@onPreviewKeyEvent true
            }
            val screenBack = shortcuts?.onBack?.invoke() == true
            if (screenBack) return@onPreviewKeyEvent true
            GamepadShortcuts.onGlobalBack?.invoke()
            return@onPreviewKeyEvent true
        }

        // Square.
        GamepadAction.SEARCH -> {
            if (dialogOpen) return@onPreviewKeyEvent true
            val s = shortcuts
            if (s != null && s.hasSearch) {
                if (s.onSearchToggle != null) {
                    s.onSearchToggle?.invoke()
                } else if (s.isSearchOpen) {
                    (s.onSearchClose ?: s.onSearchToggle)?.invoke()
                } else {
                    s.onSearchToggle?.invoke()
                }
            }
            return@onPreviewKeyEvent true
        }

        // Triangle.
        GamepadAction.TOP_ACTION -> {
            if (dialogOpen) return@onPreviewKeyEvent true
            val s = shortcuts
            if (s != null && s.hasTopAction) s.onTopAction?.invoke()
            return@onPreviewKeyEvent true
        }

        GamepadAction.PAGE_PREV, GamepadAction.PAGE_NEXT -> {
            if (dialogOpen) return@onPreviewKeyEvent true
            val onPageTarget = GamepadShortcuts.onPageTarget
            if (GamepadShortcuts.isOnMainPager && onPageTarget != null) {
                val delta = if (action == GamepadAction.PAGE_PREV) -1 else 1
                val target = (GamepadShortcuts.pageIndex + delta)
                    .coerceIn(0, GamepadShortcuts.pageCount - 1)
                if (target != GamepadShortcuts.pageIndex) onPageTarget(target)
            }
            return@onPreviewKeyEvent true
        }

        GamepadAction.CONFIRM -> {
            // Cross: click/toggle whatever content row currently holds focus.
            // Falls through when there is no activation, so a focused text
            // field can still receive Enter / D-pad centre itself.
            return@onPreviewKeyEvent GamepadShortcuts.activateFocused()
        }

        GamepadAction.DPAD_LEFT, GamepadAction.DPAD_RIGHT -> {
            // Focused slider owns horizontal input. Otherwise apply edge-aware
            // horizontal nav: blocked edges stop pager jumps and focus escapes.
            if (GamepadFocusNav.focusedSlider) {
                return@onPreviewKeyEvent false
            }
            val left = action == GamepadAction.DPAD_LEFT
            return@onPreviewKeyEvent GamepadFocusNav.isHorizontalBlocked(left)
        }

        // Vertical focus movement is the focus system's own job.
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

    val focusManager = LocalFocusManager.current
    DisposableEffect(focusManager) {
        GamepadStickNav.focusManager = focusManager
        onDispose {
            if (GamepadStickNav.focusManager === focusManager) {
                GamepadStickNav.focusManager = null
                GamepadStickNav.reset()
            }
        }
    }

    // A press that arrives after a rotation or a page swap has no focused row
    // to move from; seed one once the first frame is laid out.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (GamepadInput.dpadMode) GamepadFocusNav.restoreFocus()
    }

    LaunchedEffect(GamepadShortcuts.rootHasFocus, GamepadInput.dpadMode, GamepadShortcuts.isDialogOpen) {
        if (GamepadInput.dpadMode && !GamepadShortcuts.rootHasFocus && !GamepadShortcuts.isDialogOpen) {
            repeat(5) {
                delay(120)
                if (GamepadShortcuts.rootHasFocus) return@LaunchedEffect
                GamepadFocusNav.restoreFocus()
            }
        }
    }

    val activeDialogCount = LocalActiveDialogCount.current
    GamepadShortcuts.isDialogOpen = activeDialogCount.value > 0

    // The right stick's per-frame driver. It suspends entirely while the stick
    // is centred (`snapshotFlow.first`), so at rest there is no frame work and
    // no drift, and it only scrolls the list the active screen registered.
    LaunchedEffect(Unit) {
        while (isActive) {
            snapshotFlow { GamepadStickScroll.axisY }
                .first { abs(it) > GamepadStickScroll.DEADZONE }

            var lastNanos = 0L
            while (isActive) {
                var delta = 0f
                var deadzone = GamepadStickScroll.DEADZONE
                withFrameNanos { frameNanos ->
                    val current = GamepadShortcuts.actions
                    deadzone = current?.scrollDeadzone ?: GamepadStickScroll.DEADZONE
                    val value = GamepadStickScroll.axisY
                    if (lastNanos != 0L && abs(value) > deadzone) {
                        val dt = (frameNanos - lastNanos) / 1_000_000_000f
                        val magnitude = (abs(value) - deadzone) / (1f - deadzone)
                        val speed = current?.scrollSpeedPxPerSecond ?: 2200f
                        delta = sign(value) * magnitude * speed * dt
                    }
                    lastNanos = frameNanos
                }

                val value = GamepadStickScroll.axisY
                if (abs(value) <= deadzone) break
                if (GamepadShortcuts.isDialogOpen) break

                if (delta != 0f) {
                    GamepadInput.onDpadKey()
                    val current = GamepadShortcuts.actions
                    current?.onScrollDelta?.invoke(delta)
                    val ls = current?.scrollState
                    if (ls != null) {
                        try { ls.scrollBy(delta) } catch (_: Throwable) {}
                    }
                    val cs = current?.shiftableScrollState
                    if (cs != null) {
                        try { cs.scrollBy(delta) } catch (_: Throwable) {}
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .onFocusChanged { focusState ->
                GamepadShortcuts.rootHasFocus = focusState.hasFocus || focusState.isFocused
            }
            .gamepadKeyWatch()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            GamepadInput.onTouch()
                            // Touch is not the stick: stop any in-flight scroll.
                            GamepadStickScroll.reset()
                        }
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

/**
 * Registers this screen's gamepad behaviour while [enabled].
 *
 * A token is allocated per screen instance, and the registration is removed on
 * dispose; a screen that drops out (pager page no longer selected, subscreen
 * popped, activity recreated) can never leave a stale handler behind or clear a
 * newer screen's registration.
 *
 * The handler object is mutated in a [SideEffect] rather than captured, so
 * changing `showFilterSheet` etc. does not churn the registration.
 */
@Composable
fun RegisterGamepadActions(
    enabled: Boolean = true,
    hasSearch: Boolean = false,
    isSearchOpen: Boolean = false,
    onSearchToggle: (() -> Unit)? = null,
    onSearchClose: (() -> Unit)? = null,
    hasTopAction: Boolean = false,
    isTopActionOpen: Boolean = false,
    onTopAction: (() -> Unit)? = null,
    onCloseOverlay: (() -> Boolean)? = null,
    onBack: (() -> Boolean)? = null,
    scrollState: LazyListState? = null,
    columnScrollState: ScrollState? = null,
    onScrollDelta: ((Float) -> Unit)? = null,
) {
    val token = remember { Any() }
    val holder = remember { GamepadShortcuts.Actions() }

    SideEffect {
        holder.hasSearch = hasSearch
        holder.isSearchOpen = isSearchOpen
        holder.onSearchToggle = onSearchToggle
        holder.onSearchClose = onSearchClose
        holder.hasTopAction = hasTopAction
        holder.isTopActionOpen = isTopActionOpen
        holder.onTopAction = onTopAction
        holder.onCloseOverlay = onCloseOverlay
        holder.onBack = onBack
        holder.scrollState = scrollState
        holder.shiftableScrollState = columnScrollState
        holder.onScrollDelta = onScrollDelta
        if (enabled) GamepadShortcuts.register(token, holder)
        else GamepadShortcuts.unregister(token)
    }

    DisposableEffect(token) {
        onDispose { GamepadShortcuts.unregister(token) }
    }
}

/**
 * Registers a list as the right stick's scroll target.
 *
 * Kept as a composable so screens that need nothing else (a plain list) can opt
 * in with one line. Screens that also have search/top actions should pass
 * `scrollState` to [RegisterGamepadActions] instead of using both.
 */
@Composable
fun GamepadStickScrollEffect(
    listState: LazyListState,
    deadzone: Float = GamepadStickScroll.DEADZONE,
    speedPxPerSecond: Float = 2200f,
    onScrollDelta: ((Float) -> Unit)? = null,
) {
    val token = remember(listState) { Any() }
    val holder = remember(listState) { GamepadShortcuts.Actions() }

    SideEffect {
        holder.scrollState = listState
        holder.onScrollDelta = onScrollDelta
        holder.scrollDeadzone = deadzone
        holder.scrollSpeedPxPerSecond = speedPxPerSecond
        GamepadShortcuts.register(token, holder)
    }

    DisposableEffect(token) {
        onDispose { GamepadShortcuts.unregister(token) }
    }
}
