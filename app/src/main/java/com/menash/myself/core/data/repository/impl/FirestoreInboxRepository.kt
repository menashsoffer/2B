package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.mapper.ModelMappers
import com.menash.myself.core.data.model.ItemLocation
import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.core.data.session.HouseholdSession
import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreInboxRepository(
    private val lifeHub: LifeHubRepository,
    private val householdSession: HouseholdSession
) : InboxRepository {

    private fun resolveLocation(action: String, isPrivate: Boolean): ItemLocation {
        return try {
            ItemLocation.resolve(isPrivate, householdSession) { lifeHub.currentUid() }
        } catch (e: Exception) {
            lifeHub.reportError(action, e)
            throw e
        }
    }

    override fun observeInbox(): Flow<List<InboxItem>> {
        return householdSession.currentHouseholdId.flatMapLatest { hid ->
            if (hid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                val dummyHousehold = Household(id = hid, name = "", ownerUid = "")
                lifeHub.observeMergedInbox(dummyHousehold)
                    .map { list -> list.map { ModelMappers.inboxToDomain(it) } }
            }
        }
    }

    override suspend fun addInboxItem(text: String, forWhom: ForWhom, isPrivate: Boolean) {
        val location = resolveLocation("addInboxItem", isPrivate)
        val legacySpace = ModelMappers.forWhomToLegacySpace(forWhom, isPrivate)
        lifeHub.addInboxItem(text, legacySpace, location)
    }

    override suspend fun markInboxTriaged(itemId: String, triagedType: String, isPrivate: Boolean) {
        val location = resolveLocation("markInboxTriaged", isPrivate)
        lifeHub.markInboxTriaged(itemId, triagedType, location)
    }

    override suspend fun deleteInboxItem(itemId: String, isPrivate: Boolean) {
        val location = resolveLocation("deleteInboxItem", isPrivate)
        lifeHub.deleteInboxItem(itemId, location)
    }
}
