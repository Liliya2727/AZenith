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
import com.topjohnwu.superuser.Shell

private const val REFRESH_RATE_MAPPING_PATH = "/data/adb/.config/AZenith/util_mapping.dat"

/**
 * Reads util_mapping.dat and extracts just the hz keys.
 * Expected format per line: "hz=index" e.g. "120=0"
 */
private fun getMappedRefreshRates(): List<Int> {
    return try {
        val result = Shell.cmd("cat $REFRESH_RATE_MAPPING_PATH").exec()
        if (!result.isSuccess) return emptyList()

        result.out
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || !trimmed.contains("=")) return@mapNotNull null
                trimmed.substringBefore("=").trim().toIntOrNull()
            }
            .distinct()
    } catch (e: Exception) {
        emptyList()
    }
}

fun getSupportedRefreshRates(context: Context): List<String> {
    val mappedRR = getMappedRefreshRates().sorted()

    val finalModes = mutableListOf("default")
    
    mappedRR.forEach { mode ->
        finalModes.add(mode.toString())
    }

    if (finalModes.size == 1) {
        finalModes.addAll(listOf("60", "90", "120"))
    }

    return finalModes
}

fun getSupportedRefreshRatesPicker(context: Context): List<String> {
    val mappedRR = getMappedRefreshRates().sortedDescending()
    
    val finalModes = mappedRR.map { it.toString() }.toMutableList()

    if (finalModes.isEmpty()) {
        finalModes.addAll(listOf("120", "90", "60"))
    }

    return finalModes
}
