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

package zx.azenith

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.Display
import android.util.Log
import com.topjohnwu.superuser.Shell

class RefreshRateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AZenith"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "zx.azenith.SET_FPS") return

        val fps = intent.getIntExtra("fps", 60)
        Log.d(TAG, "Applying refresh rate: ${fps}Hz")

        applyRefreshRate(context, fps)
    }

    private fun applyRefreshRate(context: Context, fps: Int) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

        if (display == null) {
            Log.w(TAG, "Display not available")
            return
        }

        // Sort descending → index 0 = highest rate (SF behavior)
        val supportedRates = display.supportedModes
            .map { Math.round(it.refreshRate) }
            .distinct()
            .sortedDescending()

        Log.d(TAG, "Supported rates: $supportedRates")

        val sfIndex = supportedRates.indexOf(fps)

        if (sfIndex == -1) {
            Log.w(TAG, "FPS $fps not supported on this device. Supported: $supportedRates")
            return
        }

        val commands = arrayOf(
            "service call SurfaceFlinger 1035 i32 $sfIndex",
            "setprop persist.vendor.display.refresh_rate $fps",
            "setprop persist.sys.display.refresh_rate $fps",
        )

        val result = Shell.cmd(*commands).exec()

        if (result.isSuccess) {
            Log.d(TAG, "Refresh rate → ${fps}Hz (SF index: $sfIndex of $supportedRates) ✓")
        } else {
            Log.w(TAG, "Failed to set ${fps}Hz: ${result.err}")
        }
    }
}