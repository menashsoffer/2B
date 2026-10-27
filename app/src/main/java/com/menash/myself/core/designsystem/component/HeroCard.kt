package com.menash.myself.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.theme.AppShapes
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun HeroCard(
    label: String,
    message: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    val messageColor = if (value != null) colors.onAccent.copy(alpha = 0.82f) else colors.onAccent
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClickLabel = stringResource(R.string.hero_open),
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(AppShapes.card)
            .background(colors.accent)
            .then(clickModifier)
            .semantics(mergeDescendants = true) {}
            .padding(Spacing.s16),
    ) {
        Text(
            text = label,
            style = AppType.caption,
            color = colors.onAccent.copy(alpha = 0.82f),
        )
        if (value != null) {
            Spacer(modifier = Modifier.height(Spacing.s4))
            Text(text = value, style = AppType.number, color = colors.onAccent)
        }
        Spacer(modifier = Modifier.height(Spacing.s4))
        Text(text = message, style = AppType.body, color = messageColor)
    }
}

@Preview(name = "Hero light", showBackground = true, widthDp = 360, locale = "iw")
@Composable
private fun HeroCardLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        HeroCard(
            label = stringResource(R.string.nav_money),
            message = stringResource(R.string.home_money_message),
            onClick = {},
        )
    }
}

@Preview(
    name = "Hero dark",
    showBackground = true,
    widthDp = 360,
    locale = "iw",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HeroCardDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        HeroCard(
            label = stringResource(R.string.nav_money),
            message = stringResource(R.string.home_money_message),
            onClick = {},
        )
    }
}
