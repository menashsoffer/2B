package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.InstallmentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Edge cases ו (amounts and dates), ז (privacy) and header matching. */
class ImportTextTest {

    private fun money(text: String) = (amountOf(Cell.Text(text)) as AmountCell.Value).money

    @Test
    fun amountTextForms() {
        assertEquals(Money(125_050), money("₪1,250.50"))
        assertEquals(Money(-10_000), money("100-"))
        assertEquals(Money(-10_000), money("-100"))
        assertEquals(Money(-10_000), money("(100)"))
        assertEquals(Money(123_400), money("1 234 ש\"ח"))
        assertEquals(Money(1_549), money("15.49 $"))
        assertEquals(AmountCell.Blank, amountOf(Cell.Text("  ")))
        assertEquals(AmountCell.Blank, amountOf(null))
        assertEquals(AmountCell.Bad, amountOf(Cell.Text("שלושים")))
    }

    @Test
    fun amountNumbersAreExactToTheAgora() {
        assertEquals(AmountCell.Value(Money(63_230)), amountOf(Cell.Number(632.3)))
        assertEquals(AmountCell.Value(Money(10)), amountOf(Cell.Number(0.1)))
        assertEquals(AmountCell.Value(Money(3_334)), amountOf(Cell.Number(33.335)))
    }

    @Test
    fun amountSignsAndRoundingAreHandledAtBoundaries() {
        assertEquals(Money(0), money("0"))
        assertEquals(Money(10000), money("(-100)"))
        assertEquals(Money(10000), money("-100-"))
        assertEquals(Money(-1), money("-0.005"))
        assertEquals(Money(1), money("0.005"))
        assertEquals(AmountCell.Blank, amountOf(Cell.Text("₪")))
    }

    @Test
    fun dateAndInstallmentLimitsRejectInvalidValues() {
        assertNull(dateOf(null))
        assertNull(dateOf(Cell.Number(0.0)))
        assertNull(dateOf(Cell.Number(2_958_466.0)))
        assertNull(installmentOf("תשלום 0 מתוך 3"))
        assertNull(installmentOf("תשלום 1 מתוך 0"))
        assertNull(installmentOf())
        assertNull(Cell.Text("   ").text())
        assertEquals("12.5", Cell.Number(12.5).text())
        assertEquals("שלום", fixReversedHebrew("\u202eםולש\u202c"))
    }

    @Test
    fun dateForms() {
        val day = LocalDate.of(2026, 10, 5)
        assertEquals(day, dateOf(Cell.Number(46300.0)))
        assertEquals(day, dateOf(Cell.Number(46300.6458)))
        assertEquals(day, dateOf(Cell.Text("05-10-2026")))
        assertEquals(day, dateOf(Cell.Text("05/10/2026")))
        assertEquals(day, dateOf(Cell.Text("5.10.26")))
        assertEquals(day, dateOf(Cell.Text("2026-10-05 09:15:00")))
        assertNull(dateOf(Cell.Text("לא תאריך")))
        assertNull(dateOf(Cell.Number(-3.0)))
    }

    @Test
    fun headersNormalised() {
        assertEquals(headerKey("סכום בש\"ח"), headerKey("סכום\nבש״ח"))
        assertEquals("תאריך עסקה", headerKey("‏תאריך\n  עסקה "))
        assertEquals(headerKey("המרת מט\"ח"), headerKey("המרת מט״ח"))
    }

    @Test
    fun bidiFixReversesOneZeroHebrew() {
        assertEquals("ריבית רבעונית", fixReversedHebrew("‭תינועבר תיביר‬"))
        assertEquals("VOO קניה", fixReversedHebrew("VOO ‭הינק‬"))
        // Text without an override is already in reading order.
        assertEquals("ריבית רבעונית", fixReversedHebrew("ריבית רבעונית"))
    }

    @Test
    fun longDigitRunsMasked() {
        assertEquals("מזהה ••••", maskDigits("מזהה 012345678"))
        assertEquals("חשבון 12-•••• סניף 612", maskDigits("חשבון 12-345678 סניף 612"))
        assertEquals("שופרסל דיל 123", maskDigits("שופרסל דיל 123"))
    }

    @Test
    fun installmentTextIsRead() {
        assertEquals(InstallmentInfo(2, 3), installmentOf(null, "תשלום 2 מתוך 3"))
        assertEquals(InstallmentInfo(10, 12), installmentOf("תשלום 10 מתוך 12", null))
        assertNull(installmentOf("תשלומים", "תשלום 4 מתוך 3"))
    }

    @Test
    fun idsDependOnContentOnly() {
        val first = ImportIds.assign("max", listOf(listOf("a", "1"), listOf("a", "1"), listOf("b", "2")))
        val again = ImportIds.assign("max", listOf(listOf("b", "2"), listOf("a", "1")))
        assertEquals(3, first.toSet().size)
        assertEquals(first[2], again[0])
        assertEquals(first[0], again[1])
        first.forEach { assertEquals(true, it.matches(Regex("^[a-zA-Z0-9_\\-.]+$"))) }
    }
}
