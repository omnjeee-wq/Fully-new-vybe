package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.R
import com.example.domain.model.Song
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlayfairFontFamily
import com.example.ui.theme.VybeBlack
import com.example.ui.theme.VybeElectricCyan
import com.example.ui.theme.VybePink
import com.example.ui.theme.neonGlow
import com.example.ui.theme.pulsingGlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private val GreatVibesFontFamily = FontFamily(
    Font(
        resId = R.font.great_vibes,
        weight = FontWeight.Normal
    )
)

private val VibrantThemePalette = listOf(
    Color(0xFFFF6436),
    Color(0xFFFF2A85),
    Color(0xFF00E5FF),
    Color(0xFFA855F7),
    Color(0xFFFFB800),
    Color(0xFF10B981),
    Color(0xFFFF3366)
)

@Composable
fun HeroBanner(
    heroSong: Song?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val artistName = heroSong?.artist?.ifBlank { "TOP ARTIST" } ?: "TOP ARTIST"

    // The current Song model exposes artworkUrl.
    // Keep the existing backend/model untouched.
    // The artwork is also used as the circular disc centre image.
    val artistImageUrl = heroSong?.artworkUrl

    val fallbackTheme = remember(heroSong?.id) {
        val hash = abs((heroSong?.id ?: "vybe").hashCode())
        VibrantThemePalette[hash % VibrantThemePalette.size]
    }

    var targetThemeColor by remember(heroSong?.id) {
        mutableStateOf(fallbackTheme)
    }

    // Theme is extracted from the ARTIST photo, not the song thumbnail.
    LaunchedEffect(artistImageUrl) {
        if (!artistImageUrl.isNullOrBlank()) {
            targetThemeColor = extractVibrantColorFromArtwork(
                context = context,
                imageUrl = artistImageUrl,
                fallback = fallbackTheme
            )
        } else {
            targetThemeColor = fallbackTheme
        }
    }

    val themeColor by animateColorAsState(
        targetValue = targetThemeColor,
        animationSpec = tween(650),
        label = "hero_artist_theme"
    )

    // Continuous vinyl rotation.
    val rotationTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val vinylRotation by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_rotation_angle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clipToBounds()
        ) {

            // --------------------------------------------------------
            // 1. SONG THUMBNAIL = FULL BLEED BLURRED BACKGROUND
            // --------------------------------------------------------
            if (heroSong != null && heroSong.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(heroSong.artworkUrl)
                        .crossfade(true)
                        .size(768, 768)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.18f)
                        .blur(28.dp)
                )

                // Dark cinematic layer.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF26182B),
                                    VybeBlack
                                ),
                                radius = 700f
                            )
                        )
                )
            }

            // Artist-colour ambient wash.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                themeColor.copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            radius = 650f
                        )
                    )
            )

            // Left readability gradient.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xD9000000),
                            0.42f to Color(0x88000000),
                            0.70f to Color.Transparent,
                            1f to Color.Transparent
                        )
                    )
            )

            // Bottom fade.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.35f to Color(0x77000000),
                            1f to Color.Black
                        )
                    )
            )

            // --------------------------------------------------------
            // 2. ROTATING VINYL + CIRCULAR ARTIST PHOTO
            // --------------------------------------------------------
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(225.dp)
                    .graphicsLayer {
                        rotationZ = vinylRotation
                    },
                contentAlignment = Alignment.Center
            ) {

                // Big soft glow behind the disc.
                Box(
                    modifier = Modifier
                        .size(218.dp)
                        .neonGlow(
                            color = themeColor,
                            radius = 42.dp,
                            alpha = 0.75f
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.34f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Outer vinyl.
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF343434),
                                    Color(0xFF101010),
                                    Color.Black,
                                    Color(0xFF181818)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            themeColor.copy(alpha = 0.65f),
                            CircleShape
                        )
                ) {
                    // Vinyl grooves.
                    for (i in 0..7) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size((180 - i * 18).dp)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.10f),
                                    CircleShape
                                )
                        )
                    }

                    // Artist image in the centre.
                    if (!artistImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artistImageUrl)
                                .crossfade(true)
                                .size(180, 180)
                                .build(),
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(92.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    themeColor.copy(alpha = 0.95f),
                                    CircleShape
                                )
                                .neonGlow(
                                    color = themeColor,
                                    radius = 16.dp,
                                    alpha = 0.75f
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(themeColor.copy(alpha = 0.25f))
                        )
                    }

                    // Centre hole / subtle highlight.
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.25f),
                                CircleShape
                            )
                    )
                }
            }

            // --------------------------------------------------------
            // 3. FOREGROUND UI
            // --------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {

                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .neonGlow(
                                    color = themeColor,
                                    radius = 10.dp,
                                    alpha = 0.6f
                                )
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            VybeElectricCyan,
                                            themeColor
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.vybe_logo),
                                contentDescription = "VYBE Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(45.dp)
                            )
                        }

                        Text(
                            text = "VYBE",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = OutfitFontFamily,
                            letterSpacing = 1.5.sp
                        )
                    }

                    IconButton(
                        onClick = { shareVybeApp(context) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.22f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share VYBE",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // ----------------------------------------------------
                // 4. JUST VIBE + SONG NAME
                // ----------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.52f)
                        .padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "Just Vibe",
                        color = themeColor,
                        fontSize = 42.sp,
                        fontFamily = GreatVibesFontFamily,
                        fontWeight = FontWeight.Normal,
                        style = TextStyle(
                            shadow = Shadow(
                                color = themeColor.copy(alpha = 0.90f),
                                blurRadius = 28f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = heroSong?.title?.ifBlank { "Ghera Hua" } ?: "Ghera Hua",
                        color = Color.White,
                        fontSize = 39.sp,
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(
                            shadow = Shadow(
                                color = themeColor.copy(alpha = 0.50f),
                                blurRadius = 18f
                            )
                        )
                    )
                }

                // ----------------------------------------------------
                // 5. THEME-COLOURED PLAY / PAUSE BUTTON
                // ----------------------------------------------------
                Box(
                    modifier = Modifier
                        .pulsingGlow(
                            color = themeColor,
                            minRadius = 8.dp,
                            maxRadius = 20.dp,
                            cornerRadius = 24.dp
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    themeColor.copy(alpha = 0.92f),
                                    themeColor.copy(alpha = 0.58f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.38f),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable { onPlayPauseClick() }
                        .padding(
                            horizontal = 23.dp,
                            vertical = 10.dp
                        )
                        .testTag("hero_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                fadeIn(tween(150)) togetherWith
                                    androidx.compose.animation.fadeOut(tween(150))
                            },
                            label = "hero_play_icon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = if (isPlaying) "PAUSE" else "PLAY",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }

        // Keep the existing clean black separator.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color.Black)
        )
    }
}

/**
 * Extracts a vibrant colour from the artist image.
 */
private suspend fun extractVibrantColorFromArtwork(
    context: Context,
    imageUrl: String,
    fallback: Color
): Color = withContext(Dispatchers.IO) {
    try {
        val loader = Coil.imageLoader(context)

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .size(64, 64)
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        val bitmap = (result as? BitmapDrawable)?.bitmap
            ?: return@withContext fallback

        var bestColor = fallback
        var highestScore = -1f

        val width = bitmap.width
        val height = bitmap.height
        val step = maxOf(1, width / 12)

        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val pixel = bitmap.getPixel(x, y)

                val r = android.graphics.Color.red(pixel) / 255f
                val g = android.graphics.Color.green(pixel) / 255f
                val b = android.graphics.Color.blue(pixel) / 255f

                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val delta = max - min

                val saturation = if (max == 0f) 0f else delta / max
                val value = max

                if (value in 0.35f..0.98f && saturation > 0.35f) {
                    val score = saturation * 2.0f + value

                    if (score > highestScore) {
                        highestScore = score
                        bestColor = Color(r, g, b)
                    }
                }
            }
        }

        bestColor
    } catch (_: Exception) {
        fallback
    }
}

private fun shareVybeApp(context: Context) {
    val shareMessage = """
        🎧 I'm listening to music on VYBE — your personal music experience.

        Listen to music ad-free, discover songs made for your vibe, and enjoy your music your way.

        Download VYBE:
        https://t.me/VyomaOfficial

        🎵 VYBE — Made For Your Vibe.
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareMessage)
        type = "text/plain"
    }

    context.startActivity(
        Intent.createChooser(
            sendIntent,
            "Share VYBE via"
        )
    )
}
