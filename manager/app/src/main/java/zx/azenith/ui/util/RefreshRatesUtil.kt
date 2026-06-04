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

package zx.azenith.ui.util

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager

data class DisplayModeInfo(
    val modeId: Int,
    val refreshRate: String
)

fun getSupportedRefreshRatesPicker(context: Context): List<DisplayModeInfo> {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }

    val supportedModes = display?.supportedModes ?: return emptyList()
    val finalModes = mutableListOf<DisplayModeInfo>()
    val standardModes = listOf(144, 120, 90, 60)
    
    standardModes.forEach { targetRate ->
        // Cari Mode ID asli dari hardware yang cocok dengan target rate
        val mode = supportedModes.firstOrNull { it.refreshRate.toInt() in (targetRate - 1)..(targetRate + 1) }
        if (mode != null) {
            finalModes.add(DisplayModeInfo(mode.modeId, targetRate.toString()))
        }
    }
    
    return finalModes 
}
