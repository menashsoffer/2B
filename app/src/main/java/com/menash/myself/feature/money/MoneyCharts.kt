package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.CategoryActual
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.actualIncome
import com.menash.myself.feature.money.importer.monthLines
import java.time.YearMonth

/*
 * Data for the money charts. Pure functions over what the month and big-picture screens already hold;
 * the generic chart components in core/designsystem/component/charts draw them.
 */

/** One class bar in the month: plan (null for "no category yet") against what was spent. */
data class ClassBar(val kind: MonthRowKind?, val plan: Money?, val actual: Money)

/** A category among the month's largest spends; [plan] is null when the category has no plan. */
data class TopCategory(val categoryId: String, val name: String, val plan: Money?, val actual: Money)

data class CategoryTop(val shown: List<TopCategory>, val more: Int)

/** How complete one month's numbers are: closed, open with imported lines so far, or nothing at all. */
enum class FlowState { CLOSED, SO_FAR, MISSING }

data class MonthFlowPoint(val month: YearMonth, val income: Money, val expenses: Money, val state: FlowState)

/** Net worth at a month end; null when that month's balances are unknown (never drawn as zero). */
data class WorthPoint(val month: YearMonth, val worth: Money?)

private val chartClasses = listOf(MonthRowKind.FIXED, MonthRowKind.CURRENT, MonthRowKind.PERIODIC, MonthRowKind.INSTALLMENTS)

/** Plan vs actual per class once lines are imported, then uncategorised spending. Empty before any import. */
fun classBars(view: MonthView.Planned): List<ClassBar> {
    val bars = chartClasses.mapNotNull { kind ->
        val row = view.rows.firstOrNull { it.kind == kind } ?: return@mapNotNull null
        val actual = row.actual ?: return@mapNotNull null
        if (actual.isZero() && row.total.isZero()) null else ClassBar(kind, row.total, actual)
    }
    val unassigned = view.unassigned?.let { ClassBar(null, null, it) }
    if (bars.isEmpty() && unassigned == null) return emptyList()
    return bars + listOfNotNull(unassigned)
}

/** The month's largest categories by actual spending, at most [limit]; [CategoryTop.more] counts the rest. */
fun topCategories(actuals: List<CategoryActual>, categories: List<Category>, limit: Int = 5): CategoryTop {
    val names = categories.associate { it.id to it.name }
    val spent = actuals.filter { it.categoryId != null && it.categoryId in names && it.actual.isPositive() }
        .sortedByDescending { it.actual.agorot }
    return CategoryTop(
        shown = spent.take(limit).map { TopCategory(it.categoryId!!, names.getValue(it.categoryId), it.planned.takeIf { p -> p.isPositive() }, it.actual) },
        more = (spent.size - limit).coerceAtLeast(0),
    )
}

/**
 * The last [count] months up to [current]: a closed month shows its frozen figures; an open month with
 * imported lines shows income and counted spending so far; a month with neither is missing.
 */
fun monthFlows(
    closed: List<ClosedMonth>,
    stored: List<StoredTransaction>,
    memory: ImportMemory,
    categories: List<Category>,
    current: YearMonth,
    count: Int = 6,
): List<MonthFlowPoint> {
    val byMonth = closed.associateBy { it.month }
    val imported = stored.map { it.row.billingMonth }.toSet()
    return (count - 1 downTo 0).map { back ->
        val month = current.minusMonths(back.toLong())
        val close = byMonth[month]
        when {
            close != null -> MonthFlowPoint(month, close.snapshot.actualIncomes, close.snapshot.actualExpenses, FlowState.CLOSED)
            month in imported -> MonthFlowPoint(
                month,
                actualIncome(stored, month, memory),
                monthLines(stored, month, memory, categories).spent,
                FlowState.SO_FAR,
            )
            else -> MonthFlowPoint(month, Money.ZERO, Money.ZERO, FlowState.MISSING)
        }
    }
}

/**
 * Net worth after each closed month that has balances, oldest first, then today's [now] for [current].
 * A close with no balances (a retro close where they were unknown) is a gap. At most [maxPoints].
 */
fun worthHistory(closed: List<ClosedMonth>, now: Money?, current: YearMonth, maxPoints: Int = 12): List<WorthPoint> {
    val past = closed.filter { it.month < current }.sortedBy { it.month }.map { close ->
        val known = close.balances.isNotEmpty() || close.liabilities.isNotEmpty()
        val worth = if (known) {
            Money(Math.subtractExact(close.balances.values.sumOf { it.agorot }, close.liabilities.values.sumOf { it.agorot }))
        } else null
        WorthPoint(close.month, worth)
    }
    return (past + WorthPoint(current, now)).takeLast(maxPoints)
}
