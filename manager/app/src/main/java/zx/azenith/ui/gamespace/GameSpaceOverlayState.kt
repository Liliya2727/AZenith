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
import kotlinx.coroutines.delay
import zx.azenith.ui.component.GamepadFocusNav
import zx.azenith.ui.component.GamepadInput
import zx.azenith.ui.component.GamepadShortcuts
import zx.azenith.ui.component.LocalGamepadFocusEnabled

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

    LaunchedEffect(visible) {
        if (visible) {
            wasVisible = true
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
                (GamepadInput.dpadMode || GamepadInput.controllerConnected)
            ) {
                delay(120L)
                if (!GameSpaceOverlayState.isAnyOpen) {
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
                .focusGroup()
                .focusProperties {
                    if (visible) {
                        onExit = {
                            cancelFocusChange()
                            Log.d("PAD", "gs focus trap cancelExit name=$name")
                        }
                    }
                },
            content = content,
        )
    }
}
