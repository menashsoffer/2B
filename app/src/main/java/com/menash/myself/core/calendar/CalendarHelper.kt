package com.menash.myself.core.calendar

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.menash.myself.core.domain.model.CalendarEvent
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
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startOfDay)
        ContentUris.appendId(builder, endOfDay)
        val instancesUri = builder.build()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        try {
            val cursor = context.contentResolver.query(
                instancesUri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val startIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)

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
        } catch (_: SecurityException) {
            return emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return events
    }
}
