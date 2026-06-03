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
    // 1. Ambil semua kemungkinan kode SoC dan bersihkan spasi berlebih (trim)
    val boardPlatform = getSystemProperty("ro.board.platform").trim()
    val hardware = Build.HARDWARE.trim()
    val board = Build.BOARD.trim()
    
    // Ambil SOC_MODEL untuk API 31+ 
    val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.SOC_MODEL.trim()
    } else ""

    // 2. MASUKKAN socModel KE DALAM LIST agar ikut dicek ke dalam socs.json!
    val rawCodes = listOf(socModel, boardPlatform, hardware, board).filter { 
        it.isNotEmpty() && it != Build.UNKNOWN 
    }

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

        // Jika ketemu, tampilkan. Jika tidak, kembalikan kode raw terbaik yang ada
        matchedName ?: "Unknown (${rawCodes.firstOrNull() ?: "SoC"})"
    } catch (e: Exception) {
        e.printStackTrace()
        "Unknown (${rawCodes.firstOrNull() ?: "SoC"})"
    }
}

// Helper untuk mencari secara pintar di dalam JSON
private fun findSocInJson(json: JSONObject, searchKey: String): String? {
    val lowerSearchKey = searchKey.lowercase()
    
    // Ambil semua key dari JSON
    val keys = mutableListOf<String>()
    val keysIterator = json.keys()
    while (keysIterator.hasNext()) {
        keys.add(keysIterator.next())
    }

    // TAHAP 1: Coba pencarian persis (Exact Match)
    for (key in keys) {
        if (key.lowercase() == lowerSearchKey) {
            return extractSocName(json, key)
        }
    }

    // TAHAP 2: Coba pencarian sebagian (Partial Match)
    // Berfungsi jika sistem mendeteksi "mt6893" tapi di JSON hanya ada "MT6893Z_T/CZA", atau sebaliknya.
    for (key in keys) {
        val lowerKey = key.lowercase()
        // Cek apakah key mengandung searchKey, atau searchKey mengandung key
        if (lowerKey.contains(lowerSearchKey) || lowerSearchKey.contains(lowerKey)) {
            return extractSocName(json, key)
        }
    }

    return null
}

// Ekstraktor untuk menyusun nama VENDOR + NAME
private fun extractSocName(json: JSONObject, key: String): String? {
    val socObj = json.getJSONObject(key)
    val vendor = socObj.optString("VENDOR", "").trim()
    val name = socObj.optString("NAME", "").trim()
    
    return when {
        vendor.isNotEmpty() && name.isNotEmpty() -> "$vendor $name" // Contoh: "MediaTek Dimensity 8050"
        name.isNotEmpty() -> name
        else -> null
    }
}
