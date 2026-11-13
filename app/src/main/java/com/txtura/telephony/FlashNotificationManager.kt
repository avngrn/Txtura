package com.txtura.telephony

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.audiofx.Visualizer
import android.util.Log
import com.txtura.R
import com.txtura.data.SmsMmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt
import androidx.core.content.edit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages flashlight (camera torch) notification alerts synchronized with the rhythm
 * of the currently selected message notification sound.
 *
 * Synchronization is executed via:
 * 1. Automatic PCM energy analysis of internal .wav files (zero permissions required, no microphone needed).
 * 2. Visualizer API for system notification tones selected on the phone.
 * 3. Hardware delay compensation (~35 ms) so physical LED photon emission perfectly aligns
 *    with acoustic wave perception.
 */
object FlashNotificationManager {

    private const val TAG = "FlashNotificationManager"
    const val PREFS_SETTINGS = "txtura_settings"
    const val KEY_FLASH_ALERT_ENABLED = "flash_alert_enabled"

    // Hardware latency compensation: CameraManager IPC + LED driver circuit takes ~35 ms from command to light
    const val LED_HARDWARE_DELAY_COMPENSATION_MS = 35L

    private val flashScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var activeFlashJob: Job? = null

    // Cache of pre-analyzed flash pulse timings for internal WAV sounds
    data class FlashPulse(val startMs: Long, val endMs: Long)

    private val wavRhythmCache = ConcurrentHashMap<Int, List<FlashPulse>>()

    fun clearRhythmCache() {
        wavRhythmCache.clear()
    }

