package com.menash.myself.feature.life.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import com.menash.myself.shell.ShellRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** «חיים»: each life feature opens its own screen, not an empty tab. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class LifeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyEntryOpensItsRealScreen() {
        val opened = mutableListOf<String>()
        composeRule.setContent { MyApplicationTheme { LifeContent(onOpenRoute = { opened += it }) } }
        lifeEntries.forEach { entry -> composeRule.onNodeWithTag(entry.testTag).performScrollTo().performClick() }
        assertEquals(
            listOf(ShellRoute.Tasks, ShellRoute.DreamsLegacy, ShellRoute.Fitness, ShellRoute.Calendar, ShellRoute.Inbox),
            opened,
        )
    }

    @Test
    fun life_light_font10() = composeRule.captureR2Screen(darkTheme = false, fontScale = 1.0f) {
        LifeContent(onOpenRoute = {})
    }

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun life_dark_font10() = composeRule.captureR2Screen(darkTheme = true, fontScale = 1.0f) {
        LifeContent(onOpenRoute = {})
    }
}
