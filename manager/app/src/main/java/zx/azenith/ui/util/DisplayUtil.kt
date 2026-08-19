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
import android.view.WindowManager
import java.util.Locale

/**
 * Generates downscale factor steps from 0.30 to 0.95 in 0.05 increments,
 * with "default" (native, no downscale) placed last. 1.00 is omitted
 * since it's functionally identical to "default".
 */
fun getSupportedDownscaleFactors(): List<String> {
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
 * Generates FPS target steps in multiples of 30, up to the device's
 * max supported refresh rate. No "default" entry — 60fps is the baseline.
 */
@Suppress("DEPRECATION")
fun getSupportedFpsTargets(context: Context): List<String> {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val maxRefreshRate = wm.defaultDisplay.supportedModes
        .maxOf { it.refreshRate }
        .toInt()

    val steps = mutableListOf<String>()
    var fps = 30
    while (fps <= maxRefreshRate) {
        steps.add(fps.toString())
        fps += 30
    }
    return steps
}
