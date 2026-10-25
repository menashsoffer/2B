package com.menash.myself.core.external.boi

import java.time.Instant
import java.time.LocalDate

/**
 * Exchange rate model for Bank of Israel USD/ILS representative rate.
 */
data class BoiRate(
    val officialRate: Double,
    val officialRateDate: LocalDate,
    val officialFetchedAt: Instant,
    val manualRate: Double?,
    val isManualOverride: Boolean,
    val isStale: Boolean = false
) {
    val rate: Double
        get() = if (isManualOverride && manualRate != null) manualRate else officialRate

    val rateDate: LocalDate
        get() = officialRateDate
}
