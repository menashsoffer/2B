package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.CarCompareDraft
import com.menash.myself.feature.money.CarPlan
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.Donation
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.Included
import com.menash.myself.feature.money.domain.InstallmentInfo
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.MonthSnapshot
import com.menash.myself.feature.money.domain.PensionFund
import com.menash.myself.feature.money.domain.PeriodicStatus
import com.menash.myself.feature.money.domain.TitheRate
import com.menash.myself.feature.money.domain.TitheSettings
import com.menash.myself.feature.money.domain.VariableBudget
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.importer.RowStatus
import com.menash.myself.feature.money.importer.Source
import com.menash.myself.feature.money.importer.StatementRow
import com.menash.myself.feature.money.importer.StoredTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * One document per realistic shape of every money collection, built with the app's own writers.
 * firestore.test.js loads these and the rules must accept every one of them.
 */
object MoneyFixtures {

    /** collection path -> case name -> the exact document body the app writes. */
    fun all(): Map<String, Map<String, Map<String, Any>>> =
        (entries().groupBy({ it.second.collection.path }) { (case, entry) -> case to MoneyDocuments.encode(entry) }
            .mapValues { it.value.toMap() }) + imports()

    private val big = Money(9_000_000_000) // ₪90M: the largest realistic amount, inside the ±₪100M rule.

    private fun entries(): List<Pair<String, MoneyEntry>> = listOf(
        "visa" to MoneyEntry.CardEntry(MoneySamples.visa),
        "billing-day-31" to MoneyEntry.CardEntry(MoneySamples.max.copy(id = "card-31", billingDay = 31)),

        "salary-with-overrides" to MoneyEntry.IncomeEntry(
            MoneySamples.salary.copy(
                amounts = AmountHistory(
                    listOf(AmountChange(YearMonth.of(2026, 1), Money(1_800_000)), AmountChange(YearMonth.of(2026, 3), Money(1_900_000))),
                    mapOf(YearMonth.of(2026, 2) to Money(2_000_050), YearMonth.of(2026, 4) to Money(0)),
                ),
            ),
        ),
        "empty-history-chomesh" to MoneyEntry.IncomeEntry(Income("inc-gift", "מתנות", AmountHistory(emptyList()), TitheRate.CHOMESH)),
        "no-tithe" to MoneyEntry.IncomeEntry(MoneySamples.salary.copy(id = "inc-2", titheRate = TitheRate.NONE)),

        "monthly-no-card" to MoneyEntry.FixedExpenseEntry(MoneySamples.rent),
        "yearly-with-card" to MoneyEntry.FixedExpenseEntry(MoneySamples.carInsurance),

        "ils" to MoneyEntry.SubscriptionEntry(MoneySamples.netflix),
        "usd-with-category" to MoneyEntry.SubscriptionEntry(MoneySamples.chatGpt.copy(categoryId = "fixed-7")),

        "with-card-and-category" to MoneyEntry.VariableBudgetEntry(MoneySamples.food),
        "bare" to MoneyEntry.VariableBudgetEntry(VariableBudget("var-bare", "שונות", MoneySamples.history(100))),

        "fridge" to MoneyEntry.InstallmentEntry(MoneySamples.fridge),
        "large-no-card" to MoneyEntry.InstallmentEntry(MoneySamples.fridge.copy(id = "inst-car", total = big, numberOfPayments = 600, card = null)),

        "holidays" to MoneyEntry.PeriodicBudgetEntry(MoneySamples.holidays),

        "no-interest" to MoneyEntry.DebtEntry(MoneySamples.studyLoan),
        "mortgage" to MoneyEntry.DebtEntry(
            MoneySamples.studyLoan.copy(id = "debt-home", balance = big, annualInterestPercent = BigDecimal("4.25"), remainingPayments = 360),
        ),

        "with-maturity" to MoneyEntry.SavingEntry(MoneySamples.pension),
        "with-start" to MoneyEntry.SavingEntry(MoneySamples.pension.copy(startMonth = YearMonth.of(2026, 1))),
        "no-maturity" to MoneyEntry.SavingEntry(MoneySamples.pension.copy(id = "save-2", maturity = null)),

        "liquid-asset" to MoneyEntry.AssetEntry(MoneySamples.checking),
        "liability" to MoneyEntry.AssetEntry(MoneySamples.overdraft),
        "blank-type" to MoneyEntry.AssetEntry(
            Asset("asset-flat", "דירה", "", big, AssetKind.ASSET, false, LocalDate.of(2026, 1, 1)),
        ),

        "default" to MoneyEntry.CategoryEntry(defaultCategories().first()),
        "child" to MoneyEntry.CategoryEntry(defaultCategories().first { it.parentId != null }),
        "custom" to MoneyEntry.CategoryEntry(Category("c1b2", "ספרים", ExpenseClass.PERIODIC)),

        "allocated" to MoneyEntry.GoalEntry(MoneySamples.trip),
        "with-monthly-cost" to MoneyEntry.GoalEntry(MoneySamples.car),

        "for-tax-refund" to MoneyEntry.DonationEntry(MoneySamples.donations.first()),
        "matched" to MoneyEntry.DonationEntry(Donation("don-match", LocalDate.of(2026, 10, 2), Money(1_000), "", matchedLineId = "line-1")),
        "no-organisation" to MoneyEntry.DonationEntry(Donation("don-x", LocalDate.of(2026, 10, 2), Money(1_000), "")),

        "default" to MoneyEntry.AveragesSettingsEntry(null),
        "from-month" to MoneyEntry.AveragesSettingsEntry(YearMonth.of(2025, 6)),

        "full" to MoneyEntry.TitheEntry(MoneySamples.tithe),
        "negative-opening" to MoneyEntry.TitheEntry(TitheSettings(YearMonth.of(2026, 1), opening = Money(-5_000))),

        "full" to MoneyEntry.ClosedMonthEntry(
            ClosedMonth(
                YearMonth.of(2026, 9), MonthSnapshot(YearMonth.of(2026, 9), Money(1_500_000), Money(1_200_000)),
                mapOf("current-1" to Money(300_000), null to Money(4_500), "refunds" to Money(-2_000)),
                mapOf("per-holidays" to PeriodicStatus(Money(400_000), Money(120_000), Money(280_000))),
                mapOf("asset-checking" to Money(3_500_000)), mapOf("asset-od" to Money(200_000)),
                LocalDate.of(2026, 10, 2), balancesEntered = true,
            ),
        ),
        "empty" to MoneyEntry.ClosedMonthEntry(
            ClosedMonth(
                YearMonth.of(2025, 12), MonthSnapshot(YearMonth.of(2025, 12), Money.ZERO, Money.ZERO),
                emptyMap(), emptyMap(), emptyMap(), emptyMap(), LocalDate.of(2026, 11, 2),
            ),
        ),

        "defaults" to MoneyEntry.CarEntry(CarPlan()),
        "half-filled" to MoneyEntry.CarEntry(
            CarPlan(
                categoryIds = setOf("current-3", "periodic-8"),
                compare = CarCompareDraft(
                    horizonYears = 7, kmPerYear = "15000", price = "160,000", loanRate = "4.9", leaseMonthly = "2450",
                    leaseIncluded = setOf(Included.INSURANCE, Included.TIRES),
                ),
            ),
        ),

        "with-opened-on" to MoneyEntry.PensionFundEntry(
            PensionFund(
                "fund-1", FundKind.HISHTALMUT, "מיטב", Money(12_000_000), Money(9_500_000), Money(150_000),
                BigDecimal("0.53"), BigDecimal("0"), LocalDate.of(2020, 3, 1), LocalDate.of(2026, 10, 1),
            ),
        ),
        "no-opened-on" to MoneyEntry.PensionFundEntry(
            PensionFund(
                "fund-2", FundKind.PENSION, "מגדל", Money(0), Money(0), Money(0),
                BigDecimal("10"), BigDecimal("6"), null, LocalDate.of(2026, 10, 1),
            ),
        ),
    )

