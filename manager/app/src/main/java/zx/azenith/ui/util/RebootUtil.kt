package zx.azenith.ui.util

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object RebootManager {
    private val dirtyKeys = mutableSetOf<String>()
    private val baselineValues = mutableMapOf<String, Boolean>()
    private val _pendingReboot = MutableStateFlow(false)
    val pendingReboot: StateFlow<Boolean> = _pendingReboot.asStateFlow()

    private var moduleFlag = false

    @Synchronized
    fun captureBaselineOnce(key: String, value: Boolean) {
        baselineValues.putIfAbsent(key, value)
    }

    @Synchronized
    fun checkAgainstBaseline(key: String, value: Boolean) {
        val base = baselineValues[key] ?: return
        if (value != base) markDirty(key) else clearDirty(key)
    }

    @Synchronized
    private fun markDirty(key: String) {
        dirtyKeys.add(key)
        recompute()
    }

    @Synchronized
    private fun clearDirty(key: String) {
        dirtyKeys.remove(key)
        recompute()
    }

    private fun recompute() {
        _pendingReboot.value = dirtyKeys.isNotEmpty() || moduleFlag
    }

    suspend fun refreshModuleFlag() = withContext(Dispatchers.IO) {
        moduleFlag = Shell.cmd("test -f /data/adb/modules/AZenith/reboot").exec().isSuccess
        recompute()
    }

    @Synchronized
    fun resetAll() {
        dirtyKeys.clear()
        baselineValues.clear()
        recompute()
    }
}