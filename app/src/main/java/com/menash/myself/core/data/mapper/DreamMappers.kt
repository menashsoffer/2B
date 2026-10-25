package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import com.menash.myself.data.models.DreamItem

object DreamMappers {

    fun dreamToDomain(data: DreamItem): Dream {
        val (forWhom, isPrivate) = ModelMappers.legacySpaceToForWhom(data.space)
        val (parsedDate, rawDate) = ModelMappers.parseLocalDateWithRaw(data.targetDate)
        return Dream(
            id = data.id,
            title = data.title,
            list = data.list,
            estimatedCost = Money.fromShekels(data.estimatedCost),
            savedAmount = Money.fromShekels(data.savedAmount),
            link = data.link,
            priority = data.priority,
            status = data.status,
            targetDate = parsedDate,
            rawTargetDate = rawDate,
            notes = data.notes,
            ownerUid = data.userId,
            forWhom = forWhom,
            isPrivate = isPrivate,
            appIdeaQuestions = data.appIdeaQuestions,
            nextAction = data.nextAction,
            createdAt = ModelMappers.timestampToInstant(data.createdAt)
        )
    }

    fun dreamToData(domain: Dream, householdId: String): DreamItem {
        val effectiveTargetDate = if (domain.targetDate != null) {
            ModelMappers.formatLocalDate(domain.targetDate)
        } else {
            domain.rawTargetDate ?: ""
        }
        return DreamItem(
            id = domain.id,
            title = domain.title,
            list = domain.list,
            estimatedCost = domain.estimatedCost.shekels,
            savedAmount = domain.savedAmount.shekels,
            link = domain.link,
            priority = domain.priority,
            status = domain.status,
            targetDate = effectiveTargetDate,
            notes = domain.notes,
            space = ModelMappers.forWhomToLegacySpace(domain.forWhom, domain.isPrivate),
            userId = domain.ownerUid,
            householdId = householdId,
            appIdeaQuestions = domain.appIdeaQuestions,
            nextAction = domain.nextAction,
            createdAt = ModelMappers.instantToTimestamp(domain.createdAt)
        )
    }
}
