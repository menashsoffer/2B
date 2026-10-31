package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

data class CalibrationBaseline(val months: List<YearMonth>, val rows: List<CategoryBaseline>)
data class CategoryBaseline(
    val categoryId: String?, val average: Money, val median: Money, val min: Money,
    val max: Money, val latest: Money, val activeMonths: Int,
)

/** Uses closed months only; absent categories in a closed month count as zero. */
fun categoryBaseline(closed: List<ClosedMonth>, endMonth: YearMonth, window: Int = 6): CalibrationBaseline {
    require(window in 1..24)
    require(closed.map { it.month }.distinct().size == closed.size) { "One close per month is required" }
    val selected = closed.filter { it.month <= endMonth }.sortedByDescending { it.month }.take(window).sortedBy { it.month }
    val ids = selected.flatMap { it.byCategory.keys }.toSet()
    val rows = ids.map { id ->
        val values = selected.map { it.byCategory[id] ?: Money.ZERO }
        val sorted = values.sorted()
        val n = sorted.size
        val median = if (n % 2 == 1) sorted[n / 2] else sumMoney(listOf(sorted[n / 2 - 1], sorted[n / 2])) / 2
        CategoryBaseline(id, sumMoney(values) / n, median, sorted.first(), sorted.last(), values.last(),
            values.count { it != Money.ZERO })
    }.sortedWith(compareByDescending<CategoryBaseline> { it.average }.thenBy { it.categoryId })
    return CalibrationBaseline(selected.map { it.month }, rows)
}
