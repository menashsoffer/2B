package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.ExchangeRateRepository
import com.menash.myself.core.external.boi.BoiExchangeRateClient
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.boi.BoiRateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Implementation of ExchangeRateRepository backed by BoiRateStore DataStore and BoiExchangeRateClient.
 */
class BoiExchangeRateRepository(
    private val store: BoiRateStore,
    private val client: BoiExchangeRateClient,
    externalScope: CoroutineScope
) : ExchangeRateRepository {

    override val currentRateFlow: StateFlow<BoiRate?> = store.rateFlow
        .stateIn(externalScope, SharingStarted.Eagerly, null)

    override suspend fun refreshRateIfNeeded(): Result<BoiRate?> {
        val current = currentRateFlow.value
        if (current == null || current.isStale) {
            val res = client.fetchLatestRate()
            res.getOrNull()?.let { store.saveAutoRate(it) }
            return res.map { store.rateFlow.toString(); it }
        }
        return Result.success(current)
    }

    override suspend fun setManualRate(rate: Double?) {
        store.setManualOverride(rate)
    }

    override suspend fun clearManualRate() {
        store.clearManualOverride()
    }

    override suspend fun fetchHistoricalRate(date: LocalDate): Result<BoiRate> {
        // Query window of ~10 days back to find the latest valid representative rate published on or before date
        var targetDate = date
        for (i in 0..10) {
            val res = client.fetchHistoricalRate(targetDate)
            if (res.isSuccess && res.getOrNull() != null) {
                return res
            }
            targetDate = targetDate.minusDays(1)
        }
        return client.fetchLatestRate()
    }
}
