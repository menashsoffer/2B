package com.menash.myself.ui.money

import androidx.compose.runtime.staticCompositionLocalOf

/** Depth preserves the existing nested flows: close → import and maaser → transactions. */
sealed class MoneyDestination(val depth: Int) {
    data object Root : MoneyDestination(0)
    data object Goals : MoneyDestination(1)
    data object Tithe : MoneyDestination(1)
    data object Car : MoneyDestination(1)
    data object Pension : MoneyDestination(1)
    data object Close : MoneyDestination(1)
    data object Subscriptions : MoneyDestination(1)
    data object Transactions : MoneyDestination(2)
    data object Import : MoneyDestination(3)
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
)

fun moneyDestination(flags: MoneyOpenFlags): MoneyDestination = when {
    flags.importing -> MoneyDestination.Import
    flags.transactions -> MoneyDestination.Transactions
    flags.goals -> MoneyDestination.Goals
    flags.tithe -> MoneyDestination.Tithe
    flags.car -> MoneyDestination.Car
    flags.pension -> MoneyDestination.Pension
    flags.close -> MoneyDestination.Close
    flags.subscriptions -> MoneyDestination.Subscriptions
    else -> MoneyDestination.Root
}
