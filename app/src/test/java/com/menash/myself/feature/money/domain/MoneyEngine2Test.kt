package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MoneyEngine2Test {
    private val jan = YearMonth.of(2026, 1)
    private val mar = YearMonth.of(2026, 3)
    private val apr = YearMonth.of(2026, 4)
    private fun ils(value: Long) = Money.fromShekels(value)
    private fun history(value: Long) = AmountHistory(listOf(AmountChange(jan, ils(value))))
    private fun plan(items: MoneyItems, month: YearMonth, rate: BigDecimal? = BigDecimal("4")) =
        monthPlan(month, items, rate) as MonthPlan.Planned
    private val card = Card("c1", "Max", 10)

    // 1. Real card charges for yearly items

    @Test fun yearlyFixedExpenseIsBilledInFullInItsMonthWhileSurplusKeepsMonthlyEquivalent() {
        val insurance = FixedExpense("ins", "car insurance", history(1200), Frequency.YEARLY, card, Month.MARCH)
        val items = MoneyItems(fixedExpenses = listOf(insurance))
        val march = plan(items, mar)
        val april = plan(items, apr)
        assertEquals(ils(1200), march.cardCharges.single().amount)
        assertEquals(Money.ZERO, april.cardCharges.single().amount)
        assertEquals(ils(100), march.fixed)
        assertEquals(ils(100), april.fixed)
        assertEquals(ils(-100), march.expectedSurplus)
        assertEquals(ils(-100), april.expectedSurplus)
    }
    @Test fun yearlySubscriptionIsBilledInRenewalMonthConvertedOnce() {
        val sub = Subscription("s1", "app", history(120), Currency.USD, Frequency.YEARLY, LocalDate.of(2025, 4, 15), card)
        val items = MoneyItems(subscriptions = listOf(sub))
        assertEquals(ils(480), plan(items, apr).cardCharges.single().amount)
        assertEquals(Money.ZERO, plan(items, mar).cardCharges.single().amount)
        assertEquals(ils(40), plan(items, mar).subscriptions)
    }
    @Test fun monthlyItemsStillChargeEveryMonth() {
        val rent = FixedExpense("r", "rent", history(3000), card = card)
        assertEquals(ils(3000), plan(MoneyItems(fixedExpenses = listOf(rent)), apr).cardCharges.single().amount)
    }
    @Test(expected = IllegalArgumentException::class) fun yearlyFixedExpenseWithoutBillingMonthIsRejected() {
        FixedExpense("x", "x", history(1), Frequency.YEARLY)
    }

    // 2. Stable ids

    @Test fun cardsWithSameNameAndDayStaySeparateById() {
        val a = Card("a", "Visa", 2)
        val b = Card("b", "Visa", 2)
        val items = MoneyItems(variableBudgets = listOf(VariableBudget("v1", "food", history(100), a), VariableBudget("v2", "fuel", history(50), b)))
        assertEquals(listOf(ils(100), ils(50)), plan(items, jan).cardCharges.map { it.amount })
        assertEquals(listOf("a", "b"), plan(items, jan).cardCharges.map { it.card.id })
    }
    @Test(expected = IllegalArgumentException::class) fun blankIdIsRejected() { Card(" ", "x", 1) }

    // 5. API follow-ups

    @Test fun plannedExpensesSumsAllExpenseGroupsAndFeedsCushion() {
        val items = MoneyItems(
            incomes = listOf(Income("i", "salary", history(1000))),
            fixedExpenses = listOf(FixedExpense("f", "rent", history(100))),
            variableBudgets = listOf(VariableBudget("v", "food", history(200))),
        )
        val p = plan(items, jan)
        assertEquals(ils(300), p.plannedExpenses)
        val assets = listOf(Asset("a", "cash", "bank", ils(900), AssetKind.ASSET, true, LocalDate.of(2026, 1, 1)))
        assertEquals(3.0, cushionMonths(assets, p)!!, 0.0)
    }
    @Test fun nullRateIsAllowedWithoutActiveUsdItems() {
        val items = MoneyItems(
            subscriptions = listOf(
                Subscription("ils", "ils", history(10), renewalDate = jan.atDay(1)),
                Subscription("usd", "usd", AmountHistory(listOf(AmountChange(apr, ils(5)))), Currency.USD, renewalDate = jan.atDay(1)),
            ),
        )
        assertEquals(ils(10), plan(items, jan, rate = null).subscriptions)
    }
    @Test(expected = IllegalArgumentException::class) fun nullRateWithActiveUsdItemFails() {
        val sub = Subscription("usd", "usd", history(5), Currency.USD, renewalDate = jan.atDay(1))
        monthPlan(jan, MoneyItems(subscriptions = listOf(sub)), null)
    }

    // 3. Is it possible?

    private fun input(
        goal: Goal, saving: Money? = ils(1000), others: List<Goal> = emptyList(),
        liquid: Long = 30_000, expenses: Long = 10_000, variable: Long = 2_000,
    ) = FeasibilityInput(goal, others, saving, jan, ils(liquid), ils(expenses), ils(variable))
    private fun goal(amount: Long, target: YearMonth, allocated: Long = 0, newCost: Long? = null) =
        Goal("g", "goal", ils(amount), target, ils(allocated), newCost?.let { ils(it) })

    @Test fun possibleWhenNeededFitsFreeMoney() {
        assertEquals(Feasibility.Possible(ils(500)), isItPossible(input(goal(6000, jan.plusMonths(12)))))
    }
    @Test fun allocatedAndOtherGoalsAreAccounted() {
        val other = Goal("o", "other", ils(4800), jan.plusMonths(12))
        assertEquals(Feasibility.Possible(ils(500)), isItPossible(input(goal(8000, jan.plusMonths(12), allocated = 2000), others = listOf(other), saving = ils(900))))
    }
    @Test fun possibleIfOffersPostponeCutSavingsAndInstallmentsUpToThree() {
        val result = isItPossible(input(goal(12_000, jan.plusMonths(6)), liquid = 40_000)) as Feasibility.PossibleIf
        assertEquals(ils(2000), result.monthlyAmount)
        assertEquals(
            listOf(Lever.Postpone(jan.plusMonths(12)), Lever.Cut(ils(1000)), Lever.TakeFromSavings(ils(6000))),
            result.levers,
        )
    }
    @Test fun installmentsLeverWhenOthersDoNotApply() {
        // Cushion full but no spare savings, nothing to cut: postpone and installments remain.
        val result = isItPossible(input(goal(12_000, jan.plusMonths(6)), liquid = 30_000, variable = 0)) as Feasibility.PossibleIf
        assertEquals(listOf(Lever.Postpone(jan.plusMonths(12)), Lever.Installments(12, ils(1000))), result.levers)
    }
    @Test fun cushionComesFirstAndBlocksTakingFromSavings() {
        // Cushion target 30,000, liquid 18,000: 12,000 over 12 months = 1,000/month, all of the free money.
        val result = isItPossible(input(goal(1200, jan.plusMonths(12)), liquid = 18_000, variable = 500))
        assertEquals(Feasibility.PossibleIf(ils(100), listOf(Lever.Cut(ils(100)))), result)
    }
    @Test fun notNowWhenNoLeverClosesTheGap() {
        val result = isItPossible(input(goal(200_000, jan.plusMonths(2)), saving = ils(1000), liquid = 30_000, variable = 0))
        assertEquals(Feasibility.NotNow(ils(99_000), Lever.Postpone(jan.plusMonths(200))), result)
    }
    @Test fun newMonthlyCostMustLeaveAPositiveSurplus() {
        assertTrue(isItPossible(input(goal(1200, jan.plusMonths(12), newCost = 999))) is Feasibility.Possible)
        assertEquals(
            Feasibility.PossibleIf(ils(100), listOf(Lever.Cut(Money(1)))),
            isItPossible(input(goal(1200, jan.plusMonths(12), newCost = 1000))),
        )
        assertEquals(Feasibility.NotNow(Money(500_001), null), isItPossible(input(goal(1200, jan.plusMonths(12), newCost = 6000))))
    }
    @Test fun unknownWithoutSavingRate() {
        assertSame(Feasibility.Unknown, isItPossible(input(goal(1, jan.plusMonths(1)), saving = null)))
    }
    @Test(expected = IllegalArgumentException::class) fun pastTargetMonthIsInvalid() {
        isItPossible(input(goal(1, jan.minusMonths(1))))
    }
    @Test fun targetThisMonthCountsAsOneMonth() {
        assertEquals(Feasibility.Possible(ils(900)), isItPossible(input(goal(900, jan))))
    }

    // 4. Charity balance

    private val salary = Income("sal", "salary", history(0), TitheRate.MAASER)
    private val bonus = Income("bon", "bonus", history(0), TitheRate.CHOMESH)
    private val gift = Income("gif", "gift", history(0))
    private fun actual(id: String, value: Long) = ActualIncome(id, jan, ils(value))
    private fun donation(value: Long, date: LocalDate = LocalDate.of(2026, 2, 1), submitted: Boolean = false) =
        Donation("d$value$date", date, ils(value), "org", submitted)

    @Test fun eachRateAppliesToItsSources() {
        val balance = charityBalance(listOf(salary, bonus, gift),
            listOf(actual("sal", 10_000), actual("bon", 1_000), actual("gif", 5_000)), Money.ZERO, Money.ZERO, emptyList())
        assertEquals(CharityBalance.Owed(ils(1200)), balance)
    }
    @Test fun portfolioGainAddsTwentyPercentOfCurrentProfit() {
        assertEquals(CharityBalance.Owed(ils(200)), charityBalance(emptyList(), emptyList(), ils(11_000), ils(10_000), emptyList()))
    }
    @Test fun portfolioLossAloneGivesCredit() {
        assertEquals(CharityBalance.Credit(ils(200)), charityBalance(emptyList(), emptyList(), ils(9_000), ils(10_000), emptyList()))
    }
    @Test fun donationsReduceToSettledOrCredit() {
        val incomes = listOf(salary)
        val actuals = listOf(actual("sal", 10_000))
        assertEquals(CharityBalance.Settled, charityBalance(incomes, actuals, Money.ZERO, Money.ZERO, listOf(donation(1000))))
        assertEquals(CharityBalance.Credit(ils(500)), charityBalance(incomes, actuals, Money.ZERO, Money.ZERO, listOf(donation(1500))))
    }
    @Test fun percentRoundsHalfUpToAgora() {
        val balance = charityBalance(listOf(salary), listOf(ActualIncome("sal", jan, Money(5))), Money.ZERO, Money.ZERO, emptyList())
        assertEquals(CharityBalance.Owed(Money(1)), balance)
    }
    @Test(expected = IllegalArgumentException::class) fun unknownIncomeIdFails() {
        charityBalance(emptyList(), listOf(actual("nope", 1)), Money.ZERO, Money.ZERO, emptyList())
    }
    @Test fun unsubmittedCountsOnlyThisYearsOpenDonations() {
        val list = listOf(donation(100), donation(200, submitted = true), donation(300, LocalDate.of(2025, 12, 31)), donation(50))
        assertEquals(UnsubmittedDonations(2, ils(150)), unsubmittedThisYear(list, 2026))
    }
}
