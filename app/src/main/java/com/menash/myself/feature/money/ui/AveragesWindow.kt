package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import java.time.YearMonth

@Composable
fun averagesWindowCaption(from: YearMonth?): String = if (from == null) {
    stringResource(R.string.averages_last_twelve)
} else {
    stringResource(R.string.averages_from_month, hebrewMonthYear(from))
}

/** The arrows use closed-month dates; switching to the default clears the saved start. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AveragesWindowPicker(from: YearMonth?, current: YearMonth, onChange: (YearMonth?) -> Unit) {
    Text(averagesWindowCaption(from), style = AppType.caption, color = MaterialTheme.appColors.textSecondary,
        modifier = Modifier.testTag("averages_caption"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Chip(text = stringResource(R.string.averages_twelve_choice), variant = if (from == null) ChipVariant.Success else ChipVariant.Default,
            onClick = { onChange(null) }, modifier = Modifier.testTag("averages_twelve"))
        Chip(text = stringResource(R.string.averages_month_choice), variant = if (from != null) ChipVariant.Success else ChipVariant.Default,
            onClick = { if (from == null) onChange(current.minusMonths(12)) }, modifier = Modifier.testTag("averages_choose_month"))
        if (from != null) {
            Chip(text = hebrewMonthYear(from), variant = ChipVariant.Success)
            Chip(text = stringResource(R.string.tithe_month_earlier), onClick = { onChange(from.minusMonths(1)) }, modifier = Modifier.testTag("averages_earlier"))
            if (from < current.minusMonths(1)) {
                Chip(text = stringResource(R.string.tithe_month_later), onClick = { onChange(from.plusMonths(1)) }, modifier = Modifier.testTag("averages_later"))
            }
        }
    }
}
