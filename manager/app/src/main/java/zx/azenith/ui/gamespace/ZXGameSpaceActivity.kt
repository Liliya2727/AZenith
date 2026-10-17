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
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import zx.azenith.ui.theme.AZenithTheme
import zx.azenith.ui.theme.ColorMode
import zx.azenith.ui.theme.ThemeController

/**
 * Hosts Game Space. Separate from [zx.azenith.MainActivity] because the shell needs a landscape,
 * immersive window that the rest of the manager must never inherit.
 */
class ZXGameSpaceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setContent {
            // Material You straight through: the shell reads the same dynamic palette as the
            // manager, in whichever mode the system is in, so there is no branded dark room.
            AZenithTheme {
                ZXGameSpaceShell(
                    onExit = { finish() },
                    onExitApp = { finishAffinity() }
                )
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
}
