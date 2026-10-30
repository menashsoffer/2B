package com.menash.myself.feature.money.data

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.Debt
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.FixedExpense
import com.menash.myself.feature.money.domain.Goal
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.InstallmentPurchase
import com.menash.myself.feature.money.domain.PeriodicBudget
import com.menash.myself.feature.money.domain.Saving
import com.menash.myself.feature.money.domain.Subscription
import com.menash.myself.feature.money.domain.TitheRate
import com.menash.myself.feature.money.domain.VariableBudget
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/**
 * Firestore document <-> engine type. Pure Kotlin, so round trips are unit tested without Firestore.
 * Amounts are agorot (Long). Months are "YYYY-MM", dates "YYYY-MM-DD", enums their name.
 * Items refer to a card by "cardId"; [decode] resolves it from the household's cards.
 * The document id is not stored in the body.
 */
object MoneyDocuments {

    fun encode(entry: MoneyEntry): Map<String, Any> = when (entry) {
        is MoneyEntry.CardEntry -> entry.value.let { mapOf("name" to it.name, "billingDay" to it.billingDay.toLong()) }
        is MoneyEntry.IncomeEntry -> entry.value.let {
            mapOf("name" to it.name, "amounts" to encodeHistory(it.amounts), "titheRate" to it.titheRate.name)
        }
        is MoneyEntry.FixedExpenseEntry -> entry.value.let {
            buildMap {
                put("name", it.name)
                put("amounts", encodeHistory(it.amounts))
                put("frequency", it.frequency.name)
                put("klass", it.klass.name)
                it.card?.let { card -> put("cardId", card.id) }
                it.billingMonth?.let { month -> put("billingMonth", month.value.toLong()) }
                it.categoryId?.let { id -> put("categoryId", id) }
            }
        }
        is MoneyEntry.SubscriptionEntry -> entry.value.let {
            buildMap {
                put("name", it.name)
                put("amounts", encodeHistory(it.amounts))
                put("currency", it.currency.name)
                put("frequency", it.frequency.name)
                put("renewalDate", it.renewalDate.toString())
                put("klass", it.klass.name)
                it.card?.let { card -> put("cardId", card.id) }
                it.categoryId?.let { id -> put("categoryId", id) }
            }
        }
        is MoneyEntry.VariableBudgetEntry -> entry.value.let {
            buildMap {
                put("name", it.name)
                put("amounts", encodeHistory(it.amounts))
                put("klass", it.klass.name)
                it.card?.let { card -> put("cardId", card.id) }
                it.categoryId?.let { id -> put("categoryId", id) }
            }
        }
        is MoneyEntry.InstallmentEntry -> entry.value.let {
            buildMap {
                put("name", it.name)
                put("total", it.total.agorot)
                put("currentMonth", it.currentMonth.toString())
                put("numberOfPayments", it.numberOfPayments.toLong())
                put("firstMonth", it.firstMonth.toString())
                it.card?.let { card -> put("cardId", card.id) }
            }
        }
        is MoneyEntry.PeriodicBudgetEntry -> entry.value.let {
            mapOf(
                "categoryId" to it.categoryId,
                "name" to it.name,
                "annualAmount" to it.annualAmount.agorot,
                "yearStartMonth" to it.yearStartMonth.toString(),
            )
        }
        is MoneyEntry.DebtEntry -> entry.value.let {
            mapOf(
                "creditor" to it.creditor,
                "balance" to it.balance.agorot,
                "asOf" to it.asOf.toString(),
                "annualInterestPercent" to it.annualInterestPercent.toPlainString(),
                "remainingPayments" to it.remainingPayments.toLong(),
                "monthlyPayment" to it.monthlyPayment.agorot,
            )
        }
        is MoneyEntry.SavingEntry -> entry.value.let {
            buildMap {
                put("name", it.name)
                put("monthlyDeposit", it.monthlyDeposit.agorot)
                put("accumulated", it.accumulated.agorot)
                it.maturity?.let { month -> put("maturity", month.toString()) }
            }
        }
        is MoneyEntry.AssetEntry -> entry.value.let {
            mapOf(
                "name" to it.name,
                "type" to it.type,
                "balance" to it.balance.agorot,
                "kind" to it.kind.name,
                "liquid" to it.liquid,
                "lastUpdated" to it.lastUpdated.toString(),
            )
        }
        is MoneyEntry.GoalEntry -> entry.value.let {
            mapOf("name" to it.name, "amount" to it.amount.agorot, "targetMonth" to it.targetMonth.toString(), "allocated" to it.allocated.agorot) +
                listOfNotNull(it.newMonthlyCost?.let { cost -> "newMonthlyCost" to cost.agorot })
        }
        is MoneyEntry.DonationEntry -> TitheDocuments.encode(entry.value)
        is MoneyEntry.TitheEntry -> TitheDocuments.encode(entry.value)
        is MoneyEntry.CarEntry -> CarDocuments.encode(entry.value)
        is MoneyEntry.ClosedMonthEntry -> ClosedMonthDocuments.encode(entry.value)
        is MoneyEntry.CategoryEntry -> entry.value.let {
            buildMap {
                put("name", it.name)
                put("klass", it.klass.name)
                it.parentId?.let { id -> put("parentId", id) }
            }
        }
    }

