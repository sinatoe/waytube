package com.waytube.app.video.data

import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.encoding.Base64

class NewPipeVideoRepository : VideoRepository {
    override suspend fun getVideo(id: String): Result<Video> =
        runCatching {
            try {
                val info = withContext(Dispatchers.IO) {
                    StreamInfo.getInfo(
                        ServiceList.YouTube,
                        YoutubeStreamLinkHandlerFactory.getInstance().getUrl(id)
                    )
                }

                info.toVideo()
            } catch (e: ContentNotAvailableException) {
                Video.Unavailable(
                    reason = when (e) {
                        is AgeRestrictedContentException -> Video.Unavailable.Reason.AGE_RESTRICTED
                        is PaidContentException -> Video.Unavailable.Reason.MEMBERS_ONLY
                        else -> null
                    }
                )
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

private fun StreamInfo.generateDashManifestUrl(): String {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

    val mpdElement = document.createElement("MPD")
        .also(document::appendChild)
        .apply {
            setAttribute("xmlns", "urn:mpeg:dash:schema:mpd:2011")
            setAttribute("profiles", "urn:mpeg:dash:profile:isoff-on-demand:2011")
            setAttribute("minBufferTime", "PT1.5S")
            setAttribute("type", "static")
            setAttribute("mediaPresentationDuration", "PT${duration}S")
        }

    val periodElement = document.createElement("Period").also(mpdElement::appendChild)

    val videoAdaptationSetElement = document.createElement("AdaptationSet")
        .also(periodElement::appendChild)
        .apply {
            setAttribute("id", "0")
            setAttribute("contentType", "video")
            setAttribute("subsegmentAlignment", "true")
        }

    val audioAdaptationSetElement = document.createElement("AdaptationSet")
        .also(periodElement::appendChild)
        .apply {
            setAttribute("id", "1")
            setAttribute("contentType", "audio")
            setAttribute("subsegmentAlignment", "true")
        }

    videoOnlyStreams
        .map { stream ->
            document.createElement("Representation").apply {
                setAttribute("codecs", stream.codec!!)
                setAttribute("bandwidth", stream.bitrate.toString())
                setAttribute("mimeType", stream.format!!.mimeType)
                setAttribute("width", stream.width.toString())
                setAttribute("height", stream.height.toString())
                setAttribute("frameRate", stream.fps.toString())

                document.createElement("BaseURL").also(::appendChild).apply {
                    textContent = stream.content
                }

                document.createElement("SegmentBase").also(::appendChild).apply {
                    setAttribute("indexRange", "${stream.indexStart}-${stream.indexEnd}")

                    document.createElement("Initialization").also(::appendChild).apply {
                        setAttribute("range", "${stream.initStart}-${stream.initEnd}")
                    }
                }
            }
        }
        .forEach(videoAdaptationSetElement::appendChild)

    audioStreams
        .filter { stream ->
            stream.audioTrackType?.let { it == AudioTrackType.ORIGINAL } ?: true
        }
        .map { stream ->
            document.createElement("Representation").apply {
                setAttribute("codecs", stream.codec!!)
                setAttribute("bandwidth", stream.bitrate.toString())
                setAttribute("mimeType", stream.format!!.mimeType)

                document.createElement("BaseURL").also(::appendChild).apply {
                    textContent = stream.content
                }

                document.createElement("SegmentBase").also(::appendChild).apply {
                    setAttribute("indexRange", "${stream.indexStart}-${stream.indexEnd}")

                    document.createElement("Initialization").also(::appendChild).apply {
                        setAttribute("range", "${stream.initStart}-${stream.initEnd}")
                    }
                }
            }
        }
        .forEach(audioAdaptationSetElement::appendChild)

    val encodedManifest = Base64.encode(
        StringWriter()
            .also { writer ->
                TransformerFactory.newInstance()
                    .newTransformer()
                    .transform(DOMSource(document), StreamResult(writer))
            }
            .toString()
            .toByteArray()
    )

    return "data:application/dash+xml;charset=utf-8;base64,$encodedManifest"
}
