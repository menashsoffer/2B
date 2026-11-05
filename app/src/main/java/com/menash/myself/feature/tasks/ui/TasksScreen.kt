package com.menash.myself.feature.tasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
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
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.core.domain.model.VisibilityFilter
import com.menash.myself.feature.tasks.TasksViewModel
import kotlinx.coroutines.launch

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onBack: () -> Unit) {
    val appContainer = LocalAppContainer.current
    val viewModel: TasksViewModel = viewModel(
        factory = TasksViewModel.factory(
            taskRepository = appContainer.taskRepository,
            householdRepository = appContainer.householdRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val visibilityFilter by viewModel.visibilityFilter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val viewerUid = remember { runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "" }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var taskToSchedule by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    val msgDone = stringResource(R.string.ui_str_275)
    val msgUndone = stringResource(R.string.ui_str_281)
    val actionUndo = stringResource(R.string.ui_str_279)

    val onToggleTaskWithUndo: (Task) -> Unit = { task ->
        viewModel.toggleTaskDone(task)
        coroutineScope.launch {
            val res = snackbarHostState.showSnackbar(
                message = if (!task.isDone) msgDone else msgUndone,
                actionLabel = actionUndo,
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed) {
                viewModel.toggleTaskDone(task.copy(isDone = !task.isDone))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_str_230), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tasks_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_str_60))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("add_task_top_action")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ui_str_282))
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            TasksScreenBody(
                uiState = uiState,
                visibilityFilter = visibilityFilter,
                viewerUid = viewerUid,
                onFilterChange = { viewModel.setVisibilityFilter(it) },
                onClearError = { viewModel.clearError() },
                onShowCreate = { showCreateDialog = true },
                onToggleDone = onToggleTaskWithUndo,
                onSetTop3 = { task, top3 -> viewModel.setTaskTop3(task, top3) },
                onSetFiveBefore11 = { task, five -> viewModel.setTaskFiveBefore11(task, five) },
                onRequestDelete = { taskToDelete = it },
                onScheduleClick = { taskToSchedule = it }
            )
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, nextAction, isTop3, isFive, dueDate, forWhom, isPrivate ->
                viewModel.createTask(
                    title = title,
                    nextAction = nextAction,
                    isTop3 = isTop3,
                    isFiveBefore11 = isFive,
                    dueDate = dueDate,
                    forWhom = forWhom,
                    isPrivate = isPrivate
                )
                showCreateDialog = false
            }
        )
    }

    taskToSchedule?.let { task ->
        ScheduleTaskDialog(
            task = task,
            onDismiss = { taskToSchedule = null },
            onSetDate = { newDate ->
                viewModel.updateTaskDate(task, newDate)
                taskToSchedule = null
            },
            onDelete = {
                taskToDelete = task
                taskToSchedule = null
            }
        )
    }

    taskToDelete?.let { task ->
        ConfirmDeleteTaskDialog(
            task = task,
            onConfirm = {
                viewModel.deleteTask(task)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
private fun TasksScreenBody(
    uiState: UiState<List<Task>>,
    visibilityFilter: VisibilityFilter,
    viewerUid: String,
    onFilterChange: (VisibilityFilter) -> Unit,
    onClearError: () -> Unit,
    onShowCreate: () -> Unit,
    onToggleDone: (Task) -> Unit,
    onSetTop3: (Task, Boolean) -> Unit,
    onSetFiveBefore11: (Task, Boolean) -> Unit,
    onRequestDelete: (Task) -> Unit,
    onScheduleClick: (Task) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        TasksFilterBar(visibilityFilter = visibilityFilter, onFilterChange = onFilterChange)
        Spacer(modifier = Modifier.height(8.dp))
        when (uiState) {
            is UiState.Loading -> LoadingState()
            is UiState.Error -> ErrorState(message = uiState.message, onRetry = onClearError)
            is UiState.Empty -> EmptyState(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.ui_str_276),
                description = stringResource(R.string.ui_str_278),
                actionLabel = stringResource(R.string.ui_str_274),
                onActionClick = onShowCreate
            )
            is UiState.Content -> TasksContentList(
                tasks = uiState.data,
                viewerUid = viewerUid,
                onToggleDone = onToggleDone,
                onSetTop3 = onSetTop3,
                onSetFiveBefore11 = onSetFiveBefore11,
                onRequestDelete = onRequestDelete,
                onScheduleClick = onScheduleClick
            )
        }
    }
}

@Composable
private fun TasksContentList(
    tasks: List<Task>,
    viewerUid: String,
    onToggleDone: (Task) -> Unit,
    onSetTop3: (Task, Boolean) -> Unit,
    onSetFiveBefore11: (Task, Boolean) -> Unit,
    onRequestDelete: (Task) -> Unit,
    onScheduleClick: (Task) -> Unit
) {
    val top3Tasks = tasks.filter { it.isTop3 && !it.isDone }
    val fiveBefore11Tasks = tasks.filter { it.isFiveBefore11 && !it.isDone }
    val undatedTasks = tasks.filter { it.dueDate == null && it.rawDueDate.isNullOrBlank() && !it.isDone && !it.isTop3 && !it.isFiveBefore11 }
    val regularTasks = tasks.filter { !it.isDone && !it.isTop3 && !it.isFiveBefore11 }
    val completedTasks = tasks.filter { it.isDone }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Top3FocusCard(
                tasks = top3Tasks,
                viewerUid = viewerUid,
                onToggleDone = onToggleDone,
                onRemoveTop3 = { onSetTop3(it, false) },
                onToggleFiveBefore11 = { onSetFiveBefore11(it, !it.isFiveBefore11) },
                onRequestDelete = onRequestDelete
            )
        }
        item {
            FiveBefore11Card(
                tasks = fiveBefore11Tasks,
                viewerUid = viewerUid,
                onToggleDone = onToggleDone,
                onToggleTop3 = { onSetTop3(it, !it.isTop3) },
                onRemoveFiveBefore11 = { onSetFiveBefore11(it, false) },
                onRequestDelete = onRequestDelete
            )
        }
        if (undatedTasks.isNotEmpty()) {
            item { UndatedTasksNoticeCard(undatedCount = undatedTasks.size) }
        }
        item {
            Text(stringResource(R.string.ui_str_277), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
        items(regularTasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                viewerUid = viewerUid,
                onToggleDone = { onToggleDone(task) },
                onToggleTop3 = { onSetTop3(task, true) },
                onToggleFiveBefore11 = { onSetFiveBefore11(task, true) },
                onDelete = { onRequestDelete(task) },
                onScheduleClick = { onScheduleClick(task) }
            )
        }
        if (completedTasks.isNotEmpty()) {
            item {
                Text(stringResource(R.string.ui_completedtaskssize_280, completedTasks.size), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
            }
            items(completedTasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    viewerUid = viewerUid,
                    onToggleDone = { onToggleDone(task) },
                    onToggleTop3 = {},
                    onToggleFiveBefore11 = {},
                    onDelete = { onRequestDelete(task) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
