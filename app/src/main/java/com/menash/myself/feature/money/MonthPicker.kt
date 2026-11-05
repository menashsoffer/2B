package com.menash.myself.feature.money

import com.menash.myself.feature.money.importer.StoredTransaction
import java.time.YearMonth

enum class MonthCellState { CLOSED, IMPORTED, EMPTY }
data class MonthPickerCell(val month: YearMonth, val state: MonthCellState, val isCurrent: Boolean)

/** Keep at least the previous year, and extend back to the oldest imported or closed month. */
fun earliestMonth(current: YearMonth, stored: List<StoredTransaction>, closed: Set<YearMonth>): YearMonth =
    (closed + stored.map { it.row.billingMonth } + current.minusMonths(12)).filter { it <= current }.minOrNull()!!

internal fun pickerMonths(current: YearMonth, closed: Set<YearMonth>, lines: List<StoredTransaction>): List<MonthPickerCell> {
    val imported = lines.map { it.row.billingMonth }.toSet()
    val earliest = earliestMonth(current, lines, closed)
    val count = java.time.temporal.ChronoUnit.MONTHS.between(earliest, current)
    return (0L..count).map { offset ->
        val month = current.minusMonths(offset)
        MonthPickerCell(month, when (month) {
            in closed -> MonthCellState.CLOSED
            in imported -> MonthCellState.IMPORTED
            else -> MonthCellState.EMPTY
        }, month == current)
    }
}
