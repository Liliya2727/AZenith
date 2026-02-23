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

fun getSupportedRefreshRates(context: Context): List<String> {
    val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
    }


    val supportedRR = display?.supportedModes?.map { it.refreshRate.toInt() }?.distinct()?.sorted() ?: listOf(60)
    
    val finalModes = mutableListOf("Default")

    val standardModes = listOf(60, 90, 120, 144)
    
    standardModes.forEach { mode ->
        if (supportedRR.any { it >= mode - 1 && it <= mode + 1 }) { // Toleransi +/- 1Hz
            finalModes.add(mode.toString())
        }
    }
    
    return finalModes
}

fun getSupportedRefreshRatesPicker(context: Context): List<String> {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }

    val supportedRR = display?.supportedModes
        ?.map { it.refreshRate.toInt() }
        ?.distinct()
        ?.sortedDescending() ?: listOf(60)
    
    val finalModes = mutableListOf<String>()

    val standardModes = listOf(144, 120, 90, 60)
    
    standardModes.forEach { mode ->
        if (supportedRR.any { it in (mode - 1)..(mode + 1) }) {
            finalModes.add(mode.toString())
        }
    }
    
    return finalModes 
}
