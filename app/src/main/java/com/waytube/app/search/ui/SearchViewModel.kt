package com.waytube.app.search.ui

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.waytube.app.preferences.domain.PreferencesRepository
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Parcelize
private data class SearchState(
    val query: String,
    val filter: SearchFilter? = null
) : Parcelable

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SearchRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val suggestionsQuery = MutableStateFlow("")

    private val searchState = savedStateHandle.getMutableStateFlow<SearchState?>(
        key = "search_state",
        initialValue = null
    )

    val suggestions = suggestionsQuery
        .debounce { if (it.isNotEmpty()) REMOTE_SUGGESTIONS_DEBOUNCE else Duration.ZERO }
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

    val isSearchSubmitted = searchState
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = false
        )

    val selectedFilter = searchState
        .map { it?.filter }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val results = searchState
        .filterNotNull()
        .flatMapLatest { (query, filter) -> repository.getResults(query, filter) }
        .cachedIn(viewModelScope)

    init {
        searchState
            .filterNotNull()
            .map { it.query }
            .distinctUntilChanged()
            .onEach(preferencesRepository::saveSearch)
            .launchIn(viewModelScope)
    }

    fun setSuggestionQuery(query: String) {
        suggestionsQuery.value = query
    }

    fun trySubmit(query: String): Boolean = query
        .takeIf { it.isNotBlank() }
        ?.also { query ->
            searchState.update { state ->
                if (state?.query != query) SearchState(query) else state
            }
        } != null

    fun toggleFilter(filter: SearchFilter) {
        searchState.update { state ->
            state?.copy(filter = filter.takeIf { state.filter != it }) ?: state
        }
    }

    companion object {
        val REMOTE_SUGGESTIONS_DEBOUNCE = 150.milliseconds
    }
}
