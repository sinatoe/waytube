package com.waytube.app.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waytube.app.R
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    val textFieldState = rememberTextFieldState()

    LaunchedEffect(textFieldState.text) {
        viewModel.setSuggestionQuery(textFieldState.text.toString())
    }

    SearchScreenContent(
        textFieldState = textFieldState,
        suggestions = { suggestions }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenContent(
    textFieldState: TextFieldState,
    suggestions: () -> List<String>
) {
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = {
                scope.launch { searchBarState.animateToCollapsed() }
            },
            leadingIcon = {
                if (searchBarState.targetValue == SearchBarValue.Expanded) {
                    IconButton(
                        onClick = {
                            scope.launch { searchBarState.animateToCollapsed() }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null
                    )
                }
            },
            placeholder = {
                Text(text = stringResource(R.string.label_search))
            },
            trailingIcon = if (
                searchBarState.targetValue == SearchBarValue.Expanded
                && textFieldState.text.isNotEmpty()
            ) ({
                IconButton(onClick = textFieldState::clearText) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cd_clear)
                    )
                }
            }) else null
        )
    }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        windowInsets = {
            SearchBarDefaults.fullScreenWindowInsets.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            )
        }
    ) {
        LazyColumn(
            contentPadding = SearchBarDefaults.fullScreenWindowInsets
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues()
        ) {
            items(suggestions()) { suggestion ->
                ListItem(
                    modifier = Modifier.clickable {
                        textFieldState.setTextAndPlaceCursorAtEnd(suggestion)
                        scope.launch { searchBarState.animateToCollapsed() }
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null
                        )
                    },
                    headlineContent = {
                        Text(
                            text = suggestion,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                state = searchBarState,
                inputField = inputField
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding))
    }
}
