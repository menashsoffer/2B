package com.menash.myself.ui.money

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.menash.myself.feature.money.ImportUi
import com.menash.myself.feature.money.ImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.feature.money.EditKind
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.MoneyViewModel
import com.menash.myself.feature.money.TransactionsViewModel
import com.menash.myself.feature.money.draftPreview
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val Israel: ZoneId = ZoneId.of("Asia/Jerusalem")

@Composable
fun MoneyScreen(modifier: Modifier = Modifier) {
    val appContainer = LocalAppContainer.current
    val viewModel: MoneyViewModel = viewModel(
        factory = MoneyViewModel.factory(
            repository = appContainer.moneyRepository,
            usdRates = appContainer.usdRateFlow,
            today = { LocalDate.now(Israel) },
            errorReporter = appContainer.errorReporter,
            transactions = appContainer.transactionRepository,
        )
    )
    val importViewModel: ImportViewModel = viewModel(
        factory = ImportViewModel.factory(appContainer.transactionRepository, appContainer.moneyRepository, appContainer.errorReporter)
    )
    val transactionsViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.factory(appContainer.transactionRepository, appContainer.moneyRepository, appContainer.errorReporter)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transactionsUi by transactionsViewModel.ui.collectAsStateWithLifecycle()
    val importUi by importViewModel.ui.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val files = withContext(Dispatchers.IO) { uris.mapNotNull { readImportFile(resolver, it) } }
            importViewModel.load(files)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { importUris(it) }
    val shared by appContainer.importInbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(shared) {
        if (shared.isNotEmpty()) importUris(appContainer.importInbox.take().map(Uri::parse))
    }

    if (importUi != ImportUi.Idle) {
        BackHandler { importViewModel.close() }
        ImportContent(
            ui = importUi,
            actions = ImportActions(
                onPick = importViewModel::openPicker,
                onConfirm = importViewModel::confirm,
                onClose = importViewModel::close,
            ),
            modifier = modifier,
        )
        (importUi as? ImportUi.Review)?.let { review ->
            review.review.merchants.firstOrNull { it.key == review.picking }?.let { merchant ->
                ImportCategorySheet(
                    merchant = merchant,
                    categories = review.categories,
                    onDecide = { importViewModel.decide(merchant.key, it) },
                    onDismiss = importViewModel::closePicker,
                )
            }
        }
        return
    }
    if (transactionsUi.open) {
        TransactionsScreen(transactionsUi, transactionsViewModel, modifier)
        return
    }
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(MoneyTabMonth) }
    var expanded by remember { mutableStateOf(emptySet<MonthRowKind>()) }
    var picking by remember { mutableStateOf(false) }

    MoneyContent(
        state = state,
        selectedTab = selectedTab,
        expanded = expanded,
        actions = MoneyActions(
            onSelectTab = { selectedTab = it },
            onToggleRow = { kind -> expanded = if (kind in expanded) expanded - kind else expanded + kind },
            onAdd = { picking = true },
            onStart = { viewModel.startAdding(EditKind.INCOME) },
            onEdit = viewModel::startEditing,
            onAddKind = viewModel::startAdding,
            onTransactions = { filter -> transactionsViewModel.open(state.money?.month ?: YearMonth.now(Israel), filter) },
        ),
        modifier = modifier,
    )

    if (picking) {
        KindPickerSheet(
            onPick = { kind ->
                picking = false
                viewModel.startAdding(kind)
            },
            onDismiss = { picking = false },
            onImport = {
                picking = false
                picker.launch(importMimeTypes)
            },
        )
    }

    editor?.let { current ->
        MoneyEditorSheet(
            editor = current,
            choices = EditorChoices(
                cards = state.data.cards,
                categories = state.data.categories,
                preview = draftPreview(current.draft, state.money?.month ?: YearMonth.now(Israel)),
                month = state.money?.month ?: YearMonth.now(Israel),
            ),
            actions = EditorActions(
                onChange = viewModel::updateDraft,
                onSave = { viewModel.save() },
                onDelete = viewModel::requestDelete,
                onNewCard = viewModel::startNewCard,
            ),
            onScope = { scope -> viewModel.save(scope) },
            onDismissScope = viewModel::dismissScope,
            onConfirmDelete = { viewModel.editingEntry()?.let(viewModel::delete) },
            onCancelDelete = viewModel::cancelDelete,
            onDismiss = viewModel::dismissEditor,
        )
    }
}
