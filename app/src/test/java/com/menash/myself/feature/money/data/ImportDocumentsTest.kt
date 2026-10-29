package com.menash.myself.feature.money.data

import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.ImportIds
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportDocumentsTest {

    @Test
    fun everyFixtureRowRoundTrips() {
        listOf("hapoalim_oct.xlsx", "onezero_ils.xls", "onezero_usd.xls", "max_nov.xlsx", "cal_nov.xlsm").forEach { name ->
            ImportFixtures.parse(name).rows.forEachIndexed { i, row ->
                val stored = StoredTransaction(row, categoryId = if (i % 2 == 0) "current-1" else null)
                assertEquals(name, stored, ImportDocuments.decode(row.id, ImportDocuments.encode(stored)))
            }
        }
    }

    @Test
    fun documentsFitTheRules() {
        ImportFixtures.parse("max_nov.xlsx").rows.forEach { row ->
            val doc = ImportDocuments.encode(StoredTransaction(row, "current-1"))
            assertTrue(doc.size <= 20)
            assertTrue(row.id.matches(Regex("^[a-zA-Z0-9_\\-.]+$")) && row.id.length <= 128)
            assertTrue("id" !in doc)
        }
        assertTrue(ImportDocuments.memoryId("שופרסל דיל").matches(Regex("^m-[0-9a-f]{24}$")))
    }

    @Test
    fun memoryRoundTrips() {
        val shufersal = ImportIds.merchantKey("שופרסל דיל 123")
        val oneZero = ImportIds.merchantKey("העברה לחשבון אחר וואן זירו")
        val memory = ImportDocuments.decodeMemory(listOf(
            ImportDocuments.encodeMemory(shufersal, "current-1", false),
            ImportDocuments.encodeMemory(oneZero, null, true),
            mapOf("categoryId" to "orphan"),
        ))
        assertEquals(mapOf(shufersal to "current-1"), memory.merchants.categories)
        assertEquals(setOf(oneZero), memory.ownTransfers)
    }
}
