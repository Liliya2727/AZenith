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
import android.provider.Settings
import android.util.Log
import com.topjohnwu.superuser.Shell

class RefreshRateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AZenith"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "zx.azenith.SET_FPS") return

        val fps = intent.getIntExtra("fps", 60)
        Log.d(TAG, "Applying refresh rate: $fps")

        applyRefreshRate(context, fps)
    }

    private fun applyRefreshRate(context: Context, fps: Int) {
        val resolver = context.contentResolver

        listOf("peak_refresh_rate", "min_refresh_rate", "user_refresh_rate").forEach { key ->
            try {
                Settings.System.putFloat(resolver, key, fps.toFloat())
                Log.d(TAG, "System.$key = $fps ✓")
            } catch (e: Exception) {
                Log.w(TAG, "System.$key failed: ${e.message}")
            }
        }

        try {
            Settings.Secure.putInt(resolver, "miui_refresh_rate", fps)
            Log.d(TAG, "Secure.miui_refresh_rate = $fps ✓")
        } catch (e: Exception) {
            Log.w(TAG, "Secure.miui_refresh_rate failed: ${e.message}")
        }

        applyViaRoot(fps)
    }

    private fun applyViaRoot(fps: Int) {
        val commands = arrayOf(
            // System
            "settings put system peak_refresh_rate $fps",
            "settings put system min_refresh_rate $fps",
            "settings put system user_refresh_rate $fps",
            // Global (Android 12+)
            "settings put global peak_refresh_rate $fps",
            "settings put global min_refresh_rate $fps",
            // MIUI (integer, secure)
            "settings put secure miui_refresh_rate $fps",
        )

        val result = Shell.cmd(*commands).exec()
        Log.d("AZenith", "Root result: ${result.isSuccess}, out: ${result.out}")
    }
}
