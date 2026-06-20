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
import org.json.JSONObject


fun getSystemProperty(key: String, defaultValue: String = ""): String {
    return try {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod("get", String::class.java, String::class.java)
        method.invoke(null, key, defaultValue) as String
    } catch (e: Exception) {
        defaultValue
    }
}


fun getChipsetName(context: Context): String {

    val boardPlatform = getSystemProperty("ro.board.platform").trim()
    val hardware = Build.HARDWARE.trim()
    val board = Build.BOARD.trim()
    

    val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.SOC_MODEL.trim()
    } else ""


    val rawCodes = listOf(socModel, boardPlatform, hardware, board).filter { 
        it.isNotEmpty() && it != Build.UNKNOWN 
    }

    return try {

        val inputStream = context.assets.open("socs.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        var matchedName: String? = null


        for (code in rawCodes) {
            matchedName = findSocInJson(jsonObject, code)
            if (matchedName != null) break
        }


        matchedName ?: "Unknown (${rawCodes.firstOrNull() ?: "SoC"})"
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown (${rawCodes.firstOrNull() ?: "SoC"})"
    }
}


private fun findSocInJson(json: JSONObject, searchKey: String): String? {
    val lowerSearchKey = searchKey.lowercase()
    

    val keys = mutableListOf<String>()
    val keysIterator = json.keys()
    while (keysIterator.hasNext()) {
        keys.add(keysIterator.next())
    }


    for (key in keys) {
        if (key.lowercase() == lowerSearchKey) {
            return extractSocName(json, key)
        }
    }



    for (key in keys) {
        val lowerKey = key.lowercase()

        if (lowerKey.contains(lowerSearchKey) || lowerSearchKey.contains(lowerKey)) {
            return extractSocName(json, key)
        }
    }

    return null
}


private fun extractSocName(json: JSONObject, key: String): String? {
    val socObj = json.getJSONObject(key)
    val vendor = socObj.optString("VENDOR", "").trim()
    val name = socObj.optString("NAME", "").trim()
    
    return when {
        vendor.isNotEmpty() && name.isNotEmpty() -> "$vendor $name"
        name.isNotEmpty() -> name
        else -> null
    }
}
