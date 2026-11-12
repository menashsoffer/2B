package com.menash.myself.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.provider.Settings
import com.menash.myself.core.time.TodayProvider
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.feature.home.HomeViewModel
import com.menash.myself.shell.ShellRoute
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

@Composable
fun HomeScreen(
    displayName: String?,
    onOpenRoute: (String) -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContainer = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            dreamRepository = appContainer.dreamRepository,
            errorReporter = appContainer.errorReporter,
            moneyRepository = appContainer.moneyRepository,
            usdRates = appContainer.usdRateFlow,
            today = TodayProvider::today,
            transactions = appContainer.transactionRepository,
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val greetingStore = remember { GreetingStore(context.greetingDataStore) }
    val greetingRecord by greetingStore.record.map<GreetingRecord, GreetingRecord?> { it }
        .collectAsStateWithLifecycle(initialValue = null)
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nowMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val reducedMotion = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    LaunchedEffect(greetingRecord, today) {
        val loaded = greetingRecord ?: return@LaunchedEffect
        if (loaded.lastShown != today) greetingStore.markShown(today)
    }
    val loadedGreeting = greetingRecord
    val greeting = if (loadedGreeting == null) greetingLine(GreetingVisit.Returning, displayName)
        else greetingLine(greetingVisit(loadedGreeting, today), displayName)
    val animateGreeting = loadedGreeting != null && shouldAnimateGreeting(loadedGreeting, today, reducedMotion)
    HomeContent(
        state = state.copy(firstName = firstNameOf(displayName)),
        hour = 0,
        epochMillis = nowMillis,
        greeting = greeting,
        animateGreeting = animateGreeting,
        onOpenRoute = onOpenRoute,
        onOpenAccount = onOpenAccount,
        onMoneyTask = { task ->
            appContainer.monthTaskInbox.offer(task)
            onOpenRoute(ShellRoute.Money)
        },
        modifier = modifier,
    )
}
