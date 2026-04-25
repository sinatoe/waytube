package com.waytube.app.common.ui

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transformLatest

@OptIn(ExperimentalCoroutinesApi::class)
sealed interface AsyncState<out T> {
    data object Loading : AsyncState<Nothing>

    data class Loaded<T>(
        val data: T,
        val isRefreshing: Boolean,
        val refresh: () -> Unit
    ) : AsyncState<T>

    data class Error(
        val error: FetchError,
        val retry: () -> Unit
    ) : AsyncState<Nothing>

    companion object {
        fun <T> createFlow(fetch: suspend () -> FetchResult<T>): Flow<AsyncState<T>> {
            val trigger = MutableSharedFlow<Boolean>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

            fun notifyTrigger() {
                trigger.tryEmit(true)
            }

            return trigger
                .onStart { emit(false) }
                .transformLatest { isManualTrigger ->
                    if (isManualTrigger) {
                        emit(FetchEvent.Loading)
                    }
                    emit(FetchEvent.fromResult(fetch()))
                }
                .runningFold(Loading as AsyncState<T>) { state, event ->
                    when (event) {
                        is FetchEvent.Loading -> when (state) {
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
    }
}
