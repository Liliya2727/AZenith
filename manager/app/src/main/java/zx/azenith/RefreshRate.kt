package zx.azenith

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefreshRateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AZenith"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "zx.azenith.SET_FPS") return

        val fps = intent.getIntExtra("fps", 60)
        Log.d(TAG, "Applying refresh rate: ${fps}Hz")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                applyRefreshRate(context, fps)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun applyRefreshRate(context: Context, fps: Int) {
        val modDir = "/data/adb/.config/AZenith"
        val jsonFile = "$modDir/rr_modes.json"

        val findIdCmd = """
            if [ -f "$jsonFile" ]; then
                cat "$jsonFile" | grep -o '{"id":[0-9]*,"hz":$fps}' | head -n1 | grep -o '[0-9]*' | head -n1
            else
                dumpsys SurfaceFlinger | grep -oE 'id=[0-9]+,[^}]*vsyncRate=[0-9.]+' | grep "vsyncRate=$fps\." | head -n1 | sed -E 's/.*id=([0-9]+).*/\1/'
            fi
        """.trimIndent()

        val output = Shell.cmd(findIdCmd).exec().out
        val targetId = output.firstOrNull()?.trim()?.toIntOrNull()

        if (targetId == null) {
            Log.w(TAG, "Hardware mode ID not found for ${fps}Hz")
            return
        }

        val commands = arrayOf(
            "service call SurfaceFlinger 1035 i32 $targetId",
            "echo $targetId > $modDir/current_rr.txt",
            "setprop persist.vendor.display.refresh_rate $fps",
            "setprop persist.sys.display.refresh_rate $fps"
        )

        val result = Shell.cmd(*commands).exec()

        if (result.isSuccess) {
            Log.d(TAG, "Refresh rate locked successfully → ${fps}Hz (SF ID: $targetId) ✓")
        } else {
            Log.w(TAG, "Failed to lock refresh rate: ${result.err}")
        }
    }
}
