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

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * State of the GameSpace game-launch transition, hosted at the Hub level.
 */
sealed interface GameLaunchState {
    data object Idle : GameLaunchState
    data class Running(
        val packageName: String,
        val progress: Float = 0f
    ) : GameLaunchState
}

/**
 * Single launch entry point for all GameSpace game launches.
 */
object GameLauncher {
    fun launch(context: Context, packageName: String, label: String? = null) {
        ZXSidePanelService.registerAppOpenedFromAzenith(packageName)
        if (label != null) {
            ZXSidePanelService.show(context, packageName, label)
        } else {
            ZXSidePanelService.show(context, packageName)
        }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

/**
 * Coordinates the launchInProgress guard and transition state at the Hub level.
 */
@Stable
class GameLaunchController {
    var state: GameLaunchState by mutableStateOf(GameLaunchState.Idle)
        private set

    val isRunning: Boolean
        get() = state is GameLaunchState.Running

    fun requestLaunch(
        scope: CoroutineScope,
        context: Context,
        packageName: String,
        label: String? = null
    ) {
        if (isRunning) return
        state = GameLaunchState.Running(packageName = packageName, progress = 0f)
        scope.launch {
            try {
                GameLauncher.launch(context, packageName, label)
                // Brief guard window against rapid double presses (A / START).
                delay(500L)
            } finally {
                state = GameLaunchState.Idle
            }
        }
    }
}
