package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

data class CategoryYear(val categoryId: String?, val planned: Money?, val actual: Money, val diff: Money?)

fun categoryYear(closed: List<ClosedMonth>, year: Int, plannedByMonth: Map<YearMonth, Map<String?, Money>>): List<CategoryYear> {
    val rows = yearCloses(closed, year)
    val actual = mutableMapOf<String?, Money>()
    val planned = mutableMapOf<String?, Money>()
    fun add(target: MutableMap<String?, Money>, entries: Map<String?, Money>) {
        entries.forEach { (id, amount) -> target[id] = sumMoney(listOf(target[id] ?: Money.ZERO, amount)) }
    }
    rows.forEach { row ->
        add(actual, row.byCategory)
        add(planned, plannedByMonth[row.month].orEmpty())
    }
    return (actual.keys + planned.keys).map { id ->
        val amount = actual[id] ?: Money.ZERO
        val plan = planned[id]
        CategoryYear(id, plan, amount, plan?.let { subtractMoney(amount, it) })
    }.sortedWith(compareByDescending<CategoryYear> { it.diff != null }
        .thenByDescending { it.diff?.agorot }.thenByDescending { if (it.diff == null) it.actual.agorot else 0L }
        .thenBy { it.categoryId })
}

fun classYear(rows: List<CategoryYear>, categories: List<Category>): Map<ExpenseClass?, Money> {
    val classes = categories.associate { it.id to it.klass }
    val result = mutableMapOf<ExpenseClass?, Money>()
    rows.forEach { row ->
        val klass = classes[row.categoryId]
        result[klass] = sumMoney(listOf(result[klass] ?: Money.ZERO, row.actual))
    }
    return result
}
