package com.menash.myself.ui.money

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.domain.Feasibility
import com.menash.myself.feature.money.domain.Lever

/** One answer in words: the headline, its colour, the line under it, and the levers that would make it work. */
data class AnswerText(val headline: String, val color: Color, val detail: String, val levers: List<String>)

@Composable
fun leverText(lever: Lever): String = when (lever) {
    is Lever.Postpone -> stringResource(R.string.goals_lever_postpone, hebrewMonthYear(lever.targetMonth))
    is Lever.TakeFromSavings -> stringResource(R.string.goals_lever_savings, formatMoney(lever.amount))
    is Lever.Cut -> stringResource(R.string.goals_lever_cut, formatMoney(lever.perMonth))
    is Lever.Installments -> stringResource(R.string.goals_lever_installments, lever.payments, formatMoney(lever.perMonth))
}

/** [answer] null means the target month has passed. */
@Composable
fun answerText(answer: Feasibility?): AnswerText {
    val colors = MaterialTheme.appColors
    return when (answer) {
        null -> AnswerText(
            stringResource(R.string.goals_passed), colors.textSecondary, stringResource(R.string.goals_passed_detail), emptyList(),
        )
        is Feasibility.Possible -> AnswerText(
            stringResource(R.string.goals_possible), colors.success,
            stringResource(R.string.goals_possible_detail, formatMoney(answer.monthlyAmount)), emptyList(),
        )
        is Feasibility.PossibleIf -> AnswerText(
            stringResource(R.string.goals_possible_if), colors.warning,
            stringResource(R.string.goals_possible_if_detail, formatMoney(answer.monthlyAmount)),
            answer.levers.map { leverText(it) },
        )
        is Feasibility.NotNow -> AnswerText(
            stringResource(R.string.goals_not_now), colors.danger,
            stringResource(R.string.goals_not_now_detail, formatMoney(answer.gapPerMonth)),
            listOfNotNull(answer.closestLever?.let { stringResource(R.string.goals_not_now_closest, leverText(it)) }),
        )
        Feasibility.Unknown -> AnswerText(
            stringResource(R.string.goals_unknown), colors.textSecondary, stringResource(R.string.goals_unknown_detail), emptyList(),
        )
    }
}
