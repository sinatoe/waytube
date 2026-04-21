package com.waytube.app.common.ui

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
        val refreshState: RefreshState
    ) : AsyncState<T> {
        sealed interface RefreshState {
            data object Refreshing : RefreshState

            data class Idle(val refresh: () -> Unit) : RefreshState

            data class Error(
                val exception: Throwable,
                val retry: () -> Unit
            ) : RefreshState
        }
    }

    data class Error(
        val exception: Throwable,
        val retry: () -> Unit
    ) : AsyncState<Nothing>

    companion object {
        fun <T> createFlow(fetch: suspend () -> Result<T>): Flow<AsyncState<T>> {
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
                                refreshState = Loaded.RefreshState.Refreshing
                            )

                            else -> Loading
                        }

                        is FetchEvent.Success -> Loaded(
                            data = event.data,
                            refreshState = Loaded.RefreshState.Idle(refresh = ::notifyTrigger)
                        )

                        is FetchEvent.Failure -> when (state) {
                            is Loaded -> state.copy(
                                refreshState = Loaded.RefreshState.Error(
                                    exception = event.exception,
                                    retry = ::notifyTrigger
                                )
                            )

                            else -> Error(
                                exception = event.exception,
                                retry = ::notifyTrigger
                            )
                        }
                    }
                }
        }
    }
}
