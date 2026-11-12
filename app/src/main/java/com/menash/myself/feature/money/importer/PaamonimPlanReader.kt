package com.menash.myself.feature.money.importer

import com.menash.myself.core.domain.model.Money
import java.time.Month
import kotlin.math.roundToLong

/** The four parts of a Paamonim budget list, in the order the sheet lists them. */
enum class PlanGroup { FIXED, PERIODIC, CURRENT, INCOME }

/** One named budget line. [monthly] is the sheet's figure; [annual] is it times 12, rounded once from the sheet's value. */
data class PaamonimLine(val group: PlanGroup, val name: String, val monthly: Money, val annual: Money)

/**
 * The plan of a Paamonim רישומון: the budget of the latest month that has one.
 * [debtRepayment] is the sheet's monthly total for debts; debts themselves are not read here.
 */
data class PaamonimPlan(val month: Month, val lines: List<PaamonimLine>, val debtRepayment: Money?)

/**
 * Reads the "רשימת תקציבים" sheet of a Paamonim workbook. The sheet is found by its header row (a first cell
 * starting with "סעיף" and Hebrew month names after it), never by the file name. Returns null for any other file.
 *
 * Layout: column A names the group on its first row and the totals rows; column B names the line; one column
 * per month. An unused line has the number 0 instead of a name and is skipped, as is a line with no budget.
 */
fun readPaamonimPlan(workbook: Workbook): PaamonimPlan? {
    for (sheet in workbook.sheets) {
        val header = sheet.rows.indexOfFirst { row -> text(row.getOrNull(0))?.let { headerKey(it).startsWith("סעיף") } == true }
        if (header < 0) continue
        val monthColumns = sheet.rows[header].mapIndexedNotNull { col, cell -> monthOf(text(cell))?.let { it to col } }.toMap()
        if (monthColumns.size < 12) continue
        val rows = sheet.rows.drop(header + 1)
        val named = mutableListOf<Triple<PlanGroup, String, List<Cell?>>>()
        var group: PlanGroup? = null
        var debtRow: List<Cell?>? = null
        for (row in rows) {
            val label = text(row.getOrNull(0))?.let(::headerKey)
            when {
                label == null -> Unit
                label.startsWith("סהכ") || label.startsWith("הפרש") -> group = null
                label.startsWith("החזר חובות") -> { debtRow = row; group = null }
                else -> groupOf(label)?.let { group = it }
            }
            val current = group ?: continue
            val name = text(row.getOrNull(1))?.let(::cleanText)?.takeIf { it.isNotEmpty() } ?: continue
            named += Triple(current, name, row)
        }
        // The latest month with any budget: a household that budgeted only January still gets January.
        val (month, col) = monthColumns.entries.sortedByDescending { it.key }
            .firstOrNull { (_, col) -> named.any { (_, _, row) -> number(row.getOrNull(col)) > 0.0 } }
            ?.let { it.key to it.value } ?: return PaamonimPlan(Month.JANUARY, emptyList(), null)
        val lines = named.mapNotNull { (g, name, row) ->
            val value = number(row.getOrNull(col)).takeIf { it > 0.0 } ?: return@mapNotNull null
            PaamonimLine(g, name, Money((value * 100).roundToLong()), Money((value * 1200).roundToLong()))
        }
        val debt = debtRow?.let { number(it.getOrNull(col)) }?.takeIf { it > 0.0 }?.let { Money((it * 100).roundToLong()) }
        return PaamonimPlan(month, lines, debt)
    }
    return null
}

private val hebrewMonths = listOf("ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר")

private fun monthOf(text: String?): Month? = text?.let(::cleanText)?.let { hebrewMonths.indexOf(it) }?.takeIf { it >= 0 }?.let { Month.of(it + 1) }

private fun groupOf(label: String): PlanGroup? = when {
    "קבועות" in label -> PlanGroup.FIXED
    "תקופתיות" in label -> PlanGroup.PERIODIC
    "שוטפות" in label -> PlanGroup.CURRENT
    label.startsWith("הכנסות") -> PlanGroup.INCOME
    else -> null
}

private fun text(cell: Cell?): String? = (cell as? Cell.Text)?.value
private fun number(cell: Cell?): Double = (cell as? Cell.Number)?.value ?: 0.0