    /** Throws [IllegalArgumentException] on a malformed document. [cards] resolves "cardId". */
    fun decode(
        collection: MoneyCollection,
        id: String,
        data: Map<String, Any?>,
        cards: Map<String, Card> = emptyMap(),
    ): MoneyEntry {
        val doc = MoneyDoc(data)
        val card = doc.stringOrNull("cardId")?.let { cards[it] }
        return when (collection) {
            MoneyCollection.CARDS -> MoneyEntry.CardEntry(Card(id, doc.string("name"), doc.int("billingDay")))
            MoneyCollection.INCOMES -> MoneyEntry.IncomeEntry(
                Income(
                    id = id,
                    name = doc.string("name"),
                    amounts = doc.history("amounts"),
                    titheRate = doc.enumOrNull("titheRate", TitheRate.entries) ?: TitheRate.NONE,
                )
            )
            MoneyCollection.FIXED_EXPENSES -> MoneyEntry.FixedExpenseEntry(
                FixedExpense(
                    id = id,
                    name = doc.string("name"),
                    amounts = doc.history("amounts"),
                    frequency = doc.enumOrNull("frequency", Frequency.entries) ?: Frequency.MONTHLY,
                    card = card,
                    billingMonth = doc.longOrNull("billingMonth")?.let { Month.of(it.toInt()) },
                    klass = doc.enumOrNull("klass", ExpenseClass.entries) ?: ExpenseClass.FIXED,
                    categoryId = doc.stringOrNull("categoryId"),
                )
            )
            MoneyCollection.SUBSCRIPTIONS -> MoneyEntry.SubscriptionEntry(
                Subscription(
                    id = id,
                    name = doc.string("name"),
                    amounts = doc.history("amounts"),
                    currency = doc.enumOrNull("currency", Currency.entries) ?: Currency.ILS,
                    frequency = doc.enumOrNull("frequency", Frequency.entries) ?: Frequency.MONTHLY,
                    renewalDate = LocalDate.parse(doc.string("renewalDate")),
                    card = card,
                    klass = doc.enumOrNull("klass", ExpenseClass.entries) ?: ExpenseClass.FIXED,
                    categoryId = doc.stringOrNull("categoryId"),
                )
            )
            MoneyCollection.VARIABLE_BUDGETS -> MoneyEntry.VariableBudgetEntry(
                VariableBudget(
                    id = id,
                    name = doc.string("name"),
                    amounts = doc.history("amounts"),
                    card = card,
                    klass = doc.enumOrNull("klass", ExpenseClass.entries) ?: ExpenseClass.CURRENT,
                    categoryId = doc.stringOrNull("categoryId"),
                )
            )
            MoneyCollection.INSTALLMENTS -> MoneyEntry.InstallmentEntry(
                InstallmentPurchase(
                    id = id,
                    name = doc.string("name"),
                    total = doc.money("total"),
                    currentMonth = YearMonth.parse(doc.string("currentMonth")),
                    numberOfPayments = doc.int("numberOfPayments"),
                    firstMonth = YearMonth.parse(doc.string("firstMonth")),
                    card = card,
                )
            )
            MoneyCollection.PERIODIC_BUDGETS -> MoneyEntry.PeriodicBudgetEntry(
                PeriodicBudget(
                    id = id,
                    categoryId = doc.string("categoryId"),
                    name = doc.string("name"),
                    annualAmount = doc.money("annualAmount"),
                    yearStartMonth = YearMonth.parse(doc.string("yearStartMonth")),
                )
            )
            MoneyCollection.DEBTS -> MoneyEntry.DebtEntry(
                Debt(
                    id = id,
                    creditor = doc.string("creditor"),
                    balance = doc.money("balance"),
                    asOf = LocalDate.parse(doc.string("asOf")),
                    annualInterestPercent = BigDecimal(doc.string("annualInterestPercent")),
                    remainingPayments = doc.int("remainingPayments"),
                    monthlyPayment = doc.money("monthlyPayment"),
                )
            )
            MoneyCollection.SAVINGS -> MoneyEntry.SavingEntry(
                Saving(
                    id = id,
                    name = doc.string("name"),
                    monthlyDeposit = doc.money("monthlyDeposit"),
                    accumulated = doc.money("accumulated"),
                    maturity = doc.stringOrNull("maturity")?.let { YearMonth.parse(it) },
                )
            )
            MoneyCollection.ASSETS -> MoneyEntry.AssetEntry(
                Asset(
                    id = id,
                    name = doc.string("name"),
                    type = doc.stringOrNull("type").orEmpty(),
                    balance = doc.money("balance"),
                    kind = doc.enumOrNull("kind", AssetKind.entries) ?: AssetKind.ASSET,
                    liquid = doc.boolean("liquid"),
                    lastUpdated = LocalDate.parse(doc.string("lastUpdated")),
                )
            )
            MoneyCollection.GOALS -> MoneyEntry.GoalEntry(
                Goal(
                    id, doc.string("name"), doc.money("amount"), YearMonth.parse(doc.string("targetMonth")),
                    Money(doc.longOrNull("allocated") ?: 0), doc.longOrNull("newMonthlyCost")?.let(::Money),
                )
            )
            MoneyCollection.DONATIONS -> MoneyEntry.DonationEntry(TitheDocuments.donation(id, doc))
            MoneyCollection.TITHE -> MoneyEntry.TitheEntry(TitheDocuments.settings(id, doc))
            MoneyCollection.CLOSED_MONTHS -> MoneyEntry.ClosedMonthEntry(ClosedMonthDocuments.decode(id, data))
            MoneyCollection.CAR -> MoneyEntry.CarEntry(CarDocuments.plan(id, data))
            MoneyCollection.CATEGORIES -> MoneyEntry.CategoryEntry(
                Category(
                    id = id,
                    name = doc.string("name"),
                    klass = doc.enumOrNull("klass", ExpenseClass.entries) ?: ExpenseClass.CURRENT,
                    parentId = doc.stringOrNull("parentId"),
                )
            )
        }
    }

    private fun encodeHistory(history: AmountHistory): Map<String, Any> = mapOf(
        "changes" to history.changes
            .sortedBy { it.effectiveFrom }
            .map { mapOf("from" to it.effectiveFrom.toString(), "amount" to it.amount.agorot) },
        "overrides" to history.overrides.entries
            .sortedBy { it.key }
            .associate { (month, amount) -> month.toString() to amount.agorot },
    )
}
