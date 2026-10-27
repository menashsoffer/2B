package com.menash.myself.core.external.boi

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Staleness for the Bank of Israel USD/ILS representative rate.
 * Publication days are Monday–Friday. Saturday, Sunday, holidays, and holiday
 * eves are not modeled; see docs/BACKLOG_STEP8.md.
 */
object BoiStalenessPolicy {

    val ISRAEL_ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")

    /** 15 minutes after the official "shortly after 15:15" window. */
    val MONDAY_TO_THURSDAY_CUTOFF: LocalTime = LocalTime.of(15, 30)

    /** 15 minutes after the official Friday "shortly after 12:15" window. */
    val FRIDAY_CUTOFF: LocalTime = LocalTime.of(12, 30)

    fun publicationCutoff(date: LocalDate): LocalTime? = when (date.dayOfWeek) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY ->
            MONDAY_TO_THURSDAY_CUTOFF
        DayOfWeek.FRIDAY -> FRIDAY_CUTOFF
        else -> null
    }

    fun isBusinessDay(date: LocalDate): Boolean = publicationCutoff(date) != null

    fun isStale(
        rateDate: LocalDate?,
        fetchedAt: Instant?,
        nowInstant: Instant = Instant.now()
    ): Boolean {
        if (rateDate == null || fetchedAt == null) return true

        val nowZoned = nowInstant.atZone(ISRAEL_ZONE)
        val fetchedZoned = fetchedAt.atZone(ISRAEL_ZONE)
        val today = nowZoned.toLocalDate()
        val cutoffTime = publicationCutoff(today)

        if (cutoffTime != null && nowZoned.toLocalTime().isAfter(cutoffTime)) {
            val cutoff = ZonedDateTime.of(today, cutoffTime, ISRAEL_ZONE)
            if (fetchedZoned.isBefore(cutoff)) {
                return true
            }
        }

        return rateDate.isBefore(today.minusDays(4))
    }
}
