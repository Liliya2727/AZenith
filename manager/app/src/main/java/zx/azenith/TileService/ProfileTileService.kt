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
import android.widget.Toast

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
    
        val inflater = android.view.LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_profile_selector, null)
    
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setView(view)
            .create()
    
        // Bikin background dialog transparan supaya rounded drawable kelihatan
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    
        view.findViewById<android.view.View>(R.id.item_performance).setOnClickListener {
            // 👇 Tampilkan info nama profil yang sedang dipasang (misal ngambil string resource)
            showApplyingToast(getString(R.string.profile_performance))
            applyProfile("1")
            dialog.dismiss()
        }
        view.findViewById<android.view.View>(R.id.item_balanced).setOnClickListener {
            showApplyingToast(getString(R.string.profile_balanced))
            applyProfile("2")
            dialog.dismiss()
        }
        view.findViewById<android.view.View>(R.id.item_eco).setOnClickListener {
            showApplyingToast(getString(R.string.profile_eco))
            applyProfile("3")
            dialog.dismiss()
        }
    
        showDialog(dialog)

        // PAKSA LEBAR DIALOG DI SINI (85% dari layar)
        dialog.window?.let { window ->
            val layoutParams = window.attributes
            val displayMetrics = resources.displayMetrics
            layoutParams.width = (displayMetrics.widthPixels * 0.85).toInt()
            window.attributes = layoutParams
        }
    }
    
    private fun showApplyingToast(profileName: String) {
        // Menampilkan pesan "Applying [Nama Profil] Profile..."
        val message = "Applying $profileName Profile"
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun applyProfile(nextProfile: String) {
        Shell.cmd("$DAEMON_BIN -p $nextProfile").submit { result ->
            if (result.isSuccess) {
                PropertyUtils.set(PROFILE_PROP, nextProfile)
                updateTileState()
                
                // Opsional: Jika ingin memberi tahu kalau profil sudah sukses berganti total
                // Toast.makeText(applicationContext, "Profile applied successfully", Toast.LENGTH_SHORT).show()
            }
        }
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
