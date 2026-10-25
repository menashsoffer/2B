package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.Household
import kotlinx.coroutines.flow.Flow

class FirestoreDreamRepository(private val lifeHub: LifeHubRepository) : DreamRepository {
    override fun observeDreams(listFilter: String?, spaceFilter: String?, household: Household?): Flow<List<DreamItem>> =
        lifeHub.observeDreams(listFilter, spaceFilter, household)

    override suspend fun createDream(dream: DreamItem, household: Household?) =
        lifeHub.createDream(dream, household)

    override suspend fun updateDreamStatus(dreamId: String, status: String, householdId: String) =
        lifeHub.updateDreamStatus(dreamId, status, householdId)

    override suspend fun updateDreamSavings(dreamId: String, savedAmount: Double, householdId: String) =
        lifeHub.updateDreamSavings(dreamId, savedAmount, householdId)

    override suspend fun updateDreamAppIdea(
        dreamId: String,
        questions: Map<String, String>,
        nextAction: String,
        householdId: String
    ) = lifeHub.updateDreamAppIdea(dreamId, questions, nextAction, householdId)

    override suspend fun deleteDream(dreamId: String, householdId: String) =
        lifeHub.deleteDream(dreamId, householdId)
}
