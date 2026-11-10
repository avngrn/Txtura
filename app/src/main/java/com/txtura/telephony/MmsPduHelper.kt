package com.txtura.telephony

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.txtura.data.MmsCreationMode
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Parsed MMS Notification (`m-notification-ind`, 0x82) received via WAP Push.
 */
data class MmsNotificationInd(
    val transactionId: String,
    val contentLocation: String,
    val senderAddress: String?,
    val messageSize: Long,
    val expiry: Long,
    val subject: String? = null,
)

/**
 * Parsed downloaded MMS message (`m-retrieve-conf`, 0x84) received from carrier MMSC.
 */
data class MmsDownloadedMessage(
    val senderAddress: String,
    val timestampMillis: Long,
    val subject: String?,
    val bodyText: String?,
    val imageBytes: ByteArray?,
    val imageMimeType: String?,
    val audioBytes: ByteArray? = null,
    val audioMimeType: String? = null,
    val otherBytes: ByteArray? = null,
    val otherMimeType: String? = null,
    val otherFileName: String? = null,
)

/**
 * Robust on-device WAP MMS 1.2 / 1.3 (OMA MMS Encapsulation) PDU builder, parser, and media optimizer.
 * - Encodes standard `M-Send.req` binary PDUs for [android.telephony.SmsManager.sendMultimediaMessage].
 * - Decodes `M-Notification.ind` WAP Push PDUs from [android.provider.Telephony.WAP_PUSH_DELIVER].
 * - Decodes `M-Retrieve.conf` binary PDUs downloaded via [android.telephony.SmsManager.downloadMultimediaMessage].
 */
object MmsPduHelper {

    private const val TAG = "MmsPduHelper"

    // OMA MMS Header Field Names (high-bit set)
    private const val HDR_MESSAGE_TYPE = 0x8C
    private const val VAL_MESSAGE_TYPE_SEND_REQ = 0x80
    private const val VAL_MESSAGE_TYPE_NOTIFICATION_IND = 0x82
    private const val VAL_MESSAGE_TYPE_RETRIEVE_CONF = 0x84

    private const val HDR_TRANSACTION_ID = 0x98
    private const val HDR_MMS_VERSION = 0x8D
    private const val VAL_MMS_VERSION_1_2 = 0x92

    private const val HDR_DATE = 0x85
    private const val HDR_FROM = 0x89
    private const val VAL_FROM_INSERT_ADDRESS_TOKEN = 0x81
    private const val VAL_FROM_ADDRESS_PRESENT_TOKEN = 0x80

    private const val HDR_TO = 0x97
    private const val HDR_SUBJECT = 0x96
    private const val HDR_CONTENT_TYPE = 0x84
    private const val HDR_CONTENT_LOCATION = 0x8A
    private const val HDR_MESSAGE_SIZE = 0x8E
    private const val HDR_EXPIRY = 0x88

    private const val HDR_DELIVERY_REPORT = 0x86
    private const val HDR_READ_REPORT = 0x90
    private const val VAL_REPORT_YES = 0x80
    private const val VAL_REPORT_NO = 0x81

    // Content-Type short integers and tokens
    private const val CT_MULTIPART_RELATED = "application/vnd.wap.multipart.related"
    private const val CT_IMAGE_JPEG = "image/jpeg"
    private const val CT_TEXT_PLAIN = "text/plain; charset=utf-8"

