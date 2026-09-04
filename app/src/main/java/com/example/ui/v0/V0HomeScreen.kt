package com.example.ui.v0

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AppView
import com.example.model.HomeTab
import com.example.model.Playlist
import com.example.model.Song
import com.example.player.NeiroMusicStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V0HomeScreen(
    store: NeiroMusicStore,
    onMenu: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only "곡" and "플레이리스트" initially active
    val activeTabs = remember {
        mutableStateListOf(HomeTab.SONGS, HomeTab.PLAYLISTS)
    }

    var currentTab by remember { mutableStateOf(HomeTab.SONGS) }
    var showTabSettingsDialog by remember { mutableStateOf(false) }

    // Dropdown menu state for 세로 (...)
    var showSongMenuDropdown by remember { mutableStateOf(false) }
    var showPlaylistMenuDropdown by remember { mutableStateOf(false) }

    // Song Edit Mode State
    var isEditMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<String>() }

    // Dialog state for adding selected songs to playlist
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val songs by store.songs.collectAsState()
    val playlists by store.playlists.collectAsState()
    val currentSongId by store.currentSongId.collectAsState()

    // Safety fallback if active tab was removed
    if (!activeTabs.contains(currentTab) && activeTabs.isNotEmpty()) {
        currentTab = activeTabs.first()
    }

    val isPlaylistTab = currentTab == HomeTab.PLAYLISTS
    val isSongTab = currentTab == HomeTab.SONGS

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F6)) // Warm light neutral
                .statusBarsPadding()
        ) {
            // Top Bar: "뮤직 플레이어" or Edit Mode Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditMode && isSongTab) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            isEditMode = false
                            selectedSongIds.clear()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "편집 닫기",
                                tint = Color(0xFF111827)
                            )
                        }
                        Text(
                            text = if (selectedSongIds.isEmpty()) "곡 선택" else "${selectedSongIds.size}개 선택됨",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                } else {
                    Text(
                        text = "뮤직 플레이어",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        letterSpacing = (-0.5).sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditMode && isSongTab) {
                        // In edit mode: option to cancel or done
                        TextButton(onClick = {
                            isEditMode = false
                            selectedSongIds.clear()
                        }) {
                            Text("완료", fontWeight = FontWeight.Bold, color = Color(0xFF111827), fontSize = 15.sp)
                        }
                    } else {
                        // '+' is ONLY visible on the Playlists tab, placed to the left of 세로점
                        if (isPlaylistTab) {
                            IconButton(
                                onClick = {
                                    val name = "플레이리스트 ${String.format("%03d", playlists.size + 1)}"
                                    val newId = store.createPlaylist(name)
                                    store.navigate(AppView.PlaylistDetail(newId))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "새 플레이리스트",
                                    tint = Color(0xFF111827),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // 세로 (...) Button & Dropdown Menu
                        Box {
                            IconButton(onClick = {
                                if (isSongTab) {
                                    showSongMenuDropdown = true
                                } else {
                                    showPlaylistMenuDropdown = true
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "세로",
                                    tint = Color(0xFF111827),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 세로 메뉴: 곡 탭 (편집, 설정)
                            DropdownMenu(
                                expanded = showSongMenuDropdown && isSongTab,
                                onDismissRequest = { showSongMenuDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Color(0xFF1F2937),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.size(10.dp))
                                            Text("편집", fontSize = 15.sp, color = Color(0xFF111827))
                                        }
                                    },
                                    onClick = {
                                        showSongMenuDropdown = false
                                        isEditMode = true
                                        selectedSongIds.clear()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = Color(0xFF1F2937),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.size(10.dp))
                                            Text("설정", fontSize = 15.sp, color = Color(0xFF111827))
                                        }
                                    },
                                    onClick = {
                                        showSongMenuDropdown = false
                                        showTabSettingsDialog = true
                                    }
                                )
                            }

                            // 세로 메뉴: 플레이리스트 탭 (설정)
                            DropdownMenu(
                                expanded = showPlaylistMenuDropdown && isPlaylistTab,
                                onDismissRequest = { showPlaylistMenuDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = Color(0xFF1F2937),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.size(10.dp))
                                            Text("설정", fontSize = 15.sp, color = Color(0xFF111827))
                                        }
                                    },
                                    onClick = {
                                        showPlaylistMenuDropdown = false
                                        showTabSettingsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Balanced Centered Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeTabs.forEachIndexed { index, tab ->
                    val isSelected = tab == currentTab
                    Text(
                        text = tab.label,
                        fontSize = 17.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF111827) else Color(0xFF9CA3AF),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                currentTab = tab
                                if (isEditMode) {
                                    isEditMode = false
                                    selectedSongIds.clear()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Content Area with rounded top card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (currentTab) {
                    HomeTab.SONGS -> {
                        SongListTabWithScrollbar(
                            songs = songs,
                            currentSongId = currentSongId,
                            isEditMode = isEditMode,
                            selectedSongIds = selectedSongIds,
                            onToggleSelect = { songId ->
                                if (selectedSongIds.contains(songId)) {
                                    selectedSongIds.remove(songId)
                                } else {
                                    selectedSongIds.add(songId)
                                }
                            },
                            onSelectAll = { selectAll ->
                                selectedSongIds.clear()
                                if (selectAll) {
                                    selectedSongIds.addAll(songs.map { it.id })
                                }
                            },
                            onPlaySong = { index -> store.playQueue(songs.map { it.id }, index) },
                            onShuffle = { store.shufflePlay(songs.map { it.id }) },
                            onMenu = onMenu
                        )
                    }
                    HomeTab.PLAYLISTS -> {
                        PlaylistsTab(
                            playlists = playlists,
                            store = store,
                            onOpenPlaylist = { store.navigate(AppView.PlaylistDetail(it)) }
                        )
                    }
                    HomeTab.LIKED -> {
                        val likedSongs = songs.filter { it.liked }
                        SongListTabWithScrollbar(
                            songs = likedSongs,
                            currentSongId = currentSongId,
                            emptyText = "좋아요한 곡이 없습니다",
                            onPlaySong = { index -> store.playQueue(likedSongs.map { it.id }, index) },
                            onShuffle = { store.shufflePlay(likedSongs.map { it.id }) },
                            onMenu = onMenu
                        )
                    }
                    HomeTab.ALBUMS -> {
                        GroupGridTab(
                            songs = songs,
                            groupBy = { it.album },
                            store = store
                        )
                    }
                    HomeTab.ARTISTS -> {
                        GroupGridTab(
                            songs = songs,
                            groupBy = { it.artist },
                            store = store
                        )
                    }
                    HomeTab.GENRES -> {
                        GroupGridTab(
                            songs = songs,
                            groupBy = { it.genre },
                            store = store
                        )
                    }
                }
            }
        }

        // Bottom Selection Action Bar (재생 / 추가 / 삭제)
        // Slid in from bottom ONLY when isEditMode is true AND at least 1 song is checked
        AnimatedVisibility(
            visible = isEditMode && selectedSongIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF1F2937))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 재생
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                store.playQueue(selectedSongIds.toList(), 0)
                                isEditMode = false
                                selectedSongIds.clear()
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "재생",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("재생", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }

                    // 추가 (플레이리스트에 추가)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showAddToPlaylistDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "추가",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("추가", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }

                    // 삭제
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showDeleteConfirmDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("삭제", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("선택한 곡 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("선택한 ${selectedSongIds.size}개의 곡을 목록에서 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.deleteSongs(selectedSongIds.toList())
                        selectedSongIds.clear()
                        isEditMode = false
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("삭제", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("취소", color = Color(0xFF6B7280))
                }
            }
        )
    }

    // Add to Playlist modal sheet
    if (showAddToPlaylistDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAddToPlaylistDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "${selectedSongIds.size}개 곡 플레이리스트에 추가",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("새 플레이리스트 이름", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(
                        enabled = newPlaylistName.isNotBlank(),
                        onClick = {
                            val pid = store.createPlaylist(newPlaylistName.trim())
                            store.addSongsToPlaylist(pid, selectedSongIds)
                            showAddToPlaylistDialog = false
                            isEditMode = false
                            selectedSongIds.clear()
                            newPlaylistName = ""
                        }
                    ) {
                        Text("생성 및 추가", fontWeight = FontWeight.Bold)
                    }
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "플레이리스트가 없습니다",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(playlists) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    store.addSongsToPlaylist(p.id, selectedSongIds)
                                    showAddToPlaylistDialog = false
                                    isEditMode = false
                                    selectedSongIds.clear()
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = Color(0xFF374151),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(14.dp))
                            Text(
                                text = "${p.name} · ${p.songIds.size}곡",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // 설정 -> 탭구성 Modal Sheet
    if (showTabSettingsDialog) {
        ModalBottomSheet(
            onDismissRequest = { showTabSettingsDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "탭구성",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "홈 화면에 표시할 탭을 선택하세요.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HomeTab.entries.forEach { tab ->
                    val isIncluded = activeTabs.contains(tab)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (isIncluded) {
                                    if (activeTabs.size > 1) {
                                        activeTabs.remove(tab)
                                    }
                                } else {
                                    activeTabs.add(tab)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 16.sp,
                            fontWeight = if (isIncluded) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isIncluded) Color(0xFF111827) else Color(0xFF9CA3AF)
                        )

                        if (isIncluded) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF111827)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "선택됨",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE5E7EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "추가",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Enhanced Song List with Interactive Scrollbar & Selection Mode
 * - In Edit Mode: Displays "전체 곡 선택" checkbox and individual checkboxes
 * - Rounded thumb expands & changes color on touch
 * - Direct click / drag to jump to position in the list
 */
@Composable
private fun SongListTabWithScrollbar(
    songs: List<Song>,
    currentSongId: String?,
    onPlaySong: (Int) -> Unit,
    onShuffle: () -> Unit,
    onMenu: (String) -> Unit,
    emptyText: String = "곡이 없습니다",
    isEditMode: Boolean = false,
    selectedSongIds: List<String> = emptyList(),
    onToggleSelect: ((String) -> Unit)? = null,
    onSelectAll: ((Boolean) -> Unit)? = null
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emptyText, fontSize = 14.sp, color = Color(0xFF9CA3AF))
        }
        return
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var isInteractingWithScrollbar by remember { mutableStateOf(false) }

    val allSelected = songs.isNotEmpty() && selectedSongIds.size == songs.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Top row in list tab: If in edit mode, show "전체 곡 선택" checkbox row; otherwise Shuffle / Play
        if (isEditMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onSelectAll?.invoke(!allSelected)
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (allSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF111827)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "전체 선택됨",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE5E7EB))
                        )
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "전체 곡 선택",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${selectedSongIds.size}/${songs.size}",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
        } else {
            // Action Buttons: Shuffle and Play
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable(onClick = onShuffle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "셔플 재생",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111827))
                        .clickable { onPlaySong(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "재생",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // List Area with Custom Scrollbar
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalItems = songs.size

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 14.dp)
            ) {
                itemsIndexed(songs) { index, song ->
                    V0SongRow(
                        song = song,
                        isPlaying = currentSongId == song.id,
                        onPlay = { onPlaySong(index) },
                        onMenu = { onMenu(song.id) },
                        isSelectionMode = isEditMode,
                        isSelected = selectedSongIds.contains(song.id),
                        onToggleSelect = { onToggleSelect?.invoke(song.id) }
                    )
                    if (index < songs.size - 1) {
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 0.5.dp)
                    }
                }
            }

            // Interactive Scrollbar Track & Thumb
            if (totalItems > 5) {
                val firstVisibleItem = remember { derivedStateOf { listState.firstVisibleItemIndex } }

                val progress = remember(totalItems) {
                    derivedStateOf {
                        val maxScrollIndex = (totalItems - 1).coerceAtLeast(1)
                        (firstVisibleItem.value.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
                    }
                }

                val thumbWidth by animateDpAsState(
                    targetValue = if (isInteractingWithScrollbar) 10.dp else 4.5.dp,
                    label = "ThumbWidth"
                )

                val thumbColor = if (isInteractingWithScrollbar) Color(0xFF111827) else Color(0xFFD1D5DB)

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(28.dp)
                        .fillMaxHeight()
                        .pointerInput(totalItems) {
                            detectTapGestures(
                                onPress = { offset ->
                                    isInteractingWithScrollbar = true
                                    val fraction = (offset.y / size.height).coerceIn(0f, 1f)
                                    val targetIndex = (fraction * (totalItems - 1)).roundToInt()
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                    tryAwaitRelease()
                                    isInteractingWithScrollbar = false
                                }
                            )
                        }
                        .pointerInput(totalItems) {
                            detectDragGestures(
                                onDragStart = { isInteractingWithScrollbar = true },
                                onDragEnd = { isInteractingWithScrollbar = false },
                                onDragCancel = { isInteractingWithScrollbar = false },
                                onDrag = { change, _ ->
                                    val fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                                    val targetIndex = (fraction * (totalItems - 1)).roundToInt()
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                            )
                        }
                ) {
                    // Track Line
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFF3F4F6))
                    )

                    // Scrollbar Thumb
                    BoxWithConstraints(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxHeight()
                    ) {
                        val availableTrack = maxHeight - 36.dp
                        val topOffset = availableTrack * progress.value

                        Box(
                            modifier = Modifier
                                .offset(y = topOffset)
                                .align(Alignment.TopCenter)
                                .size(width = thumbWidth, height = 36.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(thumbColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    store: NeiroMusicStore,
    onOpenPlaylist: (String) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "플레이리스트가 없습니다", fontSize = 14.sp, color = Color(0xFF9CA3AF))
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(playlists) { index, playlist ->
            val firstSong = playlist.songIds.firstOrNull()?.let { store.getSong(it) }
            val coverUrl = firstSong?.cover ?: "file:///android_asset/albums/cover-pink.png"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(playlist.id) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.size(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "곡 ${playlist.songIds.size}개",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                // Play triangle button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable(enabled = playlist.songIds.isNotEmpty()) {
                            store.playQueue(playlist.songIds, 0)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "재생",
                        tint = if (playlist.songIds.isNotEmpty()) Color(0xFF111827) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (index < playlists.size - 1) {
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun GroupGridTab(
    songs: List<Song>,
    groupBy: (Song) -> String,
    store: NeiroMusicStore
) {
    val groups = songs.groupBy(groupBy).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(groups) { (groupName, groupSongs) ->
            val firstCover = groupSongs.firstOrNull()?.cover ?: "file:///android_asset/albums/cover-pink.png"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        store.playQueue(groupSongs.map { it.id }, 0)
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(firstCover)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = groupName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "곡 ${groupSongs.size}개",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
