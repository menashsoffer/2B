package com.menash.myself.core.domain.model

data class CalendarEvent(
    val id: Long = 0,
    val title: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val isAllDay: Boolean = false
)
