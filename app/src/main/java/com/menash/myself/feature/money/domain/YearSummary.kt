package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

data class YearSummary(
    val year: Int,
    val closedMonths: List<YearMonth>,
    val missingMonths: List<YearMonth>,
    val income: Money,
    val expenses: Money,
    val saving: Money,
    val savingPercent: BigDecimal?,
    val averageMonthlyExpenses: Money,
    val bestMonth: YearMonth,
    val worstMonth: YearMonth,
)

internal fun yearCloses(closed: List<ClosedMonth>, year: Int): List<ClosedMonth> {
    val rows = closed.filter { it.month.year == year }.sortedBy { it.month }
    require(rows.map { it.month }.distinct().size == rows.size) { "One close per month is required" }
    return rows
}

internal fun subtractMoney(a: Money, b: Money) = Money(Math.subtractExact(a.agorot, b.agorot))

fun yearSummary(closed: List<ClosedMonth>, year: Int): YearSummary? {
    val rows = yearCloses(closed, year)
    if (rows.isEmpty()) return null
    val months = rows.map { it.month }
    val missing = generateSequence(months.first()) { it.plusMonths(1) }
        .takeWhile { it <= months.last() }.filter { it !in months }.toList()
    val income = sumMoney(rows.map { it.snapshot.actualIncomes })
    val expenses = sumMoney(rows.map { it.snapshot.actualExpenses })
    val saving = subtractMoney(income, expenses)
    val percent = if (income == Money.ZERO) null else BigDecimal.valueOf(saving.agorot)
        .multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(income.agorot), 1, RoundingMode.HALF_UP)
    val bySaving = rows.map { it.month to subtractMoney(it.snapshot.actualIncomes, it.snapshot.actualExpenses) }
    return YearSummary(year, months, missing, income, expenses, saving, percent, expenses / rows.size,
        bySaving.maxByOrNull { it.second }!!.first, bySaving.minByOrNull { it.second }!!.first)
}
