package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.InstallmentPurchase
import com.menash.myself.feature.money.domain.PeriodicBudget
import com.menash.myself.feature.money.domain.RecurringItem
import com.menash.myself.feature.money.domain.amountFor
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

enum class EditKind { INCOME, FIXED, SUBSCRIPTION, VARIABLE, PERIODIC, INSTALLMENT, DEBT, SAVING, ASSET, CARD }

/** "מעכשיו והלאה" adds a default from this month; "רק החודש" overrides this month only. */
enum class AmountScope { FROM_NOW, THIS_MONTH }

/**
 * What the add/edit sheet holds. Text fields stay text until save, so the sheet never
 * rewrites what the user typed. Only [name] and [amount] are required; the rest has defaults.
 * [amount] is the monthly amount, the annual amount for a periodic budget, the total for an
 * installment purchase, the monthly payment for a debt, the monthly deposit for a saving and
 * the balance for an asset. For a card it is the billing day.
 */
data class ItemDraft(
    val kind: EditKind,
    val id: String? = null,
    val name: String = "",
    val amount: String = "",
    val frequency: Frequency = Frequency.MONTHLY,
    val billingMonth: Month? = null,
    val currency: Currency = Currency.ILS,
    val renewalDate: LocalDate? = null,
    val cardId: String? = null,
    val categoryId: String? = null,
    val payments: String = "1",
    val firstMonth: YearMonth? = null,
    val balance: String = "",
    val remainingPayments: String = "",
    val accumulated: String = "",
    val maturity: YearMonth? = null,
    val liquid: Boolean = true,
    val assetKind: AssetKind = AssetKind.ASSET,
)

sealed interface EditResult {
    data class Ok(val entries: List<MoneyEntry>) : EditResult
    /** An existing amount changed: ask once, "from now on" or "this month only". */
    data object NeedsScope : EditResult
    data class Invalid(val field: DraftField) : EditResult
}

enum class DraftField { NAME, AMOUNT, PAYMENTS, REMAINING_PAYMENTS, BILLING_DAY }

/** "1,250.50" or "1250.5" to agorot; null when it is not a nonnegative number. */
fun parseAmount(text: String): Money? {
    val cleaned = text.trim().replace(",", "").replace("₪", "").replace(" ", "")
    if (cleaned.isEmpty()) return null
    val value = cleaned.toBigDecimalOrNull() ?: return null
    if (value < BigDecimal.ZERO) return null
    return Money(value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact())
}

/** Agorot to the text the amount field shows ("1250", "1250.5"). */
fun amountText(money: Money): String =
    BigDecimal.valueOf(money.agorot, 2).stripTrailingZeros().toPlainString()

fun withAmount(history: AmountHistory, month: YearMonth, amount: Money, scope: AmountScope): AmountHistory =
    when (scope) {
        AmountScope.FROM_NOW -> AmountHistory(
            changes = history.changes.filter { it.effectiveFrom != month } + AmountChange(month, amount),
            overrides = history.overrides - month,
        )
        AmountScope.THIS_MONTH -> history.copy(overrides = history.overrides + (month to amount))
    }

/** A new draft with every default filled: card = last used, payments = 1, first payment = next month. */
fun newDraft(kind: EditKind, month: YearMonth, today: LocalDate, lastCardId: String?): ItemDraft = ItemDraft(
    kind = kind,
    cardId = lastCardId,
    billingMonth = month.month,
    renewalDate = today,
    firstMonth = month.plusMonths(1),
)

