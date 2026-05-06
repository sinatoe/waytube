package com.waytube.app.video.ui

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.async.mapLoaded
import com.waytube.app.playback.ui.PlaybackManager
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRepository
import com.waytube.app.video.domain.VideoResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModel(
    private val id: String,
    savedStateHandle: SavedStateHandle,
    private val repository: VideoRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {
    private var savedPosition by savedStateHandle.saved<Duration?> { null }

    private val isPlaybackRequested = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private val previewState = asyncStateFlow { repository.getVideo(id) }
        .mapLoaded { response ->
            when (response) {
                is VideoResponse.Content -> {
                    VideoPreview.Content(
                        video = response.video,
                        play = { isPlaybackRequested.tryEmit(true) }
                    )
                }

                is VideoResponse.Unavailable -> {
                    VideoPreview.Unavailable(response.restriction)
                }
            }
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    val scene = previewState
        .map { state ->
            (state as? AsyncState.Loaded)?.let { (preview, isRefreshing) ->
                (preview as? VideoPreview.Content)?.video?.takeIf { !isRefreshing }
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { video ->
            isPlaybackRequested
                .onStart { emit(false) }
                .map { isRequested ->
                    video?.takeIf { isRequested }
                }
        }
        .flatMapLatest { video ->
            if (video != null) {
                requestPlayer(video).map { player ->
                    player?.let {
                        VideoScene.Playback(
                            video = video,
                            player = it,
                            stop = { isPlaybackRequested.tryEmit(false) }
                        )
                    }
                }
            } else {
                flowOf(null)
            }
        }
        .flatMapLatest { playbackScene ->
            if (playbackScene != null) {
                flowOf(playbackScene)
            } else {
                previewState.map { VideoScene.Preview(it) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = VideoScene.Preview(AsyncState.Loading)
        )

    private fun requestPlayer(video: Video): Flow<Player?> =
        playbackManager.requestPlayer(id)
            .transformWhile { player ->
                emit(player)
                player != null
            }
            .transformLatest { player ->
                emit(player)
                while (player != null) {
                    delay(1.seconds)
                    savedPosition = player.currentPosition.milliseconds
                }
            }
            .onEach { player ->
                player?.apply {
                    val (uri, mimeType) = when (video) {
                        is Video.Regular -> video.dashManifestUrl to MimeTypes.APPLICATION_MPD
                        is Video.Live -> video.hlsPlaylistUrl to MimeTypes.APPLICATION_M3U8
                    }

                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setArtist(video.channelName)
                        .setArtworkUri(video.thumbnailUrl.toUri())
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(mimeType)
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    setMediaItem(
                        mediaItem,
                        savedPosition?.inWholeMilliseconds ?: C.TIME_UNSET
                    )
                    prepare()
                    play()
                }
            }
}
