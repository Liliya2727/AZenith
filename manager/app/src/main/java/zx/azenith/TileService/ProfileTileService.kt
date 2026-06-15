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

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.topjohnwu.superuser.Shell
import zx.azenith.R
import zx.azenith.ui.util.PropertyUtils
import zx.azenith.ui.util.RootUtils
import android.app.AlertDialog
import android.view.WindowManager

class ProfileTileService : TileService() {

    companion object {
        private const val AI_PROP = "persist.sys.azenithconf.AIenabled"
        private const val PROFILE_PROP = "persist.sys.azenithconf.profile"
        private const val DAEMON_BIN = "/data/adb/modules/AZenith/system/bin/sys.azenith-service"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
    
        val aiEnabled = PropertyUtils.get(AI_PROP, "0")
        if (aiEnabled != "0" || tile.state == Tile.STATE_UNAVAILABLE || !RootUtils.isRootGranted()) {
            return 
        }
    
        // Berikan tema dialog perangkat standar (menyesuaikan dark/light mode sistem)
        val builder = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        builder.setTitle("Select AZenith Profile")
    
        // List opsi yang akan tampil di pop-up
        val items = arrayOf("Performance Mode", "Balanced Mode", "ECO Mode")
        val profileValues = arrayOf("1", "2", "3")
    
        builder.setItems(items) { dialog, which ->
            val selectedProfile = profileValues[which]
            
            // Eksekusi daemon sesuai profil yang dipilih user
            Shell.cmd("$DAEMON_BIN -p $selectedProfile").submit { result ->
                if (result.isSuccess) {
                    PropertyUtils.set(PROFILE_PROP, selectedProfile)
                    updateTileState()
                }
            }
            dialog.dismiss()
        }
    
        val dialog = builder.create()
        
        // PENTING: Gunakan method bawaan TileService untuk nampilin di atas QS
        showDialog(dialog)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        
        val aiEnabled = PropertyUtils.get(AI_PROP, "0")

        if (aiEnabled != "0") {
            tile.state = Tile.STATE_UNAVAILABLE
            updateSubtitle(tile, "Auto Mode")
        } else {
            tile.state = Tile.STATE_ACTIVE
            
            val profileResStr = RootUtils.getCurrentProfileRes()
            updateSubtitle(tile, getString(profileResStr))
        }

        tile.updateTile()
    }

    private fun updateSubtitle(tile: Tile, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = text
        }
    }
}




