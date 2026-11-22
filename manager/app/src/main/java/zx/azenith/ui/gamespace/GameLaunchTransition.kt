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
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.topjohnwu.superuser.Shell
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.component.GamepadDirectionRepeat
import zx.azenith.ui.component.GamepadStickScroll
import zx.azenith.ui.util.AudioUtil
import zx.azenith.ui.util.BgmManager

private const val TAG = "GameLaunch"

// ============================================================================
// Config keys (SharedPreferences "settings"). Config only, no UI.
// ============================================================================
const val PREF_GAME_LAUNCH_TRANSITION = "gamespace_launch_transition" // default on
const val PREF_GAME_LAUNCH_HIDE_STATUSBAR = "gamespace_launch_hide_statusbar" // default on
private const val SETTINGS_PREFS = "settings"
private const val WALLPAPER_ENABLED_PREF = "gamespace_wallpaper_enabled"
private const val LEGACY_WALLPAPER_ENABLED_PREF = "enable_banner_image"

fun isGameLaunchTransitionEnabled(context: Context): Boolean =
    context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_GAME_LAUNCH_TRANSITION, true)

fun isGameLaunchHideStatusBarEnabled(context: Context): Boolean =
    context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_GAME_LAUNCH_HIDE_STATUSBAR, true)

// ============================================================================
// Tuning constants (timings in ms, magnitudes as floats). Everything lives here.
//
// Timeline (defaults):
//   0    UI out (300, scale 1.00 -> 1.04 + fade) & shapes fly out (300 + 0..80 stagger)
//   300  wallpaper blur (400, 0 -> 24 dp) after UI elements are gone
//   700  scrim darkens (260) + wordmark in (220, 70% size) + SOUND
//   920  hold (600), wordmark scale 1.00 -> 1.04 linear
//   1520 wordmark out (220)
//   1740 empty dark screen for PRE_LAUNCH_DELAY_MS (1000)
//   2740 status bar hidden + game launched; restore POST_LAUNCH_RESTORE_DELAY_MS later
// ============================================================================
internal const val UI_OUT_MS = 300
internal const val UI_OUT_END_SCALE = 1.04f

internal const val SHAPES_FLYOUT_DURATION_MS = 300
internal const val SHAPES_MAX_STAGGER_MS = 80
internal const val SHAPES_END_SCALE = 1.22f
internal val SHAPES_FLYOUT_DISTANCE_DP = 420.dp

internal const val BLUR_START_MS = UI_OUT_MS
internal const val BLUR_MS = 400
internal const val LAUNCH_BLUR_DP = 24f

internal const val SCRIM_START_MS = BLUR_START_MS + BLUR_MS
internal const val SCRIM_MS = 260
internal const val SCRIM_MAX_ALPHA = 0.95f

internal const val WORDMARK_START_MS = SCRIM_START_MS
internal const val WORDMARK_IN_MS = 220
internal const val WORDMARK_IN_START_SCALE = 0.92f
internal const val WORDMARK_FONT_SCALE = 0.70f
internal const val WORDMARK_LETTER_SPACING_EM = 0.4f
internal const val HOLD_MS = 600
internal const val HOLD_END_SCALE = 1.04f
internal const val WORDMARK_OUT_MS = 220
internal const val WORDMARK_OUT_END_SCALE = 1.12f

internal const val PRE_LAUNCH_DELAY_MS = 1000L
internal const val POST_LAUNCH_RESTORE_DELAY_MS = 1000L

internal const val BGM_DUCK_TARGET = 0.12f
internal const val BGM_DUCK_FADE_MS = 220L

internal const val WATCHDOG_AFTER_LAUNCH_MS = 4000L
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

enum class GameLaunchPhase { Animating, PreLaunch, Launching }

/**
 * State of the GameSpace game-launch transition, hosted at the Hub level.
 */
