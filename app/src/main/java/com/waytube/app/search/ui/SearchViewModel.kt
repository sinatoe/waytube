package com.waytube.app.search.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.waytube.app.preferences.domain.PreferencesRepository
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SearchRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val suggestionsQuery = MutableStateFlow("")

    private val submittedQuery = savedStateHandle.getMutableStateFlow<String?>(
        key = "submitted_query",
        initialValue = null
    )

    private val _selectedFilter = savedStateHandle.getMutableStateFlow<SearchFilter?>(
        key = "selected_filter",
        initialValue = null
    )

    val suggestions = suggestionsQuery
        .debounce { if (it.isNotEmpty()) 150.milliseconds else Duration.ZERO }
        .flatMapLatest { query ->
            if (query.isNotEmpty()) flow {
                emit(
                    SearchSuggestions(
                        items = query.takeIf { it.isNotBlank() }?.let {
                            repository.getSuggestions(it).getOrNull()
                        } ?: emptyList(),
                        type = SearchSuggestions.Type.REMOTE
                    )
                )
            } else preferencesRepository.searchHistory.map {
                SearchSuggestions(
                    items = it,
                    type = SearchSuggestions.Type.HISTORY
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = SearchSuggestions(
                items = emptyList(),
                type = SearchSuggestions.Type.HISTORY
            )
        )

    val isQuerySubmitted = submittedQuery
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = false
        )

    val selectedFilter = _selectedFilter.asStateFlow()

    val results = combine(
        submittedQuery,
        _selectedFilter
    ) { query, filter -> query to filter }
        .flatMapLatest { (query, filter) ->
            if (query != null) repository.getResults(query, filter) else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    fun setSuggestionQuery(query: String) {
        suggestionsQuery.value = query
    }

    fun trySubmit(query: String): Boolean = query
        .takeIf { it.isNotBlank() }
        ?.also { query ->
            if (submittedQuery.value != query) {
                submittedQuery.value = query
                _selectedFilter.value = null

                viewModelScope.launch {
                    preferencesRepository.saveSearch(query)
                }
            }
        } != null

    fun toggleFilter(filter: SearchFilter) {
        _selectedFilter.update { selectedFilter -> filter.takeIf { it != selectedFilter } }
    }
    fun clearSubmittedQuery() {
        submittedQuery.value = null
        _selectedFilter.value = null 
    }
}
