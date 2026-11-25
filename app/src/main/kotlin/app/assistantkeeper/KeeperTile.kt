package app.assistantkeeper

import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class KeeperTile : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            subtitle = null
            updateTile()
        }
    }

    override fun onClick() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            subtitle = "..."
            updateTile()
        }
        val ctx = applicationContext
        Thread {
            val res = Keeper.restore(ctx)
            Handler(Looper.getMainLooper()).post {
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    subtitle = getString(
                        when (res) {
                            Result.OK -> R.string.tile_ok
                            Result.RESTORED -> R.string.tile_restored
                            Result.NO_ROOT -> R.string.tile_no_root
                            Result.NO_TARGET -> R.string.tile_no_target
                        }
                    )
                    updateTile()
                }
            }
        }.start()
    }
}
