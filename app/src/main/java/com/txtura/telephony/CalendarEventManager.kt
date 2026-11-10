package com.txtura.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val description: String?,
    val location: String?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)

/**
 * Manages reading device calendar events (meeting invites, birthdays, reminders)
 * and converting them to standard vCalendar / iCalendar (.ics / .vcs) format under 299.9 kB for MMS.
 */
object CalendarEventManager {

    private const val TAG = "CalendarEventManager"
    const val MAX_CALENDAR_BYTES = 299_900L // 299.9 kB

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Queries device calendar events (meetings, reminders, birthdays) from CalendarContract.
     */
    fun getDeviceCalendarEvents(context: Context): List<CalendarEventItem> {
        if (!hasCalendarPermission(context)) return emptyList()

        val events = mutableListOf<CalendarEventItem>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )

        // Query upcoming and recent events (from 7 days ago to 90 days in future)
        val now = System.currentTimeMillis()
        val windowStart = now - (7L * 24 * 60 * 60 * 1000L)
        val selection = "${CalendarContract.Events.DTSTART} >= ?"
        val selectionArgs = arrayOf(windowStart.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)

                while (cursor.moveToNext() && events.size < 50) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val rawTitle = if (titleIdx >= 0) cursor.getString(titleIdx) else null
                    val title = rawTitle?.takeIf { it.isNotBlank() } ?: "Događaj"
                    val desc = if (descIdx >= 0) cursor.getString(descIdx) else null
                    val location = if (locIdx >= 0) cursor.getString(locIdx) else null
                    val start = if (startIdx >= 0) cursor.getLong(startIdx) else 0L
                    val end = if (endIdx >= 0) cursor.getLong(endIdx) else (start + 3600_000L)
                    val allDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false

                    if (start > 0L) {
                        events.add(
                            CalendarEventItem(
                                id = id,
                                title = title,
                                description = desc,
                                location = location,
                                startMillis = start,
                                endMillis = end,
                                allDay = allDay
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying device calendar events", e)
        }

        return events
    }

    /**
     * Serializes a calendar event to standard RFC 5545 iCalendar (.ics) format.
     */
    fun generateVCalendarText(event: CalendarEventItem): String {
        val sdfUtc = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sdfDate = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val nowStr = sdfUtc.format(Date())
        val uid = "txtura_event_${event.id}_${System.currentTimeMillis()}@txtura.app"

        val dtStart = if (event.allDay) {
            ";VALUE=DATE:${sdfDate.format(Date(event.startMillis))}"
        } else {
            ":${sdfUtc.format(Date(event.startMillis))}"
        }

        val dtEnd = if (event.allDay) {
            ";VALUE=DATE:${sdfDate.format(Date(event.endMillis))}"
        } else {
            ":${sdfUtc.format(Date(event.endMillis))}"
        }

        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Txtura//Txtura Android MMS//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTAMP:$nowStr")
            appendLine("DTSTART$dtStart")
            appendLine("DTEND$dtEnd")
            appendLine("SUMMARY:${event.title.replace("\n", " ").trim()}")
            if (!event.description.isNullOrBlank()) {
                appendLine("DESCRIPTION:${event.description.replace("\n", "\\n").trim()}")
            }
            if (!event.location.isNullOrBlank()) {
                appendLine("LOCATION:${event.location.replace("\n", " ").trim()}")
            }
            appendLine("STATUS:CONFIRMED")
            appendLine("END:VEVENT")
            appendLine("END:VCALENDAR")
        }
    }

    /**
     * Generates a vCalendar (.ics) file from an event and returns a FileProvider URI for MMS.
     */
    fun createVCalendarFileUri(context: Context, event: CalendarEventItem): Pair<Uri, String> {
        val icsText = generateVCalendarText(event)
        val cleanTitle = event.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24).ifBlank { "event" }
        val fileName = "$cleanTitle.ics"

        val cacheDir = File(context.cacheDir, "mms_attachments").apply { mkdirs() }
        val file = File(cacheDir, "mms_cal_${System.currentTimeMillis()}_$fileName")
        file.writeText(icsText, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return uri to fileName
    }

    /**
     * Creates a quick vCalendar (.ics) invitation on the fly without needing Calendar permission.
     */
    fun createQuickInvitationUri(
        context: Context,
        title: String,
        startMillis: Long,
        endMillis: Long = startMillis + 3600_000L,
        location: String? = null,
        description: String? = null
    ): Pair<Uri, String> {
        val event = CalendarEventItem(
            id = System.currentTimeMillis(),
            title = title.ifBlank { "Sastanak / Podsetnik" },
            description = description,
            location = location,
            startMillis = startMillis,
            endMillis = endMillis,
            allDay = false
        )
        return createVCalendarFileUri(context, event)
    }
}
