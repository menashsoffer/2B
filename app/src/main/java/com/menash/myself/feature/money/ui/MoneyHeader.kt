package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.appColors

/** The money tab's title with the search entry on the end side; search is reachable from both tabs. */
@Composable
internal fun MoneyHeader(onSearch: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScreenHeader(title = stringResource(R.string.nav_money), modifier = Modifier.weight(1f))
        IconButton(onClick = onSearch, modifier = Modifier.testTag("money_search")) {
            Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search_open), tint = MaterialTheme.appColors.textPrimary)
        }
    }
}
