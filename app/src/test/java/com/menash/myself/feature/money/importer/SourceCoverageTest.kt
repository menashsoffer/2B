package com.menash.myself.feature.money.importer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class SourceCoverageTest {
    @Test fun gapsAreCalculatedPerSourceAndDuplicatesAreIgnored() {
        val row = ImportFixtures.parse("max_nov.xlsx").rows.first()
        val first = row.copy(date = YearMonth.of(2024, 1).atDay(1), chargeDate = null)
        val last = row.copy(date = YearMonth.of(2024, 3).atDay(1), chargeDate = null)
        val other = first.copy(sourceId = "9999")
        val coverage = sourceCoverage(listOf(first, first, last, other))
        val original = coverage.first { it.sourceId == first.sourceId }
        assertEquals(listOf(YearMonth.of(2024, 1), YearMonth.of(2024, 3)), original.months)
        assertEquals(listOf(YearMonth.of(2024, 2)), original.missingMonths)
        assertEquals(emptyList<YearMonth>(), coverage.first { it.sourceId == "9999" }.missingMonths)
    }
}
