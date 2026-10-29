package com.menash.myself.feature.money.importer

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

/**
 * Minimal reader for xlsx / xlsm (Office Open XML) on the platform's zip and SAX parser, no library.
 * Reads cell values only: shared strings, inline strings, numbers, formula results. Styles are ignored;
 * the parsers know which columns hold dates.
 */
internal object XlsxReader {

    private const val MAX_UNZIPPED = 60L * 1024 * 1024

    fun read(bytes: ByteArray): Workbook {
        val parts = unzip(bytes)
        val workbook = parts["xl/workbook.xml"] ?: throw ImportException(FileProblem.CORRUPT, "No workbook part")
        val rels = parts["xl/_rels/workbook.xml.rels"]?.let(::relationships).orEmpty()
        val strings = parts["xl/sharedStrings.xml"]?.let(::sharedStrings).orEmpty()
        val sheets = sheetRefs(workbook).mapNotNull { (name, relId) ->
            val target = rels[relId] ?: return@mapNotNull null
            val path = if (target.startsWith("/")) target.removePrefix("/") else "xl/$target"
            parts[path]?.let { Sheet(name, sheetRows(it, strings)) }
        }
        return Workbook(sheets)
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val parts = mutableMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = zip.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_UNZIPPED) throw ImportException(FileProblem.TOO_LARGE, "Unzipped content too large")
                    out.write(buffer, 0, n)
                }
                parts[entry.name.removePrefix("/")] = out.toByteArray()
            }
        }
        return parts
    }

    private fun parse(xml: ByteArray, handler: DefaultHandler) {
        val factory = SAXParserFactory.newInstance()
        factory.isNamespaceAware = true
        listOf(
            "http://xml.org/sax/features/external-general-entities",
            "http://xml.org/sax/features/external-parameter-entities",
        ).forEach { feature -> runCatching { factory.setFeature(feature, false) } }
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        factory.newSAXParser().parse(InputSource(ByteArrayInputStream(xml)), handler)
    }

    private fun Attributes.byLocalName(name: String): String? =
        (0 until length).firstOrNull { getLocalName(it) == name || getQName(it) == name }?.let { getValue(it) }

    private fun relationships(xml: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        parse(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                if (localName == "Relationship") {
                    val id = attributes.byLocalName("Id")
                    val target = attributes.byLocalName("Target")
                    if (id != null && target != null) map[id] = target
                }
            }
        })
        return map
    }

    private fun sheetRefs(xml: ByteArray): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        parse(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                if (localName == "sheet") {
                    val name = attributes.byLocalName("name") ?: ""
                    val relId = (0 until attributes.length)
                        .firstOrNull { attributes.getLocalName(it) == "id" }?.let { attributes.getValue(it) }
                    if (relId != null) list += name to relId
                }
            }
        })
        return list
    }

    /** Each <si> is the text of its <t> runs, phonetic hints (<rPh>) left out. */
    private fun sharedStrings(xml: ByteArray): List<String> {
        val list = mutableListOf<String>()
        parse(xml, object : DefaultHandler() {
            private val text = StringBuilder()
            private var inT = false
            private var inPhonetic = false
            override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                when (localName) {
                    "si" -> text.setLength(0)
                    "rPh" -> inPhonetic = true
                    "t" -> inT = !inPhonetic
                }
            }
            override fun endElement(uri: String?, localName: String, qName: String?) {
                when (localName) {
                    "si" -> list += text.toString()
                    "rPh" -> inPhonetic = false
                    "t" -> inT = false
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inT) text.appendRange(ch, start, start + length)
            }
        })
        return list
    }

    private fun columnIndex(ref: String): Int {
        var index = 0
        for (ch in ref) {
            if (ch !in 'A'..'Z') break
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
    }

    private fun sheetRows(xml: ByteArray, strings: List<String>): List<List<Cell?>> {
        val rows = sortedMapOf<Int, MutableMap<Int, Cell>>()
        parse(xml, object : DefaultHandler() {
            private var rowIndex = -1
            private var colIndex = -1
            private var type: String? = null
            private val value = StringBuilder()
            private var capture = false
            override fun startElement(uri: String?, localName: String, qName: String?, attributes: Attributes) {
                when (localName) {
                    "row" -> {
                        rowIndex = attributes.byLocalName("r")?.toIntOrNull()?.minus(1) ?: (rowIndex + 1)
                        colIndex = -1
                        checkRowCount(rowIndex + 1)
                    }
                    "c" -> {
                        colIndex = attributes.byLocalName("r")?.let(::columnIndex) ?: (colIndex + 1)
                        type = attributes.byLocalName("t")
                        value.setLength(0)
                    }
                    "v", "t" -> capture = true
                }
            }
            override fun endElement(uri: String?, localName: String, qName: String?) {
                when (localName) {
                    "v", "t" -> capture = false
                    "c" -> cellOf(type, value.toString(), strings)?.let { rows.getOrPut(rowIndex) { mutableMapOf() }[colIndex] = it }
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (capture) value.appendRange(ch, start, start + length)
            }
        })
        if (rows.isEmpty()) return emptyList()
        return (0..rows.lastKey()).map { r ->
            val cells = rows[r] ?: return@map emptyList()
            (0..cells.keys.max()).map { cells[it] }
        }
    }

    private fun cellOf(type: String?, raw: String, strings: List<String>): Cell? = when (type) {
        "s" -> raw.trim().toIntOrNull()?.let { strings.getOrNull(it) }?.let { Cell.Text(it) }
        "inlineStr", "str" -> raw.takeIf { it.isNotEmpty() }?.let { Cell.Text(it) }
        "b", "e" -> null
        else -> raw.trim().toDoubleOrNull()?.let { Cell.Number(it) } ?: raw.takeIf { it.isNotBlank() }?.let { Cell.Text(it) }
    }
}
