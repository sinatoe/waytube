package com.waytube.app.common.ui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.waytube.app.R
import com.waytube.app.common.domain.PlaylistItem

@Composable
fun PlaylistItemCard(
    item: PlaylistItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ItemCardBase(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        imageOverlayText = stringResource(R.string.label_playlist),
        imageContent = {
            StyledImage(
                data = item.thumbnailUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9)
                    .clip(MaterialTheme.shapes.small)
            )
        },
        detailsContent = {
            Text(
                text = item.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = item.channelName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = pluralStringResource(
                    R.plurals.video_count,
                    item.videoCount.toPluralCount(),
                    item.videoCount.toCompactString()
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@PreviewLightDark
@Composable
private fun PlaylistItemCardPreview() {
    AppTheme {
        PlaylistItemCard(
            item = PlaylistItem(
                id = "",
                url = "",
                title = "Example playlist",
                channelId = "",
                channelName = "Example channel",
                thumbnailUrl = "",
                videoCount = 123
            ),
            onClick = {},
            onLongClick = null
        )
    }
}
