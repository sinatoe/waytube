package com.waytube.app.search.domain

interface SearchRepository {
    suspend fun getSuggestions(query: String): Result<List<String>>
}
