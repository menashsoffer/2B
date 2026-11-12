package com.menash.myself.feature.money.ui

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.CategoryTrendViewModel
import com.menash.myself.feature.money.FakeMoneyRepository
import com.menash.myself.feature.money.FakeTransactionRepository
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.ImportInbox
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.ImportViewModel
import com.menash.myself.feature.money.MoneySamples
import com.menash.myself.feature.money.MoneyViewModel
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.MonthSummaryViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.PeriodViewModel
import com.menash.myself.feature.money.PlanBasis
import com.menash.myself.feature.money.PlanImportDraft
import com.menash.myself.feature.money.StatementSource
import com.menash.myself.feature.money.SubscriptionsViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.importer.ImportPlan
import com.menash.myself.feature.money.importer.ImportReview
import com.menash.myself.feature.money.importer.MerchantDecision
import com.menash.myself.feature.money.importer.Source
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Looper
import java.time.LocalDate

/** B9b and B10: a shared file opens import, unless a dirty form asks first. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SharedImportEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cleanForm_closesStackAndImports() {
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        val closed = mutableListOf<String>()
        val imported = mutableListOf<List<Uri>>()
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(inbox, { false }, { closed += "closed" }, {}, { imported += it })
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf("closed"), closed)
        assertEquals(1, imported.size)
        assertEquals(emptyList<String>(), inbox.pending.value)
    }

    @Test
    fun dirtyForm_keepLeavesTheFilePending() {
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        val closed = mutableListOf<String>()
        val imported = mutableListOf<List<Uri>>()
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(inbox, { true }, { closed += "closed" }, {}, { imported += it })
            }
        }
        composeRule.onNodeWithTag("discard_keep").performClick()
        assertEquals(emptyList<String>(), closed)
        assertEquals(emptyList<List<Uri>>(), imported)
        assertEquals(listOf("content://bank/file"), inbox.pending.value)
    }

    @Test
    fun dirtyForm_leaveImportsAndDropsTheDraft() {
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        val closed = mutableListOf<String>()
        val imported = mutableListOf<List<Uri>>()
        var discarded = 0
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(inbox, { true }, { closed += "closed" }, { discarded++ }, { imported += it })
            }
        }
        composeRule.onNodeWithTag("discard_leave").performClick()
        assertEquals(1, discarded)
        assertEquals(listOf("closed"), closed)
        assertEquals(1, imported.size)
        assertEquals(emptyList<String>(), inbox.pending.value)
    }

    @Test
    fun anyOpenEditor_blocksTheSharedClose() {
        assertEquals(false, shareBlocked(false, false, false, false, false, false))
        listOf(
            shareBlocked(monthDirty = true, goalEditor = false, titheEditor = false, pensionEditor = false, carEditor = false, closeBalances = false),
            shareBlocked(false, goalEditor = true, titheEditor = false, pensionEditor = false, carEditor = false, closeBalances = false),
            shareBlocked(false, false, titheEditor = true, pensionEditor = false, carEditor = false, closeBalances = false),
            shareBlocked(false, false, false, pensionEditor = true, carEditor = false, closeBalances = false),
            shareBlocked(false, false, false, false, carEditor = true, closeBalances = false),
            shareBlocked(false, false, false, false, false, closeBalances = true),
        ).forEach { assertEquals(true, it) }
    }

    @Test
    fun halfTypedGoal_keepSurvivesAndFileStaysPending() {
        val money = FakeMoneyRepository(MoneySamples.filled)
        val transactions = FakeTransactionRepository()
        val rate = MutableStateFlow(MoneySamples.rate)
        val today = { LocalDate.of(2026, 10, 30) }
        val error = DefaultErrorReporter()
        val goals = GoalsViewModel(money, rate, today, error)
        val month = MoneyViewModel(money, rate, today, error)
        val tithe = TitheViewModel(money, transactions, today, error)
        val pension = PensionViewModel(money, today, error)
        val car = CarViewModel(money, transactions, today, error)
        val close = MonthCloseViewModel(money, transactions, rate, today, error)
        goals.open()
        goals.startAdding()
        goals.updateDraft { it.copy(name = "דירה") }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals("דירה", goals.ui.value.editor?.draft?.name)
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        var closed = 0
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(
                    inbox,
                    { shareBlocked(month, goals, tithe, pension, car, close, importVm()) },
                    { closed++; goals.close() },
                    {},
                    {},
                )
            }
        }
        composeRule.onNodeWithTag("discard_keep").performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, closed)
        assertNotNull(goals.ui.value.editor)
        assertEquals("דירה", goals.ui.value.editor?.draft?.name)
        assertEquals(listOf("content://bank/file"), inbox.pending.value)
    }

    @Test
    fun keep_thenTheFormCloses_importsTheWaitingFile() {
        val month = moneyVm()
        val goals = goalVm()
        val tithe = titheVm()
        val pension = pensionVm()
        val car = carVm()
        val close = closeVm()
        val import = importVm()
        goals.open()
        goals.startAdding()
        goals.updateDraft { it.copy(name = "דירה") }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        var closed = 0
        val imported = mutableListOf<List<Uri>>()
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(
                    inbox,
                    { shareBlocked(month, goals, tithe, pension, car, close, import) },
                    { closed++ },
                    {},
                    { imported += it },
                    shareBlockedFlow(month, goals, tithe, pension, car, close, import),
                )
            }
        }
        composeRule.onNodeWithTag("discard_keep").performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        assertEquals(listOf("content://bank/file"), inbox.pending.value)
        assertEquals(0, imported.size)

        goals.close()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        assertEquals(1, closed)
        assertEquals(1, imported.size)
        assertEquals(emptyList<String>(), inbox.pending.value)
        assertEquals(0, composeRule.onAllNodesWithTag("discard_confirm").fetchSemanticsNodes().size)
    }

    @Test
    fun cleanGoal_closesWithoutAsking() {
        val goals = goalVm()
        goals.open()
        goals.startAdding()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertNotNull(goals.ui.value.editor)
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        var closed = 0
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(
                    inbox,
                    { shareBlocked(moneyVm(), goals, titheVm(), pensionVm(), carVm(), closeVm(), importVm()) },
                    { closed++; goals.close() },
                    {},
                    { },
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("discard_keep").fetchSemanticsNodes().size)
        assertEquals(1, closed)
        assertNull(goals.ui.value.editor)
        assertEquals(emptyList<String>(), inbox.pending.value)
    }

    @Test
    fun importReviewAndPlan_askBeforeASharedFile() {
        val plan = ImportPlan(emptyList(), emptyList(), 0, 0, 0, 0, 0, emptyList())
        val review = ImportReview(emptyList(), plan, emptyList())
        assertFalse(ImportUi.Review(review, emptyList()).blocksShare())
        assertTrue(ImportUi.Review(review, emptyList(), decisions = mapOf("shop" to MerchantDecision.Categorise("food"))).blocksShare())
        assertTrue(ImportUi.Review(review, emptyList(), picking = "shop").blocksShare())
        val draft = PlanImportDraft(PlanBasis.PaamonimFile(Month.SEPTEMBER), YearMonth.of(2026, 9), emptyList(), null)
        assertFalse(ImportUi.PlanReview(draft).blocksShare())
        assertTrue(ImportUi.PlanReview(draft, selected = setOf("off")).blocksShare())
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(inbox, { ImportUi.PlanReview(draft, selected = setOf("off")).blocksShare() }, {}, {}, {})
            }
        }
        composeRule.onNodeWithTag("discard_keep").performClick()
        assertEquals(listOf("content://bank/file"), inbox.pending.value)
    }

    @Test
    fun closeMonthChoices_askBeforeASharedFile() {
        assertTrue(MonthCloseUi(confirmReclose = true).blocksShare())
        assertTrue(MonthCloseUi(confirmMisc = true).blocksShare())
        assertTrue(MonthCloseUi(noLines = setOf("max:1")).blocksShare())
        assertFalse(MonthCloseUi(open = true).blocksShare())
        val close = closeVm()
        close.open(YearMonth.of(2026, 9))
        close.markNoLines(StatementSource(Source.MAX, "1234"))
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(close.ui.value.blocksShare())
        val inbox = ImportInbox().also { it.offer(listOf("content://bank/file")) }
        var closed = 0
        composeRule.setContent {
            MyApplicationTheme {
                SharedImportEffect(inbox, { close.ui.value.blocksShare() }, { closed++ }, {}, {})
            }
        }
        composeRule.onNodeWithTag("discard_keep").performClick()
        assertEquals(0, closed)
        assertEquals(listOf("content://bank/file"), inbox.pending.value)
        assertTrue(close.ui.value.noLines.contains("max:1234"))
    }
}

private fun moneyVm() = MoneyViewModel(FakeMoneyRepository(MoneySamples.filled), MutableStateFlow(MoneySamples.rate), { LocalDate.of(2026, 10, 30) }, DefaultErrorReporter())
private fun goalVm() = GoalsViewModel(FakeMoneyRepository(MoneySamples.filled), MutableStateFlow(MoneySamples.rate), { LocalDate.of(2026, 10, 30) }, DefaultErrorReporter())
private fun titheVm() = TitheViewModel(FakeMoneyRepository(MoneySamples.filled), FakeTransactionRepository(), { LocalDate.of(2026, 10, 30) }, DefaultErrorReporter())
private fun pensionVm() = PensionViewModel(FakeMoneyRepository(MoneySamples.filled), { LocalDate.of(2026, 10, 30) }, DefaultErrorReporter())
private fun carVm() = CarViewModel(FakeMoneyRepository(MoneySamples.filled), FakeTransactionRepository(), { LocalDate.of(2026, 10, 30) }, DefaultErrorReporter())
private fun closeVm() = MonthCloseViewModel(FakeMoneyRepository(MoneySamples.filled), FakeTransactionRepository(), MutableStateFlow(MoneySamples.rate), { LocalDate.of(2026, 10, 30) }, DefaultErrorReporter())
private fun importVm() = ImportViewModel(FakeTransactionRepository(), FakeMoneyRepository(MoneySamples.filled), DefaultErrorReporter())
