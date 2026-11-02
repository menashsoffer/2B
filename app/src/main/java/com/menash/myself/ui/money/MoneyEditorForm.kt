package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.DraftField
import com.menash.myself.feature.money.EditKind
import com.menash.myself.feature.money.EditorState
import com.menash.myself.feature.money.domain.AssetKind
import com.menash.myself.feature.money.domain.Currency
import com.menash.myself.feature.money.domain.Frequency
import com.menash.myself.feature.money.domain.TitheRate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn

/** The sheet body, stateless so goldens render it without a window. */
@Composable
fun MoneyEditorForm(
    editor: EditorState,
    choices: EditorChoices,
    actions: EditorActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val draft = editor.draft
    val kind = draft.kind
    val change = actions.onChange
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val whole = KeyboardOptions(keyboardType = KeyboardType.Number)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag("money_editor"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        Text(
            text = if (editor.isNew) kindLabel(kind) else stringResource(R.string.money_edit_title) + " · " + kindLabel(kind),
            style = AppType.title,
            color = colors.textPrimary,
        )
        TextInput(
            value = draft.name,
            onValueChange = { value -> change { it.copy(name = value) } },
            label = stringResource(if (kind == EditKind.DEBT) R.string.money_field_creditor else R.string.money_field_name),
            error = stringResource(R.string.money_error_name).takeIf { editor.error == DraftField.NAME },
            modifier = Modifier.testTag("money_field_name"),
        )
        TextInput(
            value = draft.amount,
            onValueChange = { value -> change { it.copy(amount = value) } },
            label = stringResource(amountLabel(kind)),
            keyboardOptions = if (kind == EditKind.CARD) whole else number,
            error = when (editor.error) {
                DraftField.AMOUNT -> stringResource(R.string.money_error_amount)
                DraftField.BILLING_DAY -> stringResource(R.string.money_error_billing_day)
                else -> null
            },
            modifier = Modifier.testTag("money_field_amount"),
        )

        if (kind == EditKind.INCOME) {
            val rates = TitheRate.entries
            Text(text = stringResource(R.string.money_tithe_label), style = AppType.caption, color = colors.textSecondary)
            PillTabs(
                options = listOf(
                    stringResource(R.string.money_tithe_none),
                    stringResource(R.string.money_tithe_maaser),
                    stringResource(R.string.money_tithe_chomesh),
                ),
                selectedIndex = rates.indexOf(draft.titheRate),
                onSelect = { index -> change { it.copy(titheRate = rates[index]) } },
                modifier = Modifier.testTag("money_tithe_rate"),
            )
        }
        if (kind == EditKind.FIXED || kind == EditKind.SUBSCRIPTION) {
            PillTabs(
                options = listOf(stringResource(R.string.money_monthly), stringResource(R.string.money_yearly)),
                selectedIndex = if (draft.frequency == Frequency.YEARLY) 1 else 0,
                onSelect = { index -> change { it.copy(frequency = if (index == 1) Frequency.YEARLY else Frequency.MONTHLY) } },
            )
            if (draft.frequency == Frequency.YEARLY) {
                val billing = if (kind == EditKind.FIXED) draft.billingMonth else draft.renewalDate?.month
                if (billing != null) {
                    Chip(
                        text = stringResource(R.string.money_billing_month, hebrewMonthName(billing)),
                        onClick = {
                            change {
                                if (kind == EditKind.FIXED) {
                                    it.copy(billingMonth = billing.plus(1))
                                } else {
                                    it.copy(renewalDate = it.renewalDate?.plusMonths(1))
                                }
                            }
                        },
                    )
                }
            }
        }
        if (kind == EditKind.SUBSCRIPTION) {
            PillTabs(
                options = listOf(stringResource(R.string.money_ils), stringResource(R.string.money_usd)),
                selectedIndex = if (draft.currency == Currency.USD) 1 else 0,
                onSelect = { index -> change { it.copy(currency = if (index == 1) Currency.USD else Currency.ILS) } },
            )
        }
        if (kind in setOf(EditKind.FIXED, EditKind.SUBSCRIPTION, EditKind.VARIABLE, EditKind.PERIODIC)) {
            CategoryPicker(draft, choices.categories, kind, change)
        }
        if (kind in setOf(EditKind.FIXED, EditKind.SUBSCRIPTION, EditKind.VARIABLE, EditKind.INSTALLMENT)) {
            CardPicker(draft, choices.cards, change, actions.onNewCard)
        }
        if (kind == EditKind.PERIODIC) {
            choices.preview.monthlyReserve?.let { reserve ->
                Text(
                    text = stringResource(R.string.money_periodic_reserve, formatMoney(reserve)),
                    style = AppType.body,
                    color = colors.textSecondary,
                    modifier = Modifier.testTag("money_periodic_reserve"),
                )
            }
        }
        if (kind == EditKind.INSTALLMENT) {
            TextInput(
                value = draft.payments,
                onValueChange = { value -> change { it.copy(payments = value) } },
                label = stringResource(R.string.money_field_payments),
                keyboardOptions = whole,
                error = stringResource(R.string.money_error_payments).takeIf { editor.error == DraftField.PAYMENTS },
            )
            draft.firstMonth?.let { first ->
                Chip(
                    text = stringResource(R.string.money_first_payment, hebrewMonthYear(first)),
                    onClick = { change { it.copy(firstMonth = first.plusMonths(1)) } },
                )
            }
            val firstPayment = choices.preview.firstPayment
            if (firstPayment != null) {
                val later = choices.preview.laterPayment
                Text(
                    text = if (later == null) {
                        stringResource(R.string.money_installment_single, formatMoney(firstPayment))
                    } else {
                        stringResource(R.string.money_installment_split, formatMoney(firstPayment), formatMoney(later))
                    },
                    style = AppType.body,
                    color = colors.textSecondary,
                    modifier = Modifier.testTag("money_installment_split"),
                )
            }
        }
        if (kind == EditKind.DEBT) {
            TextInput(
                value = draft.remainingPayments,
                onValueChange = { value -> change { it.copy(remainingPayments = value) } },
                label = stringResource(R.string.money_field_remaining),
                keyboardOptions = whole,
                error = stringResource(R.string.money_error_payments).takeIf { editor.error == DraftField.REMAINING_PAYMENTS },
            )
            TextInput(
                value = draft.balance,
                onValueChange = { value -> change { it.copy(balance = value) } },
                label = stringResource(R.string.money_field_debt_balance),
                keyboardOptions = number,
            )
        }
        if (kind == EditKind.SAVING) {
            TextInput(
                value = draft.accumulated,
                onValueChange = { value -> change { it.copy(accumulated = value) } },
                label = stringResource(R.string.money_field_accumulated),
                keyboardOptions = number,
            )
            choices.month?.let { month -> MaturityChips(draft.maturity, month) { value -> change { it.copy(maturity = value) } } }
        }
        if (kind == EditKind.ASSET) {
            SwitchRow(stringResource(R.string.money_liquid), draft.liquid) { checked -> change { it.copy(liquid = checked) } }
            SwitchRow(stringResource(R.string.money_is_liability), draft.assetKind == AssetKind.LIABILITY) { checked ->
                change { it.copy(assetKind = if (checked) AssetKind.LIABILITY else AssetKind.ASSET) }
            }
        }

        PrimaryButton(
            text = stringResource(R.string.money_save),
            onClick = actions.onSave,
            isLoading = editor.saving,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("money_save"),
        )
        SaveFailure(editor.saveError, actions.onSave)
        if (!editor.isNew) {
            TextButton(onClick = actions.onDelete, modifier = Modifier.fillMaxWidth().testTag("money_delete")) {
                Text(stringResource(R.string.money_delete), style = AppType.body, color = colors.danger)
            }
        }
    }
}

private fun amountLabel(kind: EditKind): Int = when (kind) {
    EditKind.PERIODIC -> R.string.money_field_annual
    EditKind.INSTALLMENT -> R.string.money_field_total
    EditKind.DEBT -> R.string.money_field_payment
    EditKind.SAVING -> R.string.money_field_deposit
    EditKind.ASSET -> R.string.money_field_balance
    EditKind.CARD -> R.string.money_field_billing_day
    else -> R.string.money_field_amount
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.appColors
    // The whole row is the switch, so a screen reader announces the label with it and the label is tappable.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = AppType.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.accent,
                checkedThumbColor = colors.onAccent,
                uncheckedTrackColor = colors.accentSoft,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedBorderColor = colors.cardBorder,
            ),
        )
    }
}

