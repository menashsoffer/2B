package com.menash.myself.feature.money.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsReaderEdgeTest {
    private fun modifiedWorkbook(change: (ByteArray, Int) -> Unit): ByteArray {
        val file = ImportFixtures.bytes("onezero_ils.xls")
        val workbook = requireNotNull(CfbReader(file).stream("Workbook"))
        val prefix = workbook.copyOfRange(0, 24)
        val offset = (0..file.size - prefix.size).firstOrNull { at ->
            prefix.indices.all { file[at + it] == prefix[it] }
        }
        requireNotNull(offset) { "Workbook header must be present in the fixture" }
        change(file, offset)
        return file
    }

    @Test fun wrongBiffVersionIsRejected() {
        val bytes = modifiedWorkbook { file, at -> file[at + 4] = 0; file[at + 5] = 0 }
        val error = assertThrows(ImportException::class.java) { XlsReader.read(bytes) }
        assertEquals(FileProblem.UNKNOWN_FORMAT, error.problem)
    }

    @Test fun truncatedFirstRecordIsCorrupt() {
        val bytes = modifiedWorkbook { file, at -> file[at + 2] = 0xff.toByte(); file[at + 3] = 0xff.toByte() }
        val error = assertThrows(ImportException::class.java) { XlsReader.read(bytes) }
        assertEquals(FileProblem.CORRUPT, error.problem)
    }

    @Test fun filePasswordRecordIsRefused() {
        val bytes = modifiedWorkbook { file, at ->
            val length = (file[at + 2].toInt() and 0xff) or ((file[at + 3].toInt() and 0xff) shl 8)
            val next = at + 4 + length
            assertTrue(next + 2 < file.size)
            file[next] = 0x2f
            file[next + 1] = 0
        }
        val error = assertThrows(ImportException::class.java) { XlsReader.read(bytes) }
        assertEquals(FileProblem.ENCRYPTED, error.problem)
    }
}
