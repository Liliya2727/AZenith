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
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import java.io.File
import java.io.FileOutputStream


fun getRealDeviceName(context: Context): String {
    val mfg = Build.MANUFACTURER
    val model = Build.MODEL
    val device = Build.DEVICE


    val defaultName = if (model.contains(mfg, ignoreCase = true)) {
        model
    } else {
        "$mfg $model"
    }


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

            return if (defaultName.contains(device, ignoreCase = true)) defaultName else "$defaultName ($device)"
        }
    }

    var marketingName = ""
    var db: SQLiteDatabase? = null
    
    if (dbFile.exists()) {
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            

            val query = """
                SELECT name 
                FROM devices 
                WHERE model LIKE ? OR model LIKE ? 
                   OR device LIKE ? OR device LIKE ? 
                LIMIT 1
            """.trimIndent()
            
            db.rawQuery(query, arrayOf(model, cleanModel, device, cleanDevice)).use { cursor ->
                if (cursor.moveToFirst()) {

                    val nameFromDb = cursor.getString(0)
                    

                    if (!nameFromDb.isNullOrBlank()) {

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


    val finalName = marketingName.ifEmpty { defaultName }
    
    var cleanFinal = finalName.replace("($device)", "", ignoreCase = true).trim()
    cleanFinal = cleanFinal.replace("$device $device", device, ignoreCase = true).trim()
    
    return if (cleanFinal.contains(device, ignoreCase = true)) {
        cleanFinal
    } else {
        "$cleanFinal ($device)"
    }
}
