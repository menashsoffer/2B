package com.menash.myself.feature.money.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.menash.myself.feature.money.ImportUi
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImportDoneTest {
    @get:Rule val compose = createComposeRule()

    @Test fun multipleMonthsOpenTheSelectedMonth() {
        val october = YearMonth.of(2026, 10)
        val november = YearMonth.of(2026, 11)
        var opened: YearMonth? = null
        compose.setContent {
            ImportContent(ImportUi.Done(8, 0, unassignedByMonth = mapOf(november to 2, october to 5)), ImportActions(onAssign = { opened = it }))
        }
        compose.onNodeWithText("אוקטובר 2026 · 5 בלי קטגוריה").assertIsDisplayed()
        compose.onNodeWithText("נובמבר 2026 · 2 בלי קטגוריה").performClick()
        assertEquals(november, opened)
        compose.onNodeWithText("לשייך עכשיו").assertDoesNotExist()
    }

    @Test fun oneMonthKeepsTheAssignButton() {
        val month = YearMonth.of(2026, 10)
        var opened: YearMonth? = null
        compose.setContent { ImportContent(ImportUi.Done(5, 0, unassignedByMonth = mapOf(month to 5)), ImportActions(onAssign = { opened = it })) }
        compose.onNodeWithTag("import_done").assertIsDisplayed()
        compose.onNodeWithText("לשייך עכשיו").performClick()
        assertEquals(month, opened)
    }
}
