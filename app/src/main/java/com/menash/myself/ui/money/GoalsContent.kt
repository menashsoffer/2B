package com.menash.myself.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.FloatingNavBarDefaults
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.GoalAnswer
import com.menash.myself.feature.money.GoalsUi
import com.menash.myself.feature.money.RateBasis
import com.menash.myself.feature.money.domain.Goal

data class GoalsActions(
    val onAdd: () -> Unit = {},
    val onGoal: (Goal) -> Unit = {},
    val onClose: () -> Unit = {},
)

/** "האם אפשר?" full screen inside the money tab: what the answers are based on, then each goal and its answer. */
@Composable
fun GoalsContent(ui: GoalsUi, actions: GoalsActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val view = ui.view
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(FloatingNavBarDefaults.contentPadding())
            .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8)
            .testTag("goals_content"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        ScreenHeader(title = stringResource(R.string.goals_title), subtitle = stringResource(R.string.goals_subtitle))
        val saving = view.monthlySaving
        val basis = when {
            view.basis == RateBasis.CLOSED_MONTHS && saving != null -> stringResource(R.string.goals_basis_closed, formatMoney(saving))
            view.basis == RateBasis.PLAN && saving != null -> stringResource(R.string.goals_basis_plan, formatMoney(saving))
            else -> stringResource(R.string.goals_basis_none)
        }
        Text(text = basis, style = AppType.body, color = colors.textSecondary, modifier = Modifier.testTag("goals_basis"))
        if (view.goals.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.goals_empty_title),
                description = stringResource(R.string.goals_empty_detail),
                icon = Icons.Outlined.Flag,
                testTag = "goals_empty",
            )
        } else {
            view.goals.forEach { GoalCard(it, onClick = { actions.onGoal(it.goal) }) }
            Text(text = stringResource(R.string.goals_cushion_note), style = AppType.caption, color = colors.textSecondary)
        }
        PrimaryButton(
            text = stringResource(R.string.goals_add),
            onClick = actions.onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goals_add"),
        )
        TextButton(onClick = actions.onClose, modifier = Modifier.testTag("goals_back")) {
            Text(stringResource(R.string.goals_back), style = AppType.body)
        }
    }
}

@Composable
private fun GoalCard(item: GoalAnswer, onClick: () -> Unit) {
    val goal = item.goal
    val detail = listOfNotNull(
        stringResource(R.string.goals_goal_detail, formatMoney(goal.amount), hebrewMonthYear(goal.targetMonth)),
        goal.allocated.takeUnless { it.isZero() }?.let { stringResource(R.string.goals_goal_allocated, formatMoney(it)) },
        goal.newMonthlyCost?.let { stringResource(R.string.goals_goal_cost, formatMoney(it)) },
    ).joinToString(" · ")
    MoneySurface(modifier = Modifier.clickable(onClick = onClick), testTag = "goals_goal_${goal.id}") {
        AmountRow(title = goal.name, detail = detail, amount = null, titleStyle = AppType.title)
        AnswerBlock(answerText(item.answer), Modifier.padding(start = Spacing.s16, end = Spacing.s16, bottom = Spacing.s8))
    }
}

/** The headline in its colour, the line under it, and one line per lever. Shared by the list and the sheet preview. */
@Composable
fun AnswerBlock(text: AnswerText, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        Text(text = text.headline, style = AppType.title, color = text.color)
        Text(text = text.detail, style = AppType.body, color = colors.textPrimary)
        text.levers.forEach { lever ->
            Text(text = "• $lever", style = AppType.body, color = colors.textSecondary)
        }
    }
}
