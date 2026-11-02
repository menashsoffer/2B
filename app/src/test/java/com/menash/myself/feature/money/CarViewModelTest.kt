package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.data.MoneyEntry
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeMoneyRepository) =
        CarViewModel(repo, FakeTransactionRepository(), { LocalDate.of(2026, 10, 1) }, DefaultErrorReporter())

    @Test fun categoriesAreToggledAndSaved() {
        val repo = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repo)
        vm.open()
        assertTrue(vm.ui.value.open)
        vm.editCategories()
        val original = (vm.ui.value.sheet as CarSheet.Categories).selected
        vm.toggleCategory("test-category")
        assertEquals(original + "test-category", (vm.ui.value.sheet as CarSheet.Categories).selected)
        vm.saveCategories()
        assertEquals(original + "test-category", (repo.saved.single() as MoneyEntry.CarEntry).value.categoryIds)
        assertNull(vm.ui.value.sheet)
        vm.close()
        assertFalse(vm.ui.value.open)
    }

    @Test fun comparisonDraftCanBeEditedSavedAndDismissed() {
        val repo = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repo)
        vm.saveCompare()
        assertTrue(repo.saved.isEmpty())
        vm.editCompare()
        vm.updateCompare { it.copy(price = "125000", horizonYears = 4) }
        assertEquals("125000", (vm.ui.value.sheet as CarSheet.Compare).draft.price)
        vm.saveCompare()
        val saved = (repo.saved.single() as MoneyEntry.CarEntry).value.compare
        assertEquals("125000", saved.price)
        assertEquals(4, saved.horizonYears)
        vm.editCompare()
        vm.dismiss()
        assertNull(vm.ui.value.sheet)
    }
}
