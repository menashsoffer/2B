package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MonthCloseChecksTest {
    private val month = YearMonth.of(2026, 10)
    private val clear = CloseInput(month, LocalDate.of(2026, 11, 1), setOf("bank"), setOf("bank"), 0, 0, emptyList())
    private fun asset(id: String, date: LocalDate) =
        Asset(id, "private name", "bank", Money(100), AssetKind.ASSET, true, date)

    @Test fun allClear() {
        assertEquals(emptyList<CloseCheck>(), closeChecks(clear))
        assertTrue(canClose(closeChecks(clear)))
    }

    @Test fun monthNotOverBlocksAlone() {
        val checks = closeChecks(clear.copy(today = month.atDay(1)))
        assertEquals(listOf(CloseCheck(CloseCheckKind.MONTH_NOT_OVER, true, "2026-10")), checks)
        assertFalse(canClose(checks))
    }

    @Test fun monthEndIsBlockedAndFollowingDayIsClearIncludingLeapFebruary() {
        for (m in listOf(month, YearMonth.of(2024, 2), YearMonth.of(2026, 4))) {
            assertFalse(canClose(closeChecks(clear.copy(month = m, today = m.atEndOfMonth()))))
            assertTrue(canClose(closeChecks(clear.copy(month = m, today = m.atEndOfMonth().plusDays(1)))))
        }
    }

    @Test fun missingSourceBlocksAlone() {
        val checks = closeChecks(clear.copy(sourcesThisMonth = emptySet()))
        assertEquals(listOf(CloseCheck(CloseCheckKind.MISSING_SOURCE, true, "bank")), checks)
        assertFalse(canClose(checks))
    }

    @Test fun unassignedBlocksAlone() {
        val checks = closeChecks(clear.copy(unassignedCount = 2))
        assertEquals(listOf(CloseCheck(CloseCheckKind.UNASSIGNED, true, "2")), checks)
        assertFalse(canClose(checks))
    }

    @Test fun pendingAloneWarnsButAllowsClosure() {
        val checks = closeChecks(clear.copy(pendingCount = 3))
        assertEquals(listOf(CloseCheck(CloseCheckKind.PENDING, false, "3")), checks)
        assertTrue(canClose(checks))
    }

    @Test fun staleBalanceBlocksAlone() {
        val checks = closeChecks(clear.copy(assets = listOf(asset("a", month.atEndOfMonth().minusDays(8)))))
        assertEquals(listOf(CloseCheck(CloseCheckKind.STALE_BALANCE, true, "a")), checks)
        assertFalse(canClose(checks))
    }

    @Test fun exactSevenDayBoundaryAndLaterUpdatesAreFresh() {
        val dates = listOf(month.atEndOfMonth().minusDays(7), month.atEndOfMonth(), clear.today.plusDays(1))
        dates.forEach { assertTrue(canClose(closeChecks(clear.copy(assets = listOf(asset("a", it)))))) }
    }

    @Test fun allKindsAndMultipleSourcesAndAssetsAreDeterministic() {
        val input = clear.copy(
            today = month.atDay(1), sourcesExpected = linkedSetOf("z", "bank", "a"),
            unassignedCount = 1, pendingCount = 2,
            assets = listOf(asset("z", month.atDay(1)), asset("a", month.atDay(1))),
        )
        val expected = listOf(
            CloseCheck(CloseCheckKind.MONTH_NOT_OVER, true, "2026-10"),
            CloseCheck(CloseCheckKind.MISSING_SOURCE, true, "a"),
            CloseCheck(CloseCheckKind.MISSING_SOURCE, true, "z"),
            CloseCheck(CloseCheckKind.UNASSIGNED, true, "1"),
            CloseCheck(CloseCheckKind.PENDING, false, "2"),
            CloseCheck(CloseCheckKind.STALE_BALANCE, true, "a"),
            CloseCheck(CloseCheckKind.STALE_BALANCE, true, "z"),
        )
        assertEquals(expected, closeChecks(input))
        assertEquals(expected, closeChecks(input.copy(
            sourcesExpected = input.sourcesExpected.reversed().toSet(), assets = input.assets.reversed(),
        )))
    }
}
