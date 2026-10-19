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

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The one stepped clock every ambient animation on the Game Space screen reads.
 *
 * Separate tickers for the jacket spin and the floating shapes wake on unrelated frames, so
 * their writes land in different vsyncs and the screen renders ~45 frames a second where ~30
 * would do. A frame of this window costs ~9 ms of CPU, so the phase difference is not free.
 * The ticker only exists while something is animating.
 */
internal object GameSpaceClock {
    const val TICK_MS = 33L

    val seconds = mutableFloatStateOf(0f)

    private var consumers = 0
    private var ticker: Job? = null

    fun retain(scope: CoroutineScope) {
        consumers++
        if (ticker == null) {
            ticker = scope.launch {
                val start = SystemClock.uptimeMillis()
                while (isActive) {
                    seconds.floatValue = (SystemClock.uptimeMillis() - start) / 1000f
                    delay(TICK_MS)
                }
            }
        }
    }

    fun release() {
        if (--consumers <= 0) {
            ticker?.cancel()
            ticker = null
            consumers = 0
        }
    }
}

/** Holds the shared ticker open for as long as the caller is in composition. */
@Composable
internal fun rememberGameSpaceClock(): FloatState {
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        GameSpaceClock.retain(scope)
        onDispose { GameSpaceClock.release() }
    }
    return GameSpaceClock.seconds
}
