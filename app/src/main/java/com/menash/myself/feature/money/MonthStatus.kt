package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.StoredTransaction
import java.time.LocalDate
import java.time.YearMonth

/**
 * The headline of the month screen. Menash 02/11: an open month leads with "נשאר לשוטפות"; the expected
 * surplus and what was spent so far are shown smaller. A closed month shows its frozen result.
 */
sealed interface MonthStatus {
    /** Open month with nothing imported for it yet: only the plan is known. */
    data class PlanOnly(val expectedSurplus: Money) : MonthStatus

    /** Open month with imported lines. [dataUntil] is the latest counted line's date, so the screen says how fresh it is. */
    data class Open(
        val leftForCurrent: Money,
        val currentBudget: Money,
        val currentSpent: Money,
        val expectedSurplus: Money,
        val spentSoFar: Money,
        val dataUntil: LocalDate?,
    ) : MonthStatus {
        val usage: UsageLevel get() = usageLevel(currentSpent, currentBudget)
    }

    data class NoCurrentBudget(val spentSoFar: Money, val unassigned: Money?, val dataUntil: LocalDate?) : MonthStatus

    /** A closed month: income minus expenses as frozen at the close. */
    data class Closed(val left: Money, val income: Money, val expenses: Money, val closedOn: LocalDate) : MonthStatus
}

/** Normal below 80% of the budget, near from 80% up to 100%, over above it. Spending with no budget is over. */
enum class UsageLevel { NORMAL, NEAR, OVER }

fun usageLevel(spent: Money, budget: Money): UsageLevel = when {
    !budget.isPositive() -> if (spent.isPositive()) UsageLevel.OVER else UsageLevel.NORMAL
    spent.agorot > budget.agorot -> UsageLevel.OVER
    spent.agorot * 5 >= budget.agorot * 4 -> UsageLevel.NEAR
    else -> UsageLevel.NORMAL
}

/** Counted expense rows; income, saving deposits and debts are not "spent". */
private val spendingRows = setOf(MonthRowKind.FIXED, MonthRowKind.CURRENT, MonthRowKind.PERIODIC, MonthRowKind.INSTALLMENTS)

internal fun monthStatus(
    view: MonthView.Planned,
    closed: ClosedMonth?,
    stored: List<StoredTransaction>,
    month: YearMonth,
): MonthStatus {
    if (closed != null) {
        val income = closed.snapshot.actualIncomes
        val expenses = closed.snapshot.actualExpenses
        return MonthStatus.Closed(Money(Math.subtractExact(income.agorot, expenses.agorot)), income, expenses, closed.closedOn)
    }
    val imported = view.importedLines > 0
    if (!imported) return MonthStatus.PlanOnly(view.plan.expectedSurplus)
    val current = view.rows.firstOrNull { it.kind == MonthRowKind.CURRENT }
    val budget = current?.total ?: Money.ZERO
    val spent = current?.actual ?: Money.ZERO
    val spentSoFar = (view.rows.filter { it.kind in spendingRows }.mapNotNull { it.actual } + listOfNotNull(view.unassigned))
        .fold(0L) { sum, it -> Math.addExact(sum, it.agorot) }
    val until = stored.filter { it.row.billingMonth == month && it.row.status == RowStatus.COUNTED }.maxOfOrNull { it.row.date }
    if (!budget.isPositive()) return MonthStatus.NoCurrentBudget(Money(spentSoFar), view.unassigned, until)
    return MonthStatus.Open(
        leftForCurrent = Money(Math.subtractExact(budget.agorot, spent.agorot)),
        currentBudget = budget,
        currentSpent = spent,
        expectedSurplus = view.plan.expectedSurplus,
        spentSoFar = Money(spentSoFar),
        dataUntil = until,
    )
}
