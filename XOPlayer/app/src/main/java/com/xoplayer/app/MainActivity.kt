package com.xoplayer.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xoplayer.app.data.model.Song
import com.xoplayer.app.ui.components.AddToPlaylistDialog
import com.xoplayer.app.ui.components.MiniPlayer
import com.xoplayer.app.ui.favorites.FavoritesScreen
import com.xoplayer.app.ui.home.HomeScreen
import com.xoplayer.app.ui.library.LibraryScreen
import com.xoplayer.app.ui.navigation.XODest
import com.xoplayer.app.ui.nowplaying.NowPlayingScreen
import com.xoplayer.app.ui.playlists.PlaylistDetailScreen
import com.xoplayer.app.ui.playlists.PlaylistsScreen
import com.xoplayer.app.ui.queue.QueueScreen
import com.xoplayer.app.ui.settings.SettingsScreen
import com.xoplayer.app.ui.songs.SongsScreen
import com.xoplayer.app.ui.theme.XOPlayerTheme
import com.xoplayer.app.viewmodel.MusicViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result observed via re-render; MediaStore query simply returns empty until granted. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)

        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            XOPlayerTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    XOPlayerRoot(
                        viewModel = viewModel,
                        darkTheme = darkTheme,
                        onDarkThemeToggle = { darkTheme = it }
                    )
                }
            }
        }
    }
}

