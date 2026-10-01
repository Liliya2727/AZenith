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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import zx.azenith.ExpressiveShapes


/**
 * Interpolates every role of a [ColorScheme] from [from] to [to] by a single
 * driven fraction.
 *
 * A [androidx.compose.animation.Crossfade] over the whole scheme keeps the
 * same problem it always had: it *removes* the previous subtree and inserts a
 * new one, so `MainScreen`'s nav back stack, scroll positions and any other
 * `remember`ed state are rebuilt and the user is dropped back to the start
 * screen. Animating the roles in place keeps every composable mounted and
 * only repaints them.
 *
 * The fraction is driven by one [Animatable] rather than one coroutine per
 * role, so the scheme is rebuilt once per frame instead of 48 times per role
 * transition.
 */
private class SchemeAnimator {
    private val fraction = Animatable(1f)
    private var from: ColorScheme? = null
    private var to: ColorScheme? = null

    /** True while a transition is in flight, so the UI can show progress. */
    var isAnimating by mutableStateOf(false)
        private set

    fun adopt(scheme: ColorScheme) {
        from = scheme
        to = scheme
        if (!isAnimating) current = scheme
    }

    /** The scheme to display, or null before the first one is known. */
    var current by mutableStateOf<ColorScheme?>(null)
        private set

    suspend fun animateTo(target: ColorScheme) {
        val previous = to ?: run { adopt(target); return }
        if (previous == target && !isAnimating) return
        from = previous
        to = target
        isAnimating = true
        fraction.snapTo(0f)
        fraction.animateTo(
            1f,
            tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
        current = target
        isAnimating = false
    }

    /**
     * Reads the animation fraction so the caller's composition re-runs on every
     * frame, and returns the interpolated scheme for that frame.
     */
    @Composable
    fun frame(): ColorScheme? {
        val f = fraction.value // observed: recomposes each animation frame
        val start = from ?: return to
        val end = to ?: return null
        if (f >= 1f) return end
        return lerpScheme(start, end, f)
    }
}

private fun lerpScheme(from: ColorScheme, to: ColorScheme, f: Float): ColorScheme {
    fun mix(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color) =
        androidx.compose.ui.graphics.lerp(a, b, f)
    return to.copy(
        primary = mix(from.primary, to.primary),
        onPrimary = mix(from.onPrimary, to.onPrimary),
        primaryContainer = mix(from.primaryContainer, to.primaryContainer),
        onPrimaryContainer = mix(from.onPrimaryContainer, to.onPrimaryContainer),
        inversePrimary = mix(from.inversePrimary, to.inversePrimary),
        primaryFixed = mix(from.primaryFixed, to.primaryFixed),
        primaryFixedDim = mix(from.primaryFixedDim, to.primaryFixedDim),
        onPrimaryFixed = mix(from.onPrimaryFixed, to.onPrimaryFixed),
        onPrimaryFixedVariant = mix(from.onPrimaryFixedVariant, to.onPrimaryFixedVariant),
        secondary = mix(from.secondary, to.secondary),
        onSecondary = mix(from.onSecondary, to.onSecondary),
        secondaryContainer = mix(from.secondaryContainer, to.secondaryContainer),
        onSecondaryContainer = mix(from.onSecondaryContainer, to.onSecondaryContainer),
        secondaryFixed = mix(from.secondaryFixed, to.secondaryFixed),
        secondaryFixedDim = mix(from.secondaryFixedDim, to.secondaryFixedDim),
        onSecondaryFixed = mix(from.onSecondaryFixed, to.onSecondaryFixed),
        onSecondaryFixedVariant = mix(from.onSecondaryFixedVariant, to.onSecondaryFixedVariant),
        tertiary = mix(from.tertiary, to.tertiary),
        onTertiary = mix(from.onTertiary, to.onTertiary),
        tertiaryContainer = mix(from.tertiaryContainer, to.tertiaryContainer),
        onTertiaryContainer = mix(from.onTertiaryContainer, to.onTertiaryContainer),
        tertiaryFixed = mix(from.tertiaryFixed, to.tertiaryFixed),
        tertiaryFixedDim = mix(from.tertiaryFixedDim, to.tertiaryFixedDim),
        onTertiaryFixed = mix(from.onTertiaryFixed, to.onTertiaryFixed),
        onTertiaryFixedVariant = mix(from.onTertiaryFixedVariant, to.onTertiaryFixedVariant),
        error = mix(from.error, to.error),
        onError = mix(from.onError, to.onError),
        errorContainer = mix(from.errorContainer, to.errorContainer),
        onErrorContainer = mix(from.onErrorContainer, to.onErrorContainer),
        background = mix(from.background, to.background),
        onBackground = mix(from.onBackground, to.onBackground),
        surface = mix(from.surface, to.surface),
        onSurface = mix(from.onSurface, to.onSurface),
        surfaceVariant = mix(from.surfaceVariant, to.surfaceVariant),
        onSurfaceVariant = mix(from.onSurfaceVariant, to.onSurfaceVariant),
        surfaceTint = mix(from.surfaceTint, to.surfaceTint),
        inverseSurface = mix(from.inverseSurface, to.inverseSurface),
        inverseOnSurface = mix(from.inverseOnSurface, to.inverseOnSurface),
        surfaceBright = mix(from.surfaceBright, to.surfaceBright),
        surfaceDim = mix(from.surfaceDim, to.surfaceDim),
        surfaceContainerLowest = mix(from.surfaceContainerLowest, to.surfaceContainerLowest),
        surfaceContainerLow = mix(from.surfaceContainerLow, to.surfaceContainerLow),
        surfaceContainer = mix(from.surfaceContainer, to.surfaceContainer),
        surfaceContainerHigh = mix(from.surfaceContainerHigh, to.surfaceContainerHigh),
        surfaceContainerHighest = mix(from.surfaceContainerHighest, to.surfaceContainerHighest),
        outline = mix(from.outline, to.outline),
        outlineVariant = mix(from.outlineVariant, to.outlineVariant),
        scrim = mix(from.scrim, to.scrim)
    )
}

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

    val targetScheme = if (isDynamic) {
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

    // `targetScheme` is the scheme the current settings ask for. It is produced
    // above and may be recomputed while the user is still on the screen that
    // offers the choice, so the transition is driven from the target rather
    // than from the value being displayed.
    val animator = remember { SchemeAnimator() }
    if (animator.current == null) animator.adopt(targetScheme)

    LaunchedEffect(targetScheme) {
        animator.animateTo(targetScheme)
    }

    val frameScheme = animator.frame()
    val visibleScheme = frameScheme ?: targetScheme
    val isThemeBusy = animator.isAnimating

    Box(Modifier.fillMaxSize()) {
        MaterialExpressiveTheme(
            colorScheme = visibleScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            motionScheme = MotionScheme.expressive(),
            content = content
        )

        // M3 linear progress indicator, shown only while the new scheme is
        // still being built. It sits above the content rather than replacing
        // it, so the screen the user is on never goes away and the transition
        // is visible rather than a stall.
        AnimatedVisibility(
            visible = isThemeBusy,
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(260)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = 4.dp)
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
