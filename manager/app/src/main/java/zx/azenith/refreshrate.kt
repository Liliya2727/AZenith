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

class RefreshRateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "zx.azenith.SET_FPS") {
            val fps = intent.getIntExtra("fps", 60).toFloat()
            
            try {
                val contentResolver = context.contentResolver
                
                Settings.System.putFloat(contentResolver, "peak_refresh_rate", fps)
                Settings.System.putFloat(contentResolver, "min_refresh_rate", fps)
                Settings.System.putFloat(contentResolver, "user_refresh_rate", fps)
                Settings.Secure.putFloat(contentResolver, "miui_refresh_rate", fps)
                
                Log.d("AZenith", "Successfully applied native refresh rate: $fps Hz")
            } catch (e: Exception) {
                Log.e("AZenith", "Failed to set native refresh rate", e)
            }
        }
    }
}
