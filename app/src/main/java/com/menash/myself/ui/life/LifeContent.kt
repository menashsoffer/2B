package com.menash.myself.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.ui.me.MeRow
import com.menash.myself.ui.shell.ShellRoute

/** One entry in «חיים»: the label, its icon and the route it opens. */
data class LifeEntry(val labelRes: Int, val icon: ImageVector, val route: String, val testTag: String)

/** Every life feature has exactly one way in, from here. */
val lifeEntries: List<LifeEntry> = listOf(
    LifeEntry(R.string.life_tasks, Icons.Outlined.CheckCircle, ShellRoute.Tasks, "life_tasks"),
    LifeEntry(R.string.life_dreams, Icons.Outlined.StarOutline, ShellRoute.DreamsLegacy, "life_dreams"),
    LifeEntry(R.string.life_fitness, Icons.Outlined.FitnessCenter, ShellRoute.Fitness, "life_fitness"),
    LifeEntry(R.string.life_calendar, Icons.Outlined.CalendarMonth, ShellRoute.Calendar, "life_calendar"),
    LifeEntry(R.string.life_inbox, Icons.Outlined.Inbox, ShellRoute.Inbox, "life_inbox"),
)

/** «חיים» (debug builds only): tasks, dreams, fitness, calendar and the capture inbox. */
@Composable
fun LifeContent(onOpenRoute: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("life_screen"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        ScreenHeader(title = stringResource(R.string.life_title), subtitle = stringResource(R.string.life_subtitle))
        Surface(color = colors.card, shape = AppShapes.card, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = Spacing.s4)) {
                lifeEntries.forEach { entry ->
                    MeRow(
                        title = stringResource(entry.labelRes),
                        icon = entry.icon,
                        testTag = entry.testTag,
                        onClick = { onOpenRoute(entry.route) },
                    )
                }
            }
        }
    }
}
