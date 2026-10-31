package com.menash.myself.ui.money

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import com.menash.myself.core.designsystem.theme.Motion
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
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.EditKind
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.SubscriptionsViewModel
import com.menash.myself.feature.money.MonthRowKind
import com.menash.myself.feature.money.data.MoneyEntry
import com.menash.myself.feature.money.MoneyViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.TransactionsViewModel
import com.menash.myself.feature.money.draftPreview
import com.menash.myself.feature.money.importer.LineFilter
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
            events = appContainer.uiEvents,
        )
    )
    val importViewModel: ImportViewModel = viewModel(
        factory = ImportViewModel.factory(appContainer.transactionRepository, appContainer.moneyRepository, appContainer.errorReporter, appContainer.uiEvents)
    )
    val transactionsViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.factory(appContainer.transactionRepository, appContainer.moneyRepository, appContainer.errorReporter, appContainer.uiEvents)
    )
    val goalsViewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModel.factory(
            appContainer.moneyRepository, appContainer.usdRateFlow, { LocalDate.now(Israel) }, appContainer.errorReporter,
        )
    )
    val goalsUi by goalsViewModel.ui.collectAsStateWithLifecycle()
    val titheViewModel: TitheViewModel = viewModel(
        factory = TitheViewModel.factory(
            appContainer.moneyRepository, appContainer.transactionRepository, { LocalDate.now(Israel) }, appContainer.errorReporter,
            events = appContainer.uiEvents,
        )
    )
    val titheUi by titheViewModel.ui.collectAsStateWithLifecycle()
    val closeViewModel: MonthCloseViewModel = viewModel(
        factory = MonthCloseViewModel.factory(
            appContainer.moneyRepository, appContainer.transactionRepository, appContainer.usdRateFlow,
            { LocalDate.now(Israel) }, appContainer.errorReporter,
            events = appContainer.uiEvents,
        )
    )
    val closeUi by closeViewModel.ui.collectAsStateWithLifecycle()
    val carViewModel: CarViewModel = viewModel(
        factory = CarViewModel.factory(
            appContainer.moneyRepository, appContainer.transactionRepository, { LocalDate.now(Israel) }, appContainer.errorReporter,
        )
    )
    val carUi by carViewModel.ui.collectAsStateWithLifecycle()
    val pensionViewModel: PensionViewModel = viewModel(
        factory = PensionViewModel.factory(appContainer.moneyRepository, { LocalDate.now(Israel) }, appContainer.errorReporter, appContainer.uiEvents)
    )
    val pensionUi by pensionViewModel.ui.collectAsStateWithLifecycle()
    val subsViewModel: SubscriptionsViewModel = viewModel(
        factory = SubscriptionsViewModel.factory(
            appContainer.moneyRepository, appContainer.transactionRepository, appContainer.usdRateFlow,
            { LocalDate.now(Israel) }, appContainer.errorReporter,
        )
    )
    val subsUi by subsViewModel.ui.collectAsStateWithLifecycle()
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
    val pendingUnassigned by appContainer.uiEvents.unassigned.collectAsStateWithLifecycle()
    LaunchedEffect(pendingUnassigned) {
        pendingUnassigned?.let { month ->
            importViewModel.close()
            transactionsViewModel.open(month, LineFilter.UNASSIGNED)
            appContainer.uiEvents.clearUnassigned()
        }
    }
    val shared by appContainer.importInbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(shared) {
        if (shared.isNotEmpty()) importUris(appContainer.importInbox.take().map(Uri::parse))
    }

    val editor by viewModel.editor.collectAsStateWithLifecycle()
    @Composable
    fun EditorSheet() {
        val current = editor ?: return
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
    var selectedTab by rememberSaveable { mutableIntStateOf(MoneyTabMonth) }
    var expanded by remember { mutableStateOf(emptySet<MonthRowKind>()) }
    var picking by remember { mutableStateOf(false) }

    val destination = moneyDestination(MoneyOpenFlags(
        importing = importUi != ImportUi.Idle,
        transactions = transactionsUi.open,
        goals = goalsUi.open,
        tithe = titheUi.open,
        car = carUi.open,
        pension = pensionUi.open,
        close = closeUi.open,
        subscriptions = subsUi.open,
    ))
    val duration = Motion.duration(Motion.medium)
    val enter = Motion.slideIn(duration)
    val exit = Motion.slideOut(duration)
    AnimatedContent(targetState = destination, transitionSpec = {
        if (targetState.depth > initialState.depth) enter togetherWith Motion.fadeOut(duration)
        else Motion.fadeIn(duration) togetherWith exit
    }, label = "money_destination") { displayed ->
        CompositionLocalProvider(LocalMoneyBackActive provides (displayed == destination)) {
    if (displayed == MoneyDestination.Import) {
        ImportContent(
            ui = rememberShownState(importUi, importUi != ImportUi.Idle),
            actions = ImportActions(
                onPick = importViewModel::openPicker,
                onConfirm = importViewModel::confirm,
                onClose = importViewModel::close,
            ),
            modifier = modifier,
        )
        return@CompositionLocalProvider
    }
    // The transactions screen comes first: the maaser screen opens it to link a month's income.
    if (displayed == MoneyDestination.Transactions) {
        TransactionsScreen(rememberShownState(transactionsUi, transactionsUi.open), transactionsViewModel, modifier)
        return@CompositionLocalProvider
    }
    if (displayed == MoneyDestination.Goals) {
        GoalsScreen(rememberShownState(goalsUi, goalsUi.open), goalsViewModel, modifier)
        return@CompositionLocalProvider
    }
    if (displayed == MoneyDestination.Tithe) {
        TitheScreen(rememberShownState(titheUi, titheUi.open), titheViewModel, onLink = { transactionsViewModel.open(it) }, modifier = modifier)
        return@CompositionLocalProvider
    }
    if (displayed == MoneyDestination.Car) {
        CarScreen(rememberShownState(carUi, carUi.open), carViewModel, modifier)
        return@CompositionLocalProvider
    }
    if (displayed == MoneyDestination.Pension) {
        PensionScreen(rememberShownState(pensionUi, pensionUi.open), pensionViewModel, LocalDate.now(Israel), modifier)
        return@CompositionLocalProvider
    }
    // The close screen sits under transactions and import, which it opens; a stale balance opens the item sheet here.
    if (displayed == MoneyDestination.Close) {
        MonthCloseScreen(
            ui = rememberShownState(closeUi, closeUi.open),
            viewModel = closeViewModel,
            actions = MonthCloseActions(
                onImport = { picker.launch(importMimeTypes) },
                onLines = { month, filter -> transactionsViewModel.open(month, filter) },
                onAsset = { viewModel.startEditing(MoneyEntry.AssetEntry(it)) },
            ),
            modifier = modifier,
        )
        return@CompositionLocalProvider
    }
    if (displayed == MoneyDestination.Subscriptions) {
        SubscriptionsContent(
            ui = rememberShownState(subsUi, subsUi.open),
            actions = SubscriptionsActions(
                onEdit = { viewModel.startEditing(MoneyEntry.SubscriptionEntry(it)) },
                onAdd = { viewModel.startAdding(EditKind.SUBSCRIPTION) },
                onAddFrom = viewModel::startSubscriptionFrom,
                onBack = subsViewModel::close,
            ),
            modifier = modifier,
        )
        return@CompositionLocalProvider
    }
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
            onGoals = goalsViewModel::open,
            onTithe = titheViewModel::open,
            onCloseMonth = closeViewModel::open,
            onCar = carViewModel::open,
            onPension = pensionViewModel::open,
            onSubscriptions = subsViewModel::open,
            onImport = { picker.launch(importMimeTypes) },
        ),
        modifier = modifier,
    )

        }
    }
    when (destination) {
        MoneyDestination.Transactions -> TransactionsOverlays(transactionsUi, transactionsViewModel)
        MoneyDestination.Goals -> GoalsOverlays(goalsUi, goalsViewModel)
        MoneyDestination.Tithe -> TitheOverlays(titheUi, titheViewModel)
        MoneyDestination.Car -> CarOverlays(carUi, carViewModel)
        MoneyDestination.Pension -> PensionOverlays(pensionUi, pensionViewModel, LocalDate.now(Israel))
        MoneyDestination.Close -> MonthCloseOverlays(closeUi, closeViewModel)
        else -> Unit
    }
    if (destination == MoneyDestination.Import) {
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
    }
    if (picking && destination == MoneyDestination.Root) {
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

    if (destination in setOf(MoneyDestination.Root, MoneyDestination.Close, MoneyDestination.Subscriptions)) EditorSheet()
}
