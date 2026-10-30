package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PensionTest {
    private val today = LocalDate.of(2026, 10, 30)
    private fun fund(id: String = "a", balanceFee: String = "0.5", depositFee: String = "0") = PensionFund(
        id, FundKind.PENSION, "provider", Money(10000000), Money(12000000), Money(200000),
        BigDecimal(balanceFee), BigDecimal(depositFee), null, today,
    )
    @Test fun depositFeeInShekelsMakesLowerBalancePercentagePriciest() {
        val a = fund()
        val b = fund("b", "0.2", "2")
        assertEquals(Money(50000), fundSnapshot(a).yearlyFee)
        assertEquals(Money(68000), fundSnapshot(b).yearlyFee)
        val snapshot = pensionSnapshot(listOf(a, b), today)
        assertEquals(b, snapshot.priciest)
        assertEquals(Money(118000), snapshot.yearlyFee)
        assertEquals(Money(20000000), snapshot.value)
        assertEquals(Money(24000000), snapshot.deposits)
        assertEquals(Money(-4000000), snapshot.profit)
    }
    @Test fun negativeProfitAndZeroFees() {
        val f = fund(balanceFee = "0")
        assertEquals(Money(-2000000), fundSnapshot(f).profit)
        assertNull(pensionSnapshot(listOf(f), today).priciest)
        assertEquals(Money.ZERO, pensionSnapshot(emptyList(), today).yearlyFee)
    }
    @Test fun feeTiesUseId() {
        assertEquals("a", pensionSnapshot(listOf(fund("z"), fund("a")), today).priciest?.id)
    }
    @Test fun liquidationOnSixYearAnniversaryOnlyForHishtalmut() {
        val f = fund().copy(kind = FundKind.HISHTALMUT, openedOn = today.minusYears(6))
        assertEquals(today, fundSnapshot(f).liquidOn)
        assertTrue(pensionSnapshot(listOf(f), today.minusDays(1)).liquidNow.isEmpty())
        assertEquals(listOf(f), pensionSnapshot(listOf(f), today).liquidNow)
        assertNull(fundSnapshot(f.copy(openedOn = null)).liquidOn)
        for (kind in listOf(FundKind.PENSION, FundKind.GEMEL)) assertNull(fundSnapshot(f.copy(kind = kind)).liquidOn)
    }
    @Test fun staleStrictlyAfterFourCalendarMonths() {
        val f = fund().copy(lastUpdated = LocalDate.of(2026, 6, 30))
        assertTrue(pensionSnapshot(listOf(f), today).stale.isEmpty())
        assertEquals(listOf(f), pensionSnapshot(listOf(f), today.plusDays(1)).stale)
    }
    @Test fun roundOnceAfterCombiningFeeComponentsAndFunds() {
        val f = fund(balanceFee = "5", depositFee = "5").copy(value = Money(1), monthlyDeposit = Money(1))
        assertEquals(Money(1), fundSnapshot(f).yearlyFee)
        assertEquals(Money(1), pensionSnapshot(listOf(f, f.copy(id = "b")), today).yearlyFee)
    }
    @Test fun priciestUsesPublishedAgoraFeesIncludingZeroAndTies() {
        val a = fund(balanceFee = "1").copy(value = Money(1), monthlyDeposit = Money.ZERO)
        assertNull(pensionSnapshot(listOf(a), today).priciest)
        val b = a.copy(id = "b", value = Money(149))
        val c = a.copy(value = Money(100))
        assertEquals(c, pensionSnapshot(listOf(b, c), today).priciest)
    }
    @Test fun invalidInputsAndDuplicateIdsFail() {
        assertThrows(IllegalArgumentException::class.java) { fund("") }
        for (fee in listOf("-0.1", "10.01")) {
            assertThrows(IllegalArgumentException::class.java) { fund(balanceFee = fee) }
            assertThrows(IllegalArgumentException::class.java) { fund(depositFee = fee) }
        }
        assertThrows(IllegalArgumentException::class.java) { fund().copy(value = Money(-1)) }
        assertThrows(IllegalArgumentException::class.java) { fund().copy(totalDeposits = Money(-1)) }
        assertThrows(IllegalArgumentException::class.java) { fund().copy(monthlyDeposit = Money(-1)) }
        assertThrows(IllegalArgumentException::class.java) { pensionSnapshot(listOf(fund(), fund()), today) }
    }
}
