package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.InboxItem

object InboxMappers {

    fun inboxToDomain(data: com.menash.myself.core.data.legacy.models.InboxItem): InboxItem {
        val (forWhom, isPrivate) = ModelMappers.legacySpaceToForWhom(data.space)
        return InboxItem(
            id = data.id,
            text = data.text,
            status = data.status,
            triagedType = data.triagedType,
            ownerUid = data.userId,
            forWhom = forWhom,
            isPrivate = isPrivate,
            createdAt = ModelMappers.timestampToInstant(data.createdAt)
        )
    }

    fun inboxToData(domain: InboxItem, householdId: String): com.menash.myself.core.data.legacy.models.InboxItem {
        return com.menash.myself.core.data.legacy.models.InboxItem(
            id = domain.id,
            text = domain.text,
            space = ModelMappers.forWhomToLegacySpace(domain.forWhom, domain.isPrivate),
            status = domain.status,
            triagedType = domain.triagedType,
            userId = domain.ownerUid,
            householdId = householdId,
            createdAt = ModelMappers.instantToTimestamp(domain.createdAt)
        )
    }
}
