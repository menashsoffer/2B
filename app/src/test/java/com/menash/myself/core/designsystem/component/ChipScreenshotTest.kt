package com.menash.myself.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import com.menash.myself.R
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Wrap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The three [ChipVariant] values shown in the gallery, not clickable.
 * Font scale: see [com.menash.myself.testing.ProvideR2FontScale].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class ChipScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chipDefault_light_font10() = capture(false, 1.0f, ChipVariant.Default, R.string.pill_month)

    @Test
    fun chipDefault_light_font13() = capture(false, 1.3f, ChipVariant.Default, R.string.pill_month)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun chipDefault_dark_font10() = capture(true, 1.0f, ChipVariant.Default, R.string.pill_month)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun chipDefault_dark_font13() = capture(true, 1.3f, ChipVariant.Default, R.string.pill_month)

    @Test
    fun chipSuccess_light_font10() = capture(false, 1.0f, ChipVariant.Success, R.string.chip_paid)

    @Test
    fun chipSuccess_light_font13() = capture(false, 1.3f, ChipVariant.Success, R.string.chip_paid)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun chipSuccess_dark_font10() = capture(true, 1.0f, ChipVariant.Success, R.string.chip_paid)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun chipSuccess_dark_font13() = capture(true, 1.3f, ChipVariant.Success, R.string.chip_paid)

    @Test
    fun chipWarning_light_font10() = capture(false, 1.0f, ChipVariant.Warning, R.string.chip_pending)

    @Test
    fun chipWarning_light_font13() = capture(false, 1.3f, ChipVariant.Warning, R.string.chip_pending)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun chipWarning_dark_font10() = capture(true, 1.0f, ChipVariant.Warning, R.string.chip_pending)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun chipWarning_dark_font13() = capture(true, 1.3f, ChipVariant.Warning, R.string.chip_pending)

    private fun capture(
        darkTheme: Boolean,
        fontScale: Float,
        variant: ChipVariant,
        @StringRes textRes: Int,
    ) {
        composeRule.captureR2Wrap(darkTheme = darkTheme, fontScale = fontScale) {
            Chip(text = stringResource(textRes), variant = variant)
        }
    }
}
