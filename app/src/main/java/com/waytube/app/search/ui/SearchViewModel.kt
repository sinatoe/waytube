package com.waytube.app.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.search.domain.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val suggestionsQuery = MutableStateFlow("")

    val suggestions = suggestionsQuery
        .debounce(150.milliseconds)
        .mapLatest { query ->
            query.takeIf { it.isNotBlank() }?.let {
                repository.getSuggestions(it).getOrNull()
            } ?: emptyList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun setSuggestionQuery(query: String) {
        suggestionsQuery.value = query
    }
}
