package com.waytube.app.video.data

import com.waytube.app.video.domain.SkipSegment
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class NewPipeVideoRepository(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : VideoRepository {
    override suspend fun getVideo(id: String): Result<Video> =
        runCatching {
            try {
                val info = withContext(Dispatchers.IO) {
                    StreamInfo.getInfo(
                        ServiceList.YouTube,
                        ServiceList.YouTube.streamLHFactory.getUrl(id)
                    )
                }

                info.toVideo()
            } catch (e: ContentNotAvailableException) {
                Video.Unavailable(
                    reason = when (e) {
                        is AgeRestrictedContentException -> Video.Unavailable.Reason.AGE_RESTRICTED
                        else -> null
                    }
                )
            }
        }


    override suspend fun getSkipSegments(id: String): Result<List<SkipSegment>> =
        runCatching {
            withContext(Dispatchers.IO) {
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("sponsor.ajay.app")
                    .addPathSegments("api/skipSegments")
                    .addQueryParameter("videoID", id)
                    .build()

                okHttpClient.newCall(Request(url)).execute().use { response ->
                    when {
                        response.code == 404 -> emptyList()

                        response.isSuccessful -> json
                            .decodeFromString<List<SponsorBlockSkipSegment>>(response.body.string())
                            .map { it.toSkipSegment() }

                        else -> throw IOException()
                    }
                }
            }
        }
}

private fun StreamInfo.toVideo(): Video {
    val thumbnailUrl = thumbnails.maxBy { it.height }.url

    return when (streamType) {
        StreamType.VIDEO_STREAM -> Video.Content.Regular(
            id = id,
            title = name,
            channelName = uploaderName,
            thumbnailUrl = thumbnailUrl,
            dashManifestUrl = generateDashManifestUrl()
        )

        StreamType.LIVE_STREAM -> Video.Content.Live(
            id = id,
            title = name,
            channelName = uploaderName,
            thumbnailUrl = thumbnailUrl,
            hlsPlaylistUrl = hlsUrl
        )

        else -> Video.Unavailable(reason = Video.Unavailable.Reason.UNSUPPORTED)
    }
}

@Serializable
private data class SponsorBlockSkipSegment(
    @SerialName("UUID")
    val uuid: String,
    val segment: List<Double>
) {
    fun toSkipSegment(): SkipSegment = SkipSegment(
        id = uuid,
        start = segment.first().seconds,
        end = segment.last().seconds
    )
}
