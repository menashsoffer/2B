package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money

data class PlanImpact(val currentMonthly: Money, val newMonthly: Money, val difference: Money)

fun planImpact(currentPlan: Map<String, Money>, suggestions: List<PlanSuggestion>, accepted: Set<String>,
    periodic: List<PeriodicSuggestion>, acceptedPeriodic: Set<String>, budgets: List<PeriodicBudget>): PlanImpact {
    val monthlyChanges = suggestions.associateBy { it.categoryId }
    val periodicChanges = periodic.associateBy { it.budgetId }
    val current = sumMoney(currentPlan.values + budgets.map(::monthlyReserve))
    val ids = currentPlan.keys + monthlyChanges.keys.filter { it in accepted }
    val nextMonthly = ids.map { id ->
        (if (id in accepted) monthlyChanges[id]?.suggested else null) ?: currentPlan[id] ?: Money.ZERO
    }
    val nextPeriodic = budgets.map { budget ->
        val annual = if (budget.id in acceptedPeriodic) periodicChanges[budget.id]?.suggested else null
        monthlyReserve(if (annual == null) budget else budget.copy(annualAmount = annual))
    }
    val next = sumMoney(nextMonthly + nextPeriodic)
    return PlanImpact(current, next, subtractMoney(next, current))
}
