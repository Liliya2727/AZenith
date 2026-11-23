/*
 * Copyright (C) 2026-2027 Zexshia
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

package zx.azenith.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.ConcurrentHashMap
import zx.azenith.R

const val PREF_SOUND_EFFECTS = "sound_effects_enabled"
const val PREF_SOUND_EFFECTS_VOLUME = "sound_effects_volume"
const val PREF_SOUND_EFFECTS_TAP = "sound_effects_tap_enabled"
const val PREF_SOUND_EFFECTS_SCROLL = "sound_effects_scroll_enabled"
const val PREF_SOUND_EFFECTS_CANCEL = "sound_effects_cancel_enabled"
const val PREF_SOUND_EFFECTS_NOTICE = "sound_effects_notice_enabled"
private const val PREFS = "settings"

fun isSoundEffectsEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_SOUND_EFFECTS, true)

fun setSoundEffectsEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_SOUND_EFFECTS, enabled).apply()
    SoundEffectsState.enabled.value = enabled
}

fun getSoundEffectsVolume(context: Context): Int =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt(PREF_SOUND_EFFECTS_VOLUME, 100)

fun setSoundEffectsVolume(context: Context, volume: Int) {
    val clamped = volume.coerceIn(0, 100)
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putInt(PREF_SOUND_EFFECTS_VOLUME, clamped).apply()
    SoundEffectsState.volume.intValue = clamped
}

fun isTapSoundEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_SOUND_EFFECTS_TAP, true)

fun setTapSoundEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_SOUND_EFFECTS_TAP, enabled).apply()
    SoundEffectsState.tapEnabled.value = enabled
}

fun isScrollSoundEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_SOUND_EFFECTS_SCROLL, true)

fun setScrollSoundEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_SOUND_EFFECTS_SCROLL, enabled).apply()
    SoundEffectsState.scrollEnabled.value = enabled
}

fun isCancelSoundEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_SOUND_EFFECTS_CANCEL, true)

fun setCancelSoundEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_SOUND_EFFECTS_CANCEL, enabled).apply()
    SoundEffectsState.cancelEnabled.value = enabled
}

fun isNoticeSoundEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_SOUND_EFFECTS_NOTICE, true)

fun setNoticeSoundEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_SOUND_EFFECTS_NOTICE, enabled).apply()
    SoundEffectsState.noticeEnabled.value = enabled
}

object SoundEffectsState {
    val enabled = mutableStateOf(true)
    val volume = mutableIntStateOf(100)
    val tapEnabled = mutableStateOf(true)
    val scrollEnabled = mutableStateOf(true)
    val cancelEnabled = mutableStateOf(true)
    val noticeEnabled = mutableStateOf(true)
}

object AudioUtil {
    @Volatile
    var isGameHubActive: Boolean = false

    private const val SCROLL_COOLDOWN_MS = 40L
    private const val FOCUS_COOLDOWN_MS = 45L
    private const val ACTION_COOLDOWN_MS = 50L
    private const val ACTION_SUPPRESS_FOCUS_MS = 140L
    private const val ROUTE_REBUILD_DEBOUNCE_MS = 300L

    private var appContextRef: Context? = null
    private var audioManagerRef: AudioManager? = null
    private var deviceCallback: AudioDeviceCallback? = null
    private var lastExternalOutputIds: Set<Int> = emptySet()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingRebuild: Runnable? = null

    @Volatile private var soundPool: SoundPool? = null
    @Volatile private var scrollSoundId = 0
    @Volatile private var scroll2SoundId = 0
    @Volatile private var okSoundId = 0
    @Volatile private var cancelSoundId = 0
    @Volatile private var noticeSoundId = 0
    @Volatile private var noticeBackSoundId = 0
    @Volatile private var gameLaunchSoundId = 0

    @Volatile private var lastScrollAtMs = 0L
    @Volatile private var lastFocusAtMs = 0L
    @Volatile private var lastActionAtMs = 0L
    @Volatile private var lastFocusStreamId = 0

    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val pendingPlays = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    private var isInitialized = false

    private fun isExternalOutputDevice(device: AudioDeviceInfo): Boolean {
        if (!device.isSink) return false
        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE -> true
            else -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
    }

    private fun snapshotExternalOutputs(am: AudioManager): Set<Int> =
        runCatching {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter(::isExternalOutputDevice)
                .mapTo(HashSet()) { it.id }
        }.getOrDefault(emptySet())

    private fun buildSoundPoolLocked(appContext: Context) {
        runCatching { soundPool?.release() }
        soundPool = null
        loadedSoundIds.clear()
        pendingPlays.clear()
        lastFocusStreamId = 0

        // Use USAGE_MEDIA + CONTENT_TYPE_MUSIC (matching BgmManager) so AudioPolicyManager
        // routes SFX strictly through STRATEGY_MEDIA (BT A2DP / LE Audio / wired headset)
        // instead of sonification dual-routing that can leak to the phone speaker or drop on BT.
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()

        pool.setOnLoadCompleteListener { p, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
                if (pendingPlays.remove(sampleId) && SoundEffectsState.enabled.value && isGameHubActive) {
                    val vol = (SoundEffectsState.volume.intValue / 100f).coerceIn(0f, 1f)
                    if (vol > 0f) {
                        p.play(sampleId, vol, vol, 1, 0, 1f)
                    }
                }
            }
        }

        scrollSoundId = pool.load(appContext, R.raw.scroll, 1)
        scroll2SoundId = pool.load(appContext, R.raw.scroll2, 1)
        okSoundId = pool.load(appContext, R.raw.ok, 1)
        cancelSoundId = pool.load(appContext, R.raw.cancel, 1)
        noticeSoundId = pool.load(appContext, R.raw.notice, 1)
        noticeBackSoundId = pool.load(appContext, R.raw.notice_back, 1)
        gameLaunchSoundId = pool.load(appContext, R.raw.gamelaunch, 1)

        soundPool = pool
    }

    private fun scheduleRouteRebuild() {
        val appContext = appContextRef ?: return
        pendingRebuild?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            synchronized(this@AudioUtil) {
                if (!isInitialized) return@synchronized
                buildSoundPoolLocked(appContext)
            }
        }
        pendingRebuild = runnable
        mainHandler.postDelayed(runnable, ROUTE_REBUILD_DEBOUNCE_MS)
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        appContextRef = appContext
        SoundEffectsState.enabled.value = isSoundEffectsEnabled(appContext)
        SoundEffectsState.volume.intValue = getSoundEffectsVolume(appContext)
        SoundEffectsState.tapEnabled.value = isTapSoundEnabled(appContext)
        SoundEffectsState.scrollEnabled.value = isScrollSoundEnabled(appContext)
        SoundEffectsState.cancelEnabled.value = isCancelSoundEnabled(appContext)
        SoundEffectsState.noticeEnabled.value = isNoticeSoundEnabled(appContext)

        buildSoundPoolLocked(appContext)

        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            audioManagerRef = am
            lastExternalOutputIds = snapshotExternalOutputs(am)
            val cb = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    handleRouteChange(am)
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    handleRouteChange(am)
                }
            }
            deviceCallback = cb
            runCatching { am.registerAudioDeviceCallback(cb, mainHandler) }
        }

        isInitialized = true
    }

    private fun handleRouteChange(am: AudioManager) {
        val current = snapshotExternalOutputs(am)
        if (current != lastExternalOutputIds) {
            lastExternalOutputIds = current
            scheduleRouteRebuild()
        }
    }

    @Synchronized
    fun onActivityPause() {
        pendingRebuild?.let { mainHandler.removeCallbacks(it) }
        pendingRebuild = null
        runCatching { soundPool?.autoPause() }
    }

    @Synchronized
    fun onActivityResume(context: Context) {
        if (!isInitialized) return
        val am = audioManagerRef ?: (context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
        if (am != null) {
            val current = snapshotExternalOutputs(am)
            if (current != lastExternalOutputIds) {
                lastExternalOutputIds = current
                buildSoundPoolLocked(context.applicationContext)
                return
            }
        }
        runCatching { soundPool?.autoResume() }
    }

    @Synchronized
    fun release() {
        pendingRebuild?.let { mainHandler.removeCallbacks(it) }
        pendingRebuild = null
        deviceCallback?.let { cb ->
            runCatching { audioManagerRef?.unregisterAudioDeviceCallback(cb) }
        }
        deviceCallback = null
        audioManagerRef = null
        appContextRef = null
        lastExternalOutputIds = emptySet()
        runCatching { soundPool?.release() }
        soundPool = null
        scrollSoundId = 0
        scroll2SoundId = 0
        okSoundId = 0
        cancelSoundId = 0
        noticeSoundId = 0
        noticeBackSoundId = 0
        gameLaunchSoundId = 0
        lastScrollAtMs = 0L
        lastFocusAtMs = 0L
        lastActionAtMs = 0L
        lastFocusStreamId = 0
        loadedSoundIds.clear()
        pendingPlays.clear()
        isInitialized = false
    }

    private inline fun playResolved(
        context: Context,
        queueIfUnloaded: Boolean = true,
        soundIdProvider: () -> Int
    ): Int {
        if (!isGameHubActive) return 0
        if (!SoundEffectsState.enabled.value) return 0
        if (!isInitialized) {
            init(context)
        }
        val pool = soundPool ?: return 0
        val soundId = soundIdProvider()
        if (soundId == 0) return 0
        val vol = (SoundEffectsState.volume.intValue / 100f).coerceIn(0f, 1f)
        if (vol <= 0f) return 0
        return if (loadedSoundIds.contains(soundId)) {
            runCatching { pool.play(soundId, vol, vol, 1, 0, 1f) }.getOrDefault(0)
        } else {
            if (queueIfUnloaded) pendingPlays.add(soundId)
            0
        }
    }

    fun playScroll(context: Context) {
        if (!SoundEffectsState.scrollEnabled.value) return
        val now = SystemClock.uptimeMillis()
        if (now - lastScrollAtMs < SCROLL_COOLDOWN_MS) return
        lastScrollAtMs = now
        playResolved(context, queueIfUnloaded = false) { scrollSoundId }
    }

    /**
     * Plays the crisp focus-navigation sound (scroll2) for non-ribbon UI focus moves.
     * Deduplicated against rapid repeats, suppressed briefly after action sounds (ok/cancel/notice),
     * and stops any still-ringing previous focus stream to avoid muddy overlap.
     */
    fun playFocusMove() {
        if (!isGameHubActive) return
        if (!SoundEffectsState.enabled.value || !SoundEffectsState.scrollEnabled.value) return
        val now = SystemClock.uptimeMillis()
        if (now - lastActionAtMs < ACTION_SUPPRESS_FOCUS_MS) return
        if (now - lastFocusAtMs < FOCUS_COOLDOWN_MS) return
        val pool = soundPool ?: return
        val id = scroll2SoundId
        if (id == 0 || !loadedSoundIds.contains(id)) return
        val vol = (SoundEffectsState.volume.intValue / 100f).coerceIn(0f, 1f)
        if (vol <= 0f) return
        lastFocusAtMs = now
        val prevStream = lastFocusStreamId
        if (prevStream != 0) {
            runCatching { pool.stop(prevStream) }
        }
        lastFocusStreamId = runCatching { pool.play(id, vol, vol, 1, 0, 1f) }.getOrDefault(0)
    }

    private inline fun playAction(
        context: Context,
        categoryEnabled: Boolean,
        soundIdProvider: () -> Int
    ) {
        TapHapticState.suppressSound = true
        val now = SystemClock.uptimeMillis()
        val prevAction = lastActionAtMs
        lastActionAtMs = now
        if (!categoryEnabled) return
        if (now - prevAction < ACTION_COOLDOWN_MS) return
        playResolved(context, queueIfUnloaded = true, soundIdProvider = soundIdProvider)
    }

    fun playOk(context: Context) = playAction(context, SoundEffectsState.tapEnabled.value) { okSoundId }

    fun playCancel(context: Context) = playAction(context, SoundEffectsState.cancelEnabled.value) { cancelSoundId }

    fun playNotice(context: Context) = playAction(context, SoundEffectsState.noticeEnabled.value) { noticeSoundId }

    fun playNoticeBack(context: Context) = playAction(context, SoundEffectsState.noticeEnabled.value) { noticeBackSoundId }

    /**
     * Plays the preloaded game-launch sound once if SFX are enabled and the sample is already loaded.
     * Never queues a delayed play or blocks launch if the sample has not finished loading.
     */
    fun playGameLaunch() {
        if (!isGameHubActive) return
        if (!SoundEffectsState.enabled.value) return
        lastActionAtMs = SystemClock.uptimeMillis()
        val pool = soundPool ?: return
        val id = gameLaunchSoundId
        if (id == 0 || !loadedSoundIds.contains(id)) return
        val vol = (SoundEffectsState.volume.intValue / 100f).coerceIn(0f, 1f)
        if (vol <= 0f) return
        runCatching { pool.play(id, vol, vol, 1, 0, 1f) }
    }
}
