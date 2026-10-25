package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreDreamRepository(
    private val lifeHub: LifeHubRepository,
    private val householdSession: HouseholdSession
) : DreamRepository {

    override fun observeDreams(listFilter: String?, filter: VisibilityFilter): Flow<List<Dream>> {
        return householdSession.currentHouseholdId.flatMapLatest { hid ->
            if (hid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
                lifeHub.observeMergedDreams(listFilter, dummyHousehold)
                    .map { list ->
                        list.map { ModelMappers.dreamToDomain(it) }
                            .filter { dream ->
                                when (filter) {
                                    VisibilityFilter.ALL -> true
                                    VisibilityFilter.MINE_AND_BOTH ->
                                        dream.forWhom == ForWhom.ME || dream.forWhom == ForWhom.BOTH
                                }
                            }
                    }
            }
        }
    }

    override suspend fun createDream(dream: Dream) {
        val hid = if (dream.isPrivate) "private" else householdSession.requireHouseholdId()
        val dataItem = ModelMappers.dreamToData(dream, hid)
        val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
        lifeHub.createDream(dataItem, dummyHousehold)
    }

    override suspend fun updateDreamStatus(dreamId: String, status: String, isPrivate: Boolean) {
        val hid = if (isPrivate) "private" else householdSession.requireHouseholdId()
        lifeHub.updateDreamStatus(dreamId, status, hid)
    }

    override suspend fun updateDreamSavings(dreamId: String, savedAmount: Money, isPrivate: Boolean) {
        val hid = if (isPrivate) "private" else householdSession.requireHouseholdId()
        lifeHub.updateDreamSavings(dreamId, savedAmount.shekels, hid)
    }

    override suspend fun updateDreamAppIdea(
        dreamId: String,
        questions: Map<String, String>,
        nextAction: String,
        isPrivate: Boolean
    ) {
        val hid = if (isPrivate) "private" else householdSession.requireHouseholdId()
        lifeHub.updateDreamAppIdea(dreamId, questions, nextAction, hid)
    }

    override suspend fun deleteDream(dreamId: String, isPrivate: Boolean) {
        val hid = if (isPrivate) "private" else householdSession.requireHouseholdId()
        lifeHub.deleteDream(dreamId, hid)
    }
}
