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

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Robust standalone FLAC audio player using MediaExtractor, MediaCodec, and AudioTrack.
 * Handles high-resolution/24-bit FLAC streams and older Stagefright limitations where
 * standard MediaPlayer.prepare() fails.
 */
class FlacAudioTrackPlayer(
    private val file: File,
    var isLooping: Boolean = false,
    private val onCompletion: () -> Unit = {},
    private val onError: (Exception) -> Unit = {}
) {
    companion object {
        private const val TAG = "FlacAudioPlayer"
        private const val TIMEOUT_US = 10_000L
    }

    @Volatile
    private var isPlayingState = false

    @Volatile
    private var isPausedState = false

    @Volatile
    private var isReleased = false

    @Volatile
    private var pendingSeekPositionMs: Long? = null

    @Volatile
    private var currentPositionMs = 0L

    var durationMs = 0L
        private set

    private var volumeFactor = 1.0f

    private var workerThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    val isPlaying: Boolean
        get() = isPlayingState && !isPausedState

    val currentPosition: Long
        get() = currentPositionMs

    fun setVolume(volume: Float) {
        volumeFactor = volume.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(volumeFactor)
        } catch (_: Exception) {}
    }

    fun start() {
        if (isReleased) return
        if (workerThread == null) {
            isPlayingState = true
            isPausedState = false
            workerThread = thread(name = "FlacAudioDecoderThread", isDaemon = true) {
                runDecoderLoop()
            }
        } else {
            isPausedState = false
            try {
                audioTrack?.play()
            } catch (_: Exception) {}
        }
    }

    fun pause() {
        isPausedState = true
        try {
            audioTrack?.pause()
        } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Long) {
        pendingSeekPositionMs = positionMs.coerceAtLeast(0L)
    }

    fun stop() {
        isPlayingState = false
        isPausedState = false
        workerThread?.interrupt()
        try {
            audioTrack?.stop()
        } catch (_: Exception) {}
    }

    fun release() {
        isReleased = true
        stop()
        workerThread = null
        try {
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun runDecoderLoop() {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        var fis: FileInputStream? = null

        try {
            fis = FileInputStream(file)
            extractor = MediaExtractor().apply {
                setDataSource(fis.fd, 0, file.length())
            }

            var audioTrackIndex = -1
            var mediaFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    mediaFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || mediaFormat == null) {
                throw IllegalStateException("No audio track found in FLAC file ${file.name}")
            }

            extractor.selectTrack(audioTrackIndex)
            durationMs = mediaFormat.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }?.let { it / 1000L } ?: 0L

            val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME) ?: "audio/flac"
            codec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(mediaFormat, null, null, 0)
                start()
            }

            var sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, pcmEncoding)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(pcmEncoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize.coerceAtLeast(16384))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.setVolume(volumeFactor)
            track.play()
            audioTrack = track

            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false

            while (isPlayingState && !isOutputEOS && !isReleased) {
                if (isPausedState) {
                    Thread.sleep(50)
                    continue
                }

                // Handle seek request
                val seekPos = pendingSeekPositionMs
                if (seekPos != null) {
                    pendingSeekPositionMs = null
                    extractor.seekTo(seekPos * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    codec.flush()
                    try {
                        track.flush()
                    } catch (_: Exception) {}
                    currentPositionMs = seekPos
                    isInputEOS = false
                }

                // 1. Feed input buffer
                if (!isInputEOS) {
                    val inputIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIdx >= 0) {
                        val inputBuf = codec.getInputBuffer(inputIdx)
                        if (inputBuf != null) {
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isInputEOS = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                codec.queueInputBuffer(inputIdx, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // 2. Drain output buffer
                val outputIdx = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    outputIdx >= 0 -> {
                        val outputBuf = codec.getOutputBuffer(outputIdx)
                        if (outputBuf != null && bufferInfo.size > 0) {
                            val chunk = ByteArray(bufferInfo.size)
                            outputBuf.position(bufferInfo.offset)
                            outputBuf.get(chunk, 0, bufferInfo.size)

                            // Convert 24-bit PCM or Float to standard 16-bit PCM if needed
                            val pcm16Data = if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                                convertFloatTo16BitPcm(chunk)
                            } else {
                                chunk
                            }

                            track.write(pcm16Data, 0, pcm16Data.size)
                            currentPositionMs = bufferInfo.presentationTimeUs / 1000L
                        }
                        codec.releaseOutputBuffer(outputIdx, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (isLooping && isPlayingState && !isReleased) {
                                extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                                codec.flush()
                                try { track.flush() } catch (_: Exception) {}
                                isInputEOS = false
                                currentPositionMs = 0L
                            } else {
                                isOutputEOS = true
                            }
                        }
                    }
                    outputIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = codec.outputFormat
                        sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        channelConfig = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
                        pcmEncoding = if (newFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            newFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } else {
                            AudioFormat.ENCODING_PCM_16BIT
                        }
                        if (track.sampleRate != sampleRate) {
                            runCatching { track.playbackRate = sampleRate }
                        }
                    }
                }
            }

            if (isOutputEOS && !isReleased) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (!isReleased) {
                        onCompletion()
                    }
                }
            }
        } catch (e: Exception) {
            if (!isReleased) {
                Log.e(TAG, "Error in FLAC decoder loop for ${file.name}", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (!isReleased) {
                        onError(e)
                    }
                }
            }
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { audioTrack?.stop() } catch (_: Exception) {}
            try { audioTrack?.release() } catch (_: Exception) {}
            try { fis?.close() } catch (_: Exception) {}
            audioTrack = null
            isPlayingState = false
        }
    }

    private fun convertFloatTo16BitPcm(input: ByteArray): ByteArray {
        val floatBuf = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        val outBuf = ByteBuffer.allocate(floatBuf.remaining() * 2).order(ByteOrder.LITTLE_ENDIAN)
        while (floatBuf.hasRemaining()) {
            val sample = floatBuf.get().coerceIn(-1.0f, 1.0f)
            val pcm16 = (sample * 32767.0f).toInt().toShort()
            outBuf.putShort(pcm16)
        }
        return outBuf.array()
    }
}
