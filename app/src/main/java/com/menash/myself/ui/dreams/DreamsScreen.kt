package com.menash.myself.ui.dreams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Money
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.feature.dreams.DreamsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamsScreen(onBack: () -> Unit) {
    val appContainer = LocalAppContainer.current
    val viewModel: DreamsViewModel = viewModel(
        factory = DreamsViewModel.factory(
            dreamRepository = appContainer.dreamRepository,
            householdRepository = appContainer.householdRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val selectedListFilter by viewModel.selectedListFilter.collectAsState()
    val visibilityFilter by viewModel.visibilityFilter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val viewerUid = remember { runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "" }

    var isBoardView by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedDreamForAppIdea by remember { mutableStateOf<Dream?>(null) }
    var dreamToDelete by remember { mutableStateOf<Dream?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("חלומות ורשימות", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dreams_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                actions = {
                    IconButton(onClick = { isBoardView = !isBoardView }) {
                        Icon(
                            if (isBoardView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewKanban,
                            contentDescription = "החלף תצוגה"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_dream_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף חלום")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            DreamsFilterSection(
                selectedListFilter = selectedListFilter,
                visibilityFilter = visibilityFilter,
                onListFilterChange = { viewModel.setListFilter(it) },
                onVisibilityFilterChange = { viewModel.setVisibilityFilter(it) }
            )
            DreamsContentArea(
                uiState = uiState,
                isBoardView = isBoardView,
                viewerUid = viewerUid,
                onClearError = { viewModel.clearError() },
                onShowCreate = { showCreateDialog = true },
                onStatusChange = { dream, newStatus -> viewModel.updateDreamStatus(dream, newStatus) },
                onAddSavings = { dream, amount ->
                    val nextSaved = Money.fromShekels(dream.savedAmount.shekels + amount)
                    viewModel.updateDreamSavings(dream, nextSaved)
                },
                onOpenAppIdea = { selectedDreamForAppIdea = it },
                onRequestDelete = { dreamToDelete = it }
            )
        }
    }

    if (showCreateDialog) {
        CreateDreamDialog(
            defaultList = selectedListFilter,
            onDismiss = { showCreateDialog = false },
            onSave = { newDream ->
                viewModel.createDream(newDream)
                showCreateDialog = false
            }
        )
    }

    selectedDreamForAppIdea?.let { dream ->
        AppIdeaTemplateDialog(
            dream = dream,
            onDismiss = { selectedDreamForAppIdea = null },
            onSave = { questions, nextAction ->
                viewModel.updateDreamAppIdea(dream, questions, nextAction)
                selectedDreamForAppIdea = null
            }
        )
    }

    dreamToDelete?.let { dream ->
        ConfirmDeleteDreamDialog(
            dream = dream,
            onConfirm = {
                viewModel.deleteDream(dream)
                dreamToDelete = null
            },
            onDismiss = { dreamToDelete = null }
        )
    }
}

@Composable
private fun DreamsFilterSection(
    selectedListFilter: String,
    visibilityFilter: VisibilityFilter,
    onListFilterChange: (String) -> Unit,
    onVisibilityFilterChange: (VisibilityFilter) -> Unit
) {
    val defaultLists = listOf("הכל", "לבית", "טיסות משפחה", "פיתוח ואפליקציות")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(defaultLists) { listName ->
                FilterChip(
                    selected = selectedListFilter == listName,
                    onClick = { onListFilterChange(listName) },
                    label = { Text(listName, fontSize = 12.sp) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = visibilityFilter == VisibilityFilter.MINE_AND_BOTH,
                onClick = { onVisibilityFilterChange(VisibilityFilter.MINE_AND_BOTH) },
                label = { Text("שלי + של שנינו", fontSize = 11.sp) },
                modifier = Modifier.testTag("dreams_filter_mine_and_both")
            )
            FilterChip(
                selected = visibilityFilter == VisibilityFilter.ALL,
                onClick = { onVisibilityFilterChange(VisibilityFilter.ALL) },
                label = { Text("הצג הכל", fontSize = 11.sp) },
                modifier = Modifier.testTag("dreams_filter_all")
            )
        }
    }
}

@Composable
private fun DreamsContentArea(
    uiState: UiState<List<Dream>>,
    isBoardView: Boolean,
    viewerUid: String,
    onClearError: () -> Unit,
    onShowCreate: () -> Unit,
    onStatusChange: (Dream, String) -> Unit,
    onAddSavings: (Dream, Double) -> Unit,
    onOpenAppIdea: (Dream) -> Unit,
    onRequestDelete: (Dream) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = uiState.message, onRetry = onClearError)
        is UiState.Empty -> EmptyState(
            icon = Icons.Default.Star,
            title = "אין חלומות ברשימה זו",
            description = "הוסף חלום חדש או עבור לרשימה אחרת",
            actionLabel = "חלום חדש",
            onActionClick = onShowCreate
        )
        is UiState.Content -> {
            if (isBoardView) {
                DreamBoardView(dreams = uiState.data)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.data, key = { it.id }) { dream ->
                        DreamCard(
                            dream = dream,
                            viewerUid = viewerUid,
                            onStatusChange = { newStatus -> onStatusChange(dream, newStatus) },
                            onAddSavings = { amount -> onAddSavings(dream, amount) },
                            onOpenAppIdea = { onOpenAppIdea(dream) },
                            onDelete = { onRequestDelete(dream) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}
