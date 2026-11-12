package com.txtura.telephony

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale

sealed class MmsValidationError {
    object FileTooLarge : MmsValidationError()
    object UnsupportedFormat : MmsValidationError()
    data class Unknown(val message: String) : MmsValidationError()
}

object MmsPayloadValidator {
    const val MAX_MMS_PDU_SIZE = 307_200L // 300 KB limit for the entire PDU
    const val PDU_OVERHEAD_ESTIMATE = 8_192L // 8 KB reserved for SMIL, headers, and text parts

    private val MIME_MAP = mapOf(
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "doc" to "application/msword",
        "pdf" to "application/pdf",
        "odt" to "application/vnd.oasis.opendocument.text",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "xls" to "application/vnd.ms-excel",
        "csv" to "text/csv",
        "ods" to "application/vnd.oasis.opendocument.spreadsheet",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "ppt" to "application/vnd.ms-powerpoint",
        "ppsx" to "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
        "odp" to "application/vnd.oasis.opendocument.presentation",
        "txt" to "text/plain",
        "rtf" to "application/rtf",
        "md" to "text/markdown",
        "amr" to "audio/amr",
        "aac" to "audio/aac",
        "ogg" to "audio/ogg",
        "mp4" to "audio/mp4",
        "vcf" to "text/x-vcard",
        "ics" to "text/calendar",
        "vcs" to "text/calendar"
    )

    fun getMimeTypeFromExtension(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MIME_MAP[ext] ?: "application/octet-stream"
    }

    fun isSupportedLosslessDocument(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MIME_MAP.containsKey(ext)
    }

    /**
     * Pre-flight validation. Asserts that the document won't exceed the absolute limit
     * when packed into an MMS.
     */
    fun validateAttachment(context: Context, uri: Uri): MmsValidationError? {
        val size = DocumentAttachmentHelper.getFileSize(context, uri)
        if (size > (MAX_MMS_PDU_SIZE - PDU_OVERHEAD_ESTIMATE)) {
            return MmsValidationError.FileTooLarge
        }
        return null
    }
}
