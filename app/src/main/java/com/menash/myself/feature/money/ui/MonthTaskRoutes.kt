package com.menash.myself.feature.money.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.MonthTaskInbox
import com.menash.myself.feature.money.MonthTask
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.TransactionsViewModel
import com.menash.myself.feature.money.importer.LineFilter

/**
 * Where each step of "update the month" is done. Lines without a category open straight on the list of those
 * lines; every check of a month awaiting close opens that month's close screen, which handles all of them.
 */
internal fun openTask(task: MonthTask, transactions: TransactionsViewModel, close: MonthCloseViewModel, tithe: TitheViewModel) {
    when (task) {
        is MonthTask.Assign -> transactions.open(task.month, LineFilter.UNASSIGNED)
        is MonthTask.MissingFile -> close.open(task.month)
        is MonthTask.UpdateBalance -> close.open(task.month)
        is MonthTask.Close -> close.open(task.month)
        is MonthTask.Tithe -> tithe.open()
    }
}

/**
 * Opens a step tapped on the home screen once the money tab is shown. The money screens keep their state across
 * tabs, and a screen left open (an import review, the transactions list) ranks above the step's screen, so every
 * money screen is closed first ([closeAll]); an import in progress keeps its review and can be resumed.
 */
@Composable
internal fun OpenTaskFromInbox(
    inbox: MonthTaskInbox, transactions: TransactionsViewModel, close: MonthCloseViewModel, tithe: TitheViewModel,
    closeAll: List<() -> Unit>,
) {
    val pending by inbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pending) {
        inbox.take()?.let { task ->
            closeAll.forEach { it() }
            openTask(task, transactions, close, tithe)
        }
    }
}
