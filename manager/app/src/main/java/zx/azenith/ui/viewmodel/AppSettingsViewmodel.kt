package zx.azenith.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import zx.azenith.ui.util.AppConfig
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class AppSettingsViewModel : ViewModel() {
    private val configPath = "/data/adb/.config/AZenith/gamelist/azenithApplist.json"
    private val jsonHandler = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    var fullConfig by mutableStateOf<Map<String, AppConfig>>(emptyMap())
        private set

    fun loadConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = SuFile(configPath)
            if (file.exists()) {
                try {
                    val content = SuFileInputStream.open(file).bufferedReader().use { it.readText() }
                    if (content.isNotEmpty()) {
                        val decoded = jsonHandler.decodeFromString<Map<String, AppConfig>>(content)
                        withContext(Dispatchers.Main) { fullConfig = decoded }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveAndRefresh(newMap: Map<String, AppConfig>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = SuFile(configPath)
                val parent = file.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }

                val jsonString = jsonHandler.encodeToString(newMap)
                
                SuFileOutputStream.open(file).use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                withContext(Dispatchers.Main) { fullConfig = newMap }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleMasterSwitch(packageName: String, isEnabled: Boolean) {
        val newMap = fullConfig.toMutableMap()
        if (isEnabled) {
            if (!newMap.containsKey(packageName)) {
                newMap[packageName] = AppConfig()
            }
        } else {
            newMap.remove(packageName)
        }
        saveAndRefresh(newMap)
    }

    fun updateSetting(packageName: String, key: String, value: String) {
        val currentAppConfig = fullConfig[packageName] ?: AppConfig()
        val updated = when (key) {
            "perf_lite_mode" -> currentAppConfig.copy(perf_lite_mode = value)
            "dnd_on_gaming" -> currentAppConfig.copy(dnd_on_gaming = value)
            "app_priority" -> currentAppConfig.copy(app_priority = value)
            "game_preload" -> currentAppConfig.copy(game_preload = value)
            "refresh_rate" -> currentAppConfig.copy(refresh_rate = value)
            "renderer" -> currentAppConfig.copy(renderer = value)
            else -> currentAppConfig
        }
        
        val newMap = fullConfig.toMutableMap()
        newMap[packageName] = updated
        saveAndRefresh(newMap)
    }
}
