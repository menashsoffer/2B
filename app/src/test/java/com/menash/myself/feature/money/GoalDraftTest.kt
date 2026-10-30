package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.car
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.MoneySamples.trip
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class GoalDraftTest {
    private val october = YearMonth.of(2026, 10)

    @Test
    fun aNewGoal_aimsAYearAhead() {
        assertEquals(YearMonth.of(2027, 10), newGoalDraft(october).targetMonth)
    }

    @Test
    fun aStoredGoal_comesBackUnchanged() {
        listOf(trip, car).forEach { goal ->
            assertEquals(GoalResult.Ok(goal), buildGoal(goalDraftOf(goal)) { error("keeps its id") })
        }
    }

    @Test
    fun nameAndAmount_areRequired() {
        val draft = newGoalDraft(october).copy(name = "  ", amount = "1000")
        assertEquals(GoalResult.Invalid(GoalField.NAME), buildGoal(draft) { "g" })
        assertEquals(GoalResult.Invalid(GoalField.AMOUNT), buildGoal(draft.copy(name = "מחשב", amount = "0")) { "g" })
        assertEquals(GoalResult.Invalid(GoalField.AMOUNT), buildGoal(draft.copy(name = "מחשב", amount = "abc")) { "g" })
    }

    @Test
    fun emptyOptionalFields_meanZeroAndNoCost() {
        val draft = newGoalDraft(october).copy(name = " מחשב ", amount = "4,500")
        val goal = (buildGoal(draft) { "goal-new" } as GoalResult.Ok).goal

        assertEquals("goal-new", goal.id)
        assertEquals("מחשב", goal.name)
        assertEquals(shekels(4_500), goal.amount)
        assertEquals(Money.ZERO, goal.allocated)
        assertEquals(null, goal.newMonthlyCost)
    }
}
