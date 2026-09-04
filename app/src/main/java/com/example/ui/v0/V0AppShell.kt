package com.example.ui.v0

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.model.AppView
import com.example.player.NeiroMusicStore

@Composable
fun V0AppShell(
    store: NeiroMusicStore,
    modifier: Modifier = Modifier
) {
    val viewStack by store.viewStack.collectAsState()
    val currentView = viewStack.lastOrNull() ?: AppView.Home
    val baseView = viewStack.findLast { it !is AppView.NowPlaying } ?: AppView.Home
    val canGoBack = store.canGoBack()

    val currentSongId by store.currentSongId.collectAsState()
    val isPlaying by store.isPlaying.collectAsState()
    val progress by store.progress.collectAsState()
    val albumColor by store.albumColor.collectAsState()
    val currentSong = currentSongId?.let { store.getSong(it) }

    var menuSongId by remember { mutableStateOf<String?>(null) }
    val isNowPlayingOpen = currentView is AppView.NowPlaying

    BackHandler(enabled = canGoBack) {
        store.back()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                when (baseView) {
                    is AppView.Home -> {
                        V0HomeScreen(
                            store = store,
                            onMenu = { menuSongId = it }
                        )
                    }
                    is AppView.Search -> {
                        V0SearchScreen(
                            store = store,
                            onMenu = { menuSongId = it }
                        )
                    }
                    is AppView.PlaylistDetail -> {
                        V0PlaylistDetail(
                            playlistId = baseView.playlistId,
                            store = store,
                            onBack = { store.back() }
                        )
                    }
                    else -> Unit
                }
            }

            // Mini Player (Only shown when not in NowPlaying screen and a song is selected)
            if (currentSong != null && !isNowPlayingOpen) {
                V0MiniPlayer(
                    store = store,
                    song = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    albumColor = albumColor,
                    onClick = { store.navigate(AppView.NowPlaying) }
                )
            }

            // Bottom Navigation: [검색하기] [홈] [뒤로가기]
            V0BottomNav(
                currentView = baseView,
                canGoBack = canGoBack,
                onNavigate = { store.navigate(it) },
                onBack = { store.back() }
            )
        }

        // Fullscreen Now Playing Overlay with Slide Animation & Dynamic Album Gradient
        AnimatedVisibility(
            visible = isNowPlayingOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            V0NowPlaying(
                store = store,
                onClose = { store.back() }
            )
        }

        // Song Action Sheet (Play / Like / Add to Playlist / New Playlist)
        V0SongActionSheet(
            songId = menuSongId,
            store = store,
            onDismiss = { menuSongId = null }
        )
    }
}
