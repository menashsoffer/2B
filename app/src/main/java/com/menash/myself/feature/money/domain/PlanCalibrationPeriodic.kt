package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

data class PeriodicSuggestion(val budgetId: String, val current: Money, val suggested: Money?,
    val spentLast12: Money?, val monthsUsed: Int)

fun suggestPeriodic(closed: List<ClosedMonth>, budgets: List<PeriodicBudget>, endMonth: YearMonth,
    roundStep: Money = Money(10000)): List<PeriodicSuggestion> {
    require(roundStep > Money.ZERO)
    require(closed.map { it.month }.distinct().size == closed.size) { "One close per month is required" }
    val rows = closed.filter { it.month >= endMonth.minusMonths(11) && it.month <= endMonth }
    return budgets.map { budget ->
        val spent = if (rows.size == 12) sumMoney(rows.map { it.byCategory[budget.categoryId] ?: Money.ZERO }) else null
        PeriodicSuggestion(budget.id, budget.annualAmount, spent?.let { roundUp(it, roundStep) }, spent, rows.size)
    }
}
