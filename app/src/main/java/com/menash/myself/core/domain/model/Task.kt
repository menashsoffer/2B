package com.menash.myself.core.domain.model

import java.time.Instant
import java.time.LocalDate

data class Task(
    val id: String = "",
    val title: String,
    val nextAction: String = "",
    val isDone: Boolean = false,
    val isTop3: Boolean = false,
    val isFiveBefore11: Boolean = false,
    val dueDate: LocalDate? = null,
    val rawDueDate: String? = null,
    val ownerUid: String = "",
    val forWhom: ForWhom = ForWhom.ME,
    val isPrivate: Boolean = false,
    val tag: String = "",
    val createdAt: Instant? = null
)
