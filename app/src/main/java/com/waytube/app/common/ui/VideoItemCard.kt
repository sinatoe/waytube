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
import com.waytube.app.common.domain.VideoItem
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun VideoItemCard(
    item: VideoItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ItemCardBase(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        imageOverlayText = when (item) {
            is VideoItem.Regular -> item.duration.toFormattedString()
            is VideoItem.Live -> stringResource(R.string.label_live)
        },
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

            item.channelName?.let { channelName ->
                Text(
                    text = channelName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                when (item) {
                    is VideoItem.Regular -> listOfNotNull(
                        pluralStringResource(
                            R.plurals.view_count,
                            item.viewCount.toPluralCount(),
                            item.viewCount.toCompactString()
                        ),
                        item.uploadedAt?.toRelativeTimeString()
                    ).joinToString(stringResource(R.string.separator_bullet))

                    is VideoItem.Live -> pluralStringResource(
                        R.plurals.watching_count,
                        item.watchingCount.toPluralCount(),
                        item.watchingCount.toCompactString()
                    )
                },
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
private fun VideoItemCardPreview() {
    AppTheme {
        VideoItemCard(
            item = VideoItem.Regular(
                id = "",
                url = "",
                title = "Example video",
                channelId = "",
                channelName = "Example channel",
                thumbnailUrl = "",
                duration = 12.minutes + 34.seconds,
                viewCount = 1_234_567,
                uploadedAt = Clock.System.now() - 14.days
            ),
            onClick = {},
            onLongClick = null
        )
    }
}
