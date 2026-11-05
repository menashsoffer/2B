package com.menash.myself.shell

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.collectTexts
import com.menash.myself.r2.r2NavBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Spec order, right to left: בית, כסף, חלומות, קניות, מעקב. Only home is selected.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class FloatingNavBarStructureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nav_hasFourDebugTabsInSpecOrder_andOnlyHomeIsSelected() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expected = listOf(
            "בית" to R.string.nav_home,
            "כסף" to R.string.nav_money,
            "חיים" to R.string.nav_life,
            "אני" to R.string.nav_me,
        )
        expected.forEach { (label, res) ->
            assertEquals(label, context.getString(res))
        }
        assertEquals(expected.map { pair -> pair.first }, shellTabs.map { tab -> context.getString(tab.labelRes) })
        composeRule.setContent {
            MyApplicationTheme {
                r2NavBar(selectedRoute = ShellRoute.Home)
            }
        }
        val bar = composeRule.onNodeWithTag("floating_nav_bar").fetchSemanticsNode()
        val tabs = bar.children.filter { node ->
            node.config.getOrNull(SemanticsProperties.Role) == Role.Tab
        }
        assertEquals(4, tabs.size)
        tabs.forEachIndexed { index, tab ->
            assertEquals(expected[index].first, tab.collectTexts().single())
            val selected = tab.config.getOrNull(SemanticsProperties.Selected) == true
            assertEquals(index == 0, selected)
        }
        assertTrue(tabs[0].boundsInRoot.left > tabs[1].boundsInRoot.left)
    }

    @Test
    fun releaseBar_isHomeMoneyMe() {
        assertEquals(
            listOf(ShellRoute.Home, ShellRoute.Money, ShellRoute.Me),
            com.menash.myself.shell.shellTabsFor(false).map { tab -> tab.route },
        )
    }
}
