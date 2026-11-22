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
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.GamepadDirectionRepeat
import zx.azenith.ui.component.GamepadStickScroll
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.BgmManager

private const val TAG = "GameLaunch"

const val PREF_GAME_LAUNCH_TRANSITION_ENABLED = "gamespace_launch_transition_enabled"
private const val SETTINGS_PREFS = "settings"

fun isGameLaunchTransitionEnabled(context: Context): Boolean =
    context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_GAME_LAUNCH_TRANSITION_ENABLED, true)

// ============================================================================
// Game-launch transition tuning constants (timings in ms, magnitudes as floats)
// ============================================================================
internal const val CONTENT_FADE_START_MS = 0
internal const val CONTENT_FADE_DURATION_MS = 280
internal const val CONTENT_END_SCALE = 0.97f

internal const val SHAPES_FLYOUT_START_MS = 0
internal const val SHAPES_FLYOUT_DURATION_MS = 450
internal const val SHAPES_MAX_STAGGER_MS = 120
internal const val SHAPES_END_SCALE = 1.22f
internal val SHAPES_FLYOUT_DISTANCE_DP = 420.dp

internal const val SCRIM_START_MS = 120
internal const val SCRIM_DURATION_MS = 400 // 120..520 ms
internal const val SCRIM_MAX_ALPHA = 0.94f

internal const val WORDMARK_START_MS = 380
internal const val WORDMARK_DURATION_MS = 420 // 380..800 ms
internal const val WORDMARK_START_SCALE = 0.90f
internal const val WORDMARK_END_SCALE = 1.00f

internal const val BGM_DUCK_TARGET = 0.12f
internal const val BGM_DUCK_FADE_MS = 220L

internal const val LAUNCH_AT_MS = 700L
internal const val WATCHDOG_TIMEOUT_MS = 4000L
internal const val RECOVERY_FADE_MS = 200
internal const val IMMEDIATE_GUARD_MS = 500L

/**
 * Shared lock flag read by [ZXGameSpaceActivity] to swallow non-system key and
 * motion events while a game-launch transition is running.
 */
object GameLaunchInputLock {
    @Volatile
    var isLocked: Boolean = false
}

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
                    // Note: -f MUST precede --activity-no-animation because am start's -f
                    // replaces the intent flags rather than OR-ing them; putting
                    // --activity-no-animation after -f produces flg=0x10210000 (f=NO_ANIMATION).
                    val cmd = "am start --user $userId " +
                        "-a android.intent.action.MAIN " +
                        "-c android.intent.category.LAUNCHER " +
                        "-f 0x10200000 " +
                        "--activity-no-animation " +
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
 * Coordinates the launchInProgress guard, transition animations, input lock, and watchdog
 * at the Hub level. All animated values are [Animatable] instances read inside
 * `graphicsLayer` lambdas so there is zero per-frame recomposition.
 */
@Stable
class GameLaunchController {
    var state: GameLaunchState by mutableStateOf(GameLaunchState.Idle)
        private set

    val isRunning: Boolean
        get() = state is GameLaunchState.Running

    // Driven by Animatables and read strictly inside graphicsLayer / draw lambdas.
    val contentFadeAnim = Animatable(0f) // 0 = normal, 1 = faded out
    val shapesElapsedMsAnim = Animatable(0f) // 0..(SHAPES_FLYOUT_DURATION_MS + SHAPES_MAX_STAGGER_MS)
    val scrimAlphaAnim = Animatable(0f) // 0..SCRIM_MAX_ALPHA
    val wordmarkAlphaAnim = Animatable(0f) // 0..1
    val wordmarkScaleAnim = Animatable(WORDMARK_START_SCALE) // WORDMARK_START_SCALE..WORDMARK_END_SCALE

    private var activeJob: Job? = null
    private var watchdogJob: Job? = null
    private var recoveryJob: Job? = null

    @Volatile
    var isActivityPaused: Boolean = false

