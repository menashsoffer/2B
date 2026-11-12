package com.menash.myself.feature.home.ui

import com.menash.myself.core.domain.model.Money
import com.menash.myself.feature.money.MonthTask
import com.menash.myself.feature.money.SourceFreshness

/**
 * [moneySurplus] is the engine's expected surplus for this month; null while the month is not set up.
 * [moneyLeftForCurrent] is "נשאר לשוטפות" once this month has imported lines and a current budget: it leads,
 * as on the month tab. [moneyTasks] and [moneyFreshness] come from the same moneyOverview as the month tab.
 */
data class HomeUiState(
    val firstName: String? = null,
    val dreamsActive: Int? = null,
    val moneySurplus: Money? = null,
    val moneyLeftForCurrent: Money? = null,
    val moneyTasks: List<MonthTask> = emptyList(),
    val moneyFreshness: SourceFreshness? = null,
)
