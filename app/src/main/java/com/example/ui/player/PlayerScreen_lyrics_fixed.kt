package com.example.ui.player

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.VybeApp
import com.example.domain.model.Song
import com.example.player.PlayerManager
import com.example.ui.components.EqualizerWaveformIndicator
import com.example.ui.theme.*
import com.example.util.MusicUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
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

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    // Lyrics state MUST be declared before LaunchedEffect.
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var lyricsError by remember { mutableStateOf<String?>(null) }

    // Fetch lyrics whenever the current song changes.
    LaunchedEffect(currentSong?.id) {
        val songId = currentSong?.id
        if (songId.isNullOrBlank()) {
            lyricsText = null
            lyricsError = null
            lyricsLoading = false
            return@LaunchedEffect
        }

        lyricsLoading = true
        lyricsError = null
        lyricsText = null

        try {
            val result = withContext(Dispatchers.IO) {
                val encodedSongId = java.net.URLEncoder.encode(songId, "UTF-8")
                val url = URL("https://vybe.vyoma-apps.workers.dev/api/songs/$encodedSongId/lyrics")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doInput = true
                }

                try {
                    val responseCode = connection.responseCode
                    val stream = if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                    val response = stream?.bufferedReader()?.use { it.readText() }
                        ?: throw Exception("HTTP $responseCode")

                    val json = JSONObject(response)
                    if (!json.optBoolean("success", false)) {
                        throw Exception("Lyrics not available")
                    }

                    val data = json.opt("data")
                    when (data) {
                        is JSONObject -> data.optString("lyrics").trim()
                        is String -> data.trim()
                        else -> ""
                    }
                } finally {
                    connection.disconnect()
                }
            }

            lyricsText = result.ifBlank { null }
            if (lyricsText == null) {
                lyricsError = "Lyrics are not available for this song."
            }
        } catch (e: Exception) {
            lyricsText = null
            lyricsError = "Unable to load lyrics"
        } finally {
            lyricsLoading = false
        }
    }

    val isDownloaded by (
        currentSong?.let { VybeApp.instance.repository.isSongDownloaded(it.id) }
            ?: flowOf(false)
    ).collectAsStateWithLifecycle(false)

    val userPlaylists by VybeApp.instance.repository.getUserPlaylists()
        .collectAsStateWithLifecycle(emptyList())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VybeBlack)
            .testTag("full_player_screen")
    ) {
        if (currentSong != null && currentSong.artworkUrl.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize().blur(70.dp)) {
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(40.dp).background(Color(0x22FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Collapse Player", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM", color = VybeTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(
                        playerState.playlistName,
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
                        modifier = Modifier.size(40.dp).background(Color(0x22FFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.MoreVert, "Options", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(VybeSurfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share Track", color = VybeTextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Share, null, tint = VybeOrange) },
                            onClick = {
                                showMenu = false
                                currentSong?.let { shareSong(context, it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = VybeTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = VybeOrange) },
                            onClick = { showMenu = false; showAddToPlaylistSheet = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isDownloaded) "Downloaded" else "Download Track", color = VybeTextPrimary) },
                            leadingIcon = {
                                Icon(
                                    if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                    null,
                                    tint = VybeElectricCyan
                                )
                            },
                            onClick = {
                                showMenu = false
                                currentSong?.let { song ->
                                    scope.launch { VybeApp.instance.repository.toggleDownloadSong(song) }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View Queue", color = VybeTextPrimary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = VybeTextSecondary) },
                            onClick = { showMenu = false; showQueueSheet = true }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .aspectRatio(1f)
                    .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = VybeOrange.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(VybeSurfaceVariant)
            ) {
                if (currentSong != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(currentSong.artworkUrl).crossfade(true).build(),
                        contentDescription = currentSong.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, null, tint = VybeTextMuted, modifier = Modifier.size(64.dp))
                    }
                }
                if (playerState.isBuffering) {
                    Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VybeOrange)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        currentSong?.title ?: "No Track Selected",
                        color = VybeTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currentSong?.artist ?: "Vybe Music",
                        color = VybeTextSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { currentSong?.let(onToggleFavorite) }) {
                    Icon(
                        if (currentSong?.isLiked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favorite",
                        tint = if (currentSong?.isLiked == true) VybeCoral else VybeTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            YouTubePlayerTimeline(
                progress = if (isSeeking) seekProgress else playerState.progress,
                durationMs = playerState.durationMs,
                currentPositionMs = playerState.currentPositionMs,
                isSeeking = isSeeking,
                onSeekStart = { isSeeking = true; seekProgress = playerState.progress },
                onSeekProgress = { seekProgress = it },
                onSeekFinished = { targetMs -> playerManager.seekTo(targetMs); isSeeking = false }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerManager.toggleShuffle() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (playerState.isShuffleEnabled) VybeOrange else VybeTextMuted)
                }
                IconButton(onClick = { playerManager.previous() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(VybePink, VybeOrange)))
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { playerManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(onClick = { playerManager.next() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { playerManager.cycleRepeatMode() }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (playerState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) VybeOrange else VybeTextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VybeSurface)
                        .clickable { showLyricsSheet = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Lyrics, "Lyrics", tint = VybeElectricCyan, modifier = Modifier.size(18.dp))
                    Text("Lyrics", color = VybeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VybeSurface)
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", tint = VybeOrange, modifier = Modifier.size(18.dp))
                    Text("Queue (${playerState.queue.size})", color = VybeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = VybeNearBlack
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Now Playing & Queue", color = VybeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { showQueueSheet = false } }) {
                            Icon(Icons.Default.Close, "Close", tint = VybeTextSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.fillMaxWidth().height(400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(playerState.queue) { index, song ->
                            val isCurrent = index == playerState.currentIndex
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) Color(0x1FFF6436) else VybeSurface)
                                    .clickable { playerManager.playSong(song, playerState.queue, playerState.playlistName) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(VybeSurfaceVariant)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(song.artworkUrl).crossfade(true).build(),
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, color = if (isCurrent) VybeOrange else VybeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, color = VybeTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (isCurrent && playerState.isPlaying) {
                                    EqualizerWaveformIndicator(Modifier.size(20.dp), VybeOrange)
                                } else {
                                    IconButton(onClick = { playerManager.removeFromQueue(index) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Close, "Remove", tint = VybeTextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showLyricsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLyricsSheet = false },
                sheetState = sheetState,
                containerColor = VybeNearBlack
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 650.dp).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Lyrics", color = VybeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${currentSong?.title ?: "Unknown"} — ${currentSong?.artist ?: "Unknown"}",
                        color = VybeTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(24.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            when {
                                lyricsLoading -> {
                                    CircularProgressIndicator(color = VybeOrange, modifier = Modifier.size(32.dp))
                                }
                                lyricsError != null -> {
                                    Text(
                                        lyricsError!!,
                                        color = VybeTextMuted,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 24.sp
                                    )
                                }
                                !lyricsText.isNullOrBlank() -> {
                                    Text(
                                        lyricsText!!,
                                        color = VybeTextPrimary,
                                        fontSize = 16.sp,
                                        lineHeight = 26.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                else -> {
                                    Text(
                                        "Lyrics are not available for this song.",
                                        color = VybeTextMuted,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddToPlaylistSheet && currentSong != null) {
            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistSheet = false },
                containerColor = VybeSurface,
                dragHandle = null
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Add to Playlist", color = VybeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAddToPlaylistSheet = false }) {
                            Icon(Icons.Default.Close, "Close", tint = VybeTextSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (userPlaylists.isEmpty()) {
                        Text(
                            "No custom playlists found. Go to Library -> Playlists -> My Playlist to create one!",
                            color = VybeTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(userPlaylists, key = { it.id }) { playlist ->
                                Row(
                                    Modifier
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
                                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = VybeOrange, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(playlist.title, color = VybeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
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
    val safeDuration = durationMs.coerceAtLeast(0L)
    val displayedCurrentMs = if (isSeeking) (progress.coerceIn(0f, 1f) * safeDuration).toLong() else currentPositionMs

    val animatedThumbRadius by animateDpAsState(
        targetValue = if (isSeeking) 7.5.dp else 4.5.dp,
        label = "thumb_radius"
    )
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isSeeking) 3.5.dp else 2.dp,
        label = "track_height"
    )

    Column(modifier = modifier.fillMaxWidth().testTag("player_seekbar")) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        if (width > 0f) {
                            val fraction = (offset.x / width).coerceIn(0f, 1f)
                            onSeekStart()
                            onSeekProgress(fraction)
                            onSeekFinished((fraction * safeDuration).toLong())
                        }
                    }
                }
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            if (width > 0f) {
                                onSeekStart()
                                onSeekProgress((offset.x / width).coerceIn(0f, 1f))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val width = size.width.toFloat()
                            if (width > 0f) onSeekProgress((change.position.x / width).coerceIn(0f, 1f))
                        },
                        onDragEnd = { onSeekFinished((progress.coerceIn(0f, 1f) * safeDuration).toLong()) },
                        onDragCancel = { onSeekFinished((progress.coerceIn(0f, 1f) * safeDuration).toLong()) }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val safeProgress = progress.coerceIn(0f, 1f)
            val thumbXOffsetPx = totalWidthPx * safeProgress
            val density = LocalDensity.current
            val thumbOffsetDp = with(density) { thumbXOffsetPx.toDp() }
            val maxThumbOffsetDp = with(density) {
                (totalWidthPx - animatedThumbRadius.toPx() * 2).coerceAtLeast(0f).toDp()
            }

            Box(
                Modifier.fillMaxWidth().height(animatedTrackHeight).clip(CircleShape).background(Color(0x33FFFFFF))
            )
            Box(
                Modifier.width(thumbOffsetDp).height(animatedTrackHeight).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(VybePink, VybeOrange)))
            )
            Box(
                Modifier
                    .offset(x = (thumbOffsetDp - animatedThumbRadius).coerceIn(0.dp, maxThumbOffsetDp))
                    .size(animatedThumbRadius * 2)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(if (isSeeking) 2.dp else 1.5.dp, Color(0xFFFF5E3A), CircleShape)
            )
        }

        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                MusicUtils.formatDurationMs(displayedCurrentMs),
                color = if (isSeeking) VybeOrange else VybeTextSecondary,
                fontSize = 12.sp,
                fontFamily = OutfitFontFamily,
                fontWeight = if (isSeeking) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                MusicUtils.formatDurationMs(safeDuration),
                color = VybeTextMuted,
                fontSize = 12.sp,
                fontFamily = OutfitFontFamily
            )
        }
    }
}
