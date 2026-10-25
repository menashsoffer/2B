package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.ExchangeRateRepository
import com.menash.myself.core.external.boi.BoiExchangeRateClient
import com.menash.myself.core.external.boi.BoiRate
import com.menash.myself.core.external.boi.BoiRateStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import com.menash.myself.core.external.boi.BoiRateSource
import java.time.Clock

class BoiExchangeRateRepository(
    private val store: BoiRateStore,
    private val source: BoiRateSource,
    private val clock: Clock,
    externalScope: CoroutineScope
) : ExchangeRateRepository {

    override val currentRateFlow: StateFlow<BoiRate?> = store.rateFlow
        .stateIn(externalScope, SharingStarted.Eagerly, null)

    override suspend fun refreshRateIfNeeded(): Result<BoiRate?> {
        val current = currentRateFlow.value
        val now = clock.instant()
        val isStale = if (current != null && current.officialRateDate != null && current.officialFetchedAt != null) {
            com.menash.myself.core.external.boi.BoiStalenessPolicy.isStale(current.officialRateDate, current.officialFetchedAt, now)
        } else false
        
        if (current == null || isStale) {
            val res = source.fetchLatestRate()
            res.getOrNull()?.let { store.saveAutoRate(it) }
            return res.map { it }
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
        val startDate = date.minusDays(10)
        val res = source.fetchRateWindow(startDate, date)
        if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)
        
        val observations = res.getOrThrow()
        val valid = observations.filter { !it.first.isAfter(date) }
        val latest = valid.lastOrNull()
        
        return if (latest != null) {
            Result.success(BoiRate(
                officialRate = latest.second,
                officialRateDate = latest.first,
                officialFetchedAt = clock.instant(),
                manualRate = null,
                isManualOverride = false
            ))
        } else {
            Result.failure(Exception("No historical rate found"))
        }
    }
}
