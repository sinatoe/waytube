package com.waytube.app.common.ui.async

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.fold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
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
fun <T, R> asyncStateFlow(
    refreshSignal: Flow<Unit>,
    fetch: suspend () -> FetchResult<T>,
    transform: (T) -> Flow<R>
): Flow<AsyncState<R>> =
    refreshSignal
        .map { true }
        .onStart { emit(false) }
        .transformLatest { isRefresh ->
            if (isRefresh) {
                emit(FetchEvent.Started)
            }

            fetch().fold(
                onSuccess = { data ->
                    emitAll(transform(data).map { FetchEvent.Finished(it) })
                },
                onFailure = { emit(FetchEvent.Failed(it)) }
            )
        }
        .runningFold(AsyncState.Loading as AsyncState<R>) { state, event ->
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
