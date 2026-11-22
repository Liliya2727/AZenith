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

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R

private const val TAG = "GameLaunch"

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
 * Result of a game launch attempt.
 */
sealed interface GameLaunchResult {
    data object Success : GameLaunchResult
    data class Failure(val reason: String, val throwable: Throwable? = null) : GameLaunchResult
}

/**
 * Single launch entry point for all GameSpace game launches.
 */
object GameLauncher {

    private fun shellSingleQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun currentUserId(): Int = Process.myUid() / 100000

    private fun isRootShellAvailable(): Boolean = runCatching {
        Shell.isAppGrantedRoot() == true || Shell.getShell().isRoot
    }.getOrDefault(false)

    private fun resolveLaunchComponent(context: Context, packageName: String, useRootFallback: Boolean): String? {
        val fromPm = runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)?.component?.flattenToShortString()
        }.getOrNull()
        if (!fromPm.isNullOrBlank()) return fromPm

        if (!useRootFallback) return null

        val out = mutableListOf<String>()
        val err = mutableListOf<String>()
        val res = runCatching {
            Shell.cmd("cmd package resolve-activity --brief ${shellSingleQuote(packageName)}")
                .to(out, err)
                .exec()
        }.getOrNull() ?: return null

        val candidate = out.asReversed().firstOrNull { line ->
            val trimmed = line.trim()
            trimmed.contains('/') && !trimmed.startsWith("priority=") && !trimmed.startsWith("No activity")
        }?.trim()

        return candidate?.takeIf { res.code == 0 && it.isNotEmpty() }
    }

    private fun hasLaunchErrorText(text: String): Boolean =
        text.contains("Error") ||
            text.contains("Exception") ||
            text.contains("does not exist", ignoreCase = true)

    private fun launchViaInAppFallback(context: Context, packageName: String): GameLaunchResult {
        val intent = runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }.getOrNull() ?: return GameLaunchResult.Failure("No launch intent found for $packageName")

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        )

        return try {
            val options = ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
            context.startActivity(intent, options)
            context.findActivity()?.let { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    activity.overridePendingTransition(0, 0)
                }
            }
            GameLaunchResult.Success
        } catch (t: Throwable) {
            Log.e(TAG, "In-app fallback launch failed for $packageName", t)
            GameLaunchResult.Failure("In-app fallback failed: ${t.message}", t)
        }
    }

    /**
     * Executes the launch off the main thread via root `am start --activity-no-animation`,
     * falling back to an in-app no-animation `startActivity` if the root shell is unavailable or fails.
     */
    suspend fun launch(
        context: Context,
        packageName: String,
        label: String? = null
    ): GameLaunchResult {
        val appContext = context.applicationContext
        val result = withContext(Dispatchers.IO) {
            val rootAvailable = isRootShellAvailable()
            if (rootAvailable) {
                val component = resolveLaunchComponent(appContext, packageName, useRootFallback = true)
                if (component == null) {
                    val msg = "Unable to resolve launcher component for $packageName"
                    Log.e(TAG, msg)
                    GameLaunchResult.Failure(msg)
                } else {
                    val userId = currentUserId()
                    val cmd = "am start --activity-no-animation --user $userId " +
                        "-a android.intent.action.MAIN " +
                        "-c android.intent.category.LAUNCHER " +
                        "-f 0x10200000 " +
                        "-n ${shellSingleQuote(component)}"
                    val stdout = mutableListOf<String>()
                    val stderr = mutableListOf<String>()
                    val execResult = runCatching {
                        Shell.cmd(cmd).to(stdout, stderr).exec()
                    }.getOrElse { t ->
                        Log.e(TAG, "Root shell execution threw for $packageName", t)
                        null
                    }

                    if (execResult == null) {
                        Log.e(TAG, "Root shell unavailable during launch for $packageName; trying in-app fallback")
                        withContext(Dispatchers.Main) { launchViaInAppFallback(context, packageName) }
                    } else {
                        val combined = buildString {
                            if (stdout.isNotEmpty()) append(stdout.joinToString("\n"))
                            if (stderr.isNotEmpty()) {
                                if (isNotEmpty()) append('\n')
                                append(stderr.joinToString("\n"))
                            }
                        }
                        if (execResult.code != 0 || hasLaunchErrorText(combined)) {
                            Log.e(
                                TAG,
                                "am start failed pkg=$packageName component=$component rc=${execResult.code} out=$combined"
                            )
                            // If am start failed due to a missing/disabled activity, do not mask it;
                            // otherwise attempt the in-app fallback once.
                            if (hasLaunchErrorText(combined)) {
                                GameLaunchResult.Failure("am start error (rc=${execResult.code}): $combined")
                            } else {
                                withContext(Dispatchers.Main) { launchViaInAppFallback(context, packageName) }
                            }
                        } else {
                            Log.d(TAG, "am start succeeded pkg=$packageName component=$component")
                            GameLaunchResult.Success
                        }
                    }
                }
            } else {
                Log.w(TAG, "Root shell unavailable; using in-app no-animation fallback for $packageName")
                withContext(Dispatchers.Main) { launchViaInAppFallback(context, packageName) }
            }
        }

        if (result is GameLaunchResult.Success) {
            withContext(Dispatchers.Main) {
                ZXSidePanelService.registerAppOpenedFromAzenith(packageName)
                if (label != null) {
                    ZXSidePanelService.show(context, packageName, label)
                } else {
                    ZXSidePanelService.show(context, packageName)
                }
            }
        }

        return result
    }

    /**
     * Fire-and-forget convenience wrapper for legacy callers.
     */
    fun launchAsync(
        scope: CoroutineScope,
        context: Context,
        packageName: String,
        label: String? = null
    ) {
        scope.launch {
            val res = launch(context, packageName, label)
            if (res is GameLaunchResult.Failure) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_app_launch_fail),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
                val res = GameLauncher.launch(context, packageName, label)
                if (res is GameLaunchResult.Failure) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_app_launch_fail),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Brief guard window against rapid double presses (A / START).
                delay(500L)
            } finally {
                state = GameLaunchState.Idle
            }
        }
    }
}
