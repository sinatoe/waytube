package com.waytube.app.common.ui.pagination

import com.waytube.app.common.domain.FetchError

data class PaginatedData<T>(
    val items: List<T>,
    val state: State
) {
    sealed interface State {
        data object Idle : State

        data object Loading : State

        data class Error(val error: FetchError) : State

        data object Done : State
    }
}
