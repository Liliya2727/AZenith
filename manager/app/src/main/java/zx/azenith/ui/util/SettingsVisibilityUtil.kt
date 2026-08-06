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

import com.topjohnwu.superuser.io.SuFile

object DebugUtils {
    private const val FULLMODE_DEBUG_PATH = "/data/adb/.config/AZenith/debug/FullMode"

    fun isFullModeEnabled(): Boolean {
        return try {
            val file = SuFile(FULLMODE_DEBUG_PATH)
            if (!file.exists()) return false

            val content = file.readText().trim()
            content != "0" && content.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
