package com.xoplayer.app.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val path: String,
    val folderPath: String,
    val uri: Uri,
    val albumArtUri: Uri?
)
