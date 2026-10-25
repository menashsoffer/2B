package com.menash.myself.core.data.repository

import com.menash.myself.data.models.FitnessLog
import kotlinx.coroutines.flow.Flow

interface FitnessRepository {
    fun observeTodayFitness(): Flow<FitnessLog?>
    fun observeWeeklyFitness(): Flow<List<FitnessLog>>
    suspend fun saveTodayFitness(moved: Boolean, minutes: Int, type: String)
}
