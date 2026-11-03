package com.menash.myself.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.appColors

/**
 * The one search box of the app. Any screen puts it above a list and filters with
 * [com.menash.myself.core.domain.search.SearchQuery]; the field itself keeps no state.
 * [autoFocus] opens the keyboard when a search screen appears.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    testTag: String = "search_field",
) {
    val colors = MaterialTheme.appColors
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().focusRequester(focus).testTag(testTag),
        placeholder = { Text(text = placeholder, style = AppType.body, color = colors.textSecondary) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.textSecondary) },
        trailingIcon = if (value.isEmpty()) null else {
            {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.testTag("${testTag}_clear")) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.search_clear), tint = colors.textSecondary)
                }
            }
        },
        textStyle = AppType.body,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = AppShapes.field,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.cardBorder,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accent,
            focusedContainerColor = colors.card,
            unfocusedContainerColor = colors.card,
        ),
    )
}
