package com.menash.myself.r2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Canary for UX layer 2 (docs/ux/UX-CHECKS.md): a 20dp clickable box with no label, which the
 * Accessibility Test Framework must flag. If the CI report has no "atf" line for this test,
 * ATF does not see Compose nodes under Robolectric and layer 2 must run on the emulator instead.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class AtfCanaryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tinyUnlabelledButton_isReportedByAtf() {
        composeRule.setContent {
            Box(Modifier.size(20.dp).clickable { })
        }
        UxRules.recordAccessibilityChecks(composeRule, "canary")
    }
}
