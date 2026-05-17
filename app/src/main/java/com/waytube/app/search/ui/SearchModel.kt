package com.waytube.app.search.ui

import com.waytube.app.common.ui.pagination.PaginatedData
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchResult

data class SearchModel(
    val suggestions: Suggestions,
    val results: Results?
) {
    data class Suggestions(
        val data: List<String>,
        val source: Source
    ) {
        enum class Source { HISTORY, REMOTE }
    }

    data class Results(
        val data: PaginatedData<SearchResult>,
        val selectedFilter: SearchFilter?
    )
}
