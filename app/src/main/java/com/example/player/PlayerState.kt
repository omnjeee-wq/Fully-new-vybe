package com.example.player

import androidx.media3.common.Player
import com.example.domain.model.Song

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffleEnabled: Boolean = false,
    val isSmartShuffleEnabled: Boolean = false,
    val activeMoodFlow: String? = null,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playlistName: String = "Your Music"
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
