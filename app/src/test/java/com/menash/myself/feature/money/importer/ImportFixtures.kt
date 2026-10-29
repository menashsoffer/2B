package com.menash.myself.feature.money.importer

/** Synthetic statement files from tools/import-fixtures/make_fixtures.py; no real data. */
object ImportFixtures {
    fun bytes(name: String): ByteArray =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream("import/$name")) { "Missing fixture $name" }.use { it.readBytes() }

    fun parse(name: String): ParsedStatement = parseStatement(bytes(name))

    fun text(value: String?): Cell? = value?.let { Cell.Text(it) }
    fun num(value: Double): Cell = Cell.Number(value)

    private val maxHeader = listOf(
        "תאריך עסקה", "שם בית העסק", "קטגוריה", "4 ספרות אחרונות של כרטיס האשראי", "סוג עסקה", "סכום חיוב",
        "מטבע חיוב", "סכום עסקה מקורי", "מטבע עסקה מקורי", "תאריך חיוב", "הערות",
    )

    /** A Max sheet in memory: rows are (date, merchant, type, charge, original, chargeDate, notes). */
    fun maxSheet(vararg rows: List<String?>): Sheet = Sheet(
        "עסקאות במועד החיוב",
        listOf(listOf(text("רות כהן")), listOf(text("כרטיס - 1234")), listOf(text("11/2026")), maxHeader.map(::text)) +
            rows.map { r ->
                listOf(text(r[0]), text(r[1]), text("כללי"), text("1234"), text(r[2]), text(r[3]), text("₪"),
                    text(r[4] ?: r[3]), text("₪"), text(r[5]), text(r[6]))
            },
    )

    fun maxRow(date: String, merchant: String, charge: String, type: String = "רגילה", chargeDate: String? = "02-11-2026",
               original: String? = null, notes: String? = null) = listOf(date, merchant, type, charge, original, chargeDate, notes)
}
