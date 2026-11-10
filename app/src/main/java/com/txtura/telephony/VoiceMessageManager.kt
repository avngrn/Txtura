package com.txtura.telephony

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages voice recording strictly capped at 299,900 bytes (299.9 kB max limit)
 * and in-app interactive playback of voice messages.
 */
object VoiceMessageManager {

    private const val TAG = "VoiceMessageManager"
    const val MAX_VOICE_MMS_BYTES = 299_900L // 299.9 kB max limit
    private const val AMR_BYTE_RATE = 1525L // ~12.2 kbps AMR-NB encoding rate

    // ==========================================
    // 1. Voice Recording Management
    // ==========================================

    private var activeRecorder: MediaRecorder? = null
    private var activeRecordFile: File? = null
    private var recordStartTimeMillis: Long = 0L

    val isRecording = mutableStateOf(false)
    val recordingDurationMillis = mutableStateOf(0L)
    val recordingFileSizeBytes = mutableStateOf(0L)
    val recordingRemainingSeconds = mutableStateOf(196L) // ~3m 16s max (299.9 kB)

    private var recordTickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startRecording(
        context: Context,
        onMaxLimitReached: (File) -> Unit
    ): Boolean {
        try {
            stopRecordingInternal(delete = true)

            val dir = File(context.cacheDir, "voice_messages").apply { mkdirs() }
            val file = File(dir, "voice_msg_${System.currentTimeMillis()}.amr")
            activeRecordFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setAudioEncodingBitRate(12200)
            recorder.setAudioSamplingRate(8000)
            recorder.setMaxFileSize(MAX_VOICE_MMS_BYTES)
            recorder.setOutputFile(file.absolutePath)

            recorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                ) {
                    val finishedFile = stopRecording()
                    if (finishedFile != null && finishedFile.exists()) {
                        onMaxLimitReached(finishedFile)
                    }
                }
            }

            recorder.prepare()
            recorder.start()

            activeRecorder = recorder
            recordStartTimeMillis = System.currentTimeMillis()
            isRecording.value = true
            recordingDurationMillis.value = 0L
            recordingFileSizeBytes.value = 0L
            val totalMaxSeconds = (MAX_VOICE_MMS_BYTES / AMR_BYTE_RATE)
            recordingRemainingSeconds.value = totalMaxSeconds

