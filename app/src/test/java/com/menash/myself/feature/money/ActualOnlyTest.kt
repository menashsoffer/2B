package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ActualOnlyTest {
    @Test fun recurringPlanCreatedLaterDoesNotMaskImportedHistory() {
        val month = YearMonth.of(2024, 2)
        val line = StoredTransaction(StatementRow("old", Source.HAPOALIM, "1234", month.atDay(5), null,
            "shop", charge = Money(500)), "current-1")
        val data = MoneyData(items = com.menash.myself.feature.money.domain.MoneyItems(incomes = listOf(MoneySamples.salary)),
            categories = defaultCategories())
        val actual = buildMoneyMonth(data, month, LocalDate.of(2026, 11, 3), null, listOf(line)).view as MonthView.ActualOnly
        assertEquals(Money(500), actual.spent)
    }

    @Test fun legacyAndFutureSavingsDoNotInventAPlanInHistory() {
        val history = YearMonth.of(2024, 2)
        val current = YearMonth.of(2026, 11)
        val line = StoredTransaction(StatementRow("expense", Source.HAPOALIM, "1234", history.atDay(5), null,
            "shop", charge = Money(500)), "current-1")
        val legacy = com.menash.myself.feature.money.domain.Saving("old", "legacy", Money(100), Money(0))
        val future = legacy.copy(id = "new", startMonth = current)
        val data = MoneyData(items = com.menash.myself.feature.money.domain.MoneyItems(savings = listOf(legacy, future)),
            categories = defaultCategories())
        val actual = buildMoneyMonth(data, history, current.atDay(5), null, listOf(line)).view as MonthView.ActualOnly
        assertEquals(Money(500), actual.spent)
        val now = buildMoneyMonth(data, current, current.atDay(5), null).view as MonthView.Planned
        assertEquals(Money(200), now.plan.savingDeposits)
    }

    @Test fun historicalMonthUsesOnlyImportedAmounts() {
        val month = YearMonth.of(2024, 2)
        fun line(id: String, amount: Long, category: String? = null, operation: String? = null) = StoredTransaction(
            StatementRow(id, Source.HAPOALIM, "1234", month.atDay(5), null, id, charge = Money(amount), operation = operation), category)
        val data = MoneyData(categories = defaultCategories())
        val lines = listOf(line("salary", -100_000, operation = "משכורת"), line("shop", 20_000, "current-1"), line("other", 3_000))
        val actual = buildMoneyMonth(data, month, LocalDate.of(2026, 11, 3), null, lines).view as MonthView.ActualOnly
        assertEquals(Money(100_000), actual.income)
        assertEquals(Money(23_000), actual.spent)
        assertEquals(Money(77_000), actual.saving)
        assertEquals(Money(3_000), actual.unassigned)
        assertEquals(3, actual.importedLines)
        assertEquals(Money(20_000), actual.top.shown.single().actual)
    }
}
