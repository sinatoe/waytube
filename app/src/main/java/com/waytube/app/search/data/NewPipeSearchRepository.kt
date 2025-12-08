package com.waytube.app.search.data

import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.waytube.app.common.data.NewPipePagingSource
import com.waytube.app.common.data.toChannelItem
import com.waytube.app.common.data.toPlaylistItem
import com.waytube.app.common.data.toVideoItem
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchRepository
import com.waytube.app.search.domain.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeSearchRepository : SearchRepository {
    override suspend fun getSuggestions(query: String): Result<List<String>> =
        runCatching {
            withContext(Dispatchers.IO) {
                ServiceList.YouTube.suggestionExtractor.suggestionList(query)
            }
        }

    override fun getResults(query: String, filter: SearchFilter?): Flow<PagingData<SearchResult>> =
        NewPipePagingSource.createFlow(
            extractor = ServiceList.YouTube.getSearchExtractor(
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
            ),
            transform = { item ->
                when (item) {
                    is StreamInfoItem -> item.toVideoItem()?.let(SearchResult::Video)
                    is ChannelInfoItem -> SearchResult.Channel(item.toChannelItem())
                    is PlaylistInfoItem -> item.toPlaylistItem()?.let(SearchResult::Playlist)
                    else -> null
                }
            },
            onLoadError = { exception ->
                if (exception is SearchExtractor.NothingFoundException) {
                    PagingSource.LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                } else null
            }
        )
}
