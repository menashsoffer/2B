package com.menash.myself.feature.money.importer

import com.menash.myself.feature.money.domain.VEHICLE_ENERGY_CATEGORY_NAME
import com.menash.myself.feature.money.domain.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Paamonim רישומון plan sheets, read through the same reader as bank files.
 * Fixture: tools/import-fixtures/make_paamonim_fixture.py (real layout, fake numbers and names).
 */
class PaamonimPlanFixtureTest {
    private val workbook = readWorkbook(ImportFixtures.bytes("paamonim_plan.xls"))
    private val budgets = workbook.sheets.single { it.name == "רשימת תקציבים" }

    private fun text(row: Int, col: Int) = (budgets.cell(row, col) as? Cell.Text)?.value?.trim()
    private fun number(row: Int, col: Int) = (budgets.cell(row, col) as? Cell.Number)?.value ?: 0.0

    /** Expense lines sit in rows 3..47 (index 2..46), income in rows 50..60; an unused line's name is the number 0. */
    private val expenseRows = 2..46
    private val incomeRows = 49..59
    private val january = 2

    @Test fun bothPlanSheetsAreRead() {
        assertEquals(listOf("שיקוף", "רשימת תקציבים"), workbook.sheets.map { it.name })
    }

    @Test fun monthHeadersAreText() {
        assertEquals("ינואר", text(1, january))
        assertEquals("דצמבר", text(1, january + 11))
    }

    @Test fun groupLabelsMarkTheExpenseClasses() {
        assertEquals("הוצאות חודשיות קבועות", text(2, 0))
        assertEquals("הוצאות תקופתיות", text(13, 0))
        assertEquals("הוצאות שוטפות", text(35, 0))
        assertEquals("הכנסות", text(49, 0))
    }

    @Test fun everyNamedExpenseLineIsADefaultCategory() {
        val known = defaultCategories().map { it.name }.toSet()
        val names = expenseRows.mapNotNull { text(it, 1) }
        assertTrue(names.size >= 40)
        val unknown = names.filter { it !in known }
        // The file says "דלק"; the app's category is VEHICLE_ENERGY_CATEGORY_NAME.
        assertEquals(listOf("דלק"), unknown)
        assertTrue(VEHICLE_ENERGY_CATEGORY_NAME in known)
    }

    @Test fun unusedLinesHaveANumberInsteadOfAName() {
        val blanks = (expenseRows + incomeRows).filter { budgets.cell(it, 1) == Cell.Number(0.0) }
        assertTrue(blanks.isNotEmpty())
        blanks.forEach { assertEquals(0.0, number(it, january), 0.0) }
    }

    @Test fun totalsMatchTheLines() {
        val expenses = expenseRows.sumOf { number(it, january) }
        val income = incomeRows.sumOf { number(it, january) }
        assertEquals("סה\"כ הוצאות", text(47, 0))
        assertEquals(expenses, number(47, january), 0.005)
        assertEquals(income, number(60, january), 0.005)
        assertEquals(income - expenses, number(62, january), 0.005)
        assertEquals(income - expenses - number(63, january), number(64, january), 0.005)
    }

    @Test fun onlyTheFirstMonthHasABudget() {
        (january + 1..january + 11).forEach { month ->
            assertEquals(0.0, (expenseRows + incomeRows).sumOf { number(it, month) }, 0.0)
        }
    }
}
