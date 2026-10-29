package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.ImportDocuments
import com.menash.myself.feature.money.domain.ImportCheck
import com.menash.myself.feature.money.domain.InstallmentInfo
import com.menash.myself.feature.money.importer.ImportFixtures.maxRow
import com.menash.myself.feature.money.importer.ImportFixtures.maxSheet
import com.menash.myself.feature.money.importer.ImportFixtures.parse
import com.menash.myself.feature.money.importer.ImportFixtures.text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** The four sources on their fixtures, plus edge cases ה (totals), ו and ז. */
class StatementParsersTest {

    @Test
    fun hapoalim_rowsAmountsAndAccountLast4() {
        val s = parse("hapoalim_oct.xlsx")
        assertEquals(9, s.rows.size)
        assertTrue(s.rows.all { it.sourceId == "hapoalim-8901" })
        assertEquals(Money(-1_500_000), s.rows[0].charge)
        assertEquals(Money(63_230), s.rows[1].charge)
        assertEquals("הוראת קבע ועד בית", s.rows[4].description)
        assertTrue(s.warnings.isEmpty())
        assertNull(totalCheck(s))
    }

    @Test
    fun oneZero_reversedHebrewFixedAndSignsRead() {
        val s = parse("onezero_ils.xls")
        assertEquals(listOf("העברה מחשבון", "VOO קניה", "המרה לדולר", "ריבית רבעונית", "דמי מנוי"), s.rows.map { it.description })
        assertEquals(listOf(-200_000L, 150_000L, 30_000L, -125L, 990L), s.rows.map { it.charge!!.agorot })
        assertTrue(s.rows.all { it.sourceId == "onezero-ILS" && it.status == RowStatus.COUNTED })
        assertTrue(s.warnings.isEmpty())
    }

    @Test
    fun oneZeroUsdFile() {
        val s = parse("onezero_usd.xls")
        assertTrue(s.rows.all { it.sourceId == "onezero-USD" && it.charge == null && it.status == RowStatus.WAITING_FX })
        assertEquals(listOf(Money(10_000), Money(100)), s.rows.map { it.originalAmount })
        assertEquals(listOf(RowKind.TRADING, RowKind.EXPENSE), s.rows.map { classify(it) })
    }

    @Test
    fun max_rowsCategoryHintFxAndTotal() {
        val s = parse("max_nov.xlsx")
        assertEquals(7, s.rows.size)
        assertTrue(s.rows.all { it.sourceId == "max-1234" && it.chargeDate == LocalDate.of(2026, 11, 2) })
        assertEquals("מזון וצריכה", s.rows[0].hint)
        val amazon = s.rows[3]
        assertEquals(Money(8_410), amazon.charge)
        assertEquals(Money(2_250), amazon.originalAmount)
        assertEquals("USD", amazon.originalCurrency)
        assertEquals(Money(63_230), s.statementTotal)
        assertEquals(ImportCheck.Ok, totalCheck(s))
    }

    @Test
    fun cal_pendingFxStandingOrderAndCardDigits() {
        val s = parse("cal_nov.xlsm")
        assertEquals(6, s.rows.size)
        assertTrue(s.rows.all { it.sourceId == "cal-5678" })
        assertEquals(RowStatus.PENDING, s.rows[1].status)
        assertNull(s.rows[1].chargeDate)
        val netflix = s.rows[2]
        assertEquals(RowStatus.WAITING_FX, netflix.status)
        assertNull(netflix.charge)
        assertEquals(Money(1_549), netflix.originalAmount)
        assertEquals("USD", netflix.originalCurrency)
        assertEquals(RowKind.STANDING_ORDER, classify(s.rows[3]))
    }

    @Test
    fun footerRowsAreNotTransactions() {
        listOf("max_nov.xlsx", "cal_nov.xlsm").forEach { name ->
            val s = parse(name)
            assertFalse(name, s.rows.any { headerKey(it.description).startsWith("סך הכל") || headerKey(it.description).startsWith("סהכ") })
            assertFalse(name, s.rows.any { it.description.contains("המידע נכון") })
        }
    }

    @Test
    fun totalMismatchShown() {
        assertEquals(ImportCheck.Mismatch(Money(63_230 - 70_000)), totalCheck(parse("max_mismatch.xlsx")))
    }

    @Test
    fun calTotalExcludesWaiting() {
        val s = parse("cal_nov.xlsm")
        assertEquals(Money(74_250), s.statementTotal)
        assertEquals(ImportCheck.Ok, totalCheck(s))
        assertEquals(4, s.checkedRows.size)
    }

