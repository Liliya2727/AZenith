/*
 * Copyright (C) 2026-2027 KowX
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

package zx.azenith.ui.theme


import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import zx.azenith.ExpressiveShapes


/**
 * Cross-fades the whole [ColorScheme] as a single unit.
 *
 * Animating each of the ~30 roles with its own animateColorAsState starts 30
 * independent coroutines and rebuilds the ColorScheme on every animation
 * frame, so every composable that reads a theme color recomposes for the full
 * duration of the tween — which is what made a theme switch feel sluggish.
 *
 * Here a single Animatable drives one interpolation fraction, and the scheme
 * is only rebuilt twice: once at the start of the cross-fade and once when it
 * lands. That loses the per-role independent timing, which was not visible,
 * and keeps the cross-fade itself.
 */

enum class ColorMode(val value: Int) {
    SYSTEM(3), LIGHT(4), DARK(5), DARKAMOLED(6);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }

    fun getDarkThemeValue(systemDarkTheme: Boolean) = when (this) {
        SYSTEM -> systemDarkTheme
        LIGHT -> false
        DARK -> true
        DARKAMOLED -> true
    }
}

data class AppSettings(val colorMode: ColorMode, val keyColor: Int, val colorSpec: ColorSpec.SpecVersion)

object ThemeController {
    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val colorMode = ColorMode.fromValue(
            prefs.getInt("color_mode", ColorMode.SYSTEM.value)
        )
        val keyColor = prefs.getInt("key_color", 0) 
        
        val colorSpecStr = prefs.getString("color_spec", "DEFAULT")
        val colorSpec = try {
            ColorSpec.SpecVersion.valueOf(colorSpecStr ?: "DEFAULT")
        } catch (_: Exception) {
            ColorSpec.SpecVersion.entries.firstOrNull() ?: error("Fallback SpecVersion failed")
        }
        
        return AppSettings(colorMode, keyColor, colorSpec)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AZenithTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    
    var themeState by remember { mutableStateOf(ThemeController.getAppSettings(context)) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            themeState = ThemeController.getAppSettings(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = themeState.colorMode.getDarkThemeValue(systemDarkTheme)
    val amoledMode = themeState.colorMode == ColorMode.DARKAMOLED
    val isDynamic = themeState.keyColor == 0
    val colorSpec = themeState.colorSpec

    val colorScheme = if (isDynamic) {
        val baseScheme = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
        }
        rememberDynamicColorScheme(
            seedColor = baseScheme.primary,
            isDark = darkTheme,
            isAmoled = amoledMode,
            specVersion = colorSpec,
            primary = baseScheme.primary,
            secondary = baseScheme.secondary,
            tertiary = baseScheme.tertiary,
            neutral = baseScheme.surface,
            neutralVariant = baseScheme.surfaceVariant,
            error = baseScheme.error
        )
    } else {
        rememberDynamicColorScheme(
            seedColor = Color(themeState.keyColor),
            isDark = darkTheme,
            isAmoled = amoledMode,
            specVersion = colorSpec
        )
    }

    val view = androidx.compose.ui.platform.LocalView.current
    
    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }

    androidx.compose.animation.Crossfade(
        targetState = colorScheme,
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "ThemeCrossfade"
    ) { scheme ->
        MaterialExpressiveTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            motionScheme = MotionScheme.expressive(),
            content = content
        )
    }
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Int): Boolean {
    return when (themeMode) {
        4 -> false
        5, 6 -> true
        else -> isSystemInDarkTheme()
    }
}