    /**
     * Builds a complete, compliant M-Send.req binary PDU with an image part and optional text part.
     */
    fun buildSendReqPdu(
        context: Context,
        recipient: String,
        bodyText: String?,
        imageBytes: ByteArray? = null,
        imageMimeType: String = CT_IMAGE_JPEG,
        audioBytes: ByteArray? = null,
        audioMimeType: String = "audio/amr",
        otherBytes: ByteArray? = null,
        otherMimeType: String? = null,
        otherFileName: String? = null,
        requestDeliveryReport: Boolean = false,
        requestReadReport: Boolean = false,
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Headers
        out.write(HDR_MESSAGE_TYPE)
        out.write(VAL_MESSAGE_TYPE_SEND_REQ)

        out.write(HDR_TRANSACTION_ID)
        val transactionId = "T" + System.currentTimeMillis().toString(16)
        out.write(transactionId.toByteArray(Charsets.US_ASCII))
        out.write(0x00)

        out.write(HDR_MMS_VERSION)
        out.write(VAL_MMS_VERSION_1_2)

        out.write(HDR_DATE)
        val seconds = (System.currentTimeMillis() / 1000L).toInt()
        out.write(0x04)
        out.write((seconds ushr 24) and 0xFF)
        out.write((seconds ushr 16) and 0xFF)
        out.write((seconds ushr 8) and 0xFF)
        out.write(seconds and 0xFF)

        out.write(HDR_FROM)
        out.write(0x01)
        out.write(VAL_FROM_INSERT_ADDRESS_TOKEN)

        out.write(HDR_TO)
        val cleanRecipient = if (recipient.endsWith("/TYPE=PLMN")) recipient else "$recipient/TYPE=PLMN"
        out.write(cleanRecipient.toByteArray(Charsets.US_ASCII))
        out.write(0x00)

        if (!bodyText.isNullOrBlank()) {
            val preview = bodyText.take(40)
            val previewBytes = preview.toByteArray(Charsets.UTF_8)
            out.write(HDR_SUBJECT)
            writeUintvar(out, (previewBytes.size + 1).toLong())
            out.write(0xEA)
            out.write(previewBytes)
            out.write(0x00)
        }

        out.write(HDR_DELIVERY_REPORT)
        out.write(if (requestDeliveryReport) VAL_REPORT_YES else VAL_REPORT_NO)

        out.write(HDR_READ_REPORT)
        out.write(if (requestReadReport) VAL_REPORT_YES else VAL_REPORT_NO)

        out.write(HDR_CONTENT_TYPE)
        out.write(CT_MULTIPART_RELATED.toByteArray(Charsets.US_ASCII))
        out.write(0x00)

        // 2. Multipart Body Parts
        val parts = mutableListOf<MmsPart>()

        if (!bodyText.isNullOrBlank()) {
            parts.add(
                MmsPart(
                    contentType = CT_TEXT_PLAIN,
                    contentLocation = "text_01.txt",
                    contentId = "<text_01>",
                    data = bodyText.toByteArray(Charsets.UTF_8)
                )
            )
        }

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            parts.add(
                MmsPart(
                    contentType = imageMimeType,
                    contentLocation = "image_01.jpg",
                    contentId = "<image_01>",
                    data = imageBytes
                )
            )
        }

        if (audioBytes != null && audioBytes.isNotEmpty()) {
            parts.add(
                MmsPart(
                    contentType = audioMimeType,
                    contentLocation = "voice_01.amr",
                    contentId = "<voice_01>",
                    data = audioBytes
                )
            )
        }

        if (otherBytes != null && otherBytes.isNotEmpty()) {
            val fName = otherFileName?.ifBlank { "attachment.dat" } ?: "attachment.dat"
            parts.add(
                MmsPart(
                    contentType = otherMimeType ?: "application/octet-stream",
                    contentLocation = fName,
                    contentId = "<attachment_01>",
                    data = otherBytes
                )
            )
        }

        writeUintvar(out, parts.size.toLong())

        for (part in parts) {
            val headerStream = ByteArrayOutputStream()

            headerStream.write(part.contentType.toByteArray(Charsets.US_ASCII))
            headerStream.write(0x00)

            headerStream.write(0x8E)
            headerStream.write(part.contentLocation.toByteArray(Charsets.US_ASCII))
            headerStream.write(0x00)

            headerStream.write(0xC0)
            headerStream.write(part.contentId.toByteArray(Charsets.US_ASCII))
            headerStream.write(0x00)

            val partHeaders = headerStream.toByteArray()

            writeUintvar(out, partHeaders.size.toLong())
            writeUintvar(out, part.data.size.toLong())
            out.write(partHeaders)
            out.write(part.data)
        }

