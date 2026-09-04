package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "K-POP",
    val duration: Int = 180, // 초 단위
    val cover: String = "file:///android_asset/albums/cover-pink.png",
    val liked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val streamUrl: String? = null
)

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class HomeTab(val label: String, val canRemove: Boolean = true) {
    SONGS("곡", canRemove = false),
    PLAYLISTS("플레이리스트", canRemove = false),
    LIKED("좋아요", canRemove = true),
    ALBUMS("앨범", canRemove = true),
    ARTISTS("아티스트", canRemove = true),
    GENRES("장르", canRemove = true)
}

enum class SortOption(val label: String) {
    MANUAL("직접 순서"),
    ADDED("추가순"),
    TITLE("이름순"),
    ARTIST("아티스트순")
}

sealed interface AppView {
    data object Home : AppView
    data object Search : AppView
    data class PlaylistDetail(val playlistId: String) : AppView
    data object NowPlaying : AppView
}
