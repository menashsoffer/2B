package com.menash.myself.ui.money

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.Chip
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.EditKind
import com.menash.myself.feature.money.ItemDraft
import com.menash.myself.feature.money.domain.Card
import com.menash.myself.feature.money.domain.Category
import com.menash.myself.feature.money.domain.ExpenseClass

@Composable
internal fun CardPicker(
    draft: ItemDraft,
    cards: List<Card>,
    change: ((ItemDraft) -> ItemDraft) -> Unit,
    onNewCard: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = cards.firstOrNull { it.id == draft.cardId }
    Box {
        Chip(
            text = current?.let { stringResource(R.string.money_card, it.name) } ?: stringResource(R.string.money_no_card),
            onClick = { open = true },
            modifier = Modifier.testTag("money_card_picker"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            cards.forEach { card ->
                DropdownMenuItem(
                    text = { Text(card.name, style = AppType.body) },
                    onClick = {
                        open = false
                        change { it.copy(cardId = card.id) }
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.money_no_card), style = AppType.body) },
                onClick = {
                    open = false
                    change { it.copy(cardId = null) }
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.money_new_card), style = AppType.body) },
                onClick = {
                    open = false
                    onNewCard()
                },
            )
        }
    }
}

@Composable
internal fun CategoryPicker(
    draft: ItemDraft,
    categories: List<Category>,
    kind: EditKind,
    change: ((ItemDraft) -> ItemDraft) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = categories.firstOrNull { it.id == draft.categoryId }
    // A periodic budget offers periodic categories only; the other items offer every class.
    val classes = if (kind == EditKind.PERIODIC) listOf(ExpenseClass.PERIODIC) else ExpenseClass.entries
    Box {
        Chip(
            text = current?.let { stringResource(R.string.money_category, it.name) } ?: stringResource(R.string.money_no_category),
            onClick = { open = true },
            modifier = Modifier.testTag("money_category_picker"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.money_no_category), style = AppType.body) },
                onClick = {
                    open = false
                    change { it.copy(categoryId = null) }
                },
            )
            classes.forEach { klass ->
                Text(
                    text = stringResource(classLabel(klass)),
                    style = AppType.caption,
                    color = MaterialTheme.appColors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
                )
                categories.filter { it.klass == klass }.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name, style = AppType.body) },
                        onClick = {
                            open = false
                            change { draft ->
                                draft.copy(
                                    categoryId = category.id,
                                    name = draft.name.ifBlank { category.name },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun classLabel(klass: ExpenseClass): Int = when (klass) {
    ExpenseClass.FIXED -> R.string.money_class_fixed
    ExpenseClass.CURRENT -> R.string.money_class_current
    ExpenseClass.PERIODIC -> R.string.money_class_periodic
}
