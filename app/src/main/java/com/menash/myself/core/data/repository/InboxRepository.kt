package com.menash.myself.core.data.repository

import com.menash.myself.data.models.Household
import com.menash.myself.data.models.InboxItem
import kotlinx.coroutines.flow.Flow

interface InboxRepository {
    fun observeInbox(household: Household? = null): Flow<List<InboxItem>>
    suspend fun addInboxItem(text: String, space: String = "shared", household: Household? = null)
    suspend fun markInboxTriaged(itemId: String, triagedType: String, householdId: String = "")
    suspend fun deleteInboxItem(itemId: String, householdId: String = "")
}
