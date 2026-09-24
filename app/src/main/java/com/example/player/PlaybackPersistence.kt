package com.example.player

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.Song
import org.json.JSONArray
import org.json.JSONObject

class PlaybackPersistence(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vybe_playback_state", Context.MODE_PRIVATE)

    fun saveState(
        currentSong: Song?,
        queue: List<Song>,
        currentIndex: Int,
        positionMs: Long,
        repeatMode: Int,
        isShuffle: Boolean,
        playlistName: String
    ) {
        val editor = prefs.edit()
        if (currentSong != null) {
            editor.putString(KEY_CURRENT_SONG, songToJson(currentSong).toString())
        } else {
            editor.remove(KEY_CURRENT_SONG)
        }

        val queueArray = JSONArray()
        queue.forEach { song ->
            queueArray.put(songToJson(song))
        }
        editor.putString(KEY_QUEUE, queueArray.toString())
        editor.putInt(KEY_CURRENT_INDEX, currentIndex)
        editor.putLong(KEY_POSITION_MS, positionMs)
        editor.putInt(KEY_REPEAT_MODE, repeatMode)
        editor.putBoolean(KEY_SHUFFLE, isShuffle)
        editor.putString(KEY_PLAYLIST_NAME, playlistName)
        editor.apply()
    }

    fun getSavedState(): RestoredPlaybackState? {
        val currentSongJson = prefs.getString(KEY_CURRENT_SONG, null) ?: return null
        val song = parseSong(JSONObject(currentSongJson)) ?: return null
        val queueJson = prefs.getString(KEY_QUEUE, null)
        val queue = mutableListOf<Song>()
        if (queueJson != null) {
            try {
                val array = JSONArray(queueJson)
                for (i in 0 until array.length()) {
                    parseSong(array.getJSONObject(i))?.let { queue.add(it) }
                }
            } catch (_: Exception) {
            }
        }
        if (queue.isEmpty()) {
            queue.add(song)
        }
        val currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0).coerceIn(0, queue.size - 1)
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val repeatMode = prefs.getInt(KEY_REPEAT_MODE, 0)
        val isShuffle = prefs.getBoolean(KEY_SHUFFLE, false)
        val playlistName = prefs.getString(KEY_PLAYLIST_NAME, "Your Music") ?: "Your Music"

        return RestoredPlaybackState(
            currentSong = song,
            queue = queue,
            currentIndex = currentIndex,
            positionMs = positionMs,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            playlistName = playlistName
        )
    }

    private fun songToJson(song: Song): JSONObject {
        return JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("artistId", song.artistId ?: "")
            put("album", song.album)
            put("albumId", song.albumId ?: "")
            put("durationSeconds", song.durationSeconds)
            put("year", song.year)
            put("language", song.language)
            put("artworkUrl", song.artworkUrl)
            put("streamUrl", song.streamUrl)
            put("hasLyrics", song.hasLyrics)
            put("lyricsId", song.lyricsId ?: "")
            put("isLiked", song.isLiked)
        }
    }

    private fun parseSong(obj: JSONObject): Song? {
        return try {
            Song(
                id = obj.getString("id"),
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                artistId = obj.optString("artistId").takeIf { it.isNotEmpty() },
                album = obj.optString("album", ""),
                albumId = obj.optString("albumId").takeIf { it.isNotEmpty() },
                durationSeconds = obj.optInt("durationSeconds", 0),
                year = obj.optString("year", ""),
                language = obj.optString("language", ""),
                artworkUrl = obj.optString("artworkUrl", ""),
                streamUrl = obj.optString("streamUrl", ""),
                hasLyrics = obj.optBoolean("hasLyrics", false),
                lyricsId = obj.optString("lyricsId").takeIf { it.isNotEmpty() },
                isLiked = obj.optBoolean("isLiked", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_CURRENT_SONG = "key_current_song"
        private const val KEY_QUEUE = "key_queue"
        private const val KEY_CURRENT_INDEX = "key_current_index"
        private const val KEY_POSITION_MS = "key_position_ms"
        private const val KEY_REPEAT_MODE = "key_repeat_mode"
        private const val KEY_SHUFFLE = "key_shuffle"
        private const val KEY_PLAYLIST_NAME = "key_playlist_name"
    }
}

data class RestoredPlaybackState(
    val currentSong: Song,
    val queue: List<Song>,
    val currentIndex: Int,
    val positionMs: Long,
    val repeatMode: Int,
    val isShuffle: Boolean,
    val playlistName: String
)
