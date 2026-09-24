package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM song_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(): Flow<List<SongHistoryEntity>>

    @Query("SELECT * FROM song_history ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 20): List<SongHistoryEntity>

    @Query("SELECT * FROM song_history WHERE songId = :songId")
    suspend fun getHistoryBySongId(songId: String): SongHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHistory(entity: SongHistoryEntity)

    @Query("UPDATE song_history SET completedCount = completedCount + 1 WHERE songId = :songId")
    suspend fun incrementCompleted(songId: String)

    @Query("UPDATE song_history SET skipCount = skipCount + 1 WHERE songId = :songId")
    suspend fun incrementSkip(songId: String)

    @Query("UPDATE song_history SET isLiked = :isLiked WHERE songId = :songId")
    suspend fun updateLikedInHistory(songId: String, isLiked: Boolean)

    // Favorites
    @Query("SELECT * FROM favorite_songs ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    fun isFavoriteFlow(songId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteSongEntity)

    @Query("DELETE FROM favorite_songs WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    // Key-value App Preferences (e.g. banner counter)
    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: AppPreferenceEntity)

    // Search History (maximum 10 searches, auto-trims older searches)
    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearchQueries(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(queryEntity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAllSearchHistory()

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAllSearchHistorySync(): List<SearchHistoryEntity>

    @Transaction
    suspend fun addSearchQueryAndTrim(query: String, maxItems: Int = 10) {
        insertSearchQuery(SearchHistoryEntity(query = query, timestamp = System.currentTimeMillis()))
        val all = getAllSearchHistorySync()
        if (all.size > maxItems) {
            val toDelete = all.drop(maxItems)
            toDelete.forEach { deleteSearchQuery(it.query) }
        }
    }

    // Favorite Albums
    @Query("SELECT * FROM favorite_albums ORDER BY addedTimestamp DESC")
    fun getAllFavoriteAlbums(): Flow<List<FavoriteAlbumEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_albums WHERE albumId = :albumId)")
    fun isAlbumFavoriteFlow(albumId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_albums WHERE albumId = :albumId)")
    suspend fun isAlbumFavorite(albumId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteAlbum(album: FavoriteAlbumEntity)

    @Query("DELETE FROM favorite_albums WHERE albumId = :albumId")
    suspend fun removeFavoriteAlbum(albumId: String)

    // Downloaded Songs
    @Query("SELECT * FROM downloaded_songs ORDER BY downloadTimestamp DESC")
    fun getAllDownloadedSongs(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    fun isSongDownloadedFlow(songId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE songId = :songId)")
    suspend fun isSongDownloaded(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDownloadedSong(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun removeDownloadedSong(songId: String)

    @Query("DELETE FROM downloaded_songs")
    suspend fun clearAllDownloadedSongs()

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM downloaded_songs")
    fun getTotalDownloadedSizeBytes(): Flow<Long>

    // User Playlists ("My Playlists")
    @Query("SELECT * FROM user_playlists ORDER BY createdTimestamp DESC")
    fun getAllUserPlaylists(): Flow<List<UserPlaylistEntity>>

    @Query("SELECT * FROM user_playlists WHERE playlistId = :playlistId")
    suspend fun getUserPlaylistById(playlistId: String): UserPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPlaylist(playlist: UserPlaylistEntity)

    @Query("DELETE FROM user_playlists WHERE playlistId = :playlistId")
    suspend fun deleteUserPlaylist(playlistId: String)

    @Query("DELETE FROM user_playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteSongsForPlaylist(playlistId: String)

    @Transaction
    suspend fun deleteUserPlaylistAndSongs(playlistId: String) {
        deleteSongsForPlaylist(playlistId)
        deleteUserPlaylist(playlistId)
    }

    // User Playlist Songs
    @Query("SELECT * FROM user_playlist_songs WHERE playlistId = :playlistId ORDER BY addedTimestamp ASC")
    fun getSongsForPlaylistFlow(playlistId: String): Flow<List<UserPlaylistSongEntity>>

    @Query("SELECT * FROM user_playlist_songs WHERE playlistId = :playlistId ORDER BY addedTimestamp ASC")
    suspend fun getSongsForPlaylistSync(playlistId: String): List<UserPlaylistSongEntity>

    @Query("SELECT COUNT(*) FROM user_playlist_songs WHERE playlistId = :playlistId")
    fun getSongCountForPlaylistFlow(playlistId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(song: UserPlaylistSongEntity)

    @Query("DELETE FROM user_playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    // Liked Playlists ("Liked Playlists")
    @Query("SELECT * FROM liked_playlists ORDER BY addedTimestamp DESC")
    fun getAllLikedPlaylists(): Flow<List<LikedPlaylistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_playlists WHERE playlistId = :playlistId)")
    fun isPlaylistLikedFlow(playlistId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_playlists WHERE playlistId = :playlistId)")
    suspend fun isPlaylistLiked(playlistId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLikedPlaylist(playlist: LikedPlaylistEntity)

    @Query("DELETE FROM liked_playlists WHERE playlistId = :playlistId")
    suspend fun removeLikedPlaylist(playlistId: String)
}
