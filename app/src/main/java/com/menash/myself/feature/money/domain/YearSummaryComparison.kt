package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

data class MonthlyComparison(val thisYear: Money, val previousYear: Money, val difference: Money)
data class CategoryYearComparison(val categoryId: String?, val averages: MonthlyComparison)
data class YearComparison(
    val year: Int, val monthsThis: Int, val monthsPrevious: Int,
    val income: MonthlyComparison, val expenses: MonthlyComparison, val saving: MonthlyComparison,
    val categories: List<CategoryYearComparison>,
)

fun compareYears(closed: List<ClosedMonth>, year: Int, plannedByMonth: Map<YearMonth, Map<String?, Money>>): YearComparison? {
    val current = yearSummary(closed, year) ?: return null
    val previousYear = Math.subtractExact(year, 1)
    val previous = yearSummary(closed, previousYear) ?: return null
    val count = current.closedMonths.size
    val previousCount = previous.closedMonths.size
    fun comparison(a: Money, b: Money): MonthlyComparison {
        val x = BigDecimal.valueOf(a.agorot)
        val y = BigDecimal.valueOf(b.agorot)
        val n = BigDecimal.valueOf(count.toLong())
        val p = BigDecimal.valueOf(previousCount.toLong())
        fun rounded(numerator: BigDecimal, denominator: BigDecimal) =
            Money(numerator.divide(denominator, 0, RoundingMode.HALF_UP).longValueExact())
        return MonthlyComparison(rounded(x, n), rounded(y, p), rounded(x.multiply(p).subtract(y.multiply(n)), n.multiply(p)))
    }
    val currentCategories = categoryYear(closed, year, plannedByMonth).associate { it.categoryId to it.actual }
    val previousCategories = categoryYear(closed, previousYear, plannedByMonth).associate { it.categoryId to it.actual }
    val categories = (currentCategories.keys + previousCategories.keys).sortedWith(compareBy { it }).map { id ->
        CategoryYearComparison(id, comparison(currentCategories[id] ?: Money.ZERO, previousCategories[id] ?: Money.ZERO))
    }
    return YearComparison(year, count, previousCount, comparison(current.income, previous.income),
        comparison(current.expenses, previous.expenses), comparison(current.saving, previous.saving), categories)
}
