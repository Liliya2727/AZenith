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

import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import zx.azenith.ui.component.GamepadDirectionRepeat
import zx.azenith.ui.component.GamepadFocusNav
import zx.azenith.ui.component.GamepadInput
import zx.azenith.ui.component.GamepadShortcuts
import zx.azenith.ui.component.GamepadStickScroll
import zx.azenith.ui.component.LocalGamepadFocusEnabled

/**
 * Tracks the last input source in GameSpace (Gamepad vs Touch/Keyboard) from
 * real [KeyEvent] / [MotionEvent] source flags so the Search IME opens on touch
 * or physical keyboard input and stays hidden when Search is opened by gamepad.
 */
internal object GameSpaceInputSource {
    enum class Source {
        TouchOrKeyboard,
        Gamepad,
    }

    var lastSource by mutableStateOf(Source.TouchOrKeyboard)
        private set

    val isGamepadLast: Boolean
        get() = lastSource == Source.Gamepad

    private fun update(next: Source, reason: String) {
        if (lastSource != next) {
            lastSource = next
            Log.d("PAD", "gs inputSource=$next reason=$reason")
        }
    }

    fun onTouchEvent(event: MotionEvent) {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            update(Source.TouchOrKeyboard, "touch")
        }
    }

    fun onMotionEvent(event: MotionEvent) {
        val source = event.source
        val isJoystick = (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        val isGamepad = (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        if (isJoystick || isGamepad) {
            update(Source.Gamepad, "motion")
        }
    }

    fun onKeyEvent(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN) return
        val source = event.source
        val isGamepadSource = (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (source and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        val isGamepadKey = KeyEvent.isGamepadButton(event.keyCode) ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER
        if (isGamepadSource || isGamepadKey) {
            update(Source.Gamepad, "key:${event.keyCode}")
        } else if ((source and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD &&
            event.keyCode != KeyEvent.KEYCODE_BACK &&
            event.keyCode != KeyEvent.KEYCODE_ESCAPE
        ) {
            update(Source.TouchOrKeyboard, "keyboard:${event.keyCode}")
        }
    }
}

/**
 * Shared observable overlay registry and focus confinement for all GameSpace popups.
 *
 * Every GameSpace popup registers an entry via [acquire] / [release] inside a
 * [DisposableEffect] pair so a cancelled animation cannot leak. While
 * [overlayCount] > 0, the base GameSpace screen disables its own focus targets
 * and suppresses base gamepad shortcuts (ribbon movement, L1/R1, X/Y/Start).
 */
internal object GameSpaceOverlayState {
    private data class Entry(val token: Any, val name: String)

    private val stack = mutableStateListOf<Entry>()

    val overlayCount: Int
        get() = stack.size

    val isAnyOpen: Boolean
        get() = stack.isNotEmpty()

    /** Last focused requester on the base GameSpace screen before any popup opened. */
    var lastBaseRequester: FocusRequester? = null
        private set

    fun recordBaseFocus(requester: FocusRequester) {
        if (stack.isEmpty()) {
            lastBaseRequester = requester
        }
    }

    fun clearBaseFocus(requester: FocusRequester) {
        if (lastBaseRequester === requester) {
            lastBaseRequester = null
        }
    }

    fun acquire(token: Any, name: String) {
        if (stack.none { it.token === token }) {
            stack.add(Entry(token, name))
            Log.d("PAD", "gs popup open name=$name count=${stack.size}")
        }
    }

    fun release(token: Any) {
        val idx = stack.indexOfFirst { it.token === token }
        if (idx >= 0) {
            val removed = stack.removeAt(idx)
            Log.d("PAD", "gs popup close name=${removed.name} count=${stack.size}")
        }
    }

    fun restoreBaseFocus(): Boolean {
        val target = lastBaseRequester
        if (target != null) {
            val restored = runCatching { target.requestFocus() }.getOrDefault(false)
            if (restored) {
                GamepadShortcuts.reassertActivation(target)
                Log.d("PAD", "gs popup focus restored to saved base requester")
                return true
            }
        }
        val fallback = GamepadFocusNav.restoreFocus()
        Log.d("PAD", "gs popup focus restore fallback=$fallback")
        return fallback
    }
}

/**
 * Shared popup focus-confinement container for GameSpace overlays.
 *
 * - Increments/decrements [GameSpaceOverlayState] in a [DisposableEffect] pair.
 * - Enables [LocalGamepadFocusEnabled] inside the popup so [gamepadFocusOutline]
 *   and `ZenithSlider` (`slider = true`) are active.
 * - Traps D-pad focus exits with `onExit = { cancelFocusChange() }`.
 * - Seeds focus on the first control when opened (~500 ms cap) and restores the
 *   previously focused base element when the last popup closes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun GameSpacePopupContainer(
    visible: Boolean,
    name: String,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester? = null,
    seedFocusOnOpen: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val token = remember { Any() }
    val rootRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var hasFocusInside by remember { mutableStateOf(false) }
    var wasVisible by remember { mutableStateOf(false) }

    DisposableEffect(visible, token) {
        if (visible) {
            GameSpaceOverlayState.acquire(token, name)
        }
        onDispose {
            GameSpaceOverlayState.release(token)
        }
    }

    LaunchedEffect(visible, seedFocusOnOpen) {
        if (visible) {
            wasVisible = true
            if (!seedFocusOnOpen) {
                Log.d("PAD", "gs popup seed skipped name=$name inputSource=${GameSpaceInputSource.lastSource}")
                return@LaunchedEffect
            }
            val startMs = SystemClock.uptimeMillis()
            withFrameNanos { }
            var seeded = false
            while (SystemClock.uptimeMillis() - startMs < 500L) {
                val explicit = firstFocusRequester != null &&
                    runCatching { firstFocusRequester.requestFocus() }.getOrDefault(false)
                if (explicit) {
                    seeded = true
                    break
                }
                val gotRoot = runCatching { rootRequester.requestFocus() }.getOrDefault(false)
                val stepped = if (gotRoot || hasFocusInside) {
                    runCatching { focusManager.moveFocus(FocusDirection.Next) }.getOrDefault(false) ||
                        runCatching { focusManager.moveFocus(FocusDirection.Down) }.getOrDefault(false) ||
                        runCatching { focusManager.moveFocus(FocusDirection.Enter) }.getOrDefault(false)
                } else {
                    false
                }
                if (stepped || GamepadShortcuts.hasRowFocus || GamepadFocusNav.focusedSlider) {
                    seeded = true
                    break
                }
                delay(32L)
            }
            Log.d(
                "PAD",
                "gs popup seed name=$name seeded=$seeded rowFocus=${GamepadShortcuts.hasRowFocus} " +
                    "slider=${GamepadFocusNav.focusedSlider} ms=${SystemClock.uptimeMillis() - startMs}"
            )
        } else if (wasVisible) {
            wasVisible = false
            if (!GameSpaceOverlayState.isAnyOpen &&
                GameSpaceInputSource.isGamepadLast &&
                (GamepadInput.dpadMode || GamepadInput.controllerConnected)
            ) {
                delay(120L)
                if (!GameSpaceOverlayState.isAnyOpen && GameSpaceInputSource.isGamepadLast) {
                    GameSpaceOverlayState.restoreBaseFocus()
                }
            }
        }
    }

    CompositionLocalProvider(LocalGamepadFocusEnabled provides true) {
        Box(
            modifier = modifier
                .focusRequester(rootRequester)
                .onFocusChanged { hasFocusInside = it.hasFocus }
                .focusProperties {
                    if (visible) {
                        onExit = {
                            cancelFocusChange()
                            Log.d("PAD", "gs focus trap cancelExit name=$name")
                        }
                    }
                }
                .focusGroup(),
            content = content,
        )
    }
}

/**
 * Shared gamepad generic motion handler for GameSpace windows (Activity and Dialog windows).
 *
 * Translates right-stick vertical movement ([MotionEvent.AXIS_RZ] / [MotionEvent.AXIS_RY]) into
 * [GamepadStickScroll.onAxisY] and hat/left-stick axes into [GamepadDirectionRepeat] D-pad events.
 */
internal object GameSpaceGamepadMotion {
    const val TRIGGER_THRESHOLD = 0.5f

    fun handleStickAndHatMotion(event: MotionEvent): Boolean {
        val source = event.source
        val isJoystick = (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        val isGamepad = (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        if (event.action != MotionEvent.ACTION_MOVE || (!isJoystick && !isGamepad)) {
            return false
        }

        // Right stick vertical scroll (drives registered scroll state in Settings / overlays / dialogs).
        val rz = event.getAxisValue(MotionEvent.AXIS_RZ)
        val ry = event.getAxisValue(MotionEvent.AXIS_RY)
        val vertical = if (kotlin.math.abs(rz) >= kotlin.math.abs(ry)) rz else ry
        GamepadStickScroll.onAxisY(vertical)
        if (kotlin.math.abs(vertical) > GamepadStickScroll.DEADZONE) {
            GameSpaceInputSource.onMotionEvent(event)
            GamepadInput.onDpadKey()
        }

        // Hat takes priority, then the left stick.
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val stickX = event.getAxisValue(MotionEvent.AXIS_X)
        val stickY = event.getAxisValue(MotionEvent.AXIS_Y)
        val x = if (kotlin.math.abs(hatX) >= kotlin.math.abs(stickX)) hatX else stickX
        val y = if (kotlin.math.abs(hatY) >= kotlin.math.abs(stickY)) hatY else stickY
        val keyCode = when {
            kotlin.math.abs(x) >= kotlin.math.abs(y) && kotlin.math.abs(x) > 0.5f ->
                if (x < 0f) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
            kotlin.math.abs(y) > 0.5f ->
                if (y < 0f) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
            else -> null
        }
        if (keyCode != null) {
            GameSpaceInputSource.onMotionEvent(event)
            GamepadInput.onDpadKey()
            GamepadDirectionRepeat.onDirectionDown(keyCode)
        } else {
            GamepadDirectionRepeat.release()
        }
        return true
    }
}

/**
 * Attaches [GameSpaceGamepadMotion.handleStickAndHatMotion] and redirects
 * [GamepadDirectionRepeat.sendKey] to the current [Dialog] window's root view while composed.
 *
 * Separate [Dialog] windows have their own `Window`/`DecorView`, so generic motion events
 * (left stick, hat switch, right stick) and synthesized D-pad key events never reach
 * [ZXGameSpaceActivity.dispatchGenericMotionEvent] unless bound to the dialog's view.
 */
@Composable
internal fun BindGameSpaceDialogGamepad() {
    val view = LocalView.current
    DisposableEffect(view) {
        val rootView = view.rootView
        val prevSendKey = GamepadDirectionRepeat.sendKey
        GamepadDirectionRepeat.sendKey = { code, action, repeat ->
            val now = SystemClock.uptimeMillis()
            rootView.dispatchKeyEvent(KeyEvent(now, now, action, code, repeat))
        }
        rootView.setOnGenericMotionListener { _, event ->
            GameSpaceGamepadMotion.handleStickAndHatMotion(event)
        }
        onDispose {
            GamepadDirectionRepeat.release()
            GamepadStickScroll.reset()
            rootView.setOnGenericMotionListener(null)
            GamepadDirectionRepeat.sendKey = prevSendKey
        }
    }
}
