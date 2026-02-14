package com.waytube.app.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCardBase(
    onClick: () -> Unit,
    menuActions: List<MenuAction>,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    imageOverlayText: String? = null,
    imageContent: @Composable BoxScope.() -> Unit,
    detailsContent: @Composable ColumnScope.() -> Unit
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { isMenuExpanded = true }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = verticalAlignment
        ) {
            Box(modifier = Modifier.width(160.dp)) {
                imageContent()

                imageOverlayText?.let { text ->
                    ItemCardImageOverlay(
                        text = text,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                content = detailsContent
            )
        }
    }

    if (isMenuExpanded) {
        ItemMenuSheet(
            actions = menuActions,
            onDismissRequest = { isMenuExpanded = false }
        )
    }
}

@Composable
private fun ItemCardImageOverlay(
    text: String,
    modifier: Modifier = Modifier
) {
    MaterialTheme(colorScheme = AppColorScheme.Dark) {
        Text(
            text = text,
            modifier = modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
                .padding(3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                lineHeightStyle = LineHeightStyle.Default
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemMenuSheet(
    actions: List<MenuAction>,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        for ((label, iconPainter, onClick) in actions) {
            ListItem(
                leadingContent = {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text(text = label)
                },
                modifier = Modifier.clickable {
                    onClick()
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}
