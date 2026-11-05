package com.menash.myself.feature.money.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.feature.money.data.ImportDocuments

/**
 * Above the category list of one line: the merchant's alias, "רק לתנועה הזו", and "תמיד לשאול".
 * [bankName] is the text from the file, which matching keeps using whatever the alias is.
 */
@Composable
fun MerchantOptions(
    bankName: String,
    alias: String?,
    asksEachTime: Boolean,
    thisLineOnly: Boolean,
    onAlias: (String) -> Unit,
    onThisLineOnly: (Boolean) -> Unit,
    onAskEachTime: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    var text by remember(bankName, alias) { mutableStateOf(alias ?: bankName) }
    val shown = alias ?: bankName
    Column(modifier = modifier.fillMaxWidth().testTag("merchant_options"), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        TextInput(
            value = text,
            onValueChange = { text = it.take(ImportDocuments.MAX_ALIAS) },
            label = stringResource(R.string.tx_alias_label),
            modifier = Modifier.testTag("merchant_alias"),
        )
        if (alias != null) {
            Text(stringResource(R.string.tx_alias_bank_name, bankName), style = AppType.caption, color = colors.textSecondary)
        }
        if (text.trim() != shown) {
            TextButton(onClick = { onAlias(text) }, modifier = Modifier.testTag("merchant_alias_save")) {
                Text(stringResource(R.string.tx_alias_save), style = AppType.body)
            }
        }
        if (!asksEachTime) {
            SwitchRow(stringResource(R.string.tx_this_line_only), thisLineOnly, onThisLineOnly)
            if (thisLineOnly) {
                Text(stringResource(R.string.tx_this_line_only_detail, shown), style = AppType.caption, color = colors.textSecondary)
            }
        }
        SwitchRow(stringResource(R.string.tx_ask_each_time, shown), asksEachTime, onAskEachTime)
        if (asksEachTime) {
            Text(stringResource(R.string.tx_ask_each_time_detail), style = AppType.caption, color = colors.textSecondary)
        }
    }
}
