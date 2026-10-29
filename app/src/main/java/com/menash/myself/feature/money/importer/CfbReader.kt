package com.menash.myself.feature.money.importer

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal reader for OLE2 compound files (the container of old .xls): sector chains through the FAT
 * and DIFAT, the directory, and small streams in the mini stream. Read-only, streams by name.
 */
internal class CfbReader(private val bytes: ByteArray) {

    private val buf: ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private val sectorSize = 1 shl u16(0x1E)
    private val miniSectorSize = 1 shl u16(0x20)
    private val miniCutoff = u32(0x38)
    private val fat: IntArray = readFat()
    private val entries: List<Entry> = readDirectory()
    private val miniFat: IntArray = readMiniFat()
    private val miniStream: ByteArray by lazy {
        val root = entries.firstOrNull { it.type == ROOT } ?: return@lazy ByteArray(0)
        chain(root.start, root.size, fat, sectorSize) { sector -> sectorOffset(sector) }
    }

    private data class Entry(val name: String, val type: Int, val start: Int, val size: Int)

    fun streamNames(): List<String> = entries.filter { it.type == STREAM }.map { it.name }

    fun stream(name: String): ByteArray? {
        val entry = entries.firstOrNull { it.type == STREAM && it.name.equals(name, ignoreCase = true) } ?: return null
        return if (entry.size < miniCutoff) {
            readMini(entry)
        } else {
            chain(entry.start, entry.size, fat, sectorSize) { sector -> sectorOffset(sector) }
        }
    }

    private fun readMini(entry: Entry): ByteArray {
        val out = ByteArray(entry.size)
        var sector = entry.start
        var written = 0
        var guard = 0
        while (written < entry.size && sector >= 0) {
            val from = sector * miniSectorSize
            val n = minOf(miniSectorSize, entry.size - written)
            if (from + n > miniStream.size) throw ImportException(FileProblem.CORRUPT, "Mini stream out of range")
            System.arraycopy(miniStream, from, out, written, n)
            written += n
            sector = miniFat.getOrElse(sector) { END }
            if (++guard > miniFat.size + 1) throw ImportException(FileProblem.CORRUPT, "Mini FAT loop")
        }
        return out
    }

    private fun u16(offset: Int): Int = buf.getShort(offset).toInt() and 0xFFFF
    private fun u32(offset: Int): Int = buf.getInt(offset)

    private fun sectorOffset(sector: Int): Int = (sector + 1) * sectorSize

    private fun sectorInts(sector: Int): IntArray {
        val start = sectorOffset(sector)
        if (start < 0 || start + sectorSize > bytes.size) throw ImportException(FileProblem.CORRUPT, "Sector out of range")
        return IntArray(sectorSize / 4) { u32(start + it * 4) }
    }

    private fun readFat(): IntArray {
        val fatSectors = mutableListOf<Int>()
        (0 until 109).map { u32(0x4C + it * 4) }.filter { it >= 0 }.let(fatSectors::addAll)
        var difat = u32(0x44)
        var guard = 0
        while (difat >= 0 && guard++ < 10_000) {
            val ints = sectorInts(difat)
            ints.dropLast(1).filter { it >= 0 }.let(fatSectors::addAll)
            difat = ints.last()
        }
        val fatCount = u32(0x2C)
        return fatSectors.take(fatCount).flatMap { sectorInts(it).toList() }.toIntArray()
    }

    private fun readMiniFat(): IntArray {
        val first = u32(0x3C)
        if (first < 0) return IntArray(0)
        val ints = mutableListOf<Int>()
        var sector = first
        var guard = 0
        while (sector >= 0) {
            ints += sectorInts(sector).toList()
            sector = fat.getOrElse(sector) { END }
            if (++guard > fat.size + 1) throw ImportException(FileProblem.CORRUPT, "Mini FAT chain loop")
        }
        return ints.toIntArray()
    }

    private fun readDirectory(): List<Entry> {
        val list = mutableListOf<Entry>()
        var sector = u32(0x30)
        var guard = 0
        while (sector >= 0) {
            val base = sectorOffset(sector)
            for (i in 0 until sectorSize / 128) {
                val at = base + i * 128
                if (at + 128 > bytes.size) break
                val nameLength = u16(at + 0x40)
                val type = bytes[at + 0x42].toInt()
                if (type == 0 || nameLength < 2) continue
                val name = String(bytes, at, minOf(nameLength - 2, 62), Charsets.UTF_16LE)
                list += Entry(name, type, u32(at + 0x74), u32(at + 0x78))
            }
            sector = fat.getOrElse(sector) { END }
            if (++guard > fat.size + 1) throw ImportException(FileProblem.CORRUPT, "Directory chain loop")
        }
        return list
    }

    private fun chain(start: Int, size: Int, table: IntArray, unit: Int, offsetOf: (Int) -> Int): ByteArray {
        if (size < 0 || size > MAX_FILE_BYTES * 4) throw ImportException(FileProblem.CORRUPT, "Bad stream size")
        val out = ByteArray(size)
        var sector = start
        var written = 0
        var guard = 0
        while (written < size && sector >= 0) {
            val from = offsetOf(sector)
            val n = minOf(unit, size - written)
            if (from < 0 || from + n > bytes.size) throw ImportException(FileProblem.CORRUPT, "Stream out of range")
            System.arraycopy(bytes, from, out, written, n)
            written += n
            sector = table.getOrElse(sector) { END }
            if (++guard > table.size + 1) throw ImportException(FileProblem.CORRUPT, "Sector chain loop")
        }
        if (written < size) throw ImportException(FileProblem.CORRUPT, "Stream shorter than declared")
        return out
    }

    private companion object {
        const val STREAM = 2
        const val ROOT = 5
        const val END = -2
    }
}
