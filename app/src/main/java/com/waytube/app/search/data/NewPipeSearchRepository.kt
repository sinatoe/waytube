package com.waytube.app.search.data

import com.waytube.app.search.domain.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList

class NewPipeSearchRepository : SearchRepository {
    override suspend fun getSuggestions(query: String): Result<List<String>> =
        runCatching {
            withContext(Dispatchers.IO) {
                ServiceList.YouTube.suggestionExtractor.suggestionList(query)
            }
        }
}