sealed interface GameLaunchState {
    data object Idle : GameLaunchState
    data class Running(
        val packageName: String,
        val phase: GameLaunchPhase = GameLaunchPhase.Animating
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
 * Status bar hide/restore bookkeeping. The restore never depends on an activity or a coroutine
 * scope: it is dispatched as a detached root process together with the launch, and a pending
 * marker file (created BEFORE the hide, removed by the detached restore) lets the next GameSpace
 * start repair the state if that process was lost.
 */
object StatusBarGuard {
    private const val FLAGS_HIDE = "clock system-icons notification-icons"
    private const val MARKER_NAME = "statusbar_hide_pending"

    private fun marker(context: Context) = File(context.applicationContext.filesDir, MARKER_NAME)

    fun markPending(context: Context) {
        runCatching { marker(context).writeText("1") }
    }

    fun isPending(context: Context): Boolean = marker(context).exists()

    fun hideCommand(): String = "cmd statusbar send-disable-flag $FLAGS_HIDE"

    /** Detached (new session, nohup) restore that outlives the app process. */
    fun detachedRestoreCommand(context: Context, delayMs: Long): String {
        val sec = String.format(Locale.US, "%.3f", delayMs / 1000.0)
        val path = marker(context).absolutePath
        return "setsid nohup sh -c 'sleep $sec; cmd statusbar send-disable-flag none; rm -f $path' " +
            ">/dev/null 2>&1 &"
    }

    /** Blocking restore: call off the main thread. */
    fun restoreNow(context: Context) {
        runCatching {
            Shell.cmd("cmd statusbar send-disable-flag none").exec()
            marker(context).delete()
        }.onFailure { Log.e(TAG, "Status bar restore failed", it) }
    }

    /** Non-blocking safety net for GameSpace onCreate/onStart/onResume. */
    fun restoreIfPending(context: Context) {
        if (!isPending(context)) return
        val path = marker(context).absolutePath
        Log.w(TAG, "Pending status bar marker found; restoring")
        Shell.cmd("cmd statusbar send-disable-flag none", "rm -f $path").submit()
    }
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

    private fun resolveLaunchComponent(context: Context, packageName: String): String? {
        val fromPm = runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)?.component?.flattenToShortString()
        }.getOrNull()
        if (!fromPm.isNullOrBlank()) return fromPm

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

    /** Root path: hide status bar + am start + detached restore in ONE shell invocation. */
    private fun launchViaRoot(appContext: Context, packageName: String): GameLaunchResult? {
        val component = resolveLaunchComponent(appContext, packageName)
        if (component == null) {
            val msg = "Unable to resolve launcher component for $packageName"
            Log.e(TAG, msg)
            return GameLaunchResult.Failure(msg)
        }
        val hide = isGameLaunchHideStatusBarEnabled(appContext)
        val userId = currentUserId()
        // -f MUST precede --activity-no-animation: -f replaces the intent flags, so the other
        // order loses FLAG_ACTIVITY_NO_ANIMATION (verified: flg=0x10210000 only with this order).
        val amCmd = "am start --user $userId " +
            "-a android.intent.action.MAIN " +
            "-c android.intent.category.LAUNCHER " +
            "-f 0x10200000 " +
            "--activity-no-animation " +
            "-n ${shellSingleQuote(component)}"

        val script = buildString {
            if (hide) appendLine(StatusBarGuard.hideCommand())
            appendLine(amCmd)
            appendLine("RC=\$?")
            if (hide) appendLine(StatusBarGuard.detachedRestoreCommand(appContext, POST_LAUNCH_RESTORE_DELAY_MS))
            append("(exit \$RC)")
        }
        // The marker is written BEFORE the hide so a lost restore can always be repaired.
        if (hide) StatusBarGuard.markPending(appContext)

        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        val execResult = runCatching { Shell.cmd(script).to(stdout, stderr).exec() }
            .getOrElse { t ->
                Log.e(TAG, "Root shell execution threw for $packageName", t)
                null
            }

        if (execResult == null) {
            if (hide) StatusBarGuard.restoreNow(appContext)
            return null // shell lost: caller tries the in-app fallback
        }

        val combined = (stdout + stderr).joinToString("\n")
        if (execResult.code != 0 || hasLaunchErrorText(combined)) {
            Log.e(TAG, "am start failed pkg=$packageName component=$component rc=${execResult.code} out=$combined")
            if (hide) StatusBarGuard.restoreNow(appContext)
            return if (hasLaunchErrorText(combined)) {
                GameLaunchResult.Failure("am start error (rc=${execResult.code}): $combined")
            } else {
                null // plain non-zero rc: try the in-app fallback
            }
        }
        Log.d(TAG, "am start succeeded pkg=$packageName component=$component hide=$hide out=$combined")
        return GameLaunchResult.Success
    }

    /**
     * Runs the launch off the main thread. NonCancellable: once dispatched, the launch must
     * finish its bookkeeping even if the activity pauses (which cancels the transition job).
     */
    suspend fun launch(
        context: Context,
        packageName: String,
        label: String? = null
    ): GameLaunchResult = withContext(NonCancellable) {
        val appContext = context.applicationContext
        val result = withContext(Dispatchers.IO) {
            if (isRootShellAvailable()) {
                launchViaRoot(appContext, packageName)
                    ?: withContext(Dispatchers.Main) { launchViaInAppFallback(context, packageName) }
            } else {
                Log.w(TAG, "Root shell unavailable; in-app no-animation fallback for $packageName")
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
        result
    }

    /** Fire-and-forget convenience wrapper for legacy callers. */
    fun launchAsync(
        scope: CoroutineScope,
        context: Context,
        packageName: String,
        label: String? = null
    ) {
        scope.launch {
            val res = launch(context, packageName, label)
            if (res is GameLaunchResult.Failure) {
                Toast.makeText(context, context.getString(R.string.toast_app_launch_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Coordinates the launchInProgress guard, transition animations, input lock and watchdog at the
 * Hub level. All animated values are [Animatable]s read strictly inside graphicsLayer lambdas,
 * so there is no per-frame recomposition (including the blur radius).
 */
@Stable
class GameLaunchController {
    var state: GameLaunchState by mutableStateOf(GameLaunchState.Idle)
        private set

    val isRunning: Boolean
        get() = state is GameLaunchState.Running

    val contentFadeAnim = Animatable(0f) // 0 = normal, 1 = Hub layer gone
    val shapesElapsedMsAnim = Animatable(0f)
    val blurDpAnim = Animatable(0f)
    val scrimAlphaAnim = Animatable(0f)
    val wordmarkAlphaAnim = Animatable(0f)
    val wordmarkScaleAnim = Animatable(WORDMARK_IN_START_SCALE)

    private var activeJob: Job? = null
    private var watchdogJob: Job? = null
    private var recoveryJob: Job? = null

    @Volatile
    var isActivityPaused: Boolean = false

    private fun animatorDurationScale(context: Context): Float = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)

    private fun wallpaperEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(WALLPAPER_ENABLED_PREF, prefs.getBoolean(LEGACY_WALLPAPER_ENABLED_PREF, true))
    }

    private fun lockInput(locked: Boolean) {
        GameLaunchInputLock.isLocked = locked
        if (locked) {
            GamepadDirectionRepeat.release()
            GamepadStickScroll.reset()
        }
    }

    private suspend fun resetChannels() {
        contentFadeAnim.snapTo(0f)
        shapesElapsedMsAnim.snapTo(0f)
        blurDpAnim.snapTo(0f)
        scrimAlphaAnim.snapTo(0f)
        wordmarkAlphaAnim.snapTo(0f)
        wordmarkScaleAnim.snapTo(WORDMARK_IN_START_SCALE)
    }

    private fun handleFailure(scope: CoroutineScope, context: Context, packageName: String, res: GameLaunchResult.Failure) {
        Log.e(TAG, "Game launch failed for $packageName: ${res.reason}", res.throwable)
        Toast.makeText(context, context.getString(R.string.toast_app_launch_fail), Toast.LENGTH_SHORT).show()
        reverseToIdle(scope)
    }

    private suspend fun launchSafely(context: Context, packageName: String, label: String?): GameLaunchResult =
        try {
            GameLauncher.launch(context, packageName, label)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected launch error for $packageName", t)
            withContext(Dispatchers.IO + NonCancellable) { StatusBarGuard.restoreNow(context) }
            GameLaunchResult.Failure("Unexpected error: ${t.message}", t)
        }

    private fun startWatchdog(scope: CoroutineScope, context: Context, packageName: String) {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            delay(WATCHDOG_AFTER_LAUNCH_MS)
            if (isRunning && !isActivityPaused) {
                Log.w(TAG, "Launch watchdog fired for $packageName ${WATCHDOG_AFTER_LAUNCH_MS}ms after launch; recovering")
                withContext(Dispatchers.IO + NonCancellable) { StatusBarGuard.restoreNow(context) }
                reverseToIdle(scope)
            }
        }
    }

    fun requestLaunch(
        scope: CoroutineScope,
        context: Context,
        packageName: String,
        label: String? = null
    ) {
        if (isRunning) return

        val skipVisuals = !isGameLaunchTransitionEnabled(context) || animatorDurationScale(context) <= 0f
        lockInput(true)
        isActivityPaused = false
        activeJob?.cancel()
        watchdogJob?.cancel()
        recoveryJob?.cancel()
        recoveryJob = null

        if (skipVisuals) {
            // No visuals, no pauses. The status bar hide/restore still applies inside launch().
            state = GameLaunchState.Running(packageName, GameLaunchPhase.Launching)
            activeJob = scope.launch {
                try {
                    val res = launchSafely(context, packageName, label)
                    if (res is GameLaunchResult.Failure) {
                        Log.e(TAG, "Game launch failed for $packageName: ${res.reason}", res.throwable)
                        Toast.makeText(context, context.getString(R.string.toast_app_launch_fail), Toast.LENGTH_SHORT).show()
                        lockInput(false)
                        state = GameLaunchState.Idle
                    } else {
                        // Guard stays until the activity pauses (game took over) or the watchdog fires.
                        startWatchdog(scope, context, packageName)
                    }
                } catch (e: CancellationException) {
                    throw e
                }
            }
            return
        }

        state = GameLaunchState.Running(packageName, GameLaunchPhase.Animating)
        // t = 0: duck BGM so the launch sound is audible.
        BgmManager.duckForLaunch(BGM_DUCK_TARGET, BGM_DUCK_FADE_MS)

        val blurTarget = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaperEnabled(context)) {
            LAUNCH_BLUR_DP
        } else {
            0f
        }
        val shapesTotalMs = SHAPES_FLYOUT_DURATION_MS + SHAPES_MAX_STAGGER_MS

        activeJob = scope.launch {
            try {
                resetChannels()
                coroutineScope {
                    // 2. UI out + shapes fly out.
                    launch {
                        contentFadeAnim.animateTo(1f, tween(UI_OUT_MS, easing = FastOutLinearInEasing))
                    }
                    launch {
                        shapesElapsedMsAnim.animateTo(shapesTotalMs.toFloat(), tween(shapesTotalMs, easing = LinearEasing))
                    }
                    // 3. Wallpaper blur ramp.
                    if (blurTarget > 0f) launch {
                        if (BLUR_START_MS > 0) delay(BLUR_START_MS.toLong())
                        blurDpAnim.animateTo(blurTarget, tween(BLUR_MS, easing = FastOutSlowInEasing))
                    }
                    // 4. Scrim.
                    launch {
                        delay(SCRIM_START_MS.toLong())
                        scrimAlphaAnim.animateTo(SCRIM_MAX_ALPHA, tween(SCRIM_MS, easing = FastOutSlowInEasing))
                    }
                    // 5-7. Wordmark: in (+ sound), hold, out.
                    delay(WORDMARK_START_MS.toLong())
                    AudioUtil.playGameLaunch() // exactly when the wordmark starts to appear
                    coroutineScope {
                        launch { wordmarkAlphaAnim.animateTo(1f, tween(WORDMARK_IN_MS, easing = LinearOutSlowInEasing)) }
                        launch { wordmarkScaleAnim.animateTo(1f, tween(WORDMARK_IN_MS, easing = LinearOutSlowInEasing)) }
                    }
                    wordmarkScaleAnim.animateTo(HOLD_END_SCALE, tween(HOLD_MS, easing = LinearEasing))
                    coroutineScope {
                        launch { wordmarkAlphaAnim.animateTo(0f, tween(WORDMARK_OUT_MS, easing = FastOutLinearInEasing)) }
                        launch { wordmarkScaleAnim.animateTo(WORDMARK_OUT_END_SCALE, tween(WORDMARK_OUT_MS, easing = FastOutLinearInEasing)) }
                    }
                }

                // 8. Dark, empty screen.
                state = GameLaunchState.Running(packageName, GameLaunchPhase.PreLaunch)
                delay(PRE_LAUNCH_DELAY_MS)

                // 9. Hide status bar + launch (one root invocation inside GameLauncher).
                state = GameLaunchState.Running(packageName, GameLaunchPhase.Launching)
                val res = launchSafely(context, packageName, label)
                if (res is GameLaunchResult.Failure) {
                    handleFailure(scope, context, packageName, res)
                } else {
                    startWatchdog(scope, context, packageName)
                }
            } catch (_: CancellationException) {
                // Lifecycle pause/reset cancelled the transition.
            }
        }
    }

    /** Fades everything back, unlocks input, restores BGM. Status bar is handled by callers. */
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
            val spec = tween<Float>(RECOVERY_FADE_MS, easing = FastOutSlowInEasing)
            launch { contentFadeAnim.animateTo(0f, spec) }
            launch { shapesElapsedMsAnim.animateTo(0f, spec) }
            launch { blurDpAnim.animateTo(0f, spec) }
            launch { scrimAlphaAnim.animateTo(0f, spec) }
            launch { wordmarkAlphaAnim.animateTo(0f, spec) }
            launch { wordmarkScaleAnim.animateTo(WORDMARK_IN_START_SCALE, spec) }
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
            blurDpAnim.value > 0.001f ||
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
 * Full-screen overlay: theme scrim, centered "AZENITH" wordmark and the pointer blocker.
 * Values are read inside graphicsLayer lambdas only.
 */
@Composable
internal fun GameLaunchTransitionOverlay(
    controller: GameLaunchController,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val wordmarkColor = if (scheme.surface.luminance() > 0.5f) scheme.surface else scheme.onSurface
    val appName = stringResource(R.string.app_name)

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = controller.scrimAlphaAnim.value }
                .background(scheme.scrim)
        )

        val wordmarkStyle = MaterialTheme.typography.displayLarge
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = appName.uppercase(),
                style = wordmarkStyle.copy(
                    fontSize = wordmarkStyle.fontSize * WORDMARK_FONT_SCALE,
                    lineHeight = wordmarkStyle.lineHeight * WORDMARK_FONT_SCALE,
                    fontWeight = FontWeight.Black,
                    letterSpacing = WORDMARK_LETTER_SPACING_EM.em
                ),
                color = wordmarkColor,
                maxLines = 1,
                modifier = Modifier
                    .semantics { contentDescription = appName }
                    .graphicsLayer {
                        val s = controller.wordmarkScaleAnim.value
                        alpha = controller.wordmarkAlphaAnim.value
                        scaleX = s
                        scaleY = s
                    }
            )
        }

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
