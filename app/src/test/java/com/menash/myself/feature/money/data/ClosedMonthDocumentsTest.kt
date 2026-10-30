package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.*
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class ClosedMonthDocumentsTest {
    private val month = YearMonth.of(2026, 10)
    private val date = LocalDate.of(2026, 11, 1)
    private val closed = ClosedMonth(
        month, MonthSnapshot(month, Money(10000), Money(2000)),
        mapOf("food" to Money(2100), null to Money(-100)),
        mapOf("car" to PeriodicStatus(Money(1000), Money(2000), Money(-500))),
        mapOf("bank" to Money(30000)), mapOf("loan" to Money(7000)), date,
    )
    private val required: Map<String, Any?> = mapOf("actualIncomes" to 10000L, "actualExpenses" to 2000L, "closedOn" to "2026-11-01")
    private fun decode(data: Map<String, Any?>) = ClosedMonthDocuments.decode("2026-10", data)

    @Test fun roundTripPreservesFrozenValuesAndDerivesAdvance() {
        val encoded = ClosedMonthDocuments.encode(closed)
        assertEquals(closed, decode(encoded))
        val periodic = (encoded.getValue("periodic") as Map<*, *>).getValue("car") as Map<*, *>
        assertEquals(setOf("accruedToDate", "spentToDate", "remainingThisYear"), periodic.keys)
        assertEquals(Money(1000), decode(encoded).periodic.getValue("car").advance)
        assertEquals(-100L, (encoded.getValue("byCategory") as Map<*, *>)["_none"])
        assertEquals("2026-11-01", encoded["closedOn"])
        assertTrue(decode(encoded).snapshot.balances.isEmpty())
    }

    @Test fun absentOptionalMapsBecomeEmpty() {
        val decoded = decode(required)
        assertTrue(decoded.byCategory.isEmpty())
        assertTrue(decoded.periodic.isEmpty())
        assertTrue(decoded.balances.isEmpty())
        assertTrue(decoded.liabilities.isEmpty())
    }

    @Test fun incomeExpenseAndCloseDateAreRequired() {
        for (field in required.keys) {
            assertThrows(IllegalArgumentException::class.java) { decode(required - field) }
            assertThrows(IllegalArgumentException::class.java) { decode(required + (field to null)) }
        }
    }

    @Test fun documentIdMustBeCanonicalYearMonthOnEncodeAndDecode() {
        val invalid = listOf("2026-1", "26-10", "2026-00", "2026-13", "2026-10-01", "2026-10 ", "+2026-10", "-0001-10", "202610", "", "10000-10")
        invalid.forEach { id -> assertThrows(IllegalArgumentException::class.java) { ClosedMonthDocuments.decode(id, required) } }
        for (year in listOf(-1, 10000)) {
            val m = YearMonth.of(year, 10)
            assertThrows(IllegalArgumentException::class.java) {
                ClosedMonthDocuments.encode(closed.copy(month = m, snapshot = closed.snapshot.copy(month = m)))
            }
        }
        assertEquals(YearMonth.of(2024, 2), ClosedMonthDocuments.decode("2024-02", required).month)
    }

    @Test fun malformedAmountsFailRatherThanCoercingNumbers() {
        val invalid = listOf(null, "100", 100, 1.5, Double.NaN, Double.POSITIVE_INFINITY, true, listOf(1L))
        for (value in invalid) {
            for (field in listOf("actualIncomes", "actualExpenses")) {
                assertThrows(IllegalArgumentException::class.java) { decode(required + (field to value)) }
            }
            for (field in listOf("byCategory", "balances", "liabilities")) {
                assertThrows(IllegalArgumentException::class.java) { decode(required + (field to mapOf("id" to value))) }
            }
            for (field in listOf("accruedToDate", "spentToDate", "remainingThisYear")) {
                val status = mapOf("accruedToDate" to 1L, "spentToDate" to 2L, "remainingThisYear" to 3L) + (field to value)
                assertThrows(IllegalArgumentException::class.java) { decode(required + ("periodic" to mapOf("id" to status))) }
            }
        }
    }

    @Test fun longAmountsAreExactAndNegativeSnapshotOrBalanceAmountsFail() {
        assertEquals(Money(Long.MAX_VALUE), decode(required + ("actualIncomes" to Long.MAX_VALUE)).snapshot.actualIncomes)
        for (field in listOf("actualIncomes", "actualExpenses")) {
            assertThrows(IllegalArgumentException::class.java) { decode(required + (field to -1L)) }
        }
        for (field in listOf("balances", "liabilities")) {
            assertThrows(IllegalArgumentException::class.java) { decode(required + (field to mapOf("id" to -1L))) }
            assertEquals(Money.ZERO, decode(required + (field to mapOf("id" to 0L))).let {
                if (field == "balances") it.balances["id"] else it.liabilities["id"]
            })
        }
    }

    @Test fun presentMalformedMapsAndPeriodicStatusesFail() {
        for (field in listOf("byCategory", "periodic", "balances", "liabilities")) {
            for (bad in listOf(null, "text", listOf(1L), 1L, mapOf(1 to 2L))) {
                assertThrows(IllegalArgumentException::class.java) { decode(required + (field to bad)) }
            }
        }
        for (bad in listOf(null, 1L, emptyMap<String, Long>(), mapOf("accruedToDate" to 1L), mapOf(1 to 2L))) {
            assertThrows(IllegalArgumentException::class.java) { decode(required + ("periodic" to mapOf("id" to bad))) }
        }
    }

    @Test fun malformedCloseDatesFail() {
        for (bad in listOf("2026-02-30", "2026-1-01", "2026-11-01T00:00:00", "2026-11-01 ", 1L)) {
            assertThrows(Exception::class.java) { decode(required + ("closedOn" to bad)) }
        }
    }

    @Test fun literalReservedCategoryCannotCollideWithNull() {
        for (categories in listOf(mapOf("_none" to Money(1)), mapOf(null to Money(2), "_none" to Money(1)))) {
            assertThrows(IllegalArgumentException::class.java) { ClosedMonthDocuments.encode(closed.copy(byCategory = categories)) }
        }
    }

    @Test fun assetIdCannotAppearInBothMaps() {
        assertThrows(IllegalArgumentException::class.java) {
            decode(required + mapOf("balances" to mapOf("same" to 1L), "liabilities" to mapOf("same" to 2L)))
        }
    }

    @Test fun exactlySevenTopLevelKeysStayBelowExistingTwentyKeyLimit() {
        val many = (1..100).associate { "id-$it" to Money(it.toLong()) }
        val encoded = ClosedMonthDocuments.encode(closed.copy(balances = many))
        assertEquals(setOf("actualIncomes", "actualExpenses", "byCategory", "periodic", "balances", "liabilities", "closedOn"), encoded.keys)
        assertEquals(7, encoded.size)
        assertTrue(encoded.size <= 20)
        assertEquals(100, (encoded["balances"] as Map<*, *>).size)
    }

    @Test fun assetsStoreOnlyIdsAndAmountsWithoutNamesOrFabricatedMetadata() {
        val asset = Asset("asset-id", "private bank name", "private type", Money(100), AssetKind.ASSET, true, date)
        val value = closeMonth(month, MoneyItems(), emptyList(), Money.ZERO, listOf(asset), null, date)
        val encoded = ClosedMonthDocuments.encode(value)
        assertEquals(mapOf("asset-id" to 100L), encoded["balances"])
        assertFalse(encoded.toString().contains("private"))
        assertFalse(encoded.toString().contains("lastUpdated"))
        assertEquals(value, decode(encoded))
    }
}
