package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.planVsActual
import com.menash.myself.feature.money.domain.roundUp
import com.menash.myself.feature.money.domain.sumMoney
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.PaamonimLine
import com.menash.myself.feature.money.importer.PlanGroup
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.toTransactions
import java.time.LocalDate
import java.time.YearMonth

/** The fewest ended months a draft is built from, as plan calibration asks. */
const val MIN_HISTORY_MONTHS = 3
private const val HISTORY_WINDOW = 6
private val monthlyStep = Money(5_000)
private val annualStep = Money(10_000)

/** Spending of one ended month by category: frozen at close, or from its imported lines when not closed yet. */
data class SpendingMonth(val month: YearMonth, val byCategory: Map<String, Money>)

/**
 * The last ended months with spending, newest last, up to six. A closed month uses its frozen figures; an
 * ended month that is not closed uses its imported lines, so a household that imported a few months of
 * files can get a draft before it closes anything. The current month is never used: it is not over.
 */
fun spendingHistory(data: MoneyData, stored: List<StoredTransaction>, memory: ImportMemory, today: LocalDate): List<SpendingMonth> {
    val current = YearMonth.from(today)
    val closed = data.closedMonths.filter { it.month < current }.associate { closed ->
        closed.month to closed.byCategory.mapNotNull { (id, amount) -> id?.let { it to amount } }.toMap()
    }
    val transactions by lazy { toTransactions(stored, memory) }
    val imported = stored.map { it.row.billingMonth }.filter { it < current && it !in closed }.distinct().associateWith { month ->
        planVsActual(month, MoneyItems(), transactions, null).categories
            .mapNotNull { row -> row.categoryId?.takeIf { row.actual.isPositive() }?.let { it to row.actual } }.toMap()
    }
    return (closed + imported).filterValues { it.isNotEmpty() }.toSortedMap().entries.toList()
        .takeLast(HISTORY_WINDOW).map { SpendingMonth(it.key, it.value) }
}

/**
 * A plan in the shape of a Paamonim budget list, so it goes through the same review as a file: per category,
 * fixed costs take the latest month, current spending the average, periodic costs the average times 12. Amounts
 * round up (₪50 a month, ₪100 a year) to leave a little room. Null with fewer than three months.
 * Incomes are not drafted: imports give a total, not who earned it. Sub-categories (single subscriptions) are left out.
 */
fun planFromHistory(history: List<SpendingMonth>, data: MoneyData): List<PaamonimLine>? {
    if (history.size < MIN_HISTORY_MONTHS) return null
    val categories = data.categories.associateBy { it.id }
    val ids = history.flatMap { it.byCategory.keys }.toSet()
    return ids.mapNotNull { id ->
        val category = categories[id] ?: return@mapNotNull null
        // A sub-category is one subscription; subscriptions are listed one by one on their own screen.
        if (category.parentId != null) return@mapNotNull null
        val values = history.map { it.byCategory[id] ?: Money.ZERO }
        val average = sumMoney(values) / values.size
        val (group, monthly, annual) = when (category.klass) {
            ExpenseClass.FIXED -> roundUp(values.last(), monthlyStep).let { Triple(PlanGroup.FIXED, it, it * 12) }
            ExpenseClass.CURRENT -> roundUp(average, monthlyStep).let { Triple(PlanGroup.CURRENT, it, it * 12) }
            ExpenseClass.PERIODIC -> roundUp(average * 12, annualStep).let { Triple(PlanGroup.PERIODIC, it / 12, it) }
        }
        if (!monthly.isPositive()) null else PaamonimLine(group, category.name, monthly, annual)
    }.sortedWith(compareBy<PaamonimLine> { it.group }.thenByDescending { it.monthly })
}
