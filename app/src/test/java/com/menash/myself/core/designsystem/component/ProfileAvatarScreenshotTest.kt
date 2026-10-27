package com.menash.myself.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.SampleName
import com.menash.myself.r2.captureR2Wrap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Avatar for [SampleName]. Font scale: see [com.menash.myself.r2.ProvideR2FontScale]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class ProfileAvatarScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileAvatar_light_font10() = capture(darkTheme = false, fontScale = 1.0f)

    @Test
    fun profileAvatar_light_font13() = capture(darkTheme = false, fontScale = 1.3f)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun profileAvatar_dark_font10() = capture(darkTheme = true, fontScale = 1.0f)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun profileAvatar_dark_font13() = capture(darkTheme = true, fontScale = 1.3f)

    private fun capture(darkTheme: Boolean, fontScale: Float) {
        composeRule.captureR2Wrap(darkTheme = darkTheme, fontScale = fontScale) {
            ProfileAvatar(displayName = SampleName, onClick = {})
        }
    }
}
