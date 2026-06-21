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
    
    val script = """
        saveToDownloads="$saveToDownloads"
        TMP_DIR="/data/local/tmp/az_logs_tmp"
        rm -rf ${'$'}TMP_DIR
        mkdir -p ${'$'}TMP_DIR/debug
        
        # Copy file utama yang diminta
        cp -r /sys/fs/pstore ${'$'}TMP_DIR/ 2>/dev/null
        cp /data/adb/.config/AZenith/sysmon.log ${'$'}TMP_DIR/ 2>/dev/null
        cp -r /data/adb/.config/AZenith/debug/* ${'$'}TMP_DIR/debug/ 2>/dev/null
        
        # Dump dmesg & logcat
        dmesg > ${'$'}TMP_DIR/dmesg.txt
        logcat -d > ${'$'}TMP_DIR/logcat.txt
        
        # Grab root manager logs (ditambahkan alternatif path magisk yang umum)
        cat /data/adb/ksu/log/* > ${'$'}TMP_DIR/ksu.log 2>/dev/null
        cat /cache/magisk.log > ${'$'}TMP_DIR/magisk.log 2>/dev/null
        cat /data/adb/magisk.log >> ${'$'}TMP_DIR/magisk.log 2>/dev/null
        cat /data/adb/ap/log/* > ${'$'}TMP_DIR/apatch.log 2>/dev/null
        cat /data/adb/apatch/log/* >> ${'$'}TMP_DIR/apatch.log 2>/dev/null
        
        # Compress jadi tar.gz
        cd /data/local/tmp
        tar -czf "$fileName" -C az_logs_tmp .
        
        # Pindahkan ke tujuan akhir
        cp "$fileName" "$targetPath"
        
        # Penanganan khusus berdasarkan lokasi penyimpanan
        if [ "$saveToDownloads" = "true" ]; then
            # Berikan izin ke group media_rw (1023) agar kebaca sistem
            chown 1023:1023 "$targetPath"
            chmod 664 "$targetPath"
        else
            # WAJIB: Kembalikan kepemilikan ke aplikasi dan perbaiki SELinux Context
            chown $appUid:$appUid "$targetPath"
            chmod 600 "$targetPath"
            restorecon "$targetPath"
        fi
        
        # Bersihin temp files
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

