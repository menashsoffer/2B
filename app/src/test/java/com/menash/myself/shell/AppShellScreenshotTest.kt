package com.menash.myself.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.testing.DarkQualifiers
import com.menash.myself.testing.HomeContentAtFixedClock
import com.menash.myself.testing.LightQualifiers
import com.menash.myself.testing.captureR2Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home tab of the shell at font scale 1.0, one golden per theme.
 * RTL is pinned with [LocalLayoutDirection]. Name, greeting, and the Hebrew date
 * come from [com.menash.myself.testing.FixedClock] via [HomeContentAtFixedClock].
 *
 * The tree is [AppShellContent], the stateless shell [AppShell] renders, with the
 * home content in its slot. [AppShell] itself needs MainViewModel and Firestore.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LightQualifiers)
class AppShellScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appShell_home_light_font10() = capture(darkTheme = false)

    @Test
    @Config(sdk = [36], qualifiers = DarkQualifiers)
    fun appShell_home_dark_font10() = capture(darkTheme = true)

    private fun capture(darkTheme: Boolean) {
        composeRule.captureR2Screen(darkTheme = darkTheme, fontScale = 1.0f) {
            AppShellHomeAtFixedClock()
        }
    }
}

@Composable
private fun AppShellHomeAtFixedClock() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AppShellContent(
            state = AppShellState(selectedRoute = ShellRoute.Home),
            onDismissError = {},
            onSelectTab = {},
        ) { contentModifier ->
            HomeContentAtFixedClock(
                dreamsActive = null,
                modifier = contentModifier.fillMaxSize(),
            )
        }
    }
}
