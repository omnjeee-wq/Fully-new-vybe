package com.example.domain.recommendation

import com.example.data.local.SongDao
import com.example.data.local.SongHistoryEntity
import com.example.domain.model.Song
import com.example.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendationEngine(
    private val repository: MusicRepository,
    private val songDao: SongDao
) {

    /**
     * Recomputes the personalized hero banner track.
     * Takes candidate songs from recent listening, top artists, or trending API queries.
     */
    suspend fun selectHeroTrack(candidatePool: List<Song>): Song? = withContext(Dispatchers.IO) {
        if (candidatePool.isEmpty()) return@withContext null

        val history = songDao.getRecentHistory(30)
        if (history.isEmpty()) {
            // Cold start: pick first high quality candidate
            return@withContext candidatePool.firstOrNull()
        }

        val scored = candidatePool.map { song ->
            val score = calculateBannerScore(song, history)
            song to score
        }.sortedByDescending { it.second }

        // Select the top scoring song, avoiding extreme repetition if possible
        val savedId = repository.getSavedHeroSongId()
        val candidate = scored.firstOrNull { it.first.id != savedId }?.first
            ?: scored.firstOrNull()?.first
            ?: candidatePool.firstOrNull()

        candidate?.let {
            repository.saveHeroSongId(it.id)
        }
        candidate
    }

    /**
     * Scores and ranks candidates for "Recommend For You" section.
     */
    suspend fun generateRecommendations(candidatePool: List<Song>, limit: Int = 12): List<Song> = withContext(Dispatchers.IO) {
    if (candidatePool.isEmpty()) return@withContext emptyList()

    val history = songDao.getRecentHistory(50)
    val basePool = if (history.isEmpty()) {
        candidatePool.shuffled()
    } else {
        val scored = candidatePool.map { song -> song to calculateRecommendationScore(song, history) }
            .sortedByDescending { it.second }
            .map { it.first }
        val topSlice = scored.take((limit * 2.5).toInt().coerceAtLeast(limit))
        topSlice.shuffled()
    }

    applyControlledDiversity(basePool, limit = limit)
    }
    
    private fun calculateRecommendationScore(song: Song, history: List<SongHistoryEntity>): Double {
        var score = 0.0

        val matchedSong = history.find { it.songId == song.id }
        if (matchedSong != null) {
            // Completed plays give high affinity
            score += matchedSong.completedCount * 3.0
            // Replay bonus
            score += (matchedSong.playCount - 1) * 1.5
            // Liked bonus
            if (matchedSong.isLiked) score += 5.0
            // Skip penalty
            score -= matchedSong.skipCount * 3.5
            // Repetition penalty for the exact same song if played very recently
            val recencyHours = (System.currentTimeMillis() - matchedSong.lastPlayedTimestamp) / (1000.0 * 3600.0)
            if (recencyHours < 2.0) {
                score -= 4.0
            }
        }

        // Artist affinity: user listening to this artist
        val artistPlays = history.count { it.artist.contains(song.artist, ignoreCase = true) || (song.artistId != null && it.artistId == song.artistId) }
        score += artistPlays * 2.5

        // Language affinity
        if (song.language.isNotBlank()) {
            val languagePlays = history.count { it.language.equals(song.language, ignoreCase = true) }
            score += languagePlays * 1.2
        }

        return score
    }

    private fun calculateBannerScore(song: Song, history: List<SongHistoryEntity>): Double {
        val baseScore = calculateRecommendationScore(song, history)
        // High artist affinity is favored for the hero banner "Personalised Track Of [Artist]"
        val artistMatches = history.filter { it.artist.contains(song.artist, ignoreCase = true) }
        val artistBonus = if (artistMatches.isNotEmpty()) 4.0 else 0.0
        return baseScore + artistBonus
    }

    /**
     * Prevents consecutive songs from the same artist/album to ensure diversity.
     */
    private fun applyControlledDiversity(songs: List<Song>, limit: Int): List<Song> {
        val result = mutableListOf<Song>()
        val seenArtists = mutableMapOf<String, Int>()

        val maxPerArtist = if (limit > 8) 3 else 2
        for (song in songs) {
            val count = seenArtists.getOrDefault(song.artist, 0)
            if (count < maxPerArtist) { // Allow at most 2-3 per artist
                result.add(song)
                seenArtists[song.artist] = count + 1
            }
            if (result.size >= limit) break
        }

        // Fill remaining if needed
        if (result.size < limit) {
            for (song in songs) {
                if (!result.contains(song)) {
                    result.add(song)
                    if (result.size >= limit) break
                }
            }
        }

        return result
    }
}