/** The draft the sheet opens with for an existing entry. */
fun draftOf(entry: MoneyEntry, month: YearMonth, today: LocalDate): ItemDraft {
    fun recurring(kind: EditKind, item: RecurringItem, name: String) =
        newDraft(kind, month, today, null).copy(
            id = item.id,
            name = name,
            amount = amountFor(item, month)?.let(::amountText).orEmpty(),
        )
    return when (entry) {
        is MoneyEntry.IncomeEntry -> recurring(EditKind.INCOME, entry.value, entry.value.name)
        is MoneyEntry.FixedExpenseEntry -> recurring(EditKind.FIXED, entry.value, entry.value.name).copy(
            frequency = entry.value.frequency,
            billingMonth = entry.value.billingMonth ?: month.month,
            cardId = entry.value.card?.id,
            categoryId = entry.value.categoryId,
        )
        is MoneyEntry.SubscriptionEntry -> recurring(EditKind.SUBSCRIPTION, entry.value, entry.value.name).copy(
            frequency = entry.value.frequency,
            currency = entry.value.currency,
            renewalDate = entry.value.renewalDate,
            cardId = entry.value.card?.id,
            categoryId = entry.value.categoryId,
        )
        is MoneyEntry.VariableBudgetEntry -> recurring(EditKind.VARIABLE, entry.value, entry.value.name).copy(
            cardId = entry.value.card?.id,
            categoryId = entry.value.categoryId,
        )
        is MoneyEntry.PeriodicBudgetEntry -> newDraft(EditKind.PERIODIC, month, today, null).copy(
            id = entry.value.id,
            name = entry.value.name,
            amount = amountText(entry.value.annualAmount),
            categoryId = entry.value.categoryId,
            firstMonth = entry.value.yearStartMonth,
        )
        is MoneyEntry.InstallmentEntry -> newDraft(EditKind.INSTALLMENT, month, today, null).copy(
            id = entry.value.id,
            name = entry.value.name,
            amount = amountText(entry.value.total),
            payments = entry.value.numberOfPayments.toString(),
            firstMonth = entry.value.firstMonth,
            cardId = entry.value.card?.id,
        )
        is MoneyEntry.DebtEntry -> newDraft(EditKind.DEBT, month, today, null).copy(
            id = entry.value.id,
            name = entry.value.creditor,
            amount = amountText(entry.value.monthlyPayment),
            balance = amountText(entry.value.balance),
            remainingPayments = entry.value.remainingPayments.toString(),
        )
        is MoneyEntry.SavingEntry -> newDraft(EditKind.SAVING, month, today, null).copy(
            id = entry.value.id,
            name = entry.value.name,
            amount = amountText(entry.value.monthlyDeposit),
            accumulated = amountText(entry.value.accumulated),
            maturity = entry.value.maturity,
        )
        is MoneyEntry.AssetEntry -> newDraft(EditKind.ASSET, month, today, null).copy(
            id = entry.value.id,
            name = entry.value.name,
            amount = amountText(entry.value.balance),
            liquid = entry.value.liquid,
            assetKind = entry.value.kind,
        )
        is MoneyEntry.CardEntry -> newDraft(EditKind.CARD, month, today, null).copy(
            id = entry.value.id,
            name = entry.value.name,
            amount = entry.value.billingDay.toString(),
        )
        is MoneyEntry.CategoryEntry -> error("Categories are picked, not edited in this sheet")
    }
}

/** Numbers the sheet shows before saving, straight from the engine. */
data class DraftPreview(
    /** Periodic budget: what the plan reserves each month. */
    val monthlyReserve: Money? = null,
    /** Installment purchase: first payment and the payment after it (null when there is one payment). */
    val firstPayment: Money? = null,
    val laterPayment: Money? = null,
)

fun draftPreview(draft: ItemDraft, month: YearMonth): DraftPreview {
    val amount = parseAmount(draft.amount) ?: return DraftPreview()
    return when (draft.kind) {
        EditKind.PERIODIC -> DraftPreview(
            monthlyReserve = com.menash.myself.feature.money.domain.monthlyReserve(
                PeriodicBudget("preview", "preview", draft.name.ifBlank { "preview" }, amount, YearMonth.of(month.year, Month.JANUARY))
            ),
        )
        EditKind.INSTALLMENT -> {
            val payments = draft.payments.trim().toIntOrNull()?.takeIf { it > 0 } ?: return DraftPreview()
            val schedule = com.menash.myself.feature.money.domain.installmentSchedule(
                InstallmentPurchase("preview", "preview", amount, month, payments, draft.firstMonth ?: month.plusMonths(1))
            )
            DraftPreview(firstPayment = schedule.firstOrNull()?.amount, laterPayment = schedule.getOrNull(1)?.amount)
        }
        else -> DraftPreview()
    }
}