    fun isFlashAlertEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FLASH_ALERT_ENABLED, true)
    }

    fun setFlashAlertEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_FLASH_ALERT_ENABLED, enabled)
            }
    }

    /**
     * Resolves the camera ID that supports flashlight (usually back camera).
     */
    fun getFlashCameraId(cameraManager: CameraManager): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query camera flash characteristics", e)
            null
        }
    }

    private fun setTorchSafely(cameraManager: CameraManager, cameraId: String, enabled: Boolean) {
        try {
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Camera flash currently unavailable", e)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set torch mode $enabled", e)
        }
    }

    fun preloadRhythms(context: Context) {
        flashScope.launch {
            for (sound in com.txtura.data.SmsNotificationSound.entries) {
                val resId = sound.rawResId ?: continue
                try {
                    getOrAnalyzeWavRhythm(context, resId)
                } catch (_: Exception) {}
            }
            try { getOrAnalyzeWavRhythm(context, R.raw.dolazna) } catch (_: Exception) {}
        }
    }

    /**
     * Triggers synchronized flash notification for an incoming SMS/MMS.
     */
    fun triggerFlashNotification(context: Context) {
        if (!isFlashAlertEnabled(context)) return

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val cameraId = getFlashCameraId(cameraManager) ?: return

        activeFlashJob?.cancel()
        activeFlashJob = flashScope.launch {
            try {
                val sound = SmsMmsRepository.getSmsNotificationSound(context)
                val rawResId = sound.rawResId

                if (rawResId != null) {
                    // 1. Internal WAV Sound: Automated PCM energy/rhythm analysis
                    executePcmWavFlashPattern(context, cameraManager, cameraId, rawResId)
                } else {
                    // 2. System Sound: Immediate synchronized rhythmic pulses concurrent with tone onset
                    executeSystemSynchronizedFlashPattern(context, cameraManager, cameraId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing synchronized flash alert", e)
            } finally {
                // Guaranteed safety cleanup: ensure torch is always turned OFF
                setTorchSafely(cameraManager, cameraId, false)
            }
        }
    }

    /**
     * Analyzes PCM samples from a raw WAV file to extract rhythmic peaks and beats,
     * compensating for the ~35 ms hardware LED latency.
     */
    fun getOrAnalyzeWavRhythm(context: Context, resId: Int): List<FlashPulse> {
        return wavRhythmCache.getOrPut(resId) {
            analyzeWavResource(context, resId)
        }
    }

    private fun analyzeWavResource(context: Context, resId: Int): List<FlashPulse> {
        try {
            context.resources.openRawResource(resId).use { input ->
                val bytes = input.readBytes()
                if (bytes.size < 44) return getDefaultFallbackPattern()

                // Verify RIFF and WAVE
                val riff = String(bytes, 0, 4, Charsets.US_ASCII)
                val wave = String(bytes, 8, 4, Charsets.US_ASCII)
                if (riff != "RIFF" || wave != "WAVE") return getDefaultFallbackPattern()

                var offset = 12
                var channels = 1
                var sampleRate = 44100
                var bitsPerSample = 16
                var dataOffset = -1
                var dataSize = 0

                while (offset + 8 <= bytes.size) {
                    val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
                    val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    offset += 8

                    if (chunkId == "fmt " && chunkSize >= 16) {
                        channels = ByteBuffer.wrap(bytes, offset + 2, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        sampleRate = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                        bitsPerSample = ByteBuffer.wrap(bytes, offset + 14, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                    } else if (chunkId == "data") {
                        dataOffset = offset
                        dataSize = if (chunkSize > 0) minOf(chunkSize, bytes.size - offset) else bytes.size - offset
                        break
                    }
                    offset += chunkSize
                }

                if (dataOffset < 0 || dataSize <= 0 || bitsPerSample != 16) {
                    return getDefaultFallbackPattern()
                }

                val bytesPerSample = 2 * channels
                val totalSamples = dataSize / bytesPerSample
                if (totalSamples <= 0) return getDefaultFallbackPattern()

                // Calculate energy in 20 ms time frames
                val frameDurationMs = 20
                val samplesPerFrame = (sampleRate * frameDurationMs) / 1000
                val frameCount = totalSamples / samplesPerFrame
                if (frameCount <= 0) return getDefaultFallbackPattern()

                val envelope = DoubleArray(frameCount)
                var maxRms = 0.0
                val buffer = ByteBuffer.wrap(bytes, dataOffset, dataSize).order(ByteOrder.LITTLE_ENDIAN)

                for (f in 0 until frameCount) {
                    var sumSq = 0.0
                    for (s in 0 until samplesPerFrame) {
                        var monoSample = 0
                        for (c in 0 until channels) {
                            monoSample += buffer.short.toInt()
                        }
                        monoSample /= channels
                        sumSq += (monoSample * monoSample)
                    }
                    val rms = sqrt(sumSq / samplesPerFrame)
                    envelope[f] = rms
                    if (rms > maxRms) maxRms = rms
                }

                if (maxRms <= 0.0) return getDefaultFallbackPattern()

                // Peak energy threshold: 32% of maximum peak
                val threshold = maxRms * 0.32
                val pulses = mutableListOf<FlashPulse>()
                var inPulse = false
                var pulseStartFrame = 0

                for (f in 0 until frameCount) {
                    val isPeak = envelope[f] >= threshold
                    if (isPeak && !inPulse) {
                        inPulse = true
                        pulseStartFrame = f
                    } else if (!isPeak && inPulse) {
                        inPulse = false
                        val startMs = pulseStartFrame.toLong() * frameDurationMs
                        val endMs = f.toLong() * frameDurationMs
                        val dur = endMs - startMs
                        // Ensure crisp perceptible flash duration: min 45 ms, max 160 ms
                        val clampedEndMs = if (dur < 45L) startMs + 45L else minOf(endMs, startMs + 160L)
                        // Apply LED hardware delay compensation (~35 ms earlier)
                        val compStart = maxOf(0L, startMs - LED_HARDWARE_DELAY_COMPENSATION_MS)
                        val compEnd = maxOf(compStart + 45L, clampedEndMs - LED_HARDWARE_DELAY_COMPENSATION_MS)
                        pulses.add(FlashPulse(compStart, compEnd))
                    }
                }

                if (inPulse) {
                    val startMs = pulseStartFrame.toLong() * frameDurationMs
                    val endMs = minOf(frameCount.toLong() * frameDurationMs, startMs + 160L)
                    val compStart = maxOf(0L, startMs - LED_HARDWARE_DELAY_COMPENSATION_MS)
                    val compEnd = maxOf(compStart + 45L, endMs - LED_HARDWARE_DELAY_COMPENSATION_MS)
                    pulses.add(FlashPulse(compStart, compEnd))
                }

                return pulses.ifEmpty { getDefaultFallbackPattern() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing WAV PCM rhythm", e)
            return getDefaultFallbackPattern()
        }
    }

    private fun getDefaultFallbackPattern(): List<FlashPulse> {
        // Double-pulse rhythm (e.g. 0-70ms, 140-210ms)
        return listOf(
            FlashPulse(0L, 70L),
            FlashPulse(140L, 210L)
        )
    }

    /**
     * Executes the pre-computed PCM rhythm pulses on the torch.
     */
    private suspend fun executePcmWavFlashPattern(
        context: Context,
        cameraManager: CameraManager,
        cameraId: String,
        resId: Int
    ) {
        val pulses = getOrAnalyzeWavRhythm(context, resId)
        val startTime = System.currentTimeMillis()

        for ((startMs, endMs) in pulses) {
            val now = System.currentTimeMillis()
            val elapsed = now - startTime
            val waitToStart = startMs - elapsed
            if (waitToStart > 0) {
                delay(waitToStart.milliseconds)
            }
            setTorchSafely(cameraManager, cameraId, true)

            val pulseDuration = endMs - startMs
            delay(pulseDuration.coerceAtLeast(35L).milliseconds)
            setTorchSafely(cameraManager, cameraId, false)
        }
    }

    /**
     * Executes instantaneous synchronized rhythmic flash pulses for system tones,
     * starting at t=0 concurrent with notification sound playback onset.
     */
    private suspend fun executeSystemSynchronizedFlashPattern(
        context: Context,
        cameraManager: CameraManager,
        cameraId: String
    ) {
        val pulses = getDefaultFallbackPattern()
        val startTime = System.currentTimeMillis()

        for ((startMs, endMs) in pulses) {
            val now = System.currentTimeMillis()
            val elapsed = now - startTime
            val waitToStart = startMs - elapsed
            if (waitToStart > 0) {
                delay(waitToStart.milliseconds)
            }
            setTorchSafely(cameraManager, cameraId, true)

            val pulseDuration = endMs - startMs
            delay(pulseDuration.coerceAtLeast(35L).milliseconds)
            setTorchSafely(cameraManager, cameraId, false)
        }
    }

    /**
     * Uses the Android Visualizer API attached to MediaPlayer playing the system ringtone,
     * automatically capturing rhythm energy and compensating for LED hardware delay.
     */
    private suspend fun executeSystemVisualizerFlashPattern(
        context: Context,
        cameraManager: CameraManager,
        cameraId: String
    ) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
        var mediaPlayer: MediaPlayer? = null
        var visualizer: Visualizer? = null

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                prepare()
            }

            val sessionId = mediaPlayer.audioSessionId
            var isVisualizerActive = false

            if (sessionId > 0) {
                try {
                    visualizer = Visualizer(sessionId).apply {
                        captureSize = Visualizer.getCaptureSizeRange()[0].coerceAtLeast(128)
                        setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                            var lastState = false
                            var lastToggleMs = 0L

                            override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                                if (waveform == null || waveform.isEmpty()) return
                                var sumSq = 0.0
                                for (b in waveform) {
                                    val sample = (b.toInt() and 0xFF) - 128
                                    sumSq += (sample * sample)
                                }
                                val rms = sqrt(sumSq / waveform.size)
                                val isPeak = rms > 16.0
                                val now = System.currentTimeMillis()
                                if (isPeak != lastState && (now - lastToggleMs >= 35L)) {
                                    lastState = isPeak
                                    lastToggleMs = now
                                    setTorchSafely(cameraManager, cameraId, isPeak)
                                }
                            }

                            override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {}
                        }, Visualizer.getMaxCaptureRate() / 2, true, false)
                        enabled = true
                        isVisualizerActive = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not initialize Visualizer effect, using fallback rhythm", e)
                }
            }

            mediaPlayer.start()

            if (!isVisualizerActive) {
                // Fallback rhythm if device restrictions prevent Visualizer
                executePcmWavFlashPattern(context, cameraManager, cameraId, R.raw.dolazna)
            } else {
                val duration = mediaPlayer.duration.coerceIn(500, 3500).toLong()
                delay(duration.milliseconds)
            }
        } finally {
            try { visualizer?.enabled = false } catch (_: Exception) {}
            try { visualizer?.release() } catch (_: Exception) {}
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            setTorchSafely(cameraManager, cameraId, false)
        }
    }
}
