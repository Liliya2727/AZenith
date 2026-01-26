package zx.azenith.ui.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Parcelable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import zx.azenith.ui.util.AppConfig
import java.text.Collator
import java.util.Locale
import androidx.compose.ui.text.input.TextFieldValue

class ApplistViewmodel : ViewModel() {

    companion object {
        private const val TAG = "ApplistViewmodel"
        private val appsLock = Any()
        var apps by mutableStateOf<List<AppInfo>>(emptyList())

        @JvmStatic
        fun getAppIconDrawable(context: Context, packageName: String): Drawable? {
            val appList = synchronized(appsLock) { apps }
            val appDetail = appList.find { it.packageName == packageName }
            return appDetail?.packageInfo?.applicationInfo?.loadIcon(context.packageManager)
        }
    }

    @Parcelize
    data class AppInfo(
        val label: String,
        val packageInfo: PackageInfo,
        val isRecommended: Boolean = false,
        var isEnabledInConfig: Boolean = false
    ) : Parcelable {
        val packageName: String get() = packageInfo.packageName
        val isSystem: Boolean get() = (packageInfo.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0)
        val uid: Int get() = packageInfo.applicationInfo?.uid ?: 0
    }

    var isRefreshing by mutableStateOf(false)
    var showSystemApps by mutableStateOf(false)
    
    var searchTextFieldValue by mutableStateOf(TextFieldValue(""))
        private set
    
    private val searchQueryString: String get() = searchTextFieldValue.text
    
    val searchQuery: String get() = searchTextFieldValue.text
    
    fun updateSearch(newValue: TextFieldValue) {
        searchTextFieldValue = newValue
    }
    
    fun clearSearch() {
        searchTextFieldValue = TextFieldValue("")
    }

    private val configPath = "/data/adb/.config/AZenith/gamelist/azenithApplist.json"

    val filteredApps by derivedStateOf {
        val query = searchQueryString.lowercase()
        synchronized(appsLock) {
            apps.filter { app ->
                val matchesSearch = app.label.lowercase().contains(query) || 
                                  app.packageName.lowercase().contains(query)
                val matchesSystem = showSystemApps || !app.isSystem
                matchesSearch && matchesSystem
            }.sortedWith(
                compareByDescending<AppInfo> { it.isEnabledInConfig }
                    .thenByDescending { it.isRecommended }
                    .thenBy(Collator.getInstance(Locale.getDefault())) { it.label }
            )
        }
    }

    fun loadApps(context: Context, forceRefresh: Boolean = false) {
        if (!forceRefresh && apps.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            val pm = context.packageManager

            val gameList = try {
                context.assets.open("gamelist.txt").bufferedReader().useLines { it.toSet() }
            } catch (e: Exception) { emptySet() }

            val enabledList = getEnabledPackages()

            val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            val loadedApps = installed.map { pkg ->
                AppInfo(
                    label = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown",
                    packageInfo = pkg,
                    isRecommended = gameList.contains(pkg.packageName),
                    isEnabledInConfig = enabledList.contains(pkg.packageName)
                )
            }

            withContext(Dispatchers.Main) {
                synchronized(appsLock) {
                    apps = loadedApps
                }
                isRefreshing = false
            }
        }
    }

    fun refreshAppConfigStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabledList = getEnabledPackages()
            synchronized(appsLock) {
                apps = apps.map { it.copy(isEnabledInConfig = enabledList.contains(it.packageName)) }
            }
        }
    }

    private fun getEnabledPackages(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val file = SuFile(configPath)
            if (file.exists()) {
                val content = SuFileInputStream.open(file).bufferedReader().use { it.readText() }
                if (content.isNotBlank()) {
                    val json = JSONObject(content)
                    json.keys().forEach { set.add(it) }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return set
    }
}
