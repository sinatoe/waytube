package com.waytube.app.search.domain

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun getSuggestions(query: String): Result<List<String>>

    fun getResults(query: String, filter: SearchFilter?): Flow<PagingData<SearchResult>>
}
