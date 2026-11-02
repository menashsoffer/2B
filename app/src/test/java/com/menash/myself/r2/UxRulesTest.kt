package com.menash.myself.r2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = LightQualifiers)
class UxRulesTest {

    @get:Rule
    val rule = createComposeRule()

    private fun target(name: String, onClick: () -> Unit = {}) =
        Modifier.size(32.dp).semantics { contentDescription = name }.clickable(onClick = onClick)

    @Test
    fun compose_extendsASmallTargetTo48dp_soATapJustOutsideItStillCounts() {
        var taps = 0
        rule.setContent {
            Box(Modifier.size(200.dp).testTag("root"), contentAlignment = Alignment.Center) {
                Box(target("small") { taps++ })
            }
        }
        // The 32dp box spans 84..116dp; 6dp outside its edge is inside the 48dp touch area (76..124dp).
        rule.onNodeWithTag("root").performTouchInput {
            click(Offset(x = (100 + 16 + 6).dp.toPx(), y = 100.dp.toPx()))
        }
        assertEquals(1, taps)
    }

    @Test
    fun aLoneSmallTarget_isNotAFinding() {
        rule.setContent {
            Box(Modifier.size(200.dp).testTag("root"), contentAlignment = Alignment.Center) {
                Box(target("alone"))
            }
        }
        val findings = UxRules.check(rule.onNodeWithTag("root").fetchSemanticsNode())
        assertTrue(findings.toString(), findings.none { it.rule == "touch-target" })
    }

    @Test
    fun twoSmallTargetsCloserThan48dp_areReported() {
        rule.setContent {
            Row(Modifier.size(200.dp).testTag("root"), verticalAlignment = Alignment.CenterVertically) {
                Box(target("first"))
                Spacer(Modifier.width(4.dp))
                Box(target("second"))
                Text("label")
            }
        }
        val findings = UxRules.check(rule.onNodeWithTag("root").fetchSemanticsNode()).filter { it.rule == "touch-target" }
        assertEquals(setOf("first", "second"), findings.map { it.element }.toSet())
    }
}
