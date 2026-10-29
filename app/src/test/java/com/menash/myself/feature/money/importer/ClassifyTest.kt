package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.MerchantMemory
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.planVsActual
import com.menash.myself.feature.money.importer.ImportFixtures.parse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/** Classification table and edge cases ג (the same money seen from two sources). */
class ClassifyTest {

    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private fun stored(vararg names: String) = names.flatMap { parse(it).rows }.map { StoredTransaction(it) }

    @Test
    fun bankLinesByKind() {
        val kinds = parse("hapoalim_oct.xlsx").rows.associate { it.description to classify(it) }
        assertEquals(RowKind.INCOME, kinds["משכורת מזהה ••••"])
        assertEquals(RowKind.CARD_TOTAL, kinds["מקס איט פיננסי"])
        assertEquals(RowKind.CASH_WITHDRAWAL, kinds["משיכה מבנקט סניף 612"])
        assertEquals(RowKind.TRANSFER, kinds["העברה ב-bit דנה לוי"])
        assertEquals(RowKind.STANDING_ORDER, kinds["הוראת קבע ועד בית"])
        assertEquals(RowKind.CARD_TOTAL, kinds["ישראכרט"])
        assertEquals(RowKind.TRANSFER, kinds["העברה לחשבון אחר וואן זירו"])
        assertEquals(RowKind.EXPENSE, kinds["ריבית"])
        assertEquals(RowKind.INCOME, kinds["ביטוח לאומי קצבת ילדים"])
    }

    @Test
    fun oneZeroLinesByKind() {
        assertEquals(
            listOf(RowKind.PORTFOLIO_DEPOSIT, RowKind.TRADING, RowKind.TRADING, RowKind.INCOME, RowKind.EXPENSE),
            parse("onezero_ils.xls").rows.map { classify(it) },
        )
    }

    @Test
    fun issuersRecognised() {
        assertEquals(Issuer.MAX, issuerOf("מקס איט פיננסי"))
        assertEquals(Issuer.CAL, issuerOf("כאל"))
        assertEquals(Issuer.CAL, issuerOf("כרטיסי אשראי לישראל בע\"מ"))
        assertEquals(Issuer.ISRACARD, issuerOf("ישראכרט"))
        assertNull(issuerOf("כאלה ואחרות"))
        assertNull(issuerOf("שופרסל"))
    }

    @Test
    fun cardTotalNotCountedWhenDetailsExist() {
        val rows = stored("hapoalim_oct.xlsx", "max_nov.xlsx")
        // The bank paid Max on 02/10 for October; the Max file is the November bill: October's bill still counts.
        assertTrue(toTransactions(rows).any { it.merchant == "מקס איט פיננסי" })
        val sameMonth = rows.map {
            if (it.row.description == "מקס איט פיננסי") it.copy(row = it.row.copy(date = it.row.date.withMonth(11), chargeDate = it.row.chargeDate!!.withMonth(11))) else it
        }
        assertTrue(toTransactions(sameMonth).none { it.merchant == "מקס איט פיננסי" })
        val rec = reconcileCards(sameMonth).single { it.issuer == Issuer.MAX }
        assertEquals(november, rec.month)
        assertEquals(Money(63_230), rec.bankTotal)
        assertEquals(Money(63_230), rec.purchasesTotal)
        assertEquals(Money.ZERO, rec.gap)
    }

    @Test
    fun cardTotalCountedUntilDetailsArrive() {
        val txs = toTransactions(stored("hapoalim_oct.xlsx"))
        val isracard = txs.single { it.merchant == "ישראכרט" }
        assertEquals(Money(80_000), isracard.amount)
        assertNull(isracard.categoryId)
        val rec = reconcileCards(stored("hapoalim_oct.xlsx")).single { it.issuer == Issuer.ISRACARD }
        assertNull(rec.purchasesTotal)
        assertNull(rec.gap)
    }

    @Test
    fun twoCardsSameIssuer() {
        val card1 = parse("max_nov.xlsx").rows
        val card2 = MaxParser.parse(ImportFixtures.maxSheet(ImportFixtures.maxRow("07-10-2026", "פיצה", "100"))).rows
            .map { it.copy(sourceId = "max-9999") }
        val bank = parse("hapoalim_oct.xlsx").rows.single { it.description == "מקס איט פיננסי" }
        val bills = listOf(bank, bank.copy(id = "tx-hapoalim-second", charge = Money(10_000)))
            .map { it.copy(date = it.date.withMonth(11), chargeDate = it.chargeDate!!.withMonth(11)) }
        val rec = reconcileCards((card1 + card2 + bills).map { StoredTransaction(it) }).single()
        assertEquals(Money(73_230), rec.bankTotal)
        assertEquals(Money(73_230), rec.purchasesTotal)
    }

    @Test
    fun transferToOneZeroIsPortfolioDeposit() {
        val bankSide = parse("hapoalim_oct.xlsx").rows.single { it.description.contains("וואן זירו") }
        val ozSide = parse("onezero_ils.xls").rows.first()
        assertEquals(RowKind.PORTFOLIO_DEPOSIT, classify(ozSide))
        val memory = ImportMemory(ownTransfers = setOf(ImportIds.merchantKey(bankSide.description)))
        assertEquals(RowKind.OWN_TRANSFER, classify(bankSide, memory))
        val txs = toTransactions(stored("hapoalim_oct.xlsx", "onezero_ils.xls"), memory)
        assertTrue(txs.none { it.merchant.contains("וואן זירו") || it.merchant == "העברה מחשבון" })
        assertEquals(Money(1_517_425), actualIncome(stored("hapoalim_oct.xlsx", "onezero_ils.xls"), october, memory))
    }

    @Test
    fun transferCountsUntilMarked() {
        val rows = stored("hapoalim_oct.xlsx")
        assertTrue(toTransactions(rows).any { it.merchant == "העברה ב-bit דנה לוי" })
        val memory = ImportMemory(ownTransfers = setOf(ImportIds.merchantKey("העברה ב-bit דנה לוי")))
        assertTrue(toTransactions(rows, memory).none { it.merchant == "העברה ב-bit דנה לוי" })
    }

    @Test
    fun tradingNeverInTheBudget() {
        val txs = toTransactions(stored("onezero_ils.xls", "onezero_usd.xls"))
        assertEquals(listOf("דמי מנוי"), txs.map { it.merchant })
    }

    @Test
    fun merchantMemoryGivesTheCategory_userChoiceWins() {
        val memory = ImportMemory(MerchantMemory(mapOf(ImportIds.merchantKey("שופרסל דיל 123") to "current-1")))
        val rows = stored("max_nov.xlsx")
        assertEquals("current-1", toTransactions(rows, memory).single { it.merchant == "שופרסל דיל 123" }.categoryId)
        val chosen = rows.map { if (it.row.description == "שופרסל דיל 123") it.copy(categoryId = "current-2") else it }
        assertEquals("current-2", toTransactions(chosen, memory).single { it.merchant == "שופרסל דיל 123" }.categoryId)
    }

    @Test
    fun planVsActualGetsTheImportedLines() {
        val memory = ImportMemory(MerchantMemory(mapOf(ImportIds.merchantKey("שופרסל דיל 123") to "current-1")))
        val result = planVsActual(november, MoneyItems(), toTransactions(stored("max_nov.xlsx"), memory), null)
        assertEquals(Money(25_430), result.categories.single { it.categoryId == "current-1" }.actual)
        assertEquals(Money(63_230 - 25_430), result.categories.single { it.categoryId == null }.actual)
    }
}
