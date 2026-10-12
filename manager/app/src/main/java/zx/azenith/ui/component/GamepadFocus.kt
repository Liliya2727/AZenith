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

package zx.azenith.ui.component

import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
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
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
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
import kotlinx.coroutines.delay

/**
 * The shape of the row being composed, so a focus outline can follow the row's
 * own corners. The joined groups in `ExpressiveList` round only their outer
 * ends, and a hard-coded radius would cut visibly across those.
 */
val LocalRowShape = compositionLocalOf<Shape> { RoundedCornerShape(4.dp) }

/**
 * D-pad, TV-remote and gamepad state for the whole app.
 *
 * Deliberately a plain object rather than a CompositionLocal: sheets and dialogs
 * are separate windows with their own composition, and they need the same flag
 * their parent screen reads.
 */
object GamepadInput {
    /** True while a d-pad / gamepad is driving the UI. A finger going down clears it. */
    var dpadMode: Boolean by mutableStateOf(false)
        private set

    /** A gamepad or d-pad-capable device is attached. */
    var controllerConnected: Boolean by mutableStateOf(false)
        private set

    /** Whether the focus outline should be drawn at all. */
    val active: Boolean get() = dpadMode || controllerConnected

    private val DPAD_KEYCODES = setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_SELECT
    )

    fun isDpadKey(keyCode: Int): Boolean = keyCode in DPAD_KEYCODES

    fun onDpadKey() {
        if (!dpadMode) dpadMode = true
    }

    fun onTouch() {
        if (dpadMode) dpadMode = false
    }

    fun onControllerPresence(connected: Boolean) {
        if (controllerConnected != connected) controllerConnected = connected
    }

    /**
     * A controller, remote or similar is attached.
     *
     * Deliberately not "any device reporting SOURCE_DPAD": the phone's own
     * `mtk-kpd` keypad advertises `KEYBOARD | DPAD` while being an internal
     * keypad, which would leave the outline permanently enabled on a phone with
     * no controller attached. A real pad reports SOURCE_GAMEPAD, and a remote is
     * an external device.
     */
    fun hasDpadDevice(): Boolean = InputDevice.getDeviceIds().any { id ->
        val device = InputDevice.getDevice(id) ?: return@any false
        if (!device.isEnabled) return@any false
        val sources = device.sources
        sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            (sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD && device.isExternal)
    }
}

/**
 * Where the bottom bar hands focus back to the page.
 *
 * Compose cannot search its way from the bar up into the page here: the pager keeps
 * all four pages composed at the same geometry, so the upward search has four
 * equally good candidates and gives up, leaving focus on the bar. Whichever page is
 * on screen publishes its entry point, and the bar jumps to it instead of searching.
 */
object GamepadFocusNav {
    /** Entry point into the page on screen, published by the pager. */
    var pageEntry: FocusRequester? by mutableStateOf(null)
        private set

    /**
     * Entry point of whatever screen is on top — a pager page or a subscreen.
     *
     * Needed to hand the d-pad back after an overlay or a subscreen closes: the focused
     * node is disposed with the screen it lived in, which leaves nothing focused at all
     * and the d-pad dead until the user touches the screen.
     */
    var lastEntry: FocusRequester? by mutableStateOf(null)
        private set

    fun publishPageEntry(requester: FocusRequester?) {
        if (pageEntry !== requester) pageEntry = requester
    }

    fun publishLastEntry(requester: FocusRequester?) {
        if (lastEntry !== requester) lastEntry = requester
    }

    /**
     * Rows that have held focus, newest last. A hand-back walks this backwards so it lands
     * on the row the user was actually on; asking a screen's container instead picks
     * whatever the focus search happens to see first, which can be an off-screen node.
     * A popped subscreen's rows stay in here, detached, and fail their request harmlessly.
     */
    private val focusHistory = ArrayDeque<FocusRequester>()

    fun publishLastFocused(requester: FocusRequester) {
        focusHistory.remove(requester)
        focusHistory.addLast(requester)
        while (focusHistory.size > 8) focusHistory.removeFirst()
    }

    /**
     * Hands focus back to the screen on top, falling back through the recent rows and then
     * the pager page when the remembered requesters have already left the tree.
     */
    fun restoreFocus(): Boolean {
        val candidates = focusHistory.toList().asReversed() + listOfNotNull(lastEntry, pageEntry)
        return candidates.any { runCatching { it.requestFocus() }.getOrDefault(false) }
    }
}

