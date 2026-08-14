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

    private var moduleFlag = false

    fun captureBaselineOnce(key: String, value: Boolean) = synchronized(lock) {
        baselineValues.putIfAbsent(key, value)
    }

    fun checkAgainstBaseline(key: String, value: Boolean) = synchronized(lock) {
        val base = baselineValues[key] ?: return@synchronized
        if (value != base) markDirtyLocked(key) else clearDirtyLocked(key)
    }

    private fun markDirtyLocked(key: String) {
        dirtyKeys.add(key)
        recomputeLocked()
    }

    private fun clearDirtyLocked(key: String) {
        dirtyKeys.remove(key)
        recomputeLocked()
    }

    private fun recomputeLocked() {
        _pendingReboot.value = dirtyKeys.isNotEmpty() || moduleFlag
    }

    suspend fun refreshModuleFlag() = withContext(Dispatchers.IO) {
        val result = Shell.cmd("test -f /data/adb/modules/AZenith/reboot").exec().isSuccess
        synchronized(lock) {
            moduleFlag = result
            recomputeLocked()
        }
    }

    fun resetAll() = synchronized(lock) {
        dirtyKeys.clear()
        baselineValues.clear()
        recomputeLocked()
    }
    fun wouldRequireReboot(key: String, value: Boolean): Boolean = synchronized(lock) {
        val base = baselineValues[key] ?: return@synchronized false
        value != base
    }
}
