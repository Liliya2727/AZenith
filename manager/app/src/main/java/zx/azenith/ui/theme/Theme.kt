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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
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
    val seedColor = if (isDynamic) null else Color(themeState.keyColor)

    // The scheme is derived on a background dispatcher and the previous one is
    // kept until the new one lands. MaterialKolor quantizes the seed into an
    // HCT color space and derives every tonal role from it, which is
    // CPU-bound; running that inside composition blocks the frame the user is
    // still looking at, so the change appears as a stall and every press on
    // the theme screen lands late.
    val requestedScheme = remember { mutableStateOf<ColorScheme?>(null) }
    var isSchemePending by remember { mutableStateOf(false) }

    LaunchedEffect(darkTheme, amoledMode, isDynamic, seedColor, colorSpec) {
        val base = if (isDynamic) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                else ->
                    if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
            }
        } else {
            null
        }

        isSchemePending = requestedScheme.value != null
        val built = withContext(Dispatchers.Default) {
            if (base != null) {
                dynamicColorScheme(
                    seedColor = base.primary,
                    isDark = darkTheme,
                    isAmoled = amoledMode,
                    specVersion = colorSpec,
                    primary = base.primary,
                    secondary = base.secondary,
                    tertiary = base.tertiary,
                    neutral = base.surface,
                    neutralVariant = base.surfaceVariant,
                    error = base.error
                )
            } else {
                dynamicColorScheme(
                    seedColor = seedColor!!,
                    isDark = darkTheme,
                    isAmoled = amoledMode,
                    specVersion = colorSpec
                )
            }
        }
        requestedScheme.value = built
        isSchemePending = false
    }

    // Nothing is shown until the first scheme is ready, so the very first
    // frame is already the correct theme rather than a default one.
    val colorScheme = requestedScheme.value ?: return

    val view = androidx.compose.ui.platform.LocalView.current
    
    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }

    // The overlay blocks touches while a new scheme is being built, so a
    // second press cannot land on a screen that is mid-change. It is an M3
    // scrim + surface container, and it scales and fades in and out rather
    // than popping.
    Box(Modifier.fillMaxSize()) {
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

        AnimatedVisibility(
            visible = isSchemePending,
            enter = fadeIn(animationSpec = tween(140)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(220)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(200)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.scrim.copy(alpha = 0.45f))
                    // Consume taps so the content underneath cannot be pressed
                    // while the scheme is still being applied.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = stringResource(R.string.theme_applying),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
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
