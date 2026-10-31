package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

enum class SuggestionStatus { UNDER_PLANNED, OVER_PLANNED, NO_PLAN, UNUSED, OK, NOT_ENOUGH_DATA }
data class PlanSuggestion(val categoryId: String, val current: Money?, val suggested: Money?,
    val status: SuggestionStatus, val monthsUsed: Int)

internal fun roundUp(value: Money, step: Money): Money {
    require(step.agorot > 0)
    return Money(BigDecimal.valueOf(value.agorot).divide(BigDecimal.valueOf(step.agorot), 0, RoundingMode.CEILING)
        .multiply(BigDecimal.valueOf(step.agorot)).longValueExact())
}

fun suggestPlan(baseline: CalibrationBaseline, currentPlan: Map<String, Money>, categories: List<Category>,
    tolerancePercent: BigDecimal = BigDecimal.TEN, roundStep: Money = Money(5000)): List<PlanSuggestion> {
    require(tolerancePercent >= BigDecimal.ZERO && roundStep > Money.ZERO)
    val classes = categories.associate { it.id to it.klass }
    val rows = baseline.rows.filter { it.categoryId != null }.associateBy { it.categoryId!! }
    return (rows.keys + currentPlan.keys).filter { classes[it] != ExpenseClass.PERIODIC }.map { id ->
        val row = rows[id]
        val current = currentPlan[id]
        val basis = if (classes[id] == ExpenseClass.FIXED) row?.latest else row?.average
        val actual = basis ?: Money.ZERO
        val status = when {
            baseline.months.size < 3 -> SuggestionStatus.NOT_ENOUGH_DATA
            current == null && actual > Money.ZERO -> SuggestionStatus.NO_PLAN
            current != null && current > Money.ZERO && (row == null || row.activeMonths == 0) -> SuggestionStatus.UNUSED
            else -> {
                val plan = BigDecimal.valueOf((current ?: Money.ZERO).agorot)
                val delta = BigDecimal.valueOf(actual.agorot).subtract(plan)
                val threshold = plan.abs().multiply(tolerancePercent).movePointLeft(2)
                when {
                    delta > threshold -> SuggestionStatus.UNDER_PLANNED
                    delta < threshold.negate() -> SuggestionStatus.OVER_PLANNED
                    else -> SuggestionStatus.OK
                }
            }
        }
        val suggested = when (status) {
            SuggestionStatus.NOT_ENOUGH_DATA -> null
            SuggestionStatus.UNUSED -> Money.ZERO
            SuggestionStatus.OK -> current
            else -> roundUp(actual, roundStep)
        }
        PlanSuggestion(id, current, suggested, status, baseline.months.size)
    }.sortedWith(compareByDescending<PlanSuggestion> {
        BigDecimal.valueOf((it.suggested ?: Money.ZERO).agorot)
            .subtract(BigDecimal.valueOf((it.current ?: Money.ZERO).agorot)).abs()
    }.thenBy { it.categoryId })
}
