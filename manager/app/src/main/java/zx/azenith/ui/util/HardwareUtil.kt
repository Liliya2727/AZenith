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
import kotlin.math.abs

fun getSystemProperty(key: String, defaultValue: String = ""): String {
    return try {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod("get", String::class.java, String::class.java)
        method.invoke(null, key, defaultValue) as String
    } catch (e: Exception) {
        defaultValue
    }
}

private fun readSysFile(path: String): String {
    return try {
        java.io.File(path).readText().trim()
    } catch (e: Exception) {
        ""
    }
}

private fun extractChipVariants(code: String): List<String> {
    val variants = mutableListOf(code)

    val beforeSlash = code.substringBefore("/").trim()
    if (beforeSlash != code && beforeSlash.isNotEmpty()) variants.add(beforeSlash)

    val beforeUnderscore = beforeSlash.substringBefore("_").trim()
    if (beforeUnderscore != beforeSlash && beforeUnderscore.isNotEmpty()) variants.add(beforeUnderscore)

    val basePattern = Regex("^([A-Za-z]+\\d+)")
    basePattern.find(beforeUnderscore)?.groupValues?.get(1)?.let { base ->
        if (base !in variants && base.isNotEmpty()) variants.add(base)
    }

    return variants
}

private const val MIN_FUZZY_LEN = 5

fun getChipsetName(context: Context): String {

    val boardPlatform = getSystemProperty("ro.board.platform").trim()
    val hardware      = Build.HARDWARE.trim()
    val board         = Build.BOARD.trim()
    val chipname      = getSystemProperty("ro.hardware.chipname").trim()
    val mtPlatform    = getSystemProperty("ro.mediatek.platform").trim()

    val socMachine = readSysFile("/sys/devices/soc0/machine")
    val socFamily  = readSysFile("/sys/devices/soc0/family")
    val socId      = readSysFile("/sys/devices/soc0/soc_id")

    val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.SOC_MODEL.trim()
    } else ""

    val rawCodes = listOf(
        socModel, chipname, boardPlatform,
        socMachine, socFamily, socId,
        hardware, board,
        mtPlatform
    ).filter { it.isNotEmpty() && it != Build.UNKNOWN }

    if (rawCodes.isEmpty()) return fallbackLabel("SoC")

    return try {
        val inputStream = context.assets.open("socs.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        val keys = mutableListOf<String>()
        val keysIterator = jsonObject.keys()
        while (keysIterator.hasNext()) keys.add(keysIterator.next())

        for (code in rawCodes) {
            findExactMatch(keys, code)?.let { key ->
                return extractSocName(jsonObject, key) ?: fallbackLabel(code)
            }
        }
        for (code in rawCodes) {
            findNormalizedMatch(keys, code)?.let { key ->
                return extractSocName(jsonObject, key) ?: fallbackLabel(code)
            }
        }
        for (code in rawCodes) {
            findBestFuzzyMatch(keys, code)?.let { key ->
                return extractSocName(jsonObject, key) ?: fallbackLabel(code)
            }
        }

        val strippedCodes = rawCodes
            .flatMap { extractChipVariants(it) }
            .filter { it !in rawCodes }
            .distinct()

        for (code in strippedCodes) {
            findExactMatch(keys, code)?.let { key ->
                return extractSocName(jsonObject, key) ?: fallbackLabel(code)
            }
        }
        for (code in strippedCodes) {
            findNormalizedMatch(keys, code)?.let { key ->
                return extractSocName(jsonObject, key) ?: fallbackLabel(code)
            }
        }
        for (code in strippedCodes) {
            findBestFuzzyMatch(keys, code)?.let { key ->
                return extractSocName(jsonObject, key) ?: fallbackLabel(code)
            }
        }

        fallbackLabel(rawCodes.first())
    } catch (e: Exception) {
        e.printStackTrace()
        fallbackLabel(rawCodes.first())
    }
}

fun getChipsetVendor(context: Context): String {
    val chipsetName = getChipsetName(context).lowercase()

    if (!chipsetName.startsWith("unknown")) {
        return when {
            "mediatek" in chipsetName -> "mediatek"
            "qualcomm" in chipsetName || "snapdragon" in chipsetName -> "qualcomm"
            else -> "unknown"
        }
    }

    val socModel      = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.lowercase() else ""
    val boardPlatform = getSystemProperty("ro.board.platform").lowercase()
    val chipname      = getSystemProperty("ro.hardware.chipname").lowercase()
    val mtPlatform    = getSystemProperty("ro.mediatek.platform").lowercase()

    return when {
        socModel.startsWith("mt")      ||
        boardPlatform.startsWith("mt") ||
        chipname.startsWith("mt")      ||
        mtPlatform.isNotEmpty()            -> "mediatek"

        socModel.startsWith("sm")      ||
        socModel.startsWith("msm")     ||
        boardPlatform.startsWith("sm") ||
        boardPlatform.startsWith("msm")    -> "qualcomm"

        else -> "unknown"
    }
}

private fun fallbackLabel(code: String) = "Unknown ($code)"

private fun normalize(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

private fun findExactMatch(keys: List<String>, searchKey: String): String? {
    val lower = searchKey.lowercase()
    return keys.firstOrNull { it.lowercase() == lower }
}

private fun findNormalizedMatch(keys: List<String>, searchKey: String): String? {
    val norm = normalize(searchKey)
    if (norm.isEmpty()) return null
    return keys.firstOrNull { normalize(it) == norm }
}

private fun findBestFuzzyMatch(keys: List<String>, searchKey: String): String? {
    val normSearch = normalize(searchKey)
    if (normSearch.length < MIN_FUZZY_LEN) return null

    var bestKey: String? = null
    var bestScore = -1

    for (key in keys) {
        val normKey = normalize(key)
        if (normKey.length < MIN_FUZZY_LEN) continue

        val isPrefixMatch = normKey.startsWith(normSearch) || normSearch.startsWith(normKey)
        if (!isPrefixMatch) continue

        val score = minOf(normKey.length, normSearch.length) - abs(normKey.length - normSearch.length)
        if (score > bestScore) {
            bestScore = score
            bestKey = key
        }
    }

    return bestKey
}

private fun extractSocName(json: JSONObject, key: String): String? {
    val socObj = json.getJSONObject(key)
    val vendor = socObj.optString("VENDOR", "").trim()
    val name   = socObj.optString("NAME", "").trim()

    return when {
        vendor.isNotEmpty() && name.isNotEmpty() -> "$vendor $name"
        name.isNotEmpty() -> name
        vendor.isNotEmpty() -> vendor
        else -> null
    }
}
