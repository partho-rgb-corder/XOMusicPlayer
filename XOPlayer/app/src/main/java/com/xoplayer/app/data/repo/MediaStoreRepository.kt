package com.xoplayer.app.data.repo

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.xoplayer.app.data.model.Album
import com.xoplayer.app.data.model.Artist
import com.xoplayer.app.data.model.MusicFolder
import com.xoplayer.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads the local audio library via Android MediaStore.
 * All queries run off the main thread.
 */
class MediaStoreRepository(private val context: Context) {

    private val artworkBaseUri: Uri = Uri.parse("content://media/external/audio/albumart")

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idCol)
                        val albumId = cursor.getLong(albumIdCol)
                        val path = cursor.getString(dataCol) ?: continue
                        val contentUri = ContentUris.withAppendedId(collection, id)
                        val artUri = ContentUris.withAppendedId(artworkBaseUri, albumId)

                        songs.add(
                            Song(
                                id = id,
                                title = cursor.getString(titleCol) ?: File(path).nameWithoutExtension,
                                artist = cursor.getString(artistCol) ?: "Unknown Artist",
                                album = cursor.getString(albumCol) ?: "Unknown Album",
                                albumId = albumId,
                                duration = cursor.getLong(durationCol),
                                path = path,
                                folderPath = File(path).parent ?: "",
                                uri = contentUri,
                                albumArtUri = artUri
                            )
                        )
                    } catch (e: Exception) {
                        // Skip corrupted / unreadable row, keep scanning.
                    }
                }
            }
        songs
    }

    suspend fun loadAlbums(songs: List<Song>): List<Album> = withContext(Dispatchers.Default) {
        songs.groupBy { it.albumId }.map { (albumId, list) ->
            Album(
                id = albumId,
                name = list.first().album,
                artist = list.first().artist,
                songCount = list.size,
                artUri = list.first().albumArtUri
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun loadArtists(songs: List<Song>): List<Artist> = withContext(Dispatchers.Default) {
        songs.groupBy { it.artist }.map { (artist, list) ->
            Artist(
                id = artist.hashCode().toLong(),
                name = artist,
                songCount = list.size,
                albumCount = list.map { it.albumId }.distinct().size
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun loadFolders(songs: List<Song>): List<MusicFolder> = withContext(Dispatchers.Default) {
        songs.groupBy { it.folderPath }.map { (path, list) ->
            MusicFolder(
                path = path,
                name = File(path).name.ifBlank { path },
                songCount = list.size
            )
        }.sortedBy { it.name.lowercase() }
    }
}
