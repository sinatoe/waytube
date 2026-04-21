package com.waytube.app.common.ui

import com.waytube.app.common.domain.Page
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transformLatest

@OptIn(ExperimentalCoroutinesApi::class)
data class PaginatedData<T>(
    val items: List<T>,
    val state: State
) {
    sealed interface State {
        sealed interface HasMore : State {
            data object Loading : HasMore

            data class Idle(val load: () -> Unit) : HasMore
        }

        data class Error(
            val exception: Throwable,
            val retry: () -> Unit
        ) : State

        data object Done : State
    }

    companion object {
        fun <T> createFlow(page: Page<T>): Flow<PaginatedData<T>> {
            val trigger = MutableSharedFlow<suspend () -> Result<Page<T>>>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )

            return trigger
                .transformLatest { fetch ->
                    emit(FetchEvent.Loading to fetch)
                    emit(FetchEvent.fromResult(fetch()) to fetch)
                }
                .runningFold(
                    initial = PaginatedData(
                        items = page.items,
                        state = page.next?.let {
                            State.HasMore.Idle(load = { trigger.tryEmit(it) })
                        } ?: State.Done
                    )
                ) { data, (event, fetch) ->
                    when (event) {
                        is FetchEvent.Loading -> data.copy(
                            state = State.HasMore.Loading
                        )

                        is FetchEvent.Success -> data.copy(
                            items = data.items + event.data.items,
                            state = event.data.next?.let {
                                State.HasMore.Idle(load = { trigger.tryEmit(it) })
                            } ?: State.Done
                        )

                        is FetchEvent.Failure -> data.copy(
                            state = State.Error(
                                exception = event.exception,
                                retry = { trigger.tryEmit(fetch) }
                            )
                        )
                    }
                }
        }

        fun <T> createFlow(fetch: suspend () -> Result<Page<T>>): Flow<PaginatedData<T>> =
            createFlow(
                Page(items = emptyList(), next = fetch)
            )
    }
}
