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

package zx.azenith.ui.viewmodel


import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import zx.azenith.R


class ApplistViewmodel : ViewModel() {

    companion object {
        private const val TAG = "ApplistViewmodel"

        // A pull-to-refresh that finishes faster than this is held open until
        // it reaches the floor. Package enumeration is usually quicker than the
        // spinner takes to appear, so without a floor the indicator appears and
        // clears in the same frame and the gesture looks like it did nothing.
        private const val MIN_REFRESH_VISIBLE_MS = 1000L
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

        // Filtering lowercases every label and package on every keystroke, and
        // the labels are already resolved, so keep the folded forms alongside
        // the original rather than reallocating them per search.
        internal val searchLabel: String = label.lowercase(Locale.getDefault())
        internal val searchPackage: String = packageName.lowercase(Locale.getDefault())
    }

    var isRefreshing by mutableStateOf(false)

    enum class SystemFilter { SHOW_ALL, ONLY_SYSTEM, HIDE_SYSTEM }

    // Filters stack: flags are AND-ed across groups, and the two service-state
    // flags are OR-ed with each other (neither or both checked = no service
    // constraint), so Games + Disabled yields exactly the disabled games.
    // HIDE_SYSTEM is the baseline, matching the old show_system_apps=false default.
    var systemFilter by mutableStateOf(SystemFilter.HIDE_SYSTEM)
    var filterGames by mutableStateOf(false)
    var filterEnabled by mutableStateOf(false)
    var filterDisabled by mutableStateOf(false)

    val activeFilterCount: Int
        get() = (if (filterGames) 1 else 0) +
                (if (filterEnabled) 1 else 0) +
                (if (filterDisabled) 1 else 0) +
                (if (systemFilter != SystemFilter.HIDE_SYSTEM) 1 else 0)

    fun resetFilters() {
        filterGames = false
        filterEnabled = false
        filterDisabled = false
        systemFilter = SystemFilter.HIDE_SYSTEM
    }
    
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

    // One predicate for the list and for the per-option counts, so a count can
    // never drift from what selecting that option would actually show.
    private fun matchesFilters(
        app: AppInfo,
        query: String,
        games: Boolean,
        enabled: Boolean,
        disabled: Boolean,
        system: SystemFilter,
    ): Boolean {
        if (!(app.searchLabel.contains(query) || app.searchPackage.contains(query))) return false
        if (games && !app.isRecommended) return false
        val serviceOk = (!enabled && !disabled) ||
            (enabled && app.isEnabledInConfig) ||
            (disabled && !app.isEnabledInConfig)
        if (!serviceOk) return false
        return when (system) {
            SystemFilter.SHOW_ALL -> true
            SystemFilter.ONLY_SYSTEM -> app.isSystem
            SystemFilter.HIDE_SYSTEM -> !app.isSystem
        }
    }

    val filteredApps by derivedStateOf {
        val query = searchQueryString.lowercase(Locale.getDefault())
        synchronized(appsLock) {
            apps.filter {
                matchesFilters(it, query, filterGames, filterEnabled, filterDisabled, systemFilter)
            }.sortedWith(appComparator)
        }
    }

    /** What one option would show if turned on, holding every other group fixed. */
    fun countIf(
        games: Boolean = filterGames,
        enabled: Boolean = filterEnabled,
        disabled: Boolean = filterDisabled,
        system: SystemFilter = systemFilter,
    ): Int {
        val query = searchQueryString.lowercase(Locale.getDefault())
        return synchronized(appsLock) {
            apps.count { matchesFilters(it, query, games, enabled, disabled, system) }
        }
    }

    // Built once: Collator.getInstance() is locale data lookup plus allocation,
    // and it was being constructed inside the comparator, so a sort of a few
    // hundred apps built one per comparison. The same instance is also reused
    // for a stable locale ordering across searches.
    private val appComparator: Comparator<AppInfo> = run {
        val collator = Collator.getInstance(Locale.getDefault())
        compareByDescending<AppInfo> { it.isEnabledInConfig }
            .thenByDescending { it.isRecommended }
            .then(compareBy(collator) { it.label })
    }

    fun loadApps(context: Context, forceRefresh: Boolean = false) {
        if (!forceRefresh && apps.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            val refreshStarted = SystemClock.elapsedRealtime()
            val pm = context.packageManager

            val enabledList = getEnabledPackages()
            val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)

            val loadedApps = installed.map { pkg ->
                val appInfo = pkg.applicationInfo
                

                @Suppress("DEPRECATION")
                val isGame = appInfo != null && (
                    appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                    (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                )

                AppInfo(
                    label = appInfo?.loadLabel(pm)?.toString() ?: context.getString(R.string.status_unknown),
                    packageInfo = pkg,
                    isRecommended = isGame,
                    isEnabledInConfig = enabledList.contains(pkg.packageName)
                )
            }

            withContext(Dispatchers.Main) {
                synchronized(appsLock) {
                    apps = loadedApps
                }
                // Hold the spinner for a beat even when the rebuild was faster
                // than that. A pull-to-refresh that dismisses in the same frame
                // it appears reads as a dropped gesture rather than a refresh,
                // so the result is given a minimum on-screen time.
                val elapsed = SystemClock.elapsedRealtime() - refreshStarted
                val remaining = MIN_REFRESH_VISIBLE_MS - elapsed
                if (remaining > 0) delay(remaining)
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
