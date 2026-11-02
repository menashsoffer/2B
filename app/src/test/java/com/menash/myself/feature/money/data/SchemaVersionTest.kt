package com.menash.myself.feature.money.data

import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.importer.ImportIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Old documents (no "v") still load; a document from a newer app is refused, not half-read. */
class SchemaVersionTest {

    private val entry = MoneyEntry.IncomeEntry(MoneySamples.salary)
    private val encoded = MoneyDocuments.encode(entry)

    @Test
    fun missingVersionReadsAsOne() {
        assertEquals(entry, MoneyDocuments.decode(entry.collection, entry.id, encoded - SchemaVersion.KEY))
        assertEquals(1L, SchemaVersion.of(emptyMap()))
    }

    @Test
    fun currentVersionReads() {
        assertEquals(1L, encoded[SchemaVersion.KEY])
        assertEquals(entry, MoneyDocuments.decode(entry.collection, entry.id, encoded))
    }

    @Test
    fun newerVersionIsRefusedAsAnIllegalArgument() {
        val newer = encoded + (SchemaVersion.KEY to 99L)
        val error = assertThrows(IllegalArgumentException::class.java) { MoneyDocuments.decode(entry.collection, entry.id, newer) }
        assertTrue(error is NewerSchemaException)
        val stored = MoneyFixtures.all().getValue(ImportDocuments.TRANSACTIONS).getValue("card-full")
        assertThrows(NewerSchemaException::class.java) { ImportDocuments.decode("max-1a2b", stored + (SchemaVersion.KEY to 99L)) }
    }

    @Test
    fun newerMerchantMemoryIsIgnoredOthersStillLoad() {
        val key = ImportIds.merchantKey("שופרסל")
        val old = ImportDocuments.encodeMemory(key, "current-1", false) - SchemaVersion.KEY
        val newer = ImportDocuments.encodeMemory("ביט", "current-2", false) + (SchemaVersion.KEY to 99L)
        val memory = ImportDocuments.decodeMemory(listOf(old, newer))
        assertEquals(mapOf(key to "current-1"), memory.merchants.categories)
    }
}
