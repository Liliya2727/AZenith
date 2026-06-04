package zx.azenith.ui.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import java.io.File
import java.io.FileOutputStream

fun getRealDeviceName(context: Context): String {
    val defaultName = "${Build.MANUFACTURER} ${Build.MODEL}"
    val dbName = "devices.db"
    val dbFile = context.getDatabasePath(dbName)

    // 1. Ekstrak database dari assets ke internal storage (hanya jika belum ada)
    if (!dbFile.exists()) {
        try {
            dbFile.parentFile?.mkdirs()
            context.assets.open(dbName).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return defaultName
        }
    }

    // 2. Baca database untuk mencari Marketing Name
    var marketingName = ""
    var db: SQLiteDatabase? = null
    
    try {
        db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        
        // Ambil brand dan name, cari berdasarkan model atau device (case-insensitive)
        val query = "SELECT brand, name FROM devices WHERE model COLLATE NOCASE = ? OR device COLLATE NOCASE = ? LIMIT 1"
        
        db.rawQuery(query, arrayOf(Build.MODEL, Build.DEVICE)).use { cursor ->
            if (cursor.moveToFirst()) {
                val brand = cursor.getString(0) ?: ""
                val name = cursor.getString(1) ?: ""
                
                // Mencegah duplikasi brand (misal: "vivo vivo V50")
                marketingName = if (name.contains(brand, ignoreCase = true)) {
                    name
                } else {
                    "$brand $name".trim()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        db?.close()
    }

    // 3. Gabungkan hasil
    return if (marketingName.isNotEmpty()) {
        marketingName
    } else {
        defaultName
    }
}
