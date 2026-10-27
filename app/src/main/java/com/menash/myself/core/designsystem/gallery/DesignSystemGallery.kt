package com.menash.myself.core.designsystem.gallery

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.ListRow
import com.menash.myself.core.designsystem.component.MetricCard
import com.menash.myself.core.designsystem.component.PrimaryButton
import com.menash.myself.core.designsystem.component.ProgressBar
import com.menash.myself.core.designsystem.component.QuickAddButton
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.component.SharedBadge
import com.menash.myself.core.designsystem.component.TextInput
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.MyApplicationTheme
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

@Composable
fun DesignSystemGallery(
    onClose: (() -> Unit)? = null,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    var draft by rememberSaveable { mutableStateOf("חלב ולחם") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s16)
            .testTag("design_system_gallery"),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        if (onClose != null) {
            Text(
                text = "סגירה",
                style = AppType.body,
                color = colors.accent,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .testTag("design_system_gallery_close"),
            )
        }
        if (showHeader) {
            ScreenHeader(title = "מערכת העיצוב", subtitle = "לילה כחול")
        }
        DesignSystemGallerySamples()
        MetricCard(
            label = "יתרה משותפת",
            value = "24,680",
            detail = "שקלים בחשבון העובר ושב",
            progress = 0.45f,
        )
        ListRow(
            title = "קניות לשישי",
            subtitle = "חלב, לחם, ירקות",
            trailingContent = { SharedBadge(isShared = true) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "הערה אישית", style = AppType.body, color = colors.textPrimary)
            Spacer(modifier = Modifier.weight(1f))
            SharedBadge(isShared = false)
        }
        PrimaryButton(text = "שמירה", onClick = {})
        QuickAddButton(text = "הוספה מהירה", onClick = {})
        TextInput(value = draft, onValueChange = { draft = it }, label = "מה להוסיף?")
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Text(text = "חיסכון לחודש", style = AppType.caption, color = colors.textSecondary)
            ProgressBar(progress = 0.62f)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s16),
        ) {
            Text(text = "שולם", style = AppType.caption, color = colors.success)
            Text(text = "ממתין", style = AppType.caption, color = colors.warning)
            Text(text = "באיחור", style = AppType.caption, color = colors.danger)
        }
        EmptyState(
            title = "אין משימות להיום",
            description = "אפשר להוסיף אחת קצרה לשישי",
            actionLabel = "הוספת משימה",
            onActionClick = {},
        )
        Spacer(modifier = Modifier.height(Spacing.s32))
    }
}

@Preview(name = "Gallery light", showBackground = true, widthDp = 360, heightDp = 1600, locale = "iw")
@Composable
private fun GalleryLightPreview() {
    MyApplicationTheme(darkTheme = false) {
        DesignSystemGallery()
    }
}

@Preview(
    name = "Gallery dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 1600,
    locale = "iw",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GalleryDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        DesignSystemGallery()
    }
}
