package com.waytube.app.common.ui

import com.waytube.app.common.domain.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart

class PagedListLoader {
    private val trigger = MutableSharedFlow<Unit>()

    fun <T> bind(fetch: suspend () -> Result<Page<T>>): Flow<PagedList<T>> =
        channelFlow {
            var items = emptyList<T>()
            var fetch: (suspend () -> Result<Page<T>>)? = fetch

            trigger
                .onStart { emit(Unit) }
                .collectLatest {
                    val loader = fetch ?: return@collectLatest

                    send(
                        PagedList(
                            items = items,
                            state = PagedList.State.HasMore(isLoading = true)
                        )
                    )

                    loader().fold(
                        onSuccess = { page ->
                            items = items + page.items
                            fetch = page.next
                            send(
                                PagedList(
                                    items = items,
                                    state = if (fetch != null) {
                                        PagedList.State.HasMore(isLoading = false)
                                    } else PagedList.State.Done
                                )
                            )
                        },
                        onFailure = { error ->
                            send(
                                PagedList(
                                    items = items,
                                    state = PagedList.State.Error(error)
                                )
                            )
                        }
                    )
                }
        }

    suspend fun load() {
        trigger.emit(Unit)
    }
}
