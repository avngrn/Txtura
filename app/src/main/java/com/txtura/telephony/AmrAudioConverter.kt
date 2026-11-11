package com.txtura.telephony

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Ensures recorded voice messages and attached audio files are formatted as valid AMR-NB (`audio/amr`)
 * data with the standard 6-byte AMR header `#!AMR\n` (`0x23 0x21 0x41 0x4D 0x52 0x0A`) before sending over MMS.
 */
object AmrAudioConverter {

    private const val TAG = "AmrAudioConverter"

    /** Standard 6-byte AMR-NB magic file header "#!AMR\n" */
    val AMR_HEADER = byteArrayOf(0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A)

    /**
     * Inspects input audio bytes or Uri stream, ensuring it is formatted as a valid AMR-NB byte stream.
     * If the input is already valid AMR-NB with `#!AMR\n` header, returns it directly.
     * Otherwise, extracts AMR frames from containers or transcodes using MediaCodec/MediaExtractor.
     */
    fun ensureAmrFormat(context: Context?, audioUri: Uri?, rawBytes: ByteArray? = null): ByteArray? {
        val inputBytes = rawBytes ?: run {
            if (audioUri == null || context == null) return null
            try {
                context.contentResolver.openInputStream(audioUri)?.use { it.readBytes() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read input audio stream for AMR conversion", e)
                null
            }
        } ?: return null

        if (inputBytes.isEmpty()) return null

        // 1. Direct match: Already starts with #!AMR\n header
        if (inputBytes.size >= 6 && inputBytes.take(6).toByteArray().contentEquals(AMR_HEADER)) {
            return inputBytes
        }

        // 2. Embedded match: #!AMR\n appears inside (e.g. metadata prefix or PDU payload)
        val amrIdx = indexOfSubarray(inputBytes, AMR_HEADER)
        if (amrIdx >= 0) {
            return inputBytes.copyOfRange(amrIdx, inputBytes.size)
        }

        // 3. 3GP container containing 'samr' box
        val samrIdx = indexOfSubarray(inputBytes, "samr".toByteArray(Charsets.US_ASCII))
        if (samrIdx >= 0 && samrIdx + 8 < inputBytes.size) {
            val payload = inputBytes.copyOfRange(samrIdx + 8, inputBytes.size)
            return AMR_HEADER + payload
        }

        // 4. Transcode non-AMR audio formats (M4A, AAC, WAV, MP3) using MediaCodec
        if (audioUri != null && context != null) {
            try {
                val transcoded = transcodeToAmr(context, audioUri)
                if (transcoded != null && transcoded.isNotEmpty()) {
                    return transcoded
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaCodec transcoding to AMR-NB failed, fallback to header prepending", e)
            }
        }

        // Fallback: Prepend AMR header to raw payload if non-empty
        return AMR_HEADER + inputBytes
    }

    /**
     * Verifies and converts a File in-place if it is not a valid AMR-NB file.
     */
    fun ensureAmrFileInPlace(context: Context, file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        return try {
            val bytes = file.readBytes()
            val converted = ensureAmrFormat(context, file.toUri(), bytes)
            if (converted != null && !converted.contentEquals(bytes)) {
                file.writeBytes(converted)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert file in-place to AMR", e)
            false
        }
    }

    private fun indexOfSubarray(array: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || array.size < target.size) return -1
        val max = array.size - target.size
        for (i in 0..max) {
            var found = true
            for (j in target.indices) {
                if (array[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    private fun transcodeToAmr(context: Context, audioUri: Uri): ByteArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, audioUri, null)
            var audioTrackIdx = -1
            var inputFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    inputFormat = format
                    break
                }
            }
            if (audioTrackIdx < 0 || inputFormat == null) return null

            extractor.selectTrack(audioTrackIdx)

            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return null
            val decoder = MediaCodec.createDecoderByType(inputMime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val encoderFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AMR_NB,
                8000,
                1
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 12200)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AMR_NB)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val outStream = ByteArrayOutputStream()
            outStream.write(AMR_HEADER)

            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()

            var isDecoderEOS = false
            var isEncoderEOS = false
            val timeoutUs = 5000L

            var loopCount = 0
            val maxLoops = 2000

            while (!isEncoderEOS && loopCount < maxLoops) {
                loopCount++
                if (!isDecoderEOS) {
                    val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isDecoderEOS = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                var outIndex = decoder.dequeueOutputBuffer(decoderBufferInfo, timeoutUs)
                while (outIndex >= 0) {
                    val pcmBuffer = decoder.getOutputBuffer(outIndex)
                    if (pcmBuffer != null && decoderBufferInfo.size > 0) {
                        val encInputIndex = encoder.dequeueInputBuffer(timeoutUs)
                        if (encInputIndex >= 0) {
                            val encBuffer = encoder.getInputBuffer(encInputIndex)
                            if (encBuffer != null) {
                                pcmBuffer.position(decoderBufferInfo.offset)
                                pcmBuffer.limit(decoderBufferInfo.offset + decoderBufferInfo.size)
                                encBuffer.put(pcmBuffer)
                                encoder.queueInputBuffer(
                                    encInputIndex,
                                    0,
                                    decoderBufferInfo.size,
                                    decoderBufferInfo.presentationTimeUs,
                                    if ((decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                )
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    outIndex = decoder.dequeueOutputBuffer(decoderBufferInfo, 0)
                }

                var encOutIndex = encoder.dequeueOutputBuffer(encoderBufferInfo, timeoutUs)
                while (encOutIndex >= 0) {
                    val encodedBuffer = encoder.getOutputBuffer(encOutIndex)
                    if (encodedBuffer != null && encoderBufferInfo.size > 0) {
                        encodedBuffer.position(encoderBufferInfo.offset)
                        encodedBuffer.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                        val chunk = ByteArray(encoderBufferInfo.size)
                        encodedBuffer.get(chunk)
                        outStream.write(chunk)
                    }
                    if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEncoderEOS = true
                    }
                    encoder.releaseOutputBuffer(encOutIndex, false)
                    encOutIndex = encoder.dequeueOutputBuffer(encoderBufferInfo, 0)
                }
            }

            try { decoder.stop() } catch (_: Exception) {}
            try { decoder.release() } catch (_: Exception) {}
            try { encoder.stop() } catch (_: Exception) {}
            try { encoder.release() } catch (_: Exception) {}

            return outStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "MediaCodec transcoding error", e)
            return null
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
