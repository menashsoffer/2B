package com.menash.myself.core.domain.model

import java.time.Instant
import java.time.LocalDate

data class FitnessEntry(
    val id: String = "",
    val date: LocalDate,
    val moved: Boolean = false,
    val minutes: Int = 0,
    val type: String = "",
    val ownerUid: String = "",
    val createdAt: Instant? = null
)
