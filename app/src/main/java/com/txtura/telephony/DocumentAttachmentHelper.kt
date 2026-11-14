package com.txtura.telephony

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.txtura.ui.i18n.TxturaStrings
import java.io.File

/**
 * Helper for picking, validating (<= 299.9 kB) and opening documents
 * (PDF, TXT, XLS, DOC, etc.) and calendar files (.ics, .vcs) via MMS.
 */
object DocumentAttachmentHelper {

    private const val TAG = "DocumentAttachmentHelper"
    const val MAX_DOCUMENT_MMS_BYTES = 299_900L // 299.9 kB exact MMS limit

    fun getFileName(context: Context, uri: Uri): String {
        var name: String? = null
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            name = cursor.getString(idx)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve file name for $uri", e)
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/') ?: "document_${System.currentTimeMillis()}"
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        try {
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (idx >= 0) {
                            val size = cursor.getLong(idx)
                            if (size > 0L) return size
                        }
                    }
                }
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    val len = afd.length
                    if (len > 0L) return len
                }
            } else if (uri.scheme == "file") {
                uri.path?.let { File(it).length() }?.takeIf { it > 0L }?.let { return it }
            }
        } catch (_: Exception) {}
        return 0L
    }

    /**
     * Validates that the selected file fits within the active [com.txtura.data.MmsCreationMode] limit
     * (100 kB in RESTRICTED mode, 299.9 kB in WARNING/FREE modes), shows an advisory toast in WARNING mode
     * for non-core carrier extensions (.xls, .xlsx, .doc, .docx, .pdf), and copies it into cache.
     * Returns Pair(cachedFileProviderUri, fileName) on success, or Pair(null, errorMessage) on failure.
     */
    fun processDocumentUri(context: Context, uri: Uri): Pair<Uri?, String?> {
        val fileName = getFileName(context, uri)
        val reportedSize = getFileSize(context, uri)
        val creationMode = com.txtura.data.SmsMmsRepository.getMmsCreationMode(context)
        val maxAllowedBytes = if (creationMode == com.txtura.data.MmsCreationMode.RESTRICTED) {
            100L * 1024L
        } else {
            MAX_DOCUMENT_MMS_BYTES
        }

        val validationError = MmsPayloadValidator.validateAttachment(context, uri)
        if (validationError is MmsValidationError.FileTooLarge || (reportedSize > maxAllowedBytes)) {
            val sizeKb = (reportedSize / 1024.0).let { "%.1f".format(it) }
            val limitKb = if (creationMode == com.txtura.data.MmsCreationMode.RESTRICTED) "100 kB" else "299.9 kB"
            val errorMsg = if (TxturaStrings.isSerbianLocale()) {
                "Fajl premašuje limit od $limitKb za aktivni MMS režim (veličina: $sizeKb kB)."
            } else {
                "File exceeds $limitKb MMS mode limit (size: $sizeKb kB)."
            }
            return null to errorMsg
        }

        return try {
            val cacheDir = File(context.cacheDir, "mms_attachments").apply { mkdirs() }
            val cleanName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(cacheDir, "mms_doc_${System.currentTimeMillis()}_$cleanName")

            val bytesRead = context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: 0L

            if (bytesRead > maxAllowedBytes) {
                targetFile.delete()
                val sizeKb = (bytesRead / 1024.0).let { "%.1f".format(it) }
                val limitKb = if (creationMode == com.txtura.data.MmsCreationMode.RESTRICTED) "100 kB" else "299.9 kB"
                val errorMsg = if (TxturaStrings.isSerbianLocale()) {
                    "Fajl premašuje limit od $limitKb za MMS (veličina: $sizeKb kB)."
                } else {
                    "File exceeds $limitKb MMS limit (size: $sizeKb kB)."
                }
                return null to errorMsg
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                val errorMsg = if (TxturaStrings.isSerbianLocale()) "Greška pri čitanju fajla." else "Failed to read file."
                return null to errorMsg
            }

            if (creationMode == com.txtura.data.MmsCreationMode.WARNING) {
                val lower = fileName.lowercase()
                val isNonCoreDoc = lower.endsWith(".xls") || lower.endsWith(".xlsx") ||
                    lower.endsWith(".doc") || lower.endsWith(".docx") ||
                    lower.endsWith(".pdf") || lower.endsWith(".ppt") || lower.endsWith(".pptx")
                if (isNonCoreDoc || bytesRead > 100L * 1024L) {
                    val warnText = if (TxturaStrings.isSerbianLocale()) {
                        "Napomena: Pojedini mobilni operateri mogu ograničiti MMS dokumente veće od 100 kB ili Office/PDF formate."
                    } else {
                        "Note: Some mobile carriers may restrict MMS documents over 100 kB or Office/PDF formats."
                    }
                    Toast.makeText(context, warnText, Toast.LENGTH_LONG).show()
                }
            }

            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )
            fileProviderUri to fileName
        } catch (e: Exception) {
            Log.e(TAG, "Error caching document for MMS", e)
            val errorMsg = if (TxturaStrings.isSerbianLocale()) "Neuspešno prilaganje fajla." else "Failed to attach file."
            null to errorMsg
        }
    }

    /**
     * Opens an attached document or calendar file using the standard Android chooser
     * with the default apps already installed on the phone.
     */
    fun openAttachmentInExternalApp(
        context: Context,
        uri: Uri,
        mimeType: String?,
        fallbackName: String? = null
    ) {
        try {
            val resolvedMime = when {
                mimeType?.contains("calendar", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".ics", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".vcs", ignoreCase = true) == true -> "text/calendar"
                mimeType?.contains("pdf", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".pdf", ignoreCase = true) == true -> "application/pdf"
                mimeType?.contains("sheet", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".xls", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".xlsx", ignoreCase = true) == true -> "application/vnd.ms-excel"
                mimeType?.contains("word", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".doc", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".docx", ignoreCase = true) == true -> "application/msword"
                mimeType?.contains("text", ignoreCase = true) == true ||
                    fallbackName?.endsWith(".txt", ignoreCase = true) == true -> "text/plain"
                !mimeType.isNullOrBlank() && mimeType != "application/octet-stream" -> mimeType
                else -> try { context.contentResolver.getType(uri) } catch (_: Exception) { null } ?: "*/*"
            }

            // If uri comes from telephony provider (content://mms/part/...), copy to cache so external apps have access
            val shareableUri = if (uri.scheme == "content" &&
                (uri.authority?.contains("telephony") == true || uri.authority == "mms" || uri.path?.contains("/part/") == true)
            ) {
                val cacheDir = File(context.cacheDir, "mms_open").apply { mkdirs() }
                val ext = when (resolvedMime) {
                    "text/calendar" -> if (fallbackName?.endsWith(".vcs", ignoreCase = true) == true) "vcs" else "ics"
                    "application/pdf" -> "pdf"
                    "application/vnd.ms-excel" -> if (fallbackName?.endsWith(".xlsx", ignoreCase = true) == true) "xlsx" else "xls"
                    "application/msword" -> if (fallbackName?.endsWith(".docx", ignoreCase = true) == true) "docx" else "doc"
                    "text/plain" -> "txt"
                    else -> fallbackName?.substringAfterLast('.', "dat") ?: "dat"
                }
                val baseName = fallbackName?.substringBeforeLast('.')?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "file"
                val targetFile = File(cacheDir, "${baseName}_${uri.toString().hashCode()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
            } else {
                uri
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(shareableUri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserTitle = if (TxturaStrings.isSerbianLocale()) "Otvori pomoću:" else "Open with:"
            val chooser = Intent.createChooser(intent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open document with external app", e)
            val msg = if (TxturaStrings.isSerbianLocale()) {
                "Nije pronađena aplikacija za otvaranje ovog fajla."
            } else {
                "No installed app found to open this file."
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
