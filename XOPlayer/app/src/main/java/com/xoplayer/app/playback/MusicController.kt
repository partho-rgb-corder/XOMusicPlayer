package com.xoplayer.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.xoplayer.app.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode { OFF, ONE, ALL }

/**
 * Thin wrapper around a MediaController connected to PlaybackService.
 * Exposes simple state flows the UI can collect, so screens don't need
 * to talk to the Media3 APIs directly.
 */
class MusicController(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleOn = MutableStateFlow(false)
    val shuffleOn: StateFlow<Boolean> = _shuffleOn.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private var queueSongs: List<Song> = emptyList()

    fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            attachListener()
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    private fun attachListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = controller?.currentMediaItemIndex ?: 0
                queueSongs.getOrNull(idx)?.let { _currentSong.value = it }
                _durationMs.value = controller?.duration?.coerceAtLeast(0) ?: 0L
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _durationMs.value = controller?.duration?.coerceAtLeast(0) ?: 0L
            }
        })
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        queueSongs = songs
        _queue.value = songs
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
        }
        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.play()
        _currentSong.value = songs.getOrNull(startIndex)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun pollPosition() {
        controller?.let { _positionMs.value = it.currentPosition.coerceAtLeast(0) }
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val newValue = !c.shuffleModeEnabled
        c.shuffleModeEnabled = newValue
        _shuffleOn.value = newValue
    }

    fun cycleRepeat() {
        val c = controller ?: return
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        c.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun removeFromQueue(index: Int) {
        controller?.removeMediaItem(index)
        queueSongs = queueSongs.filterIndexed { i, _ -> i != index }
        _queue.value = queueSongs
    }

    fun moveInQueue(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
        val mutable = queueSongs.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        queueSongs = mutable
        _queue.value = queueSongs
    }

    fun playFromQueueIndex(index: Int) {
        controller?.seekTo(index, 0L)
        controller?.play()
    }

    fun clearQueue() {
        controller?.clearMediaItems()
        queueSongs = emptyList()
        _queue.value = emptyList()
    }
}
