package com.menash.myself.core.external.boi

import java.time.LocalDate

interface BoiRateSource {
    suspend fun fetchLatestRate(): Result<BoiRate>
    suspend fun fetchRateWindow(startDate: LocalDate, endDate: LocalDate): Result<List<Pair<LocalDate, Double>>>
}
