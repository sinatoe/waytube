package com.waytube.app.search.domain

import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page

interface SearchRepository {
    suspend fun getSuggestions(query: String): FetchResult<List<String>>

    suspend fun getResults(query: String, filter: SearchFilter?): FetchResult<Page<SearchResult>>
}
