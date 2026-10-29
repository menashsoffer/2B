package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money

/** A row before its id: the parsed line, its content key and its Excel row number. */
internal data class Draft(val row: StatementRow, val key: List<String>, val rowNumber: Int)

internal fun withIds(prefix: String, drafts: List<Draft>): List<StatementRow> =
    ImportIds.assign(prefix, drafts.map { it.key }).zip(drafts) { id, d -> d.row.copy(id = id) }

internal fun Money?.keyText(): String = this?.agorot?.toString() ?: ""

/**
 * Bank files have no total line, so the running balance is checked instead, in whichever order the file
 * lists rows (oldest or newest first). inflow = −charge. Null when it holds or cannot be checked.
 */
internal fun balanceBreak(rows: List<Triple<Int, Money, Money?>>): ImportWarning.BalanceBreak? {
    val known = rows.filter { it.third != null }
    if (known.size < 2) return null
    fun firstBreak(list: List<Triple<Int, Money, Money?>>): Int? = (1 until list.size).firstOrNull { i ->
        list[i].third!!.agorot != list[i - 1].third!!.agorot + list[i].second.agorot
    }
    val topDown = firstBreak(known) ?: return null
    val bottomUp = firstBreak(known.reversed()) ?: return null
    // The order that holds longer is the file's order; its first break is where a line is missing.
    val at = if (topDown >= bottomUp) topDown else known.size - 1 - bottomUp
    return ImportWarning.BalanceBreak(known[at].first)
}

/** Last 4 digits of the account number printed above the header, the rest of it never leaves here. */
internal fun accountLast4(sheet: Sheet, headerRow: Int): String {
    for (r in 0 until headerRow) {
        val texts = sheet.rows[r].mapNotNull { it.text() }
        if (texts.none { it.contains("חשבון") || it.contains("סניף") }) continue
        val digits = texts.filter { dateOf(Cell.Text(it)) == null }
            .flatMap { Regex("\\d[\\d\\-/ ]*\\d").findAll(it).map { m -> m.value.filter(Char::isDigit) }.toList() }
            .filter { it.length >= 4 }
            .maxByOrNull { it.length }
        if (digits != null) return digits.takeLast(4)
    }
    return ""
}

internal object HapoalimParser {
    private val columns = mapOf(
        "date" to listOf("תאריך"), "operation" to listOf("הפעולה"), "details" to listOf("פרטים"),
        "reference" to listOf("אסמכתא"), "debit" to listOf("חובה"), "credit" to listOf("זכות"),
        "balance" to listOf("יתרה בש\"ח", "יתרה"), "beneficiary" to listOf("לטובת"), "purpose" to listOf("עבור"),
    )
    private val required = setOf("date", "operation", "reference", "debit", "credit")

    fun matches(sheet: Sheet): Boolean = findColumns(sheet, columns, required) != null

    fun parse(sheet: Sheet): ParsedStatement {
        val (header, col) = findColumns(sheet, columns, required)!!
        val last4 = accountLast4(sheet, header)
        val sourceId = if (last4.isEmpty()) "hapoalim" else "hapoalim-$last4"
        val drafts = mutableListOf<Draft>()
        val warnings = mutableListOf<ImportWarning>()
        val chain = mutableListOf<Triple<Int, Money, Money?>>()
        for (r in header + 1 until sheet.rows.size) {
            val cells = sheet.rows[r]
            fun cell(name: String) = col[name]?.let { cells.getOrNull(it) }
            if (cells.all { it.text() == null }) continue
            val rowNumber = r + 1
            val date = dateOf(cell("date")) ?: run { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_DATE); null } ?: continue
            val debit = amountOf(cell("debit"))
            val credit = amountOf(cell("credit"))
            if (debit is AmountCell.Bad || credit is AmountCell.Bad) { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_AMOUNT); continue }
            val charge = when {
                debit is AmountCell.Value && debit.money.agorot != 0L -> debit.money
                credit is AmountCell.Value -> Money(-credit.money.agorot)
                debit is AmountCell.Value -> debit.money
                else -> { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.NO_AMOUNT); continue }
            }
            val operation = maskDigits(cell("operation").text().orEmpty())
            val beneficiary = cell("beneficiary").text()?.let(::maskDigits)
            val details = listOfNotNull(cell("details").text(), cell("purpose").text()).joinToString(" · ").ifEmpty { null }?.let(::maskDigits)
            // "הוראת קבע" or "העברה" alone says nothing; who it is for tells merchants apart.
            val description = listOfNotNull(operation.ifEmpty { null }, beneficiary ?: details).joinToString(" ")
            val reference = cell("reference").text().orEmpty()
            val balance = (amountOf(cell("balance")) as? AmountCell.Value)?.money
            chain += Triple(rowNumber, Money(-charge.agorot), balance)
            drafts += Draft(
                StatementRow("", Source.HAPOALIM, sourceId, date, date, description, details, charge, operation = operation),
                listOf(Source.HAPOALIM.key, sourceId, date.toString(), reference, charge.keyText(), ImportIds.merchantKey(description)),
                rowNumber,
            )
        }
        balanceBreak(chain)?.let { warnings += it }
        return ParsedStatement(Source.HAPOALIM, withIds(Source.HAPOALIM.key, drafts), null, warnings)
    }
}

