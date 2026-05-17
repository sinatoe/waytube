package com.waytube.app.search.ui

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.common.domain.fold
import com.waytube.app.common.ui.pagination.paginatedDataFlow
import com.waytube.app.preferences.domain.PreferencesRepository
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Parcelize
private data class SearchParams(
    val query: String,
    val filter: SearchFilter?
) : Parcelable

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SearchRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val suggestionsQuery = MutableStateFlow("")

    private val searchParams = savedStateHandle.getMutableStateFlow<SearchParams?>(
        key = "search_params",
        initialValue = null
    )

    private val resultsLoadSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val suggestions = suggestionsQuery
        .debounce { if (it.isEmpty()) Duration.ZERO else REMOTE_SUGGESTIONS_DEBOUNCE }
        .transformLatest { query ->
            if (query.isNotEmpty()) {
                emit(
                    SearchSuggestions(
                        data = repository.getSuggestions(query).fold(
                            onSuccess = { it },
                            onFailure = { emptyList() }
                        ),
                        source = SearchSuggestions.Source.REMOTE
                    )
                )
            } else {
                emitAll(
                    preferencesRepository.searchHistory.map { history ->
                        SearchSuggestions(
                            data = history,
                            source = SearchSuggestions.Source.HISTORY
                        )
                    }
                )
            }
        }

    private val results = searchParams
        .flatMapLatest { params ->
            params?.let { (query, filter) ->
                paginatedDataFlow(resultsLoadSignal) { repository.getResults(query, filter) }
                    .map { results ->
                        SearchResults(
                            data = results,
                            selectedFilter = filter
                        )
                    }
            } ?: flowOf(null)
        }

    val model = combine(
        suggestions,
        results
    ) { suggestions, results ->
        SearchModel(
            suggestions = suggestions,
            results = results
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = SearchModel(
                suggestions = SearchSuggestions(
                    data = emptyList(),
                    source = SearchSuggestions.Source.HISTORY
                ),
                results = null
            )
        )

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateSuggestions -> {
                suggestionsQuery.value = intent.query
            }

            is SearchIntent.Submit -> {
                searchParams.update { data ->
                    if (intent.query != data?.query) {
                        SearchParams(
                            query = intent.query,
                            filter = null
                        )
                    } else data
                }

                viewModelScope.launch {
                    preferencesRepository.saveSearch(intent.query)
                }
            }

            is SearchIntent.ToggleFilter -> {
                searchParams.update { data ->
                    data?.copy(
                        filter = intent.filter.takeUnless { it == data.filter }
                    )
                }
            }

            SearchIntent.LoadResults -> resultsLoadSignal.tryEmit(Unit)
        }
    }

    companion object {
        val REMOTE_SUGGESTIONS_DEBOUNCE = 150.milliseconds
    }
}
