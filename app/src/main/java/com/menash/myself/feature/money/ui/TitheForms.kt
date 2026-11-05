package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.DonationDraft
import com.menash.myself.feature.money.DonationEditor
import com.menash.myself.feature.money.TitheSettingsDraft
import com.menash.myself.feature.money.domain.Asset
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy")

@Composable
private fun SheetColumn(tag: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) { content() }
}

/** The donation sheet body; stateless so goldens render it without a window. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonationForm(
    editor: DonationEditor,
    today: LocalDate,
    onChange: ((DonationDraft) -> DonationDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val draft = editor.draft
    SheetColumn("tithe_donation_form") {
        Text(
            text = stringResource(if (editor.isNew) R.string.tithe_donation_new else R.string.tithe_donation_edit),
            style = AppType.title,
            color = colors.textPrimary,
        )
        TextInput(
            value = draft.amount,
            onValueChange = { value -> onChange { it.copy(amount = value) } },
            label = stringResource(R.string.tithe_field_amount),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            error = stringResource(R.string.tithe_error_amount).takeIf { editor.invalid },
            modifier = Modifier.testTag("tithe_field_amount"),
        )
        TextInput(
            value = draft.organisation,
            onValueChange = { value -> onChange { it.copy(organisation = value) } },
            label = stringResource(R.string.tithe_field_organisation),
            modifier = Modifier.testTag("tithe_field_organisation"),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Chip(text = draft.date.format(dayFormat), variant = ChipVariant.Success)
            Chip(text = stringResource(R.string.tithe_day_back), onClick = { onChange { it.copy(date = it.date.minusDays(1)) } })
            Chip(text = stringResource(R.string.tithe_week_back), onClick = { onChange { it.copy(date = it.date.minusWeeks(1)) } })
            Chip(text = stringResource(R.string.tithe_month_back), onClick = { onChange { it.copy(date = it.date.minusMonths(1)) } })
            if (draft.date != today) Chip(text = stringResource(R.string.tithe_today), onClick = { onChange { it.copy(date = today) } })
        }
        SwitchRow(stringResource(R.string.tithe_field_submitted), draft.submitted) { checked -> onChange { it.copy(submitted = checked) } }
        PrimaryButton(text = stringResource(R.string.tithe_save), onClick = onSave, isLoading = editor.saving, modifier = Modifier.fillMaxWidth().testTag("tithe_save"))
        SaveFailure(editor.saveError, onSave)
        if (!editor.isNew) {
            TextButton(onClick = onDelete, modifier = Modifier.testTag("tithe_delete")) {
                Text(stringResource(R.string.tithe_delete), style = AppType.body, color = colors.danger)
            }
        }
    }
}

/** Where the count starts, what was open then, and which asset is the portfolio. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitheSettingsForm(
    draft: TitheSettingsDraft,
    month: YearMonth,
    assets: List<Asset>,
    onChange: ((TitheSettingsDraft) -> TitheSettingsDraft) -> Unit,
    onSave: () -> Unit,
    firstDataMonth: YearMonth = month,
) {
    val colors = MaterialTheme.appColors
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    SheetColumn("tithe_settings_form") {
        Text(text = stringResource(R.string.tithe_settings_title), style = AppType.title, color = colors.textPrimary)
        PillTabs(
            options = listOf(stringResource(R.string.tithe_from_first_data, hebrewMonthYear(firstDataMonth)), stringResource(R.string.tithe_from_month)),
            selectedIndex = if (draft.fromFirstData) 0 else 1,
            onSelect = { index -> onChange { it.copy(fromFirstData = index == 0) } },
        )
        if (!draft.fromFirstData) FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            val start = draft.startMonth
            Chip(text = stringResource(R.string.tithe_start_month, hebrewMonthYear(start)), variant = ChipVariant.Success)
            Chip(text = stringResource(R.string.tithe_month_earlier), onClick = { onChange { it.copy(startMonth = start.minusMonths(1)) } })
            if (start < month) {
                Chip(text = stringResource(R.string.tithe_month_later), onClick = { onChange { it.copy(startMonth = start.plusMonths(1)) } })
            }
        }
        TextInput(
            value = draft.opening,
            onValueChange = { value -> onChange { it.copy(opening = value) } },
            label = stringResource(R.string.tithe_field_opening),
            keyboardOptions = number,
            modifier = Modifier.testTag("tithe_field_opening"),
        )
        PillTabs(
            options = listOf(stringResource(R.string.tithe_opening_owed), stringResource(R.string.tithe_opening_ahead)),
            selectedIndex = if (draft.ahead) 1 else 0,
            onSelect = { index -> onChange { it.copy(ahead = index == 1) } },
        )
        Text(text = stringResource(R.string.tithe_portfolio), style = AppType.caption, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Chip(
                text = stringResource(R.string.tithe_portfolio_none),
                variant = if (draft.portfolioAssetId == null) ChipVariant.Success else ChipVariant.Default,
                onClick = { onChange { it.copy(portfolioAssetId = null) } },
            )
            assets.forEach { asset ->
                Chip(
                    text = asset.name,
                    variant = if (draft.portfolioAssetId == asset.id) ChipVariant.Success else ChipVariant.Default,
                    onClick = { onChange { it.copy(portfolioAssetId = asset.id) } },
                    modifier = Modifier.testTag("tithe_portfolio_${asset.id}"),
                )
            }
        }
        if (draft.portfolioAssetId != null) {
            TextInput(
                value = draft.depositsBefore,
                onValueChange = { value -> onChange { it.copy(depositsBefore = value) } },
                label = stringResource(R.string.tithe_field_deposits),
                keyboardOptions = number,
                modifier = Modifier.testTag("tithe_field_deposits"),
            )
            Text(text = stringResource(R.string.tithe_deposits_note), style = AppType.caption, color = colors.textSecondary)
        }
        PrimaryButton(text = stringResource(R.string.tithe_save), onClick = onSave, modifier = Modifier.fillMaxWidth().testTag("tithe_settings_save"))
    }
}
