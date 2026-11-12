package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyData
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.AmountChange
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MoneyItems
import com.menash.myself.feature.money.domain.MonthPlan
import com.menash.myself.feature.money.domain.cushionMonths
import com.menash.myself.feature.money.domain.defaultCategories
import com.menash.myself.feature.money.domain.monthPlan
import com.menash.myself.feature.money.domain.netWorth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyViewModelTest {

    private val month = MoneySamples.month

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeMoneyRepository, rate: BigDecimal? = MoneySamples.rate): MoneyViewModel {
        var next = 0
        return MoneyViewModel(
            repository = repository,
            usdRates = MutableStateFlow(rate),
            today = { MoneySamples.today },
            errorReporter = DefaultErrorReporter(),
            newId = { "new-${++next}" },
        )
    }

    @Test
    fun monthNavigation_stopsAtTodayAndKeepsTodaysBigPicture() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled))
        val today = vm.state.value.money!!
        assertEquals(MoneySamples.month, today.month)
        assertFalse(today.canGoNext)
        vm.showNextMonth()
        assertEquals(MoneySamples.month, vm.state.value.money!!.month)
        vm.showPreviousMonth()
        val september = vm.state.value.money!!
        assertEquals(MoneySamples.month.minusMonths(1), september.month)
        assertEquals(MoneySamples.month, september.current)
        assertTrue(september.canGoNext)
        assertEquals(today.bigPicture, september.bigPicture)
        assertEquals(buildMoneyMonth(MoneySamples.filled, september.month, MoneySamples.today, MoneySamples.rate).view, september.view)
        vm.showNextMonth()
        assertEquals(today, vm.state.value.money)
        repeat(40) { vm.showPreviousMonth() }
        assertEquals(MoneySamples.month.minusMonths(12), vm.state.value.money!!.month)
        assertFalse(vm.state.value.money!!.canGoBack)
    }

    @Test
    fun noItems_isTheEmptyMonth() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.empty))
        val money = assertNotNull(vm.state.value.money).let { vm.state.value.money!! }
        assertEquals(MonthView.Empty(usdRateMissing = false), money.view)
        assertNull("no assets, no invented net worth", money.bigPicture.netWorth)
        assertNull(money.bigPicture.cushionMonths)
    }

    @Test
    fun filled_numbersAreExactlyTheEngine() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled))
        val view = vm.state.value.money!!.view as MonthView.Planned
        val engine = monthPlan(month, MoneySamples.filled.items, MoneySamples.rate) as MonthPlan.Planned
        assertEquals(engine, view.plan)
        assertFalse(view.usdRateMissing)
        val totals = view.rows.associate { it.kind to it.total }
        assertEquals(engine.income, totals[MonthRowKind.INCOME])
        assertEquals(engine.byClass[ExpenseClass.FIXED], totals[MonthRowKind.FIXED])
        assertEquals(engine.byClass[ExpenseClass.CURRENT], totals[MonthRowKind.CURRENT])
        assertEquals(engine.byClass[ExpenseClass.PERIODIC], totals[MonthRowKind.PERIODIC])
        assertEquals(engine.installments, totals[MonthRowKind.INSTALLMENTS])
        assertEquals(engine.debtPayments, totals[MonthRowKind.DEBTS])
        assertEquals(engine.savingDeposits, totals[MonthRowKind.SAVINGS])
        assertEquals(engine.cardCharges, view.cardCharges)

        // Paamonim grouping: the yearly car insurance (PERIODIC class) sits with the periodic reserve.
        val periodic = view.rows.first { it.kind == MonthRowKind.PERIODIC }.items.map { it.entry.id }
        assertEquals(listOf("fx-car", "per-holidays"), periodic)
        // Hand-checked against the fixtures: 3600/12 + 4800/12 = 700.
        assertEquals(MoneySamples.shekels(700), totals[MonthRowKind.PERIODIC])

        val bigPicture = vm.state.value.money!!.bigPicture
        assertEquals(netWorth(MoneySamples.filled.assets), bigPicture.netWorth)
        assertEquals(cushionMonths(MoneySamples.filled.assets, engine), bigPicture.cushionMonths)
        assertEquals(listOf(false, true, false), bigPicture.assets.map { it.stale })
    }

    @Test
    fun itemAmountsAreTheEngineForThatItemAlone() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled))
        val view = vm.state.value.money!!.view as MonthView.Planned
        val gpt = view.rows.flatMap { it.items }.first { it.entry.id == "sub-gpt" }
        val alone = monthPlan(month, MoneyItems(subscriptions = listOf(MoneySamples.chatGpt)), MoneySamples.rate) as MonthPlan.Planned
        assertEquals(alone.subscriptions, gpt.monthly)
        assertNull("actual stays empty until import", gpt.actual)
    }

    @Test
    fun missingUsdRate_excludesUsdSubscriptionsAndRaisesTheChip() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled), rate = null)
        val view = vm.state.value.money!!.view as MonthView.Planned
        assertTrue(view.usdRateMissing)
        val withoutUsd = MoneySamples.filled.items.copy(
            subscriptions = MoneySamples.filled.items.subscriptions.filter { it.currency != Currency.USD },
        )
        assertEquals(monthPlan(month, withoutUsd, null), view.plan)
        val gpt = view.rows.flatMap { it.items }.first { it.entry.id == "sub-gpt" }
        assertNull(gpt.monthly)
    }

    @Test
    fun onlyUsdSubscriptionsWithoutRate_staysEmptyWithTheChip() {
        val data = MoneySamples.empty.copy(items = MoneyItems(subscriptions = listOf(MoneySamples.chatGpt)))
        val vm = viewModel(FakeMoneyRepository(data), rate = null)
        assertEquals(MonthView.Empty(usdRateMissing = true), vm.state.value.money!!.view)
    }

    @Test
    fun amountEdit_fromNowOnWritesAChange() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startEditing(MoneyEntry.IncomeEntry(MoneySamples.salary))
        vm.updateDraft { it.copy(amount = "19500") }
        vm.save()
        assertTrue("the one question is asked", vm.editor.value!!.askScope)
        assertTrue(repository.saved.isEmpty())
        vm.save(AmountScope.FROM_NOW)
        val saved = (repository.saved.single() as MoneyEntry.IncomeEntry).value
        assertTrue(AmountChange(month, MoneySamples.shekels(19_500)) in saved.amounts.changes)
        assertTrue(saved.amounts.overrides.isEmpty())
        assertNull(vm.editor.value)
    }

    @Test
    fun amountEdit_thisMonthOnlyWritesAnOverride() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startEditing(MoneyEntry.VariableBudgetEntry(MoneySamples.food))
        vm.updateDraft { it.copy(amount = "3,400") }
        vm.save(AmountScope.THIS_MONTH)
        val saved = (repository.saved.single() as MoneyEntry.VariableBudgetEntry).value
        assertEquals(MoneySamples.food.amounts.changes, saved.amounts.changes)
        assertEquals(mapOf(month to MoneySamples.shekels(3_400)), saved.amounts.overrides)
    }

    @Test
    fun editWithoutAmountChange_doesNotAsk() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startEditing(MoneyEntry.FixedExpenseEntry(MoneySamples.rent))
        vm.updateDraft { it.copy(name = "שכירות") }
        vm.save()
        val saved = (repository.saved.single() as MoneyEntry.FixedExpenseEntry).value
        assertEquals("שכירות", saved.name)
        assertEquals(MoneySamples.rent.amounts, saved.amounts)
    }

    @Test
    fun subscriptionFromACharge_opensClean() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled))
        vm.startSubscriptionFrom(UnlistedCharge("netflix", "נטפליקס", "sub-1", LastCharge(month, Money(4_900)), 3))
        val editor = vm.editor.value!!
        assertEquals("נטפליקס", editor.draft.name)
        assertFalse(editor.isDirty())
    }

    @Test
    fun newItem_onlyNameAndAmount_defaultsFilled() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startAdding(EditKind.INSTALLMENT)
        val draft = vm.editor.value!!.draft
        assertEquals("1", draft.payments)
        assertEquals(month.plusMonths(1), draft.firstMonth)
        assertEquals("the last card", MoneySamples.max.id, draft.cardId)
        vm.updateDraft { it.copy(name = "ספה", amount = "4500") }
        vm.save()
        val saved = (repository.saved.single() as MoneyEntry.InstallmentEntry).value
        assertEquals("new-1", saved.id)
        assertEquals(Money(450_000), saved.total)
        assertEquals(1, saved.numberOfPayments)
        assertEquals(month.plusMonths(1), saved.firstMonth)
        assertEquals(MoneySamples.max, saved.card)
    }

    @Test
    fun lastUsedCardIsTheNextDefault() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startAdding(EditKind.VARIABLE)
        vm.updateDraft { it.copy(name = "פארם", amount = "300", cardId = MoneySamples.visa.id) }
        vm.save()
        vm.startAdding(EditKind.FIXED)
        assertEquals(MoneySamples.visa.id, vm.editor.value!!.draft.cardId)
    }

    @Test
    fun newCardFromAnItemSheet_keepsTheDraftAndPicksTheCard() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startAdding(EditKind.FIXED)
        vm.updateDraft { it.copy(name = "חוג", amount = "250") }
        vm.startNewCard()
        assertEquals(EditKind.CARD, vm.editor.value!!.draft.kind)
        vm.updateDraft { it.copy(name = "אמקס", amount = "15") }
        vm.save()
        val back = vm.editor.value!!
        assertEquals(EditKind.FIXED, back.draft.kind)
        assertEquals("חוג", back.draft.name)
        assertEquals("new-1", back.draft.cardId)
        vm.save()
        val item = repository.saved.filterIsInstance<MoneyEntry.FixedExpenseEntry>().single().value
        assertEquals("new-1", item.card?.id)
        assertEquals(null, vm.editor.value)
    }

    @Test
    fun closingTheNewCardSheet_returnsToTheItemUnchanged() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled))
        vm.startAdding(EditKind.VARIABLE)
        vm.updateDraft { it.copy(name = "פארם") }
        val before = vm.editor.value!!.draft
        vm.startNewCard()
        vm.dismissEditor()
        assertEquals(before, vm.editor.value!!.draft)
        vm.dismissEditor()
        assertEquals(null, vm.editor.value)
    }

    @Test
    fun categorySetsTheClass() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startAdding(EditKind.FIXED)
        vm.updateDraft { it.copy(name = "חשמל", amount = "450", categoryId = "periodic-3") }
        vm.save()
        val saved = (repository.saved.single() as MoneyEntry.FixedExpenseEntry).value
        assertEquals(ExpenseClass.PERIODIC, saved.klass)
        assertEquals("periodic-3", saved.categoryId)
    }

    @Test
    fun periodicWithoutCategory_getsItsOwnPeriodicCategory() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startAdding(EditKind.PERIODIC)
        vm.updateDraft { it.copy(name = "ספרי לימוד", amount = "1200") }
        vm.save()
        val category = (repository.saved[0] as MoneyEntry.CategoryEntry).value
        val budget = (repository.saved[1] as MoneyEntry.PeriodicBudgetEntry).value
        assertEquals(ExpenseClass.PERIODIC, category.klass)
        assertEquals("ספרי לימוד", category.name)
        assertEquals(category.id, budget.categoryId)
        assertEquals(MoneySamples.shekels(1_200), budget.annualAmount)
    }

    @Test
    fun missingNameOrAmount_isAFieldError() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startAdding(EditKind.INCOME)
        vm.save()
        assertEquals(DraftField.NAME, vm.editor.value!!.error)
        vm.updateDraft { it.copy(name = "בונוס", amount = "abc") }
        vm.save()
        assertEquals(DraftField.AMOUNT, vm.editor.value!!.error)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun delete_removesTheDocument() {
        val repository = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repository)
        vm.startEditing(MoneyEntry.SavingEntry(MoneySamples.pension))
        vm.delete(vm.editingEntry()!!)
        assertEquals(listOf(com.menash.myself.feature.money.data.MoneyCollection.SAVINGS to "save-kids"), repository.deleted)
    }

    @Test
    fun categoriesAreSeededOnce() {
        val repository = FakeMoneyRepository(MoneyData(householdReady = true))
        viewModel(repository)
        assertEquals(listOf(defaultCategories()), repository.savedCategoryBatches)
        // A second screen (or a second load) finds them and writes nothing.
        viewModel(repository)
        repository.data.value = repository.data.value.copy(cards = listOf(MoneySamples.visa))
        assertEquals(1, repository.savedCategoryBatches.size)
        assertEquals(defaultCategories().size, repository.data.value.categories.size)
    }

    @Test
    fun noHousehold_seedsNothing() {
        val repository = FakeMoneyRepository(MoneyData(householdReady = false))
        viewModel(repository)
        assertTrue(repository.savedCategoryBatches.isEmpty())
    }
}
