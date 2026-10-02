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

package zx.azenith.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Tracks where a trigger control sits on screen so [AZenithDialog] can expand out of it.
 *
 * Attach [modifier] to the control and hand [origin] to the dialog. Until the control has been
 * laid out the origin stays centred, which is the same resting place as before.
 */
@Stable
class DialogOriginState internal constructor() {
    var origin by mutableStateOf(Offset(0.5f, 0.5f))
        internal set

    @Composable
    fun trackedModifier(): Modifier {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val widthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val heightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        return Modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            origin = Offset(bounds.center.x / widthPx, bounds.center.y / heightPx)
        }
    }
}

@Composable
fun rememberDialogOrigin(): DialogOriginState = remember { DialogOriginState() }