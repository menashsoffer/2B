package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.CloseCheckKind
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.importer.ImportMemory
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** The close screen asks the engine's closeChecks and shows the ClosedMonth it would store. */
class MonthCloseFlowTest {

    private val october = YearMonth.of(2026, 10)
    private val september = YearMonth.of(2026, 9)
    private val afterOctober = LocalDate.of(2026, 11, 2)
    private val bank = Asset("asset-bank", "עו\"ש", "bank", Money(3_000_000), AssetKind.ASSET, true, LocalDate.of(2026, 10, 30))
    private val data = MoneyData(householdReady = true, assets = listOf(bank), categories = defaultCategories())

    private var n = 0
    private fun line(
        date: LocalDate, agorot: Long, category: String? = "current-1", card: String = "1234",
        status: RowStatus = RowStatus.COUNTED,
    ) = StoredTransaction(
        StatementRow(
            id = "row-${n++}", source = Source.MAX, sourceId = card, date = date, chargeDate = date,
            description = "שופרסל $n", charge = Money(agorot), status = status,
        ),
        categoryId = category,
    )

    private fun view(stored: List<StoredTransaction>, today: LocalDate = afterOctober, noLines: Set<String> = emptySet(), d: MoneyData = data) =
        closeView(d, october, today, null, stored, ImportMemory(), noLines)

    @Test
    fun target_lastMonthWhileItHasLinesAndIsOpen() {
        val stored = listOf(line(LocalDate.of(2026, 10, 10), 10_000))
        assertEquals(october, closeTarget(LocalDate.of(2026, 11, 3), stored, emptyList()))
        val closed = ClosedMonth(october, MonthSnapshot(october, Money.ZERO, Money.ZERO), emptyMap(), emptyMap(), emptyMap(), emptyMap(), afterOctober)
        assertEquals(october, closeTarget(LocalDate.of(2026, 11, 3), stored, listOf(closed)))
        assertEquals(october, closeTarget(LocalDate.of(2026, 11, 3), emptyList(), emptyList()))
    }

    @Test
    fun target_selectsOldestEndedMonthWithLines() {
        val january = YearMonth.of(2024, 1)
        val stored = listOf(line(LocalDate.of(2026, 11, 1), 1), line(LocalDate.of(2024, 1, 10), 1), line(LocalDate.of(2026, 10, 10), 1))
        assertEquals(january, closeTarget(afterOctober, stored, emptyList()))
    }

    @Test
    fun allClear_canCloseAndPreviewSumsTheMonth() {
        val v = view(listOf(line(LocalDate.of(2026, 10, 5), 25_000), line(LocalDate.of(2026, 10, 20), 15_000)))
        assertTrue(v.checks.toString(), v.canClose)
        assertEquals(Money(40_000), v.preview.snapshot.actualExpenses)
        assertEquals(Money(40_000), v.preview.byCategory["current-1"])
        assertEquals(Money(3_000_000), v.preview.balances["asset-bank"])
        assertEquals(Money(-40_000), v.saving)
        assertNull(v.existing)
    }

    @Test
    fun monthNotOver_blocks() {
        val v = view(listOf(line(LocalDate.of(2026, 10, 5), 25_000)), today = LocalDate.of(2026, 10, 30))
        assertFalse(v.canClose)
        assertTrue(v.checks.any { it.kind == CloseCheckKind.MONTH_NOT_OVER })
    }

    @Test
    fun cardSeenLastMonthButNotThisMonth_isMissingUntilTheUserSaysItHasNoLines() {
        val stored = listOf(
            line(LocalDate.of(2026, 9, 12), 5_000, card = "9876"),
            line(LocalDate.of(2026, 10, 5), 25_000),
        )
        val missing = view(stored)
        assertEquals(listOf(StatementSource(Source.MAX, "9876")), missing.missingSources)
        assertFalse(missing.canClose)
        val acknowledged = view(stored, noLines = setOf("max:9876"))
        assertTrue(acknowledged.missingSources.isEmpty())
        assertTrue(acknowledged.canClose)
    }

    @Test
    fun sourceOlderThanThreeMonths_isNotExpected() {
        val stored = listOf(line(LocalDate.of(2026, 6, 12), 5_000, card = "9876"), line(LocalDate.of(2026, 10, 5), 25_000))
        assertTrue(view(stored).missingSources.isEmpty())
    }

    @Test
    fun unassignedLine_blocks_pendingLine_onlyWarns() {
        val unassigned = view(listOf(line(LocalDate.of(2026, 10, 5), 25_000, category = null)))
        assertFalse(unassigned.canClose)
        val check = unassigned.checks.single { it.kind == CloseCheckKind.UNASSIGNED }
        assertEquals("1", check.detail)

        val pending = view(listOf(line(LocalDate.of(2026, 10, 5), 25_000), line(LocalDate.of(2026, 10, 29), 9_000, status = RowStatus.PENDING)))
        assertTrue(pending.canClose)
        assertTrue(pending.checks.single { it.kind == CloseCheckKind.PENDING }.blocking.not())
        assertEquals(Money(25_000), pending.preview.snapshot.actualExpenses)
    }

    @Test
    fun staleBalance_namesTheAsset() {
        val old = bank.copy(id = "asset-old", lastUpdated = LocalDate.of(2026, 10, 1))
        val v = view(listOf(line(LocalDate.of(2026, 10, 5), 25_000)), d = data.copy(assets = listOf(bank, old)))
        assertEquals(listOf(old), v.staleAssets)
        assertFalse(v.canClose)
    }

    @Test
    fun monthBeforeLast_doesNotUseTodaysBalances() {
        val old = bank.copy(id = "asset-old", lastUpdated = LocalDate.of(2026, 10, 1))
        val stored = listOf(line(LocalDate.of(2026, 9, 5), 25_000))
        val v = closeView(data.copy(assets = listOf(bank, old)), september, afterOctober, null, stored, ImportMemory())
        assertTrue(v.staleAssets.isEmpty())
        assertTrue(v.canClose)
        assertTrue(v.preview.balances.isEmpty())
        assertFalse(v.preview.balancesEntered)
    }

    @Test
    fun existingCloseIsReturned() {
        val stored = ClosedMonth(october, MonthSnapshot(october, Money(1), Money(1)), emptyMap(), emptyMap(), emptyMap(), emptyMap(), afterOctober)
        val v = view(listOf(line(LocalDate.of(2026, 10, 5), 25_000)), d = data.copy(closedMonths = listOf(stored)))
        assertEquals(stored, v.existing)
    }

    @Test
    fun closedFigures_nullUntilAMonthIsClosed() {
        assertEquals(ClosedFigures(null, null), closedFigures(emptyList()))
        fun closed(month: YearMonth, income: Long, spent: Long) =
            ClosedMonth(month, MonthSnapshot(month, Money(income), Money(spent)), emptyMap(), emptyMap(), emptyMap(), emptyMap(), afterOctober)
        val figures = closedFigures(listOf(closed(september, 1_000_000, 800_000), closed(october, 1_200_000, 900_000)))
        assertEquals(Money(250_000), figures.monthlySaving)
        assertEquals(Money(850_000), figures.averageExpenses)
    }
}
