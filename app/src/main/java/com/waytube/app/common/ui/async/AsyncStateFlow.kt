package com.waytube.app.common.ui.async

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.fold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transformLatest

private sealed interface FetchEvent<out T> {
    data object Started : FetchEvent<Nothing>

    data class Failed(val error: FetchError) : FetchEvent<Nothing>

    data class Finished<T>(val data: T) : FetchEvent<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> asyncStateFlow(
    refreshSignal: Flow<Unit>,
    fetch: suspend () -> FetchResult<T>
): Flow<AsyncState<T>> {
    return refreshSignal
        .map { true }
        .onStart { emit(false) }
        .transformLatest { isRefresh ->
            if (isRefresh) {
                emit(FetchEvent.Started)
            }

            emit(
                fetch().fold(
                    onSuccess = { FetchEvent.Finished(it) },
                    onFailure = { FetchEvent.Failed(it) }
                )
            )
        }
        .runningFold(AsyncState.Loading as AsyncState<T>) { state, event ->
            when (event) {
                FetchEvent.Started -> when (state) {
                    is AsyncState.Loaded -> {
                        state.copy(isRefreshing = true)
                    }

                    else -> AsyncState.Loading
                }

                is FetchEvent.Failed -> when (state) {
                    is AsyncState.Loaded -> state.copy(isRefreshing = false)
                    else -> AsyncState.Error(error = event.error)
                }

                is FetchEvent.Finished -> {
                    AsyncState.Loaded(
                        data = event.data,
                        isRefreshing = false
                    )
                }
            }
        }
}

fun <T, R> Flow<AsyncState<T>>.mapLoaded(
    transform: (T) -> R
): Flow<AsyncState<R>> =
    map { state ->
        when (state) {
            is AsyncState.Loading, is AsyncState.Error -> state

            is AsyncState.Loaded -> {
                AsyncState.Loaded(
                    data = transform(state.data),
                    isRefreshing = state.isRefreshing
                )
            }
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<AsyncState<T>>.flatMapLoaded(
    transform: suspend (T) -> Flow<R>
): Flow<AsyncState<R>> =
    flatMapLatest { state ->
        when (state) {
            is AsyncState.Loading, is AsyncState.Error -> flowOf(state)

            is AsyncState.Loaded -> {
                transform(state.data).map { data ->
                    AsyncState.Loaded(
                        data = data,
                        isRefreshing = state.isRefreshing
                    )
                }
            }
        }
    }
