package com.menash.myself.core.data.repository

import com.menash.myself.data.entities.AiSubscriptionEntity
import kotlinx.coroutines.flow.Flow

interface AiSubscriptionRepository {
    fun getAllAiSubscriptions(): Flow<List<AiSubscriptionEntity>>
    suspend fun getAllAiSubscriptionsList(): List<AiSubscriptionEntity>
    suspend fun insertAiSubscription(sub: AiSubscriptionEntity): Long
    suspend fun insertAiSubscriptions(subs: List<AiSubscriptionEntity>)
    suspend fun updateAiSubscription(sub: AiSubscriptionEntity)
    suspend fun deleteAiSubscription(sub: AiSubscriptionEntity)
    suspend fun deleteAllAiSubscriptions()
}
