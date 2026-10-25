package com.menash.myself.core.data.repository.impl

import com.menash.myself.core.data.repository.InboxRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.Household
import com.menash.myself.data.models.InboxItem
import kotlinx.coroutines.flow.Flow

class FirestoreInboxRepository(private val lifeHub: LifeHubRepository) : InboxRepository {
    override fun observeInbox(household: Household?): Flow<List<InboxItem>> =
        lifeHub.observeInbox(household)

    override suspend fun addInboxItem(text: String, space: String, household: Household?) =
        lifeHub.addInboxItem(text, space, household)

    override suspend fun markInboxTriaged(itemId: String, triagedType: String, householdId: String) =
        lifeHub.markInboxTriaged(itemId, triagedType, householdId)

    override suspend fun deleteInboxItem(itemId: String, householdId: String) =
        lifeHub.deleteInboxItem(itemId, householdId)
}
