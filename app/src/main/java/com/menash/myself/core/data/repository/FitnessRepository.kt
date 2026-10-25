package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.FitnessEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FitnessRepository {
    fun observeTodayFitness(): Flow<FitnessEntry?>
    fun observeWeeklyFitness(): Flow<List<FitnessEntry>>
    suspend fun saveTodayFitness(moved: Boolean, minutes: Int, type: String, date: LocalDate = LocalDate.now())
}
