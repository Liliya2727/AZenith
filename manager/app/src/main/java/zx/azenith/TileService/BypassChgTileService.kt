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

package zx.azenith.TileService

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import zx.azenith.R
import zx.azenith.ui.util.ConfigUtils

class BypassChgTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (tile.state == Tile.STATE_UNAVAILABLE) return

        if (tile.state == Tile.STATE_ACTIVE) {
            setBypassChg(false)
            tile.state = Tile.STATE_INACTIVE
            updateSubtitle(tile, getString(R.string.status_inactive))
        } else {
            setBypassChg(true)
            tile.state = Tile.STATE_ACTIVE
            updateSubtitle(tile, getString(R.string.status_active))
        }

        tile.updateTile()
    }

    private fun setBypassChg(enabled: Boolean) {
        ConfigUtils.setBool("bypass_chg", enabled)
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val cfg = ConfigUtils.load()
        val bypassPath = cfg.bypassPath

        if (bypassPath == "UNSUPPORTED") {
            tile.state = Tile.STATE_UNAVAILABLE
            updateSubtitle(tile, getString(R.string.status_not_supported))
        } else {
            val isEnabled = cfg.bypassChg
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
