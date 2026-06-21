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

import android.annotation.SuppressLint
import com.topjohnwu.superuser.Shell

@SuppressLint("PrivateApi")
object PropertyUtils {

    private val systemPropertiesClass by lazy {
        Class.forName("android.os.SystemProperties")
    }

    private val getMethod by lazy {
        systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
    }

    private val setMethod by lazy {
        systemPropertiesClass.getMethod("set", String::class.java, String::class.java)
    }

    fun get(key: String, def: String = ""): String {
        return try {
            getMethod.invoke(null, key, def) as String
        } catch (e: Exception) {
            def
        }
    }

    fun set(key: String, value: String) {
        try {
            setMethod.invoke(null, key, value)
        } catch (e: Exception) {
            val safeValue = value.replace("'", "'\\''")
            Shell.cmd("setprop $key '$safeValue'").submit()
        }
    }
}

