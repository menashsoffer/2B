package com.menash.myself.feature.money

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal sealed interface ScreenLoad<out T> {
    data object Loading : ScreenLoad<Nothing>
    data object Error : ScreenLoad<Nothing>
    data class Ready<T>(val value: T) : ScreenLoad<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> screenLoad(retry: Flow<Int>, source: () -> Flow<T>): Flow<ScreenLoad<T>> =
    retry.flatMapLatest {
        source().map<T, ScreenLoad<T>> { ScreenLoad.Ready(it) }
            .onStart { emit(ScreenLoad.Loading) }
            .catch { emit(ScreenLoad.Error) }
    }
