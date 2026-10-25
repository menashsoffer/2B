package com.menash.myself.core.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure domain value object representing an inclusive date range.
 */
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    init {
        require(!endDate.isBefore(startDate)) {
            "endDate ($endDate) cannot be before startDate ($startDate)"
        }
    }

    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    fun daysCount(): Long {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1
    }

    fun monthsCount(): Long {
        return ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), endDate.withDayOfMonth(1)) + 1
    }
}
