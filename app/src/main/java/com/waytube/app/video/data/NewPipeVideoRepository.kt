package com.waytube.app.video.data

import com.waytube.app.video.domain.SkipSegment
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.SignInConfirmNotBotException
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import kotlin.time.Duration.Companion.seconds

private const val SPONSOR_BLOCK_API_ENDPOINT = "https://sponsor.ajay.app/api/skipSegments"

class NewPipeVideoRepository(private val httpClient: HttpClient) : VideoRepository {
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
            } catch (e: ParsingException) {
                Video.Unavailable(
                    reason = when (e) {
                        is AgeRestrictedContentException -> Video.Unavailable.Reason.AGE_RESTRICTED
                        is SignInConfirmNotBotException -> Video.Unavailable.Reason.BOT_FLAGGED
                        is PaidContentException -> Video.Unavailable.Reason.MEMBERS_ONLY
                        is ContentNotAvailableException -> null
                        else -> throw e
                    }
                )
            }
        }


    override suspend fun getSkipSegments(id: String): Result<List<SkipSegment>> =
        runCatching {
            try {
                httpClient
                    .get(SPONSOR_BLOCK_API_ENDPOINT) {
                        expectSuccess = true
                        url {
                            parameters["videoID"] = id
                        }
                    }
                    .body<List<SponsorBlockSkipSegment>>()
                    .map { it.toSkipSegment() }
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.NotFound) emptyList() else throw e
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
