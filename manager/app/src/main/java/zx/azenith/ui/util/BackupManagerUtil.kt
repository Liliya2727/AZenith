package zx.azenith.ui.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object BackupManager {
    // Kunci rahasia untuk AES (Harus 16, 24, atau 32 karakter)
    private const val SECRET_KEY = "ZexshiaAZenithSecuredKey" 
    private const val ALGORITHM = "AES"

    // Helper untuk mengubah nama SOC
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

    // Fungsi enkripsi dan obfuskasi
    private fun encryptData(data: String): ByteArray {
        val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val encrypted = cipher.doFinal(data.toByteArray())
        return Base64.encode(encrypted, Base64.NO_WRAP)
    }

    // Fungsi dekripsi
    private fun decryptData(data: ByteArray): String {
        val decoded = Base64.decode(data, Base64.NO_WRAP)
        val keySpec = SecretKeySpec(SECRET_KEY.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return String(cipher.doFinal(decoded))
    }

    // Membuat file Backup
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

    // Membaca file Backup
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
