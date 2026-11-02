package com.menash.myself.feature.money.importer

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class XlsxReaderEdgeTest {
    private fun archive(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private val workbook = """<workbook xmlns:r="urn:rel"><sheets><sheet name="Data" r:id="r1"/></sheets></workbook>"""
    private val relationships = """<Relationships><Relationship Id="r1" Target="/xl/worksheets/sheet1.xml"/></Relationships>"""

    @Test fun missingWorkbookIsReportedAsCorrupt() {
        val error = assertThrows(ImportException::class.java) { XlsxReader.read(archive("irrelevant" to "x")) }
        assertEquals(FileProblem.CORRUPT, error.problem)
    }

    @Test fun missingRelationshipAndSheetAreSkipped() {
        val book = XlsxReader.read(archive("xl/workbook.xml" to workbook))
        assertTrue(book.sheets.isEmpty())
        val missing = XlsxReader.read(archive("xl/workbook.xml" to workbook, "xl/_rels/workbook.xml.rels" to relationships))
        assertTrue(missing.sheets.isEmpty())
    }

    @Test fun mixedCellsAndSharedTextAreReadWithoutPhoneticHints() {
        val strings = """<sst><si><r><t>hello</t></r><rPh><t>ignored</t></rPh><r><t> world</t></r></si></sst>"""
        val sheet = """<worksheet><sheetData><row r="2"><c r="A2" t="s"><v>0</v></c><c r="B2" t="inlineStr"><is><t>inline</t></is></c><c r="C2"><v>-12.5</v></c><c r="D2" t="b"><v>1</v></c><c r="E2" t="e"><v>#ERR</v></c><c r="F2"><v>text</v></c><c r="G2" t="s"><v>9</v></c></row></sheetData></worksheet>"""
        val book = XlsxReader.read(archive("xl/workbook.xml" to workbook, "xl/_rels/workbook.xml.rels" to relationships, "xl/sharedStrings.xml" to strings, "xl/worksheets/sheet1.xml" to sheet))
        val row = book.sheets.single()
        assertEquals("hello world", row.cell(1, 0).text())
        assertEquals("inline", row.cell(1, 1).text())
        assertEquals(Cell.Number(-12.5), row.cell(1, 2))
        assertEquals(null, row.cell(1, 3))
        assertEquals(null, row.cell(1, 4))
        assertEquals("text", row.cell(1, 5).text())
        assertEquals(null, row.cell(1, 6))
    }

    @Test fun implicitCoordinatesAndSparseRowsArePreserved() {
        val sheet = """<worksheet><sheetData><row><c t="str"><v>first</v></c><c><v>42</v></c></row><row r="3"><c r="C3"><v>3.5</v></c></row></sheetData></worksheet>"""
        val result = XlsxReader.read(archive("xl/workbook.xml" to workbook, "xl/_rels/workbook.xml.rels" to relationships, "xl/worksheets/sheet1.xml" to sheet)).sheets.single()
        assertEquals("first", result.cell(0, 0).text())
        assertEquals(Cell.Number(42.0), result.cell(0, 1))
        assertEquals(null, result.cell(1, 0))
        assertEquals(Cell.Number(3.5), result.cell(2, 2))
    }

    @Test fun sheetWithoutRowsAndMissingSheetIdsAreIgnored() {
        val emptySheet = """<worksheet><sheetData/></worksheet>"""
        val result = XlsxReader.read(archive("xl/workbook.xml" to workbook, "xl/_rels/workbook.xml.rels" to relationships, "xl/worksheets/sheet1.xml" to emptySheet))
        assertTrue(result.sheets.single().rows.isEmpty())
        val noId = """<workbook><sheets><sheet name="No id"/></sheets></workbook>"""
        assertTrue(XlsxReader.read(archive("xl/workbook.xml" to noId)).sheets.isEmpty())
    }

    @Test fun invalidRelationshipEntriesAreIgnoredAndRelativeTargetIsResolved() {
        val refs = """<Relationships><Relationship Id="missingTarget"/><Relationship Target="missingId"/><Relationship Id="r1" Target="worksheets/sheet1.xml"/></Relationships>"""
        val sheets = """<workbook xmlns:r="urn:rel"><sheets><sheet r:id="r1"/><sheet name="ignored" r:id="unknown"/></sheets></workbook>"""
        val result = XlsxReader.read(archive("xl/workbook.xml" to sheets, "xl/_rels/workbook.xml.rels" to refs, "xl/worksheets/sheet1.xml" to "<worksheet/>"))
        assertEquals(1, result.sheets.size)
        assertEquals("", result.sheets.single().name)
        assertTrue(result.sheets.single().rows.isEmpty())
    }

    @Test fun emptyInvalidAndOutOfRangeCellValuesAreIgnored() {
        val sheet = """<worksheet><sheetData><row><c t="s"><v>not a number</v></c><c t="s"><v>10</v></c><c t="str"><v></v></c><c><v>  </v></c><c t="inlineStr"><is><t>kept</t></is></c></row></sheetData></worksheet>"""
        val result = XlsxReader.read(archive("xl/workbook.xml" to workbook, "xl/_rels/workbook.xml.rels" to relationships, "xl/worksheets/sheet1.xml" to sheet)).sheets.single()
        for (column in 0..3) assertEquals(null, result.cell(0, column))
        assertEquals("kept", result.cell(0, 4).text())
    }

    @Test fun rowPastLimitIsRejected() {
        val sheet = """<worksheet><sheetData><row r="${MAX_ROWS + 1}"/></sheetData></worksheet>"""
        val error = assertThrows(org.xml.sax.SAXException::class.java) {
            XlsxReader.read(archive("xl/workbook.xml" to workbook, "xl/_rels/workbook.xml.rels" to relationships, "xl/worksheets/sheet1.xml" to sheet))
        }
        assertEquals(FileProblem.TOO_LARGE, (error.cause as ImportException).problem)
    }

}
