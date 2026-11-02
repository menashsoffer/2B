package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
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
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.appColors.card,
    ) {
        ImportCategoryList(merchant, categories, onDecide, header = header, onCreate = onCreate)
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
) {
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
        merchant.hint?.let {
            Text(text = stringResource(R.string.import_hint, it), style = AppType.caption, color = colors.textSecondary)
        }
        header()
        val suggested = categories.firstOrNull { it.id == merchant.suggested }
        if (suggested != null) {
            MoneySurface(testTag = "import_suggested") {
                AmountRow(title = suggested.name, amount = null, onClick = { onDecide(MerchantDecision.Categorise(suggested.id)) })
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
            val inClass = categories.filter { it.klass == klass && it.id != suggested?.id }
            if (inClass.isNotEmpty()) {
                SectionTitle(rowLabel(row))
                MoneySurface {
                    inClass.forEach { category ->
                        AmountRow(
                            title = category.name,
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
