package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.Task
import com.menash.myself.data.models.TaskItem

object TaskMappers {

    fun taskToDomain(data: TaskItem): Task {
        val (forWhom, isPrivate) = ModelMappers.legacySpaceToForWhom(data.space)
        val (parsedDate, rawDate) = ModelMappers.parseLocalDateWithRaw(data.dueDate)
        return Task(
            id = data.id,
            title = data.title,
            nextAction = data.nextAction,
            isDone = data.isDone,
            isTop3 = data.isTop3,
            isFiveBefore11 = data.isFiveBefore11,
            dueDate = parsedDate,
            rawDueDate = rawDate,
            ownerUid = data.userId,
            forWhom = forWhom,
            isPrivate = isPrivate,
            createdAt = ModelMappers.timestampToInstant(data.createdAt)
        )
    }

    fun taskToData(domain: Task, householdId: String): TaskItem {
        val effectiveDueDate = if (domain.dueDate != null) {
            ModelMappers.formatLocalDate(domain.dueDate)
        } else {
            domain.rawDueDate ?: ""
        }
        return TaskItem(
            id = domain.id,
            title = domain.title,
            nextAction = domain.nextAction,
            isDone = domain.isDone,
            isTop3 = domain.isTop3,
            isFiveBefore11 = domain.isFiveBefore11,
            dueDate = effectiveDueDate,
            space = ModelMappers.forWhomToLegacySpace(domain.forWhom, domain.isPrivate),
            userId = domain.ownerUid,
            householdId = householdId,
            createdAt = ModelMappers.instantToTimestamp(domain.createdAt)
        )
    }
}
