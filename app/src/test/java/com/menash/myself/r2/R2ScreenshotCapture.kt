package com.menash.myself.r2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

/**
 * Robolectric screen qualifiers for screenshot and layout tests.
 * Night is a qualifier (`w360dp-h800dp-night-xhdpi`). Font scale is not a qualifier:
 * a method `@Config` replaces the class qualifiers, so font scale is applied with
 * [ProvideR2FontScale] (absolute `LocalDensity.fontScale`, not multiplied by the host).
 * Layout tests that must also set `Configuration.fontScale` pass the same value to
 * `@Config(fontScale)` and to [ProvideR2FontScale].
 */
const val LightQualifiers = "w360dp-h800dp-xhdpi"
const val DarkQualifiers = "w360dp-h800dp-night-xhdpi"

const val R2ScreenshotTag = "r2_screenshot"

@Composable
fun ProvideR2FontScale(
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    val host = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = host.density, fontScale = fontScale),
    ) {
        content()
    }
}

fun ComposeContentTestRule.captureR2Screen(
    darkTheme: Boolean,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    captureTagged(darkTheme, fontScale) {
        Box(
            modifier = Modifier
                .size(width = 360.dp, height = 800.dp)
                .background(MaterialTheme.appColors.background),
        ) {
            content()
        }
    }
}

fun ComposeContentTestRule.captureR2FullWidth(
    darkTheme: Boolean,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    captureTagged(darkTheme, fontScale) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .background(MaterialTheme.appColors.background)
                .padding(Spacing.s16),
        ) {
            content()
        }
    }
}

fun ComposeContentTestRule.captureR2Wrap(
    darkTheme: Boolean,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    captureTagged(darkTheme, fontScale) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .background(MaterialTheme.appColors.background)
                .padding(Spacing.s16),
        ) {
            content()
        }
    }
}

private fun ComposeContentTestRule.captureTagged(
    darkTheme: Boolean,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    setContent {
        ProvideR2FontScale(fontScale) {
            MyApplicationTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.testTag(R2ScreenshotTag)) {
                    content()
                }
            }
        }
    }
    onNodeWithTag(R2ScreenshotTag).captureRoboImage()
    // UX rules ride on every golden capture (docs/ux/UX-CHECKS.md); report-only, never fails the golden.
    val variant = "${if (darkTheme) "dark" else "light"}-font$fontScale"
    runCatching {
        UxRules.record(onNodeWithTag(R2ScreenshotTag).fetchSemanticsNode(), variant)
    }
    UxRules.recordAccessibilityChecks(this, variant)
}
