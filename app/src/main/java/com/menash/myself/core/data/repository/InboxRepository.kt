package com.menash.myself.core.data.repository

import com.menash.myself.core.domain.model.ForWhom
import com.menash.myself.core.domain.model.InboxItem
import kotlinx.coroutines.flow.Flow

interface InboxRepository {
    fun observeInbox(): Flow<List<InboxItem>>
    suspend fun addInboxItem(text: String, forWhom: ForWhom = ForWhom.ME, isPrivate: Boolean = false)
    suspend fun markInboxTriaged(itemId: String, triagedType: String, isPrivate: Boolean = false)
    suspend fun deleteInboxItem(itemId: String, isPrivate: Boolean = false)
}
