package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.FitnessMappers
import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.data.legacy.firestore.LifeHubRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FirestoreFitnessRepository(
    private val lifeHub: LifeHubRepository
) : FitnessRepository {

    override fun observeTodayFitness(): Flow<FitnessEntry?> {
        return lifeHub.observeTodayFitness().map { it?.let { FitnessMappers.fitnessToDomain(it) } }
    }

    override fun observeWeeklyFitness(): Flow<List<FitnessEntry>> {
        return lifeHub.observeWeeklyFitness().map { list -> list.map { FitnessMappers.fitnessToDomain(it) } }
    }

    override suspend fun saveTodayFitness(moved: Boolean, minutes: Int, type: String, date: LocalDate) {
        lifeHub.saveTodayFitness(moved, minutes, type)
    }
}
