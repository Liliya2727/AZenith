package zx.azenith

import android.service.quicksettings.Tile
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
            updateSubtitle(tile, "Inactive")
        } else {
            PropertyUtils.set(BYPASS_PROP, "1")
            tile.state = Tile.STATE_ACTIVE
            updateSubtitle(tile, "Active")
        }
        
        tile.updateTile()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val bypassPath = PropertyUtils.get(PATH_PROP, "")
        
        if (bypassPath == "UNSUPPORTED") {
            tile.state = Tile.STATE_UNAVAILABLE
            updateSubtitle(tile, "Not Supported")
        } else {
            val isEnabled = PropertyUtils.get(BYPASS_PROP, "0") == "1"
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateSubtitle(tile, if (isEnabled) "Active" else "Inactive")
        }
        
        tile.updateTile()
    }

    // Helper biar gak nulis Build.VERSION berulang-ulang
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
