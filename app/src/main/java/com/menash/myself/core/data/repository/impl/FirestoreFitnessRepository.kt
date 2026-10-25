package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.FitnessRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.FitnessLog
import kotlinx.coroutines.flow.Flow

class FirestoreFitnessRepository(private val lifeHub: LifeHubRepository) : FitnessRepository {
    override fun observeTodayFitness(): Flow<FitnessLog?> = lifeHub.observeTodayFitness()

    override fun observeWeeklyFitness(): Flow<List<FitnessLog>> = lifeHub.observeWeeklyFitness()

    override suspend fun saveTodayFitness(moved: Boolean, minutes: Int, type: String) =
        lifeHub.saveTodayFitness(moved, minutes, type)
}
