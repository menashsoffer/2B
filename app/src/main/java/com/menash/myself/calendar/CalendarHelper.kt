package com.menash.myself.calendar

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.menash.myself.data.models.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CalendarHelper {

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getTodayEvents(context: Context): List<CalendarEvent> {
        if (!hasCalendarPermission(context)) {
            return emptyList()
        }

        val events = mutableListOf<CalendarEvent>()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ALL_DAY
        )

        val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?) OR (${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DTEND} >= ?)"
        val selectionArgs = arrayOf(
            startOfDay.toString(),
            endOfDay.toString(),
            startOfDay.toString(),
            endOfDay.toString()
        )

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = it.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val allDayIdx = it.getColumnIndex(CalendarContract.Events.ALL_DAY)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) it.getString(titleIdx) ?: "" else ""
                    val start = if (startIdx >= 0) it.getLong(startIdx) else 0L
                    val end = if (endIdx >= 0) it.getLong(endIdx) else 0L
                    val loc = if (locIdx >= 0) it.getString(locIdx) ?: "" else ""
                    val allDay = if (allDayIdx >= 0) it.getInt(allDayIdx) == 1 else false

                    val startStr = if (allDay) "כל היום" else if (start > 0) timeFormat.format(Date(start)) else ""
                    val endStr = if (allDay) "" else if (end > 0) timeFormat.format(Date(end)) else ""

                    events.add(
                        CalendarEvent(
                            id = id,
                            title = title,
                            startTime = startStr,
                            endTime = endStr,
                            location = loc,
                            isAllDay = allDay
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return events
    }
}
