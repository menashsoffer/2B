package com.menash.myself.ui.shell

import androidx.compose.ui.test.junit4.createComposeRule
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import com.menash.myself.r2.r2Placeholder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Dreams placeholder EmptyState. Font scale: see [com.menash.myself.r2.ProvideR2FontScale]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class PlaceholderEmptyStateScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun placeholder_light_font10() = capture(darkTheme = false, fontScale = 1.0f)

    @Test
    fun placeholder_light_font13() = capture(darkTheme = false, fontScale = 1.3f)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun placeholder_dark_font10() = capture(darkTheme = true, fontScale = 1.0f)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun placeholder_dark_font13() = capture(darkTheme = true, fontScale = 1.3f)

    private fun capture(darkTheme: Boolean, fontScale: Float) {
        composeRule.captureR2Screen(darkTheme = darkTheme, fontScale = fontScale) {
            r2Placeholder(route = ShellRoute.Dreams)
        }
    }
}
