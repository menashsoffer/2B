package com.menash.myself.core.data.repository

import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.Household
import kotlinx.coroutines.flow.Flow

interface DreamRepository {
    fun observeDreams(listFilter: String? = null, spaceFilter: String? = null, household: Household? = null): Flow<List<DreamItem>>
    suspend fun createDream(dream: DreamItem, household: Household? = null)
    suspend fun updateDreamStatus(dreamId: String, status: String, householdId: String = "")
    suspend fun updateDreamSavings(dreamId: String, savedAmount: Double, householdId: String = "")
    suspend fun updateDreamAppIdea(dreamId: String, questions: Map<String, String>, nextAction: String, householdId: String = "")
    suspend fun deleteDream(dreamId: String, householdId: String = "")
}
