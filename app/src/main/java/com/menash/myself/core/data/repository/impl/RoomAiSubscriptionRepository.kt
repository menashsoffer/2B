package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.CalculatorMappers
import com.menash.myself.core.data.repository.AiSubscriptionRepository
import com.menash.myself.core.domain.model.AiSubscription
import com.menash.myself.data.dao.AppDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAiSubscriptionRepository(private val dao: AppDao) : AiSubscriptionRepository {
    override fun getAllAiSubscriptions(): Flow<List<AiSubscription>> {
        return dao.getAllAiSubscriptionsFlow().map { list -> list.map { CalculatorMappers.aiSubscriptionToDomain(it) } }
    }

    override suspend fun getAllAiSubscriptionsList(): List<AiSubscription> {
        return dao.getAllAiSubscriptions().map { CalculatorMappers.aiSubscriptionToDomain(it) }
    }

    override suspend fun insertAiSubscription(sub: AiSubscription): Long {
        return dao.insertAiSubscription(CalculatorMappers.aiSubscriptionToEntity(sub))
    }

    override suspend fun insertAiSubscriptions(subs: List<AiSubscription>) {
        dao.insertAiSubscriptions(subs.map { CalculatorMappers.aiSubscriptionToEntity(it) })
    }

    override suspend fun updateAiSubscription(sub: AiSubscription) {
        dao.updateAiSubscription(CalculatorMappers.aiSubscriptionToEntity(sub))
    }

    override suspend fun deleteAiSubscription(sub: AiSubscription) {
        dao.deleteAiSubscription(CalculatorMappers.aiSubscriptionToEntity(sub))
    }

    override suspend fun deleteAllAiSubscriptions() {
        dao.deleteAllAiSubscriptions()
    }
}
