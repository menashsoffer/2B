package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Frozen actuals and id-only balances; liabilities retain their nonnegative magnitude. */
data class ClosedMonth(
    val month: YearMonth,
    val snapshot: MonthSnapshot,
    val byCategory: Map<String?, Money>,
    val periodic: Map<String, PeriodicStatus>,
    val balances: Map<String, Money>,
    val liabilities: Map<String, Money>,
    val closedOn: LocalDate,
) {
    init {
        require(snapshot.month == month) { "Snapshot month must match the close" }
        require(balances.keys.intersect(liabilities.keys).isEmpty()) { "Asset ids must be unique" }
        require((balances.values + liabilities.values).all { it >= Money.ZERO })
    }
}

/**
 * The caller supplies counted transactions (including full history for periodic budgets), actual
 * income and the exchange rate, and enforces closeChecks. No source objects are retained or changed.
 */
fun closeMonth(
    month: YearMonth,
    items: MoneyItems,
    transactions: List<Transaction>,
    actualIncome: Money,
    balancesAtClose: BalancesAtClose,
    usdToIls: BigDecimal?,
    today: LocalDate,
): ClosedMonth {
    val (balances, liabilities) = balancesAtClose.maps()
    val total = sumMoney(transactions.filter { it.billingMonth == month }.map { it.amount })
    val actuals = planVsActual(month, items, transactions, usdToIls)
    val periodic = items.periodicBudgets.filter { it.yearStartMonth <= month }.associate { budget ->
        val yearsElapsed = ChronoUnit.MONTHS.between(budget.yearStartMonth, month) / 12
        val renewed = budget.copy(yearStartMonth = budget.yearStartMonth.plusYears(yearsElapsed))
        budget.id to periodicStatus(renewed, month, transactions)
    }
    return ClosedMonth(
        month = month,
        snapshot = MonthSnapshot(month, actualIncome, maxOf(total, Money.ZERO), balances = emptyList()),
        byCategory = actuals.categories.associate { it.categoryId to it.actual },
        periodic = periodic,
        balances = balances,
        liabilities = liabilities,
        closedOn = today,
    )
}
