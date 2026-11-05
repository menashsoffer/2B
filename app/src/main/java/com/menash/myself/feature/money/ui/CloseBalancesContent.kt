package com.menash.myself.feature.money.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.menash.myself.core.designsystem.theme.Spacing
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.parseCloseBalance
import com.menash.myself.feature.money.domain.hasSuspectBalances

@Composable
fun CloseBalancesContent(ui: MonthCloseUi, actions: MonthCloseActions) {
    ui.saveError?.let { Text(it, style = AppType.body) }
    if (ui.view?.existing?.hasSuspectBalances() == true) {
        Text(stringResource(R.string.close_balances_suspect), style = AppType.body)
        TextButton(onClick = actions.onMarkUnknown, enabled = !ui.saving) {
            Text(stringResource(R.string.close_balances_mark_unknown), style = AppType.body)
        }
    }
    if (!ui.showBalancesStep) return
    SectionTitle(stringResource(R.string.close_balances_title))
    MoneySurface(testTag = "close_balances") {
        BalanceChoice(stringResource(R.string.close_balances_skip), !ui.balanceDraft.entering, !ui.saving) {
            actions.onEnterBalances(false)
        }
        BalanceChoice(stringResource(R.string.close_balances_enter), ui.balanceDraft.entering, !ui.saving) {
            actions.onEnterBalances(true)
        }
        if (ui.balanceDraft.entering) ui.assets.forEach { asset ->
            val value = ui.balanceDraft.values[asset.id].orEmpty()
            TextInput(
                value = value, onValueChange = { actions.onBalance(asset.id, it) },
                label = asset.name, modifier = Modifier.testTag("close_balance_${asset.id}"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                error = if (value.isNotBlank() && parseCloseBalance(value) == null)
                    stringResource(R.string.close_balances_invalid) else null,
            )
        }
    }
}


@Composable
private fun BalanceChoice(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, style = AppType.body, modifier = Modifier.padding(Spacing.s8))
    }
}
