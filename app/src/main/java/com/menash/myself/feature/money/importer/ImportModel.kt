package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.InstallmentInfo
import java.time.LocalDate
import java.time.YearMonth

enum class Source(val key: String) { HAPOALIM("hapoalim"), ONE_ZERO("onezero"), MAX("max"), CAL("cal") }

/** COUNTED rows feed plan vs actual. PENDING (not yet charged) and WAITING_FX (no ILS amount) never do. */
enum class RowStatus { COUNTED, PENDING, WAITING_FX }

/**
 * One statement line as the bank or card company exported it, with nothing personal in it: no holder
 * name, ID number or account number (long digit runs are masked), only the last 4 digits in [sourceId].
 * charge is ILS, positive = money out, negative = money in or a refund; null while WAITING_FX.
 * [id] is derived from the content only (see [ImportIds]), so the same line always gets the same id.
 */
data class StatementRow(
    val id: String,
    val source: Source,
    val sourceId: String,
    val date: LocalDate,
    val chargeDate: LocalDate?,
    val description: String,
    val details: String? = null,
    val charge: Money?,
    val originalAmount: Money? = null,
    val originalCurrency: String = "ILS",
    val status: RowStatus = RowStatus.COUNTED,
    val installment: InstallmentInfo? = null,
    /** The card company's own category (Max) or the bank's operation type: a hint, never the answer. */
    val hint: String? = null,
    /** סוג עסקה / הפעולה / סוג פעולה as printed. */
    val operation: String? = null,
) {
    /** Paamonim counts a purchase in the month it is charged. */
    val billingMonth: YearMonth get() = YearMonth.from(chargeDate ?: date)
}

/** A stored row plus what the user chose. Re-imports replace [row] and keep [categoryId]. */
data class StoredTransaction(val row: StatementRow, val categoryId: String? = null) {
    val id: String get() = row.id
}

enum class SkipReason { BAD_DATE, BAD_AMOUNT, NO_AMOUNT }

sealed class ImportWarning {
    /** rowNumber is the Excel row number (1-based) so the user can find it in the file. */
    data class SkippedRow(val rowNumber: Int, val reason: SkipReason) : ImportWarning()
    /** The running balance does not follow from the amounts at this row: a line may be missing. */
    data class BalanceBreak(val rowNumber: Int) : ImportWarning()
}

/**
 * One parsed file. statementTotal is the file's own total line (Max, Cal); [checkedRows] are the rows that
 * total covers (Cal leaves out FX and pending rows). Bank files have no total and are checked by balance.
 */
data class ParsedStatement(
    val source: Source,
    val rows: List<StatementRow>,
    val statementTotal: Money? = null,
    val warnings: List<ImportWarning> = emptyList(),
) {
    val firstDate: LocalDate? get() = rows.minOfOrNull { it.date }
    val lastDate: LocalDate? get() = rows.maxOfOrNull { it.date }
    /** Cal's total line leaves out foreign-currency and pending purchases, so the check does too. */
    val checkedRows: List<StatementRow> get() = rows.filter {
        it.status == RowStatus.COUNTED && (source != Source.CAL || it.originalCurrency == "ILS")
    }
}
