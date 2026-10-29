package com.menash.myself.feature.money.importer

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal BIFF8 (Excel 97–2003 .xls) cell reader: sheet names, the shared string table with its
 * CONTINUE records, and the value records (LABELSST, LABEL, NUMBER, RK, MULRK, FORMULA + STRING).
 * Formatting is ignored. A password-protected file is refused as ENCRYPTED.
 */
internal object XlsReader {

    private const val BOF = 0x0809
    private const val EOF = 0x000A
    private const val BOUNDSHEET = 0x0085
    private const val FILEPASS = 0x002F
    private const val SST = 0x00FC
    private const val CONTINUE = 0x003C
    private const val LABELSST = 0x00FD
    private const val LABEL = 0x0204
    private const val RSTRING = 0x00D6
    private const val NUMBER = 0x0203
    private const val RK = 0x027E
    private const val MULRK = 0x00BD
    private const val FORMULA = 0x0006
    private const val STRING = 0x0207

    private class Record(val type: Int, val offset: Int, val data: ByteArray) {
        val buf: ByteBuffer get() = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    }

    fun read(bytes: ByteArray): Workbook {
        val cfb = CfbReader(bytes)
        if (cfb.streamNames().any { it.equals("EncryptedPackage", ignoreCase = true) }) {
            throw ImportException(FileProblem.ENCRYPTED, "Password-protected file")
        }
        val stream = cfb.stream("Workbook") ?: cfb.stream("Book")
            ?: throw ImportException(FileProblem.UNKNOWN_FORMAT, "No workbook stream")
        val records = records(stream)
        val first = records.firstOrNull() ?: throw ImportException(FileProblem.CORRUPT, "Empty workbook")
        if (first.type != BOF || first.data.size < 2 || (first.buf.getShort(0).toInt() and 0xFFFF) != 0x0600) {
            throw ImportException(FileProblem.UNKNOWN_FORMAT, "Only Excel 97 and later .xls files are supported")
        }
        if (records.any { it.type == FILEPASS }) throw ImportException(FileProblem.ENCRYPTED, "Password-protected file")
        val sheets = mutableListOf<Pair<String, Int>>()
        var sst: List<String> = emptyList()
        var i = 0
        while (i < records.size) {
            val r = records[i]
            when (r.type) {
                BOUNDSHEET -> if (r.data.size > 8 && r.data[5].toInt() == 0) {
                    val cch = r.data[6].toInt() and 0xFF
                    sheets += StringCursor(listOf(r.data), 7).chars(cch) to r.buf.getInt(0)
                }
                SST -> {
                    val chunks = mutableListOf(r.data)
                    while (i + 1 < records.size && records[i + 1].type == CONTINUE) chunks += records[++i].data
                    sst = readSst(chunks)
                }
                EOF -> break
            }
            i++
        }
        val byOffset = records.withIndex().associate { it.value.offset to it.index }
        return Workbook(sheets.map { (name, offset) ->
            Sheet(name, byOffset[offset]?.let { cells(records, it, sst) }.orEmpty())
        })
    }

    private fun records(stream: ByteArray): List<Record> {
        val list = mutableListOf<Record>()
        val buf = ByteBuffer.wrap(stream).order(ByteOrder.LITTLE_ENDIAN)
        var at = 0
        while (at + 4 <= stream.size) {
            val type = buf.getShort(at).toInt() and 0xFFFF
            val length = buf.getShort(at + 2).toInt() and 0xFFFF
            if (at + 4 + length > stream.size) break
            list += Record(type, at, stream.copyOfRange(at + 4, at + 4 + length))
            at += 4 + length
        }
        return list
    }

    /** Reads BIFF8 unicode strings across CONTINUE boundaries (each continuation restarts with a flags byte). */
    private class StringCursor(private val chunks: List<ByteArray>, private var pos: Int) {
        private var chunk = 0

        private fun ensure() {
            while (chunk < chunks.size && pos >= chunks[chunk].size) { chunk++; pos = 0 }
            if (chunk >= chunks.size) throw ImportException(FileProblem.CORRUPT, "String table ends early")
        }
        fun u8(): Int { ensure(); return chunks[chunk][pos++].toInt() and 0xFF }
        fun u16(): Int = u8() or (u8() shl 8)
        fun u32(): Int = u16() or (u16() shl 16)
        fun skip(n: Int) { repeat(n) { u8() } }

