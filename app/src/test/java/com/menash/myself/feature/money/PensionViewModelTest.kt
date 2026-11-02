package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.data.MoneyCollection
import com.menash.myself.feature.money.domain.FundKind
import com.menash.myself.feature.money.domain.PensionFund
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PensionViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun deletingExistingFundCallsRepositoryAndClosesEditor() {
        val today = LocalDate.of(2026, 10, 30)
        val fund = PensionFund("p", FundKind.PENSION, "Provider", Money(10000), Money(5000), Money(100), BigDecimal("0.2"), BigDecimal("1.0"), null, today)
        val repo = FakeMoneyRepository(MoneySamples.filled.copy(pensionFunds = listOf(fund)))
        val vm = PensionViewModel(repo, { today }, DefaultErrorReporter())
        vm.edit(fund)
        vm.requestDelete(true)
        vm.delete()
        assertEquals(listOf(MoneyCollection.PENSION_FUNDS to "p"), repo.deleted)
        assertNull(vm.ui.value.editor)
    }
}
