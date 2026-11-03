package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.MerchantMemory
import com.menash.myself.feature.money.domain.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The review the import screen shows, and what confirming it writes and learns. */
class ImportSessionTest {

    private val categories = defaultCategories()
    private fun file(name: String) = ImportFile(name, ImportFixtures.bytes(name))

    @Test
    fun summaryPerFile_refusedFilesStayWithTheirReason() {
        val review = reviewImport(listOf(file("max_nov.xlsx"), file("encrypted.xls"), file("hapoalim_broken.xlsx")), emptyMap(), ImportMemory(), categories)
        val (max, locked, bank) = review.files
        assertEquals(Source.MAX, max.source)
        assertEquals(7, max.rows)
        assertEquals(com.menash.myself.feature.money.domain.ImportCheck.Ok, max.total)
        assertEquals(FileProblem.ENCRYPTED, locked.problem)
        assertEquals(10, bank.balanceBreakRow)
        assertEquals(listOf(9), bank.skippedRows)
        assertEquals(7 + 8, review.plan.newCount)
    }

    @Test
    fun onlyNewMerchantsOfBudgetKindsAreAsked() {
        val review = reviewImport(listOf(file("hapoalim_oct.xlsx"), file("max_nov.xlsx")), emptyMap(), ImportMemory(), categories)
        val names = review.merchants.map { it.name }.toSet()
        // Salary (income), the Max bill, Isracard bill and cash are never asked.
        assertTrue("משכורת מזהה ••••" !in names && "מקס איט פיננסי" !in names && "ישראכרט" !in names)
        assertTrue("משיכה מבנקט סניף 612" !in names)
        assertTrue("שופרסל דיל 123" in names && "הוראת קבע ועד בית" in names)
        val coffee = review.merchants.single { it.name == "קפה גרג" }
        assertEquals(2, coffee.count)
        assertEquals(Money(2_400), coffee.total)
        assertTrue(review.merchants.single { it.name == "העברה ב-bit דנה לוי" }.transfer)
    }

    @Test
    fun rememberedMerchantsAppearWithMemoryBeforeTheHint() {
        val memory = ImportMemory(
            MerchantMemory(mapOf(ImportIds.merchantKey("שופרסל דיל 123") to "current-9")),
            ownTransfers = setOf(ImportIds.merchantKey("העברה ב-bit דנה לוי")),
        )
        val merchants = reviewImport(listOf(file("hapoalim_oct.xlsx"), file("max_nov.xlsx")), emptyMap(), memory, categories).merchants
        assertEquals("current-9", merchants.single { it.name == "שופרסל דיל 123" }.suggested)
        assertTrue(merchants.none { it.name == "העברה ב-bit דנה לוי" })
    }

    @Test
    fun hapoalimFixtureNeedsFourDecisionsThenNone() {
        val first = reviewImport(listOf(file("hapoalim_oct.xlsx")), emptyMap(), ImportMemory(), categories)
        assertEquals(4, first.merchants.count { it.suggested == null })
        val again = reviewImport(listOf(file("hapoalim_oct.xlsx")), first.plan.toWrite.associateBy { it.id }, ImportMemory(), categories)
        assertEquals(0, again.merchants.size)
    }

    @Test
    fun reimportAsksNothing() {
        val first = reviewImport(listOf(file("max_nov.xlsx")), emptyMap(), ImportMemory(), categories)
        val stored = first.plan.toWrite.associateBy { it.id }
        val again = reviewImport(listOf(file("max_nov.xlsx")), stored, ImportMemory(), categories)
        assertTrue(again.merchants.isEmpty())
        assertTrue(again.nothingToSave)
    }

    @Test
    fun maxCategoryHintSuggestsACategory() {
        assertEquals("current-1", categoryForHint("מזון וצריכה", categories))
        assertNull(categoryForHint("אופנה", categories))
        // An electronics shop is not the electricity bill, and a fixed bill is never suggested.
        assertNull(categoryForHint("חשמל ומחשבים", categories))
        assertEquals("current-3", categoryForHint("טעינת רכב חשמלי", categories))
        assertEquals(categories.single { it.name == "דלק / חשמל" }.id, categoryForHint("דלק, חשמל וגז", categories))
        val review = reviewImport(listOf(file("max_nov.xlsx")), emptyMap(), ImportMemory(), categories)
        assertEquals("current-1", review.merchants.single { it.name == "שופרסל דיל 123" }.suggested)
    }

    @Test
    fun decisionsSetCategoriesAndTeachTheMemory() {
        val review = reviewImport(listOf(file("hapoalim_oct.xlsx"), file("max_nov.xlsx")), emptyMap(), ImportMemory(), categories)
        val coffee = review.merchants.single { it.name == "קפה גרג" }.key
        val bit = review.merchants.single { it.name == "העברה ב-bit דנה לוי" }.key
        val zara = review.merchants.single { it.name == "זארה" }.key
        val (plan, updates) = applyDecisions(review, mapOf(
            coffee to MerchantDecision.Categorise("current-9"),
            bit to MerchantDecision.OwnTransfer,
            zara to MerchantDecision.Later,
        ))
        assertEquals(2, plan.toWrite.count { it.categoryId == "current-9" })
        assertTrue(plan.toWrite.filter { it.row.description == "זארה" }.all { it.categoryId == null })
        assertEquals(setOf(MemoryUpdate(coffee, "current-9", false), MemoryUpdate(bit, null, true)), updates.toSet())
        assertEquals(review.plan.toWrite.map { it.id }, plan.toWrite.map { it.id })
    }
}
