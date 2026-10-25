package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.DreamRepository
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class FirestoreDreamRepository(
    private val lifeHub: LifeHubRepository,
    private val householdSession: HouseholdSession
) : DreamRepository {

    override fun observeDreams(listFilter: String?, spaceFilter: String?): Flow<List<Dream>> {
        val hid = householdSession.currentHouseholdId.value
        if (hid.isNullOrBlank()) {
            return emptyFlow()
        }
        val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
        return lifeHub.observeDreams(listFilter, spaceFilter, dummyHousehold)
            .map { list -> list.map { ModelMappers.dreamToDomain(it) } }
    }

    override suspend fun createDream(dream: Dream) {
        val hid = householdSession.requireHouseholdId()
        val dataItem = ModelMappers.dreamToData(dream, hid)
        val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
        lifeHub.createDream(dataItem, dummyHousehold)
    }

    override suspend fun updateDreamStatus(dreamId: String, status: String) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.updateDreamStatus(dreamId, status, hid)
    }

    override suspend fun updateDreamSavings(dreamId: String, savedAmount: Money) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.updateDreamSavings(dreamId, savedAmount.shekels, hid)
    }

    override suspend fun updateDreamAppIdea(
        dreamId: String,
        questions: Map<String, String>,
        nextAction: String
    ) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.updateDreamAppIdea(dreamId, questions, nextAction, hid)
    }

    override suspend fun deleteDream(dreamId: String) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.deleteDream(dreamId, hid)
    }
}
