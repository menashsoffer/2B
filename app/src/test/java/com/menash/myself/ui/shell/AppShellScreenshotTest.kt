package com.menash.myself.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.r2.DarkQualifiers
import com.menash.myself.r2.HomeContentAtFixedClock
import com.menash.myself.r2.LightQualifiers
import com.menash.myself.r2.captureR2Screen
import com.menash.myself.r2.r2NavBar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home tab of the shell at font scale 1.0, one golden per theme.
 * RTL is pinned with [LocalLayoutDirection]. Name, greeting, and the Hebrew date
 * come from [com.menash.myself.r2.FixedClock] via [HomeContentAtFixedClock].
 *
 * [AppShell] is not called: it requires [com.menash.myself.ui.MainViewModel] and
 * [com.menash.myself.core.di.LocalAppContainer], and the home route's display name
 * is a Firebase user. This tree is the home content plus the floating nav,
 * placed the same way [AppShell] places the bar.
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
        Box(modifier = Modifier.fillMaxSize()) {
            HomeContentAtFixedClock(
                dreamsActive = null,
                modifier = Modifier.fillMaxSize(),
            )
            r2NavBar(
                selectedRoute = ShellRoute.Home,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(
                        start = FloatingNavBarDefaults.outerMargin,
                        end = FloatingNavBarDefaults.outerMargin,
                        bottom = FloatingNavBarDefaults.outerMargin,
                    ),
            )
        }
    }
}
