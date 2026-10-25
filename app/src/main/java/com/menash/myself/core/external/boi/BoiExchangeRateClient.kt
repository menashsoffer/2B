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

        val header = lines[0].split(",")
        val timePeriodIdx = header.indexOf("TIME_PERIOD").takeIf { it >= 0 } ?: 12
        val obsValueIdx = header.indexOf("OBS_VALUE").takeIf { it >= 0 } ?: 13

        // Find the last valid data line
        val dataLine = lines.last { line ->
            val cols = line.split(",")
            cols.size > maxOf(timePeriodIdx, obsValueIdx) && cols[obsValueIdx].toDoubleOrNull() != null
        }

        val cols = dataLine.split(",")
        val dateStr = cols[timePeriodIdx].trim()
        val rateVal = cols[obsValueIdx].trim().toDouble()
        val rateDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)

        return BoiRate(
            rate = rateVal,
            rateDate = rateDate,
            fetchedAt = nowInstant,
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

        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}
