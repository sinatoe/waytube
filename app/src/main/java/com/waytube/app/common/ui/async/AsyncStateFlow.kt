package com.waytube.app.common.ui.async

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.fold
import com.waytube.app.common.ui.async.AsyncState.Error
import com.waytube.app.common.ui.async.AsyncState.Loaded
import com.waytube.app.common.ui.async.AsyncState.Loading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transformLatest

private enum class Trigger {
    MANUAL,
    AUTOMATIC
}

private sealed interface FetchEvent<out T> {
    data object Loading : FetchEvent<Nothing>

    data class Success<T>(val data: T) : FetchEvent<T>

    data class Failure(val error: FetchError) : FetchEvent<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> asyncStateFlow(fetch: suspend () -> FetchResult<T>): Flow<AsyncState<T>> {
    val trigger = MutableSharedFlow<Trigger>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun notifyTrigger() {
        trigger.tryEmit(Trigger.MANUAL)
    }

    return trigger
        .onStart { emit(Trigger.AUTOMATIC) }
        .transformLatest { trigger ->
            if (trigger == Trigger.MANUAL) {
                emit(FetchEvent.Loading)
            }

            emit(
                fetch().fold(
                    onSuccess = { FetchEvent.Success(it) },
                    onFailure = { FetchEvent.Failure(it) }
                )
            )
        }
        .runningFold(Loading as AsyncState<T>) { state, event ->
            when (event) {
                FetchEvent.Loading -> when (state) {
                    is Loaded -> state.copy(
                        isRefreshing = true,
                        refresh = {}
                    )

                    else -> Loading
                }

                is FetchEvent.Success -> Loaded(
                    data = event.data,
                    isRefreshing = false,
                    refresh = ::notifyTrigger
                )

                is FetchEvent.Failure -> when (state) {
                    is Loaded -> {
                        state.copy(
                            isRefreshing = false,
                            refresh = ::notifyTrigger
                        )
                    }

                    else -> Error(
                        error = event.error,
                        retry = ::notifyTrigger
                    )
                }
            }
        }
}