    private fun imports(): Map<String, Map<String, Map<String, Any>>> {
        val card = StatementRow(
            id = "max-1a2b", source = Source.MAX, sourceId = "max-file-1", date = LocalDate.of(2026, 10, 3),
            chargeDate = LocalDate.of(2026, 11, 2), description = "AMAZON MKTPLACE", details = "הוראת קבע",
            charge = Money(45_900), originalAmount = Money(12_300), originalCurrency = "USD",
            installment = InstallmentInfo(2, 12), hint = "שונות", operation = "עסקת תשלומים",
        )
        val bank = StatementRow(
            id = "hapoalim-9f", source = Source.HAPOALIM, sourceId = "hapoalim-oct", date = LocalDate.of(2026, 10, 10),
            chargeDate = null, description = "משכורת", charge = Money(-1_800_000),
        )
        val waiting = StatementRow(
            id = "cal-77", source = Source.CAL, sourceId = "cal-nov", date = LocalDate.of(2026, 11, 1),
            chargeDate = null, description = "HOTEL PARIS", charge = null, originalAmount = Money(50_000),
            originalCurrency = "EUR", status = RowStatus.WAITING_FX,
        )
        val key = "שופרסל דיל"
        return mapOf(
            ImportDocuments.TRANSACTIONS to mapOf(
                "card-full" to ImportDocuments.encode(StoredTransaction(card, "current-1")),
                "bank-minimal" to ImportDocuments.encode(StoredTransaction(bank)),
                "waiting-fx" to ImportDocuments.encode(StoredTransaction(waiting.copy(source = Source.ONE_ZERO))),
            ),
            ImportDocuments.SOURCE_CLOSURES to mapOf(
                "closed" to ImportDocuments.encodeSourceClosure(com.menash.myself.feature.money.StatementSource(Source.MAX, "max-1234"), YearMonth.of(2024, 6)),
            ),
            ImportDocuments.MERCHANT_MEMORY to mapOf(
                "learnt" to ImportDocuments.encodeMemory(key, "current-1", false),
                "own-transfer" to ImportDocuments.encodeMemory("העברה לחשבון", null, true),
                "alias-only" to ImportDocuments.encodeAlias(key, "סופר"),
                "ask-each-time" to ImportDocuments.encodeAskEachTime("ביט"),
                // remember, then alias, then ask-each-time (which deletes categoryId): the merged document.
                "merged" to ImportDocuments.encodeMemory(key, null, false) + ImportDocuments.encodeAlias(key, "x".repeat(ImportDocuments.MAX_ALIAS)) +
                    ImportDocuments.encodeAskEachTime(key),
            ),
        )
    }
}
