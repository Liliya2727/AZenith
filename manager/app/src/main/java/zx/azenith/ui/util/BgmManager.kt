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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
const val PREF_BGM_NOW_PLAYING = "gamespace_bgm_now_playing"
const val PREF_BGM_REPEAT_MODE = "gamespace_bgm_repeat_mode"
const val PREF_BGM_STAGED = "gamespace_bgm_staged"
const val PREF_BGM_SHUFFLE = "gamespace_bgm_shuffle"

enum class BgmRepeatMode {
    ALL,
    ONE
}

data class BgmTrack(
    val id: String,
    val title: String,
    val artist: String? = null,
    val durationMs: Long = 0L,
    val fileName: String,
    val relativePath: String? = null,
    val isDefault: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0L) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }

    val displayArtist: String
        get() = if (isDefault) "Zenith" else (artist?.takeIf { it.isNotBlank() } ?: fileName)


    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist ?: "")
        put("durationMs", durationMs)
        put("fileName", fileName)
        put("relativePath", relativePath ?: "")
        put("isDefault", isDefault)
    }

    companion object {
        fun fromJson(obj: JSONObject): BgmTrack = BgmTrack(
            id = obj.getString("id"),
            title = obj.getString("title"),
            artist = obj.optString("artist").takeIf { it.isNotBlank() },
            durationMs = obj.optLong("durationMs", 0L),
            fileName = obj.optString("fileName", "custom.ogg"),
            relativePath = obj.optString("relativePath").takeIf { it.isNotBlank() },
            isDefault = obj.optBoolean("isDefault", false)
        )

        val DEFAULT = BgmTrack(
            id = "default",
            title = "AZenith Theme",
            artist = null,
            durationMs = 134374L,
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

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _isNowPlayingEnabled = MutableStateFlow(true)
    val isNowPlayingEnabled: StateFlow<Boolean> = _isNowPlayingEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(BgmRepeatMode.ALL)
    val repeatMode: StateFlow<BgmRepeatMode> = _repeatMode.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _songChangeNonce = MutableStateFlow(0L)
    val songChangeNonce: StateFlow<Long> = _songChangeNonce.asStateFlow()

    private val _currentAlbumArt = MutableStateFlow<Bitmap?>(null)
    val currentAlbumArt: StateFlow<Bitmap?> = _currentAlbumArt.asStateFlow()

    private val albumArtCache = mutableMapOf<String, Bitmap?>()
    private var positionJob: Job? = null

    private val _playlist = MutableStateFlow<List<BgmTrack>>(listOf(BgmTrack.DEFAULT))
    val playlist: StateFlow<List<BgmTrack>> = _playlist.asStateFlow()

    private val _currentTrack = MutableStateFlow<BgmTrack?>(BgmTrack.DEFAULT)
    val currentTrack: StateFlow<BgmTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _stagedTracks = MutableStateFlow<List<BgmTrack>>(emptyList())
    val stagedTracks: StateFlow<List<BgmTrack>> = _stagedTracks.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false

    fun formatTimeMs(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    suspend fun loadAlbumArt(context: Context, track: BgmTrack): Bitmap? {
        if (track.isDefault || track.relativePath == null) return null
        synchronized(albumArtCache) {
            if (albumArtCache.containsKey(track.id)) {
                return albumArtCache[track.id]
            }
        }
        return withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "bgm_tracks/${track.relativePath}")
            if (!file.exists()) {
                synchronized(albumArtCache) { albumArtCache[track.id] = null }
                return@withContext null
            }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val pic = retriever.embeddedPicture
                val bmp = if (pic != null) {
                    BitmapFactory.decodeByteArray(pic, 0, pic.size)
                } else null
                synchronized(albumArtCache) { albumArtCache[track.id] = bmp }
                bmp
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load album art for ${track.title}", e)
                synchronized(albumArtCache) { albumArtCache[track.id] = null }
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }
    }

    private fun updateAlbumArtForTrack(context: Context, track: BgmTrack) {
        scope.launch {
            val bmp = loadAlbumArt(context, track)
            _currentAlbumArt.value = bmp
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (_isPlaying.value) {
                val player = mediaPlayer
                if (player != null) {
                    try {
                        _playbackPositionMs.value = player.currentPosition.toLong()
                    } catch (_: Exception) {}
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionTracker() {
        positionJob?.cancel()
        positionJob = null
    }

    fun setNowPlayingEnabled(context: Context, enabled: Boolean) {
        _isNowPlayingEnabled.value = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_BGM_NOW_PLAYING, enabled).apply()
    }

    fun toggleRepeatMode(context: Context) {
        val newMode = if (_repeatMode.value == BgmRepeatMode.ALL) BgmRepeatMode.ONE else BgmRepeatMode.ALL
        _repeatMode.value = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(PREF_BGM_REPEAT_MODE, newMode.name.lowercase()).apply()

        mediaPlayer?.let { player ->
            try {
                if (newMode == BgmRepeatMode.ONE) {
                    player.isLooping = true
                } else {
                    player.isLooping = _playlist.value.size <= 1
                    player.setOnCompletionListener { playNext(context) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle repeat mode", e)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                _playbackPositionMs.value = positionMs
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seek to $positionMs", e)
            }
        }
    }

    fun togglePlayPause(context: Context) {
        val player = mediaPlayer
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopPositionTracker()
            } else {
                player.start()
                _isPlaying.value = true
                startPositionTracker()
            }
        } else if (_isEnabled.value) {
            playCurrentTrack(context.applicationContext)
        }
    }

    fun playPreviousOrRestart(context: Context) {
        if (_playbackPositionMs.value > 3000L) {
            seekTo(0L)
        } else {
            playPrevious(context)
        }
    }

    data class AudioMetadata(
        val title: String? = null,
        val artist: String? = null,
        val durationMs: Long = 0L
    )

    fun extractMetadata(file: File): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            AudioMetadata(
                title = title?.takeIf { it.isNotBlank() },
                artist = artist?.takeIf { it.isNotBlank() },
                durationMs = durationMs
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract metadata from ${file.name}", e)
            AudioMetadata()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val enabled = prefs.getBoolean(PREF_BGM_ENABLED, true)
        val vol = if (prefs.contains(PREF_BGM_VOLUME)) {
            val saved = prefs.getInt(PREF_BGM_VOLUME, 25)
            if (saved == 70) 25 else saved.coerceIn(0, 100)
        } else {
            25
        }
        val activeId = prefs.getString(PREF_BGM_ACTIVE_ID, BgmTrack.DEFAULT.id) ?: BgmTrack.DEFAULT.id
        val nowPlayingEnabled = prefs.getBoolean(PREF_BGM_NOW_PLAYING, true)
        val repeatModeStr = prefs.getString(PREF_BGM_REPEAT_MODE, "all")
        val rMode = if (repeatModeStr == "one") BgmRepeatMode.ONE else BgmRepeatMode.ALL
        val shuffleEnabled = prefs.getBoolean(PREF_BGM_SHUFFLE, false)

        val loadedStaged = loadStagedFromPrefs(appContext, prefs)
        val loadedPlaylist = loadPlaylistFromPrefs(appContext, prefs)
        val finalPlaylist = if (loadedPlaylist.isEmpty() && loadedStaged.isEmpty()) {
            listOf(BgmTrack.DEFAULT)
        } else {
            loadedPlaylist
        }
        val selected = finalPlaylist.firstOrNull { it.id == activeId } ?: finalPlaylist.firstOrNull()

        _isEnabled.value = enabled
        _volume.value = vol
        _isNowPlayingEnabled.value = nowPlayingEnabled
        _repeatMode.value = rMode
        _isShuffleEnabled.value = shuffleEnabled
        _playlist.value = finalPlaylist
        _stagedTracks.value = loadedStaged
        _currentTrack.value = selected
        _songChangeNonce.value = 1L
        isInitialized = true

        if (selected != null) {
            updateAlbumArtForTrack(appContext, selected)
            if (enabled && isGameHubActive) {
                playCurrentTrack(appContext)
            }
        }
    }

    private fun loadPlaylistFromPrefs(context: Context, prefs: SharedPreferences): List<BgmTrack> {
        val rawJson = prefs.getString(PREF_BGM_PLAYLIST, null)
        val list = if (rawJson == null) {
            listOf(BgmTrack.DEFAULT)
        } else if (rawJson.isBlank()) {
            emptyList()
        } else {
            try {
                val array = JSONArray(rawJson)
                val tracks = mutableListOf<BgmTrack>()
                for (i in 0 until array.length()) {
                    tracks.add(BgmTrack.fromJson(array.getJSONObject(i)))
                }
                tracks
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing playlist JSON", e)
                listOf(BgmTrack.DEFAULT)
            }
        }

        var needsResave = false
        val enriched = list.map { track ->
            if (track.isDefault) {
                if (track.durationMs <= 0L) {
                    needsResave = true
                    track.copy(durationMs = 134374L)
                } else track
            } else if (track.relativePath != null) {
                val file = File(context.filesDir, "bgm_tracks/${track.relativePath}")
                if (file.exists() && (track.durationMs <= 0L || track.artist == null)) {
                    val meta = extractMetadata(file)
                    needsResave = true
                    track.copy(
                        title = meta.title ?: track.title,
                        artist = meta.artist ?: track.artist,
                        durationMs = if (meta.durationMs > 0) meta.durationMs else track.durationMs
                    )
                } else track
            } else track
        }

        if (needsResave) {
            val array = JSONArray()
            enriched.forEach { array.put(it.toJson()) }
            prefs.edit().putString(PREF_BGM_PLAYLIST, array.toString()).apply()
        }
        return enriched
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

    private fun loadStagedFromPrefs(context: Context, prefs: SharedPreferences): List<BgmTrack> {
        val rawJson = prefs.getString(PREF_BGM_STAGED, null)
        if (rawJson.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(rawJson)
            val tracks = mutableListOf<BgmTrack>()
            for (i in 0 until array.length()) {
                tracks.add(BgmTrack.fromJson(array.getJSONObject(i)))
            }
            tracks
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing staged JSON", e)
            emptyList()
        }
    }

    private fun saveStagedToPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray()
        _stagedTracks.value.forEach { array.put(it.toJson()) }
        prefs.edit().putString(PREF_BGM_STAGED, array.toString()).apply()
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
        _playbackPositionMs.value = 0L
        _songChangeNonce.value += 1L
        updateAlbumArtForTrack(appContext, track)
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(PREF_BGM_ACTIVE_ID, track.id).apply()

        if (_isEnabled.value && isGameHubActive) {
            playCurrentTrack(appContext)
        }
    }

    fun toggleShuffle(context: Context) {
        val newValue = !_isShuffleEnabled.value
        _isShuffleEnabled.value = newValue
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_BGM_SHUFFLE, newValue).apply()
    }

    fun playNext(context: Context) {
        val list = _playlist.value
        if (list.isEmpty()) return
        if (_isShuffleEnabled.value && list.size > 1) {
            val currentId = _currentTrack.value?.id
            val candidates = list.filter { it.id != currentId }
            val pick = candidates.random()
            selectTrack(context, pick)
        } else {
            val currentIndex = list.indexOfFirst { it.id == _currentTrack.value?.id }
            val nextIndex = if (currentIndex in list.indices) {
                (currentIndex + 1) % list.size
            } else {
                0
            }
            selectTrack(context, list[nextIndex])
        }
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
            val isOne = _repeatMode.value == BgmRepeatMode.ONE
            if (isOne || currentPlaylist.size <= 1) {
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
            startPositionTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BGM track: ${track.title}", e)
            _isPlaying.value = false
        }
    }

    private fun stopPlayback() {
        stopPositionTracker()
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
            _playbackPositionMs.value = 0L
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
                        startPositionTracker()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error resuming player", e)
                    playCurrentTrack(context.applicationContext)
                }
            } ?: playCurrentTrack(context.applicationContext)
        }
    }

    fun onActivityPause() {
        stopPositionTracker()
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
                    val meta = extractMetadata(destFile)
                    val resolvedTitle = meta.title
                        ?: rawTitle.replace('_', ' ').replace('-', ' ').trim().ifEmpty { "Track" }
                    newTracks.add(
                        BgmTrack(
                            id = id,
                            title = resolvedTitle,
                            artist = meta.artist,
                            durationMs = meta.durationMs,
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

        val updated = currentList.filterNot { it.id == trackId }
        _playlist.value = updated

        // Move to staging area (including default track)
        _stagedTracks.value = listOf(trackToRemove) + _stagedTracks.value.filterNot { it.id == trackId }
        saveStagedToPrefs(appContext)

        if (_currentTrack.value?.id == trackId) {
            val fallback = updated.firstOrNull()
            if (fallback != null) {
                selectTrack(appContext, fallback)
            } else {
                _currentTrack.value = null
                stopPlayback()
                savePlaylistToPrefs(appContext)
            }
        } else {
            savePlaylistToPrefs(appContext)
            if (updated.size <= 1 && _repeatMode.value != BgmRepeatMode.ONE) {
                mediaPlayer?.isLooping = true
            }
        }
    }

    fun restoreFromStaging(context: Context, trackId: String) {
        val appContext = context.applicationContext
        val staged = _stagedTracks.value
        val trackToRestore = staged.firstOrNull { it.id == trackId } ?: return

        if (!trackToRestore.isDefault && trackToRestore.relativePath != null) {
            val file = File(appContext.filesDir, "bgm_tracks/${trackToRestore.relativePath}")
            if (!file.exists()) {
                _stagedTracks.value = staged.filterNot { it.id == trackId }
                saveStagedToPrefs(appContext)
                return
            }
        }

        _stagedTracks.value = staged.filterNot { it.id == trackId }
        val updated = _playlist.value + trackToRestore
        _playlist.value = updated
        saveStagedToPrefs(appContext)
        savePlaylistToPrefs(appContext)

        if (_currentTrack.value == null) {
            selectTrack(appContext, trackToRestore)
        } else if (updated.size > 1) {
            mediaPlayer?.isLooping = _repeatMode.value == BgmRepeatMode.ONE
            if (_repeatMode.value != BgmRepeatMode.ONE) {
                mediaPlayer?.setOnCompletionListener { playNext(appContext) }
            }
        }
    }

    fun deletePermanently(context: Context, trackId: String) {
        val appContext = context.applicationContext
        val staged = _stagedTracks.value
        val track = staged.firstOrNull { it.id == trackId } ?: return

        // Default track cannot be permanently deleted
        if (track.isDefault) return

        if (track.relativePath != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val file = File(appContext.filesDir, "bgm_tracks/${track.relativePath}")
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete staged track file", e)
                }
            }
        }

        synchronized(albumArtCache) {
            albumArtCache.remove(trackId)
        }

        _stagedTracks.value = staged.filterNot { it.id == trackId }
        saveStagedToPrefs(appContext)
    }

    fun moveTrack(context: Context, fromIndex: Int, toIndex: Int) {
        val list = _playlist.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _playlist.value = list
        savePlaylistToPrefs(context.applicationContext)
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
        synchronized(albumArtCache) {
            albumArtCache.clear()
        }
        _currentAlbumArt.value = null
        _stagedTracks.value = emptyList()
        saveStagedToPrefs(appContext)
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
