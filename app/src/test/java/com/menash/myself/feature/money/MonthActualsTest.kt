package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import com.menash.myself.feature.money.importer.actualIncome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

/** "בפועל" on the month rows comes from imported lines through planVsActual, and only when there are some. */
class MonthActualsTest {

    private val november = YearMonth.of(2026, 11)
    private val october = YearMonth.of(2026, 10)

    private fun planned(month: YearMonth, lines: List<StoredTransaction>): MonthView.Planned =
        buildMoneyMonth(MoneySamples.filled, month, month.atDay(5), MoneySamples.rate, lines).view as MonthView.Planned

    private fun lines(vararg names: String) = names.flatMap { ImportFixtures.parse(it).rows }.map { StoredTransaction(it) }

    @Test
    fun noImportedLines_monthUnchanged() {
        val view = planned(october, emptyList())
        assertEquals(buildMoneyMonth(MoneySamples.filled, october, october.atDay(5), MoneySamples.rate).view, view)
        view.rows.forEach { row -> assertNull(row.actual); row.items.forEach { assertNull(it.actual) } }
        assertNull(view.unassigned)
    }

    @Test
    fun categorisedSpendingFillsRowAndItem() {
        val shufersal = lines("max_nov.xlsx").map { if (it.row.description == "שופרסל דיל 123") it.copy(categoryId = "current-1") else it }
        val view = planned(november, shufersal)
        val current = view.rows.single { it.kind == MonthRowKind.CURRENT }
        assertEquals(Money(25_430), current.actual)
        assertEquals(Money(25_430), current.items.single { it.entry.id == "var-food" }.actual)
        assertEquals(Money.ZERO, current.items.single { it.entry.id == "var-fuel" }.actual)
        assertEquals(Money(63_230 - 25_430), view.unassigned)
    }

    @Test
    fun incomeRowShowsImportedIncome() {
        val stored = lines("hapoalim_oct.xlsx")
        val view = planned(october, stored)
        assertEquals(actualIncome(stored, october, ImportMemory()), view.rows.single { it.kind == MonthRowKind.INCOME }.actual)
        assertEquals(Money(1_517_300), view.rows.single { it.kind == MonthRowKind.INCOME }.actual)
    }

    @Test
    fun otherMonthsLinesDoNotCount() {
        val view = planned(october, lines("max_nov.xlsx"))
        view.rows.forEach { assertNull(it.actual) }
    }
}
