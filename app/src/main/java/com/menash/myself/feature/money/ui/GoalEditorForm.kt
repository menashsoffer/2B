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
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.GoalDraft
import com.menash.myself.feature.money.GoalEditor
import com.menash.myself.feature.money.GoalField
import java.time.YearMonth

data class GoalEditorActions(
    val onChange: ((GoalDraft) -> GoalDraft) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

/** The goal sheet body, stateless so goldens render it without a window. The answer updates as the fields change. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalEditorForm(editor: GoalEditor, month: YearMonth, actions: GoalEditorActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val draft = editor.draft
    val change = actions.onChange
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag("goals_editor"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        Text(
            text = stringResource(if (editor.isNew) R.string.goals_new_title else R.string.goals_edit_title),
            style = AppType.title,
            color = colors.textPrimary,
        )
        TextInput(
            value = draft.name,
            onValueChange = { value -> change { it.copy(name = value) } },
            label = stringResource(R.string.goals_field_name),
            error = stringResource(R.string.goals_error_name).takeIf { editor.error == GoalField.NAME },
            modifier = Modifier.testTag("goals_field_name"),
        )
        TextInput(
            value = draft.amount,
            onValueChange = { value -> change { it.copy(amount = value) } },
            label = stringResource(R.string.goals_field_amount),
            keyboardOptions = number,
            error = stringResource(R.string.goals_error_amount).takeIf { editor.error == GoalField.AMOUNT },
            modifier = Modifier.testTag("goals_field_amount"),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s8),
            modifier = Modifier.testTag("goals_target"),
        ) {
            val target = draft.targetMonth
            Chip(text = stringResource(R.string.goals_target, hebrewMonthYear(target)), variant = ChipVariant.Success)
            if (target > month) {
                Chip(text = stringResource(R.string.goals_month_back), onClick = { change { it.copy(targetMonth = target.minusMonths(1)) } })
            }
            Chip(text = stringResource(R.string.goals_month_on), onClick = { change { it.copy(targetMonth = target.plusMonths(1)) } })
            Chip(text = stringResource(R.string.goals_year_on), onClick = { change { it.copy(targetMonth = target.plusYears(1)) } })
        }
        TextInput(
            value = draft.allocated,
            onValueChange = { value -> change { it.copy(allocated = value) } },
            label = stringResource(R.string.goals_field_allocated),
            keyboardOptions = number,
            modifier = Modifier.testTag("goals_field_allocated"),
        )
        TextInput(
            value = draft.monthlyCost,
            onValueChange = { value -> change { it.copy(monthlyCost = value) } },
            label = stringResource(R.string.goals_field_cost),
            keyboardOptions = number,
            modifier = Modifier.testTag("goals_field_cost"),
        )
        if (editor.preview != null || draft.targetMonth < month) {
            MoneySurface(testTag = "goals_preview") {
                Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8)) {
                    Text(text = stringResource(R.string.goals_preview), style = AppType.caption, color = colors.textSecondary)
                    AnswerBlock(answerText(editor.preview))
                }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.goals_save),
            onClick = actions.onSave,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goals_save"),
        )
        if (!editor.isNew) {
            TextButton(onClick = actions.onDelete, modifier = Modifier.testTag("goals_delete")) {
                Text(stringResource(R.string.goals_delete), style = AppType.body, color = colors.danger)
            }
        }
    }
}
