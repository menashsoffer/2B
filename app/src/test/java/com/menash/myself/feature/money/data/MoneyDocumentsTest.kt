package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.menash.myself.feature.money.domain.TitheSettings
import java.time.YearMonth

/** Every stored type survives encode then decode unchanged, cards resolved by id. */
class MoneyDocumentsTest {

    private val cards = MoneySamples.filled.cards.associateBy { it.id }

    private fun roundTrip(entry: MoneyEntry): MoneyEntry =
        MoneyDocuments.decode(entry.collection, entry.id, MoneyDocuments.encode(entry), cards)

    private fun assertRoundTrip(entry: MoneyEntry) = assertEquals(entry, roundTrip(entry))

    @Test
    fun card() = MoneySamples.filled.cards.forEach { assertRoundTrip(MoneyEntry.CardEntry(it)) }

    @Test
    fun income_keepsTitheRateAndHistory() {
        val income = MoneySamples.salary.copy(
            amounts = AmountHistory(
                changes = listOf(
                    AmountChange(YearMonth.of(2026, 3), Money(1_900_000)),
                    AmountChange(YearMonth.of(2026, 1), Money(1_800_000)),
                ),
                overrides = mapOf(YearMonth.of(2026, 4) to Money(0), YearMonth.of(2026, 2) to Money(2_000_050)),
            ),
        )
        val decoded = roundTrip(MoneyEntry.IncomeEntry(income)) as MoneyEntry.IncomeEntry
        assertEquals(income.titheRate, decoded.value.titheRate)
        assertEquals(income.amounts.changes.toSet(), decoded.value.amounts.changes.toSet())
        assertEquals(income.amounts.overrides, decoded.value.amounts.overrides)
    }

    @Test
    fun fixedExpenses_monthlyAndYearlyWithCardClassAndCategory() {
        assertRoundTrip(MoneyEntry.FixedExpenseEntry(MoneySamples.rent))
        assertRoundTrip(MoneyEntry.FixedExpenseEntry(MoneySamples.carInsurance))
    }

    @Test
    fun subscriptions_ilsAndUsd() {
        assertRoundTrip(MoneyEntry.SubscriptionEntry(MoneySamples.netflix))
        assertRoundTrip(MoneyEntry.SubscriptionEntry(MoneySamples.chatGpt))
    }

    @Test
    fun variableBudgets() {
        assertRoundTrip(MoneyEntry.VariableBudgetEntry(MoneySamples.food))
        assertRoundTrip(MoneyEntry.VariableBudgetEntry(MoneySamples.fuel))
    }

    @Test
    fun installment() = assertRoundTrip(MoneyEntry.InstallmentEntry(MoneySamples.fridge))

    @Test
    fun periodicBudget() = assertRoundTrip(MoneyEntry.PeriodicBudgetEntry(MoneySamples.holidays))

    @Test
    fun debt_keepsInterestExactly() {
        assertRoundTrip(MoneyEntry.DebtEntry(MoneySamples.studyLoan))
        val withInterest = MoneySamples.studyLoan.copy(annualInterestPercent = java.math.BigDecimal("4.25"))
        assertRoundTrip(MoneyEntry.DebtEntry(withInterest))
    }

    @Test
    fun saving_withAndWithoutMaturity() {
        assertRoundTrip(MoneyEntry.SavingEntry(MoneySamples.pension))
        assertRoundTrip(MoneyEntry.SavingEntry(MoneySamples.pension.copy(maturity = null)))
    }

    @Test
    fun assets_assetAndLiability() {
        MoneySamples.filled.assets.forEach { assertRoundTrip(MoneyEntry.AssetEntry(it)) }
    }

    @Test
    fun categories_allDefaultsAndACustomOne() {
        defaultCategories().forEach { assertRoundTrip(MoneyEntry.CategoryEntry(it)) }
        assertRoundTrip(MoneyEntry.CategoryEntry(Category("c1b2", "ספרים", ExpenseClass.PERIODIC)))
    }

    @Test
    fun goals_withAndWithoutAMonthlyCost() {
        assertRoundTrip(MoneyEntry.GoalEntry(MoneySamples.trip))
        assertRoundTrip(MoneyEntry.GoalEntry(MoneySamples.car))
    }

