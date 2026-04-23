package com.waytube.app.video.domain

import com.waytube.app.common.domain.FetchResult

interface VideoRepository {
    suspend fun getVideo(id: String): FetchResult<Video>

    suspend fun getSkipSegments(id: String): FetchResult<List<SkipSegment>>
}
