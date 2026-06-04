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

    // 1. Perbaiki fallback agar tidak dobel (Misal: "INFINIX Infinix X6739" -> "Infinix X6739")
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
            // Jika gagal ekstrak, tambahkan codename di dalam kurung jika beda dengan model
            return if (defaultName.contains(device, ignoreCase = true)) defaultName else "$defaultName ($device)"
        }
    }

    var marketingName = ""
    var db: SQLiteDatabase? = null
    
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
        
        // Masukkan 4 parameter sesuai jumlah tanda tanya (?)
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

    // 4. Gabungkan hasil akhir dengan menambahkan (codename)
    return if (marketingName.isNotEmpty()) {
        "$marketingName ($device)"
    } else {
        // Fallback jika tidak ada di DB, tambahkan codename juga asalkan tidak sama persis dengan fallback-nya
        if (defaultName.contains(device, ignoreCase = true)) {
            defaultName
        } else {
            "$defaultName ($device)"
        }
    }
}
