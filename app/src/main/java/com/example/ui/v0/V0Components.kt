package com.example.ui.v0

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AppView
import com.example.model.ExtractedAlbumColor
import com.example.model.Song
import com.example.player.NeiroMusicStore

@Composable
fun V0BottomNav(
    currentView: AppView,
    canGoBack: Boolean,
    onNavigate: (AppView) -> Unit,
    onBack: () -> Unit,
    actionControls: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSearch = currentView is AppView.Search
    val isHome = currentView is AppView.Home

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF8F5))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            color = Color(0xFFE8E5DF),
            thickness = 0.5.dp
        )

        if (actionControls != null) {
            actionControls()
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                V0NavButton(
                    icon = Icons.Default.Search,
                    label = "검색하기",
                    active = isSearch,
                    onClick = { onNavigate(AppView.Search) }
                )
                V0NavButton(
                    icon = Icons.Default.Home,
                    label = "홈",
                    active = isHome,
                    onClick = { onNavigate(AppView.Home) }
                )
                V0NavButton(
                    icon = Icons.Default.ChevronLeft,
                    label = "뒤로가기",
                    active = false,
                    disabled = !canGoBack,
                    onClick = onBack
                )
            }
        }
    }
}

@Composable
fun V0ActionButtons(
    onPlay: () -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onRename: (() -> Unit)? = null,
    disabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        V0NavButton(
            icon = Icons.Default.PlayArrow,
            label = "재생",
            active = false,
            disabled = disabled,
            onClick = onPlay
        )
        V0NavButton(
            icon = Icons.Default.PlaylistAdd,
            label = "추가",
            active = false,
            disabled = disabled,
            onClick = onAdd
        )
        V0NavButton(
            icon = Icons.Default.Delete,
            label = "삭제",
            active = false,
            disabled = disabled,
            customColor = if (disabled) null else Color(0xFFEF4444),
            onClick = onDelete
        )
        if (onRename != null) {
            V0NavButton(
                icon = Icons.Default.DriveFileRenameOutline,
                label = "이름변경",
                active = false,
                disabled = disabled,
                onClick = onRename
            )
        }
    }
}

/**
 * Action box for selection or long-press options.
 * Placed above the bottom nav (below mini-player), matching the ivory background of the nav.
 */
@Composable
fun V0ActionControls(
    onPlay: () -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onRename: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF8F5))
    ) {
        HorizontalDivider(
            color = Color(0xFFE8E5DF),
            thickness = 0.5.dp
        )
        V0ActionButtons(
            onPlay = onPlay,
            onAdd = onAdd,
            onDelete = onDelete,
            onRename = onRename
        )
    }
}

@Composable
private fun V0NavButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    disabled: Boolean = false,
    customColor: Color? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !disabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val tint = when {
                customColor != null -> customColor
                disabled -> Color(0xFF9CA3AF).copy(alpha = 0.35f)
                active -> Color(0xFF111827) // Primary dark
                else -> Color(0xFF6B7280) // Muted gray
            }

            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = tint
            )
        }
    }
}

@Composable
fun V0MiniPlayer(
    store: NeiroMusicStore,
    song: Song?,
    isPlaying: Boolean,
    progress: Float,
    albumColor: ExtractedAlbumColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    val duration = if (song.duration > 0) song.duration.toFloat() else 1f
    val pct = (progress / duration).coerceIn(0f, 1f)

    val textColor = if (albumColor.isDark) Color.White else Color(0xFF111827)
    val subColor = textColor.copy(alpha = 0.75f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = if (albumColor.palette.size >= 3) {
                        albumColor.palette
                    } else {
                        listOf(albumColor.gradientTop, albumColor.gradientMid, albumColor.gradientBottom)
                    }
                )
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round album cover
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.cover)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.size(10.dp))

                // Song info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontSize = 12.sp,
                        color = subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls: Prev, Play/Pause, Next, ListMusic
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "이전 곡",
                        tint = textColor,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { store.prev() }
                            .padding(2.dp)
                    )

                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "일시정지" else "재생",
                        tint = textColor,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { store.togglePlay() }
                            .padding(2.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "다음 곡",
                        tint = textColor,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { store.next() }
                            .padding(2.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "재생화면",
                        tint = textColor,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { store.navigate(AppView.NowPlaying) }
                            .padding(2.dp)
                    )
                }
            }

            // Progress bar at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color.Black.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = pct)
                        .height(2.5.dp)
                        .background(if (albumColor.isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f))
                )
            }
        }
    }
}
