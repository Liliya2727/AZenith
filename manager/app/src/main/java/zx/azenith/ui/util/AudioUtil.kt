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
import android.media.SoundPool
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

    private var soundPool: SoundPool? = null
    private var scrollSoundId = 0
    private var okSoundId = 0
    private var cancelSoundId = 0
    private var noticeSoundId = 0
    private var noticeBackSoundId = 0

    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val pendingPlays = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        SoundEffectsState.enabled.value = isSoundEffectsEnabled(appContext)
        SoundEffectsState.volume.intValue = getSoundEffectsVolume(appContext)
        SoundEffectsState.tapEnabled.value = isTapSoundEnabled(appContext)
        SoundEffectsState.scrollEnabled.value = isScrollSoundEnabled(appContext)
        SoundEffectsState.cancelEnabled.value = isCancelSoundEnabled(appContext)
        SoundEffectsState.noticeEnabled.value = isNoticeSoundEnabled(appContext)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
                    p.play(sampleId, vol, vol, 1, 0, 1f)
                }
            }
        }

        scrollSoundId = pool.load(appContext, R.raw.scroll, 1)
        okSoundId = pool.load(appContext, R.raw.ok, 1)
        cancelSoundId = pool.load(appContext, R.raw.cancel, 1)
        noticeSoundId = pool.load(appContext, R.raw.notice, 1)
        noticeBackSoundId = pool.load(appContext, R.raw.notice_back, 1)

        soundPool = pool
        isInitialized = true
    }

    private fun play(context: Context, soundId: Int) {
        if (!isGameHubActive) return
        if (!SoundEffectsState.enabled.value) return
        if (!isInitialized) {
            init(context)
        }
        val pool = soundPool ?: return
        val vol = (SoundEffectsState.volume.intValue / 100f).coerceIn(0f, 1f)
        if (loadedSoundIds.contains(soundId)) {
            pool.play(soundId, vol, vol, 1, 0, 1f)
        } else {
            pendingPlays.add(soundId)
        }
    }

    fun playScroll(context: Context) {
        if (!SoundEffectsState.scrollEnabled.value) return
        play(context, scrollSoundId)
    }

    fun playOk(context: Context) {
        TapHapticState.suppressSound = true
        if (!SoundEffectsState.tapEnabled.value) return
        play(context, okSoundId)
    }

    fun playCancel(context: Context) {
        TapHapticState.suppressSound = true
        if (!SoundEffectsState.cancelEnabled.value) return
        play(context, cancelSoundId)
    }

    fun playNotice(context: Context) {
        TapHapticState.suppressSound = true
        if (!SoundEffectsState.noticeEnabled.value) return
        play(context, noticeSoundId)
    }

    fun playNoticeBack(context: Context) {
        TapHapticState.suppressSound = true
        if (!SoundEffectsState.noticeEnabled.value) return
        play(context, noticeBackSoundId)
    }
}
