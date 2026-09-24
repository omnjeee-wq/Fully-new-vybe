package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_history")
data class SongHistoryEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationSeconds: Int = 0,
    val language: String = "",
    val playCount: Int = 1,
    val completedCount: Int = 0,
    val skipCount: Int = 0,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val isLiked: Boolean = false
)

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationSeconds: Int = 0,
    val album: String = "",
    val language: String = "",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_albums")
data class FavoriteAlbumEntity(
    @PrimaryKey val albumId: String,
    val title: String,
    val artist: String = "",
    val artworkUrl: String = "",
    val year: String = "",
    val songCount: Int = 0,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationSeconds: Int = 0,
    val sizeBytes: Long = 0,
    val downloadTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_playlists")
data class UserPlaylistEntity(
    @PrimaryKey val playlistId: String,
    val title: String,
    val description: String = "",
    val artworkUrl: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class UserPlaylistSongEntity(
    val playlistId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationSeconds: Int = 0,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked_playlists")
data class LikedPlaylistEntity(
    @PrimaryKey val playlistId: String,
    val title: String,
    val subtitle: String = "",
    val artworkUrl: String = "",
    val songCount: Int = 0,
    val addedTimestamp: Long = System.currentTimeMillis()
)

