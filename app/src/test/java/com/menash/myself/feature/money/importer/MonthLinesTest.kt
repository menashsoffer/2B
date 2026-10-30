package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.MerchantMemory
import com.menash.myself.feature.money.domain.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/** The transactions screen's list: the same counting as the month's "בפועל", plus pending and income lines. */
class MonthLinesTest {

    private val categories = defaultCategories()
    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)

    private fun stored(vararg names: String) = names.flatMap { ImportFixtures.parse(it).rows }.map { StoredTransaction(it) }

    @Test
    fun onlyTheMonthsLines_newestFirst() {
        val lines = monthLines(stored("max_nov.xlsx", "hapoalim_oct.xlsx"), november, ImportMemory(), categories)
        assertTrue(lines.lines.all { it.row.billingMonth == november })
        assertEquals(lines.lines.sortedByDescending { it.row.date }.map { it.row.date }, lines.lines.map { it.row.date })
        assertEquals(7, lines.lines.count { it.row.source == Source.MAX })
    }

    @Test
    fun uncategorisedSpendingIsUnassigned_untilTheUserOrTheMemoryKnowsIt() {
        val rows = stored("max_nov.xlsx")
        val shufersal = ImportIds.merchantKey("שופרסל דיל 123")
        val before = monthLines(rows, november, ImportMemory(), categories)
        assertEquals(before.lines.size, before.unassignedCount)

        val learnt = monthLines(rows, november, ImportMemory(MerchantMemory(mapOf(shufersal to "current-1"))), categories)
        val line = learnt.lines.first { it.row.description == "שופרסל דיל 123" }
        assertEquals("current-1", line.categoryId)
        assertFalse(line.unassigned)

        val chosen = rows.map { if (it.row.description == "קפה גרג") it.copy(categoryId = "current-9") else it }
        val coffee = monthLines(chosen, november, ImportMemory(), categories).lines.filter { it.row.description == "קפה גרג" }
        assertTrue(coffee.all { it.categoryId == "current-9" && !it.unassigned })
        assertEquals(before.unassignedCount - 2, monthLines(chosen, november, ImportMemory(), categories).unassignedCount)
    }

    @Test
    fun unknownCategoryIdCountsAsUnassigned() {
        val rows = stored("max_nov.xlsx").map { it.copy(categoryId = "deleted-category") }
        assertTrue(monthLines(rows, november, ImportMemory(), categories).lines.all { it.unassigned })
    }

    @Test
    fun pendingLineIsShownRemovableAndNeverCounted() {
        val lines = monthLines(stored("cal_nov.xlsm"), october, ImportMemory(), categories)
        val pending = lines.filtered(LineFilter.PENDING).single()
        assertEquals("פז דלק", pending.row.description)
        assertTrue(pending.removable)
        assertFalse(pending.counted || pending.unassigned || pending.canCategorise)
        assertEquals(Money.ZERO, lines.spent)
    }

    @Test
    fun waitingForTheRateIsShownButNotCounted() {
        val netflix = monthLines(stored("cal_nov.xlsm"), november, ImportMemory(), categories).lines.single { it.row.description == "NETFLIX.COM" }
        assertFalse(netflix.counted)
        assertFalse(netflix.removable)
    }

    @Test
    fun incomeMatchesTheMonthsIncome() {
        val rows = stored("hapoalim_oct.xlsx")
        val lines = monthLines(rows, october, ImportMemory(), categories)
        assertEquals(actualIncome(rows, october, ImportMemory()), lines.income)
        assertTrue(lines.lines.filter { it.income }.none { it.counted || it.unassigned || it.canCategorise })
    }

    @Test
    fun anIncomeLine_showsTheSourceItIsLinkedTo() {
        val rows = stored("hapoalim_oct.xlsx")
        val allowance = rows.single { it.row.description.contains("קצבת ילדים") }
        val linked = rows.map { if (it.id == allowance.id) it.copy(categoryId = "inc-allowance") else it }
        val lines = monthLines(linked, october, ImportMemory(), categories, setOf("inc-allowance"))

        assertEquals("inc-allowance", lines.lines.single { it.id == allowance.id }.incomeId)
        assertTrue(lines.lines.filter { it.id != allowance.id }.all { it.incomeId == null })
        assertEquals(lines.lines.filter { it.counted }, monthLines(rows, october, ImportMemory(), categories).lines.filter { it.counted })
    }

    @Test
    fun spentMatchesTheCountedTransactions() {
        val rows = stored("hapoalim_oct.xlsx", "max_nov.xlsx", "cal_nov.xlsm")
        for (month in listOf(october, november)) {
            val expected = toTransactions(rows, ImportMemory()).filter { it.billingMonth == month }.sumOf { it.amount.agorot }
            assertEquals(Money(expected), monthLines(rows, month, ImportMemory(), categories).spent)
        }
    }

    @Test
    fun choiceUsesTheLinesCategoryElseTheHint() {
        val lines = monthLines(stored("max_nov.xlsx"), november, ImportMemory(), categories).lines
        val shufersal = lines.first { it.row.description == "שופרסל דיל 123" }
        assertEquals("current-1", choiceFor(shufersal, categories).suggested)
        assertEquals("current-9", choiceFor(shufersal.copy(categoryId = "current-9"), categories).suggested)
        assertNull(choiceFor(lines.first { it.row.description == "מחסני חשמל" }, categories).suggested)
    }

    @Test
    fun transfersCanBeOwn_cardBillsAndCashAreNotLearnt() {
        val lines = monthLines(stored("hapoalim_oct.xlsx"), october, ImportMemory(), categories).lines
        assertTrue(choiceFor(lines.first { it.kind == RowKind.TRANSFER && it.counted }, categories).transfer)
        lines.filter { it.kind == RowKind.CARD_TOTAL || it.kind == RowKind.CASH_WITHDRAWAL }.also { assertTrue(it.isNotEmpty()) }
            .forEach { assertFalse(teachesMemory(it)) }
        assertTrue(lines.filter { it.kind == RowKind.EXPENSE }.all { teachesMemory(it) })
    }
}
