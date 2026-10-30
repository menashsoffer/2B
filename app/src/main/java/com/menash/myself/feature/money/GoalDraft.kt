package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.domain.Goal
import java.time.YearMonth

/** What the goal sheet holds; amounts stay text until save. Only [name] and [amount] are required. */
data class GoalDraft(
    val id: String? = null,
    val name: String = "",
    val amount: String = "",
    val targetMonth: YearMonth,
    val allocated: String = "",
    val monthlyCost: String = "",
)

enum class GoalField { NAME, AMOUNT }

/** A new goal starts a year from now: far enough to plan, close enough to feel real. */
fun newGoalDraft(month: YearMonth): GoalDraft = GoalDraft(targetMonth = month.plusYears(1))

fun goalDraftOf(goal: Goal): GoalDraft = GoalDraft(
    id = goal.id,
    name = goal.name,
    amount = amountText(goal.amount),
    targetMonth = goal.targetMonth,
    allocated = goal.allocated.takeUnless { it.isZero() }?.let(::amountText).orEmpty(),
    monthlyCost = goal.newMonthlyCost?.let(::amountText).orEmpty(),
)

sealed interface GoalResult {
    data class Ok(val goal: Goal) : GoalResult
    data class Invalid(val field: GoalField) : GoalResult
}

/** An empty "already saved" is zero and an empty monthly cost is none; text that is not a number is ignored. */
fun buildGoal(draft: GoalDraft, newId: () -> String): GoalResult {
    val name = draft.name.trim()
    if (name.isEmpty()) return GoalResult.Invalid(GoalField.NAME)
    val amount = parseAmount(draft.amount)?.takeIf { it > Money.ZERO } ?: return GoalResult.Invalid(GoalField.AMOUNT)
    return GoalResult.Ok(
        Goal(
            id = draft.id ?: newId(),
            name = name,
            amount = amount,
            targetMonth = draft.targetMonth,
            allocated = parseAmount(draft.allocated) ?: Money.ZERO,
            newMonthlyCost = parseAmount(draft.monthlyCost)?.takeIf { it > Money.ZERO },
        )
    )
}
