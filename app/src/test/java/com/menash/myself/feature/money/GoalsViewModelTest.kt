package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.MoneySamples.trip
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.data.MoneyEntry
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
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {

    private val today = LocalDate.of(2026, 10, 30)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repo: FakeMoneyRepository) =
        GoalsViewModel(repo, MutableStateFlow<BigDecimal?>(MoneySamples.rate), { today }, DefaultErrorReporter()) { "goal-new" }

    @Test
    fun opens_withEachStoredGoalAnswered_andCloses() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled.copy(goals = listOf(trip))))
        assertFalse(vm.ui.value.open)
        vm.open()
        assertTrue(vm.ui.value.open)
        assertEquals(RateBasis.PLAN, vm.ui.value.view.basis)
        assertEquals(listOf(trip.id), vm.ui.value.view.goals.map { it.goal.id })
        vm.close()
        assertFalse(vm.ui.value.open)
    }

    @Test
    fun usdRateNotLoaded_goalsStillShow_andThePlanBasisIsWithheld() {
        val vm = GoalsViewModel(
            FakeMoneyRepository(MoneySamples.filled.copy(goals = listOf(trip))),
            MutableStateFlow<BigDecimal?>(null), { today }, DefaultErrorReporter(),
        ) { "goal-new" }
        vm.open()
        assertTrue(vm.ui.value.open)
        assertNull(vm.ui.value.view.basis)
        assertEquals(listOf(trip.id), vm.ui.value.view.goals.map { it.goal.id })
    }

    @Test
    fun typing_updatesTheLivePreview() {
        val vm = viewModel(FakeMoneyRepository(MoneySamples.filled))
        vm.startAdding()
        assertEquals(YearMonth.of(2027, 10), vm.ui.value.editor!!.draft.targetMonth)
        assertNull(vm.ui.value.editor!!.preview)
        vm.updateDraft { it.copy(name = "מחשב", amount = "4000") }
        assertNotNull(vm.ui.value.editor!!.preview)
    }

    @Test
    fun save_writesTheGoal_orShowsWhatIsMissing() {
        val repo = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repo)
        vm.startAdding()
        vm.save()
        assertEquals(GoalField.NAME, vm.ui.value.editor!!.error)
        assertTrue(repo.saved.isEmpty())

        vm.updateDraft { it.copy(name = "מחשב", amount = "4000") }
        assertNull(vm.ui.value.editor!!.error)
        vm.save()
        assertNull(vm.ui.value.editor)
        val saved = (repo.saved.single() as MoneyEntry.GoalEntry).value
        assertEquals("goal-new", saved.id)
        assertEquals("מחשב", saved.name)
    }

    @Test
    fun delete_asksFirst_thenRemovesTheGoal() {
        val repo = FakeMoneyRepository(MoneySamples.filled.copy(goals = listOf(trip)))
        val vm = viewModel(repo)
        vm.startEditing(trip)
        vm.requestDelete(true)
        assertTrue(vm.ui.value.editor!!.confirmDelete)
        vm.delete()
        assertNull(vm.ui.value.editor)
        assertEquals(listOf(MoneyCollection.GOALS to trip.id), repo.deleted)
    }
}
