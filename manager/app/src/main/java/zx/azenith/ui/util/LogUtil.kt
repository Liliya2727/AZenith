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
import android.content.Intent
import android.media.MediaScannerConnection
import androidx.core.content.FileProvider
import com.topjohnwu.superuser.Shell
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun dumpDiagnosticLogs(context: Context, saveToDownloads: Boolean): File? = withContext(Dispatchers.IO) {
    // Bersihkan file log lama di cache
    val cacheDir = context.cacheDir
    cacheDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("AZenith_Logs_") && file.name.endsWith(".tar.gz")) {
            file.delete()
        }
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "AZenith_Logs_$timeStamp.tar.gz"
    val appUid = context.applicationInfo.uid

    // UBAH: Gunakan /storage/emulated/0 agar lewat FUSE Android (Otomatis ngatur permission)
    val targetPath = if (saveToDownloads) {
        "/storage/emulated/0/Download/$fileName"
    } else {
        "${context.cacheDir.absolutePath}/$fileName"
    }
    
    val realDeviceName = getRealDeviceName(context)
    val chipsetName = getChipsetName(context)
    val appVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "Unknown"
    }
    
    // TRIK AMPUH: Gunakan variabel ini untuk memanggil variabel Bash agar tidak bentrok dengan Kotlin
    val d = "$" 
    
    val script = """
        TMP_DIR="/data/local/tmp/az_logs_tmp"
        rm -rf ${d}TMP_DIR
        mkdir -p ${d}TMP_DIR
        
        # Salin seluruh berkas dan struktur direktori AZenith tanpa terkecuali
        cp -r /data/adb/.config/AZenith/* ${d}TMP_DIR/ 2>/dev/null
        
        # Buat berkas system_info baru di root arsip sementara
        INFO_FILE="${d}TMP_DIR/system_info.txt"
        
        # UBAH: Gunakan perintah sub-shell bash yang benar tanpa escape \ yang merusak
        MODULE_VER=${d}(grep '^version=' /data/adb/modules/AZenith/module.prop 2>/dev/null | cut -d= -f2)
        [ -z "${d}MODULE_VER" ] && MODULE_VER="Unknown"
        
        KERNEL_INFO=${d}(uname -r -m)
        ANDROID_VER=${d}(getprop ro.build.version.release)
        API_LEVEL=${d}(getprop ro.build.version.sdk)
        FINGERPRINT=${d}(getprop ro.build.fingerprint)
        SELINUX_STATUS=${d}(getenforce 2>/dev/null)
        
        echo "################################################################" > "${d}INFO_FILE"
        echo "                       AZenith Diagnostics                      " >> "${d}INFO_FILE"
        echo "################################################################" >> "${d}INFO_FILE"
        echo "" >> "${d}INFO_FILE"
        echo "  [Device Information]" >> "${d}INFO_FILE"
        echo "    Device Name   : $realDeviceName" >> "${d}INFO_FILE"
        echo "    Chipset       : $chipsetName" >> "${d}INFO_FILE"
        echo "    Android Ver   : ${d}ANDROID_VER (API ${d}API_LEVEL)" >> "${d}INFO_FILE"
        echo "    Kernel        : ${d}KERNEL_INFO" >> "${d}INFO_FILE"
        echo "    SELinux Status: ${d}SELINUX_STATUS" >> "${d}INFO_FILE"
        echo "    Fingerprint   : ${d}FINGERPRINT" >> "${d}INFO_FILE"
        echo "" >> "${d}INFO_FILE"
        echo "  [AZenith Software]" >> "${d}INFO_FILE"
        echo "    App Version   : $appVersion" >> "${d}INFO_FILE"
        echo "    Module Version: ${d}MODULE_VER" >> "${d}INFO_FILE"
        echo "" >> "${d}INFO_FILE"
        echo "################################################################" >> "${d}INFO_FILE"
        echo "" >> "${d}INFO_FILE"
        
        if [ -f "/data/adb/.config/AZenith/debug/AZenith.log" ]; then
            echo "--- START OF AZENITH EXECUTION LOG ---" >> "${d}INFO_FILE"
            cat /data/adb/.config/AZenith/debug/AZenith.log >> "${d}INFO_FILE"
        fi

        cp -r /sys/fs/pstore ${d}TMP_DIR/ 2>/dev/null
        
        dmesg > ${d}TMP_DIR/dmesg.txt 2>/dev/null
        logcat -d > ${d}TMP_DIR/logcat.txt 2>/dev/null
        
        cat /data/adb/ksu/log/* > ${d}TMP_DIR/ksu.log 2>/dev/null
        cat /cache/magisk.log > ${d}TMP_DIR/magisk.log 2>/dev/null
        cat /data/adb/magisk.log >> ${d}TMP_DIR/magisk.log 2>/dev/null
        cat /data/adb/ap/log/* > ${d}TMP_DIR/apatch.log 2>/dev/null
        cat /data/adb/apatch/log/* >> ${d}TMP_DIR/apatch.log 2>/dev/null
        
        cd /data/local/tmp
        tar -czf "$fileName" -C az_logs_tmp .
        
        # Pastikan folder target ada sebelum di-copy
        mkdir -p "${d}(dirname "$targetPath")"
        cp "$fileName" "$targetPath"
        
        if [ "$saveToDownloads" = "true" ]; then
            # UBAH: Jangan gunakan chown 1023, biarkan FUSE Android yang mengatur
            chmod 666 "$targetPath"
        else
            chown $appUid:$appUid "$targetPath"
            chmod 600 "$targetPath"
            restorecon "$targetPath" 2>/dev/null
        fi
        
        rm -rf az_logs_tmp
        rm -f "$fileName"
    """.trimIndent()

    val result = Shell.cmd(script).exec()

    val finalFileForApp = File(targetPath)

    if (result.isSuccess && finalFileForApp.exists()) {
        if (saveToDownloads) {
            MediaScannerConnection.scanFile(context, arrayOf(finalFileForApp.absolutePath), null, null)
        }
        finalFileForApp
    } else {
        null
    }
}


fun getShareLogIntent(context: Context, file: File): Intent {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gzip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(intent, "Send AZenith Logs")
}

