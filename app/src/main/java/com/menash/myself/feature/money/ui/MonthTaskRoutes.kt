package com.menash.myself.feature.money.ui

import com.menash.myself.feature.money.MonthCloseViewModel
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
