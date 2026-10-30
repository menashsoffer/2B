package com.menash.myself.feature.money.data

import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.ClosedMonth
import com.menash.myself.feature.money.domain.Debt
import com.menash.myself.feature.money.domain.Donation
import com.menash.myself.feature.money.domain.FixedExpense
import com.menash.myself.feature.money.domain.Goal
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.InstallmentPurchase
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.PensionFund
import com.menash.myself.feature.money.domain.PeriodicBudget
import com.menash.myself.feature.money.domain.Saving
import com.menash.myself.feature.money.domain.Subscription
import com.menash.myself.feature.money.domain.TitheSettings
import com.menash.myself.feature.money.domain.VariableBudget

/** Firestore subcollections under households/{hid}. The document id is the engine id. */
enum class MoneyCollection(val path: String) {
    CARDS("cards"),
    INCOMES("incomes"),
    FIXED_EXPENSES("fixedExpenses"),
    SUBSCRIPTIONS("subscriptions"),
    VARIABLE_BUDGETS("variableBudgets"),
    INSTALLMENTS("installments"),
    PERIODIC_BUDGETS("periodicBudgets"),
    DEBTS("debts"),
    SAVINGS("savings"),
    ASSETS("assets"),
    CATEGORIES("categories"),
    GOALS("goals"),
    DONATIONS("donations"),
    TITHE("tithe"),
    /** One document per closed month, id "yyyy-MM". */
    CLOSED_MONTHS("closedMonths"),
    PENSION_FUNDS("pensionFunds"),
}

/** Everything the money pillar stores for one household. [householdReady] is false while no household is active. */
data class MoneyData(
    val householdReady: Boolean = false,
    val cards: List<Card> = emptyList(),
    val items: MoneyItems = MoneyItems(),
    val assets: List<Asset> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** "האם אפשר?" goals: a trip, a car, a flat. */
    val goals: List<Goal> = emptyList(),
    val donations: List<Donation> = emptyList(),
    /** Null until the household sets where its maaser count starts. */
    val tithe: TitheSettings? = null,
    /** Frozen months, oldest first. */
    val closedMonths: List<ClosedMonth> = emptyList(),
    /** Pension, hishtalmut and gemel funds: a current snapshot, updated by hand from the quarterly report. */
    val pensionFunds: List<PensionFund> = emptyList(),
)

/** One stored document: an engine value and the collection it lives in. */
sealed interface MoneyEntry {
    val id: String
    val collection: MoneyCollection

    data class CardEntry(val value: Card) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.CARDS
    }
    data class IncomeEntry(val value: Income) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.INCOMES
    }
    data class FixedExpenseEntry(val value: FixedExpense) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.FIXED_EXPENSES
    }
    data class SubscriptionEntry(val value: Subscription) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.SUBSCRIPTIONS
    }
    data class VariableBudgetEntry(val value: VariableBudget) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.VARIABLE_BUDGETS
    }
    data class InstallmentEntry(val value: InstallmentPurchase) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.INSTALLMENTS
    }
    data class PeriodicBudgetEntry(val value: PeriodicBudget) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.PERIODIC_BUDGETS
    }
    data class DebtEntry(val value: Debt) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.DEBTS
    }
    data class SavingEntry(val value: Saving) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.SAVINGS
    }
    data class AssetEntry(val value: Asset) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.ASSETS
    }
    data class GoalEntry(val value: Goal) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.GOALS
    }
    data class DonationEntry(val value: Donation) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.DONATIONS
    }
    data class TitheEntry(val value: TitheSettings) : MoneyEntry {
        override val id get() = TitheSettings.ID
        override val collection get() = MoneyCollection.TITHE
    }
    data class ClosedMonthEntry(val value: ClosedMonth) : MoneyEntry {
        override val id get() = value.month.toString()
        override val collection get() = MoneyCollection.CLOSED_MONTHS
    }
    data class PensionFundEntry(val value: PensionFund) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.PENSION_FUNDS
    }
    data class CategoryEntry(val value: Category) : MoneyEntry {
        override val id get() = value.id
        override val collection get() = MoneyCollection.CATEGORIES
    }
}
