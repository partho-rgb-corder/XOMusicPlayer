package com.xoplayer.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    darkThemeEnabled: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
    onRescanLibrary: () -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp)
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.DarkMode,
                title = "Dark theme",
                subtitle = "Match XOPlayer's premium green theme"
            ) {
                Switch(checked = darkThemeEnabled, onCheckedChange = onDarkThemeToggle)
            }
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Refresh,
                title = "Rescan library",
                subtitle = "Look for new local audio files",
                onClick = onRescanLibrary
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Tune,
                title = "Playback",
                subtitle = "Gapless behavior, audio focus handling"
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.LibraryMusic,
                title = "Library",
                subtitle = "Supported formats: MP3, M4A, AAC, WAV, FLAC, OGG"
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "About XOPlayer",
                subtitle = "Version 1.0 · Fully offline music player"
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}
