package com.waytube.app.common.ui.pagination

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.fold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform

private sealed interface FetchEvent<out T> {
    data object Started : FetchEvent<Nothing>

    data class Failed<T>(val error: FetchError) : FetchEvent<T>

    data class Finished<T>(val page: Page<T>) : FetchEvent<T>
}

fun <T> paginatedDataFlow(
    page: Page<T>,
    loadSignal: Flow<Unit>
): Flow<PaginatedData<T>> {
    if (page.next == null) {
        return flowOf(
            PaginatedData(
                items = page.items,
                state = PaginatedData.State.Done
            )
        )
    }

    fun fetchEventFlow(fetch: suspend () -> FetchResult<Page<T>>): Flow<FetchEvent<T>> =
        loadSignal
            .take(1)
            .transform {
                emit(FetchEvent.Started)
                emit(
                    fetch().fold(
                        onSuccess = { FetchEvent.Finished(it) },
                        onFailure = { FetchEvent.Failed(it) }
                    )
                )
            }
            .transform { event ->
                emit(event)

                when (event) {
                    FetchEvent.Started -> {}
                    is FetchEvent.Failed -> emitAll(fetchEventFlow(fetch))
                    is FetchEvent.Finished -> {
                        event.page.next?.let { fetch ->
                            emitAll(fetchEventFlow(fetch))
                        }
                    }
                }
            }

    return fetchEventFlow(page.next)
        .runningFold(
            PaginatedData(
                items = page.items,
                state = PaginatedData.State.Idle
            )
        ) { data, event ->
            when (event) {
                FetchEvent.Started -> {
                    data.copy(state = PaginatedData.State.Loading)
                }

                is FetchEvent.Failed -> {
                    data.copy(state = PaginatedData.State.Error(event.error))
                }

                is FetchEvent.Finished -> {
                    data.copy(
                        items = data.items + event.page.items,
                        state = if (event.page.next != null) {
                            PaginatedData.State.Idle
                        } else {
                            PaginatedData.State.Done
                        }
                    )
                }
            }
        }
}

fun <T> paginatedDataFlow(
    loadSignal: Flow<Unit>,
    fetch: suspend () -> FetchResult<Page<T>>
): Flow<PaginatedData<T>> =
    paginatedDataFlow(
        page = Page(items = emptyList(), next = fetch),
        loadSignal = loadSignal
    )
