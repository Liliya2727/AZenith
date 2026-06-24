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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "zx.azenith.SET_FPS") {
            val fps = intent.getIntExtra("fps", 60).toFloat()
            
            Log.d("AZenith", "Attempting to apply refresh rate: $fps")

            // Terapkan ke masing-masing key dengan helper yang aman
            applySetting(context, "peak_refresh_rate", fps, isSecure = false)
            applySetting(context, "min_refresh_rate", fps, isSecure = false)
            applySetting(context, "user_refresh_rate", fps, isSecure = false)
            applySetting(context, "miui_refresh_rate", fps, isSecure = true)
            
            Log.d("AZenith", "Finished applying refresh rate tweaks")
        }
    }

    private fun applySetting(context: Context, key: String, value: Float, isSecure: Boolean) {
        val resolver = context.contentResolver
        try {
            if (isSecure) {
                Settings.Secure.putFloat(resolver, key, value)
            } else {
                Settings.System.putFloat(resolver, key, value)
            }
        } catch (e: IllegalArgumentException) {
            Log.w("AZenith", "Key $key rejected by System, falling back to Secure.")
            try {
                Settings.Secure.putFloat(resolver, key, value)
            } catch (e2: Exception) {
                fallbackToRoot(key, value)
            }
        } catch (e: SecurityException) {
            Log.w("AZenith", "Lacking WRITE_SETTINGS permission for $key, using Root Shell.")
            fallbackToRoot(key, value)
        } catch (e: Exception) {
            fallbackToRoot(key, value)
        }
    }

    private fun fallbackToRoot(key: String, value: Float) {
        Shell.cmd("settings put system $key $value").exec()
        Shell.cmd("settings put secure $key $value").exec()
    }
}
