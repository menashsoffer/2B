package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.VisibilityFilter
import kotlinx.coroutines.flow.Flow

interface DreamRepository {
    fun observeDreams(
        listFilter: String? = null,
        filter: VisibilityFilter = VisibilityFilter.MINE_AND_BOTH
    ): Flow<List<Dream>>

    suspend fun createDream(dream: Dream)
    suspend fun updateDreamStatus(dreamId: String, status: String, isPrivate: Boolean = false)
    suspend fun updateDreamSavings(dreamId: String, savedAmount: Money, isPrivate: Boolean = false)
    suspend fun updateDreamAppIdea(
        dreamId: String,
        questions: Map<String, String>,
        nextAction: String,
        isPrivate: Boolean = false
    )
    suspend fun deleteDream(dreamId: String, isPrivate: Boolean = false)
}
