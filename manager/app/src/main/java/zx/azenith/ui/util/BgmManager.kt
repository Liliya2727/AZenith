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

package zx.azenith.ui.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import zx.azenith.R
import java.io.File
import java.util.UUID

private const val TAG = "BgmManager"
private const val PREFS = "settings"
const val PREF_BGM_ENABLED = "gamespace_bgm_enabled"
const val PREF_BGM_VOLUME = "gamespace_bgm_volume"
const val PREF_BGM_PLAYLIST = "gamespace_bgm_playlist"
const val PREF_BGM_ACTIVE_ID = "gamespace_bgm_active_id"

data class BgmTrack(
    val id: String,
    val title: String,
    val fileName: String,
    val relativePath: String? = null,
    val isDefault: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("fileName", fileName)
        put("relativePath", relativePath ?: "")
        put("isDefault", isDefault)
    }

    companion object {
        fun fromJson(obj: JSONObject): BgmTrack = BgmTrack(
            id = obj.getString("id"),
            title = obj.getString("title"),
            fileName = obj.optString("fileName", "custom.ogg"),
            relativePath = obj.optString("relativePath").takeIf { it.isNotBlank() },
            isDefault = obj.optBoolean("isDefault", false)
        )

        val DEFAULT = BgmTrack(
            id = "default",
            title = "AZenith Theme",
            fileName = "bgm.ogg",
            relativePath = null,
            isDefault = true
        )
    }
}

object BgmManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    var isGameHubActive: Boolean = false

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _volume = MutableStateFlow(70)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _playlist = MutableStateFlow<List<BgmTrack>>(listOf(BgmTrack.DEFAULT))
    val playlist: StateFlow<List<BgmTrack>> = _playlist.asStateFlow()

    private val _currentTrack = MutableStateFlow<BgmTrack?>(BgmTrack.DEFAULT)
    val currentTrack: StateFlow<BgmTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val enabled = prefs.getBoolean(PREF_BGM_ENABLED, false)
        val vol = prefs.getInt(PREF_BGM_VOLUME, 70).coerceIn(0, 100)
        val activeId = prefs.getString(PREF_BGM_ACTIVE_ID, BgmTrack.DEFAULT.id) ?: BgmTrack.DEFAULT.id

        val loadedPlaylist = loadPlaylistFromPrefs(prefs)
        val selected = loadedPlaylist.firstOrNull { it.id == activeId } ?: loadedPlaylist.firstOrNull() ?: BgmTrack.DEFAULT

        _isEnabled.value = enabled
        _volume.value = vol
        _playlist.value = loadedPlaylist
        _currentTrack.value = selected
        isInitialized = true

        if (enabled && isGameHubActive) {
            playCurrentTrack(appContext)
        }
    }

    private fun loadPlaylistFromPrefs(prefs: SharedPreferences): List<BgmTrack> {
        val rawJson = prefs.getString(PREF_BGM_PLAYLIST, null)
        if (rawJson.isNullOrBlank()) {
            return listOf(BgmTrack.DEFAULT)
        }
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<BgmTrack>()
            for (i in 0 until array.length()) {
                list.add(BgmTrack.fromJson(array.getJSONObject(i)))
            }
            if (list.isEmpty()) listOf(BgmTrack.DEFAULT) else list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing playlist JSON", e)
            listOf(BgmTrack.DEFAULT)
        }
    }

    private fun savePlaylistToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray()
        _playlist.value.forEach { array.put(it.toJson()) }
        prefs.edit()
            .putString(PREF_BGM_PLAYLIST, array.toString())
            .putString(PREF_BGM_ACTIVE_ID, _currentTrack.value?.id ?: BgmTrack.DEFAULT.id)
            .apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        _isEnabled.value = enabled
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_BGM_ENABLED, enabled).apply()

        if (enabled) {
            if (isGameHubActive) {
                playCurrentTrack(appContext)
            }
        } else {
            stopPlayback()
        }
    }

    fun setVolume(context: Context, volPercent: Int) {
        val clamped = volPercent.coerceIn(0, 100)
        _volume.value = clamped
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(PREF_BGM_VOLUME, clamped).apply()

        mediaPlayer?.let { player ->
            try {
                val volFactor = clamped / 100f
                player.setVolume(volFactor, volFactor)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set volume", e)
            }
        }
    }

    fun selectTrack(context: Context, track: BgmTrack) {
        val appContext = context.applicationContext
        _currentTrack.value = track
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(PREF_BGM_ACTIVE_ID, track.id).apply()

        if (_isEnabled.value && isGameHubActive) {
            playCurrentTrack(appContext)
        }
    }

    fun playNext(context: Context) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentTrack.value?.id }
        val nextIndex = if (currentIndex in list.indices) {
            (currentIndex + 1) % list.size
        } else {
            0
        }
        selectTrack(context, list[nextIndex])
    }

    fun playPrevious(context: Context) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentTrack.value?.id }
        val prevIndex = if (currentIndex in list.indices) {
            (currentIndex - 1 + list.size) % list.size
        } else {
            0
        }
        selectTrack(context, list[prevIndex])
    }

    @Synchronized
    private fun playCurrentTrack(context: Context) {
        val track = _currentTrack.value ?: BgmTrack.DEFAULT
        stopPlayback()

        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            if (track.isDefault) {
                val afd = context.resources.openRawResourceFd(R.raw.bgm)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            } else {
                val file = File(context.filesDir, "bgm_tracks/${track.relativePath}")
                if (!file.exists()) {
                    Log.w(TAG, "Track file does not exist: ${file.absolutePath}, falling back to default")
                    selectTrack(context, BgmTrack.DEFAULT)
                    return
                }
                player.setDataSource(file.absolutePath)
            }

            val volFactor = _volume.value / 100f
            player.setVolume(volFactor, volFactor)

            val currentPlaylist = _playlist.value
            if (currentPlaylist.size <= 1) {
                player.isLooping = true
            } else {
                player.isLooping = false
                player.setOnCompletionListener {
                    playNext(context)
                }
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                playNext(context)
                true
            }

            player.prepare()
            player.start()
            mediaPlayer = player
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BGM track: ${track.title}", e)
            _isPlaying.value = false
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
        }
    }

    fun onActivityResume(context: Context) {
        isGameHubActive = true
        if (_isEnabled.value) {
            mediaPlayer?.let { player ->
                try {
                    if (!player.isPlaying) {
                        player.start()
                        _isPlaying.value = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error resuming player", e)
                    playCurrentTrack(context.applicationContext)
                }
            } ?: playCurrentTrack(context.applicationContext)
        }
    }

    fun onActivityPause() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing player", e)
        } finally {
            _isPlaying.value = false
        }
    }

    fun onDestroy() {
        isGameHubActive = false
        stopPlayback()
    }

    suspend fun addTracksFromUris(context: Context, uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val tracksDir = File(appContext.filesDir, "bgm_tracks").apply { if (!exists()) mkdirs() }
        val newTracks = mutableListOf<BgmTrack>()

        for (uri in uris) {
            try {
                val displayName = queryDisplayName(appContext, uri) ?: "custom_${System.currentTimeMillis()}.ogg"
                val ext = displayName.substringAfterLast('.', "ogg").lowercase()
                val rawTitle = displayName.substringBeforeLast('.')
                val id = UUID.randomUUID().toString()
                val destFileName = "$id.$ext"
                val destFile = File(tracksDir, destFileName)

                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    newTracks.add(
                        BgmTrack(
                            id = id,
                            title = rawTitle.replace('_', ' ').replace('-', ' ').trim().ifEmpty { "Track" },
                            fileName = displayName,
                            relativePath = destFileName,
                            isDefault = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import BGM URI: $uri", e)
            }
        }

        if (newTracks.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                val updated = _playlist.value + newTracks
                _playlist.value = updated
                savePlaylistToPrefs(appContext)

                if (_currentTrack.value == null || _currentTrack.value == BgmTrack.DEFAULT && updated.size == 2) {
                    selectTrack(appContext, newTracks.first())
                } else if (_isEnabled.value && isGameHubActive && mediaPlayer?.isLooping == true && updated.size > 1) {
                    mediaPlayer?.isLooping = false
                    mediaPlayer?.setOnCompletionListener { playNext(appContext) }
                }
            }
        }
        newTracks.size
    }

    fun removeTrack(context: Context, trackId: String) {
        val appContext = context.applicationContext
        val currentList = _playlist.value
        val trackToRemove = currentList.firstOrNull { it.id == trackId } ?: return

        if (!trackToRemove.isDefault && trackToRemove.relativePath != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val file = File(appContext.filesDir, "bgm_tracks/${trackToRemove.relativePath}")
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete track file", e)
                }
            }
        }

        var updated = currentList.filterNot { it.id == trackId }
        if (updated.isEmpty()) {
            updated = listOf(BgmTrack.DEFAULT)
        }
        _playlist.value = updated

        if (_currentTrack.value?.id == trackId) {
            val fallback = updated.first()
            selectTrack(appContext, fallback)
        } else {
            savePlaylistToPrefs(appContext)
            if (updated.size <= 1) {
                mediaPlayer?.isLooping = true
            }
        }
    }

    fun restoreDefaultTrack(context: Context) {
        val currentList = _playlist.value
        if (currentList.none { it.id == BgmTrack.DEFAULT.id }) {
            val updated = listOf(BgmTrack.DEFAULT) + currentList
            _playlist.value = updated
            savePlaylistToPrefs(context.applicationContext)
        }
    }

    fun resetPlaylistToDefault(context: Context) {
        val appContext = context.applicationContext
        val tracksDir = File(appContext.filesDir, "bgm_tracks")
        scope.launch(Dispatchers.IO) {
            try {
                tracksDir.deleteRecursively()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear tracks directory", e)
            }
        }
        _playlist.value = listOf(BgmTrack.DEFAULT)
        selectTrack(appContext, BgmTrack.DEFAULT)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (colIndex != -1) {
                            result = cursor.getString(colIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query display name", e)
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }
}
