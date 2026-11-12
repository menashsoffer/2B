package com.menash.myself.feature.money.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.menash.myself.feature.money.MoneyFrame
import com.menash.myself.feature.money.MoneyPlace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/** B12: process death keeps the walk order, even when the open flags arrive a frame later. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MoneyRestoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun b12_restoreKeepsWalkOrderWhenFlagsArriveLate() {
        val tester = StateRestorationTester(composeRule)
        val boots = AtomicInteger(0)
        tester.setContent {
            val boot = remember { boots.incrementAndGet() }
            var open by remember {
                mutableStateOf(listOf(frame(MoneyPlace.Transactions), frame(MoneyPlace.Close)))
            }
            var cleared by remember { mutableStateOf(false) }
            if (boot > 1 && !cleared) {
                open = emptyList()
                cleared = true
            }
            val shown = restoredMoneyStack(open) { missing -> open = missing }
            Text(shown.joinToString("-") { it.place.name }, modifier = Modifier.testTag("stack"))
        }
        composeRule.waitForIdle()
        assertEquals("Transactions-Close", text())
        tester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        assertEquals("Transactions-Close", text())
    }

    private fun text(): String {
        val node = composeRule.onNodeWithTag("stack").fetchSemanticsNode()
        return node.config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .orEmpty()
            .joinToString("") { it.text }
    }

    private fun frame(place: MoneyPlace) = MoneyFrame(place, month = "2026-09")
}