    private fun animatorDurationScale(context: Context): Float = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }.getOrDefault(1f)

    private fun lockInput(locked: Boolean) {
        GameLaunchInputLock.isLocked = locked
        if (locked) {
            GamepadDirectionRepeat.release()
            GamepadStickScroll.reset()
        }
    }

    fun requestLaunch(
        scope: CoroutineScope,
        context: Context,
        packageName: String,
        label: String? = null
    ) {
        if (isRunning) return

        val durationScale = animatorDurationScale(context)
        val transitionEnabled = isGameLaunchTransitionEnabled(context)
        if (!transitionEnabled || durationScale <= 0f) {
            // Transition disabled or system animations off: skip transition and fire launch immediately.
            state = GameLaunchState.Running(packageName = packageName, progress = 1f)
            lockInput(true)
            activeJob?.cancel()
            watchdogJob?.cancel()
            recoveryJob?.cancel()
            recoveryJob = null
            activeJob = scope.launch {
                try {
                    val res = try {
                        GameLauncher.launch(context, packageName, label)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        Log.e(TAG, "Unexpected launch error for $packageName", t)
                        GameLaunchResult.Failure("Unexpected error: ${t.message}", t)
                    }
                    if (res is GameLaunchResult.Failure) {
                        Log.e(TAG, "Game launch failed for $packageName: ${res.reason}", res.throwable)
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_app_launch_fail),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        delay(IMMEDIATE_GUARD_MS)
                    }
                } finally {
                    lockInput(false)
                    state = GameLaunchState.Idle
                }
            }
            return
        }

        state = GameLaunchState.Running(packageName = packageName, progress = 0f)
        lockInput(true)
        isActivityPaused = false
        activeJob?.cancel()
        watchdogJob?.cancel()
        recoveryJob?.cancel()
        recoveryJob = null

        // t = 0 ms: duck BGM and play preloaded gamelaunch sound once.
        BgmManager.duckForLaunch(BGM_DUCK_TARGET, BGM_DUCK_FADE_MS)
        AudioUtil.playGameLaunch()

        activeJob = scope.launch {
            try {
                // Reset channels cleanly before starting.
                contentFadeAnim.snapTo(0f)
                shapesElapsedMsAnim.snapTo(0f)
                scrimAlphaAnim.snapTo(0f)
                wordmarkAlphaAnim.snapTo(0f)
                wordmarkScaleAnim.snapTo(WORDMARK_START_SCALE)

                // 0..280 ms: UI content fades 1 -> 0 with accelerating easing & slight scale-down.
                val contentJob = launch {
                    if (CONTENT_FADE_START_MS > 0) delay(CONTENT_FADE_START_MS.toLong())
                    contentFadeAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = CONTENT_FADE_DURATION_MS,
                            easing = FastOutLinearInEasing
                        )
                    )
                }

                // 0..450 ms (+ up to 120 ms stagger): floating shapes fly out radially.
                val shapesTotalMs = SHAPES_FLYOUT_DURATION_MS + SHAPES_MAX_STAGGER_MS
                val shapesJob = launch {
                    if (SHAPES_FLYOUT_START_MS > 0) delay(SHAPES_FLYOUT_START_MS.toLong())
                    shapesElapsedMsAnim.animateTo(
                        targetValue = shapesTotalMs.toFloat(),
                        animationSpec = tween(
                            durationMillis = shapesTotalMs,
                            easing = LinearEasing
                        )
                    )
                }

                // 120..520 ms: scrim darkens to near-opaque.
                val scrimJob = launch {
                    delay(SCRIM_START_MS.toLong())
                    scrimAlphaAnim.animateTo(
                        targetValue = SCRIM_MAX_ALPHA,
                        animationSpec = tween(
                            durationMillis = SCRIM_DURATION_MS,
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                // 380..800 ms: centered "AZenith" wordmark fades in with slight scale-up and stays.
                val wordmarkJob = launch {
                    delay(WORDMARK_START_MS.toLong())
                    launch {
                        wordmarkAlphaAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = WORDMARK_DURATION_MS,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                    launch {
                        wordmarkScaleAnim.animateTo(
                            targetValue = WORDMARK_END_SCALE,
                            animationSpec = tween(
                                durationMillis = WORDMARK_DURATION_MS,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }

                // Wait until LAUNCH_AT_MS, then fire the launch off the main thread.
                delay(LAUNCH_AT_MS)
                val res = try {
                    GameLauncher.launch(context, packageName, label)
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    Log.e(TAG, "Unexpected launch error for $packageName", t)
                    GameLaunchResult.Failure("Unexpected error: ${t.message}", t)
                }
                if (res is GameLaunchResult.Failure) {
                    Log.e(TAG, "Game launch failed for $packageName: ${res.reason}", res.throwable)
                    contentJob.cancel()
                    shapesJob.cancel()
                    scrimJob.cancel()
                    wordmarkJob.cancel()
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_app_launch_fail),
                        Toast.LENGTH_SHORT
                    ).show()
                    reverseToIdle(scope)
                } else {
                    // Start watchdog in case the game crashes on start or never takes focus.
                    watchdogJob = scope.launch {
                        delay(WATCHDOG_TIMEOUT_MS)
                        if (isRunning && !isActivityPaused) {
                            Log.w(
                                TAG,
                                "Launch transition watchdog triggered for $packageName after ${LAUNCH_AT_MS + WATCHDOG_TIMEOUT_MS}ms"
                            )
                            reverseToIdle(scope)
                        }
                    }
                }
            } catch (_: CancellationException) {
                // Lifecycle pause/reset cancelled the transition.
            }
        }
    }

    /**
     * Smoothly fades the Hub UI and floating shapes back in and unlocks input.
     */
    fun reverseToIdle(scope: CoroutineScope) {
        activeJob?.cancel()
        activeJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        lockInput(false)
        state = GameLaunchState.Idle
        BgmManager.restoreFromLaunch(RECOVERY_FADE_MS.toLong())

        if (recoveryJob?.isActive == true) return
        recoveryJob = scope.launch {
            launch {
                contentFadeAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(RECOVERY_FADE_MS, easing = FastOutSlowInEasing)
                )
            }
            launch {
                shapesElapsedMsAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(RECOVERY_FADE_MS, easing = FastOutSlowInEasing)
                )
            }
            launch {
                scrimAlphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(RECOVERY_FADE_MS, easing = FastOutSlowInEasing)
                )
            }
            launch {
                wordmarkAlphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(RECOVERY_FADE_MS, easing = FastOutSlowInEasing)
                )
            }
            launch {
                wordmarkScaleAnim.animateTo(
                    targetValue = WORDMARK_START_SCALE,
                    animationSpec = tween(RECOVERY_FADE_MS, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    fun onActivityPaused() {
        isActivityPaused = true
        activeJob?.cancel()
        activeJob = null
        watchdogJob?.cancel()
        watchdogJob = null
        recoveryJob?.cancel()
        recoveryJob = null
        lockInput(false)
    }

    fun onActivityResumed(scope: CoroutineScope) {
        isActivityPaused = false
        val needsReset = isRunning ||
            contentFadeAnim.value > 0.001f ||
            scrimAlphaAnim.value > 0.001f ||
            wordmarkAlphaAnim.value > 0.001f ||
            shapesElapsedMsAnim.value > 0.001f
        if (needsReset) {
            reverseToIdle(scope)
        } else {
            lockInput(false)
            BgmManager.restoreFromLaunch(0L)
        }
    }

    fun onDispose() {
        activeJob?.cancel()
        watchdogJob?.cancel()
        recoveryJob?.cancel()
        lockInput(false)
        BgmManager.restoreFromLaunch(0L)
    }
}

/**
 * Full-screen overlay rendering the darkening theme scrim, centered AZenith wordmark,
 * and pointer-event blocker during a game-launch transition.
 * Reads [Animatable] values strictly inside `graphicsLayer` lambdas (zero per-frame recomposition).
 */
@Composable
internal fun GameLaunchTransitionOverlay(
    controller: GameLaunchController,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // Theme-only color with guaranteed contrast over MaterialTheme.colorScheme.scrim
    // in both Light and Dark/AMOLED schemes.
    val wordmarkColor = if (scheme.surface.luminance() > 0.5f) {
        scheme.surface
    } else {
        scheme.onSurface
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Darkening scrim over the wallpaper layer.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = controller.scrimAlphaAnim.value }
                .background(scheme.scrim)
        )

        // Centered AZenith wordmark matching HomeWordmark / TopBar typography.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp
                ),
                color = wordmarkColor,
                maxLines = 1,
                modifier = Modifier.graphicsLayer {
                    val s = controller.wordmarkScaleAnim.value
                    alpha = controller.wordmarkAlphaAnim.value
                    scaleX = s
                    scaleY = s
                }
            )
        }

        // Swallow all pointer/touch events while the transition is active.
        if (controller.isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }
    }
}
