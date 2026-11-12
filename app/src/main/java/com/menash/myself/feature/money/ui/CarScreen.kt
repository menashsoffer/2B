package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.component.ChipVariant
import com.menash.myself.core.designsystem.component.PillTabs
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.CarCompareDraft
import com.menash.myself.feature.money.CarOption
import com.menash.myself.feature.money.CarSheet
import com.menash.myself.feature.money.CarUi
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.Included

/** "רכב" with the categories sheet and the new-car offers sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarScreen(ui: CarUi, viewModel: CarViewModel, modifier: Modifier = Modifier) {
    CarContent(
        ui = ui,
        actions = CarActions(onCategories = viewModel::editCategories, onCompare = viewModel::editCompare, onClose = viewModel::close),
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CarOverlays(ui: CarUi, viewModel: CarViewModel) {
    val sheet = ui.sheet ?: return
    ModalBottomSheet(
        onDismissRequest = viewModel::dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        when (sheet) {
            is CarSheet.Categories -> CarCategoriesForm(ui.categories, sheet.selected, viewModel::toggleCategory, viewModel::saveCategories)
            is CarSheet.Compare -> CarCompareForm(sheet.draft, viewModel::updateCompare, viewModel::saveCompare)
        }
    }
}

@Composable
private fun SheetColumn(tag: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarCategoriesForm(categories: List<Category>, selected: Set<String>, onToggle: (String) -> Unit, onSave: () -> Unit) {
    SheetColumn("car_categories_form") {
        Text(stringResource(R.string.car_categories_title), style = AppType.title, color = MaterialTheme.appColors.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            categories.forEach { category ->
                Chip(
                    text = category.name,
                    variant = if (category.id in selected) ChipVariant.Success else ChipVariant.Default,
                    onClick = { onToggle(category.id) },
                    modifier = Modifier.testTag("car_category_${category.id}"),
                )
            }
        }
        PrimaryButton(text = stringResource(R.string.car_categories_save), onClick = onSave, modifier = Modifier.fillMaxWidth().testTag("car_categories_save"))
    }
}

private val horizons = listOf(3, 5, 7)

/** Every offer on one sheet; an offer left empty is simply not compared. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarCompareForm(draft: CarCompareDraft, onChange: ((CarCompareDraft) -> CarCompareDraft) -> Unit, onSave: () -> Unit) {
    val colors = MaterialTheme.appColors
    @Composable
    fun Field(value: String, label: Int, tag: String, change: (CarCompareDraft, String) -> CarCompareDraft) {
        TextInput(
            value = value,
            onValueChange = { text -> onChange { change(it, text) } },
            label = stringResource(label),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.testTag("car_field_$tag"),
        )
    }
    SheetColumn("car_compare_form") {
        Text(stringResource(R.string.car_form_title), style = AppType.title, color = colors.textPrimary)
        PillTabs(
            options = horizons.map { stringResource(R.string.car_years, it) },
            selectedIndex = horizons.indexOf(draft.horizonYears).coerceAtLeast(0),
            onSelect = { index -> onChange { it.copy(horizonYears = horizons[index]) } },
        )
        Text(stringResource(R.string.car_horizon_hint), style = AppType.caption, color = colors.textSecondary)
        SectionTitle(stringResource(R.string.car_form_common))
        Field(draft.price, R.string.car_field_price, "price") { d, t -> d.copy(price = t) }
        Field(draft.tradeIn, R.string.car_field_trade_in, "trade_in") { d, t -> d.copy(tradeIn = t) }
        Field(draft.resale, R.string.car_field_resale, "resale") { d, t -> d.copy(resale = t) }
        Field(draft.lostReturn, R.string.car_field_lost_return, "lost_return") { d, t -> d.copy(lostReturn = t) }
        Field(draft.kmPerYear, R.string.car_field_km, "km") { d, t -> d.copy(kmPerYear = t) }
        SectionTitle(carOptionLabel(CarOption.BANK_LOAN))
        Field(draft.loanDown, R.string.car_field_down, "loan_down") { d, t -> d.copy(loanDown = t) }
        Field(draft.loanRate, R.string.car_field_rate, "loan_rate") { d, t -> d.copy(loanRate = t) }
        Field(draft.loanMonths, R.string.car_field_months, "loan_months") { d, t -> d.copy(loanMonths = t) }
        SectionTitle(carOptionLabel(CarOption.DEALER))
        Field(draft.dealerList, R.string.car_field_list, "dealer_list") { d, t -> d.copy(dealerList = t) }
        Field(draft.dealerRate, R.string.car_field_rate, "dealer_rate") { d, t -> d.copy(dealerRate = t) }
        Field(draft.dealerMonths, R.string.car_field_months, "dealer_months") { d, t -> d.copy(dealerMonths = t) }
        Field(draft.dealerBalloon, R.string.car_field_balloon, "dealer_balloon") { d, t -> d.copy(dealerBalloon = t) }
        SectionTitle(carOptionLabel(CarOption.LEASING))
        Field(draft.leaseMonthly, R.string.car_field_lease_monthly, "lease_monthly") { d, t -> d.copy(leaseMonthly = t) }
        Field(draft.leaseUpfront, R.string.car_field_lease_upfront, "lease_upfront") { d, t -> d.copy(leaseUpfront = t) }
        Field(draft.leaseMonths, R.string.car_field_months, "lease_months") { d, t -> d.copy(leaseMonths = t) }
        Field(draft.leaseKmLimit, R.string.car_field_lease_km, "lease_km") { d, t -> d.copy(leaseKmLimit = t) }
        Field(draft.leaseExtraKm, R.string.car_field_lease_extra, "lease_extra") { d, t -> d.copy(leaseExtraKm = t) }
        Text(stringResource(R.string.car_lease_included), style = AppType.caption, color = colors.textSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            listOf(Included.INSURANCE to R.string.car_inc_insurance, Included.MAINTENANCE to R.string.car_inc_maintenance).forEach { (item, label) ->
                val on = item in draft.leaseIncluded
                Chip(
                    text = stringResource(label),
                    variant = if (on) ChipVariant.Success else ChipVariant.Default,
                    onClick = { onChange { it.copy(leaseIncluded = if (on) it.leaseIncluded - item else it.leaseIncluded + item) } },
                )
            }
        }
        PrimaryButton(text = stringResource(R.string.car_compare_save), onClick = onSave, modifier = Modifier.fillMaxWidth().testTag("car_compare_save"))
    }
}
