package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.Debt
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.FixedExpense
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.InstallmentPurchase
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.PeriodicBudget
import com.menash.myself.feature.money.domain.Saving
import com.menash.myself.feature.money.domain.Subscription
import com.menash.myself.feature.money.domain.TitheRate
import com.menash.myself.feature.money.domain.VariableBudget
import com.menash.myself.feature.money.domain.defaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/** Shared money fixtures. Today is 2026-10-27, the R2 fixed clock date. */
object MoneySamples {
    val today: LocalDate = LocalDate.of(2026, 10, 27)
    val month: YearMonth = YearMonth.of(2026, 10)

    fun shekels(value: Long) = Money(value * 100)
    fun history(value: Long) = AmountHistory(listOf(AmountChange(YearMonth.of(2026, 1), shekels(value))))

    val visa = Card("card-visa", "ויזה כאל", 10)
    val max = Card("card-max", "מקס", 2)

    val salary = Income("inc-salary", "משכורת", history(18_000), TitheRate.MAASER)
    val rent = FixedExpense("fx-rent", "שכר דירה", history(5_200), categoryId = "fixed-3")
    val carInsurance = FixedExpense(
        "fx-car", "ביטוח רכב", history(3_600), Frequency.YEARLY, card = max, billingMonth = Month.MARCH,
        klass = ExpenseClass.PERIODIC, categoryId = "periodic-8",
    )
    val netflix = Subscription("sub-netflix", "נטפליקס", history(55), renewalDate = LocalDate.of(2026, 1, 5), card = visa)
    val chatGpt = Subscription(
        "sub-gpt", "ChatGPT", history(20), currency = Currency.USD, renewalDate = LocalDate.of(2026, 1, 12), card = visa,
    )
    val food = VariableBudget("var-food", "מזון", history(3_000), card = visa, categoryId = "current-1")
    val fuel = VariableBudget("var-fuel", "דלק", history(800), card = max, categoryId = "current-3")
    val fridge = InstallmentPurchase("inst-fridge", "מקרר", shekels(6_000), YearMonth.of(2026, 9), 12, YearMonth.of(2026, 10), visa)
    val holidays = PeriodicBudget("per-holidays", "periodic-16", "יהדות / חגים", shekels(4_800), YearMonth.of(2026, 1))
    val studyLoan = Debt(
        "debt-study", "קרן הלוואות", shekels(24_000), LocalDate.of(2026, 9, 30), BigDecimal.ZERO, 24, shekels(1_000),
    )
    val pension = Saving("save-kids", "חיסכון לילדים", shekels(500), shekels(12_000), YearMonth.of(2030, 12))
    val checking = Asset("asset-checking", "עו\"ש", "bank", shekels(35_000), AssetKind.ASSET, true, LocalDate.of(2026, 10, 20))
    val oldFund = Asset("asset-fund", "קרן השתלמות", "fund", shekels(80_000), AssetKind.ASSET, false, LocalDate.of(2026, 5, 1))
    val overdraft = Asset("asset-od", "מינוס", "bank", shekels(2_000), AssetKind.LIABILITY, true, LocalDate.of(2026, 10, 20))

    val filled = MoneyData(
        householdReady = true,
        cards = listOf(visa, max),
        items = MoneyItems(
            incomes = listOf(salary),
            fixedExpenses = listOf(rent, carInsurance),
            subscriptions = listOf(netflix, chatGpt),
            variableBudgets = listOf(food, fuel),
            installments = listOf(fridge),
            periodicBudgets = listOf(holidays),
            debts = listOf(studyLoan),
            savings = listOf(pension),
        ),
        assets = listOf(checking, oldFund, overdraft),
        categories = defaultCategories(),
    )

    val empty = MoneyData(householdReady = true, categories = defaultCategories())

    val rate: BigDecimal = BigDecimal("3.72")
}

class FakeMoneyRepository(initial: MoneyData = MoneyData(householdReady = true)) : MoneyRepository {
    val data = MutableStateFlow(initial)
    val saved = mutableListOf<MoneyEntry>()
    val deleted = mutableListOf<Pair<MoneyCollection, String>>()
    val savedCategoryBatches = mutableListOf<List<Category>>()

    override fun observe(): Flow<MoneyData> = data

    override suspend fun save(entry: MoneyEntry) {
        saved += entry
    }

    override suspend fun delete(collection: MoneyCollection, id: String) {
        deleted += collection to id
    }

    override suspend fun saveCategories(categories: List<Category>) {
        savedCategoryBatches += categories
        data.value = data.value.copy(categories = data.value.categories + categories)
    }
}
