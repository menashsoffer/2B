package com.menash.myself.ui.money

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.feature.money.MonthCellState
import com.menash.myself.feature.money.MonthPickerCell
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonthPickerSheet(months: List<MonthPickerCell>, onDismiss: () -> Unit, onSelect: (YearMonth) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        MonthPickerGrid(months, onSelect)
    }
}

@Composable
internal fun MonthPickerGrid(months: List<MonthPickerCell>, onSelect: (YearMonth) -> Unit) {
    val groups = months.groupBy { it.month.year }.entries.toList()
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 16.dp)
        .semantics { testTagsAsResourceId = true }.testTag("money_month_picker")) {
        item { Text(stringResource(R.string.status_choose_month), style = MaterialTheme.typography.titleLarge) }
        items(groups, key = { it.key }) { (year, cells) ->
            Text(year.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            cells.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { cell ->
                        val border = if (cell.isCurrent) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)) else Modifier
                        Row(Modifier.weight(1f).padding(2.dp).then(border).heightIn(min = 48.dp)
                            .clickable { onSelect(cell.month) }.testTag("money_pick_${cell.month}"),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(hebrewMonthName(cell.month.month))
                            when (cell.state) {
                                MonthCellState.CLOSED -> Icon(Icons.Outlined.Check, stringResource(R.string.status_closed), Modifier.size(18.dp))
                                MonthCellState.IMPORTED -> {
                                    val description = stringResource(R.string.status_month_imported)
                                    Text(" •", color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { contentDescription = description })
                                }
                                MonthCellState.EMPTY -> Unit
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
