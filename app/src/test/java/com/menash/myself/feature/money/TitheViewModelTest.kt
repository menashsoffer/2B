package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.domain.TitheSettings
import com.menash.myself.feature.money.importer.ImportFixtures
import com.menash.myself.feature.money.importer.StoredTransaction
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
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TitheViewModelTest {

    private val today = LocalDate.of(2026, 10, 30)
    private val october = YearMonth.of(2026, 10)
    private val withTithe = MoneySamples.filled.copy(donations = MoneySamples.donations, tithe = MoneySamples.tithe)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(money: FakeMoneyRepository, transactions: FakeTransactionRepository = FakeTransactionRepository()) =
        TitheViewModel(money, transactions, { today }, DefaultErrorReporter()) { "don-new" }

    @Test
    fun noStartMonth_noView_andTheFirstSettingsStartThisMonth() {
        val repo = FakeMoneyRepository(MoneySamples.filled)
        val vm = viewModel(repo)
        vm.open()
        assertTrue(vm.ui.value.open)
        assertNull(vm.ui.value.view)
        vm.startSettings()
        assertEquals(october, vm.ui.value.sheets.settings!!.startMonth)
        vm.updateSettings { it.copy(opening = "300") }
        vm.saveSettings()
        assertNull(vm.ui.value.sheets.settings)
        assertEquals(MoneyEntry.TitheEntry(TitheSettings(october, opening = shekels(300))), repo.saved.single())
        vm.close()
        assertFalse(vm.ui.value.open)
    }

    @Test
    fun theView_countsALinkedImportedIncome() {
        val transactions = FakeTransactionRepository()
        val rows = ImportFixtures.parse("hapoalim_oct.xlsx").rows.map { StoredTransaction(it) }
        val allowance = rows.single { it.row.description.contains("קצבת ילדים") }
        transactions.lines.value = rows.map { if (it.id == allowance.id) it.copy(categoryId = MoneySamples.salary.id) else it }
        val vm = viewModel(FakeMoneyRepository(withTithe), transactions)

        val oct = vm.ui.value.view!!.months.first()
        assertEquals(october, oct.month)
        assertTrue(oct.sources.single().actual)
        assertEquals(listOf(MoneySamples.checking, MoneySamples.oldFund), vm.ui.value.assets)
    }

    @Test
    fun aDonation_needsAnAmount_thenIsSaved() {
        val repo = FakeMoneyRepository(withTithe)
        val vm = viewModel(repo)
        vm.startDonation()
        assertEquals(today, vm.ui.value.sheets.donation!!.draft.date)
        vm.saveDonation()
        assertTrue(vm.ui.value.sheets.donation!!.invalid)
        vm.updateDonation { it.copy(amount = "180", organisation = "קופת העיר") }
        assertFalse(vm.ui.value.sheets.donation!!.invalid)
        vm.saveDonation()
        assertNull(vm.ui.value.sheets.donation)
        val saved = (repo.saved.single() as MoneyEntry.DonationEntry).value
        assertEquals("don-new", saved.id)
        assertEquals(shekels(180), saved.amount)
    }

    @Test
    fun deletingADonation_asksFirst() {
        val repo = FakeMoneyRepository(withTithe)
        val vm = viewModel(repo)
        val donation = MoneySamples.donations.first()
        vm.editDonation(donation)
        vm.requestDeleteDonation(true)
        assertTrue(vm.ui.value.sheets.donation!!.confirmDelete)
        vm.deleteDonation()
        assertEquals(listOf(MoneyCollection.DONATIONS to donation.id), repo.deleted)
    }
}
