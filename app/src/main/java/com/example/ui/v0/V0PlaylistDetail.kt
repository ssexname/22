package com.example.ui.v0

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Song
import com.example.model.SortOption
import com.example.player.NeiroMusicStore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun V0PlaylistDetail(
    playlistId: String,
    store: NeiroMusicStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by store.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val currentSongId by store.currentSongId.collectAsState()

    var sortOption by remember { mutableStateOf(SortOption.MANUAL) }
    var showSortSheet by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var editNameValue by remember(playlist?.name) { mutableStateOf(playlist?.name ?: "") }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showTitleActionControls by remember { mutableStateOf(false) }

    if (playlist == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "플레이리스트를 찾을 수 없습니다", color = Color(0xFF9CA3AF))
        }
        return
    }

    val orderedSongs = remember(playlist.songIds, sortOption, store.songs.collectAsState().value) {
        val list = playlist.songIds.mapNotNull { store.getSong(it) }
        when (sortOption) {
            SortOption.MANUAL -> list
            SortOption.ADDED -> list.sortedBy { it.addedAt }
            SortOption.TITLE -> list.sortedBy { it.title }
            SortOption.ARTIST -> list.sortedBy { it.artist }
        }
    }

    val isManual = sortOption == SortOption.MANUAL

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F6))
                .statusBarsPadding()
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingName) {
                        OutlinedTextField(
                            value = editNameValue,
                            onValueChange = { editNameValue = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        IconButton(
                            onClick = {
                                if (editNameValue.isNotBlank()) {
                                    store.renamePlaylist(playlist.id, editNameValue.trim())
                                }
                                isEditingName = false
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "완료", tint = Color(0xFF10B981))
                        }
                    } else {
                        // Title with long-click support
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = {
                                        showTitleActionControls = !showTitleActionControls
                                    }
                                )
                        ) {
                            Text(
                                text = playlist.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 세로점 (MoreVert) button
                        Box {
                            IconButton(onClick = { showMenuDropdown = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기 메뉴",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenuDropdown,
                                onDismissRequest = { showMenuDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("수정", fontSize = 15.sp, color = Color(0xFF111827)) },
                                    onClick = {
                                        showMenuDropdown = false
                                        editNameValue = playlist.name
                                        isEditingName = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("삭제", fontSize = 15.sp, color = Color(0xFFEF4444)) },
                                    onClick = {
                                        showMenuDropdown = false
                                        store.deletePlaylist(playlist.id)
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "곡 ${playlist.songIds.size}개",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 2.dp)
                )

            Spacer(modifier = Modifier.height(10.dp))

            // Sort Selector and Play/Shuffle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSortSheet = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "정렬",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = sortOption.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(enabled = orderedSongs.isNotEmpty()) {
                                store.shufflePlay(orderedSongs.map { it.id })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "셔플 재생",
                            tint = if (orderedSongs.isNotEmpty()) Color(0xFF111827) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111827))
                            .clickable(enabled = orderedSongs.isNotEmpty()) {
                                store.playQueue(orderedSongs.map { it.id }, 0)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "재생",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isManual) {
                Text(
                    text = "순서를 바꾸려면 '직접 순서'로 전환하세요",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Song List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (orderedSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "곡이 없습니다", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(orderedSongs) { index, song ->
                        V0PlaylistSongRow(
                            index = index + 1,
                            song = song,
                            isPlaying = currentSongId == song.id,
                            isManual = isManual,
                            canMoveUp = index > 0,
                            canMoveDown = index < orderedSongs.size - 1,
                            onPlay = { store.playQueue(orderedSongs.map { it.id }, index) },
                            onMoveUp = { store.reorderPlaylist(playlist.id, index, index - 1) },
                            onMoveDown = { store.reorderPlaylist(playlist.id, index, index + 1) },
                            onRemove = { store.removeSongFromPlaylist(playlist.id, song.id) }
                        )
                        if (index < orderedSongs.size - 1) {
                            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    // Sort Selection Sheet
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "정렬",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SortOption.entries.forEach { option ->
                    val isSelected = option == sortOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                sortOption = option
                                showSortSheet = false
                            }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF111827) else Color(0xFF4B5563)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V0PlaylistSongRow(
    index: Int,
    song: Song,
    isPlaying: Boolean,
    isManual: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index number
        Text(
            text = "$index",
            fontSize = 13.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.size(24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.size(6.dp))

        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.cover)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.size(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPlaying) Color(0xFF111827) else Color(0xFF1F2937),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Up/Down reordering (Manual order only)
        if (isManual) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "위로 이동",
                    tint = if (canMoveUp) Color(0xFF6B7280) else Color(0xFFD1D5DB),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = canMoveUp, onClick = onMoveUp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "아래로 이동",
                    tint = if (canMoveDown) Color(0xFF6B7280) else Color(0xFFD1D5DB),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = canMoveDown, onClick = onMoveDown)
                )
            }
        }

        // Remove from playlist
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "플레이리스트에서 제거",
            tint = Color(0xFF9CA3AF),
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onRemove)
                .padding(6.dp)
        )
    }
}
