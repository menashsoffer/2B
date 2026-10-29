package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import java.time.LocalDate
import java.time.YearMonth

private fun negateIfRefund(charge: Money, type: String): Money =
    if (type.contains("זיכוי")) Money(-Math.abs(charge.agorot)) else charge

/**
 * Card lines share one content key: card, purchase date, merchant, the purchase amount (the original
 * amount for foreign currency) and the payment number. The charge date stays out, so a pending line and
 * the same line once charged are one transaction. An installment line whose number cannot be read keeps
 * its charge month in the key instead, so monthly payments never merge.
 */
private fun cardKey(
    source: Source, sourceId: String, date: LocalDate, merchant: String, basis: Money?, currency: String,
    installment: com.menash.myself.feature.money.domain.InstallmentInfo?, type: String, chargeMonth: YearMonth?,
): List<String> = listOf(
    source.key, sourceId, date.toString(), ImportIds.merchantKey(merchant), basis.keyText(), currency,
    installment?.let { "${it.number}/${it.of}" } ?: if (type.contains("תשלומים")) "m:${chargeMonth ?: ""}" else "",
)

/** First amount in the row other than the label cell; Max puts its total one row below the label. */
private fun totalNear(sheet: Sheet, r: Int, labelCol: Int): Money? {
    fun scan(row: Int): Money? = sheet.rows.getOrNull(row)?.withIndex()
        ?.filter { it.index != labelCol || row != r }
        ?.firstNotNullOfOrNull { (amountOf(it.value) as? AmountCell.Value)?.money }
    return scan(r) ?: scan(r + 1)
}

private fun footerAt(cells: List<Cell?>, label: String): Int? =
    cells.indexOfFirst { (it as? Cell.Text)?.let { t -> headerKey(t.value).startsWith(headerKey(label)) } == true }.takeIf { it >= 0 }

internal object MaxParser {
    private val columns = mapOf(
        "date" to listOf("תאריך עסקה"), "merchant" to listOf("שם בית העסק"), "category" to listOf("קטגוריה"),
        "last4" to listOf("4 ספרות אחרונות"), "type" to listOf("סוג עסקה"), "charge" to listOf("סכום חיוב"),
        "chargeCurrency" to listOf("מטבע חיוב"), "original" to listOf("סכום עסקה מקורי"),
        "originalCurrency" to listOf("מטבע עסקה מקורי"), "chargeDate" to listOf("תאריך חיוב"), "notes" to listOf("הערות"),
    )
    private val required = setOf("date", "merchant", "charge", "chargeDate")

    fun matches(sheet: Sheet): Boolean = findColumns(sheet, columns, required) != null

    fun parse(sheet: Sheet): ParsedStatement {
        val (header, col) = findColumns(sheet, columns, required)!!
        val drafts = mutableListOf<Draft>()
        val warnings = mutableListOf<ImportWarning>()
        var total: Money? = null
        for (r in header + 1 until sheet.rows.size) {
            val cells = sheet.rows[r]
            fun cell(name: String) = col[name]?.let { cells.getOrNull(it) }
            footerAt(cells, "סך הכל")?.let { labelCol -> total = totalNear(sheet, r, labelCol); break }
            if (cells.all { it.text() == null }) continue
            val rowNumber = r + 1
            val date = dateOf(cell("date")) ?: run { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_DATE); null } ?: continue
            val type = cell("type").text().orEmpty()
            val notes = cell("notes").text()
            val chargeCurrency = currencyCode(cell("chargeCurrency").text()) ?: "ILS"
            val rawCharge = when (val a = amountOf(cell("charge"))) {
                is AmountCell.Value -> negateIfRefund(a.money, type)
                AmountCell.Blank -> null
                AmountCell.Bad -> { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_AMOUNT); continue }
            }
            val original = (amountOf(cell("original")) as? AmountCell.Value)?.money?.let { negateIfRefund(it, type) }
            val originalCurrency = currencyCode(cell("originalCurrency").text()) ?: chargeCurrency
            if (rawCharge == null && original == null) { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.NO_AMOUNT); continue }
            val chargeDate = dateOf(cell("chargeDate"))
            val ils = chargeCurrency == "ILS" && rawCharge != null
            val installment = installmentOf(type, notes)
            val merchant = maskDigits(cell("merchant").text().orEmpty())
            val last4 = cell("last4").text()?.filter(Char::isDigit)?.takeLast(4).orEmpty()
            val sourceId = if (last4.isEmpty()) "max" else "max-$last4"
            val basis = original ?: rawCharge
            val basisCurrency = if (original != null) originalCurrency else chargeCurrency
            drafts += Draft(
                StatementRow(
                    "", Source.MAX, sourceId, date, chargeDate, merchant, notes?.let(::maskDigits),
                    charge = if (ils) rawCharge else null,
                    originalAmount = if (basisCurrency != "ILS") basis else null,
                    originalCurrency = if (basisCurrency != "ILS") basisCurrency else "ILS",
                    status = when { chargeDate == null -> RowStatus.PENDING; !ils -> RowStatus.WAITING_FX; else -> RowStatus.COUNTED },
                    installment = installment, hint = cell("category").text(), operation = type,
                ),
                cardKey(Source.MAX, sourceId, date, merchant, basis, basisCurrency, installment, type, chargeDate?.let(YearMonth::from)),
                rowNumber,
            )
        }
        return ParsedStatement(Source.MAX, withIds(Source.MAX.key, drafts), total, warnings)
    }
}

