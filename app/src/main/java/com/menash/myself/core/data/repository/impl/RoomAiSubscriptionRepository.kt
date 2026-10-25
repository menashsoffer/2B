package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.AiSubscriptionRepository
import com.menash.myself.data.dao.AppDao
import com.menash.myself.data.entities.AiSubscriptionEntity
import kotlinx.coroutines.flow.Flow

class RoomAiSubscriptionRepository(private val dao: AppDao) : AiSubscriptionRepository {
    override fun getAllAiSubscriptions(): Flow<List<AiSubscriptionEntity>> = dao.getAllAiSubscriptionsFlow()

    override suspend fun getAllAiSubscriptionsList(): List<AiSubscriptionEntity> = dao.getAllAiSubscriptions()

    override suspend fun insertAiSubscription(sub: AiSubscriptionEntity): Long = dao.insertAiSubscription(sub)

    override suspend fun insertAiSubscriptions(subs: List<AiSubscriptionEntity>) = dao.insertAiSubscriptions(subs)

    override suspend fun updateAiSubscription(sub: AiSubscriptionEntity) = dao.updateAiSubscription(sub)

    override suspend fun deleteAiSubscription(sub: AiSubscriptionEntity) = dao.deleteAiSubscription(sub)

    override suspend fun deleteAllAiSubscriptions() = dao.deleteAllAiSubscriptions()
}
