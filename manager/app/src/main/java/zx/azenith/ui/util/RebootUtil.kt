package zx.azenith.ui.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object RebootManager {
    private val lock = Any()
    private val dirtyKeys = mutableSetOf<String>()
    private val baselineValues = mutableMapOf<String, Boolean>()
    private val _pendingReboot = MutableStateFlow(false)
    val pendingReboot: StateFlow<Boolean> = _pendingReboot.asStateFlow()

    private const val FLAG_PATH = "/data/adb/modules/AZenith/reboot"
    private var moduleFlag = false

    fun captureBaselineOnce(key: String, value: Boolean) = synchronized(lock) {
        baselineValues.putIfAbsent(key, value)
    }

    fun checkAgainstBaseline(key: String, value: Boolean) = synchronized(lock) {
        val base = baselineValues[key] ?: return@synchronized
        if (value != base) markDirtyLocked(key) else clearDirtyLocked(key)
    }

    fun wouldRequireReboot(key: String, value: Boolean): Boolean = synchronized(lock) {
        val base = baselineValues[key] ?: return@synchronized false
        value != base
    }

    private fun markDirtyLocked(key: String) {
        dirtyKeys.add(key)
        persistFlagLocked()
        recomputeLocked()
    }

    private fun clearDirtyLocked(key: String) {
        dirtyKeys.remove(key)
        persistFlagLocked()
        recomputeLocked()
    }

    private fun persistFlagLocked() {
        Shell.cmd(
            if (dirtyKeys.isNotEmpty()) "touch $FLAG_PATH" else "rm -f $FLAG_PATH"
        ).submit()
    }

    private fun recomputeLocked() {
        _pendingReboot.value = dirtyKeys.isNotEmpty() || moduleFlag
    }

    suspend fun refreshModuleFlag() = withContext(Dispatchers.IO) {
        val result = Shell.cmd("test -f $FLAG_PATH").exec().isSuccess
        synchronized(lock) {
            moduleFlag = result
            recomputeLocked()
        }
    }

    fun resetAll() = synchronized(lock) {
        dirtyKeys.clear()
        baselineValues.clear()
        Shell.cmd("rm -f $FLAG_PATH").submit()
        recomputeLocked()
    }
}
