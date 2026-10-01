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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zx.azenith.ExpressiveShapes
import zx.azenith.R


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

    // rememberDynamicColorScheme quantises a seed colour into a full Material 3
    // scheme. That is HCT colour-space work — tens of milliseconds of pure CPU —
    // and it ran inside composition, so every preset tap froze the UI for exactly
    // as long as the quantisation took. The Crossfade below could not hide that,
    // because the scheme it was animating toward had not been computed yet.
    //
    // The computation is a plain function, so it can move off the main thread.
    // While the new scheme is being built the last one keeps rendering, and
    // isSchemePending lets the caller show a loading state instead of leaving
    // the user tapping into a frozen screen.
    val seedScheme = if (isDynamic) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            else ->
                if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
        }
    } else {
        null
    }

    var resolvedScheme by remember { mutableStateOf<androidx.compose.material3.ColorScheme?>(null) }
    var isSchemePending by remember { mutableStateOf(false) }
    val schemeKey = remember(themeState, darkTheme, amoledMode) {
        listOf(themeState.keyColor, themeState.colorMode, darkTheme, amoledMode, colorSpec)
    }

    LaunchedEffect(schemeKey) {
        val seed = seedScheme?.primary ?: Color(themeState.keyColor)
        isSchemePending = true
        val computed = withContext(Dispatchers.Default) {
            dynamicColorScheme(
                seedColor = seed,
                isDark = darkTheme,
                isAmoled = amoledMode,
                specVersion = colorSpec,
                primary = seedScheme?.primary,
                secondary = seedScheme?.secondary,
                tertiary = seedScheme?.tertiary,
                neutral = seedScheme?.surface,
                neutralVariant = seedScheme?.surfaceVariant,
                error = seedScheme?.error
            )
        }
        resolvedScheme = computed
        isSchemePending = false
    }

    // The very first composition has nothing to fall back on, so use the stock
    // scheme for that single frame rather than flashing an unthemed window.
    val fallbackScheme = if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
    val colorScheme = resolvedScheme ?: fallbackScheme

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
            content = {
                Box(Modifier.fillMaxSize()) {
                    content()

                    // Building a scheme takes real CPU. Rather than let the user
                    // tap into a frozen screen, say that the preset is being
                    // applied; it fades out the moment the scheme lands.
                    AnimatedVisibility(
                        visible = isSchemePending,
                        enter = fadeIn(animationSpec = tween(120)),
                        exit = fadeOut(animationSpec = tween(220)),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        ThemeApplyingIndicator()
                    }
                }
            }
        )
    }
}

/**
 * Small centred indicator shown while a theme preset is being quantised.
 *
 * Deliberately not a blocking dialog: the content behind it stays visible and
 * interactive-looking, and the indicator fades rather than snapping, so a fast
 * preset change never produces a flash of a full-screen scrim.
 */
@Composable
private fun ThemeApplyingIndicator() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = stringResource(R.string.theme_applying),
                style = MaterialTheme.typography.labelLarge
            )
        }
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
