package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** A savings goal; allocated is what is already set aside for it. */
data class Goal(
    val id: String, val name: String, val amount: Money, val targetMonth: YearMonth,
    val allocated: Money = Money.ZERO, val newMonthlyCost: Money? = null,
) {
    init {
        requireId(id)
        require(amount >= Money.ZERO && allocated >= Money.ZERO)
        require(newMonthlyCost == null || newMonthlyCost >= Money.ZERO)
    }
}

sealed class Lever {
    /** Earliest target month that fits at the current free rate. */
    data class Postpone(val targetMonth: YearMonth) : Lever()
    /** One-off amount from liquid assets above the cushion minimum. */
    data class TakeFromSavings(val amount: Money) : Lever()
    /** Per-month cut from variable budgets, never more than their total. */
    data class Cut(val perMonth: Money) : Lever()
    /** Buy now and pay in this many monthly payments (max 36). */
    data class Installments(val payments: Int, val perMonth: Money) : Lever()
}

sealed class Feasibility {
    data class Possible(val monthlyAmount: Money) : Feasibility()
    data class PossibleIf(val monthlyAmount: Money, val levers: List<Lever>) : Feasibility()
    data class NotNow(val gapPerMonth: Money, val closestLever: Lever?) : Feasibility()
    /** No month has been closed yet, so there is no saving rate to judge by. */
    object Unknown : Feasibility()
}

/**
 * savingRate comes from [savingRate]; liquidAssets and averageMonthlyExpenses are the caller's current values.
 * The safety cushion (cushionMinMonths × averageMonthlyExpenses) is an automatic first goal: while it is not
 * full, gap / cushionFillMonths is taken from the free money before any other goal.
 */
data class FeasibilityInput(
    val goal: Goal, val otherGoals: List<Goal>, val savingRate: Money?, val currentMonth: YearMonth,
    val liquidAssets: Money, val averageMonthlyExpenses: Money, val variableBudgetsTotal: Money,
    val cushionMinMonths: Int = 3, val cushionFillMonths: Int = 12,
) {
    init {
        require(cushionMinMonths >= 0 && cushionFillMonths > 0)
        require(liquidAssets >= Money.ZERO && averageMonthlyExpenses >= Money.ZERO && variableBudgetsTotal >= Money.ZERO)
    }
}

const val MAX_POSTPONE_MONTHS = 60
const val MAX_INSTALLMENTS = 36

private fun ceilDiv(value: Long, divisor: Long): Long = if (value <= 0) 0 else (value - 1) / divisor + 1
private fun minus(a: Money, b: Money) = Money(Math.subtractExact(a.agorot, b.agorot))
private fun remaining(goal: Goal) = Money(maxOf(0L, goal.amount.agorot - goal.allocated.agorot))

/** Months from current to target, at least 1. A target already passed counts as 1 (all of it now). */
private fun monthsLeft(target: YearMonth, current: YearMonth): Long =
    maxOf(1L, ChronoUnit.MONTHS.between(current, target))

/** Rounded up, so saving this every month really reaches the goal on time. */
fun neededPerMonth(goal: Goal, currentMonth: YearMonth): Money =
    Money(ceilDiv(remaining(goal).agorot, monthsLeft(goal.targetMonth, currentMonth)))

fun isItPossible(input: FeasibilityInput): Feasibility {
    val goal = input.goal
    val current = input.currentMonth
    require(!goal.targetMonth.isBefore(current)) { "Target month ${goal.targetMonth} is in the past" }
    val saving = input.savingRate ?: return Feasibility.Unknown

    val free = minus(saving, sumMoney(input.otherGoals.map { neededPerMonth(it, current) }))
    val cushionTarget = Money(Math.multiplyExact(input.averageMonthlyExpenses.agorot, input.cushionMinMonths.toLong()))
    val cushionGap = maxOf(0L, cushionTarget.agorot - input.liquidAssets.agorot)
    val freeForGoal = minus(free, Money(ceilDiv(cushionGap, input.cushionFillMonths.toLong())))
    val needed = neededPerMonth(goal, current)
    val left = remaining(goal)

    // An ongoing cost after the goal must leave a positive monthly surplus; only a cut fixes that.
    val cost = goal.newMonthlyCost
    if (cost != null && minus(free, cost) <= Money.ZERO) {
        val cut = Money(maxOf(needed.agorot - freeForGoal.agorot, cost.agorot - free.agorot + 1))
        return if (cut <= input.variableBudgetsTotal) Feasibility.PossibleIf(needed, listOf(Lever.Cut(cut)))
        else Feasibility.NotNow(cut, null)
    }
    if (needed <= freeForGoal) return Feasibility.Possible(needed)

    val gap = minus(needed, freeForGoal)
    val levers = mutableListOf<Lever>()
    var beyondLimit: Lever? = null
    if (freeForGoal > Money.ZERO) {
        val months = ceilDiv(left.agorot, freeForGoal.agorot)
        val postpone = Lever.Postpone(current.plusMonths(months))
        if (months <= MAX_POSTPONE_MONTHS) levers += postpone else beyondLimit = postpone
    }
    if (gap <= input.variableBudgetsTotal) levers += Lever.Cut(gap)
    if (cushionGap == 0L) {
        val available = input.liquidAssets.agorot - cushionTarget.agorot
        val covered = Math.multiplyExact(maxOf(0L, freeForGoal.agorot), monthsLeft(goal.targetMonth, current))
        val take = left.agorot - covered
        if (take in 1..available) levers += Lever.TakeFromSavings(Money(take))
    }
    if (freeForGoal > Money.ZERO) {
        (1..MAX_INSTALLMENTS).firstOrNull { ceilDiv(left.agorot, it.toLong()) <= freeForGoal.agorot }?.let {
            levers += Lever.Installments(it, Money(ceilDiv(left.agorot, it.toLong())))
        }
    }
    return if (levers.isEmpty()) Feasibility.NotNow(gap, beyondLimit)
    else Feasibility.PossibleIf(needed, levers.take(3))
}
