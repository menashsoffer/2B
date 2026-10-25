package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import kotlinx.coroutines.flow.Flow

interface DreamRepository {
    fun observeDreams(listFilter: String? = null, spaceFilter: String? = null): Flow<List<Dream>>
    suspend fun createDream(dream: Dream)
    suspend fun updateDreamStatus(dreamId: String, status: String)
    suspend fun updateDreamSavings(dreamId: String, savedAmount: Money)
    suspend fun updateDreamAppIdea(dreamId: String, questions: Map<String, String>, nextAction: String)
    suspend fun deleteDream(dreamId: String)
}
