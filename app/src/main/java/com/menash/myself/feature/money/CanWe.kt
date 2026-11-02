package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.Feasibility
import com.menash.myself.feature.money.domain.FeasibilityInput
import com.menash.myself.feature.money.domain.Goal
import com.menash.myself.feature.money.domain.MonthPlan
import com.menash.myself.feature.money.domain.amountFor
import com.menash.myself.feature.money.domain.isItPossible
import com.menash.myself.feature.money.domain.monthPlan
import com.menash.myself.feature.money.domain.sumMoney
import java.math.BigDecimal
import java.time.YearMonth

/** Where the monthly saving the answers use comes from. */
enum class RateBasis {
    /** The average of closed months: the real number. */
    CLOSED_MONTHS,
    /** No month is closed yet: this month's planned surplus stands in, and the screen says so. */
    PLAN,
}

/** [answer] is null when the target month has already passed. */
data class GoalAnswer(val goal: Goal, val answer: Feasibility?)

data class CanWeView(val basis: RateBasis?, val monthlySaving: Money?, val goals: List<GoalAnswer>)

/**
 * The month's plan, or null when it cannot be priced: an active USD subscription with no USD rate yet.
 * Leaving that subscription out would understate expenses and make every answer look better than it is.
 */
private fun plannedMonth(data: MoneyData, month: YearMonth, usdToIls: BigDecimal?): MonthPlan.Planned? {
    val rate = usdToIls?.takeIf { it > BigDecimal.ZERO }
    if (rate == null && data.items.subscriptions.any { it.currency == Currency.USD && amountFor(it, month) != null }) return null
    return monthPlan(month, data.items, rate) as? MonthPlan.Planned
}

private fun inputFor(goal: Goal, others: List<Goal>, data: MoneyData, month: YearMonth, plan: MonthPlan.Planned?, saving: Money?) =
    FeasibilityInput(
        goal = goal,
        otherGoals = others,
        savingRate = saving,
        currentMonth = month,
        liquidAssets = sumMoney(data.assets.filter { it.kind == AssetKind.ASSET && it.liquid }.map { it.balance }),
        averageMonthlyExpenses = plan?.plannedExpenses ?: Money.ZERO,
        variableBudgetsTotal = plan?.variable ?: Money.ZERO,
    )

/**
 * Every stored goal with its "האם אפשר?" answer from the engine. Each goal is judged with the household's
 * other goals already taking their share of the monthly saving. [closedSaving] is the closed-months average
 * once months are closed; until then the plan's expected surplus is used and [CanWeView.basis] says so.
 */
fun canWe(data: MoneyData, month: YearMonth, usdToIls: BigDecimal?, closedSaving: Money? = null): CanWeView {
    val plan = plannedMonth(data, month, usdToIls)
    val basis = when {
        closedSaving != null -> RateBasis.CLOSED_MONTHS
        plan != null -> RateBasis.PLAN
        else -> null
    }
    val saving = closedSaving ?: plan?.expectedSurplus
    val goals = data.goals.sortedBy { it.targetMonth }.map { goal ->
        val others = data.goals.filter { it.id != goal.id && !it.targetMonth.isBefore(month) }
        GoalAnswer(goal, if (goal.targetMonth.isBefore(month)) null else isItPossible(inputFor(goal, others, data, month, plan, saving)))
    }
    return CanWeView(basis, saving, goals)
}

/** The answer for a goal being typed, judged against the stored goals (the one being edited left out). */
fun previewAnswer(goal: Goal, data: MoneyData, month: YearMonth, usdToIls: BigDecimal?, closedSaving: Money? = null): Feasibility? {
    if (goal.targetMonth.isBefore(month)) return null
    val plan = plannedMonth(data, month, usdToIls)
    val others = data.goals.filter { it.id != goal.id && !it.targetMonth.isBefore(month) }
    return isItPossible(inputFor(goal, others, data, month, plan, closedSaving ?: plan?.expectedSurplus))
}
