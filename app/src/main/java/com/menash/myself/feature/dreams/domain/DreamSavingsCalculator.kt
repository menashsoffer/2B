package com.menash.myself.feature.dreams.domain

import com.menash.myself.core.domain.model.Money
import kotlin.math.ceil

data class DreamSavingsInput(
    val targetAmount: Money,
    val savedAmount: Money,
    val monthlySavingsCapacity: Money = Money.ZERO
)

data class DreamSavingsResult(
    val remainingAmount: Money,
    val progressPercent: Double,
    val monthsNeeded: Int,
    val isCompleted: Boolean
)

object DreamSavingsCalculator {

    fun calculateSavingsProgress(input: DreamSavingsInput): DreamSavingsResult {
        val remainingAgorot = kotlin.math.max(0L, input.targetAmount.agorot - input.savedAmount.agorot)
        val remaining = Money.fromAgorot(remainingAgorot)

        val progressPercent = if (input.targetAmount.agorot > 0L) {
            val ratio = (input.savedAmount.agorot.toDouble() / input.targetAmount.agorot.toDouble()) * 100.0
            kotlin.math.min(100.0, kotlin.math.max(0.0, ratio))
        } else {
            0.0
        }

        val monthsNeeded = if (remaining.isZero()) {
            0
        } else if (input.monthlySavingsCapacity.isPositive()) {
            ceil(remaining.agorot.toDouble() / input.monthlySavingsCapacity.agorot.toDouble()).toInt()
        } else {
            Int.MAX_VALUE
        }

        return DreamSavingsResult(
            remainingAmount = remaining,
            progressPercent = progressPercent,
            monthsNeeded = monthsNeeded,
            isCompleted = remaining.isZero() && input.targetAmount.isPositive()
        )
    }
}
