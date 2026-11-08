package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** One computation for the home screen and the month tab: headline, steps and freshness. */
class MoneyOverviewTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val max = StatementSource(Source.MAX, "1234")
    private val bank = StatementSource(Source.HAPOALIM, "5678")
    private var n = 0
    private fun line(source: StatementSource, date: LocalDate, category: String? = "current-1") = StoredTransaction(
        StatementRow(id = "r-${n++}", source = source.source, sourceId = source.sourceId, date = date, chargeDate = date,
            description = "שורה $n", charge = Money(10_000), status = RowStatus.COUNTED),
        categoryId = category,
    )

    @Test fun freshnessIsTheOldestSourceNotTheNewestLine() {
        val stored = listOf(line(max, LocalDate.of(2026, 11, 5)), line(bank, LocalDate.of(2026, 10, 20)), line(bank, LocalDate.of(2026, 10, 2)))
        assertEquals(SourceFreshness(bank, LocalDate.of(2026, 10, 20)), oldestSource(stored, today))
    }

    @Test fun sourcesUnusedForThreeMonthsOrClosedDoNotCount() {
        val stored = listOf(line(max, LocalDate.of(2026, 11, 5)), line(bank, LocalDate.of(2026, 6, 1)))
        assertEquals(SourceFreshness(max, LocalDate.of(2026, 11, 5)), oldestSource(stored, today))
        val recent = listOf(line(max, LocalDate.of(2026, 11, 5)), line(bank, LocalDate.of(2026, 10, 1)))
        assertEquals(max, oldestSource(recent, today, mapOf(bank to YearMonth.of(2026, 11)))?.source)
        assertNull(oldestSource(emptyList(), today))
    }

    @Test fun headlineLeadsWithLeftForCurrentOnceThisMonthHasLines() {
        val stored = listOf(line(max, LocalDate.of(2026, 11, 3)))
        val overview = moneyOverview(MoneySamples.filled, today, MoneySamples.rate, stored, ImportMemory())
        val open = overview.headline as MonthStatus.Open
        assertEquals(Money(10_000), open.currentSpent)
        assertEquals(Money(open.currentBudget.agorot - 10_000), open.leftForCurrent)
        assertEquals(SourceFreshness(max, LocalDate.of(2026, 11, 3)), overview.oldestSource)
    }

    @Test fun headlineIsPlanOnlyWithoutLinesAndNullWithoutPlan() {
        assertTrue(moneyOverview(MoneySamples.filled, today, MoneySamples.rate, emptyList(), ImportMemory()).headline is MonthStatus.PlanOnly)
        assertNull(moneyOverview(MoneySamples.empty, today, MoneySamples.rate, emptyList(), ImportMemory()).headline)
    }

    @Test fun stepsAreTheSameAsMonthTasks() {
        val stored = listOf(line(max, LocalDate.of(2026, 10, 3)), line(max, LocalDate.of(2026, 11, 3), category = null))
        val overview = moneyOverview(MoneySamples.filled, today, MoneySamples.rate, stored, ImportMemory())
        val now = buildMoneyMonth(MoneySamples.filled, YearMonth.from(today), today, MoneySamples.rate, stored, ImportMemory())
        assertEquals(monthTasks(MoneySamples.filled, today, MoneySamples.rate, stored, ImportMemory(), emptyMap(), now.bigPicture.titheBalance), overview.tasks)
        assertTrue(MonthTask.Assign(YearMonth.of(2026, 11), 1) in overview.tasks)
    }

    @Test fun inboxHandsATaskOverOnce() {
        val inbox = MonthTaskInbox()
        assertNull(inbox.take())
        inbox.offer(MonthTask.Close(YearMonth.of(2026, 10)))
        assertEquals(MonthTask.Close(YearMonth.of(2026, 10)), inbox.pending.value)
        assertEquals(MonthTask.Close(YearMonth.of(2026, 10)), inbox.take())
        assertNull(inbox.take())
    }
}
