package com.menash.myself.feature.money.importer

/** One spreadsheet cell as stored in the file. Dates arrive as Excel serial numbers or as text. */
sealed class Cell {
    data class Text(val value: String) : Cell()
    data class Number(val value: Double) : Cell()
}

data class Sheet(val name: String, val rows: List<List<Cell?>>) {
    fun cell(row: Int, col: Int): Cell? = rows.getOrNull(row)?.getOrNull(col)
}

data class Workbook(val sheets: List<Sheet>)

/** Why a whole file was refused. Nothing from a refused file is imported. */
enum class FileProblem { UNKNOWN_FORMAT, CORRUPT, ENCRYPTED, TOO_LARGE, NO_TRANSACTIONS }

class ImportException(val problem: FileProblem, message: String, cause: Throwable? = null) : Exception(message, cause)

const val MAX_FILE_BYTES = 10 * 1024 * 1024
const val MAX_ROWS = 20_000

private val zipMagic = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
private val cfbMagic = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())

private fun ByteArray.startsWith(prefix: ByteArray) = size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

/** xlsx and xlsm are zip packages; old xls (and password-protected xlsx) are OLE2 compound files. */
fun readWorkbook(bytes: ByteArray): Workbook {
    if (bytes.size > MAX_FILE_BYTES) throw ImportException(FileProblem.TOO_LARGE, "File is larger than $MAX_FILE_BYTES bytes")
    return try {
        when {
            bytes.startsWith(zipMagic) -> XlsxReader.read(bytes)
            bytes.startsWith(cfbMagic) -> XlsReader.read(bytes)
            else -> throw ImportException(FileProblem.UNKNOWN_FORMAT, "Not an Excel file")
        }
    } catch (e: ImportException) {
        throw e
    } catch (e: Exception) {
        throw ImportException(FileProblem.CORRUPT, "Unreadable file: ${e.javaClass.simpleName}", e)
    }
}

internal fun checkRowCount(count: Int) {
    if (count > MAX_ROWS) throw ImportException(FileProblem.TOO_LARGE, "More than $MAX_ROWS rows")
}
