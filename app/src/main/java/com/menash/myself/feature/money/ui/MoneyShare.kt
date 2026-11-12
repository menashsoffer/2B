package com.menash.myself.feature.money.ui

import com.menash.myself.feature.money.CarSheet
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.ImportViewModel
import com.menash.myself.feature.money.MoneyViewModel
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.isDirty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A shared file asks before it drops work the user has actually changed.
 * An editor that is still the draft it opened with closes without asking.
 */
fun shareBlocked(
    month: MoneyViewModel,
    goals: GoalsViewModel,
    tithe: TitheViewModel,
    pension: PensionViewModel,
    car: CarViewModel,
    close: MonthCloseViewModel,
    import: ImportViewModel,
): Boolean {
    val sheets = tithe.ui.value.sheets
    return shareBlocked(
        monthDirty = month.editor.value?.isDirty() == true,
        goalEditor = goals.ui.value.editor?.let { it.draft != it.opened } == true,
        titheEditor = sheets.donation?.let { it.draft != it.opened } == true ||
            (sheets.settings != null && sheets.settings != sheets.settingsOpened),
        pensionEditor = pension.ui.value.editor?.let { it.draft != it.opened } == true,
        carEditor = when (val sheet = car.ui.value.sheet) {
            is CarSheet.Categories -> sheet.selected != sheet.opened
            is CarSheet.Compare -> sheet.draft != sheet.opened
            null -> false
        },
        closeBalances = close.ui.value.blocksShare(),
        importWork = import.ui.value.blocksShare(),
    )
}

/** [shareBlocked] as a flow, so a waiting shared file is read as soon as the form saves or closes. */
fun shareBlockedFlow(
    month: MoneyViewModel,
    goals: GoalsViewModel,
    tithe: TitheViewModel,
    pension: PensionViewModel,
    car: CarViewModel,
    close: MonthCloseViewModel,
    import: ImportViewModel,
): Flow<Boolean> = combine(
    listOf<Flow<Any?>>(month.editor, goals.ui, tithe.ui, pension.ui, car.ui, close.ui, import.ui),
) { shareBlocked(month, goals, tithe, pension, car, close, import) }.distinctUntilChanged()

fun shareBlocked(
    monthDirty: Boolean,
    goalEditor: Boolean,
    titheEditor: Boolean,
    pensionEditor: Boolean,
    carEditor: Boolean,
    closeBalances: Boolean,
    importWork: Boolean = false,
): Boolean = monthDirty || goalEditor || titheEditor || pensionEditor || carEditor || closeBalances || importWork

/** Category picks, or a plan review with lines switched off. A fresh review is not work yet. */
fun ImportUi.blocksShare(): Boolean = when (this) {
    is ImportUi.Review -> decisions.isNotEmpty() || picking != null
    is ImportUi.PlanReview -> selected != draft.selectableKeys
    else -> false
}

/** "אין חיובים", a confirm step, or a balance the user typed. Opening the screen alone is not. */
fun MonthCloseUi.blocksShare(): Boolean =
    noLines.isNotEmpty() || confirmReclose || confirmMisc || balanceDraft.values.any { it.value.isNotBlank() }
