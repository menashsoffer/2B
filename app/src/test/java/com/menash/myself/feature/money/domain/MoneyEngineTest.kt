package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MoneyEngineTest {
    private val jan = YearMonth.of(2026, 1)
    private val feb = jan.plusMonths(1)
    private val mar = jan.plusMonths(2)
    private fun ils(value: Long) = Money.fromShekels(value)
    private fun history(value: Long) = AmountHistory(listOf(AmountChange(jan, ils(value))))
    private fun plan(items: MoneyItems, month: YearMonth = jan, rate: String = "4") =
        monthPlan(month, items, BigDecimal(rate)) as MonthPlan.Planned
    private fun asset(balance: Long, kind: AssetKind = AssetKind.ASSET, liquid: Boolean = true,
                      updated: LocalDate = LocalDate.of(2026, 1, 31)) =
        Asset("balance", "cash", ils(balance), kind, liquid, updated)
    private fun snapshot(month: YearMonth, income: Long, expenses: Long) =
        MonthSnapshot(month, ils(income), ils(expenses))

    @Test fun overrideOnlyAffectsOneMonth() {
        val item = Income("salary", AmountHistory(listOf(AmountChange(jan, ils(100))), mapOf(feb to ils(200))))
        assertEquals(ils(100), amountFor(item, jan))
        assertEquals(ils(200), amountFor(item, feb))
        assertEquals(ils(100), amountFor(item, mar))
    }
    @Test fun defaultChangePreservesEarlierMonthsAndDoesNotDependOnInputOrder() {
        val item = FixedExpense("rent", AmountHistory(listOf(AmountChange(mar, ils(300)), AmountChange(jan, ils(100)))))
        assertNull(amountFor(item, jan.minusMonths(1)))
        assertEquals(ils(100), amountFor(item, feb))
        assertEquals(ils(300), amountFor(item, mar.plusMonths(20)))
    }
    @Test fun overridePrecedesDefaultIncludingZeroAndBeforeFirstDefault() {
        val item = VariableBudget("food", AmountHistory(listOf(AmountChange(mar, ils(50))), mapOf(jan to Money.ZERO)))
        assertEquals(Money.ZERO, amountFor(item, jan))
        assertNull(amountFor(item, feb))
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateEffectiveMonthsAreRejected() {
        AmountHistory(listOf(AmountChange(jan, ils(1)), AmountChange(jan, ils(2))))
    }
    @Test fun installmentsSplitExactlyAndEnd() {
        val purchase = InstallmentPurchase("purchase", ils(1000), jan, 3)
        val payments = installmentSchedule(purchase)
        assertEquals(listOf(Money(33334), Money(33333), Money(33333)), payments.map { it.amount })
        assertEquals(listOf(feb, mar, mar.plusMonths(1)), payments.map { it.month })
        assertEquals(ils(1000), sumMoney(payments.map { it.amount }))
        val items = MoneyItems(installments = listOf(purchase))
        assertEquals(Money.ZERO, plan(items, jan).installments)
        payments.forEach { assertEquals(it.amount, plan(items, it.month).installments) }
        assertEquals(Money.ZERO, plan(items, jan.plusMonths(4)).installments)
    }
    @Test fun installmentSchedulesAlwaysPreserveTotalAcrossSmallAmounts() {
        for (total in 0L..100L) for (count in 1..12) {
            val purchase = InstallmentPurchase("purchase", Money(total), jan, count)
            val payments = installmentSchedule(purchase)
            assertEquals(count, payments.size)
            assertEquals(Money(total), sumMoney(payments.map { it.amount }))
            assertTrue(payments.all { it.amount >= Money.ZERO })
        }
    }
    @Test(expected = ArithmeticException::class) fun overflowingTotalsFailRatherThanWrap() {
        val large = AmountHistory(listOf(AmountChange(jan, Money(Long.MAX_VALUE))))
        plan(MoneyItems(incomes = listOf(Income("one", large), Income("two", large))))
    }
    @Test fun tinyTotalRemainderIsOnFirstPaymentAndSinglePaymentDefaults() {
        val tiny = InstallmentPurchase("tiny", Money(2), jan, 3)
        assertEquals(listOf(Money(2), Money.ZERO, Money.ZERO), installmentSchedule(tiny).map { it.amount })
        assertEquals(listOf(InstallmentPayment(feb, ils(10))), installmentSchedule(InstallmentPurchase("one", ils(10), jan)))
    }
    @Test(expected = IllegalArgumentException::class) fun zeroPaymentsRejected() {
        InstallmentPurchase("bad", ils(1), jan, 0)
    }
    @Test fun annualExpenseIsMonthlyEquivalentAndUsesHalfUp() {
        assertEquals(ils(100), plan(MoneyItems(fixedExpenses = listOf(FixedExpense("annual", history(1200), Frequency.YEARLY)))).fixed)
        val item = FixedExpense("fraction", AmountHistory(listOf(AmountChange(jan, Money(6)))), Frequency.YEARLY)
        assertEquals(Money(1), plan(MoneyItems(fixedExpenses = listOf(item))).fixed)
    }
    @Test fun usdUsesPassedRateAndConvertsAnnualAmountBeforeRounding() {
        val sub = Subscription("usd", history(12), Currency.USD, Frequency.YEARLY, jan.atDay(1))
        val items = MoneyItems(subscriptions = listOf(sub))
        assertEquals(ils(4), plan(items, rate = "4").subscriptions)
        assertEquals(Money(325), plan(items, rate = "3.25").subscriptions)
        val fractional = sub.copy(amounts = AmountHistory(listOf(AmountChange(jan, Money(2)))))
        assertEquals(Money(1), plan(MoneyItems(subscriptions = listOf(fractional)), rate = "3").subscriptions)
    }
    @Test fun ilsSubscriptionDoesNotUseDollarRate() {
        val sub = Subscription("ils", history(100), renewalDate = jan.atDay(1))
        assertEquals(ils(100), plan(MoneyItems(subscriptions = listOf(sub)), rate = "9").subscriptions)
    }
    @Test fun emptyIsDistinctFromNoActiveItems() {
        assertSame(MonthPlan.Empty, monthPlan(jan, MoneyItems(), BigDecimal.ONE))
        val result = plan(MoneyItems(incomes = listOf(Income("future", AmountHistory(listOf(AmountChange(feb, ils(10))))))))
        assertEquals(Money.ZERO, result.income)
        assertEquals(Money.ZERO, result.expectedSurplus)
        assertTrue(result.cardCharges.isEmpty())
    }
    @Test fun planCombinesAllCategoriesAndKeepsCardsSeparate() {
        val a = Card("A", 31)
        val b = Card("B", 10)
        val items = MoneyItems(
            incomes = listOf(Income("salary", history(1000))),
            fixedExpenses = listOf(FixedExpense("rent", history(100), card = a), FixedExpense("cash", history(50))),
            subscriptions = listOf(Subscription("sub", history(120), frequency = Frequency.YEARLY, renewalDate = jan.atDay(1), card = a)),
            variableBudgets = listOf(VariableBudget("food", history(200), b)),
            installments = listOf(InstallmentPurchase("purchase", ils(300), jan, 3, jan, a)),
        )
        val result = plan(items)
        assertEquals(ils(1000), result.income)
        assertEquals(ils(150), result.fixed)
        assertEquals(ils(10), result.subscriptions)
        assertEquals(ils(200), result.variable)
        assertEquals(ils(100), result.installments)
        assertEquals(ils(540), result.expectedSurplus)
        assertEquals(listOf(CardCharge(a, jan.atDay(31), ils(210)), CardCharge(b, jan.atDay(10), ils(200))), result.cardCharges)
    }
    @Test fun deficitIsNotClamped() {
        assertEquals(ils(-100), plan(MoneyItems(variableBudgets = listOf(VariableBudget("expense", history(100))))).expectedSurplus)
    }
    @Test fun billingDayClampsForFebruaryAndThirtyDayMonths() {
        val card = Card("card", 31)
        assertEquals(LocalDate.of(2026, 2, 28), chargeDate(card, feb))
        assertEquals(LocalDate.of(2024, 2, 29), chargeDate(card, YearMonth.of(2024, 2)))
        assertEquals(LocalDate.of(2026, 4, 30), chargeDate(card, YearMonth.of(2026, 4)))
        assertEquals(feb.atDay(1), chargeDate(Card("first", 1), feb))
        val items = MoneyItems(fixedExpenses = listOf(FixedExpense("card payment", history(10), card = card)))
        assertEquals(feb.atEndOfMonth(), plan(items, feb).cardCharges.single().date)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidBillingDayRejected() { Card("bad", 32) }
    @Test(expected = IllegalArgumentException::class) fun zeroExchangeRateRejected() { monthPlan(jan, MoneyItems(), BigDecimal.ZERO) }
    @Test fun netWorthSubtractsLiabilitiesRegardlessOfLiquidity() {
        assertEquals(ils(600), netWorth(listOf(asset(1000), asset(300, AssetKind.LIABILITY), asset(100, AssetKind.LIABILITY, false))))
        assertEquals(Money.ZERO, netWorth(emptyList()))
        assertEquals(ils(-100), netWorth(listOf(asset(100, AssetKind.LIABILITY))))
    }
    @Test fun cushionUsesOnlyLiquidAssetsAndHandlesUnknownOrZeroExpenses() {
        val assets = listOf(asset(600), asset(1000, liquid = false), asset(200, AssetKind.LIABILITY))
        assertEquals(3.0, cushionMonths(assets, ils(200))!!, 0.0)
        assertNull(cushionMonths(assets, null))
        assertNull(cushionMonths(assets, Money.ZERO))
        assertEquals(0.0, cushionMonths(emptyList(), ils(100))!!, 0.0)
    }
    @Test fun savingUsesLatestThreeInChronologicalOrderAndAllowsLosses() {
        assertNull(savingRate(emptyList()))
        assertEquals(ils(-10), savingRate(listOf(snapshot(jan, 90, 100))))
        assertEquals(ils(15), savingRate(listOf(snapshot(jan, 100, 90), snapshot(feb, 100, 80))))
        assertEquals(ils(20), savingRate(listOf(snapshot(mar, 100, 80), snapshot(jan.minusMonths(1), 1000, 0), snapshot(jan, 100, 90), snapshot(feb, 100, 70))))
    }
    @Test fun savingAverageRoundsHalfUp() {
        assertEquals(Money(1), savingRate(listOf(MonthSnapshot(jan, Money(1), Money.ZERO), MonthSnapshot(feb, Money.ZERO, Money.ZERO))))
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateSnapshotMonthRejected() {
        savingRate(listOf(snapshot(jan, 1, 0), snapshot(jan, 2, 0)))
    }
    @Test fun staleOnlyAfterThreeCalendarMonthsIncludingMonthEndAndLeapYear() {
        val value = asset(1)
        assertFalse(isStale(value, LocalDate.of(2026, 4, 29)))
        assertFalse(isStale(value, LocalDate.of(2026, 4, 30)))
        assertTrue(isStale(value, LocalDate.of(2026, 5, 1)))
        assertFalse(isStale(value, LocalDate.of(2025, 1, 1)))
        val leap = asset(1, updated = LocalDate.of(2023, 11, 30))
        assertFalse(isStale(leap, LocalDate.of(2024, 2, 29)))
        assertTrue(isStale(leap, LocalDate.of(2024, 3, 1)))
    }
}
