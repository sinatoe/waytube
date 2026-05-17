package com.waytube.app.search.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.preferences.domain.PreferencesRepository
import com.waytube.app.search.domain.SearchRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `suggestions logic handles rapid keystrokes`() = runTest(testDispatcher) {
        val repository = mockk<SearchRepository>()
        val preferencesRepository = mockk<PreferencesRepository>()

        every { preferencesRepository.searchHistory } returns flowOf(emptyList())
        coEvery { repository.getSuggestions(any()) } returns FetchResult.Success(emptyList())

        val viewModel = SearchViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            preferencesRepository = preferencesRepository
        )

        val halfDebounce = SearchViewModel.REMOTE_SUGGESTIONS_DEBOUNCE / 2

        viewModel.model.test {
            assertEquals(SearchSuggestions.Source.HISTORY, awaitItem().suggestions.source)

            viewModel.handleIntent(SearchIntent.UpdateSuggestions(("a")))
            advanceTimeBy(halfDebounce)
            runCurrent()
            expectNoEvents()

            viewModel.handleIntent(SearchIntent.UpdateSuggestions(("ab")))
            advanceTimeBy(halfDebounce)
            runCurrent()
            expectNoEvents()

            viewModel.handleIntent(SearchIntent.UpdateSuggestions(("abc")))
            advanceTimeBy(halfDebounce)
            runCurrent()
            expectNoEvents()

            assertEquals(SearchSuggestions.Source.REMOTE, awaitItem().suggestions.source)
            expectNoEvents()

            viewModel.handleIntent(SearchIntent.UpdateSuggestions(("")))
            runCurrent()
            assertEquals(
                SearchSuggestions.Source.HISTORY,
                expectMostRecentItem().suggestions.source
            )
        }
    }
}
