package com.waytube.app.navigation.data

import com.waytube.app.navigation.domain.DeepLinkResult
import com.waytube.app.navigation.domain.NavigationRepository
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService

class NewPipeNavigationRepository : NavigationRepository {
    override fun resolveDeepLink(url: String): DeepLinkResult? =
        when (ServiceList.YouTube.getLinkTypeByUrl(url)) {
            StreamingService.LinkType.STREAM -> DeepLinkResult.Video(
                ServiceList.YouTube.streamLHFactory.getId(url)
            )

            StreamingService.LinkType.CHANNEL -> DeepLinkResult.Channel(
                ServiceList.YouTube.channelLHFactory.getId(url)
            )

            StreamingService.LinkType.PLAYLIST -> DeepLinkResult.Playlist(
                ServiceList.YouTube.playlistLHFactory.getId(url)
            )

            StreamingService.LinkType.NONE -> null
        }
}
