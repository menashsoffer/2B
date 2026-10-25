package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class FirestoreInboxRepository(
    private val lifeHub: LifeHubRepository,
    private val householdSession: HouseholdSession
) : InboxRepository {

    override fun observeInbox(): Flow<List<InboxItem>> {
        val hid = householdSession.currentHouseholdId.value
        if (hid.isNullOrBlank()) {
            return emptyFlow()
        }
        val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
        return lifeHub.observeInbox(dummyHousehold)
            .map { list -> list.map { ModelMappers.inboxToDomain(it) } }
    }

    override suspend fun addInboxItem(text: String, forWhom: ForWhom, isPrivate: Boolean) {
        val hid = householdSession.requireHouseholdId()
        val legacySpace = ModelMappers.forWhomToLegacySpace(forWhom, isPrivate)
        val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
        lifeHub.addInboxItem(text, legacySpace, dummyHousehold)
    }

    override suspend fun markInboxTriaged(itemId: String, triagedType: String) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.markInboxTriaged(itemId, triagedType, hid)
    }

    override suspend fun deleteInboxItem(itemId: String) {
        val hid = householdSession.requireHouseholdId()
        lifeHub.deleteInboxItem(itemId, hid)
    }
}
