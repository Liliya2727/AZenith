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

package zx.azenith.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** A row search asked for, with the time it was asked so it can go stale. */
data class SearchJumpRequest(val entryId: String, val atUptimeMs: Long)

/**
 * The row a tapped search result should land on.
 *
 * Search runs in its own route, so there is no composition-time hand-off: the
 * request is left here and whichever screen owns the row picks it up. A plain
 * property rather than a nav argument, because the screen that takes the jump is
 * often not the one being navigated to.
 *
 * Keyed by entry id, which is unique across the whole index, so a screen only
 * has to name the ids it owns.
 *
 * Only the owning screen consumes a request. Non-owners must leave it alone:
 * the pager keeps every tab composed at once, so a screen that clears a request
 * it does not own destroys it before the real owner ever sees it. The age stamp
 * keeps an unclaimed request from firing later -- it expires instead of blinking
 * on whatever screen the user wandered to next.
 */
object SearchJump {
    private val state = mutableStateOf<SearchJumpRequest?>(null)

    /** The pending request, or null if none is outstanding. */
    val pending: SearchJumpRequest? get() = state.value

    fun request(entryId: String) {
        state.value = SearchJumpRequest(entryId, SystemClock.uptimeMillis())
    }

    fun clear() {
        state.value = null
    }
}

/** How long the landing row pulses. */
private const val PULSE_MS = 1400L

/** Settling time after the scroll, before the pulse starts. */
private const val SETTLE_MS = 220L

/** A request older than this belongs to a tap the user has moved on from. */
private const val REQUEST_TTL_MS = 3000L

/**
 * Scrolls to the row search asked for and returns the entry id to pulse.
 *
 * [owns] answers whether this screen is the one the entry id belongs to, and
 * [indexOf] maps it to the LazyColumn item holding that row. An id this screen
 * does not own is ignored rather than cleared -- the pager keeps several screens
 * composed at once, so clearing here would take the request away from its owner
 * before that owner ever reads it.
 *
 * The scroll is left to settle before the pulse starts, or the highlight rides
 * up the screen with the list instead of landing on the row.
 */
@Composable
fun LazyListState.searchJumpTarget(
    owns: (String) -> Boolean,
    indexOf: (String) -> Int?
): String? {
    val pending = SearchJump.pending
    val pulsing = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pending) {
        val req = pending ?: return@LaunchedEffect
        if (!owns(req.entryId)) return@LaunchedEffect
        if (SystemClock.uptimeMillis() - req.atUptimeMs > REQUEST_TTL_MS) {
            SearchJump.clear()
            return@LaunchedEffect
        }

        val index = indexOf(req.entryId)
        if (index != null) {
            animateScrollToItem(index)
            delay(SETTLE_MS)
        }
        pulsing.value = req.entryId
        delay(PULSE_MS)
        pulsing.value = null
        SearchJump.clear()
    }

    return pulsing.value
}

/**
 * Wraps one searchable row so it blinks while it is the row search landed on.
 *
 * Opt-in per row rather than baked into the row components: the pulse is an
 * overlay, and rows paint their own surface over the list item, so a background
 * underneath would never be seen.
 *
 * [highlightShape] clips the overlay. Section headings sit outside the joined
 * card containers that clip their own rows, so they should pass a rounded shape
 * to keep the highlight from reading as a hard box; rows inside a container can
 * leave it as the default square and the container's clip does the work.
 */
@Composable
fun JumpRow(
    activeId: String?,
    entryId: String,
    modifier: Modifier = Modifier,
    highlightShape: Shape? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (activeId != null && activeId == entryId) {
            val transition = rememberInfiniteTransition(label = "SearchJumpPulse")
            val alpha by transition.animateFloat(
                initialValue = 0.38f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "SearchJumpAlpha"
            )
            if (alpha > 0.005f) {
                val shape = highlightShape ?: RoundedCornerShape(0.dp)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                )
            }
        }
    }
}
