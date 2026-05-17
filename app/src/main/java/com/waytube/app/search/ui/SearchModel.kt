package com.waytube.app.search.ui

import com.waytube.app.common.ui.pagination.PaginatedData
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchResult

data class SearchModel(
    val suggestions: SearchSuggestions,
    val results: SearchResults?
)

data class SearchSuggestions(
    val data: List<String>,
    val source: Source
) {
    enum class Source { HISTORY, REMOTE }
}

data class SearchResults(
    val data: PaginatedData<SearchResult>,
    val selectedFilter: SearchFilter?
)
