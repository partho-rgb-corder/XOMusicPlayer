package com.xoplayer.app.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.xoplayer.app.data.model.Album
import com.xoplayer.app.data.model.Artist
import com.xoplayer.app.data.model.MusicFolder
import com.xoplayer.app.ui.albums.AlbumsScreen
import com.xoplayer.app.ui.artists.ArtistsScreen
import com.xoplayer.app.ui.folders.FoldersScreen

private val tabs = listOf("Albums", "Artists", "Folders")

@Composable
fun LibraryScreen(
    albums: List<Album>,
    artists: List<Artist>,
    folders: List<MusicFolder>,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onFolderClick: (MusicFolder) -> Unit,
    contentPadding: PaddingValues
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding.let { androidx.compose.foundation.layout.PaddingValues(top = it.calculateTopPadding()) })
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        val innerPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())
        when (selectedTab) {
            0 -> AlbumsScreen(albums = albums, onAlbumClick = onAlbumClick, contentPadding = innerPadding)
            1 -> ArtistsScreen(artists = artists, onArtistClick = onArtistClick, contentPadding = innerPadding)
            2 -> FoldersScreen(folders = folders, onFolderClick = onFolderClick, contentPadding = innerPadding)
        }
    }
}
