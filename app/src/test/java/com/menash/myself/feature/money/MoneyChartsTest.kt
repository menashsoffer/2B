package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.CategoryActual
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Data behind the money charts: classes and top categories in a month, half a year of flows, net worth. */
class MoneyChartsTest {

    private val november = YearMonth.of(2026, 11)
    private val categories = defaultCategories()
    private fun ils(shekels: Long) = Money(shekels * 100)

    private fun closed(month: YearMonth, income: Long, expenses: Long, balances: Map<String, Money> = emptyMap()) = ClosedMonth(
        month = month,
        snapshot = MonthSnapshot(month, ils(income), ils(expenses)),
        byCategory = emptyMap(),
        periodic = emptyMap(),
        balances = balances,
        liabilities = emptyMap(),
        closedOn = month.plusMonths(1).atDay(2),
    )

    private fun line(id: String, date: LocalDate, shekels: Long) = StoredTransaction(
        StatementRow(id = id, source = Source.MAX, sourceId = "card", date = date, chargeDate = null, description = "שופרסל", charge = ils(shekels)),
    )

    @Test
    fun topCategories_largestFirst_cappedWithTheRestCounted() {
        val actuals = (1..7).map { CategoryActual("current-$it", if (it == 1) ils(500) else Money.ZERO, ils(it * 100L)) } +
            CategoryActual(null, Money.ZERO, ils(9_999)) + CategoryActual("deleted", Money.ZERO, ils(8_888))
        val top = topCategories(actuals, categories)
        assertEquals(listOf("current-7", "current-6", "current-5", "current-4", "current-3"), top.shown.map { it.categoryId })
        assertEquals(2, top.more)
        assertNull(top.shown.first().plan)
    }

    @Test
    fun topCategories_keepsAPlanOnlyWhenPositive() {
        val top = topCategories(listOf(CategoryActual("current-1", ils(500), ils(700))), categories)
        assertEquals(ils(500), top.shown.single().plan)
        assertEquals("מזון", top.shown.single().name)
    }

    @Test
    fun flows_closedSoFarAndMissing() {
        val stored = listOf(line("a", LocalDate.of(2026, 11, 2), 300), line("b", LocalDate.of(2026, 9, 5), 120))
        val flows = monthFlows(listOf(closed(YearMonth.of(2026, 8), 20_000, 15_000)), stored, ImportMemory(), categories, november)
        assertEquals((5 downTo 0).map { november.minusMonths(it.toLong()) }, flows.map { it.month })
        assertEquals(FlowState.MISSING, flows[0].state)
        assertEquals(FlowState.CLOSED, flows[2].state)
        assertEquals(ils(15_000), flows[2].expenses)
        assertEquals(FlowState.SO_FAR, flows[3].state)
        assertEquals(ils(120), flows[3].expenses)
        assertEquals(FlowState.MISSING, flows[4].state)
        assertEquals(ils(300), flows[5].expenses)
    }

    @Test
    fun worth_skipsUnknownBalances_andEndsWithToday() {
        val history = worthHistory(
            listOf(
                closed(YearMonth.of(2026, 8), 1, 1, mapOf("a" to ils(10_000))),
                closed(YearMonth.of(2026, 9), 1, 1),
                closed(YearMonth.of(2026, 10), 1, 1, mapOf("a" to ils(12_000), "b" to ils(1_000))),
            ),
            now = ils(14_000),
            current = november,
        )
        assertEquals(listOf(ils(10_000), null, ils(13_000), ils(14_000)), history.map { it.worth })
        assertEquals(november, history.last().month)
    }

    @Test
    fun worth_isCappedToTheLatestPoints() {
        val closes = (1..20).map { closed(november.minusMonths(it.toLong()), 1, 1, mapOf("a" to ils(it.toLong()))) }
        val history = worthHistory(closes, ils(1), november, maxPoints = 12)
        assertEquals(12, history.size)
        assertTrue(history.zipWithNext().all { (a, b) -> a.month < b.month })
    }
}