        return out.toByteArray()
    }

    /**
     * Parses an incoming OMA MMS Notification Indication (`m-notification-ind`, 0x82)
     * delivered via WAP Push broadcast (`WAP_PUSH_DELIVER_ACTION`).
     */
    fun parseNotificationInd(bytes: ByteArray): MmsNotificationInd? {
        if (bytes.isEmpty()) return null
        var i = 0
        var transactionId = ""
        var contentLocation = ""
        var senderAddress: String? = null
        var messageSize = 0L
        var expiry = 0L
        var subject: String? = null

        while (i < bytes.size) {
            val tag = bytes[i].toInt() and 0xFF
            when (tag) {
                HDR_MESSAGE_TYPE -> {
                    i++
                    if (i < bytes.size) {
                        val mType = bytes[i].toInt() and 0xFF
                        if (mType != VAL_MESSAGE_TYPE_NOTIFICATION_IND && mType != VAL_MESSAGE_TYPE_RETRIEVE_CONF) {
                            Log.w(TAG, "Unrecognized MMS Push message type: 0x${mType.toString(16)}")
                        }
                        i++
                    }
                }
                HDR_TRANSACTION_ID -> {
                    i++
                    val str = readNullTerminatedString(bytes, i)
                    transactionId = str.text
                    i = str.nextIndex
                }
                HDR_MMS_VERSION -> {
                    i += 2 // Skip version byte
                }
                HDR_FROM -> {
                    i++
                    if (i < bytes.size) {
                        val vLen = readValueLength(bytes, i)
                        i = vLen.nextIndex
                        val endFrom = (i + vLen.length).coerceAtMost(bytes.size)
                        if (i < endFrom) {
                            val token = bytes[i].toInt() and 0xFF
                            if (token == VAL_FROM_ADDRESS_PRESENT_TOKEN) {
                                i++
                                val str = readNullTerminatedString(bytes, i)
                                var addr = str.text.trim()
                                if (addr.endsWith("/TYPE=PLMN", ignoreCase = true)) {
                                    addr = addr.substring(0, addr.length - 10)
                                }
                                senderAddress = addr
                            }
                        }
                        i = endFrom
                    }
                }
                HDR_CONTENT_LOCATION -> {
                    i++
                    val str = readNullTerminatedString(bytes, i)
                    contentLocation = str.text
                    i = str.nextIndex
                }
                HDR_MESSAGE_SIZE -> {
                    i++
                    val num = readLongInteger(bytes, i)
                    messageSize = num.value
                    i = num.nextIndex
                }
                HDR_EXPIRY -> {
                    i++
                    val vLen = readValueLength(bytes, i)
                    i = vLen.nextIndex
                    if (i < bytes.size) {
                        val exp = readLongInteger(bytes, i + 1)
                        expiry = exp.value
                    }
                    i = (i + vLen.length).coerceAtMost(bytes.size)
                }
                HDR_SUBJECT -> {
                    i++
                    val str = readEncodedStringValue(bytes, i)
                    subject = str.text
                    i = str.nextIndex
                }
                else -> {
                    i++
                }
            }
        }

        // Fallback: If content location wasn't parsed via standard header offset, scan bytes for URL
        if (contentLocation.isBlank()) {
            contentLocation = extractUrlFromBytes(bytes) ?: return null
        }

        if (senderAddress.isNullOrBlank()) {
            senderAddress = extractSenderFromBytes(bytes)
        }

        return MmsNotificationInd(
            transactionId = transactionId,
            contentLocation = contentLocation,
            senderAddress = senderAddress,
            messageSize = messageSize,
            expiry = expiry,
            subject = subject
        )
    }

    /**
     * Parses a downloaded MMS binary PDU (`m-retrieve-conf`, 0x84) saved from [android.telephony.SmsManager.downloadMultimediaMessage].
     * Extracts sender address, subject, text message body, and binary image attachments.
     */
    fun parseRetrieveConf(bytes: ByteArray): MmsDownloadedMessage? {
        if (bytes.isEmpty()) return null
        var i = 0
        var senderAddress = ""
        var timestampSeconds = 0L
        var subject: String? = null
        var bodyText: String? = null
        var imageBytes: ByteArray? = null
        var imageMimeType: String? = null
        var audioBytes: ByteArray? = null
        var audioMimeType: String? = null
        var otherBytes: ByteArray? = null
        var otherMimeType: String? = null
        var otherFileName: String? = null

        // 1. Parse Headers
        while (i < bytes.size) {
            val tag = bytes[i].toInt() and 0xFF

            if (tag == HDR_CONTENT_TYPE) {
                i++
                // Content-Type found, end of main headers
                // Skip content-type string or short integer
                if (i < bytes.size) {
                    val ctByte = bytes[i].toInt() and 0xFF
                    if ((ctByte and 0x80) != 0) {
                        i++ // Short integer token
                    } else {
                        val str = readNullTerminatedString(bytes, i)
                        i = str.nextIndex
                    }
                }
                break
            }

            when (tag) {
                HDR_MESSAGE_TYPE -> {
                    i += 2
                }
                HDR_TRANSACTION_ID -> {
                    i++
                    val str = readNullTerminatedString(bytes, i)
                    i = str.nextIndex
                }
                HDR_MMS_VERSION -> {
                    i += 2
                }
                HDR_DATE -> {
                    i++
                    val dateInt = readLongInteger(bytes, i)
                    timestampSeconds = dateInt.value
                    i = dateInt.nextIndex
                }
                HDR_FROM -> {
                    i++
                    if (i < bytes.size) {
                        val vLen = readValueLength(bytes, i)
                        i = vLen.nextIndex
                        val endFrom = (i + vLen.length).coerceAtMost(bytes.size)
                        if (i < endFrom) {
                            val token = bytes[i].toInt() and 0xFF
                            if (token == VAL_FROM_ADDRESS_PRESENT_TOKEN) {
                                i++
                                val str = readNullTerminatedString(bytes, i)
                                var addr = str.text.trim()
                                if (addr.endsWith("/TYPE=PLMN", ignoreCase = true)) {
                                    addr = addr.substring(0, addr.length - 10)
                                }
                                senderAddress = addr
                            }
                        }
                        i = endFrom
                    }
                }
                HDR_SUBJECT -> {
                    i++
                    val str = readEncodedStringValue(bytes, i)
                    subject = str.text
                    i = str.nextIndex
                }
                HDR_TO -> {
                    i++
                    val str = readNullTerminatedString(bytes, i)
                    i = str.nextIndex
                }
                else -> {
                    i++
                }
            }
        }

        // Fallbacks for sender and date if headers were unconventional
        if (senderAddress.isBlank()) {
            senderAddress = extractSenderFromBytes(bytes) ?: "MMS"
        }
        val timestampMillis = if (timestampSeconds > 0) timestampSeconds * 1000L else System.currentTimeMillis()

        // 2. Parse Multipart Body Parts
        if (i < bytes.size) {
            try {
                val (numPartsLong, numPartsNext) = readUintvar(bytes, i)
                val numParts = numPartsLong.toInt()
                i = numPartsNext

                if (numParts in 1..60) {
                    for (p in 0 until numParts) {
                        if (i >= bytes.size) break
                        val (hLenVal, hLenNext) = readUintvar(bytes, i)
                        val headerLen = hLenVal.toInt()
                        i = hLenNext

                        val (dLenVal, dLenNext) = readUintvar(bytes, i)
                        val dataLen = dLenVal.toInt()
                        i = dLenNext

                        val partHeaderEnd = (i + headerLen).coerceAtMost(bytes.size)
                        var partContentType = ""
                        var partLocation = ""

                        if (i < partHeaderEnd) {
                            val firstByte = bytes[i].toInt() and 0xFF
                            when {
                                firstByte == 0x83 -> {
                                    partContentType = "text/plain"
                                    i++
                                }
                                firstByte == 0x9D -> {
                                    partContentType = "image/jpeg"
                                    i++
                                }
                                firstByte == 0xA0 -> {
                                    partContentType = "image/png"
                                    i++
                                }
                                firstByte == 0x8C -> {
                                    partContentType = "image/gif"
                                    i++
                                }
                                else -> {
                                    val str = readNullTerminatedString(bytes, i)
                                    partContentType = str.text
                                    i = str.nextIndex
                                }
                            }

                            // Scan remaining header bytes for Content-Location (0x8E)
                            while (i < partHeaderEnd) {
                                val hTag = bytes[i].toInt() and 0xFF
                                if (hTag == 0x8E) {
                                    i++
                                    val locStr = readNullTerminatedString(bytes, i)
                                    partLocation = locStr.text
                                    i = locStr.nextIndex
                                } else {
                                    i++
                                }
                            }
                        }
                        i = partHeaderEnd

                        val partDataEnd = (i + dataLen).coerceAtMost(bytes.size)
                        if (partDataEnd > i) {
                            val partData = bytes.copyOfRange(i, partDataEnd)

                            when {
                                partContentType.contains("text/plain", ignoreCase = true) -> {
                                    val decoded = String(partData, Charsets.UTF_8).trim()
                                    if (decoded.isNotBlank()) {
                                        bodyText = decoded
                                    }
                                }
                                isImageContent(partContentType, partData) -> {
                                    if (imageBytes == null) {
                                        imageBytes = partData
                                        imageMimeType = detectImageMimeType(partContentType, partData)
                                    }
                                }
                                isAudioContent(partContentType, partData) -> {
                                    if (audioBytes == null) {
                                        audioBytes = partData
                                        audioMimeType = if (partContentType.startsWith("audio/")) partContentType else "audio/amr"
                                    }
                                }
                                else -> {
                                    if (otherBytes == null && partData.isNotEmpty()) {
                                        otherBytes = partData
                                        otherMimeType = partContentType.ifBlank { "application/octet-stream" }
                                        otherFileName = partLocation.ifBlank {
                                            if (partContentType.contains("calendar", ignoreCase = true)) "event.ics" else "attachment.dat"
                                        }
                                    }
                                }
                            }
                        }
                        i = partDataEnd
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Multipart parser exception, proceeding to binary fallback", e)
            }
        }

        // 3. Robust Fallback: Scan entire PDU for embedded images or audio if multipart missed it
        if (imageBytes == null) {
            val (extractedBytes, mime) = extractImageBytesFallback(bytes)
            if (extractedBytes != null) {
                imageBytes = extractedBytes
                imageMimeType = mime
            }
        }
        if (audioBytes == null && imageBytes == null) {
            val extractedAudio = extractAudioBytesFallback(bytes)
            if (extractedAudio != null) {
                audioBytes = extractedAudio
                audioMimeType = "audio/amr"
            }
        }

        return MmsDownloadedMessage(
            senderAddress = senderAddress,
            timestampMillis = timestampMillis,
            subject = subject,
            bodyText = bodyText,
            imageBytes = imageBytes,
            imageMimeType = imageMimeType,
            audioBytes = audioBytes,
            audioMimeType = audioMimeType,
            otherBytes = otherBytes,
            otherMimeType = otherMimeType,
            otherFileName = otherFileName,
        )
    }

    /**
     * Compresses and downscales gallery photo to fit strictly within MMS constraints (max 299.9 kB)
     * adhering to the configured [MmsCreationMode] while preserving authentic photo colors.
     */
    fun downscaleImageForMms(
        context: Context,
        imageUri: Uri,
        creationMode: MmsCreationMode = MmsCreationMode.FREE,
        maxBytes: Int = when (creationMode) {
            MmsCreationMode.RESTRICTED -> 100 * 1024
            MmsCreationMode.WARNING -> 299_900
            MmsCreationMode.FREE -> 299_900
        },
    ): ByteArray? {
        val targetMaxBytes = minOf(maxBytes.toLong(), MmsImageCompressor.MAX_MMS_IMAGE_BYTES)
        return MmsImageCompressor.compressImageBytes(context, imageUri, targetMaxBytes)
    }

    private fun isImageContent(contentType: String, data: ByteArray): Boolean {
        if (contentType.startsWith("image/", ignoreCase = true)) return true
        if (data.size < 4) return false
        // JPEG magic bytes
        if (data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte()) return true
        // PNG magic bytes
        if (data[0] == 0x89.toByte() && data[1] == 0x50.toByte() && data[2] == 0x4E.toByte()) return true
        // GIF magic bytes
        if (data[0] == 0x47.toByte() && data[1] == 0x49.toByte() && data[2] == 0x46.toByte()) return true
        // WEBP magic bytes
        if (data.size >= 12 && data[0] == 'R'.code.toByte() && data[8] == 'W'.code.toByte()) return true
        return false
    }

    private fun detectImageMimeType(contentType: String, data: ByteArray): String {
        if (contentType.startsWith("image/", ignoreCase = true)) return contentType
        if (data.size >= 3 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte()) return "image/jpeg"
        if (data.size >= 4 && data[0] == 0x89.toByte() && data[1] == 0x50.toByte()) return "image/png"
        if (data.size >= 3 && data[0] == 'G'.code.toByte() && data[1] == 'I'.code.toByte()) return "image/gif"
        if (data.size >= 12 && data[0] == 'R'.code.toByte() && data[8] == 'W'.code.toByte()) return "image/webp"
        return "image/jpeg"
    }

    private fun isAudioContent(contentType: String, data: ByteArray): Boolean {
        if (contentType.startsWith("audio/", ignoreCase = true)) return true
        if (data.size >= 6 && data[0] == 0x23.toByte() && data[1] == 0x21.toByte() &&
            data[2] == 0x41.toByte() && data[3] == 0x4D.toByte() && data[4] == 0x52.toByte() && data[5] == 0x0A.toByte()
        ) {
            return true
        }
        return false
    }

    private fun extractAudioBytesFallback(bytes: ByteArray): ByteArray? {
        val amrHeader = byteArrayOf(0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A)
        val idx = indexOfSubarray(bytes, amrHeader)
        if (idx >= 0 && idx + 6 < bytes.size) {
            return bytes.copyOfRange(idx, bytes.size)
        }
        return null
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

    private fun extractImageBytesFallback(bytes: ByteArray): Pair<ByteArray?, String?> {
        // Search for JPEG SOI (0xFF, 0xD8, 0xFF) and EOI (0xFF, 0xD9)
        var soi = -1
        for (k in 0 until bytes.size - 3) {
            if (bytes[k] == 0xFF.toByte() && bytes[k + 1] == 0xD8.toByte() && bytes[k + 2] == 0xFF.toByte()) {
                soi = k
                break
            }
        }
        if (soi >= 0) {
            var eoi = -1
            for (k in bytes.size - 2 downTo soi + 2) {
                if (bytes[k] == 0xFF.toByte() && bytes[k + 1] == 0xD9.toByte()) {
                    eoi = k + 2
                    break
                }
            }
            if (eoi > soi) {
                return bytes.copyOfRange(soi, eoi) to "image/jpeg"
            }
        }

        // Search for PNG (0x89, 0x50, 0x4E, 0x47)
        for (k in 0 until bytes.size - 8) {
            if (bytes[k] == 0x89.toByte() && bytes[k + 1] == 0x50.toByte() && bytes[k + 2] == 0x4E.toByte() && bytes[k + 3] == 0x47.toByte()) {
                return bytes.copyOfRange(k, bytes.size) to "image/png"
            }
        }

        return null to null
    }

    fun extractUrlFromBytes(bytes: ByteArray): String? {
        val raw = String(bytes, Charsets.ISO_8859_1)
        val httpIndex = raw.indexOf("http://").takeIf { it >= 0 }
            ?: raw.indexOf("https://").takeIf { it >= 0 }
            ?: return null
        var end = httpIndex
        while (end < raw.length && raw[end] > ' ' && raw[end] != '\u0000' && raw[end] != '"') {
            end++
        }
        val url = raw.substring(httpIndex, end).trim()
        return if (url.length > 7) url else null
    }

    fun extractSenderFromBytes(bytes: ByteArray): String? {
        val raw = String(bytes, Charsets.ISO_8859_1)
        val typePlmn = raw.indexOf("/TYPE=PLMN")
        if (typePlmn > 0) {
            var start = typePlmn - 1
            while (start >= 0 && (raw[start].isDigit() || raw[start] == '+')) {
                start--
            }
            val phone = raw.substring(start + 1, typePlmn).trim()
            if (phone.isNotBlank()) return phone
        }
        return null
    }

    private fun readUintvar(bytes: ByteArray, start: Int): Pair<Long, Int> {
        var i = start
        var value = 0L
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            value = (value shl 7) or ((b and 0x7F).toLong())
            i++
            if ((b and 0x80) == 0) break
        }
        return value to i
    }

    private fun readValueLength(bytes: ByteArray, start: Int): ValueLength {
        if (start >= bytes.size) return ValueLength(0, start)
        val b = bytes[start].toInt() and 0xFF
        return if (b < 31) {
            ValueLength(b, start + 1)
        } else {
            val uint = readUintvar(bytes, start + 1)
            ValueLength(uint.first.toInt(), uint.second)
        }
    }

    private fun readLongInteger(bytes: ByteArray, start: Int): LongIntResult {
        if (start >= bytes.size) return LongIntResult(0L, start)
        val len = bytes[start].toInt() and 0xFF
        var i = start + 1
        return if (len in 1..8) {
            var v = 0L
            for (k in 0 until len) {
                if (i < bytes.size) {
                    v = (v shl 8) or ((bytes[i].toInt() and 0xFF).toLong())
                    i++
                }
            }
            LongIntResult(v, i)
        } else {
            LongIntResult((len and 0x7F).toLong(), i)
        }
    }

    private fun readNullTerminatedString(bytes: ByteArray, start: Int): StringResult {
        var i = start
        while (i < bytes.size && bytes[i] != 0.toByte()) {
            i++
        }
        val text = String(bytes, start, i - start, Charsets.UTF_8)
        if (i < bytes.size) i++
        return StringResult(text, i)
    }

    private fun readEncodedStringValue(bytes: ByteArray, start: Int): StringResult {
        if (start >= bytes.size) return StringResult("", start)
        val b = bytes[start].toInt() and 0xFF
        return if (b < 32) {
            val len = b
            val strStart = (start + 2).coerceAtMost(bytes.size)
            val strEnd = (start + 1 + len).coerceAtMost(bytes.size)
            val text = if (strStart < strEnd) {
                String(bytes, strStart, strEnd - strStart, Charsets.UTF_8).trimEnd('\u0000')
            } else ""
            StringResult(text, (start + 1 + len).coerceAtMost(bytes.size))
        } else {
            readNullTerminatedString(bytes, start)
        }
    }

    private fun writeUintvar(out: ByteArrayOutputStream, value: Long) {
        var v = value
        val buf = mutableListOf<Byte>()
        buf.add((v and 0x7F).toByte())
        v = v ushr 7
        while (v > 0) {
            buf.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        for (k in buf.indices.reversed()) {
            out.write(buf[k].toInt())
        }
    }

    private data class ValueLength(val length: Int, val nextIndex: Int)
    private data class LongIntResult(val value: Long, val nextIndex: Int)
    private data class StringResult(val text: String, val nextIndex: Int)

    private data class MmsPart(
        val contentType: String,
        val contentLocation: String,
        val contentId: String,
        val data: ByteArray,
    )
}
