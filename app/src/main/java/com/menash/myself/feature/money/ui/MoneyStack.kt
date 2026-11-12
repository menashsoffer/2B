package com.menash.myself.feature.money.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import com.menash.myself.feature.money.CarViewModel
import com.menash.myself.feature.money.CategoryTrendUi
import com.menash.myself.feature.money.CategoryTrendViewModel
import com.menash.myself.feature.money.GoalsViewModel
import com.menash.myself.feature.money.ImportViewModel
import com.menash.myself.feature.money.MoneyFrame
import com.menash.myself.feature.money.MoneyPlace
import com.menash.myself.feature.money.MonthCloseUi
import com.menash.myself.feature.money.MonthCloseViewModel
import com.menash.myself.feature.money.MonthSummaryUi
import com.menash.myself.feature.money.MonthSummaryViewModel
import com.menash.myself.feature.money.PensionViewModel
import com.menash.myself.feature.money.PeriodViewModel
import com.menash.myself.feature.money.SubscriptionsViewModel
import com.menash.myself.feature.money.TitheViewModel
import com.menash.myself.feature.money.TransactionsUi
import com.menash.myself.feature.money.TransactionsViewModel
import com.menash.myself.feature.money.decodeMoneyHistory
import com.menash.myself.feature.money.encodeMoneyHistory
import com.menash.myself.feature.money.importer.LineFilter
import com.menash.myself.feature.money.framesToReplay
import com.menash.myself.feature.money.shareImportShouldAsk
import com.menash.myself.feature.money.syncMoneyHistory
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menash.myself.feature.money.ImportInbox
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Open money screens, in the order a brand-new pair should stack (parent, then child). */
class MoneyNavInputs(
    val importOpen: Boolean,
    val transactions: TransactionsUi,
    val goalsOpen: Boolean,
    val titheOpen: Boolean,
    val carOpen: Boolean,
    val pensionOpen: Boolean,
    val close: MonthCloseUi,
    val subscriptionsOpen: Boolean,
    val periodOpen: Boolean,
    val summary: MonthSummaryUi,
    val trend: CategoryTrendUi,
) {
    fun frames(): List<MoneyFrame> = buildList {
        if (goalsOpen) add(MoneyFrame(MoneyPlace.Goals))
        if (titheOpen) add(MoneyFrame(MoneyPlace.Tithe))
        if (carOpen) add(MoneyFrame(MoneyPlace.Car))
        if (pensionOpen) add(MoneyFrame(MoneyPlace.Pension))
        if (subscriptionsOpen) add(MoneyFrame(MoneyPlace.Subscriptions))
        if (periodOpen) add(MoneyFrame(MoneyPlace.Period))
        if (close.open) add(MoneyFrame(MoneyPlace.Close, month = close.view?.month?.toString()))
        if (summary.open) add(MoneyFrame(MoneyPlace.Summary, month = summary.month?.toString()))
        if (transactions.open) add(transactionFrame(transactions))
        if (trend.open) add(MoneyFrame(MoneyPlace.Trend, categoryId = trend.categoryId))
        if (importOpen) add(MoneyFrame(MoneyPlace.Import))
    }
}

class MoneyNavVms(
    val transactions: TransactionsViewModel,
    val close: MonthCloseViewModel,
    val summary: MonthSummaryViewModel,
    val trend: CategoryTrendViewModel,
    val goals: GoalsViewModel,
    val tithe: TitheViewModel,
    val car: CarViewModel,
    val pension: PensionViewModel,
    val subscriptions: SubscriptionsViewModel,
    val period: PeriodViewModel,
    val import: ImportViewModel,
) {
    fun closeAll() {
        import.close()
        transactions.close()
        transactions.close()
        trend.close()
        summary.close()
        goals.close()
        tithe.close()
        car.close()
        pension.close()
        close.close()
        subscriptions.close()
        period.close()
    }

    fun replay(frame: MoneyFrame) {
        val month = frame.month?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        when (frame.place) {
            MoneyPlace.Goals -> goals.open()
            MoneyPlace.Tithe -> tithe.open()
            MoneyPlace.Car -> car.open()
            MoneyPlace.Pension -> pension.open()
            MoneyPlace.Subscriptions -> subscriptions.open()
            MoneyPlace.Period -> period.open()
            MoneyPlace.Close -> close.open(month)
            MoneyPlace.Summary -> month?.let(summary::open)
            MoneyPlace.Transactions -> replayTransactions(transactions, month, frame.filter)
            MoneyPlace.Trend -> frame.categoryId?.let(trend::open)
            MoneyPlace.Import -> import.resume()
        }
    }
}

