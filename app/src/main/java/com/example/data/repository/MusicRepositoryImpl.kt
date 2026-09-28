package com.example.data.repository

import com.example.data.local.AppPreferenceEntity
import com.example.data.local.DownloadedSongEntity
import com.example.data.local.FavoriteAlbumEntity
import com.example.data.local.FavoriteSongEntity
import com.example.data.local.LikedPlaylistEntity
import com.example.data.local.SongDao
import com.example.data.local.SongHistoryEntity
import com.example.data.local.UserPlaylistEntity
import com.example.data.local.UserPlaylistSongEntity
import com.example.data.remote.JioSaavnApiService
import com.example.domain.model.Album
import com.example.domain.model.HomeSection
import com.example.domain.model.ListeningEventType
import com.example.domain.model.Playlist
import com.example.domain.model.SectionItem
import com.example.domain.model.SectionItemType
import com.example.domain.model.Song
import com.example.domain.repository.MusicRepository
import com.example.util.MusicUtils
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepositoryImpl(
    private val apiService: JioSaavnApiService,
    private val songDao: SongDao
) : MusicRepository {

    // In-memory cache for Home sections
    private var cachedSections: List<HomeSection>? = null
    private var lastCacheTimestamp: Long = 0
    private val cacheTtlMs = 60 * 60 * 1000L // 1 hour cache

    override suspend fun getHomeSections(): Result<List<HomeSection>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedSections != null && (now - lastCacheTimestamp < cacheTtlMs)) {
            return@withContext Result.success(cachedSections!!)
        }

        try {
            coroutineScope {
                // All home sections are now PLAYLISTS
                val newReleasesDeferred = async { fetchSectionPlaylists("New Hindi Releases", 12) }
                val topChartsDeferred = async { fetchSectionPlaylists("Top Charts", 12) }
                val editorsPicksDeferred = async { fetchSectionPlaylists("Editor's Picks", 12) }
                val genresMoodsDeferred = async { fetchSectionPlaylists("Top Bollywood Hits", 12) }
                val freshHitsDeferred = async { fetchSectionPlaylists("Fresh Hits", 12) }
                val marathiDeferred = async { fetchSectionPlaylists("Marathi Hits", 12) }
                val popHindiDeferred = async { fetchSectionPlaylists("Hindi Pop Hits", 12) }
                val moodDeferred = async { fetchSectionPlaylists("Romantic Moods", 12) }

                val sections = listOf(
                    HomeSection(
                        id = "new_releases",
                        title = "New Releases",
                        itemType = SectionItemType.PLAYLIST,
                        items = newReleasesDeferred.await()
                    ),
                    HomeSection(
                        id = "top_charts",
                        title = "Top Charts",
                        itemType = SectionItemType.PLAYLIST,
                        items = topChartsDeferred.await()
                    ),
                    HomeSection(
                        id = "editors_picks",
                        title = "Editor's Picks",
                        itemType = SectionItemType.PLAYLIST,
                        items = editorsPicksDeferred.await()
                    ),
                    HomeSection(
                        id = "genres_moods",
                        title = "Top Genres and Moods",
                        itemType = SectionItemType.PLAYLIST,
                        items = genresMoodsDeferred.await()
                    ),
                    HomeSection(
                        id = "fresh_hits",
                        title = "Fresh Hits",
                        itemType = SectionItemType.PLAYLIST,
                        items = freshHitsDeferred.await()
                    ),
                    HomeSection(
                        id = "marathi_chitrahaar",
                        title = "Marathi Chitrahaar",
                        itemType = SectionItemType.PLAYLIST,
                        items = marathiDeferred.await()
                    ),
                    HomeSection(
                        id = "pop_hindi",
                        title = "New Releases Pop - Hindi",
                        itemType = SectionItemType.PLAYLIST,
                        items = popHindiDeferred.await()
                    ),
                    HomeSection(
                        id = "your_mood",
                        title = "Made For Your Mood",
                        itemType = SectionItemType.PLAYLIST,
                        items = moodDeferred.await()
                    )
                )

                cachedSections = sections
                lastCacheTimestamp = now
                Result.success(sections)
            }
        } catch (e: Exception) {
            // Return cached if available, otherwise fail gracefully
            cachedSections?.let { return@withContext Result.success(it) }
            Result.failure(e)
        }
    }

    override suspend fun getRecommendationCandidates(): List<Song> = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val trendingDeferred = async {
                    try {
                        val resp = apiService.searchSongs(query = "Trending Hindi", limit = 20)
                        resp.data?.results?.map { MusicUtils.toDomainSong(it) } ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val topDeferred = async {
                    try {
                        val resp = apiService.searchSongs(query = "Top Bollywood", limit = 20)
                        resp.data?.results?.map { MusicUtils.toDomainSong(it) } ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val list1 = trendingDeferred.await()
                val list2 = topDeferred.await()
                (list1 + list2).distinctBy { it.id }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchSectionAlbums(query: String, limit: Int): List<SectionItem> {
        return try {
            val response = apiService.searchAlbums(query = query, limit = limit)
            val results = response.data?.results ?: emptyList()
            results.map {
                val primaryArtists = it.artists?.primary?.mapNotNull { a -> a.name } ?: emptyList()
                val artistName = if (primaryArtists.isNotEmpty()) {
                    primaryArtists.joinToString(", ") { a -> MusicUtils.unescapeHtml(a) }
                } else {
                    it.artists?.all?.mapNotNull { a -> a.name }?.joinToString(", ") { a -> MusicUtils.unescapeHtml(a) }
                        ?: it.year?.let { y -> "Album • $y" }
                        ?: "Album"
                }
                SectionItem(
                    id = it.id,
                    title = MusicUtils.unescapeHtml(it.name),
                    subtitle = artistName,
                    artworkUrl = MusicUtils.selectBestImage(it.image),
                    type = SectionItemType.ALBUM,
                    songCount = it.songCount?.toIntOrNull() ?: 0,
                    year = it.year.orEmpty(),
                    language = it.language.orEmpty()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchSectionPlaylists(query: String, limit: Int, isChart: Boolean = false): List<SectionItem> {
        return try {
            val response = apiService.searchPlaylists(query = query, limit = limit)
            val results = response.data?.results ?: emptyList()
            results.map {
                val count = it.songCount?.toIntOrNull() ?: 0
                val countStr = if (count > 0) "$count Songs" else "Playlist"
                val subtitle = if (isChart) "Chart • $countStr" else "Playlist • $countStr"
                SectionItem(
                    id = it.id,
                    title = MusicUtils.unescapeHtml(it.name),
                    subtitle = subtitle,
                    artworkUrl = MusicUtils.selectBestImage(it.image),
                    type = if (isChart) SectionItemType.CHART else SectionItemType.PLAYLIST,
                    songCount = count,
                    language = it.language.orEmpty()
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getAlbumDetails(albumId: String): Result<Album> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAlbumById(albumId)
            val apiAlbum = response.data
            if (apiAlbum != null) {
                Result.success(MusicUtils.toDomainAlbum(apiAlbum))
            } else {
                Result.failure(NoSuchElementException("Album not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPlaylistDetails(playlistId: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPlaylistById(playlistId, limit = 50)
            val apiPlaylist = response.data
            if (apiPlaylist != null) {
                Result.success(MusicUtils.toDomainPlaylist(apiPlaylist))
            } else {
                Result.failure(NoSuchElementException("Playlist not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchSongs(query: String, limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchSongs(query = query, limit = limit)
            val songs = response.data?.results?.map { MusicUtils.toDomainSong(it) } ?: emptyList()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val queueUrl = "https://vyberadio.vyoma-apps.workers.dev/queue"

    override suspend fun getRadioQueue(songId: String, limit: Int): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val recentIds = songDao.getRecentHistory(50).map { it.songId }
            val seeds = (listOf(songId) + recentIds).distinct().take(3).joinToString(",")
            val exclude = recentIds.reversed().joinToString(",")

            val response = apiService.getQueue(url = queueUrl, seeds = seeds, exclude = exclude, limit = limit)
            val songs = response.data?.map { MusicUtils.toDomainSong(it) } ?: emptyList()

            if (songs.isNotEmpty()) {
                Result.success(songs)
            } else {
                // backup: purana suggestions endpoint
                val old = apiService.getSongSuggestions(id = songId, limit = limit)
                Result.success(old.data?.map { MusicUtils.toDomainSong(it) } ?: emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchAlbums(query: String, limit: Int): Result<List<Album>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchAlbums(query = query, limit = limit)
            val albums = response.data?.results?.map {
                Album(
                    id = it.id,
                    title = MusicUtils.unescapeHtml(it.name),
                    year = it.year.orEmpty(),
                    artworkUrl = MusicUtils.selectBestImage(it.image)
                )
            } ?: emptyList()
            Result.success(albums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchPlaylists(query: String, limit: Int): Result<List<Playlist>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchPlaylists(query = query, limit = limit)
            val playlists = response.data?.results?.map {
                Playlist(
                    id = it.id,
                    title = MusicUtils.unescapeHtml(it.name),
                    artworkUrl = MusicUtils.selectBestImage(it.image),
                    language = it.language.orEmpty()
                )
            } ?: emptyList()
            Result.success(playlists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getArtistTopSongs(artistId: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getArtistById(id = artistId, songCount = 15)
            val songs = response.data?.topSongs?.map { MusicUtils.toDomainSong(it) } ?: emptyList()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSongById(songId: String): Result<Song> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSongById(songId)
            val apiSong = response.data?.firstOrNull()
            if (apiSong != null) {
                val isFav = songDao.isFavorite(songId)
                Result.success(MusicUtils.toDomainSong(apiSong, isLiked = isFav))
            } else {
                Result.failure(NoSuchElementException("Song not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getFavorites(): Flow<List<Song>> {
        return songDao.getAllFavorites().map { list ->
            list.map {
                Song(
                    id = it.songId,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    artworkUrl = it.artworkUrl,
                    streamUrl = it.streamUrl,
                    durationSeconds = it.durationSeconds,
                    language = it.language,
                    isLiked = true
                )
            }
        }
    }

    override fun isFavorite(songId: String): Flow<Boolean> {
        return songDao.isFavoriteFlow(songId)
    }

    override suspend fun toggleFavorite(song: Song): Boolean = withContext(Dispatchers.IO) {
        val currentlyFav = songDao.isFavorite(song.id)
        if (currentlyFav) {
            songDao.removeFavorite(song.id)
            songDao.updateLikedInHistory(song.id, false)
            false
        } else {
            songDao.addFavorite(
                FavoriteSongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    artworkUrl = song.artworkUrl,
                    streamUrl = song.streamUrl,
                    durationSeconds = song.durationSeconds,
                    language = song.language
                )
            )
            songDao.updateLikedInHistory(song.id, true)
            true
        }
    }

    override suspend fun recordListeningEvent(song: Song, event: ListeningEventType) {
        withContext(Dispatchers.IO) {
            val existing = songDao.getHistoryBySongId(song.id)
            when (event) {
                ListeningEventType.PLAY_STARTED -> {
                    val isFav = songDao.isFavorite(song.id)
                    val entity = existing?.copy(
                        playCount = existing.playCount + 1,
                        lastPlayedTimestamp = System.currentTimeMillis(),
                        isLiked = isFav
                    ) ?: SongHistoryEntity(
                        songId = song.id,
                        title = song.title,
                        artist = song.artist,
                        artistId = song.artistId,
                        album = song.album,
                        artworkUrl = song.artworkUrl,
                        streamUrl = song.streamUrl,
                        durationSeconds = song.durationSeconds,
                        language = song.language,
                        playCount = 1,
                        isLiked = isFav
                    )
                    songDao.insertOrUpdateHistory(entity)
                }
                ListeningEventType.PLAY_COMPLETED -> {
                    songDao.incrementCompleted(song.id)
                }
                ListeningEventType.SKIPPED -> {
                    songDao.incrementSkip(song.id)
                }
                ListeningEventType.LIKED -> {
                    toggleFavorite(song)
                }
                ListeningEventType.UNLIKED -> {
                    toggleFavorite(song)
                }
                ListeningEventType.REPLAYED -> {
                    existing?.let {
                        songDao.insertOrUpdateHistory(it.copy(playCount = it.playCount + 1))
                    }
                }
                else -> {}
            }
        }
    }

    override suspend fun getRecentPlayedSongs(limit: Int): List<Song> = withContext(Dispatchers.IO) {
        songDao.getRecentHistory(limit).map {
            Song(
                id = it.songId,
                title = it.title,
                artist = it.artist,
                artistId = it.artistId,
                album = it.album,
                artworkUrl = it.artworkUrl,
                streamUrl = it.streamUrl,
                durationSeconds = it.durationSeconds,
                language = it.language,
                isLiked = it.isLiked
            )
        }
    }

    override suspend fun getQualifyingPlayCount(): Int = withContext(Dispatchers.IO) {
        songDao.getPreference(KEY_QUALIFYING_PLAYS)?.toIntOrNull() ?: 0
    }

    override suspend fun incrementQualifyingPlayCount(): Int = withContext(Dispatchers.IO) {
        val current = getQualifyingPlayCount()
        val next = current + 1
        songDao.setPreference(AppPreferenceEntity(KEY_QUALIFYING_PLAYS, next.toString()))
        next
    }

    override suspend fun resetQualifyingPlayCount() = withContext(Dispatchers.IO) {
        songDao.setPreference(AppPreferenceEntity(KEY_QUALIFYING_PLAYS, "0"))
    }

    override suspend fun getSavedHeroSongId(): String? = withContext(Dispatchers.IO) {
        songDao.getPreference(KEY_HERO_SONG_ID)
    }

    override suspend fun saveHeroSongId(songId: String) = withContext(Dispatchers.IO) {
        songDao.setPreference(AppPreferenceEntity(KEY_HERO_SONG_ID, songId))
    }

    override fun getSearchHistory(): Flow<List<String>> = songDao.getRecentSearchQueries()

    override suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            songDao.addSearchQueryAndTrim(trimmed, maxItems = 10)
        }
    }

    override suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
        songDao.deleteSearchQuery(query.trim())
    }

    override suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        songDao.clearAllSearchHistory()
    }

    // Favorite Albums
    override fun getFavoriteAlbums(): Flow<List<Album>> {
        return songDao.getAllFavoriteAlbums().map { list ->
            list.map {
                Album(
                    id = it.albumId,
                    title = it.title,
                    artist = it.artist,
                    artworkUrl = it.artworkUrl,
                    year = it.year,
                    songCount = it.songCount,
                    isLiked = true
                )
            }
        }
    }

    override fun isAlbumFavorite(albumId: String): Flow<Boolean> {
        return songDao.isAlbumFavoriteFlow(albumId)
    }

    override suspend fun toggleFavoriteAlbum(album: Album): Boolean = withContext(Dispatchers.IO) {
        val exists = songDao.isAlbumFavorite(album.id)
        if (exists) {
            songDao.removeFavoriteAlbum(album.id)
            false
        } else {
            songDao.addFavoriteAlbum(
                FavoriteAlbumEntity(
                    albumId = album.id,
                    title = album.title,
                    artist = album.artist,
                    artworkUrl = album.artworkUrl,
                    year = album.year,
                    songCount = album.songCount
                )
            )
            true
        }
    }

    // Downloads
    override fun getDownloadedSongs(): Flow<List<Song>> {
        return songDao.getAllDownloadedSongs().map { list ->
            list.map {
                Song(
                    id = it.songId,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    artworkUrl = it.artworkUrl,
                    streamUrl = it.streamUrl,
                    durationSeconds = it.durationSeconds
                )
            }
        }
    }

    override fun isSongDownloaded(songId: String): Flow<Boolean> {
        return songDao.isSongDownloadedFlow(songId)
    }

    override suspend fun toggleDownloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        val downloaded = songDao.isSongDownloaded(song.id)
        if (downloaded) {
            songDao.removeDownloadedSong(song.id)
            false
        } else {
            val estimatedBytes = (song.durationSeconds * 32_000L).coerceAtLeast(3_800_000L)
            songDao.addDownloadedSong(
                DownloadedSongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    artworkUrl = song.artworkUrl,
                    streamUrl = song.streamUrl,
                    durationSeconds = song.durationSeconds,
                    sizeBytes = estimatedBytes
                )
            )
            true
        }
    }

    override suspend fun removeDownloadedSong(songId: String) = withContext(Dispatchers.IO) {
        songDao.removeDownloadedSong(songId)
    }

    override suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        songDao.clearAllDownloadedSongs()
    }

    override fun getTotalDownloadedSizeBytes(): Flow<Long> {
        return songDao.getTotalDownloadedSizeBytes()
    }

    // User Playlists ("My Playlists")
    override fun getUserPlaylists(): Flow<List<Playlist>> {
        return songDao.getAllUserPlaylists().map { list ->
            list.map {
                Playlist(
                    id = it.playlistId,
                    title = it.title,
                    description = it.description,
                    artworkUrl = it.artworkUrl,
                    isUserCreated = true,
                    createdTimestamp = it.createdTimestamp
                )
            }
        }
    }

    override suspend fun createUserPlaylist(title: String, description: String): String = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        songDao.insertUserPlaylist(
            UserPlaylistEntity(
                playlistId = newId,
                title = title.trim(),
                description = description.trim(),
                createdTimestamp = System.currentTimeMillis()
            )
        )
        newId
    }

    override suspend fun deleteUserPlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        songDao.deleteUserPlaylistAndSongs(playlistId)
    }

    override fun getUserPlaylistSongs(playlistId: String): Flow<List<Song>> {
        return songDao.getSongsForPlaylistFlow(playlistId).map { list ->
            list.map {
                Song(
                    id = it.songId,
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    artworkUrl = it.artworkUrl,
                    streamUrl = it.streamUrl,
                    durationSeconds = it.durationSeconds
                )
            }
        }
    }

    override suspend fun addSongToUserPlaylist(playlistId: String, song: Song) = withContext(Dispatchers.IO) {
        songDao.addSongToPlaylist(
            UserPlaylistSongEntity(
                playlistId = playlistId,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                artworkUrl = song.artworkUrl,
                streamUrl = song.streamUrl,
                durationSeconds = song.durationSeconds
            )
        )
    }

    override suspend fun removeSongFromUserPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        songDao.removeSongFromPlaylist(playlistId, songId)
    }

    // Liked Playlists ("Liked Playlists")
    override fun getLikedPlaylists(): Flow<List<Playlist>> {
        return songDao.getAllLikedPlaylists().map { list ->
            list.map {
                Playlist(
                    id = it.playlistId,
                    title = it.title,
                    subtitle = it.subtitle,
                    artworkUrl = it.artworkUrl,
                    songCount = it.songCount,
                    isLiked = true
                )
            }
        }
    }

    override fun isPlaylistLiked(playlistId: String): Flow<Boolean> {
        return songDao.isPlaylistLikedFlow(playlistId)
    }

    override suspend fun toggleLikedPlaylist(playlist: Playlist): Boolean = withContext(Dispatchers.IO) {
        val exists = songDao.isPlaylistLiked(playlist.id)
        if (exists) {
            songDao.removeLikedPlaylist(playlist.id)
            false
        } else {
            songDao.addLikedPlaylist(
                LikedPlaylistEntity(
                    playlistId = playlist.id,
                    title = playlist.title,
                    subtitle = playlist.subtitle,
                    artworkUrl = playlist.artworkUrl,
                    songCount = playlist.songCount
                )
            )
            true
        }
    }

    companion object {
        private const val KEY_QUALIFYING_PLAYS = "qualifying_plays_count"
        private const val KEY_HERO_SONG_ID = "hero_song_id"
    }
}
