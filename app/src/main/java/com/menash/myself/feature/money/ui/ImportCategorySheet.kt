package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.ImportViewModel
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.importer.MerchantChoice
import com.menash.myself.feature.money.importer.MerchantDecision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCategorySheet(
    merchant: MerchantChoice,
    categories: List<Category>,
    onDecide: (MerchantDecision) -> Unit,
    onDismiss: () -> Unit,
    header: @Composable () -> Unit = {},
    onCreate: ((String, ExpenseClass) -> Unit)? = null,
    recentCategories: List<String> = emptyList(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        ImportCategoryList(merchant, categories, onDecide, header = header, onCreate = onCreate, recentCategories = recentCategories)
    }
}

private val classRows = listOf(
    ExpenseClass.CURRENT to MonthRowKind.CURRENT,
    ExpenseClass.FIXED to MonthRowKind.FIXED,
    ExpenseClass.PERIODIC to MonthRowKind.PERIODIC,
)

/** The suggestion first, then every category by Paamonim class; a transfer can be marked as our own. */
@Composable
fun ImportCategoryList(
    merchant: MerchantChoice,
    categories: List<Category>,
    onDecide: (MerchantDecision) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    onCreate: ((String, ExpenseClass) -> Unit)? = null,
    recentCategories: List<String> = emptyList(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    fun displayName(category: Category): String = categories.firstOrNull { it.id == category.parentId }?.let { "${it.name} › ${category.name}" } ?: category.name
    val matching = categories.filter { displayName(it).contains(query.trim(), ignoreCase = true) }
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.s16)
            .padding(bottom = Spacing.s24)
            .testTag("import_category_list"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Text(
            text = stringResource(R.string.import_pick_title, merchant.name),
            style = AppType.title,
            color = colors.textPrimary,
        )
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(stringResource(R.string.import_search_categories)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), singleLine = true, modifier = Modifier.fillMaxWidth())
        merchant.hint?.let {
            Text(text = stringResource(R.string.import_hint, it), style = AppType.caption, color = colors.textSecondary)
        }
        header()
        val first = (recentCategories + listOfNotNull(merchant.suggested)).distinct().mapNotNull { id -> matching.firstOrNull { it.id == id } }
        if (first.isNotEmpty()) {
            MoneySurface(testTag = "import_suggested") {
                first.forEach { category ->
                    AmountRow(title = displayName(category), amount = null, onClick = { onDecide(MerchantDecision.Categorise(category.id)) })
                }
            }
        }
        if (merchant.transfer) {
            MoneySurface {
                AmountRow(
                    title = stringResource(R.string.import_own_transfer),
                    amount = null,
                    onClick = { onDecide(MerchantDecision.OwnTransfer) },
                    testTag = "import_own_transfer",
                )
            }
        }
        classRows.forEach { (klass, row) ->
            val inClass = matching.filter { it.klass == klass && first.none { item -> item.id == it.id } }
            if (inClass.isNotEmpty()) {
                SectionTitle(rowLabel(row))
                MoneySurface {
                    inClass.forEach { category ->
                        AmountRow(
                            title = displayName(category),
                            amount = null,
                            onClick = { onDecide(MerchantDecision.Categorise(category.id)) },
                            testTag = "import_category_${category.id}",
                        )
                    }
                }
            }
        }
        onCreate?.let { NewCategoryForm(it) }
        MoneySurface {
            AmountRow(
                title = stringResource(R.string.import_later),
                amount = null,
                onClick = { onDecide(MerchantDecision.Later) },
                testTag = "import_later",
            )
        }
    }
}

/** The category sheet of the merchant being picked in the import review. */
@Composable
fun ImportOverlays(importUi: ImportUi, importViewModel: ImportViewModel) {
    val review = importUi as? ImportUi.Review ?: return
    val merchant = review.review.merchants.firstOrNull { it.key == review.picking } ?: return
    ImportCategorySheet(
        merchant = merchant,
        categories = review.categories,
        recentCategories = review.recentCategories,
        onDecide = { importViewModel.decide(merchant.key, it) },
        onDismiss = importViewModel::closePicker,
        onCreate = { name, klass -> importViewModel.createCategory(merchant.key, name, klass) },
    )
}
