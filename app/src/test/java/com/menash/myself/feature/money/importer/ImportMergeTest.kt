package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.importer.ImportFixtures.maxRow
import com.menash.myself.feature.money.importer.ImportFixtures.maxSheet
import com.menash.myself.feature.money.importer.ImportFixtures.parse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Edge cases א (same or overlapping files) and ב (lines that change between imports). */
class ImportMergeTest {

    /** Stores a plan the way the repository does: upsert by id, then deletes. */
    private fun store(existing: Map<String, StoredTransaction>, plan: ImportPlan): Map<String, StoredTransaction> =
        (existing + plan.toWrite.associateBy { it.id }) - plan.toDelete.toSet()

    private fun importAll(vararg names: String): Map<String, StoredTransaction> =
        names.fold(emptyMap()) { db, name -> store(db, planImport(db, listOf(parse(name)))) }

    @Test
    fun sameFileTwice_addsNothing() {
        val db = importAll("hapoalim_oct.xlsx")
        val again = planImport(db, listOf(parse("hapoalim_oct.xlsx")))
        assertEquals(0, again.newCount)
        assertEquals(0, again.updatedCount)
        assertEquals(9, again.alreadyImported)
        assertTrue(again.toWrite.isEmpty() && again.toDelete.isEmpty())
        assertEquals(db, store(db, again))
    }

    @Test
    fun overlappingFiles_addOnlyTheNewDays() {
        val db = importAll("hapoalim_oct.xlsx")
        val plan = planImport(db, listOf(parse("hapoalim_overlap.xlsx")))
        assertEquals(2, plan.newCount)
        assertEquals(4, plan.alreadyImported)
        assertEquals(setOf("שופרסל", "משכורת מזהה ••••"), plan.toWrite.map { it.row.description }.toSet())
        assertEquals(11, store(db, plan).size)
    }

    @Test
    fun duplicatesAcrossFilesInOneBatch() {
        val plan = planImport(emptyMap(), listOf(parse("hapoalim_oct.xlsx"), parse("hapoalim_overlap.xlsx")))
        assertEquals(11, plan.newCount)
        assertEquals(4, plan.duplicatesInBatch)
        assertEquals(11, plan.toWrite.map { it.id }.toSet().size)
    }

    @Test
    fun twoIdenticalPurchases_keptAsTwo() {
        val coffees = parse("max_nov.xlsx").rows.filter { it.description == "קפה גרג" }
        assertEquals(2, coffees.map { it.id }.toSet().size)
        val db = importAll("max_nov.xlsx")
        assertEquals(0, planImport(db, listOf(parse("max_nov.xlsx"))).newCount)
        assertEquals(2, db.values.count { it.row.description == "קפה גרג" })
    }

    @Test
    fun partialOverlap_neverDuplicates() {
        val two = MaxParser.parse(maxSheet(maxRow("06-10-2026", "קפה גרג", "12"), maxRow("06-10-2026", "קפה גרג", "12")))
        val one = MaxParser.parse(maxSheet(maxRow("06-10-2026", "קפה גרג", "12")))
        val db = store(emptyMap(), planImport(emptyMap(), listOf(two)))
        assertEquals(0, planImport(db, listOf(one)).newCount)
        // The other way round: one first, then the file with both adds exactly the second.
        val db1 = store(emptyMap(), planImport(emptyMap(), listOf(one)))
        assertEquals(1, planImport(db1, listOf(two)).newCount)
    }

    @Test
    fun maxSameCardTwoExports() {
        val monthFile = parse("max_nov.xlsx")
        val allFile = MaxParser.parse(maxSheet(maxRow("05-10-2026", "שופרסל דיל 123", "254.30"), maxRow("20-10-2026", "חדש", "50")))
        val db = store(emptyMap(), planImport(emptyMap(), listOf(monthFile)))
        val plan = planImport(db, listOf(allFile))
        assertEquals(1, plan.newCount)
        // The second export prints a different category hint: the line is refreshed, not added.
        assertEquals(1, plan.alreadyImported + plan.updatedCount)
        assertEquals(8, store(db, plan).size)
    }

