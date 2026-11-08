package com.menash.myself.feature.money

import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.StoredTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** The latest imported line of one bank account or card. */
data class SourceFreshness(val source: StatementSource, val lastLine: LocalDate)

/**
 * What the home screen and the top of the month tab both show, from one computation so they never disagree:
 * this month's headline, the steps to do, and how fresh the imported data is.
 */
data class MoneyOverview(
    /** Null while this month has no plan. */
    val headline: MonthStatus?,
    val tasks: List<MonthTask>,
    /** The source whose latest line is oldest, among those still in use. Null with nothing imported. */
    val oldestSource: SourceFreshness?,
)

/** [now] is today's month as the month tab builds it; pass it to avoid building it twice. */
fun moneyOverview(
    data: MoneyData,
    today: LocalDate,
    usdToIls: BigDecimal?,
    stored: List<StoredTransaction>,
    memory: ImportMemory,
    sourceClosures: Map<StatementSource, YearMonth> = emptyMap(),
    now: MoneyMonth = buildMoneyMonth(data, YearMonth.from(today), today, usdToIls, stored, memory),
): MoneyOverview = MoneyOverview(
    headline = (now.view as? MonthView.Planned)?.status,
    tasks = monthTasks(data, today, usdToIls, stored, memory, sourceClosures, now.bigPicture.titheBalance),
    oldestSource = oldestSource(stored, today, sourceClosures),
)

/**
 * A fresh card file can hide a bank file weeks old, so freshness is the oldest source, not the newest line.
 * Only sources with a line in the last three months count, and a source closed by the household does not.
 */
fun oldestSource(stored: List<StoredTransaction>, today: LocalDate, sourceClosures: Map<StatementSource, YearMonth> = emptyMap()): SourceFreshness? {
    val since = YearMonth.from(today).minusMonths(3).atDay(1)
    return stored.groupBy { StatementSource(it.row.source, it.row.sourceId) }
        .filterKeys { source -> sourceClosures[source]?.let { it > YearMonth.from(today) } ?: true }
        .mapValues { (_, lines) -> lines.maxOf { it.row.date } }
        .filterValues { it >= since }
        .minByOrNull { it.value }
        ?.let { SourceFreshness(it.key, it.value) }
}

/** A step tapped outside the money tab (on the home screen), waiting for the money tab to open its screen. */
class MonthTaskInbox {
    private val _pending = MutableStateFlow<MonthTask?>(null)
    val pending: StateFlow<MonthTask?> = _pending.asStateFlow()

    fun offer(task: MonthTask) { _pending.value = task }

    /** Returns the waiting step and clears it, so it opens once. */
    fun take(): MonthTask? = _pending.value.also { _pending.value = null }
}
