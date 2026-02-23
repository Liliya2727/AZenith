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
import androidx.compose.runtime.Composable
import com.topjohnwu.superuser.io.SuFile
import zx.azenith.R

fun getSELinuxStatus(context: Context): String = SuFile("/sys/fs/selinux/enforce").run {
    when {
        !exists() -> context.getString(R.string.selinux_disabled)
        !isFile -> context.getString(R.string.status_unknown)
        !canRead() -> context.getString(R.string.selinux_enforcing)
        else -> {
            val content = runCatching { 
                newInputStream().bufferedReader().use { it.readLine()?.trim() } 
            }.getOrNull()

            when (content) {
                "1" -> context.getString(R.string.selinux_enforcing)
                "0" -> context.getString(R.string.selinux_permissive)
                else -> context.getString(R.string.status_unknown)
            }
        }
    }
}
