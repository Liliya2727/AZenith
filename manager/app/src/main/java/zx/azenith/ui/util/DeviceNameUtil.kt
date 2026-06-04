package zx.azenith.ui.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import java.io.File
import java.io.FileOutputStream

fun getRealDeviceName(context: Context): String {
    val mfg = Build.MANUFACTURER
    val model = Build.MODEL
    val device = Build.DEVICE

    // 1. Perbaiki fallback agar tidak dobel
    val defaultName = if (model.contains(mfg, ignoreCase = true)) {
        model
    } else {
        "$mfg $model"
    }

    // 2. Bikin versi "Bersih" dengan menghapus nama pabrikan
    val cleanModel = model.replace(mfg, "", ignoreCase = true).trim(' ', '-', '_')
    val cleanDevice = device.replace(mfg, "", ignoreCase = true).trim(' ', '-', '_')

    val dbName = "devices.db"
    val dbFile = context.getDatabasePath(dbName)

    // Ekstrak database jika belum ada
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
            // Jika ekstrak gagal, kembalikan fallback
            return if (defaultName.contains(device, ignoreCase = true)) defaultName else "$defaultName ($device)"
        }
    }

    var marketingName = ""
    var db: SQLiteDatabase? = null
    
    if (dbFile.exists()) {
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            // 3. Query diperbaiki: HANYA SELECT name (karena kolom brand tidak ada)
            val query = """
                SELECT name 
                FROM devices 
                WHERE model LIKE ? OR model LIKE ? 
                   OR device LIKE ? OR device LIKE ? 
                LIMIT 1
            """.trimIndent()
            
            db.rawQuery(query, arrayOf(model, cleanModel, device, cleanDevice)).use { cursor ->
                if (cursor.moveToFirst()) {
                    // Ambil string dari kolom index 0 (yaitu kolom 'name')
                    val nameFromDb = cursor.getString(0)
                    
                    // Terkadang database berisi nilai null atau kosong
                    if (!nameFromDb.isNullOrBlank()) {
                        // Tambahkan nama pabrikan (mfg) jika belum ada di dalam nama marketing
                        marketingName = if (nameFromDb.contains(mfg, ignoreCase = true)) {
                            nameFromDb.trim()
                        } else {
                            "$mfg $nameFromDb".trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
    }

    // 4. Tahap Akhir: Pembersihan Duplikat Codename dan Penggabungan
    val finalName = marketingName.ifEmpty { defaultName }
    
    var cleanFinal = finalName.replace("($device)", "", ignoreCase = true).trim()
    cleanFinal = cleanFinal.replace("$device $device", device, ignoreCase = true).trim()
    
    return if (cleanFinal.contains(device, ignoreCase = true)) {
        cleanFinal
    } else {
        "$cleanFinal ($device)"
    }
}
