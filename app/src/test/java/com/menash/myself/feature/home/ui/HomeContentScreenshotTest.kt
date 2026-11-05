package com.menash.myself.feature.home.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.HomeContentAtFixedClock
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * HomeContent goldens. Font scale is a [com.menash.myself.r2.ProvideR2FontScale] override;
 * see [com.menash.myself.r2.captureR2Screen]. Clock and name come from [com.menash.myself.r2.FixedClock].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class HomeContentScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeEmpty_light_font10() = capture(darkTheme = false, fontScale = 1.0f, dreamsActive = null)

    @Test
    fun homeEmpty_light_font13() = capture(darkTheme = false, fontScale = 1.3f, dreamsActive = null)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun homeEmpty_dark_font10() = capture(darkTheme = true, fontScale = 1.0f, dreamsActive = null)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun homeEmpty_dark_font13() = capture(darkTheme = true, fontScale = 1.3f, dreamsActive = null)

    @Test
    fun homeDreams4_light_font10() = capture(darkTheme = false, fontScale = 1.0f, dreamsActive = 4)

    @Test
    fun homeDreams4_light_font13() = capture(darkTheme = false, fontScale = 1.3f, dreamsActive = 4)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun homeDreams4_dark_font10() = capture(darkTheme = true, fontScale = 1.0f, dreamsActive = 4)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun homeDreams4_dark_font13() = capture(darkTheme = true, fontScale = 1.3f, dreamsActive = 4)

    @Test
    fun homeDreams128_light_font13() = capture(darkTheme = false, fontScale = 1.3f, dreamsActive = 128)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun homeDreams128_dark_font13() = capture(darkTheme = true, fontScale = 1.3f, dreamsActive = 128)

    @Test
    fun homeRelease_light_font10() {
        org.junit.Assert.assertEquals(listOf("home", "money", "me"),
            com.menash.myself.ui.shell.shellTabsFor(false).map { it.route })
        composeRule.captureR2Screen(darkTheme = false, fontScale = 1.0f) {
            HomeContent(
                state = HomeUiState(firstName = com.menash.myself.r2.SampleName),
                hour = com.menash.myself.r2.FixedClock.HOUR,
                epochMillis = com.menash.myself.r2.FixedClock.epochMillis,
                onOpenRoute = {}, onOpenAccount = {}, debug = false,
            )
        }
    }

    private fun capture(darkTheme: Boolean, fontScale: Float, dreamsActive: Int?) {
        composeRule.captureR2Screen(darkTheme = darkTheme, fontScale = fontScale) {
            HomeContentAtFixedClock(dreamsActive = dreamsActive)
        }
    }
}