    @Test
    fun pendingBecomesCharged_sameRow() {
        val db = importAll("cal_nov.xlsm")
        val plan = planImport(db, listOf(parse("cal_next.xlsm")))
        val after = store(db, plan)
        val fuel = after.values.filter { it.row.description == "פז דלק" }
        assertEquals(1, fuel.size)
        assertEquals(RowStatus.COUNTED, fuel.single().row.status)
        assertEquals(Money(21_240), fuel.single().row.charge)
    }

    @Test
    fun fxGetsIlsAmount_sameRow() {
        val before = importAll("cal_nov.xlsm")
        val netflixId = before.values.single { it.row.description == "NETFLIX.COM" }.id
        val after = store(before, planImport(before, listOf(parse("cal_next.xlsm"))))
        val netflix = after.getValue(netflixId).row
        assertEquals(RowStatus.COUNTED, netflix.status)
        assertEquals(Money(5_780), netflix.charge)
        assertEquals(1, after.values.count { it.row.description == "NETFLIX.COM" })
    }

    @Test
    fun stalePendingRemoved() {
        val db = importAll("cal_nov.xlsm")
        val oldFuel = db.values.single { it.row.description == "פז דלק" }
        val plan = planImport(db, listOf(parse("cal_next.xlsm")))
        assertEquals(listOf(oldFuel.id), plan.toDelete)
        assertEquals(2, plan.newCount)
        assertEquals(1, plan.updatedCount)
    }

    @Test
    fun pendingWithoutSuccessorIsKept() {
        val db = importAll("cal_nov.xlsm")
        val unrelated = MaxParser.parse(maxSheet(maxRow("20-10-2026", "חדש", "50")))
        assertTrue(planImport(db, listOf(unrelated)).toDelete.isEmpty())
    }

    @Test
    fun olderFileNeverTurnsAChargedLineBackToPending() {
        val db = importAll("cal_nov.xlsm", "cal_next.xlsm")
        val netflixId = db.values.single { it.row.description == "NETFLIX.COM" }.id
        val replay = planImport(db, listOf(parse("cal_nov.xlsm")))
        assertTrue(replay.toWrite.none { it.id == netflixId })
        assertEquals(RowStatus.COUNTED, store(db, replay).getValue(netflixId).row.status)
    }

    @Test
    fun reimportKeepsUserCategory() {
        val db = importAll("cal_nov.xlsm")
        val netflix = db.values.single { it.row.description == "NETFLIX.COM" }
        val categorised = db + (netflix.id to netflix.copy(categoryId = "sub-3"))
        val after = store(categorised, planImport(categorised, listOf(parse("cal_next.xlsm"))))
        assertEquals("sub-3", after.getValue(netflix.id).categoryId)
        val same = store(categorised, planImport(categorised, listOf(parse("cal_nov.xlsm"))))
        assertEquals("sub-3", same.getValue(netflix.id).categoryId)
    }

    @Test
    fun reimportKeepsOwnTransferMark() {
        val db = importAll("hapoalim_oct.xlsx")
        val memory = ImportMemory(ownTransfers = setOf(ImportIds.merchantKey("העברה ב-bit דנה לוי")))
        val after = store(db, planImport(db, listOf(parse("hapoalim_oct.xlsx"), parse("hapoalim_overlap.xlsx"))))
        // The mark lives in the merchant memory, not on the line, so no re-import can undo it.
        assertEquals(RowKind.OWN_TRANSFER, classify(after.values.single { it.row.description == "העברה ב-bit דנה לוי" }.row, memory))
    }

    @Test
    fun waitingNotCounted() {
        val db = importAll("cal_nov.xlsm")
        val plan = planImport(emptyMap(), listOf(parse("cal_nov.xlsm")))
        assertEquals(2, plan.waiting)
        val counted = toTransactions(db.values.toList()).map { it.merchant }
        assertTrue("פז דלק" !in counted && "NETFLIX.COM" !in counted)
        assertEquals(4, counted.size)
    }

    @Test
    fun fileChecksTravelWithThePlan() {
        val plan = planImport(emptyMap(), listOf(parse("max_mismatch.xlsx"), parse("hapoalim_broken.xlsx")))
        assertEquals(listOf(Source.MAX, Source.HAPOALIM), plan.checks.map { it.source })
        assertTrue(plan.checks[0].total is com.menash.myself.feature.money.domain.ImportCheck.Mismatch)
        assertTrue(plan.checks[1].warnings.any { it is ImportWarning.BalanceBreak })
    }
}