        /** cch characters whose flags byte comes first. */
        fun chars(cch: Int): String {
            var wide = u8() and 0x01 != 0
            return charsWith(cch, wide) { wide = it }
        }

        fun charsWith(cch: Int, startWide: Boolean, onFlags: (Boolean) -> Unit = {}): String {
            val sb = StringBuilder(cch)
            var wide = startWide
            while (sb.length < cch) {
                if (chunk < chunks.size && pos >= chunks[chunk].size) {
                    chunk++; pos = 0
                    wide = u8() and 0x01 != 0
                    onFlags(wide)
                    continue
                }
                sb.append(if (wide) u16().toChar() else u8().toChar())
            }
            return sb.toString()
        }

        /** A full XLUnicodeRichExtendedString as stored in the SST. */
        fun richString(): String {
            val cch = u16()
            val flags = u8()
            val runs = if (flags and 0x08 != 0) u16() else 0
            val ext = if (flags and 0x04 != 0) u32() else 0
            val text = charsWith(cch, flags and 0x01 != 0)
            skip(runs * 4)
            skip(ext)
            return text
        }
    }

    private fun readSst(chunks: List<ByteArray>): List<String> {
        val cursor = StringCursor(chunks, 8)
        val unique = ByteBuffer.wrap(chunks[0]).order(ByteOrder.LITTLE_ENDIAN).getInt(4)
        if (unique < 0 || unique > MAX_ROWS * 50) throw ImportException(FileProblem.CORRUPT, "Bad string table")
        return List(unique) { cursor.richString() }
    }

    private fun rk(value: Int): Double {
        val number = if (value and 0x02 != 0) (value shr 2).toDouble()
        else java.lang.Double.longBitsToDouble((value.toLong() and 0xFFFFFFFCL) shl 32)
        return if (value and 0x01 != 0) number / 100 else number
    }

    private fun cells(records: List<Record>, bofIndex: Int, sst: List<String>): List<List<Cell?>> {
        val grid = sortedMapOf<Int, MutableMap<Int, Cell>>()
        fun put(row: Int, col: Int, cell: Cell?) {
            if (cell == null) return
            checkRowCount(row + 1)
            grid.getOrPut(row) { mutableMapOf() }[col] = cell
        }
        var pendingFormula: Pair<Int, Int>? = null
        var i = bofIndex + 1
        while (i < records.size) {
            val r = records[i++]
            if (r.type == EOF) break
            val b = r.buf
            if (r.type == STRING) {
                pendingFormula?.let { (fr, fc) ->
                    val cch = b.getShort(0).toInt() and 0xFFFF
                    put(fr, fc, Cell.Text(StringCursor(listOf(r.data), 2).chars(cch)))
                }
                pendingFormula = null
                continue
            }
            if (r.data.size < 6) continue
            val row = b.getShort(0).toInt() and 0xFFFF
            val col = b.getShort(2).toInt() and 0xFFFF
            when (r.type) {
                LABELSST -> put(row, col, sst.getOrNull(b.getInt(6))?.let { Cell.Text(it) })
                LABEL, RSTRING -> {
                    val cch = b.getShort(6).toInt() and 0xFFFF
                    put(row, col, Cell.Text(StringCursor(listOf(r.data), 8).chars(cch)))
                }
                NUMBER -> put(row, col, Cell.Number(b.getDouble(6)))
                RK -> put(row, col, Cell.Number(rk(b.getInt(6))))
                MULRK -> {
                    val last = b.getShort(r.data.size - 2).toInt() and 0xFFFF
                    for (c in col..last) put(row, c, Cell.Number(rk(b.getInt(4 + (c - col) * 6 + 2))))
                }
                FORMULA -> {
                    val marker = b.getShort(12).toInt() and 0xFFFF
                    if (marker != 0xFFFF) put(row, col, Cell.Number(b.getDouble(6)))
                    else if (r.data[6].toInt() == 0) pendingFormula = row to col
                }
            }
        }
        if (grid.isEmpty()) return emptyList()
        return (0..grid.lastKey()).map { r ->
            val cells = grid[r] ?: return@map emptyList()
            (0..cells.keys.max()).map { cells[it] }
        }
    }
}
