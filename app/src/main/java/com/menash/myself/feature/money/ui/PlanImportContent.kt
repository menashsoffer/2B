package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.designsystem.theme.tabular
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.PlanBasis
import com.menash.myself.feature.money.PlanImportLine
import com.menash.myself.feature.money.PlanLineStatus
import com.menash.myself.feature.money.importer.PlanGroup
import java.time.format.TextStyle
import java.util.Locale

private val groupTitles = mapOf(
    PlanGroup.INCOME to R.string.plan_import_group_income,
    PlanGroup.FIXED to R.string.plan_import_group_fixed,
    PlanGroup.CURRENT to R.string.plan_import_group_current,
    PlanGroup.PERIODIC to R.string.plan_import_group_periodic,
)

/** The Paamonim draft: one checkbox per line that would change the plan; lines that would not are shown greyed. */
internal fun LazyListScope.planReviewBody(ui: ImportUi.PlanReview, actions: ImportActions) {
    val draft = ui.draft
    item(key = "plan_title") {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8), modifier = Modifier.padding(horizontal = Spacing.s8)) {
            Text(
                when (val basis = draft.basis) {
                    is PlanBasis.PaamonimFile -> stringResource(R.string.plan_import_subtitle,
                        basis.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("he")), hebrewMonthYear(draft.from))
                    is PlanBasis.History -> stringResource(R.string.plan_history_subtitle, basis.months.size,
                        hebrewMonthYear(basis.months.first()), hebrewMonthYear(basis.months.last()), hebrewMonthYear(draft.from))
                },
                style = AppType.caption, color = MaterialTheme.appColors.textSecondary,
            )
        }
    }
    groupTitles.forEach { (group, title) ->
        val lines = draft.lines.filter { it.group == group }
        if (lines.isEmpty()) return@forEach
        item(key = "plan_group_$group") { SectionTitle(stringResource(title)) }
        item(key = "plan_lines_$group") {
            MoneySurface(testTag = "plan_group_${group.name.lowercase()}") {
                lines.forEach { line -> PlanLineRow(line, line.key in ui.selected) { actions.onTogglePlanLine(line.key) } }
            }
        }
    }
    draft.debtRepayment?.let { debt ->
        item(key = "plan_debts") {
            Text(stringResource(R.string.plan_import_debts, formatMoney(debt)), style = AppType.caption,
                color = MaterialTheme.appColors.textSecondary, modifier = Modifier.padding(horizontal = Spacing.s8).testTag("plan_debts"))
        }
    }
    item(key = "plan_confirm") {
        val count = ui.selected.intersect(draft.selectableKeys).size
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (draft.selectableKeys.isEmpty()) {
                Text(stringResource(R.string.plan_import_nothing), style = AppType.body, color = MaterialTheme.appColors.textSecondary,
                    modifier = Modifier.weight(1f).padding(horizontal = Spacing.s8).testTag("plan_nothing"))
            } else {
                PrimaryButton(text = stringResource(R.string.plan_import_confirm, count), onClick = actions.onConfirmPlan,
                    enabled = count > 0, modifier = Modifier.weight(1f).testTag("plan_confirm"))
            }
            TextButton(onClick = actions.onCancel, modifier = Modifier.testTag("plan_cancel")) {
                Text(stringResource(if (draft.selectableKeys.isEmpty()) R.string.import_close else R.string.import_cancel), style = AppType.body)
            }
        }
    }
}

internal fun LazyListScope.planDoneBody(ui: ImportUi.PlanDone, actions: ImportActions) {
    item(key = "plan_done") {
        Column(modifier = Modifier.fillMaxWidth().testTag("plan_done"), verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
            Text(stringResource(R.string.plan_import_done, ui.saved, hebrewMonthYear(ui.from)), style = AppType.title,
                color = MaterialTheme.appColors.textPrimary)
            Text(stringResource(R.string.plan_import_done_detail), style = AppType.body, color = MaterialTheme.appColors.textSecondary)
            PrimaryButton(text = stringResource(R.string.import_to_month), onClick = actions.onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A selectable line is one toggle (row and checkbox together); any other line is plain text with its reason. */
@androidx.compose.runtime.Composable
private fun PlanLineRow(line: PlanImportLine, checked: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.appColors
    val detail = when (line.status) {
        PlanLineStatus.NEW -> stringResource(R.string.plan_import_new)
        PlanLineStatus.NEW_CATEGORY -> stringResource(R.string.plan_import_new_category)
        PlanLineStatus.CHANGED -> stringResource(R.string.plan_import_changed, formatMoney(line.current ?: com.menash.myself.core.domain.model.Money.ZERO))
        PlanLineStatus.SAME -> stringResource(R.string.plan_import_same)
        PlanLineStatus.SPLIT -> line.current?.let { stringResource(R.string.plan_import_split, formatMoney(it)) }
            ?: stringResource(R.string.plan_import_split_unknown)
        PlanLineStatus.SUBSCRIPTIONS -> stringResource(R.string.plan_import_subscriptions)
    }
    val toggle = if (line.selectable) Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() }) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(toggle)
            .testTag("plan_line_${line.key}")
            .padding(horizontal = Spacing.s8, vertical = Spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        if (line.selectable) {
            Checkbox(checked = checked, onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = colors.accent, uncheckedColor = colors.textSecondary))
        }
        Column(modifier = Modifier.weight(1f).padding(start = if (line.selectable) 0.dp else Spacing.s8)) {
            Text(line.name, style = AppType.body, color = if (line.selectable) colors.textPrimary else colors.textSecondary)
            Text(detail, style = AppType.caption, color = colors.textSecondary)
        }
        Text(formatMoney(line.monthly), style = AppType.body.tabular(), color = if (line.selectable) colors.textPrimary else colors.textSecondary)
    }
}
