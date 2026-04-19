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
    ) : AsyncState<T>

    data class Error(
        val exception: Throwable,
        val retry: () -> Unit
    ) : AsyncState<Nothing>

    companion object {
        fun <T> createFlow(fetch: suspend () -> Result<T>): Flow<AsyncState<T>> {
            val trigger = MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

            fun notifyTrigger() {
                trigger.tryEmit(Unit)
            }

            return trigger
                .onStart { emit(Unit) }
                .transformLatest {
                    emit(null)
                    emit(fetch())
                }
                .runningFold(Loading as AsyncState<T>) { state, result ->
                    result?.fold(
                        onSuccess = { data ->
                            Loaded(
                                data = data,
                                refreshState = RefreshState.Idle(refresh = ::notifyTrigger)
                            )
                        },
                        onFailure = { exception ->
                            when (state) {
                                is Loaded -> state.copy(
                                    refreshState = RefreshState.Error(
                                        exception = exception,
                                        retry = ::notifyTrigger
                                    )
                                )

                                else -> Error(
                                    exception = exception,
                                    retry = ::notifyTrigger
                                )
                            }
                        }
                    ) ?: when (state) {
                        is Loaded -> state.copy(
                            refreshState = RefreshState.Refreshing
                        )

                        else -> Loading
                    }
                }
        }
    }
}
