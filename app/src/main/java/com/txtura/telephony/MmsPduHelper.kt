@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma", "ClarifyingParentheses")

package com.txtura.telephony

import android.content.Context
import android.net.Uri
import android.util.Log
import com.txtura.data.MmsCreationMode
import java.io.ByteArrayOutputStream
import java.util.Locale

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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MmsDownloadedMessage

        if (timestampMillis != other.timestampMillis) return false
        if (senderAddress != other.senderAddress) return false
        if (subject != other.subject) return false
        if (bodyText != other.bodyText) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false
        if (imageMimeType != other.imageMimeType) return false
        if (!audioBytes.contentEquals(other.audioBytes)) return false
        if (audioMimeType != other.audioMimeType) return false
        if (!otherBytes.contentEquals(other.otherBytes)) return false
        if (otherMimeType != other.otherMimeType) return false
        return otherFileName == other.otherFileName
    }

    override fun hashCode(): Int {
        var result = timestampMillis.hashCode()
        result = (31 * result) + senderAddress.hashCode()
        result = (31 * result) + subject.hashCode()
        result = (31 * result) + bodyText.hashCode()
        result = (31 * result) + (imageBytes?.contentHashCode() ?: 0)
        result = (31 * result) + imageMimeType.hashCode()
        result = (31 * result) + (audioBytes?.contentHashCode() ?: 0)
        result = (31 * result) + audioMimeType.hashCode()
        result = (31 * result) + (otherBytes?.contentHashCode() ?: 0)
        result = (31 * result) + otherMimeType.hashCode()
        result = (31 * result) + otherFileName.hashCode()
        return result
    }
}

