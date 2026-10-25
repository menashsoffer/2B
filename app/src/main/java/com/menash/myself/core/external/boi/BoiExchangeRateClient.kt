package com.menash.myself.core.external.boi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Client for Bank of Israel SDMX API (CSV format).
 * Endpoint: https://edge.boi.gov.il/FusionEdgeServer/sdmx/v2/data/dataflow/BOI.STATISTICS/EXR/1.0/RER_USD_ILS?c[DATA_TYPE]=OF00&lastNObservations=1&format=csv
 */
class BoiExchangeRateClient {

    companion object {
        const val BASE_SDMX_URL =
            "https://edge.boi.gov.il/FusionEdgeServer/sdmx/v2/data/dataflow/BOI.STATISTICS/EXR/1.0/RER_USD_ILS?c%5BDATA_TYPE%5D=OF00"
    }

    suspend fun fetchLatestRate(): Result<BoiRate> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = "$BASE_SDMX_URL&lastNObservations=1&format=csv"
            val csv = fetchUrl(urlStr)
            parseSdmxCsv(csv)
        }
    }

    suspend fun fetchHistoricalRate(date: LocalDate): Result<BoiRate> = withContext(Dispatchers.IO) {
        runCatching {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val urlStr = "$BASE_SDMX_URL&startPeriod=$dateStr&endPeriod=$dateStr&format=csv"
            val csv = fetchUrl(urlStr)
            parseSdmxCsv(csv)
        }
    }

    fun parseSdmxCsv(csvContent: String, nowInstant: Instant = Instant.now()): BoiRate {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size < 2) {
            throw IllegalArgumentException("Invalid SDMX CSV response: insufficient lines")
        }

        val header = lines[0].split(",").map { it.trim().trim('"') }
        val timePeriodIdx = header.indexOf("TIME_PERIOD").takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Missing TIME_PERIOD column in BOI CSV header")
        val obsValueIdx = header.indexOf("OBS_VALUE").takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Missing OBS_VALUE column in BOI CSV header")

        val dataLine = lines.lastOrNull { line ->
            val cols = line.split(",").map { it.trim().trim('"') }
            cols.size > maxOf(timePeriodIdx, obsValueIdx) && cols[obsValueIdx].toDoubleOrNull() != null
        } ?: throw IllegalArgumentException("No valid observations found in BOI CSV")

        val cols = dataLine.split(",").map { it.trim().trim('"') }
        val dateStr = cols[timePeriodIdx].trim()
        val rateVal = cols[obsValueIdx].trim().toDouble()
        val rateDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)

        return BoiRate(
            officialRate = rateVal,
            officialRateDate = rateDate,
            officialFetchedAt = nowInstant,
            manualRate = null,
            isManualOverride = false,
            isStale = false
        )
    }

    private fun fetchUrl(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "MyselfApp/1.0 (Android)")
        }

        try {
            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorText = runCatching {
                    conn.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull() ?: "HTTP error code $responseCode"
                throw java.io.IOException("Failed to fetch BOI rate: $errorText")
            }
        } finally {
            runCatching { conn.disconnect() }
        }
    }
}