            recordTickerJob?.cancel()
            recordTickerJob = scope.launch {
                while (isActive && isRecording.value) {
                    delay(100)
                    val elapsed = System.currentTimeMillis() - recordStartTimeMillis
                    recordingDurationMillis.value = elapsed

                    val len = activeRecordFile?.length() ?: 0L
                    recordingFileSizeBytes.value = len

                    val elapsedSeconds = elapsed / 1000L
                    val remSecs = if (len >= MAX_VOICE_MMS_BYTES) 0L else (totalMaxSeconds - elapsedSeconds).coerceAtLeast(0L)
                    recordingRemainingSeconds.value = remSecs

                    if (remSecs <= 0L) {
                        val finishedFile = stopRecording()
                        if (finishedFile != null && finishedFile.exists()) {
                            onMaxLimitReached(finishedFile)
                        }
                        break
                    }
                }
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder for voice message", e)
            stopRecordingInternal(delete = true)
            return false
        }
    }

    fun stopRecording(): File? {
        val file = activeRecordFile
        stopRecordingInternal(delete = false)
        return file
    }

    fun cancelRecording() {
        stopRecordingInternal(delete = true)
    }

    private fun stopRecordingInternal(delete: Boolean) {
        recordTickerJob?.cancel()
        recordTickerJob = null
        isRecording.value = false

        try {
            activeRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recorder", e)
        } finally {
            activeRecorder = null
        }

        if (delete) {
            try {
                activeRecordFile?.delete()
            } catch (_: Exception) {}
            activeRecordFile = null
        }
    }

    // ==========================================
    // 2. In-App Audio Playback Management
    // ==========================================

    private var activePlayer: MediaPlayer? = null
    private val _playingUri = mutableStateOf<Uri?>(null)
    val playingUri: State<Uri?> = _playingUri

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPositionMs = mutableIntStateOf(0)
    val currentPositionMs: State<Int> = _currentPositionMs

    private val _totalDurationMs = mutableIntStateOf(0)
    val totalDurationMs: State<Int> = _totalDurationMs

    private val _progressFraction = mutableFloatStateOf(0f)
    val progressFraction: State<Float> = _progressFraction

    private var playbackProgressJob: Job? = null
    private val durationCache = ConcurrentHashMap<String, Long>()

    /**
     * Resolves and caches audio data to a local file in cache so that MediaPlayer and
     * MediaMetadataRetriever can reliably access and play it without permission or IPC issues,
     * whether the audio is incoming from Telephony MMS (content://mms/part/...), FileProvider, or local cache.
     */
    fun getPlayableAudioFile(context: Context, uri: Uri): File? {
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                val f = File(path)
                if (f.exists() && f.length() > 0L) return f
            }
        }
        val cacheDir = File(context.cacheDir, "voice_playback_cache").apply { mkdirs() }
        val safeHash = uri.toString().hashCode().toString().replace("-", "m")
        val cacheFile = File(cacheDir, "cached_voice_$safeHash.amr")
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            return cacheFile
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (cacheFile.exists() && cacheFile.length() > 0L) cacheFile else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve/cache audio from $uri", e)
            null
        }
    }

    fun togglePlayPause(context: Context, uri: Uri) {
        val currentUri = _playingUri.value
        if (currentUri == uri && activePlayer != null) {
            if (_isPlaying.value) {
                pausePlayback()
            } else {
                resumePlayback()
            }
            return
        }

        // Switching or starting new playback
        stopPlayback()

        try {
            val audioFile = getPlayableAudioFile(context, uri)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (audioFile != null && audioFile.exists()) {
                    setDataSource(audioFile.absolutePath)
                } else {
                    setDataSource(context, uri)
                }
                prepare()
            }

            _playingUri.value = uri
            _totalDurationMs.intValue = player.duration.coerceAtLeast(0)
            durationCache[uri.toString()] = player.duration.toLong().coerceAtLeast(0L)

            player.setOnCompletionListener {
                _isPlaying.value = false
                _progressFraction.floatValue = 0f
                _currentPositionMs.intValue = 0
                playbackProgressJob?.cancel()
            }

            player.setOnErrorListener { _, _, _ ->
                stopPlayback()
                true
            }

            player.start()
            activePlayer = player
            _isPlaying.value = true
            startProgressTicker()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback for $uri", e)
            stopPlayback()
        }
    }

    fun seekTo(uri: Uri, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        if (_playingUri.value == uri && activePlayer != null) {
            val targetMs = (_totalDurationMs.intValue * clamped).toInt()
            try {
                activePlayer?.seekTo(targetMs)
                _currentPositionMs.intValue = targetMs
                _progressFraction.floatValue = clamped
            } catch (_: Exception) {}
        }
    }

    private fun resumePlayback() {
        try {
            activePlayer?.start()
            _isPlaying.value = true
            startProgressTicker()
        } catch (_: Exception) {}
    }

    fun pausePlayback() {
        try {
            activePlayer?.pause()
            _isPlaying.value = false
            playbackProgressJob?.cancel()
        } catch (_: Exception) {}
    }

    fun stopPlayback() {
        playbackProgressJob?.cancel()
        playbackProgressJob = null
        try {
            activePlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        activePlayer = null
        _playingUri.value = null
        _isPlaying.value = false
        _currentPositionMs.intValue = 0
        _progressFraction.floatValue = 0f
    }

    private fun startProgressTicker() {
        playbackProgressJob?.cancel()
        playbackProgressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val pos = activePlayer?.currentPosition ?: 0
                    val dur = activePlayer?.duration ?: 1
                    _currentPositionMs.intValue = pos
                    _progressFraction.floatValue = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                } catch (_: Exception) {}
                delay(40)
            }
        }
    }

    fun getAudioDurationMs(context: Context, uri: Uri): Long {
        durationCache[uri.toString()]?.let { return it }
        val audioFile = getPlayableAudioFile(context, uri)
        var retriever: MediaMetadataRetriever? = null
        var dur = 0L
        try {
            retriever = MediaMetadataRetriever()
            if (audioFile != null && audioFile.exists()) {
                retriever.setDataSource(audioFile.absolutePath)
            } else {
                retriever.setDataSource(context, uri)
            }
            val timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            dur = timeStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever failed for $uri", e)
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }

        // AMR-NB fallback: calculate duration from file size (12.2 kbps = ~1600 bytes/sec, 6-byte header)
        if (dur <= 0L && audioFile != null && audioFile.exists() && audioFile.length() > 6L) {
            val secs = ((audioFile.length() - 6L) / 1600L).coerceAtLeast(1L)
            dur = secs * 1000L
        }

        if (dur > 0L) {
            durationCache[uri.toString()] = dur
        }
        return dur
    }

    fun formatDuration(millis: Long): String {
        val totalSecs = (millis / 1000L).coerceAtLeast(0L)
        val mins = totalSecs / 60L
        val secs = totalSecs % 60L
        return "%d:%02d".format(mins, secs)
    }
}
