package com.menash.myself.feature.money.ui

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.menash.myself.core.data.session.DefaultErrorReporter
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.feature.money.FakeMoneyRepository
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.ImportInbox
import com.menash.myself.feature.money.MoneySamples
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        val goals = GoalsViewModel(
            FakeMoneyRepository(MoneySamples.filled),
            MutableStateFlow(MoneySamples.rate),
            { LocalDate.of(2026, 10, 30) },
            DefaultErrorReporter(),
        )
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
                    {
                        shareBlocked(
                            monthDirty = false,
                            goalEditor = goals.ui.value.editor != null,
                            titheEditor = false,
                            pensionEditor = false,
                            carEditor = false,
                            closeBalances = false,
                        )
                    },
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
}