/** The visible money screen: walk order, restored after process death, cleared when כסף is reselected. */
@Composable
fun rememberMoneyDestination(
    inputs: MoneyNavInputs,
    vms: MoneyNavVms,
    shownMonth: StateFlow<YearMonth?>,
    onRestoreMonth: (YearMonth) -> Unit,
    onScrollTop: () -> Unit,
): MoneyDestination {
    val open = inputs.frames()
    var savedMonth by rememberSaveable { mutableStateOf<String?>(null) }
    val month by shownMonth.collectAsStateWithLifecycle()
    val restoredMonth = savedMonth
    LaunchedEffect(Unit) {
        restoredMonth?.let { raw -> runCatching { YearMonth.parse(raw) }.getOrNull()?.let(onRestoreMonth) }
    }
    LaunchedEffect(month) { savedMonth = month?.toString() }
    val shown = restoredMoneyStack(open) { missing -> missing.forEach(vms::replay) }
    val reselect = LocalMoneyReselect.current
    LaunchedEffect(reselect) {
        reselect.ticks.collect {
            vms.closeAll()
            onScrollTop()
        }
    }
    return shown.lastOrNull()?.place.toMoneyDestination()
}

/**
 * Saved walk order stays on screen until every restored frame is open again.
 * Marking ready in the same breath as replay would sync against an empty flag list and wipe the stack.
 */
@Composable
fun restoredMoneyStack(
    open: List<MoneyFrame>,
    replay: (List<MoneyFrame>) -> Unit,
): List<MoneyFrame> {
    var stored by rememberSaveable { mutableStateOf("") }
    val history = decodeMoneyHistory(stored)
    var replayStarted by remember { mutableStateOf(history.isEmpty()) }
    var waited by remember { mutableIntStateOf(0) }
    val missing = framesToReplay(history, open.map { it.place }.toSet())
    LaunchedEffect(Unit) {
        if (missing.isNotEmpty()) replay(missing)
        replayStarted = true
    }
    if (replayStarted && missing.isNotEmpty()) SideEffect { waited += 1 }
    val ready = replayStarted && (missing.isEmpty() || waited > 8)
    val shown = if (ready) syncMoneyHistory(history, open) else history
    val encoded = encodeMoneyHistory(shown)
    SideEffect { if (stored != encoded) stored = encoded }
    return shown
}

/** A shared file replaces the money stack with import. A dirty form asks first and can keep the file pending. */
@Composable
fun SharedImportEffect(
    inbox: ImportInbox,
    dirty: () -> Boolean,
    onCloseAll: () -> Unit,
    onDiscard: () -> Unit,
    importUris: (List<Uri>) -> Unit,
) {
    val pending by inbox.pending.collectAsStateWithLifecycle()
    var ask by remember { mutableStateOf(false) }
    LaunchedEffect(pending) {
        if (pending.isEmpty() || ask) return@LaunchedEffect
        if (shareImportShouldAsk(dirty())) ask = true
        else {
            onCloseAll()
            importUris(inbox.take().map(Uri::parse))
        }
    }
    if (ask) {
        DiscardChangesDialog(
            onKeep = { ask = false },
            onLeave = {
                ask = false
                onDiscard()
                onCloseAll()
                importUris(inbox.take().map(Uri::parse))
            },
        )
    }
}

private fun transactionFrame(ui: TransactionsUi): MoneyFrame {
    val month = ui.query.month
    val filter = if (month == null) "SEARCH" else ui.query.filter.name
    return MoneyFrame(MoneyPlace.Transactions, month = month?.toString(), filter = filter)
}

private fun replayTransactions(transactions: TransactionsViewModel, month: YearMonth?, filter: String?) {
    if (month == null || filter == "SEARCH") transactions.openSearch()
    else transactions.open(month, runCatching { LineFilter.valueOf(filter ?: "ALL") }.getOrDefault(LineFilter.ALL))
}

private fun MoneyPlace?.toMoneyDestination(): MoneyDestination = when (this) {
    null -> MoneyDestination.Root
    MoneyPlace.Goals -> MoneyDestination.Goals
    MoneyPlace.Tithe -> MoneyDestination.Tithe
    MoneyPlace.Car -> MoneyDestination.Car
    MoneyPlace.Pension -> MoneyDestination.Pension
    MoneyPlace.Subscriptions -> MoneyDestination.Subscriptions
    MoneyPlace.Period -> MoneyDestination.Period
    MoneyPlace.Close -> MoneyDestination.Close
    MoneyPlace.Summary -> MoneyDestination.Summary
    MoneyPlace.Transactions -> MoneyDestination.Transactions
    MoneyPlace.Trend -> MoneyDestination.Trend
    MoneyPlace.Import -> MoneyDestination.Import
}

/** Second tap on כסף. [AppShell] sends it; the money screen closes its stack and scrolls up. */
class MoneyReselectBus {
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val ticks: SharedFlow<Unit> = events.asSharedFlow()
    fun send() { events.tryEmit(Unit) }
}

val LocalMoneyReselect = staticCompositionLocalOf { MoneyReselectBus() }
