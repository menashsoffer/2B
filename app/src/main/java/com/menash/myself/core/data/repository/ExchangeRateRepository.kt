package com.menash.myself.core.data.repository

import com.menash.myself.core.external.boi.BoiRate
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Interface for accessing representative USD/ILS exchange rate.
 */
interface ExchangeRateRepository {
    val currentRateFlow: StateFlow<BoiRate?>
    suspend fun refreshRateIfNeeded(): Result<BoiRate?>
    suspend fun setManualRate(rate: Double?)
    suspend fun clearManualRate()
    suspend fun fetchHistoricalRate(date: LocalDate): Result<BoiRate>
}
