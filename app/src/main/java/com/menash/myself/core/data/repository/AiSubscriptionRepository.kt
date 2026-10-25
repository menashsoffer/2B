package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.AiSubscription
import kotlinx.coroutines.flow.Flow

interface AiSubscriptionRepository {
    fun getAllAiSubscriptions(): Flow<List<AiSubscription>>
    suspend fun getAllAiSubscriptionsList(): List<AiSubscription>
    suspend fun insertAiSubscription(sub: AiSubscription): Long
    suspend fun insertAiSubscriptions(subs: List<AiSubscription>)
    suspend fun updateAiSubscription(sub: AiSubscription)
    suspend fun deleteAiSubscription(sub: AiSubscription)
    suspend fun deleteAllAiSubscriptions()
}