@Composable
fun XOPlayerRoot(
    viewModel: MusicViewModel,
    darkTheme: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.positionMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()
    val shuffleOn by viewModel.shuffleOn.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queue by viewModel.queue.collectAsState()

    val favoriteSongs = songs.filter { favoriteIds.contains(it.id) }
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomBarRoutes = setOf(
        XODest.Home.route, XODest.Songs.route, XODest.Library.route,
        XODest.Playlists.route, XODest.Favorites.route
    )
    val showBottomBar = currentRoute in bottomBarRoutes
    val showMiniPlayer = currentSong != null && currentRoute != XODest.NowPlaying.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val items = listOf(
                        Triple(XODest.Home.route, Icons.Filled.Home, "Home"),
                        Triple(XODest.Songs.route, Icons.Filled.LibraryMusic, "Songs"),
                        Triple(XODest.Library.route, Icons.Filled.QueueMusic, "Library"),
                        Triple(XODest.Playlists.route, Icons.Filled.Folder, "Playlists"),
                        Triple(XODest.Favorites.route, Icons.Filled.Favorite, "Favorites")
                    )
                    items.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = XODest.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(XODest.Home.route) {
                    HomeScreen(
                        recentlyAdded = songs.take(15),
                        quickAccess = songs,
                        onSongClick = { viewModel.playSong(it); navController.navigate(XODest.NowPlaying.route) },
                        onSettingsClick = { navController.navigate(XODest.Settings.route) },
                        contentPadding = bottomAwareContentPadding(innerPadding, showMiniPlayer)
                    )
                }
                composable(XODest.Songs.route) {
                    SongsScreen(
                        songs = songs,
                        favoriteIds = favoriteIds,
                        onSongClick = { viewModel.playSong(it); navController.navigate(XODest.NowPlaying.route) },
                        onFavoriteClick = { viewModel.toggleFavorite(it.id) },
                        onMoreClick = { song -> songForPlaylistDialog = song },
                        contentPadding = bottomAwareContentPadding(innerPadding, showMiniPlayer)
                    )
                }
                composable(XODest.Library.route) {
                    LibraryScreen(
                        albums = albums,
                        artists = artists,
                        folders = folders,
                        onAlbumClick = { album ->
                            val albumSongs = songs.filter { it.albumId == album.id }
                            if (albumSongs.isNotEmpty()) {
                                viewModel.playSong(albumSongs.first(), albumSongs)
                                navController.navigate(XODest.NowPlaying.route)
                            }
                        },
                        onArtistClick = { artist ->
                            val artistSongs = songs.filter { it.artist == artist.name }
                            if (artistSongs.isNotEmpty()) {
                                viewModel.playSong(artistSongs.first(), artistSongs)
                                navController.navigate(XODest.NowPlaying.route)
                            }
                        },
                        onFolderClick = { folder ->
                            val folderSongs = songs.filter { it.folderPath == folder.path }
                            if (folderSongs.isNotEmpty()) {
                                viewModel.playSong(folderSongs.first(), folderSongs)
                                navController.navigate(XODest.NowPlaying.route)
                            }
                        },
                        contentPadding = bottomAwareContentPadding(innerPadding, showMiniPlayer)
                    )
                }
                composable(XODest.Playlists.route) {
                    PlaylistsScreen(
                        playlists = playlists,
                        onPlaylistClick = { id -> navController.navigate(XODest.PlaylistDetail.build(id)) },
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                        contentPadding = bottomAwareContentPadding(innerPadding, showMiniPlayer)
                    )
                }
                composable(XODest.Favorites.route) {
                    FavoritesScreen(
                        favoriteSongs = favoriteSongs,
                        onSongClick = { viewModel.playSong(it, favoriteSongs); navController.navigate(XODest.NowPlaying.route) },
                        onFavoriteClick = { viewModel.toggleFavorite(it.id) },
                        onMoreClick = { song -> songForPlaylistDialog = song },
                        contentPadding = bottomAwareContentPadding(innerPadding, showMiniPlayer)
                    )
                }
                composable(XODest.Settings.route) {
                    SettingsScreen(
                        darkThemeEnabled = darkTheme,
                        onDarkThemeToggle = onDarkThemeToggle,
                        onRescanLibrary = { viewModel.rescanLibrary() },
                        contentPadding = innerPadding
                    )
                }
                composable(XODest.NowPlaying.route) {
                    val isFav = currentSong?.let { favoriteIds.contains(it.id) } ?: false
                    NowPlayingScreen(
                        song = currentSong,
                        isPlaying = isPlaying,
                        isFavorite = isFav,
                        positionMs = position,
                        durationMs = duration,
                        shuffleOn = shuffleOn,
                        repeatMode = repeatMode,
                        onBack = { navController.popBackStack() },
                        onPlayPause = { viewModel.controller.togglePlayPause() },
                        onNext = { viewModel.controller.next() },
                        onPrevious = { viewModel.controller.previous() },
                        onSeek = { viewModel.controller.seekTo(it) },
                        onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
                        onToggleShuffle = { viewModel.controller.toggleShuffle() },
                        onCycleRepeat = { viewModel.controller.cycleRepeat() },
                        onQueueClick = { navController.navigate(XODest.Queue.route) }
                    )
                }
                composable(XODest.Queue.route) {
                    val currentIndex = queue.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)
                    QueueScreen(
                        queue = queue,
                        currentIndex = currentIndex,
                        onPlayIndex = { viewModel.controller.playFromQueueIndex(it) },
                        onRemove = { viewModel.controller.removeFromQueue(it) },
                        onMoveUp = { if (it > 0) viewModel.controller.moveInQueue(it, it - 1) },
                        onMoveDown = { if (it < queue.lastIndex) viewModel.controller.moveInQueue(it, it + 1) },
                        onClearQueue = { viewModel.controller.clearQueue() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = XODest.PlaylistDetail.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                    val playlistSongs by viewModel.playlistSongsFlow(playlistId).collectAsState(initial = emptyList())
                    PlaylistDetailScreen(
                        playlistName = viewModel.playlistName(playlistId),
                        songs = playlistSongs,
                        favoriteIds = favoriteIds,
                        onBack = { navController.popBackStack() },
                        onSongClick = { viewModel.playSong(it, playlistSongs); navController.navigate(XODest.NowPlaying.route) },
                        onFavoriteClick = { viewModel.toggleFavorite(it.id) },
                        onRemoveFromPlaylist = { viewModel.removeSongFromPlaylist(playlistId, it.id) }
                    )
                }
            }

            if (showMiniPlayer && currentSong != null) {
                val song = currentSong!!
                val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                MiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    progress = progress,
                    onClick = { navController.navigate(XODest.NowPlaying.route) },
                    onPlayPause = { viewModel.controller.togglePlayPause() },
                    onNext = { viewModel.controller.next() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = if (showBottomBar) innerPadding.calculateBottomPadding() + 4.dp else 12.dp
                        )
                )
            }
        }
    }

    songForPlaylistDialog?.let { song ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songForPlaylistDialog = null },
            onPlaylistSelected = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song.id)
                songForPlaylistDialog = null
            }
        )
    }
}

@Composable
private fun bottomAwareContentPadding(innerPadding: PaddingValues, showMiniPlayer: Boolean): PaddingValues {
    val extra = if (showMiniPlayer) 78.dp else 0.dp
    return PaddingValues(
        top = innerPadding.calculateTopPadding(),
        bottom = innerPadding.calculateBottomPadding() + extra
    )
}
