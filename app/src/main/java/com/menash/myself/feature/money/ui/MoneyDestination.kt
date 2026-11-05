package com.menash.myself.feature.money.ui

import androidx.compose.runtime.staticCompositionLocalOf

/** Depth preserves the existing nested flows: close → import, maaser → transactions and close → month summary. */
sealed class MoneyDestination(val depth: Int) {
    data object Root : MoneyDestination(0)
    data object Goals : MoneyDestination(1)
    data object Tithe : MoneyDestination(1)
    data object Car : MoneyDestination(1)
    data object Pension : MoneyDestination(1)
    data object Close : MoneyDestination(1)
    data object Subscriptions : MoneyDestination(1)
    data object Period : MoneyDestination(1)
    data object Transactions : MoneyDestination(2)
    data object Summary : MoneyDestination(2)
    data object Import : MoneyDestination(3)
    data object Trend : MoneyDestination(3)
}

val LocalMoneyBackActive = staticCompositionLocalOf { true }

data class MoneyOpenFlags(
    val importing: Boolean = false,
    val transactions: Boolean = false,
    val goals: Boolean = false,
    val tithe: Boolean = false,
    val car: Boolean = false,
    val pension: Boolean = false,
    val close: Boolean = false,
    val subscriptions: Boolean = false,
    val period: Boolean = false,
    val summary: Boolean = false,
    val trend: Boolean = false,
)

fun moneyDestination(flags: MoneyOpenFlags): MoneyDestination = when {
    flags.importing -> MoneyDestination.Import
    flags.transactions -> MoneyDestination.Transactions
    flags.trend -> MoneyDestination.Trend
    flags.summary -> MoneyDestination.Summary
    flags.goals -> MoneyDestination.Goals
    flags.tithe -> MoneyDestination.Tithe
    flags.car -> MoneyDestination.Car
    flags.pension -> MoneyDestination.Pension
    flags.close -> MoneyDestination.Close
    flags.subscriptions -> MoneyDestination.Subscriptions
    flags.period -> MoneyDestination.Period
    else -> MoneyDestination.Root
}
