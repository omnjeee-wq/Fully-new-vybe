package com.example.data.drive

import com.example.data.local.AppPreferenceEntity
import com.example.data.local.FavoriteAlbumEntity
import com.example.data.local.FavoriteSongEntity
import com.example.data.local.LikedPlaylistEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.SongHistoryEntity
import com.example.data.local.UserPlaylistEntity
import com.example.data.local.UserPlaylistSongEntity

/**
 * Data model representing the full portable backup package for VYBE.
 * Contains user favorites, custom playlists, listening history, search queries,
 * and user preferences without including downloaded audio files.
 */
data class VybeBackupData(
    val version: Int = 1,
    val appName: String = "VYBE",
    val appVersion: String = "1.0",
    val backupTimestamp: Long = System.currentTimeMillis(),
    val deviceModel: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
    val favoriteSongs: List<FavoriteSongEntity> = emptyList(),
    val favoriteAlbums: List<FavoriteAlbumEntity> = emptyList(),
    val userPlaylists: List<UserPlaylistEntity> = emptyList(),
    val userPlaylistSongs: List<UserPlaylistSongEntity> = emptyList(),
    val likedPlaylists: List<LikedPlaylistEntity> = emptyList(),
    val songHistory: List<SongHistoryEntity> = emptyList(),
    val searchHistory: List<SearchHistoryEntity> = emptyList(),
    val appPreferences: List<AppPreferenceEntity> = emptyList()
) {
    val totalItems: Int
        get() = favoriteSongs.size +
                favoriteAlbums.size +
                userPlaylists.size +
                userPlaylistSongs.size +
                likedPlaylists.size +
                songHistory.size +
                appPreferences.size
}

data class DriveAccountInfo(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)

data class DriveBackupSummary(
    val fileId: String,
    val modifiedTime: Long,
    val sizeBytes: Long,
    val totalSongs: Int,
    val totalPlaylists: Int,
    val totalHistory: Int
)

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Connecting : SyncStatus
    data class BackingUp(val message: String = "Backing up your data to Google Drive...") : SyncStatus
    data class Restoring(val message: String = "Restoring your data from Google Drive...") : SyncStatus
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus
    data class Error(val message: String, val isAuthError: Boolean = false) : SyncStatus
}