/**
 * Robust on-device WAP MMS 1.2 / 1.3 (OMA MMS Encapsulation) PDU builder, parser, and media optimizer.
 * - Encodes standard `M-Send.req` binary PDUs with SMIL presentation for [android.telephony.SmsManager.sendMultimediaMessage].
 * - Decodes `M-Notification.ind` WAP Push PDUs from `WAP_PUSH_DELIVER`.
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

    // WSP Content-Type tokens & parameter IDs
    private const val CT_MULTIPART_RELATED = "application/vnd.wap.multipart.related"
    private const val CT_SMIL = "application/smil"
    private const val CT_IMAGE_JPEG = "image/jpeg"
    private const val CT_TEXT_PLAIN = "text/plain"
    private const val WSP_PARAM_CHARSET = 0x81
    private const val WSP_PARAM_TYPE = 0x89
    private const val WSP_PARAM_START = 0x8A
    private const val WSP_CHARSET_UTF8 = 0xEA

    /**
     * Builds a complete, OMA-compliant M-Send.req binary PDU with SMIL root presentation,
     * supporting voice messages (AMR/M4A/3GP/OGG), images, text, calendar invites, and documents.
     */
    @Suppress("UNUSED_PARAMETER")
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

        // 1. OMA MMS Headers
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
        val cleanRecipient = if (recipient.endsWith("/TYPE=PLMN", ignoreCase = true)) {
            recipient
        } else {
            "$recipient/TYPE=PLMN"
        }
        out.write(cleanRecipient.toByteArray(Charsets.US_ASCII))
        out.write(0x00)

        if (!bodyText.isNullOrBlank()) {
            val preview = bodyText.take(40)
            val previewBytes = preview.toByteArray(Charsets.UTF_8)
            out.write(HDR_SUBJECT)
            writeValueLength(out, previewBytes.size + 2)
            out.write(WSP_CHARSET_UTF8)
            out.write(previewBytes)
            out.write(0x00)
        }

        out.write(HDR_DELIVERY_REPORT)
        out.write(if (requestDeliveryReport) VAL_REPORT_YES else VAL_REPORT_NO)

        out.write(HDR_READ_REPORT)
        out.write(if (requestReadReport) VAL_REPORT_YES else VAL_REPORT_NO)

        // WSP-compliant Content-Type: application/vnd.wap.multipart.related; type="application/smil"; start="<smil>"
        out.write(HDR_CONTENT_TYPE)
        val ctParamsStream = ByteArrayOutputStream().apply {
            write(CT_MULTIPART_RELATED.toByteArray(Charsets.US_ASCII))
            write(0x00)
            write(WSP_PARAM_TYPE)
            write(CT_SMIL.toByteArray(Charsets.US_ASCII))
            write(0x00)
            write(WSP_PARAM_START)
            write("<smil>".toByteArray(Charsets.US_ASCII))
            write(0x00)
        }
        val ctHeaderBytes = ctParamsStream.toByteArray()
        writeValueLength(out, ctHeaderBytes.size)
        out.write(ctHeaderBytes)

        // 2. Build SMIL and Media Multipart Body Parts
        val parts = mutableListOf<MmsPart>()
        val smilParElements = StringBuilder()

        val hasImage = imageBytes != null && imageBytes.isNotEmpty()
        val hasAudio = audioBytes != null && audioBytes.isNotEmpty()
        val hasOther = otherBytes != null && otherBytes.isNotEmpty()
        val hasText = !bodyText.isNullOrBlank()

        if (hasImage) {
            val imgExt = when {
                imageMimeType.contains("png", ignoreCase = true) -> "png"
                imageMimeType.contains("gif", ignoreCase = true) -> "gif"
                imageMimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val imgLoc = "image_01.$imgExt"
            smilParElements.append("<img src=\"$imgLoc\" region=\"Image\"/>")
        }

        if (hasAudio) {
            val audExt = extensionForAudioMime(audioMimeType)
            val audLoc = "voice_01.$audExt"
            smilParElements.append("<audio src=\"$audLoc\"/>")
        }

        if (hasText) {
            smilParElements.append("<text src=\"text_01.txt\" region=\"Text\"/>")
        }

        if (hasOther) {
            val safeRef = (otherFileName?.ifBlank { "attachment.dat" } ?: "attachment.dat")
                .replace("\"", "_")
            smilParElements.append("<ref src=\"$safeRef\"/>")
        }

        val smilXml = buildString {
            append("<smil><head><layout>")
            append("<root-layout width=\"100%\" height=\"100%\"/>")
            append("<region id=\"Image\" top=\"0%\" left=\"0%\" height=\"80%\" width=\"100%\" fit=\"meet\"/>")
            append("<region id=\"Text\" top=\"80%\" left=\"0%\" height=\"20%\" width=\"100%\" fit=\"scroll\"/>")
            append("</layout></head><body><par dur=\"10000ms\">")
            append(smilParElements)
            append("</par></body></smil>")
        }

        parts.add(
            MmsPart(
                contentType = CT_SMIL,
                contentLocation = "smil.xml",
                contentId = "<smil>",
                data = smilXml.toByteArray(Charsets.UTF_8),
                isUtf8Text = false
            )
        )

        if (!bodyText.isNullOrBlank()) {
            parts.add(
                MmsPart(
                    contentType = CT_TEXT_PLAIN,
                    contentLocation = "text_01.txt",
                    contentId = "<text_01>",
                    data = bodyText.toByteArray(Charsets.UTF_8),
                    isUtf8Text = true
                )
            )
        }

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val imgExt = when {
                imageMimeType.contains("png", ignoreCase = true) -> "png"
                imageMimeType.contains("gif", ignoreCase = true) -> "gif"
                imageMimeType.contains("webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            parts.add(
                MmsPart(
                    contentType = imageMimeType,
                    contentLocation = "image_01.$imgExt",
                    contentId = "<image_01>",
                    data = imageBytes,
                    isUtf8Text = false
                )
            )
        }

        if (audioBytes != null && audioBytes.isNotEmpty()) {
            val audExt = extensionForAudioMime(audioMimeType)
            parts.add(
                MmsPart(
                    contentType = audioMimeType.ifBlank { "audio/amr" },
                    contentLocation = "voice_01.$audExt",
                    contentId = "<voice_01>",
                    data = audioBytes,
                    isUtf8Text = false
                )
            )
        }

        if (otherBytes != null && otherBytes.isNotEmpty()) {
            val fName = otherFileName?.ifBlank { "attachment.dat" } ?: "attachment.dat"
            val resolvedMime = otherMimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
                ?: MmsPayloadValidator.getMimeTypeFromExtension(fName)
            parts.add(
                MmsPart(
                    contentType = resolvedMime,
                    contentLocation = fName,
                    contentId = "<attachment_01>",
                    data = otherBytes,
                    isUtf8Text = resolvedMime.startsWith("text/", ignoreCase = true)
                )
            )
        }

        writeUintvar(out, parts.size.toLong())

        for (part in parts) {
            val headerStream = ByteArrayOutputStream()

            if (part.isUtf8Text) {
                val ctBytes = ByteArrayOutputStream().apply {
                    write(part.contentType.toByteArray(Charsets.US_ASCII))
                    write(0x00)
                    write(WSP_PARAM_CHARSET)
                    write(WSP_CHARSET_UTF8)
                }.toByteArray()
                writeValueLength(headerStream, ctBytes.size)
                headerStream.write(ctBytes)
            } else {
                headerStream.write(part.contentType.toByteArray(Charsets.US_ASCII))
                headerStream.write(0x00)
            }

            // Content-Location (0x8E)
            headerStream.write(0x8E)
            headerStream.write(part.contentLocation.toByteArray(Charsets.US_ASCII))
            headerStream.write(0x00)

            // Content-ID (0xC0)
            headerStream.write(0xC0)
            headerStream.write(part.contentId.toByteArray(Charsets.US_ASCII))
            headerStream.write(0x00)

            val partHeaders = headerStream.toByteArray()

            writeUintvar(out, partHeaders.size.toLong())
            writeUintvar(out, part.data.size.toLong())
            out.write(partHeaders)
            out.write(part.data)
        }

        val pduBytes = out.toByteArray()
        if (pduBytes.size > MmsPayloadValidator.MAX_MMS_PDU_SIZE) {
            Log.w(TAG, "Generated PDU size (${pduBytes.size} bytes) exceeds strict limit of ${MmsPayloadValidator.MAX_MMS_PDU_SIZE} bytes")
        }
        return pduBytes
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
            when (bytes[i].toInt() and 0xFF) {
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
                    i += 2
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
     * Extracts sender address, subject, text message body, voice messages, images, calendar events, and documents.
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

        // 1. Parse Main MMS Headers
        while (i < bytes.size) {
            val tag = bytes[i].toInt() and 0xFF

            if (tag == HDR_CONTENT_TYPE) {
                i++
                i = skipWspContentType(bytes, i, bytes.size)
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
                    val str = readEncodedStringValue(bytes, i)
                    i = str.nextIndex
                }
                else -> {
                    i++
                }
            }
        }

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
                    var partIdx = 0
                    while (partIdx < numParts && i < bytes.size) {
                        partIdx++
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
                            val parsedCt = parsePartContentTypeAndParams(bytes, i, partHeaderEnd)
                            partContentType = parsedCt.mimeType
                            if (parsedCt.paramFileName.isNotBlank()) {
                                partLocation = parsedCt.paramFileName
                            }
                            i = parsedCt.nextIndex

                            // Scan remaining part headers for Content-Location (0x8E) or Content-Disposition filename
                            while (i < partHeaderEnd) {
                                val hTag = bytes[i].toInt() and 0xFF
                                if (hTag == 0x8E) {
                                    i++
                                    val locStr = readNullTerminatedStringBounded(bytes, i, partHeaderEnd)
                                    if (locStr.text.isNotBlank()) {
                                        partLocation = locStr.text
                                    }
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
                            val cleanMime = partContentType.substringBefore(';').trim().lowercase(Locale.ROOT)
                            val cleanLoc = partLocation.substringAfterLast('/').trim()

                            when {
                                cleanMime == "application/smil" ||
                                    cleanMime == "text/vnd.wap.wml" ||
                                    cleanMime.startsWith("application/vnd.wap.multipart") ||
                                    cleanLoc.endsWith(".smil", ignoreCase = true) ||
                                    cleanLoc.equals("smil.xml", ignoreCase = true) -> {
                                    // Skip presentation SMIL layout part
                                }

                                isAudioContent(cleanMime, cleanLoc, partData) -> {
                                    if (audioBytes == null) {
                                        audioBytes = partData
                                        audioMimeType = detectAudioMimeType(cleanMime, cleanLoc, partData)
                                    }
                                }

                                isImageContent(cleanMime, partData) -> {
                                    if (imageBytes == null) {
                                        imageBytes = partData
                                        imageMimeType = detectImageMimeType(cleanMime, partData)
                                    }
                                }

                                cleanMime == "text/plain" -> {
                                    val decoded = String(partData, Charsets.UTF_8).trim().trimEnd('\u0000')
                                    val isDefaultSlideText = cleanLoc.isBlank() ||
                                        cleanLoc.equals("text_01.txt", ignoreCase = true) ||
                                        cleanLoc.equals("text_0.txt", ignoreCase = true) ||
                                        cleanLoc.equals("text.txt", ignoreCase = true) ||
                                        cleanLoc.equals("body.txt", ignoreCase = true)

                                    if (!isDefaultSlideText && cleanLoc.endsWith(".txt", ignoreCase = true) && otherBytes == null) {
                                        otherBytes = partData
                                        otherMimeType = "text/plain"
                                        otherFileName = cleanLoc
                                        if (bodyText.isNullOrBlank() && decoded.isNotBlank()) {
                                            bodyText = decoded.take(120)
                                        }
                                    } else if (decoded.isNotBlank() && !decoded.startsWith("<smil", ignoreCase = true)) {
                                        bodyText = if (bodyText.isNullOrBlank()) decoded else "$bodyText\n$decoded"
                                    }
                                }

                                else -> {
                                    val asText = if (partData.size < 2048) {
                                        String(partData, Charsets.ISO_8859_1).trimStart()
                                    } else ""
                                    if (!asText.startsWith("<smil", ignoreCase = true) && otherBytes == null && partData.isNotEmpty()) {
                                        otherBytes = partData
                                        val effectiveFileName = cleanLoc.ifBlank {
                                            if (cleanMime.contains("calendar")) "event.ics" else "attachment.dat"
                                        }
                                        otherFileName = effectiveFileName
                                        otherMimeType = cleanMime.takeIf {
                                            it.isNotBlank() && it != "application/octet-stream"
                                        } ?: MmsPayloadValidator.getMimeTypeFromExtension(effectiveFileName)
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

        // 3. Binary Fallback: Scan PDU for embedded images or audio if header offsets were non-standard
        if (imageBytes == null && audioBytes == null && otherBytes == null) {
            val (extractedBytes, mime) = extractImageBytesFallback(bytes)
            if (extractedBytes != null) {
                imageBytes = extractedBytes
                imageMimeType = mime
            }
        }
        if (audioBytes == null && imageBytes == null && otherBytes == null) {
            val extractedAudio = extractAudioBytesFallback(bytes)
            if (extractedAudio != null) {
                audioBytes = extractedAudio.first
                audioMimeType = extractedAudio.second
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

    fun extensionForAudioMime(mimeType: String?): String {
        val lower = mimeType.orEmpty().lowercase(Locale.ROOT)
        return when {
            lower.contains("mp4") || lower.contains("m4a") -> "m4a"
            lower.contains("3gpp") || lower.contains("3gp") -> "3gp"
            lower.contains("mpeg") || lower.contains("mp3") -> "mp3"
            lower.contains("ogg") || lower.contains("opus") -> "ogg"
            lower.contains("aac") -> "aac"
            lower.contains("wav") -> "wav"
            else -> "amr"
        }
    }

    fun isAudioContentType(contentType: String?, locationOrName: String?): Boolean {
        val ct = contentType.orEmpty().lowercase(Locale.ROOT)
        if (ct.startsWith("audio/") || ct == "application/ogg") return true
        val loc = locationOrName.orEmpty().lowercase(Locale.ROOT)
        return loc.endsWith(".amr") || loc.endsWith(".3gp") || loc.endsWith(".3gpp") ||
            loc.endsWith(".m4a") || loc.endsWith(".aac") || loc.endsWith(".mp3") ||
            loc.endsWith(".ogg") || loc.endsWith(".opus") || loc.endsWith(".wav")
    }

    private fun skipWspContentType(bytes: ByteArray, start: Int, limit: Int): Int {
        if (start >= limit) return limit
        val first = bytes[start].toInt() and 0xFF
        return when {
            first < 31 -> (start + 1 + first).coerceAtMost(limit)
            first == 31 -> {
                val (len, nextIdx) = readUintvar(bytes, start + 1)
                (nextIdx + len.toInt()).coerceAtMost(limit)
            }
            (first and 0x80) != 0 -> (start + 1).coerceAtMost(limit)
            else -> readNullTerminatedStringBounded(bytes, start, limit).nextIndex
        }
    }

    private data class ParsedContentType(
        val mimeType: String,
        val paramFileName: String,
        val nextIndex: Int,
    )

    private fun parsePartContentTypeAndParams(bytes: ByteArray, start: Int, limit: Int): ParsedContentType {
        if (start >= limit) return ParsedContentType("", "", limit)
        val first = bytes[start].toInt() and 0xFF
        var idx = start
        var ctEnd = limit
        var hasValueLength = false

        if (first < 31) {
            hasValueLength = true
            idx = start + 1
            ctEnd = (idx + first).coerceAtMost(limit)
        } else if (first == 31) {
            hasValueLength = true
            val (len, nextIdx) = readUintvar(bytes, start + 1)
            idx = nextIdx
            ctEnd = (idx + len.toInt()).coerceAtMost(limit)
        }

        if (idx >= ctEnd) {
            return ParsedContentType("", "", ctEnd)
        }

        val mediaByte = bytes[idx].toInt() and 0xFF
        val mimeType: String
        if ((mediaByte and 0x80) != 0) {
            mimeType = wellKnownMimeFromShortInt(mediaByte)
            idx++
        } else {
            val str = readNullTerminatedStringBounded(bytes, idx, ctEnd)
            mimeType = str.text
            idx = str.nextIndex
        }

        var paramName = ""
        if (hasValueLength) {
            while (idx < ctEnd) {
                when (bytes[idx].toInt() and 0xFF) {
                    0x85, 0x97, 0x8E -> {
                        idx++
                        val nameRes = readNullTerminatedStringBounded(bytes, idx, ctEnd)
                        if (nameRes.text.isNotBlank()) {
                            paramName = nameRes.text
                        }
                        idx = nameRes.nextIndex
                    }
                    WSP_PARAM_CHARSET -> {
                        idx += 2
                    }
                    else -> {
                        idx++
                    }
                }
            }
            idx = ctEnd
        }

        return ParsedContentType(mimeType = mimeType, paramFileName = paramName, nextIndex = idx.coerceAtMost(limit))
    }

    private fun wellKnownMimeFromShortInt(token: Int): String {
        return when (token and 0xFF) {
            0x83 -> "text/plain"
            0x86 -> "text/x-vCalendar"
            0x87 -> "text/x-vCard"
            0x9D, 0x9E -> "image/jpeg"
            0x9F -> "image/tiff"
            0xA0 -> "image/png"
            0x8C -> "image/gif"
            0xA1 -> "image/vnd.wap.wbmp"
            0xA2 -> "application/vnd.wap.multipart.mixed"
            0xB3 -> "application/vnd.wap.multipart.related"
            else -> "application/octet-stream"
        }
    }

    private fun isImageContent(contentType: String, data: ByteArray): Boolean {
        if (contentType.startsWith("image/", ignoreCase = true)) return true
        if (data.size < 4) return false
        if (data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte()) return true
        if (data[0] == 0x89.toByte() && data[1] == 0x50.toByte() && data[2] == 0x4E.toByte()) return true
        if (data[0] == 0x47.toByte() && data[1] == 0x49.toByte() && data[2] == 0x46.toByte()) return true
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

    private fun isAudioContent(contentType: String, location: String, data: ByteArray): Boolean {
        if (contentType.startsWith("audio/", ignoreCase = true) ||
            contentType.equals("application/ogg", ignoreCase = true)
        ) {
            return true
        }
        val lowerLoc = location.lowercase(Locale.ROOT)
        if (lowerLoc.endsWith(".amr") || lowerLoc.endsWith(".3gp") || lowerLoc.endsWith(".3gpp") ||
            lowerLoc.endsWith(".m4a") || lowerLoc.endsWith(".aac") || lowerLoc.endsWith(".mp3") ||
            lowerLoc.endsWith(".ogg") || lowerLoc.endsWith(".opus") || lowerLoc.endsWith(".wav")
        ) {
            return true
        }
        // AMR-NB / AMR-WB header "#!AMR"
        if (data.size >= 5 && data[0] == 0x23.toByte() && data[1] == 0x21.toByte() &&
            data[2] == 0x41.toByte() && data[3] == 0x4D.toByte() && data[4] == 0x52.toByte()
        ) {
            return true
        }
        // OGG header "OggS"
        if (data.size >= 4 && data[0] == 'O'.code.toByte() && data[1] == 'g'.code.toByte() &&
            data[2] == 'g'.code.toByte() && data[3] == 'S'.code.toByte()
        ) {
            return true
        }
        // MP4 / M4A / 3GP box header "ftyp" at offset 4
        if (data.size >= 12 && data[4] == 'f'.code.toByte() && data[5] == 't'.code.toByte() &&
            data[6] == 'y'.code.toByte() && data[7] == 'p'.code.toByte()
        ) {
            return true
        }
        return false
    }

    private fun detectAudioMimeType(contentType: String, location: String, data: ByteArray): String {
        if (contentType.startsWith("audio/", ignoreCase = true)) return contentType
        val lowerLoc = location.lowercase(Locale.ROOT)
        return when {
            lowerLoc.endsWith(".m4a") || lowerLoc.endsWith(".mp4") -> "audio/mp4"
            lowerLoc.endsWith(".3gp") || lowerLoc.endsWith(".3gpp") -> "audio/3gpp"
            lowerLoc.endsWith(".mp3") -> "audio/mpeg"
            lowerLoc.endsWith(".ogg") || lowerLoc.endsWith(".opus") || contentType.contains("ogg", ignoreCase = true) -> "audio/ogg"
            lowerLoc.endsWith(".aac") -> "audio/aac"
            lowerLoc.endsWith(".wav") -> "audio/wav"
            data.size >= 4 && data[0] == 'O'.code.toByte() && data[1] == 'g'.code.toByte() -> "audio/ogg"
            data.size >= 12 && data[4] == 'f'.code.toByte() && data[5] == 't'.code.toByte() -> "audio/mp4"
            else -> "audio/amr"
        }
    }

    private fun extractAudioBytesFallback(bytes: ByteArray): Pair<ByteArray, String>? {
        val amrHeader = byteArrayOf(0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A)
        val idx = indexOfSubarray(bytes, amrHeader)
        if (idx >= 0 && idx + 6 < bytes.size) {
            return bytes.copyOfRange(idx, bytes.size) to "audio/amr"
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

    private fun writeValueLength(out: ByteArrayOutputStream, length: Int) {
        if (length < 31) {
            out.write(length)
        } else {
            out.write(31)
            writeUintvar(out, length.toLong())
        }
    }

    private fun readLongInteger(bytes: ByteArray, start: Int): LongIntResult {
        if (start >= bytes.size) return LongIntResult(0L, start)
        val len = bytes[start].toInt() and 0xFF
        var i = start + 1
        return if (len in 1..8) {
            var v = 0L
            repeat(len) {
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
        return readNullTerminatedStringBounded(bytes, start, bytes.size)
    }

    private fun readNullTerminatedStringBounded(bytes: ByteArray, start: Int, limit: Int): StringResult {
        val safeLimit = limit.coerceAtMost(bytes.size)
        var i = start.coerceAtMost(safeLimit)
        while (i < safeLimit && bytes[i] != 0.toByte()) {
            i++
        }
        val text = if (i > start) {
            String(bytes, start, i - start, Charsets.UTF_8)
        } else {
            ""
        }
        if (i < safeLimit && bytes[i] == 0.toByte()) i++
        return StringResult(text, i)
    }

    private fun readEncodedStringValue(bytes: ByteArray, start: Int): StringResult {
        if (start >= bytes.size) return StringResult("", start)
        val b = bytes[start].toInt() and 0xFF
        return if (b < 32) {
            val vLen = readValueLength(bytes, start)
            val strStart = (vLen.nextIndex + 1).coerceAtMost(bytes.size)
            val strEnd = (vLen.nextIndex + vLen.length).coerceAtMost(bytes.size)
            val text = if (strStart < strEnd) {
                String(bytes, strStart, strEnd - strStart, Charsets.UTF_8).trimEnd('\u0000')
            } else ""
            StringResult(text, strEnd)
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
        val isUtf8Text: Boolean = false,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MmsPart

            if (contentType != other.contentType) return false
            if (contentLocation != other.contentLocation) return false
            if (contentId != other.contentId) return false
            if (isUtf8Text != other.isUtf8Text) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = contentType.hashCode()
            result = 31 * result + contentLocation.hashCode()
            result = 31 * result + contentId.hashCode()
            result = 31 * result + isUtf8Text.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}
