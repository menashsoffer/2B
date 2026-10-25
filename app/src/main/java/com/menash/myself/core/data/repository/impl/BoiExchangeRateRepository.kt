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
        val startDate = date.minusDays(10)
        val startStr = startDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val urlStr = "${BoiExchangeRateClient.BASE_SDMX_URL}&startPeriod=$startStr&endPeriod=$endStr&format=csv"
        
        return runCatching {
            val conn = (java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "MyselfApp/1.0 (Android)")
            }
            val csv = try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw java.io.IOException("HTTP error ${conn.responseCode}")
                }
            } finally {
                runCatching { conn.disconnect() }
            }

            val lines = csv.lines().filter { it.isNotBlank() }
            if (lines.size < 2) throw IllegalArgumentException("Insufficient data in historical window")
            val header = lines[0].split(",").map { it.trim().trim('"') }
            val timePeriodIdx = header.indexOf("TIME_PERIOD").takeIf { it >= 0 } ?: throw IllegalArgumentException("Missing TIME_PERIOD")
            val obsValueIdx = header.indexOf("OBS_VALUE").takeIf { it >= 0 } ?: throw IllegalArgumentException("Missing OBS_VALUE")

            val validLines = lines.drop(1).map { line ->
                val cols = line.split(",").map { it.trim().trim('"') }
                if (cols.size > maxOf(timePeriodIdx, obsValueIdx)) {
                    val dStr = cols[timePeriodIdx]
                    val vVal = cols[obsValueIdx].toDoubleOrNull()
                    if (vVal != null) {
                        runCatching { LocalDate.parse(dStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) to vVal }.getOrNull()
                    } else null
                } else null
            }.filterNotNull()
            .filter { !it.first.isAfter(date) }
            .sortedBy { it.first }

            val latest = validLines.lastOrNull() ?: throw IllegalArgumentException("No historical rate found in window on or before $date")
            BoiRate(
                officialRate = latest.second,
                officialRateDate = latest.first,
                officialFetchedAt = Instant.now(),
                manualRate = null,
                isManualOverride = false
            )
        }
    }
}
