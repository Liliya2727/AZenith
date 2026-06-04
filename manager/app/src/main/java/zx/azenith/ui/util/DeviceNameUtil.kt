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

    // 2. Bikin versi "Bersih" dengan menghapus nama pabrikan dan karakter aneh
    val cleanModel = model.replace(mfg, "", ignoreCase = true).trim(' ', '-', '_')
    val cleanDevice = device.replace(mfg, "", ignoreCase = true).trim(' ', '-', '_')

    val dbName = "devices.db"
    val dbFile = context.getDatabasePath(dbName)

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
        }
    }

    var marketingName = ""
    var db: SQLiteDatabase? = null
    
    if (dbFile.exists()) {
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            // 3. Cek kecocokan menggunakan RAW dan CLEAN
            val query = """
                SELECT brand, name 
                FROM devices 
                WHERE model COLLATE NOCASE IN (?, ?) 
                   OR device COLLATE NOCASE IN (?, ?) 
                LIMIT 1
            """.trimIndent()
            
            db.rawQuery(query, arrayOf(model, cleanModel, device, cleanDevice)).use { cursor ->
                if (cursor.moveToFirst()) {
                    val brand = cursor.getString(0) ?: ""
                    val name = cursor.getString(1) ?: ""
                    
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
    }

    // 4. Tahap Akhir: Pembersihan Duplikat Codename
    val finalName = marketingName.ifEmpty { defaultName }
    
    // Hapus format "(codename)" jika kebetulan dari DB sudah membawanya
    var cleanFinal = finalName.replace("($device)", "", ignoreCase = true).trim()
    
    // Pastikan tidak ada duplikat berjejer karena kelalaian data
    cleanFinal = cleanFinal.replace("$device $device", device, ignoreCase = true)
    
    // Kembalikan nama ditambah codename, tapi JANGAN tambah lagi kalau teksnya sudah punya codename
    return if (cleanFinal.contains(device, ignoreCase = true)) {
        cleanFinal
    } else {
        "$cleanFinal ($device)"
    }
}
