package com.menash.myself.feature.home.ui

import com.menash.myself.core.domain.model.Money

/** [moneySurplus] is the engine's expected surplus for this month; null while the month is not set up. */
data class HomeUiState(
    val firstName: String? = null,
    val dreamsActive: Int? = null,
    val moneySurplus: Money? = null,
)
