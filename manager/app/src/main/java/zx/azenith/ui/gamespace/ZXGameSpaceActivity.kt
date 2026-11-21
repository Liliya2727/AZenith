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

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.InputDevice
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import zx.azenith.ui.component.GamepadDirectionRepeat
import zx.azenith.ui.component.GamepadInput
import zx.azenith.ui.component.GamepadStickScroll
import zx.azenith.ui.theme.AZenithTheme
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController
import zx.azenith.ui.util.BgmManager
import zx.azenith.ui.util.TapHapticState
import zx.azenith.ui.util.tapHaptic

/**
 * Hosts Game Space. Separate from [zx.azenith.MainActivity] because the shell needs a landscape,
 * immersive window that the rest of the manager must never inherit.
 */
class ZXGameSpaceActivity : ComponentActivity() {

    companion object {
        private const val TRIGGER_THRESHOLD = 0.5f
    }

    // Logical pressed state shared by the digital key and analog axis paths, so a
    // controller that reports both can't toggle an overlay twice per press.
    private var l2Held = false
    private var r2Held = false

    /** Feed a synthesized L2/R2 key through this window so the hub's key handler sees it. */
    private fun emitTrigger(down: Boolean, keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        val action = if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        super.dispatchKeyEvent(KeyEvent(now, now, action, keyCode, 0))
        Log.d("PAD", "trigger synth key=$keyCode down=$down")
    }

    /** Synthesized direction keys must dispatch through this window, not the manager's. */
    private fun wireGamepadDirectionRepeat() {
        GamepadDirectionRepeat.sendKey = { code, action, repeat ->
            val now = android.os.SystemClock.uptimeMillis()
            super@ZXGameSpaceActivity.dispatchKeyEvent(KeyEvent(now, now, action, code, repeat))
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Real L2/R2 digital keys: keep the shared held state in sync and swallow
        // a duplicate arrived after we already synthesized the same press from
        // the analog axis.
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_L2 ||
            event.keyCode == KeyEvent.KEYCODE_BUTTON_R2
        ) {
            val isL2 = event.keyCode == KeyEvent.KEYCODE_BUTTON_L2
            val held = if (isL2) l2Held else r2Held
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        if (held) {
                            Log.d("PAD", "trigger duplicate key ignored key=${event.keyCode}")
                            return true
                        }
                        if (isL2) l2Held = true else r2Held = true
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (isL2) l2Held = false else r2Held = false
                }
            }
        }
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_B || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                onBackPressedDispatcher.onBackPressed()
            }
            return true
        }
        // Physical left/right feed the same accelerating pipeline as the stick
        // and hat so a held direction sweeps the game ribbon. R1/L1 and other
        // keys still reach the hub's own onPreviewKeyEvent handler.
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        ) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    GamepadInput.onDpadKey()
                    GamepadDirectionRepeat.onDirectionDown(event.keyCode)
                }
                KeyEvent.ACTION_UP -> GamepadDirectionRepeat.releaseIf(event.keyCode)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val source = event.source
        val isJoystick = source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        val isGamepad = source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
        if (event.action == MotionEvent.ACTION_MOVE && (isJoystick || isGamepad)) {
            // Analog triggers usually arrive only as motion axes (many pads never
            // emit BUTTON_L2/R2 key events). Synthesize the digital keys the hub
            // listens for. The shared held flag means a pad that sends both paths
            // won't fire the same press twice.
            val l2Value = maxOf(
                event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
                event.getAxisValue(MotionEvent.AXIS_BRAKE)
            )
            val r2Value = maxOf(
                event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
                event.getAxisValue(MotionEvent.AXIS_GAS)
            )
            val l2Down = l2Value >= TRIGGER_THRESHOLD
            val r2Down = r2Value >= TRIGGER_THRESHOLD
            if (l2Down != l2Held) {
                l2Held = l2Down
                emitTrigger(l2Down, KeyEvent.KEYCODE_BUTTON_L2)
            }
            if (r2Down != r2Held) {
                r2Held = r2Down
                emitTrigger(r2Down, KeyEvent.KEYCODE_BUTTON_R2)
            }
            // Right stick vertical scroll (drives registered scroll state in Settings / overlays).
            val rz = event.getAxisValue(MotionEvent.AXIS_RZ)
            val ry = event.getAxisValue(MotionEvent.AXIS_RY)
            val vertical = if (kotlin.math.abs(rz) >= kotlin.math.abs(ry)) rz else ry
            GamepadStickScroll.onAxisY(vertical)
            if (kotlin.math.abs(vertical) > GamepadStickScroll.DEADZONE) {
                GamepadInput.onDpadKey()
            }

            // Hat takes priority, then the left stick. The dominant axis decides
            // the direction: horizontal walks the game ribbon (and Bento cards),
            // vertical reaches the Bento cards. Holding accelerates repeats.
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
                GamepadInput.onDpadKey()
                GamepadDirectionRepeat.onDirectionDown(keyCode)
            } else {
                GamepadDirectionRepeat.release()
            }
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wireGamepadDirectionRepeat()
        TapHapticState.init(this)
        zx.azenith.ui.util.AudioUtil.isGameHubActive = true
        zx.azenith.ui.util.AudioUtil.init(this)
        BgmManager.init(this)

        // The window background shows through until the first Compose frame, so it follows the same
        // scheme the shell wears rather than a fixed colour — a hardcoded one flashes against it.
        window.setBackgroundDrawable(ColorDrawable(windowSurface()))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attrib = window.attributes
            attrib.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attrib
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        if (ZXSidePanelService.isForceSidepanelEnabled() && !ZXSidePanelService.isServiceActive) {
            ZXSidePanelService.show(this)
        }

        setContent {
            // Material You straight through: the shell reads the same dynamic palette as the
            // manager, in whichever mode the system is in, so there is no branded dark room.
            AZenithTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .tapHaptic()
                ) {
                    ZXGameSpaceShell(
                        onExit = { finish() },
                        onExitApp = { finishAffinity() }
                    )
                }
            }
        }
    }

    /** The surface [AZenithTheme] resolves to for the saved prefs, so the window agrees with it. */
    private fun windowSurface(): Int {
        val settings = ThemeController.getAppSettings(this)
        if (settings.colorMode == ColorMode.DARKAMOLED) return Color.BLACK

        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val dark = settings.colorMode.getDarkThemeValue(systemDark)
        val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
        } else {
            if (dark) darkColorScheme() else lightColorScheme()
        }
        return scheme.surface.toArgb()
    }

    override fun onResume() {
        super.onResume()
        // This window owns direction-key dispatch while GameSpace is foreground.
        wireGamepadDirectionRepeat()
        ZXSidePanelService.hide(this)
        BgmManager.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        GamepadDirectionRepeat.release()
        GamepadStickScroll.reset()
        BgmManager.onActivityPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        zx.azenith.ui.util.AudioUtil.isGameHubActive = false
        BgmManager.onDestroy()
    }
}
