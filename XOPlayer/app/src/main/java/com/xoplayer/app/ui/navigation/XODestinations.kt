package com.xoplayer.app.ui.navigation

sealed class XODest(val route: String) {
    data object Home : XODest("home")
    data object Songs : XODest("songs")
    data object Library : XODest("library")
    data object Playlists : XODest("playlists")
    data object Favorites : XODest("favorites")
    data object Settings : XODest("settings")
    data object NowPlaying : XODest("now_playing")
    data object Queue : XODest("queue")
    data object AlbumDetail : XODest("album_detail/{albumId}") {
        fun build(albumId: Long) = "album_detail/$albumId"
    }
    data object ArtistDetail : XODest("artist_detail/{artistName}") {
        fun build(artistName: String) = "artist_detail/$artistName"
    }
    data object FolderDetail : XODest("folder_detail/{folderPath}") {
        fun build(folderPath: String) = "folder_detail/${android.net.Uri.encode(folderPath)}"
    }
    data object PlaylistDetail : XODest("playlist_detail/{playlistId}") {
        fun build(playlistId: Long) = "playlist_detail/$playlistId"
    }
}
