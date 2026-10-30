package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth

/**
 * What the car really cost over the 12 billing months ending at [endMonth]: counted transactions (the same
 * list planVsActual takes) in [carCategoryIds], refunds reducing it. Null when no such line exists in the window.
 */
fun carActualYearly(rows: List<Transaction>, carCategoryIds: Set<String>, endMonth: YearMonth): Money? {
    val window = endMonth.minusMonths(11)..endMonth
    val lines = rows.filter { it.billingMonth in window && it.categoryId != null && it.categoryId in carCategoryIds }
    return if (lines.isEmpty()) null else sumMoney(lines.map { it.amount })
}
