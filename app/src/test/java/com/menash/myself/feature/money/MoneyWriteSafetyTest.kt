package com.menash.myself.feature.money

import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.ui.UiEvent
import com.menash.myself.core.ui.UiEvents
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.data.MoneyRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MoneyWriteSafetyTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun hangingWriteClosesEditorAfterTimeout() = runTest {
        val base = FakeMoneyRepository(MoneySamples.filled)
        val repo = object : MoneyRepository by base {
            override suspend fun saveAll(entries: List<MoneyEntry>) { CompletableDeferred<Unit>().await() }
        }
        val events = UiEvents()
        val received = mutableListOf<UiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.events.collect { received += it } }
        val vm = MoneyViewModel(repo, MutableStateFlow(MoneySamples.rate), { MoneySamples.today }, DefaultErrorReporter(), events = events)
        vm.startAdding(EditKind.INCOME)
        vm.updateDraft { it.copy(name = "משכורת", amount = "12000") }
        vm.save()
        assertTrue(vm.editor.value!!.saving)
        advanceTimeBy(2_999)
        runCurrent()
        assertNotNull(vm.editor.value)
        advanceTimeBy(1)
        runCurrent()
        assertNull(vm.editor.value)
        assertTrue(received.contains(UiEvent.SavedOffline))
    }

    @Test fun moneyDraftSurvivesFailedSaveAndCanBeRetried() {
        val base = FakeMoneyRepository(MoneySamples.filled)
        val gate = CompletableDeferred<Unit>()
        var fail = true
        val attempts = mutableListOf<String>()
        val repository = object : MoneyRepository by base {
            override suspend fun saveAll(entries: List<MoneyEntry>) = entries.forEach { save(it) }
            override suspend fun save(entry: MoneyEntry) {
                attempts += entry.id
                if (fail) gate.await()
                base.save(entry)
            }
        }
        val reporter = DefaultErrorReporter()
        val vm = MoneyViewModel(repository, MutableStateFlow(MoneySamples.rate), { MoneySamples.today }, reporter)
        vm.startAdding(EditKind.INCOME)
        vm.updateDraft { it.copy(name = "משכורת", amount = "12000") }
        vm.save()
        assertTrue(vm.editor.value!!.saving)
        vm.dismissEditor()
        assertNotNull(vm.editor.value)
        gate.completeExceptionally(IOException("write failed"))
        assertFalse(vm.editor.value!!.saving)
        assertEquals("משכורת", vm.editor.value!!.draft.name)
        assertNull(reporter.errorState.value)
        fail = false
        vm.save()
        assertNull(vm.editor.value)
        assertEquals(1, base.saved.size)
        assertEquals(attempts.first(), attempts.last())
    }

    @Test fun donationDraftSurvivesFailedSaveAndCanBeRetried() {
        val base = FakeMoneyRepository(MoneySamples.filled)
        val gate = CompletableDeferred<Unit>()
        var fail = true
        val attempts = mutableListOf<String>()
        val repository = object : MoneyRepository by base {
            override suspend fun save(entry: MoneyEntry) {
                attempts += entry.id
                if (fail) gate.await()
                base.save(entry)
            }
        }
        val reporter = DefaultErrorReporter()
        val vm = TitheViewModel(repository, FakeTransactionRepository(), { MoneySamples.today }, reporter)
        vm.startDonation()
        vm.updateDonation { it.copy(amount = "180", organisation = "קופה") }
        vm.saveDonation()
        assertTrue(vm.ui.value.sheets.donation!!.saving)
        vm.dismissSheets()
        assertNotNull(vm.ui.value.sheets.donation)
        gate.completeExceptionally(IOException("write failed"))
        assertFalse(vm.ui.value.sheets.donation!!.saving)
        assertEquals("180", vm.ui.value.sheets.donation!!.draft.amount)
        assertNull(reporter.errorState.value)
        fail = false
        vm.saveDonation()
        assertNull(vm.ui.value.sheets.donation)
        assertEquals(1, base.saved.size)
        assertEquals(attempts.first(), attempts.last())
    }

    @Test fun pensionDraftSurvivesFailedSaveAndCanBeRetried() {
        val base = FakeMoneyRepository(MoneySamples.filled)
        val gate = CompletableDeferred<Unit>()
        var fail = true
        val attempts = mutableListOf<String>()
        val repository = object : MoneyRepository by base {
            override suspend fun save(entry: MoneyEntry) {
                attempts += entry.id
                if (fail) gate.await()
                base.save(entry)
            }
        }
        val vm = PensionViewModel(repository, { MoneySamples.today }, DefaultErrorReporter())
        vm.startAdding()
        vm.update { it.copy(provider = "קרן", value = "30000") }
        vm.save()
        assertTrue(vm.ui.value.editor!!.saving)
        vm.dismiss()
        assertNotNull(vm.ui.value.editor)
        gate.completeExceptionally(IOException("write failed"))
        assertFalse(vm.ui.value.editor!!.saving)
        assertEquals("קרן", vm.ui.value.editor!!.draft.provider)
        fail = false
        vm.save()
        assertNull(vm.ui.value.editor)
        assertEquals(1, base.saved.size)
        assertEquals(attempts.first(), attempts.last())
    }
}
