package com.menash.myself.core.domain.model

import java.time.Instant
import java.time.LocalDate

data class Dream(
    val id: String = "",
    val title: String,
    val list: String = "חלומות קרובים",
    val estimatedCost: Money = Money.ZERO,
    val savedAmount: Money = Money.ZERO,
    val link: String = "",
    val priority: Int = 1,
    val status: String = "active", // active, completed, archived
    val targetDate: LocalDate? = null,
    val rawTargetDate: String? = null,
    val notes: String = "",
    val ownerUid: String = "",
    val forWhom: ForWhom = ForWhom.BOTH,
    val isPrivate: Boolean = false,
    val appIdeaQuestions: Map<String, String> = emptyMap(),
    val nextAction: String = "",
    val createdAt: Instant? = null
) {
    val progressPercent: Float
        get() = if (estimatedCost.isPositive()) {
            (savedAmount.shekels / estimatedCost.shekels).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

    val remainingAmount: Money
        get() = if (estimatedCost > savedAmount) estimatedCost - savedAmount else Money.ZERO

    val isCompleted: Boolean
        get() = status == "completed" || (estimatedCost.isPositive() && savedAmount >= estimatedCost)
}
