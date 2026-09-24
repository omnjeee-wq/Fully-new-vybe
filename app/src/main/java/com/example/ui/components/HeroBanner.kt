package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.domain.model.Song
import com.example.ui.theme.AmbientBackdropGlow
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.PlayfairFontFamily
import com.example.ui.theme.VybeBlack
import com.example.ui.theme.VybeElectricCyan
import com.example.ui.theme.VybePink
import com.example.ui.theme.VybeTextPrimary
import com.example.ui.theme.neonGlow
import com.example.ui.theme.pulsingGlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

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

    val artistName =
        heroSong?.artist?.ifBlank { "TOP ARTIST" }
            ?: "TOP ARTIST"

    // ------------------------------------------------------------
    // Dynamic theme color
    // ------------------------------------------------------------

    val fallbackTheme = remember(heroSong?.id) {
        val hash = abs(
            (heroSong?.id ?: "vybe").hashCode()
        )

        VibrantThemePalette[
            hash % VibrantThemePalette.size
        ]
    }

    var targetThemeColor by remember(heroSong?.id) {
        mutableStateOf(fallbackTheme)
    }

    LaunchedEffect(heroSong?.artworkUrl) {
        val artworkUrl = heroSong?.artworkUrl

        if (!artworkUrl.isNullOrBlank()) {
            val extracted =
                extractVibrantColorFromArtwork(
                    context = context,
                    imageUrl = artworkUrl,
                    fallback = fallbackTheme
                )

            targetThemeColor = extracted
        } else {
            targetThemeColor = fallbackTheme
        }
    }

    val animatedThemeColor by animateColorAsState(
        targetValue = targetThemeColor,
        animationSpec = tween(
            durationMillis = 650
        ),
        label = "hero_banner_theme_color"
    )

    // ============================================================
    // HERO BANNER + CLEAN BLACK SEPARATOR
    // ============================================================

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_banner")
    ) {

        // ========================================================
        // MAIN HERO AREA
        // ========================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
                

            // ====================================================
            // 1. BLURRED FULL-WIDTH BACKGROUND
            // ====================================================

            if (
                heroSong != null &&
                heroSong.artworkUrl.isNotBlank()
            ) {

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(heroSong.artworkUrl)
                        .size(256, 256)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.20f)
                        .blur(28.dp)
                )

                // Dark cinematic overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = 0.18f
                            )
                        )
                )

                // ====================================================
                // 2. SHARP ARTWORK — RIGHT SIDE
                // ====================================================

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.68f)
                        .align(Alignment.CenterEnd)
                ) {

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(heroSong.artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = heroSong.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy =
                                    CompositingStrategy.Offscreen
                            }
                            .drawWithContent {

                                drawContent()

                                /*
                                 * Smoothly dissolves the sharp
                                 * artwork into the blurred artwork.
                                 *
                                 * This prevents a visible
                                 * vertical split.
                                 */
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0.0f to Color.Transparent,
                                        0.12f to Color.Transparent,
                                        0.30f to Color.Black.copy(
                                            alpha = 0.25f
                                        ),
                                        0.48f to Color.Black,
                                        1.0f to Color.Black
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    )
                }

            } else {

                // ====================================================
                // FALLBACK BACKGROUND
                // ====================================================

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

            // ========================================================
            // 3. LEFT TEXT READABILITY SCRIM
            // ========================================================

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to Color(0xC009090D),
                            0.30f to Color(0x8809090D),
                            0.55f to Color(0x3309090D),
                            0.75f to Color.Transparent,
                            1.0f to Color.Transparent
                        )
                    )
            )

            // ========================================================
            // 4. STATUS BAR SCRIM
            // ========================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0x99000000),
                            0.55f to Color(0x33000000),
                            1.0f to Color.Transparent
                        )
                    )
            )

            // ========================================================
            // 5. BOTTOM FADE INTO BLACK
            // ========================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.30f to Color.Transparent,
                            0.72f to Color(0xCC000000),
                            1.0f to Color.Black
                        )
                    )
            )

            // ========================================================
            // 6. DYNAMIC AMBIENT GLOW
            // ========================================================

            AmbientBackdropGlow(
                color = animatedThemeColor,
                alpha = 0.30f,
                diameter = 260.dp,
                modifier = Modifier.align(
                    Alignment.TopStart
                )
            )

            // ========================================================
            // 7. FOREGROUND CONTENT
            // ========================================================

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

                // ====================================================
                // TOP BAR
                // ====================================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .neonGlow(
                                    color = animatedThemeColor,
                                    radius = 10.dp,
                                    alpha = 0.6f
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            VybeElectricCyan,
                                            animatedThemeColor
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "♪",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "VYBE",
                            color = VybeTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = OutfitFontFamily,
                            letterSpacing = 2.5.sp
                        )
                    }

                    // =================================================
                    // SHARE BUTTON
                    // =================================================

                    IconButton(
                        onClick = {
                            shareVybeApp(context)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = 0.20f
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(
                                    alpha = 0.20f
                                ),
                                shape = CircleShape
                            )
                            .testTag("share_button")
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Share,
                            contentDescription =
                                "Share VYBE",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // ====================================================
                // MIDDLE TEXT
                // ====================================================

                Column(
                    modifier = Modifier.padding(
                        bottom = 6.dp
                    )
                ) {

                    Text(
                        text = "MADE FOR",
                        color = Color.White.copy(
                            alpha = 0.85f
                        ),
                        fontSize = 12.sp,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.5.sp
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Your ",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontFamily = OutfitFontFamily,
                            fontWeight =
                                FontWeight.ExtraBold
                        )

                        Text(
                            text = "Vibe",
                            color = animatedThemeColor,
                            fontSize = 36.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontFamily =
                                PlayfairFontFamily,
                            style = TextStyle(
                                shadow = Shadow(
                                    color =
                                        animatedThemeColor.copy(
                                            alpha = 0.8f
                                        ),
                                    blurRadius = 24f
                                )
                            )
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Icon(
                            imageVector =
                                Icons.Default.Headphones,
                            contentDescription = null,
                            tint = animatedThemeColor,
                            modifier = Modifier
                                .size(22.dp)
                                .neonGlow(
                                    color =
                                        animatedThemeColor,
                                    radius = 8.dp,
                                    alpha = 0.5f
                                )
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "PERSONALISED TRACK OF " +
                                artistName.uppercase(),
                        color = Color.White.copy(
                            alpha = 0.75f
                        ),
                        fontSize = 11.sp,
                        fontFamily = OutfitFontFamily,
                        fontWeight =
                            FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }

                // ====================================================
                // PLAY BUTTON
                // ====================================================

                Box(
                    modifier = Modifier
                        .pulsingGlow(
                            color = animatedThemeColor,
                            minRadius = 8.dp,
                            maxRadius = 18.dp,
                            cornerRadius = 24.dp
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    VybePink,
                                    animatedThemeColor
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(
                                alpha = 0.35f
                            ),
                            CircleShape
                        )
                        .clickable {
                            onPlayPauseClick()
                        }
                        .padding(
                            horizontal = 24.dp,
                            vertical = 10.dp
                        )
                        .testTag(
                            "hero_play_button"
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                fadeIn(
                                    tween(150)
                                ) togetherWith
                                    fadeOut(
                                        tween(150)
                                    )
                            },
                            label = "hero_play_icon"
                        ) { playing ->

                            Icon(
                                imageVector =
                                    if (playing) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                contentDescription =
                                    if (playing) {
                                        "Pause"
                                    } else {
                                        "Play"
                                    },
                                tint = Color.White,
                                modifier = Modifier.size(
                                    18.dp
                                )
                            )
                        }

                        Text(
                            text =
                                if (isPlaying) {
                                    "PAUSE"
                                } else {
                                    "PLAY"
                                },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily =
                                OutfitFontFamily,
                            fontWeight =
                                FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
        }

        // ============================================================
        // SOLID BLACK SEPARATOR
        // ============================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(Color.Black)
        )
    }
}