    @Test
    fun donationsAndMaaserSettings() {
        MoneySamples.donations.forEach { assertRoundTrip(MoneyEntry.DonationEntry(it)) }
        assertRoundTrip(MoneyEntry.TitheEntry(MoneySamples.tithe))
        assertRoundTrip(MoneyEntry.TitheEntry(TitheSettings(java.time.YearMonth.of(2026, 1))))
    }

    @Test
    fun closedMonth_storedUnderItsMonth() {
        val month = java.time.YearMonth.of(2026, 9)
        val closed = com.menash.myself.feature.money.domain.ClosedMonth(
            month, com.menash.myself.feature.money.domain.MonthSnapshot(month, Money(1_500_000), Money(1_200_000)),
            mapOf("food" to Money(300_000)), emptyMap(), mapOf("asset-bank" to Money(2_000_000)), emptyMap(),
            java.time.LocalDate.of(2026, 10, 2),
        )
        val entry = MoneyEntry.ClosedMonthEntry(closed)
        assertEquals("2026-09", entry.id)
        assertRoundTrip(entry)
    }

    @Test
    fun pensionFunds_feesKeepTheirDecimals() {
        val fund = com.menash.myself.feature.money.domain.PensionFund(
            "fund-1", com.menash.myself.feature.money.domain.FundKind.HISHTALMUT, "מיטב", Money(12_000_000), Money(9_500_000),
            Money(150_000), java.math.BigDecimal("0.53"), java.math.BigDecimal("0"), java.time.LocalDate.of(2020, 3, 1),
            java.time.LocalDate.of(2026, 10, 1),
        )
        assertRoundTrip(MoneyEntry.PensionFundEntry(fund))
        assertRoundTrip(MoneyEntry.PensionFundEntry(fund.copy(openedOn = null)))
        assertEquals("0.53", MoneyDocuments.encode(MoneyEntry.PensionFundEntry(fund))["feeOnBalance"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun maaserSettings_onlyUnderItsOwnId() {
        MoneyDocuments.decode(MoneyCollection.TITHE, "other", MoneyDocuments.encode(MoneyEntry.TitheEntry(MoneySamples.tithe)))
    }

    @Test
    fun amountsAreStoredAsAgorotLongs() {
        val encoded = MoneyDocuments.encode(MoneyEntry.InstallmentEntry(MoneySamples.fridge))
        assertEquals(600_000L, encoded["total"])
        assertEquals("2026-10", encoded["firstMonth"])
        assertEquals("card-visa", encoded["cardId"])
        assertFalse("the document id is not stored in the body", "id" in encoded)
    }

    @Test
    fun firestoreDoublesAndIntsAreAccepted() {
        val decoded = MoneyDocuments.decode(
            MoneyCollection.CARDS, "c1", mapOf("name" to "ישראכרט", "billingDay" to 15.0),
        ) as MoneyEntry.CardEntry
        assertEquals(15, decoded.value.billingDay)
    }

    @Test
    fun unknownCardIdDecodesToNoCard() {
        val encoded = MoneyDocuments.encode(MoneyEntry.VariableBudgetEntry(MoneySamples.food))
        val decoded = MoneyDocuments.decode(MoneyCollection.VARIABLE_BUDGETS, "var-food", encoded, emptyMap())
            as MoneyEntry.VariableBudgetEntry
        assertNull(decoded.value.card)
    }

    @Test
    fun malformedDocumentThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException::class.java) {
            MoneyDocuments.decode(MoneyCollection.INCOMES, "inc", mapOf("amounts" to emptyMap<String, Any>()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            MoneyDocuments.decode(MoneyCollection.CARDS, "c", mapOf("name" to "x", "billingDay" to 40L))
        }
    }

    @Test
    fun missingHistoryIsAnEmptyHistory() {
        val decoded = MoneyDocuments.decode(MoneyCollection.INCOMES, "inc", mapOf("name" to "בונוס"))
            as MoneyEntry.IncomeEntry
        assertEquals(Income("inc", "בונוס", AmountHistory(emptyList())), decoded.value)
        assertTrue(decoded.value.amounts.changes.isEmpty())
    }

    @Test
    fun seedOnlyWhenTheHouseholdHasNoCategories() {
        assertEquals(defaultCategories(), categoriesToSeed(emptyList(), defaultCategories()))
        assertEquals(emptyList<Category>(), categoriesToSeed(listOf(defaultCategories().first()), defaultCategories()))
    }
}
