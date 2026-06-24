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
    val cacheDir = context.cacheDir
    cacheDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("AZenith_Logs_") && file.name.endsWith(".tar.gz")) {
            file.delete()
        }
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "AZenith_Logs_$timeStamp.tar.gz"
    val appUid = context.applicationInfo.uid

    val targetPath = if (saveToDownloads) {
        "/data/media/0/Download/$fileName"
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
    
    val script = """
        saveToDownloads="$saveToDownloads"
        TMP_DIR="/data/local/tmp/az_logs_tmp"
        rm -rf ${'$'}TMP_DIR
        mkdir -p ${'$'}TMP_DIR
        
        # Salin seluruh berkas dan struktur direktori AZenith tanpa terkecuali
        cp -r /data/adb/.config/AZenith/* ${'$'}TMP_DIR/ 2>/dev/null
        
        # Buat berkas system_info baru di root arsip sementara
        INFO_FILE="${'$'}TMP_DIR/system_info.txt"
        
        MODULE_VER=\$(grep '^version=' /data/adb/modules/AZenith/module.prop | cut -d= -f2)
        [ -z "${'$'}MODULE_VER" ] && MODULE_VER="Unknown"
        
        KERNEL_INFO=\$(uname -r -m)
        ANDROID_VER=\$(getprop ro.build.version.release)
        API_LEVEL=\$(getprop ro.build.version.sdk)
        FINGERPRINT=\$(getprop ro.build.fingerprint)
        SELINUX_STATUS=\$(getenforce 2>/dev/null)
        
        echo "################################################################" > "${"$"}${"{INFO_FILE}"}"
        echo "                       AZenith Diagnostics                      " >> "${"$"}${"{INFO_FILE}"}"
        echo "################################################################" >> "${"$"}${"{INFO_FILE}"}"
        echo "" >> "${"$"}${"{INFO_FILE}"}"
        echo "  [Device Information]" >> "${"$"}${"{INFO_FILE}"}"
        echo "    Device Name   : $realDeviceName" >> "${"$"}${"{INFO_FILE}"}"
        echo "    Chipset       : $chipsetName" >> "${"$"}${"{INFO_FILE}"}"
        echo "    Android Ver   : ${'$'}ANDROID_VER (API ${'$'}API_LEVEL)" >> "${"$"}${"{INFO_FILE}"}"
        echo "    Kernel        : ${'$'}KERNEL_INFO" >> "${"$"}${"{INFO_FILE}"}"
        echo "    SELinux Status: ${'$'}SELINUX_STATUS" >> "${"$"}${"{INFO_FILE}"}"
        echo "    Fingerprint   : ${'$'}FINGERPRINT" >> "${"$"}${"{INFO_FILE}"}"
        echo "" >> "${"$"}${"{INFO_FILE}"}"
        echo "  [AZenith Software]" >> "${"$"}${"{INFO_FILE}"}"
        echo "    App Version   : $appVersion" >> "${"$"}${"{INFO_FILE}"}"
        echo "    Module Version: ${'$'}MODULE_VER" >> "${"$"}${"{INFO_FILE}"}"
        echo "" >> "${"$"}${"{INFO_FILE}"}"
        echo "################################################################" >> "${"$"}${"{INFO_FILE}"}"
        echo "" >> "${"$"}${"{INFO_FILE}"}"
        
        if [ -f "/data/adb/.config/AZenith/debug/AZenith.log" ]; then
            echo "--- START OF AZENITH EXECUTION LOG ---" >> "${"$"}${"{INFO_FILE}"}"
            cat /data/adb/.config/AZenith/debug/AZenith.log >> "${"$"}${"{INFO_FILE}"}"
        fi

        cp -r /sys/fs/pstore ${'$'}TMP_DIR/ 2>/dev/null
        
        dmesg > ${'$'}TMP_DIR/dmesg.txt
        logcat -d > ${'$'}TMP_DIR/logcat.txt
        
        cat /data/adb/ksu/log/* > ${'$'}TMP_DIR/ksu.log 2>/dev/null
        cat /cache/magisk.log > ${'$'}TMP_DIR/magisk.log 2>/dev/null
        cat /data/adb/magisk.log >> ${'$'}TMP_DIR/magisk.log 2>/dev/null
        cat /data/adb/ap/log/* > ${'$'}TMP_DIR/apatch.log 2>/dev/null
        cat /data/adb/apatch/log/* >> ${'$'}TMP_DIR/apatch.log 2>/dev/null
        
        cd /data/local/tmp
        tar -czf "$fileName" -C az_logs_tmp .
        
        cp "$fileName" "$targetPath"
        
        if [ "$saveToDownloads" = "true" ]; then
            chown 1023:1023 "$targetPath"
            chmod 664 "$targetPath"
        else
            chown $appUid:$appUid "$targetPath"
            chmod 600 "$targetPath"
            restorecon "$targetPath"
        fi
        
        rm -rf az_logs_tmp
        rm -f "$fileName"
    """.trimIndent()

    val result = Shell.cmd(script).exec()

    val finalFileForApp = if (saveToDownloads) {
        File("/storage/emulated/0/Download/$fileName")
    } else {
        File(targetPath)
    }

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

