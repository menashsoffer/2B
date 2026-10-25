package com.menash.myself.core.external.boi

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Staleness checking policy for Bank of Israel representative USD/ILS rate.
 */
object BoiStalenessPolicy {

    val ISRAEL_ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")
    val PUBLISH_TIME: LocalTime = LocalTime.of(16, 0)

    fun isBusinessDay(date: LocalDate): Boolean {
        val dow = date.dayOfWeek
        // In Israel, business days for BOI representative rate are Sunday through Thursday
        return dow == DayOfWeek.SUNDAY || dow == DayOfWeek.MONDAY || dow == DayOfWeek.TUESDAY ||
                dow == DayOfWeek.WEDNESDAY || dow == DayOfWeek.THURSDAY
    }

    fun isStale(
        rateDate: LocalDate?,
        fetchedAt: Instant?,
        nowInstant: Instant = Instant.now()
    ): Boolean {
        if (rateDate == null || fetchedAt == null) return true

        val nowZoned = nowInstant.atZone(ISRAEL_ZONE)
        val fetchedZoned = fetchedAt.atZone(ISRAEL_ZONE)
        val today = nowZoned.toLocalDate()

        // If fetched date is from a previous day and today is past 16:00 on a business day, it is stale
        if (isBusinessDay(today) && nowZoned.toLocalTime().isAfter(PUBLISH_TIME)) {
            val cutoff = ZonedDateTime.of(today, PUBLISH_TIME, ISRAEL_ZONE)
            if (fetchedZoned.isBefore(cutoff)) {
                return true
            }
        }

        // If rate date is more than 4 days old (e.g. over weekend/holidays) or behind current day
        return rateDate.isBefore(today.minusDays(4))
    }
}
