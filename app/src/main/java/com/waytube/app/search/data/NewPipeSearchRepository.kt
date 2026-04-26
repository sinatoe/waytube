package com.waytube.app.search.data

import com.waytube.app.common.data.fetch
import com.waytube.app.common.data.mapping.toChannelItem
import com.waytube.app.common.data.mapping.toPlaylistItem
import com.waytube.app.common.data.mapping.toVideoItem
import com.waytube.app.common.data.paginate
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchRepository
import com.waytube.app.search.domain.SearchResult
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeSearchRepository : SearchRepository {
    override suspend fun getSuggestions(query: String): FetchResult<List<String>> =
        fetch {
            ServiceList.YouTube.suggestionExtractor.suggestionList(query)
        }

    override suspend fun getResults(
        query: String,
        filter: SearchFilter?
    ): FetchResult<Page<SearchResult>> =
        ServiceList.YouTube
            .getSearchExtractor(
                query,
                listOfNotNull(
                    when (filter) {
                        SearchFilter.VIDEOS -> YoutubeSearchQueryHandlerFactory.VIDEOS
                        SearchFilter.CHANNELS -> YoutubeSearchQueryHandlerFactory.CHANNELS
                        SearchFilter.PLAYLISTS -> YoutubeSearchQueryHandlerFactory.PLAYLISTS
                        null -> null
                    },
                ),
                null
            )
            .paginate(
                treatErrorAsEmpty = { it is SearchExtractor.NothingFoundException }
            ) { item ->
                when (item) {
                    is StreamInfoItem -> item.toVideoItem()?.let(SearchResult::Video)
                    is ChannelInfoItem -> SearchResult.Channel(item.toChannelItem())
                    is PlaylistInfoItem -> item.toPlaylistItem()?.let(SearchResult::Playlist)
                    else -> null
                }
            }
}
