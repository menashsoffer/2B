package com.menash.myself.testing

import android.content.Context
import android.icu.util.Calendar
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.menash.myself.core.designsystem.component.FloatingNavBar
import com.menash.myself.core.designsystem.component.FloatingNavItem
import com.menash.myself.feature.home.ui.HomeContent
import com.menash.myself.feature.home.ui.HomeUiState
import com.menash.myself.R
import com.menash.myself.feature.home.ui.hebrewDateLine
import com.menash.myself.shell.PlaceholderTab
import com.menash.myself.shell.ShellRoute
import com.menash.myself.shell.ShellTab
import com.menash.myself.shell.placeholderTabs
import com.menash.myself.shell.shellTabs

/** The only sample name used by R2 tests and screenshots. */
const val SampleName = "דנה"

/**
 * Fixed clock for every R2 test that shows a time or a date.
 * 2026-10-27 21:14 Asia/Jerusalem.
 *
 * [HomeContent] takes [HOUR] and [epochMillis]. [HomeContentAtFixedClock] is the only call site.
 */
object FixedClock {
    const val ZONE_ID = "Asia/Jerusalem"
    const val HOUR = 21
    const val MINUTE = 14

    val epochMillis: Long = Calendar.getInstance(TimeZone.getTimeZone(ZONE_ID)).apply {
        set(2026, Calendar.OCTOBER, 27, HOUR, MINUTE, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    init {
        val clock = Calendar.getInstance(TimeZone.getTimeZone(ZONE_ID))
        clock.timeInMillis = epochMillis
        check(clock.get(Calendar.HOUR_OF_DAY) == HOUR && clock.get(Calendar.MINUTE) == MINUTE) {
            "Fixed clock drifted: $clock"
        }
    }
}

/** The returning-visit greeting. The hour no longer changes the words. */
fun greetingText(context: Context, hour: Int): String {
    check(hour in 0..23)
    return context.getString(R.string.greeting_return)
}

/** Date line at [FixedClock.epochMillis]. */
fun dateLineText(context: Context): String {
    check(context.packageName.isNotEmpty())
    return hebrewDateLine(FixedClock.epochMillis)
}

@Composable
fun HomeContentAtFixedClock(
    dreamsActive: Int?,
    onOpenRoute: (String) -> Unit = {},
    onOpenAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HomeContent(
        state = HomeUiState(firstName = SampleName, dreamsActive = dreamsActive),
        hour = FixedClock.HOUR,
        epochMillis = FixedClock.epochMillis,
        onOpenRoute = onOpenRoute,
        onOpenAccount = onOpenAccount,
        modifier = modifier,
    )
}

@Composable
fun r2NavItems(): List<FloatingNavItem> = shellTabs.map { tab -> tab.toNavItem() }

@Composable
fun r2NavBar(
    selectedRoute: String = ShellRoute.Home,
    onSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    FloatingNavBar(
        items = r2NavItems(),
        selectedRoute = selectedRoute,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun r2Placeholder(route: String) {
    val tab = placeholderTabs.first { item -> item.route == route }
    PlaceholderTab(
        title = stringResource(tab.labelRes),
        icon = tab.icon,
        route = tab.route,
    )
}

@Composable
fun r2HomeShell(dreamsActive: Int?) {
    Box(modifier = Modifier.fillMaxSize()) {
        HomeContentAtFixedClock(
            dreamsActive = dreamsActive,
            modifier = Modifier.fillMaxSize(),
        )
        r2NavBar()
    }
}

@Composable
private fun ShellTab.toNavItem(): FloatingNavItem = FloatingNavItem(
    route = route,
    label = stringResource(labelRes),
    icon = icon,
    testTag = testTag,
)
