package com.xoplayer.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xoplayer.app.data.db.AppDatabase
import com.xoplayer.app.data.db.FavoriteEntity
import com.xoplayer.app.data.db.PlaylistEntity
import com.xoplayer.app.data.db.PlaylistSongEntity
import com.xoplayer.app.data.model.Album
import com.xoplayer.app.data.model.Artist
import com.xoplayer.app.data.model.MusicFolder
import com.xoplayer.app.data.model.Song
import com.xoplayer.app.data.repo.MediaStoreRepository
import com.xoplayer.app.playback.MusicController
import com.xoplayer.app.playback.RepeatMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class PlaylistWithCount(val playlist: PlaylistEntity, val songCount: Int)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application)
    private val db = AppDatabase.getInstance(application)
    val controller = MusicController(application)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistWithCount>>(emptyList())
    val playlists: StateFlow<List<PlaylistWithCount>> = _playlists.asStateFlow()

    val currentSong = controller.currentSong
    val isPlaying = controller.isPlaying
    val positionMs = controller.positionMs
    val durationMs = controller.durationMs
    val shuffleOn = controller.shuffleOn
    val repeatMode: StateFlow<RepeatMode> = controller.repeatMode
    val queue = controller.queue

    init {
        controller.connect()
        loadLibrary()
        observeFavorites()
        observePlaylists()
        startPositionPolling()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            val loadedSongs = repository.loadSongs()
            _songs.value = loadedSongs
            _albums.value = repository.loadAlbums(loadedSongs)
            _artists.value = repository.loadArtists(loadedSongs)
            _folders.value = repository.loadFolders(loadedSongs)
            _isLoading.value = false
        }
    }

    fun rescanLibrary() = loadLibrary()

    private fun observeFavorites() {
        viewModelScope.launch {
            db.favoriteDao().getAllFavoriteIds().collectLatest { ids ->
                _favoriteIds.value = ids.toSet()
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            db.playlistDao().getAllPlaylists().collectLatest { lists ->
                _playlists.value = lists.map { PlaylistWithCount(it, db.playlistDao().songCount(it.id)) }
            }
        }
    }

    private fun startPositionPolling() {
        viewModelScope.launch {
            while (true) {
                controller.pollPosition()
                delay(500)
            }
        }
    }

    fun playSong(song: Song, fromList: List<Song> = _songs.value) {
        val index = fromList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        controller.playQueue(fromList, index)
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            if (favoriteIds.value.contains(songId)) {
                db.favoriteDao().remove(songId)
            } else {
                db.favoriteDao().add(FavoriteEntity(songId))
            }
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            db.playlistDao().createPlaylist(PlaylistEntity(name = name))
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { db.playlistDao().renamePlaylist(id, name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { db.playlistDao().deletePlaylist(id) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            val count = db.playlistDao().songCount(playlistId)
            db.playlistDao().addSongToPlaylist(
                PlaylistSongEntity(playlistId = playlistId, songId = songId, position = count)
            )
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { db.playlistDao().removeSongFromPlaylist(playlistId, songId) }
    }

    fun playlistName(playlistId: Long): String {
        return playlists.value.firstOrNull { it.playlist.id == playlistId }?.playlist?.name ?: ""
    }

    fun playlistSongsFlow(playlistId: Long) =
        db.playlistDao().getSongsForPlaylist(playlistId).map { entries ->
            val allSongs = _songs.value
            entries.mapNotNull { entry -> allSongs.firstOrNull { it.id == entry.songId } }
        }

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }
}
