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

class ProfileTileService : TileService() {

    private val AI_PROP = "persist.sys.azenithconf.AIenabled"
    // Sesuaikan nama properti profil ini dengan yang digunakan oleh daemon/RootUtils AZenith
    private val PROFILE_PROP = "persist.sys.azenithconf.profile" 

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        // Cek status AI seperti di HomeViewModel
        val aiEnabled = PropertyUtils.get(AI_PROP, "0")
        
        // Blokir eksekusi jika AI aktif atau Tile tidak tersedia
        if (aiEnabled != "0" || tile.state == Tile.STATE_UNAVAILABLE) {
            return 
        }

        // Ambil profil saat ini (Default ke "2" / Balanced jika kosong)
        val currentProfile = PropertyUtils.get(PROFILE_PROP, "2")

        // Logika rotasi: Balanced (2) -> Performance (1) -> ECO (3) -> kembali ke Balanced (2)
        val nextProfile = when (currentProfile) {
            "2" -> "1"
            "1" -> "3"
            "3" -> "2"
            else -> "2"
        }

        // Eksekusi perintah shell yang sama dengan di HomeViewModel
        Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service -p $nextProfile").submit()
        
        // Update properti secara lokal untuk mempercepat pembaruan UI di Tile
        PropertyUtils.set(PROFILE_PROP, nextProfile)

        updateTileState(nextProfile)
    }

    private fun updateTileState(forcedProfile: String? = null) {
        val tile = qsTile ?: return
        val aiEnabled = PropertyUtils.get(AI_PROP, "0")

        if (aiEnabled != "0") {
            tile.state = Tile.STATE_UNAVAILABLE
            updateSubtitle(tile, "AI Controlled") // Bisa diganti dengan string resource
        } else {
            tile.state = Tile.STATE_ACTIVE
            val currentProfile = forcedProfile ?: PropertyUtils.get(PROFILE_PROP, "2")
            
            val subtitleText = when (currentProfile) {
                "1" -> getString(R.string.Profile_Performance)
                "2" -> getString(R.string.Profile_Balanced)
                "3" -> getString(R.string.Profile_ECO_mode)
                else -> getString(R.string.Profile_Balanced)
            }
            updateSubtitle(tile, subtitleText)
        }

        tile.updateTile()
    }

    private fun updateSubtitle(tile: Tile, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = text
        }
    }
}
