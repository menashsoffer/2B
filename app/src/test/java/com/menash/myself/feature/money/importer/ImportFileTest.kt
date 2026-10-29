package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.importer.ImportFixtures.parse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Edge cases ד (bad files) and the readers themselves. */
class ImportFileTest {

    private fun problemOf(bytes: ByteArray): FileProblem =
        assertThrows(ImportException::class.java) { parseStatement(bytes) }.problem

    @Test
    fun unknownFileRejected() {
        assertEquals(FileProblem.UNKNOWN_FORMAT, problemOf(ImportFixtures.bytes("unknown.xlsx")))
        assertEquals(FileProblem.UNKNOWN_FORMAT, problemOf(ImportFixtures.bytes("not_excel.xlsx")))
    }

    @Test
    fun corruptFileRejected() {
        assertEquals(FileProblem.CORRUPT, problemOf(ImportFixtures.bytes("corrupt.xlsx")))
        assertEquals(FileProblem.UNKNOWN_FORMAT, problemOf(ByteArray(0)))
        val truncated = ImportFixtures.bytes("onezero_ils.xls").copyOf(1500)
        assertEquals(FileProblem.CORRUPT, problemOf(truncated))
    }

    @Test
    fun encryptedFileRejected() {
        assertEquals(FileProblem.ENCRYPTED, problemOf(ImportFixtures.bytes("encrypted.xls")))
    }

    @Test
    fun tooLargeRejected() {
        val big = ByteArray(MAX_FILE_BYTES + 1).also { byteArrayOf(0x50, 0x4B, 0x03, 0x04).copyInto(it) }
        assertEquals(FileProblem.TOO_LARGE, problemOf(big))
        assertThrows(ImportException::class.java) { checkRowCount(MAX_ROWS + 1) }
    }

    @Test
    fun emptyStatement() {
        val empty = parse("max_empty.xlsx")
        assertEquals(Source.MAX, empty.source)
        assertTrue(empty.rows.isEmpty())
        assertTrue(empty.warnings.isEmpty())
    }

    @Test
    fun xlsReaderReadsTextNumbersAndDates() {
        val sheet = readWorkbook(ImportFixtures.bytes("onezero_ils.xls")).sheets.first()
        assertEquals("2026-10-01_2026-10-31_ALL", sheet.name)
        assertEquals("תאריך תנועה", (sheet.cell(0, 0) as Cell.Text).value)
        assertEquals(LocalDate.of(2026, 10, 1), dateOf(sheet.cell(1, 0)))
        assertEquals(AmountCell.Value(Money(200_000)), amountOf(sheet.cell(1, 4)))
        assertEquals("OZ-1001", sheet.cell(1, 8).text())
    }

    @Test
    fun xlsxReaderReadsSharedStringsAndLineBreaks() {
        val sheet = readWorkbook(ImportFixtures.bytes("cal_nov.xlsm")).sheets.first()
        assertEquals("פירוט עסקאות וזיכויים", sheet.name)
        assertEquals("תאריך\nעסקה", (sheet.cell(1, 0) as Cell.Text).value)
        assertEquals(LocalDate.of(2026, 10, 3), dateOf(sheet.cell(2, 0)))
    }

    @Test
    fun sourceDetectedFromHeadersNotFileName() {
        assertEquals(Source.HAPOALIM, parseStatement(ImportFixtures.bytes("hapoalim_oct.xlsx")).source)
        assertEquals(Source.MAX, parseStatement(ImportFixtures.bytes("max_nov.xlsx")).source)
        assertEquals(Source.CAL, parseStatement(ImportFixtures.bytes("cal_nov.xlsm")).source)
        assertEquals(Source.ONE_ZERO, parseStatement(ImportFixtures.bytes("onezero_ils.xls")).source)
    }

    @Test
    fun fileNameIgnored() {
        // The parser only ever sees bytes; the same bytes under any name give the same ids.
        assertEquals(parse("max_nov.xlsx").rows.map { it.id }, parseStatement(ImportFixtures.bytes("max_nov.xlsx").copyOf()).rows.map { it.id })
    }
}
