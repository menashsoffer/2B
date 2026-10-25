package com.menash.myself.core.external.boi

import java.time.Instant
import java.time.LocalDate

/**
 * Exchange rate model for Bank of Israel USD/ILS representative rate.
 */
data class BoiRate(
    val rate: Double,
    val rateDate: LocalDate,
    val fetchedAt: Instant,
    val isManualOverride: Boolean = false,
    val isStale: Boolean = false
)