    @Test
    fun balanceChainBreakWarns() {
        val s = parse("hapoalim_broken.xlsx")
        assertTrue(s.warnings.contains(ImportWarning.BalanceBreak(10)))
        assertTrue(parse("hapoalim_oct.xlsx").warnings.none { it is ImportWarning.BalanceBreak })
        assertTrue(parse("onezero_ils.xls").warnings.none { it is ImportWarning.BalanceBreak })
    }

    @Test
    fun balanceChainHoldsNewestFirst() {
        val rows = listOf(Triple(6, Money(-50), Money(950)), Triple(7, Money(-100), Money(1_000)), Triple(8, Money(1_100), Money(1_100)))
        assertNull(balanceBreak(rows))
    }

    @Test
    fun badRowReportedNotDropped() {
        val s = parse("hapoalim_broken.xlsx")
        assertTrue(s.warnings.contains(ImportWarning.SkippedRow(9, SkipReason.BAD_DATE)))
        val bad = maxSheet(maxRow("05-10-2026", "שופרסל", "שלושים"), maxRow("06-10-2026", "קפה", "12"))
        val parsed = MaxParser.parse(bad)
        assertEquals(1, parsed.rows.size)
        assertEquals(listOf(ImportWarning.SkippedRow(5, SkipReason.BAD_AMOUNT)), parsed.warnings)
    }

    @Test
    fun columnsByName() {
        val base = maxSheet(maxRow("05-10-2026", "שופרסל", "254.30"))
        val order = listOf(9, 5, 1, 0, 2, 3, 4, 6, 7, 8, 10)
        val shuffled = Sheet(base.name, base.rows.mapIndexed { i, row -> if (i < 3) row else order.map { row[it] } + text("עמודה חדשה") })
        val a = MaxParser.parse(base).rows.single()
        val b = MaxParser.parse(shuffled).rows.single()
        assertEquals(a, b)
    }

    @Test
    fun refundIsNegative() {
        assertEquals(Money(-15_000), parse("max_nov.xlsx").rows[6].charge)
        assertEquals(Money(-6_000), parse("cal_nov.xlsm").rows[5].charge)
    }

    @Test
    fun installmentPaymentsAreSeparate() {
        val october = MaxParser.parse(maxSheet(maxRow("12-09-2026", "מחסני חשמל", "400", "תשלומים", "02-10-2026", "1200", "תשלום 1 מתוך 3")))
        val november = MaxParser.parse(maxSheet(maxRow("12-09-2026", "מחסני חשמל", "400", "תשלומים", "02-11-2026", "1200", "תשלום 2 מתוך 3")))
        assertEquals(InstallmentInfo(1, 3), october.rows.single().installment)
        assertNotEquals(october.rows.single().id, november.rows.single().id)
    }

    @Test
    fun installmentWithoutNumber() {
        val october = MaxParser.parse(maxSheet(maxRow("12-09-2026", "מחסני חשמל", "400", "תשלומים", "02-10-2026", "1200")))
        val november = MaxParser.parse(maxSheet(maxRow("12-09-2026", "מחסני חשמל", "400", "תשלומים", "02-11-2026", "1200")))
        assertNull(october.rows.single().installment)
        assertNotEquals(october.rows.single().id, november.rows.single().id)
    }

    @Test
    fun countedInBillingMonth() {
        val row = MaxParser.parse(maxSheet(maxRow("31-10-2026", "שופרסל", "100", chargeDate = "02-11-2026"))).rows.single()
        assertEquals(YearMonth.of(2026, 11), row.billingMonth)
        assertEquals(YearMonth.of(2026, 11), toTransactions(listOf(StoredTransaction(row))).single().billingMonth)
    }

    @Test
    fun headerPersonalDataNotStored() {
        val stored = listOf("hapoalim_oct.xlsx", "max_nov.xlsx", "cal_nov.xlsm", "onezero_ils.xls")
            .flatMap { parse(it).rows }.map { ImportDocuments.encode(StoredTransaction(it)).toString() }
        listOf("רות כהן", "ישראל ישראלי", "012345678", "678901", "987654321").forEach { secret ->
            assertTrue(secret, stored.none { it.contains(secret) })
        }
    }

    @Test
    fun longDigitRunsMaskedInStoredRows() {
        val salary = parse("hapoalim_oct.xlsx").rows[0]
        assertEquals("משכורת מזהה ••••", salary.description)
        assertEquals("מזהה ••••", salary.details)
    }
}
