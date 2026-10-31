package com.menash.myself

import com.menash.myself.data.io.CsvHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun csvHelper_parsesAndExportsCorrectly() {
        val headers = listOf("Name", "Provider", "Value")
        val rows = listOf(
            listOf("קרן השתלמות", "אלטשולר", "145000"),
            listOf("פנסיה", "מנורה, מבטחים", "320000")
        )
        val csv = CsvHelper.exportToCsv(headers, rows)
        val parsed = CsvHelper.parseCsv(csv)

        assertEquals(3, parsed.size)
        assertEquals("Name", parsed[0][0])
        assertEquals("מנורה, מבטחים", parsed[2][1])
    }
}
