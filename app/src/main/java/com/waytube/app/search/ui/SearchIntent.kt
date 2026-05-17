package com.waytube.app.search.ui

import com.waytube.app.search.domain.SearchFilter

sealed interface SearchIntent {
    data class UpdateSuggestions(val query: String) : SearchIntent

    data class Submit(val query: String) : SearchIntent

    data class ToggleFilter(val filter: SearchFilter) : SearchIntent

    data object LoadResults : SearchIntent
}
