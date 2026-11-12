package com.menash.myself.feature.money.ui

import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.MoneyViewModel
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.isDirty

/**
 * A shared file must not call [MoneyNavVms.closeAll] while a money editor is open.
 * close() drops the sheet, so a half-typed goal, tithe, pension, car, or close-balance draft would vanish.
 * The month sheet is cleared only on purpose, and only once it is dirty.
 */
fun shareBlocked(
    month: MoneyViewModel,
    goals: GoalsViewModel,
    tithe: TitheViewModel,
    pension: PensionViewModel,
    car: CarViewModel,
    close: MonthCloseViewModel,
): Boolean = shareBlocked(
    monthDirty = month.editor.value?.isDirty() == true,
    goalEditor = goals.ui.value.editor != null,
    titheEditor = tithe.ui.value.sheets.let { it.donation != null || it.settings != null },
    pensionEditor = pension.ui.value.editor != null,
    carEditor = car.ui.value.sheet != null,
    closeBalances = close.ui.value.balanceDraft.let { it.entering || it.values.any { value -> value.value.isNotBlank() } },
)

fun shareBlocked(
    monthDirty: Boolean,
    goalEditor: Boolean,
    titheEditor: Boolean,
    pensionEditor: Boolean,
    carEditor: Boolean,
    closeBalances: Boolean,
): Boolean = monthDirty || goalEditor || titheEditor || pensionEditor || carEditor || closeBalances
