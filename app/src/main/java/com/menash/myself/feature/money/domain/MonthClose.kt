package com.menash.myself.feature.money.domain

import java.time.LocalDate
import java.time.YearMonth

data class CloseInput(
    val month: YearMonth,
    val today: LocalDate,
    val sourcesThisMonth: Set<String>,
    val sourcesExpected: Set<String>,
    val unassignedCount: Int,
    val pendingCount: Int,
    val assets: List<Asset>,
)

enum class CloseCheckKind { MONTH_NOT_OVER, MISSING_SOURCE, UNASSIGNED, PENDING, STALE_BALANCE }

/** detail is the month, missing source key, line count, or stale asset id. */
data class CloseCheck(val kind: CloseCheckKind, val blocking: Boolean, val detail: String)

/** Closing is allowed only after month-end. The exact seven-day balance boundary is fresh. */
fun closeChecks(input: CloseInput): List<CloseCheck> = buildList {
    if (input.today <= input.month.atEndOfMonth()) {
        add(CloseCheck(CloseCheckKind.MONTH_NOT_OVER, true, input.month.toString()))
    }
    (input.sourcesExpected - input.sourcesThisMonth).sorted().forEach {
        add(CloseCheck(CloseCheckKind.MISSING_SOURCE, true, it))
    }
    if (input.unassignedCount > 0) {
        add(CloseCheck(CloseCheckKind.UNASSIGNED, true, input.unassignedCount.toString()))
    }
    if (input.pendingCount > 0) {
        add(CloseCheck(CloseCheckKind.PENDING, false, input.pendingCount.toString()))
    }
    val freshFrom = input.month.atEndOfMonth().minusDays(7)
    input.assets.filter { it.lastUpdated < freshFrom }.sortedBy { it.id }.forEach {
        add(CloseCheck(CloseCheckKind.STALE_BALANCE, true, it.id))
    }
}

fun canClose(checks: List<CloseCheck>): Boolean = checks.none { it.blocking }
