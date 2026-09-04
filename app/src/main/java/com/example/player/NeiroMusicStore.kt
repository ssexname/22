package com.example.player

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.model.AlbumColorExtractor
import com.example.model.AppView
import com.example.model.ExtractedAlbumColor
import com.example.model.Playlist
import com.example.model.SampleData
import com.example.model.Song
import com.example.model.SortOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NeiroMusicStore(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    // 1. Data State
    private val _songs = MutableStateFlow<List<Song>>(SampleData.songs)
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(SampleData.playlists)
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // 2. Playback State
    private val _currentSongId = MutableStateFlow<String?>("s1")
    val currentSongId: StateFlow<String?> = _currentSongId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _queue = MutableStateFlow<List<String>>(SampleData.songs.map { it.id })
    val queue: StateFlow<List<String>> = _queue.asStateFlow()

    // 3. Navigation Stack State
    private val _viewStack = MutableStateFlow<List<AppView>>(listOf(AppView.Home))
    val currentView: StateFlow<AppView> = MutableStateFlow(AppView.Home)

    // Current Album Color
    private val _albumColor = MutableStateFlow(AlbumColorExtractor.DEFAULT)
    val albumColor: StateFlow<ExtractedAlbumColor> = _albumColor.asStateFlow()

    init {
        startProgressTracker()
        updateCurrentSongColor("s1")
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(200)
                if (_isPlaying.value) {
                    val current = getSong(_currentSongId.value ?: "")
                    if (current != null) {
                        val duration = current.duration.toFloat()
                        val nextProg = _progress.value + 0.2f
                        if (nextProg >= duration) {
                            next()
                        } else {
                            _progress.value = nextProg
                        }
                    }
                }
            }
        }
    }

    fun getSong(id: String): Song? = _songs.value.find { it.id == id }

    fun getPlaylist(id: String): Playlist? = _playlists.value.find { it.id == id }

    fun toggleLike(id: String) {
        _songs.update { list ->
            list.map { if (it.id == id) it.copy(liked = !it.liked) else it }
        }
    }

    fun createPlaylist(name: String): String {
        val id = "p${System.currentTimeMillis()}"
        val newPlaylist = Playlist(id = id, name = name, songIds = emptyList(), createdAt = System.currentTimeMillis())
        _playlists.update { it + newPlaylist }
        return id
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId && !p.songIds.contains(songId)) {
                    p.copy(songIds = p.songIds + songId)
                } else {
                    p
                }
            }
        }
    }

    fun addSongsToPlaylist(playlistId: String, songIds: Collection<String>) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId) {
                    val newIds = (p.songIds + songIds).distinct()
                    p.copy(songIds = newIds)
                } else {
                    p
                }
            }
        }
    }

    fun deleteSongs(songIds: Collection<String>) {
        val set = songIds.toSet()
        _songs.update { list -> list.filterNot { it.id in set } }
        _playlists.update { list ->
            list.map { p -> p.copy(songIds = p.songIds.filterNot { it in set }) }
        }
        _queue.update { list -> list.filterNot { it in set } }
        if (_currentSongId.value in set) {
            val remaining = _queue.value
            if (remaining.isNotEmpty()) {
                _currentSongId.value = remaining.first()
                updateCurrentSongColor(remaining.first())
            } else {
                _currentSongId.value = null
                _isPlaying.value = false
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId) {
                    p.copy(songIds = p.songIds - songId)
                } else {
                    p
                }
            }
        }
    }

    fun reorderPlaylist(playlistId: String, from: Int, to: Int) {
        _playlists.update { list ->
            list.map { p ->
                if (p.id == playlistId) {
                    val ids = p.songIds.toMutableList()
                    if (from in ids.indices && to in ids.indices) {
                        val item = ids.removeAt(from)
                        ids.add(to, item)
                        p.copy(songIds = ids)
                    } else {
                        p
                    }
                } else {
                    p
                }
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        _playlists.update { list ->
            list.map { if (it.id == playlistId) it.copy(name = newName) else it }
        }
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.update { list -> list.filter { it.id != playlistId } }
    }

    fun playQueue(songIds: List<String>, startIndex: Int) {
        if (songIds.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, songIds.size - 1)
        _queue.value = songIds
        val songId = songIds[validIndex]
        _currentSongId.value = songId
        _progress.value = 0f
        _isPlaying.value = true
        updateCurrentSongColor(songId)
    }

    fun togglePlay() {
        if (_currentSongId.value == null && _songs.value.isNotEmpty()) {
            playQueue(_songs.value.map { it.id }, 0)
            return
        }
        _isPlaying.update { !it }
    }

    fun next() {
        val q = _queue.value
        val currentId = _currentSongId.value ?: return
        if (q.isEmpty()) return
        val idx = q.indexOf(currentId)
        val nextIdx = (idx + 1) % q.size
        val nextId = q[nextIdx]
        _currentSongId.value = nextId
        _progress.value = 0f
        _isPlaying.value = true
        updateCurrentSongColor(nextId)
    }

    fun prev() {
        val q = _queue.value
        val currentId = _currentSongId.value ?: return
        if (q.isEmpty()) return
        if (_progress.value > 3f) {
            _progress.value = 0f
            return
        }
        val idx = q.indexOf(currentId)
        val prevIdx = if (idx <= 0) q.size - 1 else idx - 1
        val prevId = q[prevIdx]
        _currentSongId.value = prevId
        _progress.value = 0f
        _isPlaying.value = true
        updateCurrentSongColor(prevId)
    }

    fun seek(seconds: Float) {
        _progress.value = seconds.coerceAtLeast(0f)
    }

    fun shufflePlay(songIds: List<String>) {
        if (songIds.isEmpty()) return
        val shuffled = songIds.shuffled()
        playQueue(shuffled, 0)
    }

    private fun updateCurrentSongColor(songId: String) {
        val s = getSong(songId) ?: return
        scope.launch {
            val color = AlbumColorExtractor.extractFromAsset(context, s.cover)
            _albumColor.value = color
        }
    }

    // Navigation Stack
    val viewStack: StateFlow<List<AppView>> = _viewStack.asStateFlow()

    fun navigate(view: AppView) {
        _viewStack.update { it + view }
    }

    fun back() {
        _viewStack.update { if (it.size > 1) it.dropLast(1) else it }
    }

    fun canGoBack(): Boolean = _viewStack.value.size > 1
}
