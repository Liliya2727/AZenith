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

package zx.azenith.TileService

import android.app.AlertDialog
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zx.azenith.R
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RootUtils

class ProfileTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val AI_PROP = "persist.sys.azenithconf.AIenabled"
        private const val DAEMON_BIN = "/data/adb/modules/AZenith/system/bin/sys.azenith-service"
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            RootUtils.isRootGranted()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        serviceScope.launch {
            val aiEnabled = PropertyUtils.get(AI_PROP, "0")
            val isRooted = RootUtils.isRootGranted()

            withContext(Dispatchers.Main) {
                if (aiEnabled != "0" || tile.state == Tile.STATE_UNAVAILABLE || !isRooted) {
                    return@withContext
                }
                showProfileDialog()
            }
        }
    }

    private fun showProfileDialog() {
        val themedContext = android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_DayNight)
        val inflater = android.view.LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.dialog_profile_selector, null)

        val dialog = AlertDialog.Builder(themedContext, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        view.findViewById<android.view.View>(R.id.item_performance).setOnClickListener {
            showApplyingToast()
            applyProfile("1")
            dialog.dismiss()
        }
        view.findViewById<android.view.View>(R.id.item_balanced).setOnClickListener {
            showApplyingToast()
            applyProfile("2")
            dialog.dismiss()
        }
        view.findViewById<android.view.View>(R.id.item_eco).setOnClickListener {
            showApplyingToast()
            applyProfile("3")
            dialog.dismiss()
        }

        showDialog(dialog)

        dialog.window?.let { window ->
            val layoutParams = window.attributes
            val displayMetrics = resources.displayMetrics
            layoutParams.width = (displayMetrics.widthPixels * 0.85).toInt()
            window.attributes = layoutParams
        }
    }

    private fun showApplyingToast() {
        val message = getString(R.string.toast_applying_profile)
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun applyProfile(nextProfile: String) {
        Shell.cmd("$DAEMON_BIN -p $nextProfile").submit { result ->
            if (result.isSuccess) {
                updateTileState()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        serviceScope.launch {
            val aiEnabled = PropertyUtils.get(AI_PROP, "0")

            if (aiEnabled != "0") {
                withContext(Dispatchers.Main) {
                    tile.state = Tile.STATE_UNAVAILABLE
                    updateSubtitle(tile, getString(R.string.str_auto_mode))
                    tile.updateTile()
                }
            } else {
                val profileResStr = RootUtils.getCurrentProfileRes()

                withContext(Dispatchers.Main) {
                    tile.state = Tile.STATE_ACTIVE
                    updateSubtitle(tile, getString(profileResStr))
                    tile.updateTile()
                }
            }
        }
    }

    private fun updateSubtitle(tile: Tile, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = text
        }
    }
}