internal object OneZeroParser {
    private val columns = mapOf(
        "date" to listOf("תאריך תנועה"), "operation" to listOf("סוג פעולה"), "description" to listOf("תיאור"),
        "amount" to listOf("סכום פעולה"), "currency" to listOf("מטבע"), "direction" to listOf("חיוב/זיכוי"),
        "balance" to listOf("יתרה"), "reference" to listOf("אסמכתא"),
    )
    private val required = setOf("date", "operation", "description", "amount")

    fun matches(sheet: Sheet): Boolean = findColumns(sheet, columns, required) != null

    fun parse(sheet: Sheet): ParsedStatement {
        val (header, col) = findColumns(sheet, columns, required)!!
        val drafts = mutableListOf<Draft>()
        val warnings = mutableListOf<ImportWarning>()
        val chain = mutableListOf<Triple<Int, Money, Money?>>()
        var fileCurrency: String? = null
        for (r in header + 1 until sheet.rows.size) {
            val cells = sheet.rows[r]
            fun cell(name: String) = col[name]?.let { cells.getOrNull(it) }
            if (cells.all { it.text() == null }) continue
            val rowNumber = r + 1
            val dateCell = cell("date")
            val date = dateOf(dateCell) ?: run { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_DATE); null } ?: continue
            val amount = when (val a = amountOf(cell("amount"))) {
                is AmountCell.Value -> a.money
                AmountCell.Blank -> { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.NO_AMOUNT); continue }
                AmountCell.Bad -> { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_AMOUNT); continue }
            }
            val direction = cell("direction").text().orEmpty()
            val fileCharge = when {
                direction.contains("חיוב") -> Money(Math.abs(amount.agorot))
                direction.contains("זיכוי") -> Money(-Math.abs(amount.agorot))
                else -> Money(-amount.agorot)
            }
            val currency = currencyCode(cell("currency").text()) ?: fileCurrency ?: "ILS"
            fileCurrency = fileCurrency ?: currency
            val rawDescription = (cell("description") as? Cell.Text)?.value ?: cell("description").text().orEmpty()
            val description = maskDigits(fixReversedHebrew(rawDescription))
            val operation = cell("operation").text().orEmpty()
            val reference = cell("reference").text().orEmpty()
            val sourceId = "onezero-$currency"
            chain += Triple(rowNumber, Money(-fileCharge.agorot), (amountOf(cell("balance")) as? AmountCell.Value)?.money)
            val ils = currency == "ILS"
            drafts += Draft(
                StatementRow(
                    "", Source.ONE_ZERO, sourceId, date, date, description, null,
                    charge = if (ils) fileCharge else null,
                    originalAmount = if (ils) null else fileCharge, originalCurrency = currency,
                    status = if (ils) RowStatus.COUNTED else RowStatus.WAITING_FX, operation = operation,
                ),
                listOf(Source.ONE_ZERO.key, sourceId, reference, (dateCell as? Cell.Number)?.value?.toString() ?: dateCell.text().orEmpty(),
                    fileCharge.keyText(), ImportIds.merchantKey(description)),
                rowNumber,
            )
        }
        balanceBreak(chain)?.let { warnings += it }
        return ParsedStatement(Source.ONE_ZERO, withIds(Source.ONE_ZERO.key, drafts), null, warnings)
    }
}

internal fun currencyCode(text: String?): String? {
    val t = text?.let(::cleanText)?.uppercase() ?: return null
    return when {
        t.isEmpty() -> null
        t.contains("₪") || t.contains("ILS") || t.contains("שח") || t.contains("ש\"ח") || t.contains("ש״ח") || t.contains("NIS") -> "ILS"
        t.contains("$") || t.contains("USD") || t.contains("דולר") -> "USD"
        t.contains("€") || t.contains("EUR") || t.contains("אירו") -> "EUR"
        t.contains("£") || t.contains("GBP") -> "GBP"
        else -> t.take(3)
    }
}