internal object CalParser {
    private val columns = mapOf(
        "date" to listOf("תאריך עסקה"), "merchant" to listOf("שם בית עסק"), "charge" to listOf("סכום בש\"ח", "סכום"),
        "chargeDate" to listOf("מועד חיוב"), "type" to listOf("סוג עסקה"), "notes" to listOf("הערות"),
    )
    private val required = setOf("date", "merchant", "charge", "chargeDate")
    private val fxNote = Regex("סכום העסקה הוא\\s*([\\d.,]+)\\s*([$€£]|USD|EUR|GBP)", RegexOption.IGNORE_CASE)
    private val cardDigits = Regex("(?:המסתיים|מסתיים|כרטיס)\\D{0,12}(\\d{4})(?!\\d)")

    fun matches(sheet: Sheet): Boolean = findColumns(sheet, columns, required) != null

    fun parse(sheet: Sheet): ParsedStatement {
        val (header, col) = findColumns(sheet, columns, required)!!
        val last4 = (0 until header).flatMap { sheet.rows[it].mapNotNull { c -> c.text() } }
            .firstNotNullOfOrNull { cardDigits.find(it)?.groupValues?.get(1) }
        val sourceId = if (last4 == null) "cal" else "cal-$last4"
        val drafts = mutableListOf<Draft>()
        val warnings = mutableListOf<ImportWarning>()
        var total: Money? = null
        for (r in header + 1 until sheet.rows.size) {
            val cells = sheet.rows[r]
            fun cell(name: String) = col[name]?.let { cells.getOrNull(it) }
            footerAt(cells, "סה\"כ")?.let { labelCol -> total = totalNear(sheet, r, labelCol); break }
            if (cells.all { it.text() == null }) continue
            val rowNumber = r + 1
            val date = dateOf(cell("date")) ?: run { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_DATE); null } ?: continue
            val type = cell("type").text().orEmpty()
            val notes = cell("notes").text()
            val fx = notes?.let { fxNote.find(it) }?.let { m ->
                val value = (amountOf(Cell.Text(m.groupValues[1])) as? AmountCell.Value)?.money
                value?.let { negateIfRefund(it, type) to currencyCode(m.groupValues[2])!! }
            }
            val charge = when (val a = amountOf(cell("charge"))) {
                is AmountCell.Value -> negateIfRefund(a.money, type)
                AmountCell.Blank -> null
                AmountCell.Bad -> { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.BAD_AMOUNT); continue }
            }
            if (charge == null && fx == null) { warnings += ImportWarning.SkippedRow(rowNumber, SkipReason.NO_AMOUNT); continue }
            val chargeDate = dateOf(cell("chargeDate"))
            val pending = chargeDate == null || notes?.contains("עסקה בקליטה") == true
            val installment = installmentOf(type, notes)
            val merchant = maskDigits(cell("merchant").text().orEmpty())
            drafts += Draft(
                StatementRow(
                    "", Source.CAL, sourceId, date, chargeDate, merchant, notes?.let(::maskDigits), charge,
                    originalAmount = fx?.first, originalCurrency = fx?.second ?: "ILS",
                    status = when { pending -> RowStatus.PENDING; charge == null -> RowStatus.WAITING_FX; else -> RowStatus.COUNTED },
                    installment = installment, operation = type,
                ),
                cardKey(Source.CAL, sourceId, date, merchant, fx?.first ?: charge, fx?.second ?: "ILS", installment, type,
                    chargeDate?.let(YearMonth::from)),
                rowNumber,
            )
        }
        return ParsedStatement(Source.CAL, withIds(Source.CAL.key, drafts), total, warnings)
    }
}
