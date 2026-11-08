package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.domain.ExpenseClass
import com.menash.myself.feature.money.domain.MAX_CATEGORY_NAME

private val newCategoryClasses = listOf(
    ExpenseClass.CURRENT to MonthRowKind.CURRENT,
    ExpenseClass.FIXED to MonthRowKind.FIXED,
    ExpenseClass.PERIODIC to MonthRowKind.PERIODIC,
)

/** "קטגוריה חדשה": a row that opens a name field and the Paamonim class, then adds and picks it. */
@Composable
fun NewCategoryForm(onCreate: (String, ExpenseClass) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var klass by remember { mutableStateOf(ExpenseClass.CURRENT) }
    MoneySurface(testTag = "new_category") {
        if (!open) {
            AmountRow(
                title = stringResource(R.string.import_new_category),
                amount = null,
                action = RowAction.Expand(expanded = false) { open = true },
                testTag = "new_category_open",
            )
        } else Column(modifier = Modifier.fillMaxWidth().padding(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            TextInput(
                value = name,
                onValueChange = { name = it.take(MAX_CATEGORY_NAME) },
                label = stringResource(R.string.import_new_category_name),
                modifier = Modifier.testTag("new_category_name"),
            )
            newCategoryClasses.forEach { (value, row) ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .selectable(selected = klass == value, role = Role.RadioButton, onClick = { klass = value }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = klass == value, onClick = null)
                    Text(rowLabel(row), style = AppType.body, modifier = Modifier.padding(Spacing.s8))
                }
            }
            TextButton(
                onClick = { onCreate(name, klass) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("new_category_add"),
            ) {
                Text(stringResource(R.string.import_new_category_add), style = AppType.body)
            }
        }
    }
}
