package com.waytube.app.common.ui.pagination

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.fold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transformLatest

private fun interface Trigger<T> {
    suspend fun execute(): FetchResult<Page<T>>
}

private sealed interface FetchEvent<out T> {
    data object Loading : FetchEvent<Nothing>

    data class Success<T>(val page: Page<T>) : FetchEvent<T>

    data class Failure<T>(
        val error: FetchError,
        val retry: suspend () -> FetchResult<Page<T>>
    ) : FetchEvent<T>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> paginatedDataFlow(page: Page<T>): Flow<PaginatedData<T>> {
    val trigger = MutableSharedFlow<Trigger<T>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    return trigger
        .transformLatest { trigger ->
            emit(FetchEvent.Loading)
            emit(
                trigger.execute().fold(
                    onSuccess = { FetchEvent.Success(it) },
                    onFailure = { error ->
                        FetchEvent.Failure(
                            error = error,
                            retry = trigger::execute
                        )
                    }
                )
            )
        }
        .runningFold(
            initial = PaginatedData(
                items = page.items,
                state = page.next?.let {
                    PaginatedData.State.HasMore.Idle(load = { trigger.tryEmit(it) })
                } ?: PaginatedData.State.Done
            )
        ) { data, event ->
            when (event) {
                FetchEvent.Loading -> data.copy(
                    state = PaginatedData.State.HasMore.Loading
                )

                is FetchEvent.Success -> data.copy(
                    items = data.items + event.page.items,
                    state = event.page.next?.let {
                        PaginatedData.State.HasMore.Idle(load = { trigger.tryEmit(it) })
                    } ?: PaginatedData.State.Done
                )

                is FetchEvent.Failure -> data.copy(
                    state = PaginatedData.State.Error(
                        error = event.error,
                        retry = { trigger.tryEmit(event.retry) }
                    )
                )
            }
        }
}

fun <T> paginatedDataFlow(fetch: suspend () -> FetchResult<Page<T>>): Flow<PaginatedData<T>> =
    paginatedDataFlow(
        Page(items = emptyList(), next = fetch)
    )
