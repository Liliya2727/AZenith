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
import android.os.Build
import android.view.WindowManager
import java.util.Locale

private val LEGACY_DOWNSCALE_STEPS = listOf(
    "0.3", "0.35", "0.4", "0.45", "0.5", "0.55",
    "0.6", "0.65", "0.7", "0.75", "0.8", "0.85", "0.9",
    "default"
)

private val LEGACY_FPS_STEPS = listOf(30, 40, 45, 60, 90, 120)

/**
 * Generates downscale factor steps.
 * On SDK <= 33, GameManagerService only accepts exact ratios 0.3..0.9 (no 0.95).
 * On SDK > 33, generates 0.30 to 0.95 in 0.05 increments, with "default" last.
 */
fun getSupportedDownscaleFactors(): List<String> {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
        return LEGACY_DOWNSCALE_STEPS
    }
    val factors = mutableListOf<String>()
    var current = 0.30
    while (current <= 0.951) {
        factors.add(String.format(Locale.US, "%.2f", current))
        current += 0.05
    }
    factors.add("default")
    return factors
}

/**
 * Generates FPS target steps up to the device's max supported refresh rate.
 * On SDK <= 33, limits to GameManagerService's allowlist {30, 40, 45, 60, 90, 120}.
 * On SDK > 33, generates multiples of 30 up to maxRefreshRate.
 */
@Suppress("DEPRECATION")
fun getSupportedFpsTargets(context: Context): List<String> {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val maxRefreshRate = wm.defaultDisplay.supportedModes
        .maxOf { it.refreshRate }
        .toInt()

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
        val steps = LEGACY_FPS_STEPS.filter { it <= maxRefreshRate }.map { it.toString() }
        return steps.ifEmpty { listOf("60") }
    }

    val steps = mutableListOf<String>()
    var fps = 30
    while (fps <= maxRefreshRate) {
        steps.add(fps.toString())
        fps += 30
    }
    return steps
}
