package com.menash.myself.feature.money.ui

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.feature.money.ImportInbox
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
}
