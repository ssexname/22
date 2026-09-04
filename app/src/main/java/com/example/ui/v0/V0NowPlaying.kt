package com.example.ui.v0

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.player.NeiroMusicStore
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun V0NowPlaying(
    store: NeiroMusicStore,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSongId by store.currentSongId.collectAsState()
    val isPlaying by store.isPlaying.collectAsState()
    val progress by store.progress.collectAsState()
    val albumColor by store.albumColor.collectAsState()

    val song = currentSongId?.let { store.getSong(it) } ?: return

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var showLyrics by remember { mutableStateOf(false) }

    val currentSeconds = if (isDraggingSlider) sliderPosition else progress
    val durationSeconds = if (song.duration > 0) song.duration.toFloat() else 1f

    val textColor = if (albumColor.isDark) Color.White else Color(0xFF111827)
    val subColor = textColor.copy(alpha = 0.72f)

    // Swipe down on the screen to dismiss
    var offsetY by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        val newOffset = offsetY + delta
        if (newOffset >= 0) {
            offsetY = newOffset
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (offsetY > 150f || velocity > 800f) {
                        onClose()
                    }
                    offsetY = 0f
                }
            )
            .background(
                Brush.verticalGradient(
                    colors = if (albumColor.palette.size >= 3) {
                        albumColor.palette
                    } else {
                        listOf(
                            albumColor.gradientTop,
                            albumColor.gradientMid,
                            albumColor.gradientBottom
                        )
                    }
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clean Top Pill Bar (visual affordance for drag-down to close)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(textColor.copy(alpha = 0.35f))
                )
            }

            Spacer(modifier = Modifier.weight(0.12f))

            // Main Visual Area: Toggle between Album Art & Lyrics View
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "CoverLyricsToggle"
            ) { lyricsVisible ->
                if (lyricsVisible) {
                    // Clean Lyrics Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (albumColor.isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f))
                            .clickable { showLyrics = false }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "가사",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = subColor,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "${song.title} - ${song.artist}\n\n" +
                                        "거리마다 울려 퍼지는 멜로디\n" +
                                        "너와 함께 듣던 그 노래처럼\n" +
                                        "마음 깊은 곳에 남은 기억들\n" +
                                        "오늘도 난 너를 떠올려\n\n" +
                                        "화면을 다시 탭하면\n" +
                                        "앨범 커버 화면으로 전환됩니다.",
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Album Artwork with rich elevation and rounded corners
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.cover)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${song.album} 앨범 커버",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(elevation = 18.dp, shape = RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { showLyrics = true }
                        )

                        // Floating Lyrics Badge on Artwork
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { showLyrics = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Lyrics,
                                    contentDescription = "가사",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "가사",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.18f))

            // Title and Artist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = song.artist,
                    fontSize = 16.sp,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Progress Slider and Timestamps
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Slider(
                    value = currentSeconds.coerceIn(0f, durationSeconds),
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        store.seek(sliderPosition)
                        isDraggingSlider = false
                    },
                    valueRange = 0f..durationSeconds,
                    colors = SliderDefaults.colors(
                        thumbColor = if (albumColor.isDark) Color.White else Color(0xFF111827),
                        activeTrackColor = if (albumColor.isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF111827),
                        inactiveTrackColor = if (albumColor.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentSeconds.toInt()),
                        fontSize = 12.sp,
                        color = subColor
                    )
                    Text(
                        text = formatTime(song.duration),
                        fontSize = 12.sp,
                        color = subColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Clean Playback Controls: Shuffle, Prev, Big Play/Pause, Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val q = store.queue.value
                    if (q.isNotEmpty()) store.shufflePlay(q)
                }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "셔플",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { store.prev() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "이전 곡",
                        tint = textColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Center Play/Pause button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { store.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "일시정지" else "재생",
                        tint = textColor,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = { store.next() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "다음 곡",
                        tint = textColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { /* Repeat toggle */ }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "반복",
                        tint = textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.12f))
        }
    }
}

private fun formatTime(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, r)
}

