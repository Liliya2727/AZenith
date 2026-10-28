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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package zx.azenith.ui.gamespace

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/** The `settings` preferences file the manager's own switches live in. */
private const val GAME_SPACE_PREFS = "settings"

/** Key behind the Game Space mode switch; the same file the theme and UI switches use. */
const val GAME_SPACE_MODE_KEY = "game_space_mode"

private const val GAME_SPACE_LAUNCHER_ALIAS = "zx.azenith.ZXGameSpaceLauncher"

/** True when the AZGameSpace launcher should be visible. */
fun isGameSpaceLauncherEnabled(context: Context): Boolean {
    val pm = context.packageManager
    val componentName = ComponentName(context, GAME_SPACE_LAUNCHER_ALIAS)
    return when (pm.getComponentEnabledSetting(componentName)) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> false
        else -> false
    }
}

/** Show or hide the AZGameSpace launcher shortcut. */
fun setGameSpaceLauncherEnabled(context: Context, enabled: Boolean) {
    val pm = context.packageManager
    val componentName = ComponentName(context, GAME_SPACE_LAUNCHER_ALIAS)
    val newState = if (enabled) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }
    pm.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP)
}

@Deprecated("Use isGameSpaceLauncherEnabled")
fun isGameSpaceEnabled(context: Context): Boolean = isGameSpaceLauncherEnabled(context)

@Deprecated("Use setGameSpaceLauncherEnabled")
fun setGameSpaceEnabled(context: Context, enabled: Boolean) {
    setGameSpaceLauncherEnabled(context, enabled)
}

// One handoff per session: an activity recreated while the mode is on (rotation, process reuse)
// must not push the shell back in front of the user.
private var handoffDone = false

fun isHandoffDone(): Boolean = handoffDone

fun setHandoffDone(done: Boolean) {
    handoffDone = done
}

fun launchGameSpaceDirectly(activity: Activity) {
    handoffDone = true
    val intent = Intent(activity, ZXGameSpaceActivity::class.java)
    activity.startActivity(intent)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
    } else {
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(0, 0)
    }
}

/**
 * The way into Game Space. Sits at the window root so the fade covers whichever screen the user is
 * standing on, and lifts once the shell has the display.
 *
 * [auto] is the difference between the two callers: the mode handoff walks in once per process and
 * gives the screen behind it a frame to compose first, while a tap fires every time and covers
 * straight away. [onLifted] lets a manual entry clear its trigger so the row can be tapped again.
 */
@Composable
private fun GameSpaceFade(
    armed: Boolean,
    auto: Boolean,
    onLifted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme
    val fade = remember { Animatable(0f) }

    LaunchedEffect(armed) {
        if (!armed) {
            if (auto) handoffDone = false
            return@LaunchedEffect
        }
        if (auto) {
            if (handoffDone) return@LaunchedEffect
            handoffDone = true
        }
        fade.animateTo(1f, motion.defaultEffectsSpec())
        context.startActivity(Intent(context, ZXGameSpaceActivity::class.java))
        // Slower than the launch itself, so a late shell leaves the manager fading back in rather
        // than cutting to it behind the transition.
        delay(320)
        fade.animateTo(0f, motion.defaultEffectsSpec())
        onLifted()
    }

    // graphicsLayer first: an alpha layer only covers what is drawn after it in the chain.
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(MaterialTheme.colorScheme.surface)
    )
}

/**
 * The mode handoff, composed at the manager's window root.
 *
 * [armed] is the mode switch itself, read live: turning it off re-arms the handoff, so enabling the
 * mode again hands off again.
 */
@Composable
fun ZXGameSpaceHandoff(armed: Boolean, modifier: Modifier = Modifier) {
    GameSpaceFade(armed = armed, auto = true, modifier = modifier)
}

/**
 * Manual entry, for a row that opens the shell without waiting for a mode change. Compose it last
 * in whichever Box holds the screen, and clear [onLifted] on the caller's trigger state.
 */
@Composable
fun ZXGameSpaceEntry(armed: Boolean, onLifted: () -> Unit, modifier: Modifier = Modifier) {
    GameSpaceFade(armed = armed, auto = false, onLifted = onLifted, modifier = modifier)
}
