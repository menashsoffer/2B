package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.DreamMappers
import com.menash.myself.core.data.model.ItemLocation
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
                        list.map { DreamMappers.dreamToDomain(it) }
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
        val location = lifeHub.locationFor("createDream", dream.isPrivate, householdSession)
        val hid = (location as? ItemLocation.Household)?.id ?: ""
        val dataItem = DreamMappers.dreamToData(dream, hid)
        lifeHub.createDream(dataItem, location)
    }

    override suspend fun updateDreamStatus(dreamId: String, status: String, isPrivate: Boolean) {
        val location = lifeHub.locationFor("updateDreamStatus", isPrivate, householdSession)
        lifeHub.updateDreamStatus(dreamId, status, location)
    }

    override suspend fun updateDreamSavings(dreamId: String, savedAmount: Money, isPrivate: Boolean) {
        val location = lifeHub.locationFor("updateDreamSavings", isPrivate, householdSession)
        lifeHub.updateDreamSavings(dreamId, savedAmount.shekels, location)
    }

    override suspend fun updateDreamAppIdea(
        dreamId: String,
        questions: Map<String, String>,
        nextAction: String,
        isPrivate: Boolean
    ) {
        val location = lifeHub.locationFor("updateDreamAppIdea", isPrivate, householdSession)
        lifeHub.updateDreamAppIdea(dreamId, questions, nextAction, location)
    }

    override suspend fun deleteDream(dreamId: String, isPrivate: Boolean) {
        val location = lifeHub.locationFor("deleteDream", isPrivate, householdSession)
        lifeHub.deleteDream(dreamId, location)
    }
}
