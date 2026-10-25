package com.menash.myself.core.data.mapper

import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.data.models.FitnessLog
import java.time.LocalDate

object FitnessMappers {

    fun fitnessToDomain(data: FitnessLog): FitnessEntry {
        val date = ModelMappers.parseLocalDate(data.date) ?: LocalDate.now()
        return FitnessEntry(
            id = data.id,
            date = date,
            moved = data.moved,
            minutes = data.minutes,
            type = data.type,
            ownerUid = data.userId,
            createdAt = ModelMappers.timestampToInstant(data.createdAt)
        )
    }

    fun fitnessToData(domain: FitnessEntry): FitnessLog {
        return FitnessLog(
            id = domain.id,
            date = ModelMappers.formatLocalDate(domain.date),
            moved = domain.moved,
            minutes = domain.minutes,
            type = domain.type,
            userId = domain.ownerUid,
            createdAt = ModelMappers.instantToTimestamp(domain.createdAt)
        )
    }
}
