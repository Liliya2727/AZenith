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
    private var soundPool: SoundPool? = null
    private var scrollSoundId = 0
    private var okSoundId = 0
    private var cancelSoundId = 0
    private var noticeSoundId = 0
    private var noticeBackSoundId = 0
    private var isInitialized = false

    @Synchronized
    private fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        SoundEffectsState.enabled.value = isSoundEffectsEnabled(appContext)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        scrollSoundId = pool.load(appContext, R.raw.scroll, 1)
        okSoundId = pool.load(appContext, R.raw.ok, 1)
        cancelSoundId = pool.load(appContext, R.raw.cancel, 1)
        noticeSoundId = pool.load(appContext, R.raw.notice, 1)
        noticeBackSoundId = pool.load(appContext, R.raw.notice_back, 1)

        soundPool = pool
        isInitialized = true
    }

    private fun play(context: Context, soundId: Int) {
        if (!SoundEffectsState.enabled.value) return
        if (!isInitialized) {
            init(context)
        }
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun playScroll(context: Context) {
        if (!isInitialized) init(context)
        play(context, scrollSoundId)
    }

    fun playOk(context: Context) {
        if (!isInitialized) init(context)
        play(context, okSoundId)
    }

    fun playCancel(context: Context) {
        if (!isInitialized) init(context)
        play(context, cancelSoundId)
    }

    fun playNotice(context: Context) {
        if (!isInitialized) init(context)
        play(context, noticeSoundId)
    }

    fun playNoticeBack(context: Context) {
        if (!isInitialized) init(context)
        play(context, noticeBackSoundId)
    }
}
