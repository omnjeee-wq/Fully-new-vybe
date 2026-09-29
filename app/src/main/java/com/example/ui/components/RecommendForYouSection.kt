package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.Song
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.VybeBorder
import com.example.ui.theme.VybeCoral
import com.example.ui.theme.VybeOrange
import com.example.ui.theme.VybeSurface
import com.example.ui.theme.VybeSurfaceVariant
import com.example.ui.theme.VybeTextMuted
import com.example.ui.theme.VybeTextPrimary
import com.example.ui.theme.VybeTextSecondary

@Composable
fun RecommendForYouSection(
    songs: List<Song>,
    currentPlayingSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (songs.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("recommend_for_you_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(VybeOrange)
                )
                Text(
                    text = "Recommend For You",
                    color = VybeTextPrimary,
                    fontSize = 18.sp,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier.background(Color.Transparent)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { /* View All Navigation */ }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "View All",
                    color = VybeOrange,
                    fontSize = 13.sp,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = VybeOrange,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // 3-song vertical columns in horizontal carousel (e.g. 12 songs = 4 columns)
        val chunkedSongs = remember(songs) { songs.chunked(3) }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            items(chunkedSongs) { columnSongs ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.width(280.dp)
                ) {
                    columnSongs.forEach { song ->
                        RecommendSongItem(
                            song = song,
                            isCurrent = song.id == currentPlayingSongId,
                            isPlaying = isPlaying && song.id == currentPlayingSongId,
                            onClick = { onSongClick(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onToggleFavorite = { onToggleFavorite(song) },
                            onShare = { shareSong(context, song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendSongItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0x1FFF6436) else VybeSurfaceVariant.copy(alpha = 0.55f))
            .border(
                1.dp,
                if (isCurrent) VybeOrange.copy(alpha = 0.5f) else VybeBorder.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(8.dp)
            .testTag("recommend_song_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(54.dp)
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
                modifier = Modifier.size(54.dp)
            )

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    EqualizerWaveformIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Titles
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

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = song.artist,
                color = VybeTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More Actions
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = VybeTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(VybeSurfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Queue", color = VybeTextPrimary) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = VybeOrange)
                    },
                    onClick = {
                        menuExpanded = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(if (song.isLiked) "Remove Favorite" else "Favorite", color = VybeTextPrimary)
                    },
                    leadingIcon = {
                        Icon(
                            if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (song.isLiked) Color.Red else VybeCoral
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleFavorite()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share Song", color = VybeTextPrimary) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = VybeTextSecondary)
                    },
                    onClick = {
                        menuExpanded = false
                        onShare()
                    }
                )
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
