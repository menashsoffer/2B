package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MonthCloseRenewalTest {
    private val start = YearMonth.of(2025, 1)
    private val budget = PeriodicBudget("annual", "car", "Car costs", Money(12000), start)
    private fun tx(id: String, month: YearMonth, amount: Long, category: String = "car") =
        Transaction(id, month.atDay(1), month, "merchant", Money(amount), PaymentMethod.CASH, "cash", category)
    private fun close(month: YearMonth, budgets: List<PeriodicBudget> = listOf(budget), txs: List<Transaction> = emptyList()) =
        closeMonth(month, MoneyItems(periodicBudgets = budgets), txs, Money.ZERO, BalancesAtClose.Unknown, null, month.atEndOfMonth().plusDays(1))

    @Test fun marchNextYearAccruesThreeMonthsAndUsesOnlyRenewedYearsHistory() {
        val month = YearMonth.of(2026, 3)
        val txs = listOf(
            tx("old", YearMonth.of(2025, 12), 8000), tx("jan", YearMonth.of(2026, 1), 4000),
            tx("march", month, 500), tx("future", month.plusMonths(1), 9000), tx("other", month, 9000, "other"),
        )
        val status = close(month, txs = txs).periodic.getValue("annual")
        assertEquals(PeriodicStatus(Money(3000), Money(4500), Money(7500)), status)
        assertEquals(Money(1500), status.advance)
        assertEquals(YearMonth.of(2025, 1), budget.yearStartMonth)
        assertEquals(PeriodicBudget("annual", "car", "Car costs", Money(12000), start), budget)
    }

    @Test fun renewsExactlyAtAnniversary() {
        val anniversary = start.plusYears(1)
        val status = close(anniversary, txs = listOf(tx("old", anniversary.minusMonths(1), 9000), tx("new", anniversary, 100))).periodic.getValue("annual")
        assertEquals(PeriodicStatus(Money(1000), Money(100), Money(11900)), status)
    }

    @Test fun lastMonthBeforeAnniversaryAccruesFullAnnualAmount() {
        assertEquals(Money(12000), close(start.plusMonths(11)).periodic.getValue("annual").accruedToDate)
    }

    @Test fun multipleElapsedYearsRenewWithoutChangingOriginalBudgetList() {
        val budgets = mutableListOf(budget)
        val month = start.plusYears(4).plusMonths(2)
        val closed = close(month, budgets, listOf(tx("old", start, 11000), tx("new", month.minusMonths(1), 700)))
        budgets.clear()
        assertEquals(PeriodicStatus(Money(3000), Money(700), Money(11300)), closed.periodic["annual"])
        assertEquals(start, budget.yearStartMonth)
    }

    @Test fun nonJanuaryYearSpansCalendarYearAndRenewsInItsStartMonth() {
        val july = budget.copy(yearStartMonth = YearMonth.of(2024, 7))
        val march = YearMonth.of(2026, 3)
        val txs = listOf(tx("old", YearMonth.of(2025, 6), 10000), tx("july", YearMonth.of(2025, 7), 200), tx("new", march, 300))
        assertEquals(PeriodicStatus(Money(9000), Money(500), Money(11500)), close(march, listOf(july), txs).periodic["annual"])
        assertEquals(Money(1000), close(YearMonth.of(2026, 7), listOf(july)).periodic.getValue("annual").accruedToDate)
        assertEquals(YearMonth.of(2024, 7), july.yearStartMonth)
    }

    @Test fun futureBudgetsAreOmittedWithoutBlockingCloseAndStartMonthIsIncluded() {
        val future = budget.copy(id = "future", categoryId = "future-cat", yearStartMonth = start.plusMonths(1))
        val closed = close(start, listOf(budget, future))
        assertEquals(setOf("annual"), closed.periodic.keys)
        assertEquals(mapOf<String?, Money>("car" to Money.ZERO), closed.byCategory)
        assertEquals(Money(1000), closed.periodic.getValue("annual").accruedToDate)
        val beforeStart = close(start.minusMonths(1))
        assertTrue(beforeStart.periodic.isEmpty())
        assertTrue(beforeStart.byCategory.isEmpty())
        assertTrue(canClose(closeChecks(CloseInput(start, closed.closedOn, emptySet(), emptySet(), 0, 0, emptyList()))))
    }
}
