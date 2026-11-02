package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MonthRange
import com.menash.myself.feature.money.domain.NetWorthChange
import com.menash.myself.feature.money.domain.PeriodSummary
import com.menash.myself.feature.money.domain.YearComparison
import com.menash.myself.feature.money.domain.compareYears
import com.menash.myself.feature.money.domain.categoryPeriod
import com.menash.myself.feature.money.domain.classYear
import com.menash.myself.feature.money.domain.netWorthChange
import com.menash.myself.feature.money.domain.periodSummary
import com.menash.myself.feature.money.domain.planVsActual
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** The ranges the period screen offers; a free range comes later. */
enum class PeriodPreset { LAST_3, LAST_6, THIS_YEAR, LAST_YEAR }

/**
 * The last month that can already be closed is last month, so every preset ends there. In January "this year"
 * has no month that can be closed yet: the range is January alone and the screen shows the empty state.
 */
fun periodRange(preset: PeriodPreset, today: LocalDate): MonthRange {
    val end = YearMonth.from(today).minusMonths(1)
    return when (preset) {
        PeriodPreset.LAST_3 -> MonthRange.last(end, 3)
        PeriodPreset.LAST_6 -> MonthRange.last(end, 6)
        PeriodPreset.THIS_YEAR -> if (end.year == today.year) MonthRange.year(today.year, end)
            else YearMonth.of(today.year, 1).let { MonthRange(it, it) }
        PeriodPreset.LAST_YEAR -> MonthRange.year(today.year - 1)
    }
}

/** One category's plan vs actual over the closed months of the range; [name] is null for uncategorised spending. */
data class PeriodCategoryRow(val categoryId: String?, val name: String?, val planned: Money?, val actual: Money, val diff: Money?)

data class PeriodView(
    val preset: PeriodPreset,
    val range: MonthRange,
    /** Null when no month of the range is closed. */
    val summary: PeriodSummary?,
    /** Fixed, current, periodic, then spending of an unknown class; zero classes are left out. */
    val classes: List<Pair<ExpenseClass?, Money>>,
    /** The largest categories by actual spending. */
    val categories: List<PeriodCategoryRow>,
    val netWorth: NetWorthChange?,
    /** The "last year" tab is labelled with the year itself. */
    val lastYear: Int,
    /** A calendar year against the year before, by monthly averages; null for the other ranges or without both years. */
    val comparison: YearComparison? = null,
    /** The categories whose monthly average grew the most against the year before, largest first. */
    val risers: List<PeriodRiser> = emptyList(),
)

/** A category whose monthly average rose against the year before. */
data class PeriodRiser(val categoryId: String?, val name: String?, val thisYear: Money, val previousYear: Money, val difference: Money)

private const val TopRisers = 3

private const val TopCategories = 6

/**
 * The plan of each closed month, per category, from today's items (the plan is not frozen at close).
 * A month that cannot be planned (a USD subscription with no stored rate) has no plan rather than a wrong one.
 */
internal fun plansFor(data: MoneyData, months: List<YearMonth>, rate: BigDecimal?): Map<YearMonth, Map<String?, Money>> =
    months.mapNotNull { month ->
        runCatching { planVsActual(month, data.items, emptyList(), rate) }.getOrNull()
            ?.let { result -> month to result.categories.associate { it.categoryId to it.planned } }
    }.toMap()

fun periodView(data: MoneyData, preset: PeriodPreset, today: LocalDate, rate: BigDecimal?): PeriodView {
    val range = periodRange(preset, today)
    val summary = periodSummary(data.closedMonths, range)
    if (summary == null) return PeriodView(preset, range, null, emptyList(), emptyList(), null, today.year - 1)
    val rows = categoryPeriod(data.closedMonths, range, plansFor(data, summary.closedMonths, rate))
    val byClass = classYear(rows, data.categories)
    val classes = listOf(ExpenseClass.FIXED, ExpenseClass.CURRENT, ExpenseClass.PERIODIC, null)
        .mapNotNull { klass -> byClass[klass]?.takeIf { it.isPositive() }?.let { klass to it } }
    val names = data.categories.associate { it.id to it.name }
    val categories = rows.filter { it.actual.isPositive() }.sortedByDescending { it.actual.agorot }.take(TopCategories)
        .map { PeriodCategoryRow(it.categoryId, it.categoryId?.let(names::get), it.planned, it.actual, it.diff) }
    val comparison = if (preset == PeriodPreset.THIS_YEAR || preset == PeriodPreset.LAST_YEAR)
        compareYears(data.closedMonths, range.from.year, emptyMap()) else null
    val risers = comparison?.categories.orEmpty().filter { it.averages.difference.isPositive() }
        .sortedByDescending { it.averages.difference.agorot }.take(TopRisers)
        .map { PeriodRiser(it.categoryId, it.categoryId?.let(names::get), it.averages.thisYear, it.averages.previousYear, it.averages.difference) }
    return PeriodView(
        preset, range, summary, classes, categories, netWorthChange(data.closedMonths, range), today.year - 1, comparison, risers,
    )
}

/** In January this year has no closed month yet, so the period screen opens on the year that just ended. */
fun defaultPreset(today: LocalDate): PeriodPreset =
    if (today.monthValue == 1) PeriodPreset.LAST_YEAR else PeriodPreset.THIS_YEAR
