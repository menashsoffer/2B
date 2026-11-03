package com.menash.myself.ui.money

import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.MoneyScreenState
import com.menash.myself.feature.money.data.MoneyEntry
import java.time.YearMonth

/** Callbacks of the money tab. Everything shown comes from [MoneyScreenState]; nothing is computed here. */
data class MoneyActions(
    val onSelectTab: (Int) -> Unit = {},
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onShowMonth: (YearMonth) -> Unit = {},
    val onToggleRow: (MonthRowKind) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onStart: () -> Unit = {},
    val onEdit: (MoneyEntry) -> Unit = {},
    val onAddKind: (com.menash.myself.feature.money.EditKind) -> Unit = {},
    val onTransactions: (LineFilter) -> Unit = {},
    val onGoals: () -> Unit = {},
    val onTithe: () -> Unit = {},
    val onCloseMonth: () -> Unit = {},
    val onCar: () -> Unit = {},
    val onPension: () -> Unit = {},
    val onSubscriptions: () -> Unit = {},
    val onPeriod: () -> Unit = {},
    val onMonthSummary: (YearMonth) -> Unit = {},
    val onImport: () -> Unit = {},
    val onCategory: (String) -> Unit = {},
    val onResumeImport: (() -> Unit)? = null,
    val onSearch: () -> Unit = {},
)
