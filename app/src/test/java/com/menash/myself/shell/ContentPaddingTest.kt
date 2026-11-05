package com.menash.myself.shell

import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.r2.LightQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/**
 * contentPadding() bottom is at least the bar height plus 12dp plus the navigation-bar inset.
 * The inset injected here is 48dp, so a zero inset fails the test instead of passing vacuously.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class ContentPaddingTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentPadding_bottomCoversBarMarginAndNavigationInset() {
        composeRule.setContent {
            MyApplicationTheme {
                InjectNavigationInset(bottom = 48.dp)
                PaddingReadout()
            }
        }
        composeRule.waitForIdle()
        val raw = composeRule.onNodeWithTag("content_padding")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .joinToString(separator = "") { text -> text.text }
        val parts = raw.split("|")
        val navInset = parts[0].toFloat()
        val bottom = parts[1].toFloat()
        assertTrue("navigation inset was $navInset; expected the injected 48dp", navInset >= 40f)
        val minimum = FloatingNavBarDefaults.barHeight.value + 12f + navInset
        assertTrue(
            "bottom $bottom < bar ${FloatingNavBarDefaults.barHeight.value} + 12 + inset $navInset",
            bottom + 0.5f >= minimum,
        )
        assertTrue(FloatingNavBarDefaults.outerMargin.value + 0.01f >= 12f)
        assertTrue(FloatingNavBarDefaults.barHeight.value + 0.01f >= 64f)
    }
}

@Composable
private fun InjectNavigationInset(bottom: Dp) {
    val bars = WindowInsets.navigationBars
    val view = LocalView.current
    val density = LocalDensity.current
    val bottomPx = with(density) { bottom.roundToPx() }
    var applied by remember { mutableStateOf(false) }
    SideEffect {
        // Robolectric applies a zero navigation inset after the holder attaches.
        // Drop that listener, then write the same AndroidWindowInsets instance
        // contentPadding() reads. `applied` recomposes so the readout sees the write.
        clearInsetListener(view)
        // The setter is internal, so its JVM name carries a module suffix that changes between
        // Compose versions (foundation_layout_release in 1.7, different from 1.8 on). Match by shape.
        val setter = bars.javaClass.methods.firstOrNull { method ->
            method.name.startsWith("setInsets") && method.parameterTypes.contentEquals(arrayOf(Insets::class.java))
        } ?: error("No insets setter on ${bars.javaClass.name}: ${bars.javaClass.methods.map { it.name }.filter { "nsets" in it }}")
        setter.invoke(bars, Insets.of(0, 0, 0, bottomPx))
        applied = true
    }
    check(!applied || bottomPx > 0)
}

private fun clearInsetListener(view: View) {
    ViewCompat.setOnApplyWindowInsetsListener(view, null)
}

@Composable
private fun PaddingReadout() {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottom = FloatingNavBarDefaults.contentPadding().calculateBottomPadding()
    Text(
        text = String.format(Locale.US, "%.2f|%.2f", navInset.value, bottom.value),
        modifier = Modifier.testTag("content_padding"),
    )
}
