package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/** An inclusive run of calendar months. The caller picks the end (normally the last month that can be closed). */
data class MonthRange(val from: YearMonth, val to: YearMonth) {
    init { require(from <= to) { "Range must not end before it starts" } }

    val months: List<YearMonth>
        get() = generateSequence(from) { it.plusMonths(1) }.takeWhile { it <= to }.toList()

    companion object {
        /** The [count] months ending with [end], e.g. last(Oct 2026, 3) = Aug–Oct 2026. */
        fun last(end: YearMonth, count: Int): MonthRange {
            require(count > 0) { "At least one month is required" }
            return MonthRange(end.minusMonths((count - 1).toLong()), end)
        }

        /** January through [end] when [end] is in [year], otherwise the whole calendar year. */
        fun year(year: Int, end: YearMonth? = null): MonthRange {
            val last = if (end != null && end.year == year) end else YearMonth.of(year, 12)
            return MonthRange(YearMonth.of(year, 1), last)
        }
    }
}

data class MonthFlow(val month: YearMonth, val income: Money, val expenses: Money, val saving: Money)

data class PeriodSummary(
    val range: MonthRange,
    val closedMonths: List<YearMonth>,
    /** Every month of the range without a close. They are never counted as zero. */
    val missingMonths: List<YearMonth>,
    val income: Money,
    val expenses: Money,
    val saving: Money,
    val savingPercent: BigDecimal?,
    val averageMonthlyIncome: Money,
    val averageMonthlyExpenses: Money,
    val averageMonthlySaving: Money,
    /** One entry per closed month, oldest first; missing months are absent. */
    val months: List<MonthFlow>,
    val bestMonth: YearMonth,
    val worstMonth: YearMonth,
)

internal fun rangeCloses(closed: List<ClosedMonth>, range: MonthRange): List<ClosedMonth> {
    val rows = closed.filter { it.month >= range.from && it.month <= range.to }.sortedBy { it.month }
    require(rows.map { it.month }.distinct().size == rows.size) { "One close per month is required" }
    return rows
}

internal fun percentOf(part: Money, whole: Money): BigDecimal? = if (whole == Money.ZERO) null else
    BigDecimal.valueOf(part.agorot).multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(whole.agorot), 1, RoundingMode.HALF_UP)

/** Totals and averages over the closed months of [range]; null when none of them is closed. */
fun periodSummary(closed: List<ClosedMonth>, range: MonthRange): PeriodSummary? {
    val rows = rangeCloses(closed, range)
    if (rows.isEmpty()) return null
    val months = rows.map { it.month }
    val flows = rows.map {
        MonthFlow(it.month, it.snapshot.actualIncomes, it.snapshot.actualExpenses,
            subtractMoney(it.snapshot.actualIncomes, it.snapshot.actualExpenses))
    }
    val income = sumMoney(flows.map { it.income })
    val expenses = sumMoney(flows.map { it.expenses })
    val saving = subtractMoney(income, expenses)
    val n = rows.size
    return PeriodSummary(
        range, months, range.months.filter { it !in months },
        income, expenses, saving, percentOf(saving, income),
        income / n, expenses / n, saving / n, flows,
        flows.maxByOrNull { it.saving }!!.month, flows.minByOrNull { it.saving }!!.month,
    )
}

/** Plan vs actual per category, summed only over the closed months of [range]. Same order as [categoryYear]. */
fun categoryPeriod(
    closed: List<ClosedMonth>, range: MonthRange, plannedByMonth: Map<YearMonth, Map<String?, Money>>,
): List<CategoryYear> = categoryTotals(rangeCloses(closed, range), plannedByMonth)

/** Periodic budgets as frozen at the last close inside [range]; empty when the range has none. */
fun periodicAtPeriodEnd(closed: List<ClosedMonth>, range: MonthRange): Map<String, PeriodicStatus> =
    rangeCloses(closed, range).lastOrNull()?.periodic ?: emptyMap()

/**
 * Net worth at the last close of [range] with known balances, against the last known close before the
 * range (any earlier month), or the first known close inside it. Closes with no balances are unknown and skipped.
 */
fun netWorthChange(closed: List<ClosedMonth>, range: MonthRange): NetWorthChange? {
    fun known(row: ClosedMonth) = row.balances.isNotEmpty() || row.liabilities.isNotEmpty()
    val rows = rangeCloses(closed, range).filter(::known)
    if (rows.isEmpty()) return null
    val before = closed.filter { it.month < range.from && known(it) }.maxByOrNull { it.month }
    val first = before ?: rows.first()
    fun worth(row: ClosedMonth) = subtractMoney(sumMoney(row.balances.values), sumMoney(row.liabilities.values))
    val start = worth(first)
    val end = worth(rows.last())
    return NetWorthChange(start, end, subtractMoney(end, start), first.month)
}
