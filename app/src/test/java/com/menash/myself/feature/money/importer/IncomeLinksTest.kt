package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.ActualIncome
import com.menash.myself.feature.money.domain.MerchantMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomeLinksTest {
    private val rows = ImportFixtures.parse("hapoalim_oct.xlsx").rows.map { StoredTransaction(it) }
    private val incomeLines = rows.filter { isIncomeLine(it, ImportMemory()) }
    private val allowance = incomeLines.single { it.row.description.contains("קצבת ילדים") }
    private val month = allowance.row.billingMonth

    @Test
    fun nothingLinked_everyIncomeLineIsWaiting() {
        val imported = importedIncome(rows, ImportMemory(), setOf("inc-allowance"))

        assertTrue(imported.actual.isEmpty())
        assertEquals(incomeLines.groupingBy { it.row.billingMonth }.eachCount(), imported.unlinked)
    }

    @Test
    fun aLineLinkedByHand_countsForItsSource() {
        val linked = rows.map { if (it.id == allowance.id) it.copy(categoryId = "inc-allowance") else it }
        val imported = importedIncome(linked, ImportMemory(), setOf("inc-allowance"))

        assertEquals(listOf(ActualIncome("inc-allowance", month, Money(-allowance.row.charge!!.agorot))), imported.actual)
        assertEquals(incomeLines.size - 1, imported.unlinked.values.sum())
    }

    @Test
    fun theRememberedPayer_linksLaterLines_butOnlyToAKnownSource() {
        val memory = ImportMemory(MerchantMemory(mapOf(ImportIds.merchantKey(allowance.row.description) to "inc-allowance")))

        assertEquals("inc-allowance", incomeSourceOf(allowance, memory, setOf("inc-allowance")))
        assertEquals(null, incomeSourceOf(allowance, memory, setOf("inc-other")))
    }

    @Test
    fun theSameLinesAsTheMonthsIncome() {
        val total = Money(-incomeLines.filter { it.row.billingMonth == month }.sumOf { it.row.charge!!.agorot })
        assertEquals(actualIncome(rows, month), total)
    }
}
