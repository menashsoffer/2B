package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** "Update the month": the close checks of the oldest open month first, then this month, then maaser. */
class MonthTasksTest {
    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 8)
    private val bank = Asset("asset-bank", "עו\"ש", "bank", Money(3_000_000), AssetKind.ASSET, true, LocalDate.of(2026, 10, 30))
    private val data = MoneyData(householdReady = true, assets = listOf(bank), categories = defaultCategories())

    private var n = 0
    private fun line(date: LocalDate, category: String? = "current-1", card: String = "1234") = StoredTransaction(
        StatementRow(id = "row-${n++}", source = Source.MAX, sourceId = card, date = date, chargeDate = date,
            description = "שופרסל $n", charge = Money(10_000), status = RowStatus.COUNTED),
        categoryId = category,
    )

    private fun tasks(stored: List<StoredTransaction>, d: MoneyData = data, tithe: Money? = null,
                      closures: Map<StatementSource, YearMonth> = emptyMap()) =
        monthTasks(d, today, null, stored, ImportMemory(), closures, tithe)

    private fun closed(month: YearMonth) =
        ClosedMonth(month, MonthSnapshot(month, Money.ZERO, Money.ZERO), emptyMap(), emptyMap(), emptyMap(), emptyMap(), today)

    @Test fun nothingToDoIsAnEmptyList() {
        assertEquals(emptyList<MonthTask>(), tasks(emptyList()))
    }

    @Test fun aReadyMonthAsksToBeClosed() {
        assertEquals(listOf(MonthTask.Close(october)), tasks(listOf(line(october.atDay(10)))))
    }

    @Test fun blockingChecksComeBeforeTheCloseAndReplaceIt() {
        val stored = listOf(line(october.atDay(10), category = null), line(october.atDay(11), category = null))
        assertEquals(listOf(MonthTask.Assign(october, 2)), tasks(stored))
    }

    @Test fun aMissingCardFileIsAStep() {
        val stored = listOf(line(YearMonth.of(2026, 9).atDay(5), card = "9999"), line(october.atDay(10)))
        val d = data.copy(closedMonths = listOf(closed(YearMonth.of(2026, 9))))
        assertEquals(listOf(MonthTask.MissingFile(october, StatementSource(Source.MAX, "9999"))), tasks(stored, d))
    }

    @Test fun aClosedCardIsNotAskedFor() {
        val stored = listOf(line(YearMonth.of(2026, 9).atDay(5), card = "9999"), line(october.atDay(10)))
        val d = data.copy(closedMonths = listOf(closed(YearMonth.of(2026, 9))))
        val result = tasks(stored, d, closures = mapOf(StatementSource(Source.MAX, "9999") to october))
        assertEquals(listOf(MonthTask.Close(october)), result)
    }

    @Test fun aStaleBalanceIsAStep() {
        val old = bank.copy(lastUpdated = LocalDate.of(2026, 9, 1))
        val result = tasks(listOf(line(october.atDay(10))), data.copy(assets = listOf(old)))
        assertEquals(listOf(MonthTask.UpdateBalance(october, old)), result)
    }

    @Test fun onlyTheOldestOpenMonthIsListed() {
        val stored = listOf(line(YearMonth.of(2026, 9).atDay(10)), line(october.atDay(10)))
        assertEquals(listOf(MonthTask.Close(YearMonth.of(2026, 9))), tasks(stored))
        val afterSeptember = data.copy(closedMonths = listOf(closed(YearMonth.of(2026, 9))))
        assertEquals(listOf(MonthTask.Close(october)), tasks(stored, afterSeptember))
    }

    @Test fun thisMonthsUnassignedLinesFollowTheClose() {
        val stored = listOf(line(october.atDay(10)), line(november.atDay(2), category = null))
        assertEquals(listOf(MonthTask.Close(october), MonthTask.Assign(november, 1)), tasks(stored))
    }

    @Test fun maaserOwedIsTheLastStep() {
        val owed = Money(25_000)
        assertEquals(listOf(MonthTask.Tithe(owed)), tasks(emptyList(), tithe = owed))
        assertEquals(emptyList<MonthTask>(), tasks(emptyList(), tithe = Money.ZERO))
        assertEquals(emptyList<MonthTask>(), tasks(emptyList(), tithe = Money(-5_000)))
    }
}
