package com.menash.myself.feature.money.domain

import com.menash.myself.core.domain.model.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class MoneyEngine3Test {
    private val jan = YearMonth.of(2026, 1)
    private val feb = YearMonth.of(2026, 2)
    private fun ils(value: Long) = Money.fromShekels(value)
    private fun history(value: Long) = AmountHistory(listOf(AmountChange(jan, ils(value))))
    private fun plan(items: MoneyItems, month: YearMonth = jan) = monthPlan(month, items, null) as MonthPlan.Planned
    private val card = Card("max", "Max", 10)
    private fun tx(id: String, amount: Money, categoryId: String? = null, month: YearMonth = jan, merchant: String = "shop",
                   installment: InstallmentInfo? = null, source: String = "max") =
        Transaction(id, month.atDay(5), month, merchant, amount, PaymentMethod.CREDIT_CARD, source, categoryId, installment = installment)

    // 1. Expense classes

    @Test fun defaultCategoriesCoverAllPaamonimClassesWithUniqueIds() {
        val all = defaultCategories()
        assertEquals(all.size, all.map { it.id }.distinct().size)
        assertEquals(16, all.count { it.klass == ExpenseClass.FIXED })
        assertEquals(20, all.count { it.klass == ExpenseClass.PERIODIC })
        assertEquals(12, all.count { it.klass == ExpenseClass.CURRENT })
        val subscriptions = all.single { it.name == "מנויים" }
        assertEquals(8, all.count { it.parentId == subscriptions.id })
        assertTrue(all.any { it.name == "חניה - כביש 6" && it.klass == ExpenseClass.CURRENT })
    }
    @Test fun planTotalsPerClass() {
        val items = MoneyItems(
            fixedExpenses = listOf(
                FixedExpense("rent", "rent", history(3000)),
                FixedExpense("arnona", "arnona", history(1200), Frequency.YEARLY, billingMonth = java.time.Month.MAY, klass = ExpenseClass.PERIODIC),
            ),
            subscriptions = listOf(Subscription("net", "internet", history(100), renewalDate = jan.atDay(1))),
            variableBudgets = listOf(VariableBudget("food", "food", history(2000))),
            periodicBudgets = listOf(PeriodicBudget("pb", "periodic-10", "clothes", ils(2400), jan)),
        )
        val p = plan(items)
        assertEquals(ils(3100), p.byClass[ExpenseClass.FIXED])
        assertEquals(ils(2000), p.byClass[ExpenseClass.CURRENT])
        assertEquals(ils(100 + 200), p.byClass[ExpenseClass.PERIODIC])
        assertEquals(ils(200), p.periodic)
    }

    // 2. Periodic sinking fund

    @Test fun periodicBudgetReservesEveryMonthAndTracksAdvance() {
        val budget = PeriodicBudget("pb", "periodic-10", "clothes", ils(1200), jan)
        assertEquals(ils(100), plan(MoneyItems(periodicBudgets = listOf(budget)), feb).periodic)
        val status = periodicStatus(budget, feb, listOf(tx("t1", ils(900), "periodic-10", feb), tx("t2", ils(50), "other", feb)))
        assertEquals(ils(200), status.accruedToDate)
        assertEquals(ils(900), status.spentToDate)
        assertEquals(ils(300), status.remainingThisYear)
        assertEquals(ils(700), status.advance)
    }
    @Test fun periodicYearCanStartMidCalendar() {
        val budget = PeriodicBudget("pb", "edu", "school", ils(1200), YearMonth.of(2025, 9))
        val spentBeforeYear = tx("old", ils(500), "edu", YearMonth.of(2025, 8))
        val status = periodicStatus(budget, feb, listOf(spentBeforeYear, tx("t", ils(100), "edu", YearMonth.of(2025, 12))))
        assertEquals(ils(600), status.accruedToDate)
        assertEquals(ils(100), status.spentToDate)
        assertEquals(ils(-500), status.advance)
    }
    @Test(expected = IllegalArgumentException::class) fun periodicMonthOutsideBudgetYearIsRejected() {
        periodicStatus(PeriodicBudget("pb", "c", "x", ils(1), jan), YearMonth.of(2027, 1), emptyList())
    }

    // 3. Actuals and plan vs actual

    @Test fun planVsActualPerCategoryWithUncategorisedAndRefunds() {
        val items = MoneyItems(variableBudgets = listOf(VariableBudget("food", "food", history(2000), categoryId = "current-1")))
        val result = planVsActual(jan, items, listOf(
            tx("a", ils(1500), "current-1"), tx("b", ils(-200), "current-1"), tx("c", ils(80)), tx("feb", ils(999), "current-1", feb),
        ), null)
        assertEquals(listOf(
            CategoryActual("current-1", ils(2000), ils(1300)),
            CategoryActual(null, Money.ZERO, ils(80)),
        ), result.categories)
        assertEquals(ils(700), result.categories.first().difference)
    }
    @Test fun matchingInstallmentCountsOnceAgainstThePurchase() {
        val purchase = InstallmentPurchase("tv", "tv", ils(1000), YearMonth.of(2025, 12), 3, card = card)
        val items = MoneyItems(installments = listOf(purchase))
        val payment = tx("p1", Money(33334), "current-1", installment = InstallmentInfo(1, 3))
        val result = planVsActual(jan, items, listOf(payment), null)
        assertEquals(listOf(InstallmentActual("tv", Money(33334), "p1", Money(33334))), result.installments)
        assertTrue(result.categories.isEmpty())
    }
    @Test fun installmentWithoutMatchingPurchaseGoesToItsCategory() {
        val purchase = InstallmentPurchase("tv", "tv", ils(1000), YearMonth.of(2025, 12), 3, card = card)
        val otherCard = tx("p1", Money(33334), "current-1", installment = InstallmentInfo(1, 3), source = "visa")
        val otherAmount = tx("p2", ils(500), "current-1", installment = InstallmentInfo(1, 3))
        val result = planVsActual(jan, MoneyItems(installments = listOf(purchase)), listOf(otherCard, otherAmount), null)
        assertTrue(result.installments.isEmpty())
        assertEquals(listOf(CategoryActual("current-1", Money.ZERO, Money(83334))), result.categories)
    }
    @Test fun importCheckUsesExactAgorot() {
        val rows = listOf(tx("a", Money(10_050)), tx("b", Money(-50)))
        assertSame(ImportCheck.Ok, importCheck(rows, ils(100)))
        assertEquals(ImportCheck.Mismatch(Money(-1)), importCheck(rows, Money(10_001)))
    }

    // 4. Merchant memory

    @Test fun normalizeDropsBranchAndCardNumbersButKeepsHebrew() {
        assertEquals("שופרסל דיל", normalizeMerchant("  שופרסל   דיל 123 "))
        assertEquals("סופר פארם", normalizeMerchant("סופר פארם סניף 12"))
        assertEquals("super-pharm", normalizeMerchant("SUPER-PHARM *1234"))
        assertEquals("7 eleven", normalizeMerchant("7 Eleven"))
    }
    @Test fun learnedMerchantIsSuggestedAcrossBranches() {
        val memory = learn(MerchantMemory(), "שופרסל דיל 123", "current-1")
        assertEquals("current-1", suggestCategory("שופרסל דיל 456", memory))
        assertNull(suggestCategory("שופרסל", memory))
    }
    @Test fun differentMerchantsDoNotMerge() {
        val memory = learn(MerchantMemory(), "רמי לוי", "current-1")
        assertNull(suggestCategory("רמי לוי תקשורת", memory))
        assertEquals("current-5", suggestCategory("רמי לוי תקשורת", learn(memory, "רמי לוי תקשורת", "current-5")))
        assertEquals("current-1", suggestCategory("רמי לוי", learn(memory, "רמי לוי תקשורת", "current-5")))
    }

    // 5. Debts and savings

    @Test fun debtPaymentsRunForRemainingMonthsAfterAsOf() {
        val loan = Debt("d", "bank", ils(3000), LocalDate.of(2025, 12, 20), BigDecimal("5.5"), 2, ils(1000))
        val items = MoneyItems(incomes = listOf(Income("i", "salary", history(5000))), debts = listOf(loan))
        assertEquals(ils(1000), plan(items, jan).debtPayments)
        assertEquals(ils(4000), plan(items, jan).expectedSurplus)
        assertEquals(ils(1000), plan(items, jan).plannedExpenses)
        assertEquals(ils(1000), plan(items, feb).debtPayments)
        assertEquals(Money.ZERO, plan(items, feb.plusMonths(1)).debtPayments)
        assertEquals(Money.ZERO, plan(items, YearMonth.of(2025, 12)).debtPayments)
    }
    @Test fun savingDepositsRespectTheKnownStartMonth() {
        val saving = Saving("s", "kids", ils(500), ils(10_000), startMonth = feb)
        val items = MoneyItems(savings = listOf(saving))
        assertEquals(Money.ZERO, plan(items, jan).savingDeposits)
        assertEquals(ils(500), plan(items, feb).savingDeposits)
    }
    @Test fun savingDepositsReduceSurplusUntilMaturityButAreNotExpenses() {
        val saving = Saving("s", "kids", ils(500), ils(10_000), maturity = feb)
        val items = MoneyItems(incomes = listOf(Income("i", "salary", history(5000))), savings = listOf(saving))
        assertEquals(ils(500), plan(items, feb).savingDeposits)
        assertEquals(ils(4500), plan(items, feb).expectedSurplus)
        assertEquals(Money.ZERO, plan(items, feb).plannedExpenses)
        assertEquals(Money.ZERO, plan(items, feb.plusMonths(1)).savingDeposits)
    }
}
