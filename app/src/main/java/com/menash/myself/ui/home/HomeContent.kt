package com.menash.myself.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.HeroCard
import com.menash.myself.core.designsystem.component.MetricCard
import com.menash.myself.core.designsystem.component.MetricValueStyle
import com.menash.myself.core.designsystem.component.ProfileAvatar
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.ui.shell.ShellRoute

@Composable
fun HomeContent(
    state: HomeUiState,
    hour: Int,
    epochMillis: Long,
    onOpenRoute: (String) -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val dreamsValue = state.dreamsActive?.toString()
    val dreamsDetail = when (state.dreamsActive) {
        null -> stringResource(R.string.home_no_data)
        1 -> stringResource(R.string.home_dreams_active_one)
        else -> stringResource(R.string.home_dreams_active_many)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(
                start = Spacing.s16,
                end = Spacing.s16,
                top = Spacing.s8,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { isTraversalGroup = true },
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenHeader(
                title = homeTitle(state.firstName, stringResource(greetingFor(hour))),
                subtitle = hebrewDateLine(epochMillis),
                modifier = Modifier.weight(1f),
            )
            ProfileAvatar(
                displayName = state.firstName,
                onClick = onOpenAccount,
            )
        }
        HeroCard(
            label = stringResource(R.string.nav_money),
            message = stringResource(R.string.home_money_message),
            value = null,
            onClick = { onOpenRoute(ShellRoute.Money) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .semantics { isTraversalGroup = true },
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            MetricCard(
                label = stringResource(R.string.nav_dreams),
                value = dreamsValue,
                detail = dreamsDetail,
                onClick = { onOpenRoute(ShellRoute.Dreams) },
                valueStyle = MetricValueStyle.Medium,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            MetricCard(
                label = stringResource(R.string.nav_shopping),
                value = null,
                detail = stringResource(R.string.home_no_data),
                onClick = { onOpenRoute(ShellRoute.Shopping) },
                valueStyle = MetricValueStyle.Medium,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        MetricCard(
            label = stringResource(R.string.nav_tracking),
            value = null,
            detail = stringResource(R.string.home_no_data),
            onClick = { onOpenRoute(ShellRoute.Tracking) },
            valueStyle = MetricValueStyle.Medium,
        )
    }
}

@Preview(name = "Home empty light", showBackground = true, widthDp = 360, heightDp = 800, locale = "iw")
@Composable
private fun HomeEmptyLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        HomePreview(dreamsActive = null)
    }
}

@Preview(
    name = "Home empty dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    locale = "iw",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeEmptyDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        HomePreview(dreamsActive = null)
    }
}

@Preview(name = "Home dreams light", showBackground = true, widthDp = 360, heightDp = 800, locale = "iw")
@Composable
private fun HomeDreamsPreview() {
    MyApplicationTheme(darkTheme = false) {
        HomePreview(dreamsActive = 4)
    }
}

@Composable
private fun HomePreview(dreamsActive: Int?) {
    val calendar = android.icu.util.Calendar.getInstance()
    calendar.set(2026, android.icu.util.Calendar.OCTOBER, 27, 21, 14, 0)
    calendar.set(android.icu.util.Calendar.MILLISECOND, 0)
    HomeContent(
        state = HomeUiState(firstName = "דנה", dreamsActive = dreamsActive),
        hour = 21,
        epochMillis = calendar.timeInMillis,
        onOpenRoute = {},
        onOpenAccount = {},
    )
}
