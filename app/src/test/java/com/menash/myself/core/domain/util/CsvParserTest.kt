package com.menash.myself.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** The CSV reader behind the file catalog import (FileCatalogScreen). */
class CsvParserTest {

    @Test
    fun exportThenParse_keepsQuotedCommas() {
        val headers = listOf("Name", "Provider", "Value")
        val rows = listOf(
            listOf("קרן השתלמות", "אלטשולר", "145000"),
            listOf("פנסיה", "מנורה, מבטחים", "320000"),
        )
        val parsed = CsvParser.parseCsv(CsvParser.exportToCsv(headers, rows))

        assertEquals(3, parsed.size)
        assertEquals("Name", parsed[0][0])
        assertEquals("מנורה, מבטחים", parsed[2][1])
    }

    @Test
    fun parse_skipsBlankLinesAndRowsWithOnlyEmptyCells() {
        val parsed = CsvParser.parseCsv("a,b\n\n , \n c , d \n")

        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parsed)
    }
}
