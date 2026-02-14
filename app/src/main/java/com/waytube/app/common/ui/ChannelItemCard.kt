package com.waytube.app.common.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.waytube.app.R
import com.waytube.app.common.domain.ChannelItem

@Composable
fun ChannelItemCard(
    item: ChannelItem,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ItemCardBase(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        imageContent = {
            StyledImage(
                data = item.avatarUrl,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
            )
        },
        detailsContent = {
            Text(
                text = item.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )

            item.subscriberCount?.let { subscriberCount ->
                Text(
                    text = pluralStringResource(
                        R.plurals.subscriber_count,
                        subscriberCount.toPluralCount(),
                        subscriberCount.toCompactString()
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun ChannelItemCardPreview() {
    AppTheme {
        ChannelItemCard(
            item = ChannelItem(
                id = "",
                url = "",
                name = "Example channel",
                avatarUrl = "",
                subscriberCount = 12_345
            ),
            onClick = {},
            onLongClick = null
        )
    }
}
