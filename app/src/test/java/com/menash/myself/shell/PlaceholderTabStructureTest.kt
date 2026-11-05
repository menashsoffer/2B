package com.menash.myself.shell

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.collectTestTags
import com.menash.myself.testing.collectTexts
import com.menash.myself.testing.r2Placeholder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Placeholder tabs have the building copy and no clickable node. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class PlaceholderTabStructureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun placeholders_haveNoClickableNode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val building = context.getString(R.string.placeholder_building)
        assertEquals("העמוד הזה בבנייה", building)
        // Money is a real screen since R3; the other tabs are still placeholders.
        val routes = listOf(ShellRoute.Dreams, ShellRoute.Shopping, ShellRoute.Tracking)
        composeRule.setContent {
            MyApplicationTheme {
                Column {
                    routes.forEach { route -> r2Placeholder(route = route) }
                }
            }
        }
        val root = composeRule.onRoot().fetchSemanticsNode()
        assertEquals(0, composeRule.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes().size)
        assertFalse(root.hasClickActionDeep())
        assertFalse(root.hasButtonRole())
        routes.forEach { route ->
            val title = context.getString(placeholderTabs.first { tab -> tab.route == route }.labelRes)
            assertTrue(root.collectTexts().contains(title))
            assertTrue(root.collectTexts().contains(building))
            assertTrue(root.collectTestTags().contains("placeholder_$route"))
        }
    }

    private fun SemanticsNode.hasClickActionDeep(): Boolean {
        if (config.getOrNull(SemanticsActions.OnClick) != null) return true
        return children.any { child -> child.hasClickActionDeep() }
    }

    private fun SemanticsNode.hasButtonRole(): Boolean {
        if (config.getOrNull(SemanticsProperties.Role) == Role.Button) return true
        return children.any { child -> child.hasButtonRole() }
    }
}
