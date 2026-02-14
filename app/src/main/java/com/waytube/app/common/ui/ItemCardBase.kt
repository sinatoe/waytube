package com.waytube.app.common.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCardBase(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    imageOverlayText: String? = null,
    imageContent: @Composable BoxScope.() -> Unit,
    detailsContent: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
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
