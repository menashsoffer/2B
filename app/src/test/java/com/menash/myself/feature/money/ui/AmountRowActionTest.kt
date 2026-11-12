package com.menash.myself.feature.money.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Stage 0 of the money UX brief: each row kind carries its own end mark, and a plain row has none. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AmountRowActionTest {
    @get:Rule
    val rule = createComposeRule()

    private val marks = listOf("row_mark_open", "row_mark_edit", "row_mark_expand")

    private fun show(action: RowAction?) = rule.setContent {
        MyApplicationTheme(darkTheme = false) { AmountRow(title = "שורה", amount = "₪100", action = action, testTag = "row") }
    }

    private fun assertOnlyMark(expected: String?) = marks.forEach { tag ->
        val nodes = rule.onAllNodesWithTagUnmerged(tag)
        assertEquals("$tag shown", if (tag == expected) 1 else 0, nodes)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagUnmerged(tag: String): Int =
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, tag), useUnmergedTree = true)
            .fetchSemanticsNodes().size

    @Test fun infoRowHasNoMarkAndNoClick() {
        show(null)
        rule.onNodeWithTag("row").assertHasNoClickAction()
        assertOnlyMark(null)
    }

    @Test fun openRowShowsChevronAndRuns() {
        var opened = 0
        show(RowAction.Open { opened++ })
        assertOnlyMark("row_mark_open")
        rule.onNodeWithTag("row").assertHasClickAction().performClick()
        assertEquals(1, opened)
    }

    @Test fun editRowShowsPencil() {
        show(RowAction.Edit {})
        assertOnlyMark("row_mark_edit")
    }

    @Test fun expandRowShowsTurningChevronAndState() {
        show(RowAction.Expand(expanded = true) {})
        assertOnlyMark("row_mark_expand")
        rule.onNodeWithTag("row").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
    }

    @Test fun chooseRowIsClickableWithoutMark() {
        show(RowAction.Choose {})
        rule.onNodeWithTag("row").assertHasClickAction()
        assertOnlyMark(null)
    }
}
