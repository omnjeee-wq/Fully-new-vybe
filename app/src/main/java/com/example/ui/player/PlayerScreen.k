package com.example.ui.player

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import com.example.ui.theme.GlowOrange
import com.example.ui.theme.GlowPink
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.neonGlow
import com.example.ui.theme.pulsingGlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Share
import com.example.VybeApp
import kotlinx.coroutines.flow.flowOf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.Song
import com.example.player.PlayerManager
import com.example.player.PlayerState
import com.example.ui.components.EqualizerWaveformIndicator
import com.example.ui.theme.VybeBlack
import com.example.ui.theme.VybeBorder
import com.example.ui.theme.VybeCoral
import com.example.ui.theme.VybeElectricCyan
import com.example.ui.theme.VybeNearBlack
import com.example.ui.theme.VybeOrange
import com.example.ui.theme.VybePink
import com.example.ui.theme.VybeSurface
import com.example.ui.theme.VybeSurfaceVariant
import com.example.ui.theme.VybeTextMuted
import com.example.ui.theme.VybeTextPrimary
import com.example.ui.theme.VybeTextSecondary
import com.example.util.MusicUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerManager: PlayerManager,
    onCollapse: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerState by playerManager.playerState.collectAsStateWithLifecycle()
    val currentSong = playerState.currentSong
    LaunchedEffect(currentSong?.id) {

    val songId = currentSong?.id ?: return@LaunchedEffect

    lyricsLoading = true
    lyricsError = null
    lyricsText = null

    try {
        val result = withContext(Dispatchers.IO) {

            val url = URL(
                "https://vybe.vyoma-apps.workers.dev/api/songs/{SONG_ID}/lyrics"
            )

            val connection = url.openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            val response = connection
                .getInputStream()
                .bufferedReader()
                .use { it.readText() }

            val json = JSONObject(response)

            if (!json.optBoolean("success")) {
                throw Exception("Lyrics not available")
            }

            val data = json.opt("data")

            when (data) {

                is JSONObject -> {
                    data.optString("lyrics")
                }

                is String -> {
                    data
                }

                else -> {
                    ""
                }
            }
        }

        lyricsText = result.ifBlank {
            "Lyrics are not available for this song."
        }

    } catch (e: Exception) {

        lyricsError = "Unable to load lyrics"

    } finally {

        lyricsLoading = false
    }
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var lyricsText by remember { mutableStateOf<String?>(null) }
var lyricsLoading by remember { mutableStateOf(false) }
var lyricsError by remember { mutableStateOf<String?>(null) }

    val isDownloaded by (currentSong?.let { VybeApp.instance.repository.isSongDownloaded(it.id) } ?: flowOf(false))
        .collectAsStateWithLifecycle(false)
    val userPlaylists by VybeApp.instance.repository.getUserPlaylists()
        .collectAsStateWithLifecycle(emptyList())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Slider dragging state
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VybeBlack)
            .testTag("full_player_screen")
    ) {
        // Atmospheric Glow Background from Song Artwork
        if (currentSong != null && currentSong.artworkUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(70.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentSong.artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Dark gradient overlay to keep it subtle & readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xEB0A0A0E),
                            0.4f to Color(0xD90A0A0E),
                            1.0f to Color(0xFA0A0A0E)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x22FFFFFF), CircleShape)
                        .testTag("collapse_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM",
                        color = VybeTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = playerState.playlistName,
                        color = VybeTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                            .testTag("player_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(VybeSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share Track", color = VybeTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Share, contentDescription = null, tint = VybeOrange)
                            },
                            onClick = {
                                showMenu = false
                                currentSong?.let { shareSong(context, it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = VybeTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = VybeOrange)
                            },
                            onClick = {
                                showMenu = false
                                showAddToPlaylistSheet = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isDownloaded) "Downloaded" else "Download Track", color = VybeTextPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = VybeElectricCyan
                                )
                            },
                            onClick = {
                                showMenu = false
                                currentSong?.let { song ->
                                    scope.launch {
                                        VybeApp.instance.repository.toggleDownloadSong(song)
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View Queue", color = VybeTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = VybeTextSecondary)
                            },
                            onClick = {
                                showMenu = false
                                showQueueSheet = true
                            }
                        )
                    }
                }
            }

            // CENTER ARTWORK
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .aspectRatio(1f)
                    .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = VybeOrange.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(VybeSurfaceVariant)
                    .testTag("player_artwork")
            ) {
                if (currentSong != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentSong.artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = currentSong.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = VybeTextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Buffering indicator
                if (playerState.isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VybeOrange)
                    }
                }
            }

            // TRACK METADATA & FAVORITE BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentSong?.title ?: "No Track Selected",
                        color = VybeTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.artist ?: "Vybe Music",
                        color = VybeTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { currentSong?.let { onToggleFavorite(it) } },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("player_favorite_button")
                ) {
                    Icon(
                        imageVector = if (currentSong?.isLiked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentSong?.isLiked == true) VybeCoral else VybeTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // YOUTUBE STYLE SLEEK TIMELINE & DURATION TIMESTAMPS
            YouTubePlayerTimeline(
                progress = if (isSeeking) seekProgress else playerState.progress,
                durationMs = playerState.durationMs,
                currentPositionMs = playerState.currentPositionMs,
                isSeeking = isSeeking,
                onSeekStart = {
                    isSeeking = true
                    seekProgress = playerState.progress
                },
                onSeekProgress = { fraction ->
                    seekProgress = fraction
                },
                onSeekFinished = { targetMs ->
                    playerManager.seekTo(targetMs)
                    isSeeking = false
                }
            )

            // MAIN PLAYBACK CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = { playerManager.toggleShuffle() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.isShuffleEnabled) VybeOrange else VybeTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { playerManager.previous() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Main Play/Pause Button with Pulsing Glow
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .pulsingGlow(
                            color = VybePink,
                            minRadius = 10.dp,
                            maxRadius = 22.dp,
                            minAlpha = 0.35f,
                            maxAlpha = 0.85f
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(VybePink, VybeOrange)
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { playerManager.togglePlayPause() }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { playerManager.next() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode
                IconButton(
                    onClick = { playerManager.cycleRepeatMode() },
                    modifier = Modifier.size(40.dp)
                ) {
                    val icon = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    }
                    val tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) VybeOrange else VybeTextMuted
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // BOTTOM ACTION ROW: LYRICS & QUEUE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VybeSurface)
                        .clickable { showLyricsSheet = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("lyrics_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lyrics,
                        contentDescription = "Lyrics",
                        tint = VybeElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Lyrics",
                        color = VybeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Queue button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VybeSurface)
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("queue_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = VybeOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Queue (${playerState.queue.size})",
                        color = VybeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // QUEUE BOTTOM SHEET
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = VybeNearBlack
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Now Playing & Queue",
                            color = VybeTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showQueueSheet = false }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = VybeTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(playerState.queue) { index, song ->
                            val isCurrent = index == playerState.currentIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) Color(0x1FFF6436) else VybeSurface)
                                    .clickable {
                                        playerManager.playSong(song, playerState.queue, playerState.playlistName)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(VybeSurfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(song.artworkUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) VybeOrange else VybeTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = VybeTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isCurrent && playerState.isPlaying) {
                                    EqualizerWaveformIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = VybeOrange
                                    )
                                } else {
                                    IconButton(
                                        onClick = { playerManager.removeFromQueue(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = VybeTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // LYRICS BOTTOM SHEET
        if (showLyricsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLyricsSheet = false },
                sheetState = sheetState,
                containerColor = VybeNearBlack
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lyrics",
                        color = VybeTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${currentSong?.title} — ${currentSong?.artist}",
                        color = VybeTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    

             LazyColumn(
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    item {
                    when {

    lyricsLoading -> {

        CircularProgressIndicator(
            color = VybeOrange,
            modifier = Modifier.size(32.dp)
        )
    }

    lyricsError != null -> {

        Text(
            text = lyricsError ?: "Lyrics unavailable",
            color = VybeTextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }

    !lyricsText.isNullOrBlank() -> {

        Text(
            text = lyricsText!!,
            color = VybeTextPrimary,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center
        )
    }

    else -> {

        Text(
            text = "Lyrics are not available for this song.",
            color = VybeTextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Add to Playlist Sheet
        if (showAddToPlaylistSheet && currentSong != null) {
            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistSheet = false },
                containerColor = VybeSurface,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add to Playlist",
                            color = VybeTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showAddToPlaylistSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = VybeTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (userPlaylists.isEmpty()) {
                        Text(
                            text = "No custom playlists found. Go to Library -> Playlists -> My Playlist to create one!",
                            color = VybeTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = userPlaylists,
                                key = { it.id }
                            ) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(VybeSurfaceVariant)
                                        .clickable {
                                            scope.launch {
                                                VybeApp.instance.repository.addSongToUserPlaylist(playlist.id, currentSong)
                                                showAddToPlaylistSheet = false
                                            }
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = VybeOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = playlist.title,
                                        color = VybeTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareSong(context: Context, song: Song) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            "🎵 Listening to \"${song.title}\" by ${song.artist} on VYBE!\n\nDownload VYBE: https://t.me/VyomaOfficial"
        )
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Track via"))
}

@Composable
fun YouTubePlayerTimeline(
    progress: Float,
    durationMs: Long,
    currentPositionMs: Long,
    isSeeking: Boolean,
    onSeekStart: () -> Unit,
    onSeekProgress: (Float) -> Unit,
    onSeekFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedCurrentMs = if (isSeeking) {
        (progress * durationMs).toLong()
    } else {
        currentPositionMs
    }

    // YouTube circle scrubber size: small and sleek (4.5dp radius = 9dp diameter), expands when scrubbing
    val animatedThumbRadius by animateDpAsState(
        targetValue = if (isSeeking) 7.5.dp else 4.5.dp,
        label = "thumb_radius"
    )
    // Thin and sleek timeline bar: 2dp height, expands slightly to 3.5dp when scrubbing
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isSeeking) 3.5.dp else 2.dp,
        label = "track_height"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_seekbar")
    ) {
        // Touch target container (height 28.dp for sleek footprint, centered)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(durationMs) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width.toFloat()
                            if (width > 0) {
                                val fraction = (offset.x / width).coerceIn(0f, 1f)
                                onSeekStart()
                                onSeekProgress(fraction)
                                val success = tryAwaitRelease()
                                if (success) {
                                    val targetMs = (fraction * durationMs).toLong()
                                    onSeekFinished(targetMs)
                                }
                            }
                        }
                    )
                }
                .pointerInput(durationMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            if (width > 0) {
                                onSeekStart()
                                val fraction = (offset.x / width).coerceIn(0f, 1f)
                                onSeekProgress(fraction)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val width = size.width.toFloat()
                            if (width > 0) {
                                val fraction = (change.position.x / width).coerceIn(0f, 1f)
                                onSeekProgress(fraction)
                            }
                        },
                        onDragEnd = {
                            val targetMs = (progress * durationMs).toLong()
                            onSeekFinished(targetMs)
                        },
                        onDragCancel = {
                            val targetMs = (progress * durationMs).toLong()
                            onSeekFinished(targetMs)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val safeProgress = progress.coerceIn(0f, 1f)
            val thumbXOffsetPx = totalWidthPx * safeProgress
            val density = LocalDensity.current
            val thumbOffsetDp = with(density) { thumbXOffsetPx.toDp() }
            val maxThumbOffsetDp = with(density) { (totalWidthPx - animatedThumbRadius.toPx() * 2).coerceAtLeast(0f).toDp() }

            // Inactive Track (sleek thin translucent background bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTrackHeight)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            )

            // Active Track (sleek gradient progress line)
            Box(
                modifier = Modifier
                    .width(thumbOffsetDp)
                    .height(animatedTrackHeight)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(VybePink, VybeOrange)
                        )
                    )
            )

            // YouTube-style front circular scrubber dot with glowing aura
            Box(
                modifier = Modifier
                    .offset(x = (thumbOffsetDp - animatedThumbRadius).coerceIn(0.dp, maxThumbOffsetDp))
                    .size(animatedThumbRadius * 2)
                    .neonGlow(
                        color = GlowOrange,
                        radius = if (isSeeking) 14.dp else 6.dp,
                        alpha = if (isSeeking) 0.85f else 0.55f
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = if (isSeeking) 2.dp else 1.5.dp,
                        color = Color(0xFFFF5E3A),
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Timestamps below the timeline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MusicUtils.formatDurationMs(displayedCurrentMs),
                color = if (isSeeking) VybeOrange else VybeTextSecondary,
                fontSize = 12.sp,
                fontFamily = OutfitFontFamily,
                fontWeight = if (isSeeking) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = MusicUtils.formatDurationMs(durationMs),
                color = VybeTextMuted,
                fontSize = 12.sp,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