/**
 * Extracts a vibrant theme color from the artwork.
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

        val result =
            (loader.execute(request) as? SuccessResult)
                ?.drawable

        val bitmap =
            (result as? BitmapDrawable)?.bitmap
                ?: return@withContext fallback

        var bestColor = fallback
        var highestScore = -1f

        val width = bitmap.width
        val height = bitmap.height

        val step = maxOf(
            1,
            width / 12
        )

        for (x in 0 until width step step) {

            for (y in 0 until height step step) {

                val pixel = bitmap.getPixel(x, y)

                val r =
                    android.graphics.Color.red(pixel) /
                        255f

                val g =
                    android.graphics.Color.green(pixel) /
                        255f

                val b =
                    android.graphics.Color.blue(pixel) /
                        255f

                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)

                val delta = max - min

                val saturation =
                    if (max == 0f) {
                        0f
                    } else {
                        delta / max
                    }

                val value = max

                if (
                    value in 0.40f..0.95f &&
                    saturation > 0.45f
                ) {

                    val score =
                        saturation * 2.0f +
                            value

                    if (score > highestScore) {

                        highestScore = score

                        bestColor = Color(
                            r,
                            g,
                            b
                        )
                    }
                }
            }
        }

        bestColor

    } catch (e: Exception) {

        fallback
    }
}

private fun shareVybeApp(
    context: Context
) {

    val shareMessage = """
        🎧 I'm listening to music on VYBE — your personal music experience.

        Listen to music ad-free, discover songs made for your vibe, and enjoy your music your way.

        Download VYBE:
        https://t.me/VyomaOfficial

        🎵 VYBE — Made For Your Vibe.
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            shareMessage
        )
        type = "text/plain"
    }

    val shareIntent =
        Intent.createChooser(
            sendIntent,
            "Share VYBE via"
        )

    context.startActivity(shareIntent)
}
