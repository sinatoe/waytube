package com.waytube.app.search.domain

import com.waytube.app.common.domain.Page

interface SearchRepository {
    suspend fun getSuggestions(query: String): Result<List<String>>

    suspend fun getResults(query: String, filter: SearchFilter?): Result<Page<SearchResult>>
}
