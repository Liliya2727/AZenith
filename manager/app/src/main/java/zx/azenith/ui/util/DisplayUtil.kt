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
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Returns the device's native (smallest-dimension) width in pixels,
 * regardless of any active window size override.
 */
@Suppress("DEPRECATION")
fun getDeviceNativeWidth(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    wm.defaultDisplay.getRealMetrics(metrics)
    return minOf(metrics.widthPixels, metrics.heightPixels)
}

/**
 * Generates a descending list of resolution width options,
 * stepping down from the device's native width to 240p.
 * "default" is always first, representing native/unscaled resolution.
 */
fun getSupportedResolutions(context: Context): List<String> {
    val maxWidth = getDeviceNativeWidth(context)
    val step = 120
    val floor = 240

    val widths = mutableListOf<Int>()
    var current = maxWidth - step
    while (current > floor) {
        widths.add(current)
        current -= step
    }
    widths.add(floor)

    return listOf("default") + widths.map { it.toString() }
}