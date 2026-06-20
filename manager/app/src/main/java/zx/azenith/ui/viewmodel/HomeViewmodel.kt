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

package zx.azenith.ui.viewmodel


import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zx.azenith.R
import zx.azenith.ui.util.RootUtils
import zx.azenith.ui.util.isBannerImageEnabled


data class HomeUiState(
    val isBannerEnabled: Boolean = false,
    val moduleInstalled: Boolean = false,
    val autoMode: String? = null,
    val rootStatus: Boolean = false,
    val serviceStatusRes: Int = R.string.status_suspended,
    val servicePid: String = "",
    val currentProfileRes: Int = R.string.status_initializing,
    val runningGamePkg: String? = null,
    val runningGameStartTime: String? = null
)


class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enable_banner_image") {
            _uiState.value = _uiState.value.copy(isBannerEnabled = context.isBannerImageEnabled())
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        _uiState.value = _uiState.value.copy(isBannerEnabled = context.isBannerImageEnabled())
        
        observeRootUtils()
        fetchInitialSystemData()
    }

    private fun observeRootUtils() {
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeServiceStatusRes().collect { (statusRes, pid) ->
                _uiState.value = _uiState.value.copy(serviceStatusRes = statusRes, servicePid = pid)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeProfileRes().collect { profileRes ->
                _uiState.value = _uiState.value.copy(currentProfileRes = profileRes)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            RootUtils.observeGameInfo().collect { info ->
                _uiState.value = _uiState.value.copy(
                    runningGamePkg = info.pkg,
                    runningGameStartTime = info.startTime
                )
            }
        }
    }


    private fun fetchInitialSystemData() {
        viewModelScope.launch(Dispatchers.IO) {
            val isRooted = RootUtils.requestRootAccess()
            val isModuleInstalled = RootUtils.isModuleInstalled()
            val mode = Shell.cmd("getprop persist.sys.azenithconf.AIenabled").exec().out.firstOrNull()?.trim()

            _uiState.value = _uiState.value.copy(
                rootStatus = isRooted,
                moduleInstalled = isModuleInstalled,
                autoMode = mode
            )
        }
    }

    fun applyProfile(profileReason: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            Shell.cmd("/data/adb/modules/AZenith/system/bin/sys.azenith-service -p $profileReason").submit()
            viewModelScope.launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun rebootDevice(reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cmd = when (reason) {
                "" -> "svc power reboot"
                "soft_reboot" -> "killall system_server"
                "recovery" -> "/system/bin/input keyevent 26 && svc power reboot $reason || reboot $reason"
                else -> "svc power reboot $reason || reboot $reason"
            }
            Shell.cmd(cmd).submit()
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
    


    fun refreshAiMode() {
        viewModelScope.launch(Dispatchers.IO) {
            val mode = Shell.cmd("getprop persist.sys.azenithconf.AIenabled").exec().out.firstOrNull()?.trim()
            _uiState.value = _uiState.value.copy(autoMode = mode)
        }
    }

}
