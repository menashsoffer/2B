package com.menash.myself.ui.inbox

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.designsystem.component.SpaceBadge
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.feature.inbox.InboxViewModel

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTriageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.factory(
            inboxRepository = appContainer.inboxRepository,
            taskRepository = appContainer.taskRepository,
            dreamRepository = appContainer.dreamRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val inboxItems by viewModel.inboxItems.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val viewerUid = remember { runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.ui_inbox_309), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(stringResource(R.string.ui_inboxitemsoremptysize_319, inboxItems.orEmpty().size), fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("inbox_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_str_60))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            when (val state = uiState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.clearError() })
                is UiState.Empty -> EmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.ui_inbox_308),
                    description = stringResource(R.string.ui_str_310)
                )
                is UiState.Content -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { item ->
                        InboxItemCard(
                            item = item,
                            viewerUid = viewerUid,
                            onTriageToTask = {
                                viewModel.triageAsTask(item, title = item.text, nextAction = "", isTop3 = false)
                                Toast.makeText(context, context.getString(R.string.ui_str_320), Toast.LENGTH_SHORT).show()
                            },
                            onTriageToDream = {
                                viewModel.triageAsDream(item, title = item.text, list = context.getString(R.string.ui_str_311))
                                Toast.makeText(context, context.getString(R.string.ui_str_312), Toast.LENGTH_SHORT).show()
                            },
                            onTriageToIdea = {
                                viewModel.triageAsDream(item, title = item.text, list = context.getString(R.string.ui_str_307))
                                Toast.makeText(context, context.getString(R.string.ui_str_313), Toast.LENGTH_SHORT).show()
                            },
                            onTriageToNote = {
                                viewModel.triageAsNote(item)
                                Toast.makeText(context, context.getString(R.string.ui_str_316), Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { viewModel.deleteInboxItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InboxItemCard(
    item: InboxItem,
    viewerUid: String = "",
    onTriageToTask: () -> Unit,
    onTriageToDream: () -> Unit,
    onTriageToIdea: () -> Unit,
    onTriageToNote: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpaceBadge(
                    forWhom = item.forWhom,
                    ownerUid = item.ownerUid.ifBlank { viewerUid },
                    viewerUid = viewerUid
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_str_260), tint = Color.LightGray)
                }
            }
            Text(
                text = item.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            HorizontalDivider(color = Color(0xFFF1F5F9))
            InboxTriageActionsRow(
                onTriageToTask = onTriageToTask,
                onTriageToDream = onTriageToDream,
                onTriageToIdea = onTriageToIdea,
                onTriageToNote = onTriageToNote
            )
        }
    }
}

@Composable
private fun InboxTriageActionsRow(
    onTriageToTask: () -> Unit,
    onTriageToDream: () -> Unit,
    onTriageToIdea: () -> Unit,
    onTriageToNote: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            onClick = onTriageToTask,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.ui_str_315), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onTriageToDream,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.ui_str_317), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onTriageToIdea,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.ui_str_318), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onTriageToNote,
            modifier = Modifier.weight(0.9f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.ui_str_314), fontSize = 11.sp)
        }
    }
}
