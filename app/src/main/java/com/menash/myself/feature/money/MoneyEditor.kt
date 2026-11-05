package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.Asset
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.Debt
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.FixedExpense
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.Income
import com.menash.myself.feature.money.domain.InstallmentPurchase
import com.menash.myself.feature.money.domain.PeriodicBudget
import com.menash.myself.feature.money.domain.RecurringItem
import com.menash.myself.feature.money.domain.Saving
import com.menash.myself.feature.money.domain.Subscription
import com.menash.myself.feature.money.domain.VariableBudget
import com.menash.myself.feature.money.domain.amountFor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/**
 * Turns a draft into the documents to save. [scope] is needed only when an existing
 * recurring amount changed; without it the result is [EditResult.NeedsScope].
 * A periodic budget saved without a category gets its own PERIODIC category named like it.
 */
fun buildEntries(
    draft: ItemDraft,
    data: MoneyData,
    month: YearMonth,
    today: LocalDate,
    scope: AmountScope?,
    newId: () -> String,
): EditResult {
    val name = draft.name.trim()
    if (name.isEmpty()) return EditResult.Invalid(DraftField.NAME)
    if (draft.kind == EditKind.CARD) {
        val day = draft.amount.trim().toIntOrNull()?.takeIf { it in 1..31 }
            ?: return EditResult.Invalid(DraftField.BILLING_DAY)
        return EditResult.Ok(listOf(MoneyEntry.CardEntry(Card(draft.id ?: newId(), name, day))))
    }
    val amount = parseAmount(draft.amount) ?: return EditResult.Invalid(DraftField.AMOUNT)
    val id = draft.id ?: newId()
    val card = draft.cardId?.let { cardId -> data.cards.firstOrNull { it.id == cardId } }
    val category = draft.categoryId?.let { categoryId -> data.categories.firstOrNull { it.id == categoryId } }

    fun history(existing: RecurringItem?): AmountHistory? {
        if (existing == null) return AmountHistory(listOf(AmountChange(month, amount)))
        if (amountFor(existing, month) == amount) return existing.amounts
        return scope?.let { withAmount(existing.amounts, month, amount, it) }
    }

    val entries: List<MoneyEntry> = when (draft.kind) {
        EditKind.INCOME -> {
            val existing = data.items.incomes.firstOrNull { it.id == id }
            val amounts = history(existing) ?: return EditResult.NeedsScope
            listOf(
                MoneyEntry.IncomeEntry(
                    existing?.copy(name = name, amounts = amounts, titheRate = draft.titheRate)
                        ?: Income(id, name, amounts, draft.titheRate)
                )
            )
        }
        EditKind.FIXED -> {
            val existing = data.items.fixedExpenses.firstOrNull { it.id == id }
            val amounts = history(existing) ?: return EditResult.NeedsScope
            listOf(
                MoneyEntry.FixedExpenseEntry(
                    FixedExpense(
                        id = id,
                        name = name,
                        amounts = amounts,
                        frequency = draft.frequency,
                        card = card,
                        billingMonth = if (draft.frequency == Frequency.YEARLY) draft.billingMonth ?: month.month else null,
                        klass = category?.klass ?: ExpenseClass.FIXED,
                        categoryId = category?.id,
                    )
                )
            )
        }
        EditKind.SUBSCRIPTION -> {
            val existing = data.items.subscriptions.firstOrNull { it.id == id }
            val amounts = history(existing) ?: return EditResult.NeedsScope
            listOf(
                MoneyEntry.SubscriptionEntry(
                    Subscription(
                        id = id,
                        name = name,
                        amounts = amounts,
                        currency = draft.currency,
                        frequency = draft.frequency,
                        renewalDate = draft.renewalDate ?: today,
                        card = card,
                        klass = category?.klass ?: ExpenseClass.FIXED,
                        categoryId = category?.id,
                    )
                )
            )
        }
        EditKind.VARIABLE -> {
            val existing = data.items.variableBudgets.firstOrNull { it.id == id }
            val amounts = history(existing) ?: return EditResult.NeedsScope
            listOf(
                MoneyEntry.VariableBudgetEntry(
                    VariableBudget(
                        id = id,
                        name = name,
                        amounts = amounts,
                        card = card,
                        klass = category?.klass ?: ExpenseClass.CURRENT,
                        categoryId = category?.id,
                    )
                )
            )
        }
        EditKind.PERIODIC -> {
            val own = if (category == null) Category(newId(), name, ExpenseClass.PERIODIC) else null
            val budget = PeriodicBudget(
                id = id,
                categoryId = category?.id ?: own!!.id,
                name = name,
                annualAmount = amount,
                yearStartMonth = draft.firstMonth ?: YearMonth.of(month.year, Month.JANUARY),
            )
            listOfNotNull(own?.let { MoneyEntry.CategoryEntry(it) }, MoneyEntry.PeriodicBudgetEntry(budget))
        }
        EditKind.INSTALLMENT -> {
            val payments = draft.payments.trim().toIntOrNull()?.takeIf { it > 0 }
                ?: return EditResult.Invalid(DraftField.PAYMENTS)
            val existing = data.items.installments.firstOrNull { it.id == id }
            listOf(
                MoneyEntry.InstallmentEntry(
                    InstallmentPurchase(
                        id = id,
                        name = name,
                        total = amount,
                        currentMonth = existing?.currentMonth ?: month,
                        numberOfPayments = payments,
                        firstMonth = draft.firstMonth ?: month.plusMonths(1),
                        card = card,
                    )
                )
            )
        }
        EditKind.DEBT -> {
            val remaining = draft.remainingPayments.trim().toIntOrNull()?.takeIf { it > 0 }
                ?: return EditResult.Invalid(DraftField.REMAINING_PAYMENTS)
            val existing = data.items.debts.firstOrNull { it.id == id }
            val balance = parseAmount(draft.balance) ?: existing?.balance ?: Money.ZERO
            // Untouched balance and payments left keep the stored count and its date, so editing only the
            // name or the payment does not restart the count. Otherwise the sheet's numbers are from this month
            // on: payments start the month after asOf, so asOf is the end of last month.
            val untouched = existing != null && existing.balance == balance && paymentsLeft(existing, month) == remaining
            listOf(
                MoneyEntry.DebtEntry(
                    Debt(
                        id = id,
                        creditor = name,
                        balance = balance,
                        asOf = if (untouched) existing!!.asOf else month.minusMonths(1).atEndOfMonth(),
                        annualInterestPercent = existing?.annualInterestPercent ?: BigDecimal.ZERO,
                        remainingPayments = if (untouched) existing!!.remainingPayments else remaining,
                        monthlyPayment = amount,
                    )
                )
            )
        }
        EditKind.SAVING -> listOf(
            MoneyEntry.SavingEntry(
                Saving(
                    id = id,
                    name = name,
                    monthlyDeposit = amount,
                    accumulated = parseAmount(draft.accumulated) ?: Money.ZERO,
                    maturity = draft.maturity,
                    startMonth = data.items.savings.firstOrNull { it.id == id }?.let { it.startMonth }
                        ?: if (data.items.savings.any { it.id == id }) null else month,
                )
            )
        )
        EditKind.ASSET -> {
            val existing = data.assets.firstOrNull { it.id == id }
            listOf(
                MoneyEntry.AssetEntry(
                    Asset(
                        id = id,
                        name = name,
                        type = existing?.type.orEmpty(),
                        balance = amount,
                        kind = draft.assetKind,
                        liquid = draft.liquid,
                        lastUpdated = if (existing != null && existing.balance == amount) existing.lastUpdated else today,
                    )
                )
            )
        }
        EditKind.CARD -> error("handled above")
    }
    return EditResult.Ok(entries)
}

