package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.PensionFund
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** "פנסיה וחיסכון": today's snapshot per fund, and the fund sheet. */
class PensionViewTest {

    private val today = LocalDate.of(2026, 10, 30)
    private val pension = PensionFund(
        "p", FundKind.PENSION, "הראל", Money(20_000_000), Money(16_000_000), Money(300_000),
        BigDecimal("0.22"), BigDecimal("1.5"), null, LocalDate.of(2026, 10, 1),
    )
    private val hishtalmut = PensionFund(
        "h", FundKind.HISHTALMUT, "מיטב", Money(12_000_000), Money(10_000_000), Money(150_000),
        BigDecimal("0.7"), BigDecimal.ZERO, LocalDate.of(2019, 6, 1), LocalDate.of(2026, 5, 1),
    )

    @Test
    fun noFunds_noTotal() {
        val v = pensionView(emptyList(), today)
        assertTrue(v.rows.isEmpty())
        assertNull(v.total)
    }

    @Test
    fun rowsInKindOrder_withPriciestStaleAndLiquidFromTheEngine() {
        val v = pensionView(listOf(hishtalmut, pension), today)
        assertEquals(listOf("p", "h"), v.rows.map { it.fund.id })
        // Pension: 200,000 x 0.22% = 440 + 3,000 x 12 x 1.5% = 540 -> 980. Hishtalmut: 120,000 x 0.7% = 840.
        assertEquals(Money(98_000), v.rows[0].snapshot.yearlyFee)
        assertEquals(Money(84_000), v.rows[1].snapshot.yearlyFee)
        assertTrue(v.rows[0].priciest)
        assertFalse(v.rows[1].priciest)
        assertTrue(v.rows[1].stale)
        assertTrue(v.rows[1].liquid)
        assertEquals(Money(32_000_000), v.total!!.value)
        assertEquals(Money(6_000_000), v.total!!.profit)
    }

    @Test
    fun draft_roundTripsAndStampsToday() {
        val result = buildFund(fundDraftOf(hishtalmut), today) { "new" } as FundResult.Ok
        assertEquals(hishtalmut.copy(lastUpdated = today), result.fund)
    }

    @Test
    fun draft_needsProviderAndValue_feesInRange() {
        val base = FundDraft(provider = "אלטשולר", value = "50,000")
        assertEquals(FundResult.Invalid(FundField.PROVIDER), buildFund(base.copy(provider = " "), today) { "x" })
        assertEquals(FundResult.Invalid(FundField.VALUE), buildFund(base.copy(value = ""), today) { "x" })
        assertEquals(FundResult.Invalid(FundField.FEE_BALANCE), buildFund(base.copy(feeOnBalance = "12"), today) { "x" })
        assertEquals(FundResult.Invalid(FundField.FEE_DEPOSITS), buildFund(base.copy(feeOnDeposits = "abc"), today) { "x" })
        val ok = buildFund(base.copy(feeOnBalance = "0.5%"), today) { "x" } as FundResult.Ok
        assertEquals(Money(5_000_000), ok.fund.value)
        assertEquals(Money.ZERO, ok.fund.totalDeposits)
        assertEquals(BigDecimal("0.5"), ok.fund.feeOnBalance)
        assertEquals("x", ok.fund.id)
    }

    @Test
    fun bigPicture_countsFundsInTotalWorthButNotInLiquidNetWorth() {
        val month = java.time.YearMonth.of(2026, 10)
        val none = buildMoneyMonth(MoneyData(householdReady = true), month, today, null).bigPicture
        assertNull(none.totalWorth)
        assertNull(none.pension)

        val onlyFunds = buildMoneyMonth(MoneyData(householdReady = true, pensionFunds = listOf(pension, hishtalmut)), month, today, null).bigPicture
        assertNull(onlyFunds.netWorth)
        assertEquals(Money(32_000_000), onlyFunds.totalWorth)
        assertEquals(Money(182_000), onlyFunds.pension?.yearlyFee)

        val withAssets = buildMoneyMonth(MoneySamples.filled.copy(pensionFunds = listOf(pension)), month, today, null).bigPicture
        val liquid = withAssets.netWorth!!
        assertEquals(liquid + Money(20_000_000), withAssets.totalWorth)
    }
}
