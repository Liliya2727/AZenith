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
import android.hardware.display.DisplayManager
import android.view.Display
import kotlin.math.roundToInt

/**
 * Mengambil list refresh rate langsung dari DisplayManager Android native.
 * Secara otomatis membulatkan angka desimal (misal 119.9f jadi 120) 
 * dan menghapus nilai yang duplikat.
 */
private fun getSystemRefreshRates(context: Context): List<Int> {
    return try {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return emptyList()

        display.supportedModes
            .map { it.refreshRate.roundToInt() }
            .distinct()
            .sorted() 
    } catch (e: Exception) {
        emptyList()
    }
}

fun getSupportedRefreshRates(context: Context): List<String> {
    val systemRR = getSystemRefreshRates(context)

    val finalModes = mutableListOf("default")
    
    systemRR.forEach { mode ->
        finalModes.add(mode.toString())
    }

    if (finalModes.size == 1) {
        finalModes.addAll(listOf("60", "90", "120"))
    }

    return finalModes
}

fun getSupportedRefreshRatesPicker(context: Context): List<String> {
    val systemRR = getSystemRefreshRates(context).sortedDescending()
    
    val finalModes = systemRR.map { it.toString() }.toMutableList()

    if (finalModes.isEmpty()) {
        finalModes.addAll(listOf("120", "90", "60"))
    }

    return finalModes
}
