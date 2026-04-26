package com.waytube.app.common.ui.pagination

import com.waytube.app.common.domain.FetchError

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
            val error: FetchError,
            val retry: () -> Unit
        ) : State

        data object Done : State
    }
}
