package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Smoke screenshot of [PrimaryButton] in the light and dark themes.
 * Goldens: app/src/test/snapshots/designsystem.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class PrimaryButtonScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryButton_light() {
        captureButton(darkTheme = false)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h800dp-night-xhdpi")
    fun primaryButton_dark() {
        captureButton(darkTheme = true)
    }

    private fun captureButton(darkTheme: Boolean) {
        composeRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .background(MaterialTheme.appColors.background)
                        .padding(Spacing.s16)
                        .testTag(ScreenshotTag),
                ) {
                    PrimaryButton(
                        text = "שמירה",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        composeRule.onNodeWithTag(ScreenshotTag).captureRoboImage()
    }

    private companion object {
        const val ScreenshotTag = "primary_button_screenshot"
    }
}
