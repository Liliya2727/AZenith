/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith

import android.service.quicksettings.Tile
import zx.azenith.R
import android.service.quicksettings.TileService
import zx.azenith.ui.util.PropertyUtils

class BypassChgTileService : TileService() {

    private val BYPASS_PROP = "persist.sys.azenithconf.bypasschg"
    private val PATH_PROP = "persist.sys.azenithconf.bypasspath"

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        
        if (tile.state == Tile.STATE_UNAVAILABLE) return
        if (tile.state == Tile.STATE_ACTIVE) {
            PropertyUtils.set(BYPASS_PROP, "0")
            tile.state = Tile.STATE_INACTIVE
            updateSubtitle(tile, getString(R.string.status_inactive))
        } else {
            PropertyUtils.set(BYPASS_PROP, "1")
            tile.state = Tile.STATE_ACTIVE
            updateSubtitle(tile, getString(R.string.status_active))
        }
        
        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val bypassPath = PropertyUtils.get(PATH_PROP, "")
        
        if (bypassPath == "UNSUPPORTED") {
            tile.state = Tile.STATE_UNAVAILABLE
            updateSubtitle(tile, getString(R.string.status_not_supported))
        } else {
            val isEnabled = PropertyUtils.get(BYPASS_PROP, "0") == "1"
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateSubtitle(tile, getString(if (isEnabled) R.string.status_active else R.string.status_inactive))
        }
        
        tile.updateTile()
    }

    private fun updateSubtitle(tile: Tile, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            tile.subtitle = text
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }
}
