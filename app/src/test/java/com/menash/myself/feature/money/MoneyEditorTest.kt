package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.AmountHistory
import com.menash.myself.feature.money.domain.installmentSchedule
import com.menash.myself.feature.money.domain.monthlyReserve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class MoneyEditorTest {

    private val month = MoneySamples.month

    @Test
    fun parseAmount_acceptsCommasDecimalsAndShekelSign() {
        assertEquals(Money(125_000), parseAmount("1,250"))
        assertEquals(Money(125_050), parseAmount(" ₪1250.5 "))
        assertEquals(Money(0), parseAmount("0"))
        assertNull(parseAmount(""))
        assertNull(parseAmount("-5"))
        assertNull(parseAmount("שלוש"))
    }

    @Test
    fun amountText_isWhatTheFieldShows() {
        assertEquals("1250", amountText(Money(125_000)))
        assertEquals("1250.5", amountText(Money(125_050)))
        assertEquals("0", amountText(Money(0)))
    }

    @Test
    fun fromNow_replacesAChangeInTheSameMonthAndDropsThisMonthsOverride() {
        val history = AmountHistory(
            changes = listOf(AmountChange(YearMonth.of(2026, 1), Money(100)), AmountChange(month, Money(200))),
            overrides = mapOf(month to Money(50)),
        )
        val updated = withAmount(history, month, Money(300), AmountScope.FROM_NOW)
        assertEquals(listOf(AmountChange(YearMonth.of(2026, 1), Money(100)), AmountChange(month, Money(300))), updated.changes)
        assertEquals(emptyMap<YearMonth, Money>(), updated.overrides)
    }

    @Test
    fun thisMonth_keepsTheDefaults() {
        val history = MoneySamples.history(500)
        val updated = withAmount(history, month, Money(0), AmountScope.THIS_MONTH)
        assertEquals(history.changes, updated.changes)
        assertEquals(mapOf(month to Money(0)), updated.overrides)
    }

    @Test
    fun preview_periodicReserveIsTheEngines() {
        val draft = ItemDraft(EditKind.PERIODIC, name = "חגים", amount = "4800")
        assertEquals(monthlyReserve(MoneySamples.holidays), draftPreview(draft, month).monthlyReserve)
    }

    @Test
    fun preview_installmentSplitIsTheEngines() {
        val draft = ItemDraft(EditKind.INSTALLMENT, name = "מקרר", amount = "1000", payments = "3", firstMonth = month)
        val schedule = installmentSchedule(
            com.menash.myself.feature.money.domain.InstallmentPurchase("x", "x", Money(100_000), month, 3, month),
        )
        val preview = draftPreview(draft, month)
        assertEquals(schedule[0].amount, preview.firstPayment)
        assertEquals(schedule[1].amount, preview.laterPayment)
        assertEquals(Money(33_334), preview.firstPayment)
    }

    @Test
    fun draftOf_thenBuild_roundTripsWithoutQuestions() {
        listOf(
            MoneyEntry.IncomeEntry(MoneySamples.salary),
            MoneyEntry.FixedExpenseEntry(MoneySamples.carInsurance),
            MoneyEntry.SubscriptionEntry(MoneySamples.chatGpt),
            MoneyEntry.VariableBudgetEntry(MoneySamples.food),
            MoneyEntry.InstallmentEntry(MoneySamples.fridge),
            MoneyEntry.PeriodicBudgetEntry(MoneySamples.holidays),
            MoneyEntry.SavingEntry(MoneySamples.pension),
            MoneyEntry.DebtEntry(MoneySamples.studyLoan),
            MoneyEntry.AssetEntry(MoneySamples.checking),
            MoneyEntry.CardEntry(MoneySamples.visa),
        ).forEach { entry ->
            val result = buildEntries(
                draftOf(entry, month, MoneySamples.today), MoneySamples.filled, month, MoneySamples.today, null,
            ) { error("no new id for an edit") }
            assertEquals(entry.id, EditResult.Ok(listOf(entry)), result)
        }
    }

    @Test
    fun debt_paymentsLeftCountFromThisMonth() {
        val loan = MoneySamples.studyLoan
        assertEquals(24, paymentsLeft(loan, month))
        assertEquals(22, paymentsLeft(loan, month.plusMonths(2)))
        assertEquals(24, paymentsLeft(loan, YearMonth.of(2026, 9)))
        assertEquals(0, paymentsLeft(loan, month.plusYears(5)))
    }

    @Test
    fun debt_editingOnlyThePaymentKeepsTheCount() {
        val december = month.plusMonths(2)
        val draft = draftOf(MoneyEntry.DebtEntry(MoneySamples.studyLoan), december, MoneySamples.today)
        assertEquals("22", draft.remainingPayments)
        val result = buildEntries(draft.copy(amount = "900"), MoneySamples.filled, december, MoneySamples.today, null) { error("edit") }
        val saved = ((result as EditResult.Ok).entries.single() as MoneyEntry.DebtEntry).value
        assertEquals(MoneySamples.studyLoan.copy(monthlyPayment = Money(90_000)), saved)
    }

    @Test
    fun debt_newBalanceOrCountStartsFromThisMonth() {
        val december = month.plusMonths(2)
        val draft = draftOf(MoneyEntry.DebtEntry(MoneySamples.studyLoan), december, MoneySamples.today).copy(remainingPayments = "20")
        val saved = ((buildEntries(draft, MoneySamples.filled, december, MoneySamples.today, null) { error("edit") } as EditResult.Ok)
            .entries.single() as MoneyEntry.DebtEntry).value
        assertEquals(java.time.LocalDate.of(2026, 11, 30), saved.asOf)
        assertEquals(20, saved.remainingPayments)
        assertEquals(20, paymentsLeft(saved, december))
    }
}
