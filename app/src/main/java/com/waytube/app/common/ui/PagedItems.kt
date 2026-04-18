package com.waytube.app.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waytube.app.R
import com.waytube.app.common.domain.Identifiable

fun <T : Identifiable> LazyListScope.pagedItems(
    list: PagedList<T>,
    onLoad: () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    items(
        items = list.items,
        key = { it.id }
    ) {
        itemContent(it)
    }

    when (val state = list.state) {
        is PagedList.State.HasMore -> {
            item {
                if (!state.isLoading) {
                    LaunchedEffect(Unit) {
                        onLoad()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is PagedList.State.Error -> {
            item {
                StateMessage(
                    text = stringResource(R.string.message_paging_load_error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    onRetry = onLoad
                )
            }
        }

        is PagedList.State.Done -> {
            if (list.items.isEmpty()) {
                item {
                    StateMessage(
                        text = stringResource(R.string.message_paging_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
            }
        }
    }
}
