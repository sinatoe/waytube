package com.waytube.app.common.ui.pagination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waytube.app.R
import com.waytube.app.common.domain.Identifiable
import com.waytube.app.common.ui.element.RetryButton

fun <T : Identifiable> LazyListScope.paginatedDataItems(
    data: PaginatedData<T>,
    onLoad: () -> Unit,
    itemContent: @Composable (T) -> Unit
) {
    items(
        items = data.items,
        key = { it.id }
    ) {
        itemContent(it)
    }

    when (val state = data.state) {
        PaginatedData.State.Idle, PaginatedData.State.Loading -> {
            item {
                if (state is PaginatedData.State.Idle) {
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

        is PaginatedData.State.Error -> {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.message_paging_load_error),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )

                    RetryButton(onClick = onLoad)
                }
            }
        }

        PaginatedData.State.Done -> {
            if (data.items.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.message_paging_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 32.dp
                            ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
