package com.menash.myself.feature.money

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MoneySamples.car
import com.menash.myself.feature.money.MoneySamples.filled
import com.menash.myself.feature.money.MoneySamples.rate
import com.menash.myself.feature.money.MoneySamples.shekels
import com.menash.myself.feature.money.MoneySamples.trip
import com.menash.myself.feature.money.domain.Feasibility
import com.menash.myself.feature.money.domain.Goal
import com.menash.myself.feature.money.domain.MonthPlan
import com.menash.myself.feature.money.domain.monthPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CanWeTest {
    private val october = YearMonth.of(2026, 10)
    private val withGoals = filled.copy(goals = listOf(car, trip))
    private val plan = monthPlan(october, filled.items, rate) as MonthPlan.Planned

    @Test
    fun noClosedMonth_usesThePlannedSurplusAndSaysSo() {
        val view = canWe(withGoals, october, rate)

        assertEquals(RateBasis.PLAN, view.basis)
        assertEquals(plan.expectedSurplus, view.monthlySaving)
    }

    @Test
    fun closedMonths_winOverThePlan() {
        val view = canWe(withGoals, october, rate, closedSaving = shekels(5_000))

        assertEquals(RateBasis.CLOSED_MONTHS, view.basis)
        assertEquals(shekels(5_000), view.monthlySaving)
    }

    @Test
    fun goals_areListedByTargetMonth() {
        val view = canWe(withGoals, october, rate, closedSaving = shekels(5_000))

        assertEquals(listOf(trip.id, car.id), view.goals.map { it.goal.id })
    }

    @Test
    fun aGoalThatFits_isPossibleAtItsMonthlyAmount() {
        // Trip: 13,000 left over 9 months is 1,444.45 a month; the car takes 1,500 a month of the 5,000.
        val answer = canWe(withGoals, october, rate, closedSaving = shekels(5_000)).goals.first().answer

        assertEquals(Feasibility.Possible(Money(144_445)), answer)
    }

    @Test
    fun theOtherGoals_takeTheirShareFirst() {
        val alone = canWe(filled.copy(goals = listOf(trip)), october, rate, closedSaving = shekels(1_500)).goals.single()
        val together = canWe(withGoals, october, rate, closedSaving = shekels(1_500)).goals.first()

        assertTrue(alone.answer is Feasibility.Possible)
        assertTrue(together.answer !is Feasibility.Possible)
    }

    @Test
    fun aPassedTarget_hasNoAnswer() {
        val old = Goal("goal-old", "מחשב", shekels(4_000), YearMonth.of(2026, 3))
        val view = canWe(filled.copy(goals = listOf(old)), october, rate, closedSaving = shekels(5_000))

        assertNull(view.goals.single().answer)
    }

    @Test
    fun noPlanAndNoClosedMonth_isUnknown() {
        val view = canWe(MoneySamples.empty.copy(goals = listOf(trip)), october, rate)

        assertNull(view.basis)
        assertEquals(Feasibility.Unknown, view.goals.single().answer)
    }

    @Test
    fun preview_leavesTheEditedGoalOutOfTheOthers() {
        val bigger = trip.copy(amount = shekels(16_000))
        val preview = previewAnswer(bigger, filled.copy(goals = listOf(trip)), october, rate, closedSaving = shekels(5_000))

        // 14,000 over 9 months, judged alone (the stored trip is the same goal).
        assertEquals(Feasibility.Possible(Money(155_556)), preview)
    }

    @Test
    fun preview_ofAPassedTarget_isNull() {
        assertNull(previewAnswer(trip.copy(targetMonth = YearMonth.of(2026, 9)), filled, october, rate, shekels(5_000)))
    }
}
