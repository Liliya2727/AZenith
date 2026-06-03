package zx.azenith.ui.util

import android.content.Context
import android.os.Build
import org.json.JSONObject

// Fungsi membaca properti sistem tanpa Root (menggunakan Reflection)
fun getSystemProperty(key: String, defaultValue: String = ""): String {
    return try {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getDeclaredMethod("get", String::class.java, String::class.java)
        method.invoke(null, key, defaultValue) as String
    } catch (e: Exception) {
        defaultValue
    }
}

// Fungsi utama mengambil nama Chipset
fun getChipsetName(context: Context): String {
    // Ambil kode SoC dari beberapa sumber yang umum digunakan Android
    val boardPlatform = getSystemProperty("ro.board.platform")
    val hardware = Build.HARDWARE
    val board = Build.BOARD

    val rawCodes = listOf(boardPlatform, hardware, board).filter { it.isNotEmpty() }

    return try {
        // Baca file socs.json dari folder assets
        val inputStream = context.assets.open("socs.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        var matchedName: String? = null

        // Cek satu per satu dari daftar kode raw
        for (code in rawCodes) {
            matchedName = findSocInJson(jsonObject, code)
            if (matchedName != null) break
        }

        // Fallback tambahan (API 31+) jika OEM menanamkan nama asli di sistem
        if (matchedName == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            if (socModel != Build.UNKNOWN) {
                matchedName = socModel
            }
        }

        // Jika ketemu, tampilkan. Jika tidak, kembalikan kode raw terbaik yang ada
        matchedName ?: "Unknown (${rawCodes.firstOrNull() ?: "SoC"})"
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown (${rawCodes.firstOrNull() ?: "SoC"})"
    }
}

// Helper untuk mencari secara case-insensitive agar "gs301" dan "GS301" terbaca sama
private fun findSocInJson(json: JSONObject, searchKey: String): String? {
    val lowerSearchKey = searchKey.lowercase()
    val keys = json.keys()
    
    while (keys.hasNext()) {
        val key = keys.next()
        if (key.lowercase() == lowerSearchKey) {
            val socObj = json.getJSONObject(key)
            val vendor = socObj.optString("VENDOR", "").trim()
            val name = socObj.optString("NAME", "").trim()
            
            return when {
                vendor.isNotEmpty() && name.isNotEmpty() -> "$vendor $name" // Contoh: "Google Tensor G3"
                name.isNotEmpty() -> name
                else -> null
            }
        }
    }
    return null
}
