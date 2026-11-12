package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.MonthTask
import java.time.YearMonth

/**
 * "Update the month": the first step, one tap from the month tab, and the others folded under it.
 * Nothing is shown when there is nothing to do.
 */
@Composable
fun NextStepCard(tasks: List<MonthTask>, current: YearMonth, onTask: (MonthTask) -> Unit) {
    if (tasks.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    MoneySurface(testTag = "money_next_step") {
        Text(
            stringResource(R.string.next_step_title), style = AppType.caption, color = MaterialTheme.appColors.textSecondary,
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s4),
        )
        TaskRow(tasks.first(), current, onTask, testTag = "next_step_first")
        val rest = tasks.drop(1)
        if (rest.isNotEmpty()) {
            AmountRow(
                title = if (rest.size == 1) stringResource(R.string.next_step_more_one) else stringResource(R.string.next_step_more, rest.size),
                amount = null, titleStyle = AppType.caption,
                action = RowAction.Expand(expanded) { expanded = !expanded },
                testTag = "next_step_more",
            )
            if (expanded) rest.forEachIndexed { index, task -> TaskRow(task, current, onTask, testTag = "next_step_$index") }
        }
    }
}

@Composable
private fun TaskRow(task: MonthTask, current: YearMonth, onTask: (MonthTask) -> Unit, testTag: String) {
    val (title, detail) = when (task) {
        is MonthTask.Assign -> stringResource(R.string.task_assign, task.count) to monthDetail(task.month, current)
        is MonthTask.MissingFile -> stringResource(R.string.task_missing_file, sourceLabel(task.source.source), task.source.sourceId) to
            stringResource(R.string.task_before_close, hebrewMonthName(task.month.month))
        is MonthTask.UpdateBalance -> stringResource(R.string.task_update_balance, task.asset.name) to
            stringResource(R.string.task_before_close, hebrewMonthName(task.month.month))
        is MonthTask.Close -> stringResource(R.string.task_close, hebrewMonthYear(task.month)) to stringResource(R.string.task_close_detail)
        is MonthTask.Tithe -> stringResource(R.string.task_tithe, formatMoney(task.owed)) to stringResource(R.string.task_tithe_detail)
    }
    AmountRow(title = title, detail = detail, amount = null, action = RowAction.Open { onTask(task) }, testTag = testTag)
}

@Composable
private fun monthDetail(month: YearMonth, current: YearMonth): String =
    if (month == current) stringResource(R.string.task_this_month, hebrewMonthName(month.month))
    else stringResource(R.string.task_before_close, hebrewMonthName(month.month))
