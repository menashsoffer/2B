package com.menash.myself.core.designsystem.component

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import com.menash.myself.R
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2FullWidth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Gallery state: התמונה הגדולה selected. Font scale: see [com.menash.myself.testing.ProvideR2FontScale]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class PillTabsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pillTabs_light_font10() = capture(darkTheme = false, fontScale = 1.0f)

    @Test
    fun pillTabs_light_font13() = capture(darkTheme = false, fontScale = 1.3f)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun pillTabs_dark_font10() = capture(darkTheme = true, fontScale = 1.0f)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun pillTabs_dark_font13() = capture(darkTheme = true, fontScale = 1.3f)

    private fun capture(darkTheme: Boolean, fontScale: Float) {
        composeRule.captureR2FullWidth(darkTheme = darkTheme, fontScale = fontScale) {
            PillTabs(
                options = listOf(
                    stringResource(R.string.pill_month),
                    stringResource(R.string.pill_big_picture),
                ),
                selectedIndex = 1,
                onSelect = {},
            )
        }
    }
}