/**
 * Draws the focus outline on the row it is applied to.
 *
 * Place it in the modifier chain *before* the row's `clickable`/`toggleable`:
 * `onFocusChanged` only observes the focus target that follows it, so an outline
 * applied after the clickable never sees focus at all and silently never draws.
 *
 * The outline is only visible while a d-pad or gamepad is in use, so a touch-driven
 * session looks exactly as it did before.
 */
@Composable
fun Modifier.gamepadFocusOutline(
    shape: Shape = LocalRowShape.current,
    width: Dp = 2.dp,
    register: Boolean = false,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    // Only list rows register: they are what a hand-back should return to, and the
    // buttons inside dialogs would otherwise leave the registry pointing into a dialog
    // that is about to disappear.
    val requester = remember { FocusRequester() }
    val alpha by animateFloatAsState(
        targetValue = if (focused && GamepadInput.active) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "GamepadFocusOutline"
    )
    return this
        .onFocusChanged {
            focused = it.isFocused
            if (it.isFocused && register) GamepadFocusNav.publishLastFocused(requester)
        }
        .then(if (register) focusRequester(requester) else Modifier)
        .then(
            if (alpha > 0.01f) {
                Modifier.border(
                    width = width,
                    // onSurface, not primary: the row being focused can itself be
                    // painted with the accent (the selected navbar pill), and an
                    // accent ring on an accent fill is invisible.
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
}

/**
 * Marks a screen's scrollable root as its d-pad entry point.
 *
 * Navigating away disposes the node that held focus along with the screen it lived in, so a
 * screen that does not re-seed leaves the d-pad with nothing to move from and reads as
 * unreachable. The group makes the seed land on the first row rather than on the container,
 * and gives a direction-aware entry when the search comes in from the side.
 */
@Composable
fun Modifier.gamepadScreenEntry(): Modifier {
    val requester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        GamepadFocusNav.publishLastEntry(requester)
        // The screen is still running its enter transition, and its rows only appear once the
        // root-side property reads come back, so retry until something takes focus.
        repeat(8) {
            delay(150)
            if (entered) return@LaunchedEffect
            // requestFocus on a focus group does not descend into a lazy list's items, so fall
            // back to the traversal the tab key performs, which does.
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
 * Marks the app as d-pad driven when a d-pad key is pressed in the window this is
 * attached to, which is what makes [gamepadFocusOutline] draw.
 *
 * Key events are the signal to trust: a d-pad press proves a d-pad is in use, while
 * device detection only says one is plugged in. Compose delivers preview key events
 * to this node even while nothing is focused, so the first press already counts.
 * Sheets and dialogs are separate windows that the activity never sees, which is why
 * they apply this themselves.
 */
fun Modifier.gamepadKeyWatch(): Modifier = this.onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown && GamepadInput.isDpadKey(event.nativeKeyEvent.keyCode)) {
        GamepadInput.onDpadKey()
    }
    false
}

/**
 * Root of a window's content. Keeps [GamepadInput] fed and hands the UI back to
 * touch as soon as a finger goes down.
 *
 * Deliberately without a focus anchor: a focusable anchor becomes the first node the
 * window focuses and stays in the focus order, so a d-pad moving off the bottom bar
 * would step onto it — invisible, so the outline simply disappears. Compose focuses
 * the first focusable node by itself, and an outline only draws while d-pad input is
 * in use, so a touch-driven session still looks untouched.
 *
 * The content lambda keeps `BoxScope` because the callers place dialogs and overlays
 * in it with `Modifier.align`.
 */
@Composable
fun GamepadFocusRoot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // A connection can happen at any time, and there is no cheaper signal than
    // polling for it: the app registers no input-device listener.
    LaunchedEffect(Unit) {
        while (true) {
            GamepadInput.onControllerPresence(GamepadInput.hasDpadDevice())
            delay(3000)
        }
    }

    var hasFocus by remember { mutableStateOf(false) }
    val activeDialogCount = LocalActiveDialogCount.current

    Box(
        modifier = modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .onPreviewKeyEvent { event ->
                val isDpadPress = event.type == KeyEventType.KeyDown &&
                    GamepadInput.isDpadKey(event.nativeKeyEvent.keyCode)
                if (!isDpadPress) return@onPreviewKeyEvent false
                GamepadInput.onDpadKey()
                // Nothing focused: the node that held it was disposed with the screen or the
                // overlay it lived in, so a move would go nowhere. Seed the screen instead of
                // swallowing the press.
                if (!hasFocus && activeDialogCount.value == 0 && GamepadFocusNav.restoreFocus()) {
                    return@onPreviewKeyEvent true
                }
                false
            }
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

/** Provides [LocalRowShape] for the rows inside one group, so the outline matches their corners. */
@Composable
fun ProvideRowShape(shape: Shape, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRowShape provides shape) { content() }
}
