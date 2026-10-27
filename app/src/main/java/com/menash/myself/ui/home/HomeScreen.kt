package com.menash.myself.ui.home

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.feature.home.HomeViewModel
import kotlinx.coroutines.delay

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
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
    HomeContent(
        state = state.copy(firstName = firstNameOf(displayName)),
        hour = hourOf(nowMillis),
        epochMillis = nowMillis,
        onOpenRoute = onOpenRoute,
        onOpenAccount = onOpenAccount,
        modifier = modifier,
    )
}
