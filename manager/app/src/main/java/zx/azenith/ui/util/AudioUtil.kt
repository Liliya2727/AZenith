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
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.ConcurrentHashMap
import zx.azenith.R

const val PREF_SOUND_EFFECTS = "sound_effects_enabled"
private const val PREFS = "settings"

fun isSoundEffectsEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(PREF_SOUND_EFFECTS, true)

fun setSoundEffectsEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putBoolean(PREF_SOUND_EFFECTS, enabled).apply()
    SoundEffectsState.enabled.value = enabled
}

object SoundEffectsState {
    val enabled = mutableStateOf(true)
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
                    p.play(sampleId, 1f, 1f, 1, 0, 1f)
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
        if (loadedSoundIds.contains(soundId)) {
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            pendingPlays.add(soundId)
        }
    }

    fun playScroll(context: Context) {
        play(context, scrollSoundId)
    }

    fun playOk(context: Context) {
        TapHapticState.suppressSound = true
        play(context, okSoundId)
    }

    fun playCancel(context: Context) {
        TapHapticState.suppressSound = true
        play(context, cancelSoundId)
    }

    fun playNotice(context: Context) {
        TapHapticState.suppressSound = true
        play(context, noticeSoundId)
    }

    fun playNoticeBack(context: Context) {
        TapHapticState.suppressSound = true
        play(context, noticeBackSoundId)
    }
}
