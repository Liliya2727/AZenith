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
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject


object BackupManager {

    private const val SECRET_KEY = "ZexshiaAZenithSecuredKey" 
    private const val ALGORITHM = "AES"


    fun getSocName(type: String?): String {
        return when (type) {
            "1" -> "MediaTek"
            "2" -> "Snapdragon"
            "3" -> "Exynos"
            "4" -> "Unisoc"
            "5" -> "Tensor"
            else -> "Unknown ($type)"
        }
    }


    private fun encryptData(data: String): ByteArray {
        val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(data.toByteArray())
        return Base64.encode(encrypted, Base64.NO_WRAP)
    }


    private fun decryptData(data: ByteArray): String {
        val decoded = Base64.decode(data, Base64.NO_WRAP)
        val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return String(cipher.doFinal(decoded))
    }


    fun createBackup(context: Context, uri: Uri, properties: Map<String, String>): Boolean {
        return try {
            val jsonObject = JSONObject()
            properties.forEach { (key, value) -> jsonObject.put(key, value) }
            
            val encryptedData = encryptData(jsonObject.toString())
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(encryptedData)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun readBackup(context: Context, uri: Uri): Map<String, String>? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArrayOutputStream()
                inputStream.copyTo(buffer)
                buffer.toByteArray()
            } ?: return null

            val decryptedJson = decryptData(bytes)
            val jsonObject = JSONObject(decryptedJson)
            
            val resultMap = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                resultMap[key] = jsonObject.getString(key)
            }
            resultMap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
