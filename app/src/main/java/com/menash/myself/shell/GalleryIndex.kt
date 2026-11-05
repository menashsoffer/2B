package com.menash.myself.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.menash.myself.R
import com.menash.myself.core.designsystem.component.ListRow
import com.menash.myself.core.designsystem.component.ScreenHeader
import com.menash.myself.core.designsystem.gallery.DesignSystemGallery
import com.menash.myself.core.designsystem.theme.Spacing
import com.menash.myself.core.designsystem.theme.appColors

private enum class GalleryPage {
    Index,
    Design,
    Legacy,
}

private data class LegacyDestination(
    val route: String,
    val labelRes: Int,
    val testTag: String,
)

private val legacyDestinations = listOf(
    LegacyDestination(ShellRoute.LegacyHome, R.string.legacy_home, "legacy_home"),
    LegacyDestination(ShellRoute.Tasks, R.string.legacy_tasks, "tasks"),
    LegacyDestination(ShellRoute.DreamsLegacy, R.string.legacy_dreams, "dreams_legacy"),
    LegacyDestination(ShellRoute.More, R.string.legacy_more, "more"),
    LegacyDestination(ShellRoute.Inbox, R.string.legacy_inbox, "inbox"),
    LegacyDestination(ShellRoute.Fitness, R.string.legacy_fitness, "fitness"),
    LegacyDestination(ShellRoute.Calendar, R.string.legacy_calendar, "calendar"),
    LegacyDestination(ShellRoute.Catalog, R.string.legacy_catalog, "catalog"),
)

@Composable
fun GalleryIndex(
    onClose: () -> Unit,
    onOpenLegacy: (String) -> Unit,
) {
    var pageName by rememberSaveable { mutableStateOf(GalleryPage.Index.name) }
    val page = GalleryPage.valueOf(pageName)
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        GalleryTopBar(
            title = when (page) {
                GalleryPage.Index -> stringResource(R.string.gallery_title)
                GalleryPage.Design -> stringResource(R.string.gallery_design_system)
                GalleryPage.Legacy -> stringResource(R.string.gallery_legacy_screens)
            },
            showBack = page != GalleryPage.Index,
            onBack = { pageName = GalleryPage.Index.name },
            onClose = onClose,
        )
        when (page) {
            GalleryPage.Index -> GalleryMenu(
                onOpenDesign = { pageName = GalleryPage.Design.name },
                onOpenLegacyList = { pageName = GalleryPage.Legacy.name },
                modifier = Modifier.weight(1f),
            )
            GalleryPage.Design -> DesignSystemGallery(
                showHeader = false,
                modifier = Modifier.weight(1f),
            )
            GalleryPage.Legacy -> LegacyList(
                onOpen = onOpenLegacy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GalleryTopBar(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = Spacing.s8, vertical = Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.ui_str_60),
                )
            }
        }
        ScreenHeader(title = title, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.gallery_close),
            )
        }
    }
}

@Composable
private fun GalleryMenu(
    onOpenDesign: () -> Unit,
    onOpenLegacyList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        ListRow(
            title = stringResource(R.string.gallery_design_system),
            onClick = onOpenDesign,
            testTag = "gallery_design_system",
            trailingContent = { ForwardIcon() },
        )
        ListRow(
            title = stringResource(R.string.gallery_legacy_screens),
            onClick = onOpenLegacyList,
            testTag = "gallery_legacy_screens",
            trailingContent = { ForwardIcon() },
        )
    }
}

@Composable
private fun LegacyList(
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        legacyDestinations.forEach { destination ->
            ListRow(
                title = stringResource(destination.labelRes),
                onClick = { onOpen(destination.route) },
                testTag = destination.testTag,
                trailingContent = { ForwardIcon() },
            )
        }
    }
}

@Composable
private fun ForwardIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.appColors.textSecondary,
    )
}
