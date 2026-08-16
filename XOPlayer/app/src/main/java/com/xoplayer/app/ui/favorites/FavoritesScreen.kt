package com.xoplayer.app.ui.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xoplayer.app.data.model.Song
import com.xoplayer.app.ui.components.SongRow

@Composable
fun FavoritesScreen(
    favoriteSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Text(
                "Favorites",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 10.dp)
            )
        }
        if (favoriteSongs.isEmpty()) {
            item {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No favorites yet. Tap the heart on any song to add it here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(favoriteSongs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isFavorite = true,
                    onClick = { onSongClick(song) },
                    onFavoriteClick = { onFavoriteClick(song) },
                    onMoreClick = { onMoreClick(song) }
                )
            }
        }
    }
}
