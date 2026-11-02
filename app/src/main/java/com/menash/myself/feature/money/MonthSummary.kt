package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MonthRange
import com.menash.myself.feature.money.domain.NetWorthChange
import com.menash.myself.feature.money.domain.PeriodicStatus
import com.menash.myself.feature.money.domain.SuggestionStatus
import com.menash.myself.feature.money.domain.categoryBaseline
import com.menash.myself.feature.money.domain.categoryPeriod
import com.menash.myself.feature.money.domain.netWorthChange
import com.menash.myself.feature.money.domain.subtractMoney
import com.menash.myself.feature.money.domain.suggestPlan
import com.menash.myself.feature.money.domain.sumMoney
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/** One class of the closed month against its plan; [planned] is null when nothing of the class is planned. */
data class SummaryClassRow(val klass: ExpenseClass?, val planned: Money?, val actual: Money)

/** A category that went over its plan, by how much. */
data class SummaryOverrun(val categoryId: String?, val name: String?, val planned: Money, val actual: Money, val over: Money)

data class SummaryPeriodic(val budgetId: String, val name: String, val status: PeriodicStatus)

/** A category whose plan the last closed months suggest changing. */
data class SummarySuggestion(val categoryId: String, val name: String, val current: Money?, val suggested: Money)

/**
 * "סיכום החודש" for a closed month, read only: what was left, how it compares, where the plan held and where
 * it did not, the periodic funds, net worth, and which plans the closed months suggest changing.
 */
data class MonthSummaryView(
    val month: YearMonth,
    val closed: ClosedMonth,
    val income: Money,
    val expenses: Money,
    val left: Money,
    /** Left as a share of income; null without income. */
    val leftPercent: BigDecimal?,
    /** The average left over in up to three closed months before this one; null when there are none. */
    val previousAverage: Money?,
    val previousCount: Int,
    val classes: List<SummaryClassRow>,
    val overruns: List<SummaryOverrun>,
    val periodic: List<SummaryPeriodic>,
    /** Null when this month's balances are unknown or there is no earlier known close to compare with. */
    val netWorth: NetWorthChange?,
    /** Null until three months are closed: too few to suggest anything. */
    val suggestions: List<SummarySuggestion>?,
    /** How many closed months the suggestions read. */
    val suggestionMonths: Int,
) {
    val vsAverage: Money? get() = previousAverage?.let { subtractMoney(left, it) }
}

private const val CompareMonths = 3
private const val TopOverruns = 3
private const val TopSuggestions = 3

private fun percent(part: Money, whole: Money): BigDecimal? = if (whole == Money.ZERO) null else
    BigDecimal.valueOf(part.agorot).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(whole.agorot), 1, RoundingMode.HALF_UP)

private fun ClosedMonth.left(): Money = subtractMoney(snapshot.actualIncomes, snapshot.actualExpenses)

/** Null when [month] is not closed. The plan is today's plan for that month, as on the period screen. */
fun monthSummary(data: MoneyData, month: YearMonth, rate: BigDecimal?): MonthSummaryView? {
    val closed = data.closedMonths.firstOrNull { it.month == month } ?: return null
    val names = data.categories.associate { it.id to it.name }
    val classOf = data.categories.associate { it.id to it.klass }
    val plans = plansFor(data, listOf(month), rate)
    val rows = categoryPeriod(listOf(closed), MonthRange(month, month), plans)

    val classes = listOf(ExpenseClass.FIXED, ExpenseClass.CURRENT, ExpenseClass.PERIODIC, null).mapNotNull { klass ->
        val inClass = rows.filter { classOf[it.categoryId] == klass }
        val actual = sumMoney(inClass.map { it.actual })
        val planned = inClass.mapNotNull { it.planned }.takeIf { it.isNotEmpty() }?.let(::sumMoney)?.takeIf { it.isPositive() }
        if (!actual.isPositive() && planned == null) null else SummaryClassRow(klass, planned, actual)
    }
    val overruns = rows.filter { row -> row.diff?.isPositive() == true && row.planned?.isPositive() == true }
        .sortedByDescending { it.diff!!.agorot }.take(TopOverruns)
        .map { SummaryOverrun(it.categoryId, it.categoryId?.let(names::get), it.planned!!, it.actual, it.diff!!) }

    val budgets = data.items.periodicBudgets.associateBy { it.id }
    val periodic = closed.periodic.mapNotNull { (id, status) -> budgets[id]?.let { SummaryPeriodic(id, it.name, status) } }
        .sortedBy { it.name }

    val earlier = data.closedMonths.filter { it.month < month }.sortedByDescending { it.month }.take(CompareMonths)
    val previousAverage = if (earlier.isEmpty()) null else sumMoney(earlier.map { it.left() }) / earlier.size.toLong()

    val worth = netWorthChange(data.closedMonths, MonthRange(month, month))?.takeIf { it.startFrom < month }

    val baseline = categoryBaseline(data.closedMonths, month)
    val currentPlan = plans[month].orEmpty().mapNotNull { (id, amount) -> id?.takeIf { amount.isPositive() }?.let { it to amount } }.toMap()
    val raw = suggestPlan(baseline, currentPlan, data.categories)
    val suggestions = if (raw.any { it.status == SuggestionStatus.NOT_ENOUGH_DATA } || baseline.months.size < CompareMonths) null
    else raw.filter { it.status in Changes && it.suggested != null && it.categoryId in names }.take(TopSuggestions)
        .map { SummarySuggestion(it.categoryId, names.getValue(it.categoryId), it.current, it.suggested!!) }

    val left = closed.left()
    return MonthSummaryView(
        month = month,
        closed = closed,
        income = closed.snapshot.actualIncomes,
        expenses = closed.snapshot.actualExpenses,
        left = left,
        leftPercent = percent(left, closed.snapshot.actualIncomes),
        previousAverage = previousAverage,
        previousCount = earlier.size,
        classes = classes,
        overruns = overruns,
        periodic = periodic,
        netWorth = worth,
        suggestions = suggestions,
        suggestionMonths = baseline.months.size,
    )
}

private val Changes = setOf(SuggestionStatus.UNDER_PLANNED, SuggestionStatus.OVER_PLANNED, SuggestionStatus.NO_PLAN, SuggestionStatus.UNUSED)
