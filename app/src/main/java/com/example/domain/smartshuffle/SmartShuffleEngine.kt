package com.example.domain.smartshuffle

import com.example.domain.model.Song
import kotlin.math.abs

enum class MoodCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val colorHex: Long
) {
    CHILL(
        id = "chill",
        title = "Chill",
        emoji = "☕",
        description = "Mellow acoustic, late-night & ambient vibes",
        colorHex = 0xFF06B6D4
    ),
    UPBEAT(
        id = "upbeat",
        title = "Upbeat",
        emoji = "⚡",
        description = "Groovy, danceable & high-energy rhythms",
        colorHex = 0xFFFF6436
    ),
    INTENSE(
        id = "intense",
        title = "Intense",
        emoji = "🔥",
        description = "Driving beats, powerful bass & fast cadence",
        colorHex = 0xFFF43F5E
    ),
    SOULFUL(
        id = "soulful",
        title = "Soulful",
        emoji = "🌙",
        description = "Deep, introspective & emotionally resonant harmonies",
        colorHex = 0xFFA855F7
    ),
    EUPHORIC(
        id = "euphoric",
        title = "Euphoric",
        emoji = "✨",
        description = "Soaring, radiant & uplifting melodies",
        colorHex = 0xFFFFB800
    );

    val displayName: String
        get() = "$emoji $title"
}

data class SongMoodProfile(
    val song: Song,
    val energy: Float,
    val valence: Float,
    val danceability: Float,
    val estimatedBpm: Int,
    val acousticness: Float,
    val primaryMood: MoodCategory
)

object SmartShuffleEngine {

    private val chillKeywords = setOf(
        "slow", "chill", "night", "rain", "sleep", "acoustic", "lofi", "ambient",
        "quiet", "breeze", "calm", "coffee", "sunset", "autumn", "lullaby", "soft",
        "whisper", "peace", "waves", "moon", "stars", "gentle", "lazy", "dreamy",
        "piano", "unplugged", "serenade", "winter", "cloud", "breathe"
    )

    private val upbeatKeywords = setOf(
        "dance", "club", "party", "disco", "bounce", "groove", "funk", "shake",
        "summer", "beat", "jump", "pop", "move", "rhythm", "fun", "celebrate",
        "radiant", "sunshine", "vibe", "groovin", "uptown", "roller", "city",
        "neon", "bassline", "drive", "shine", "spark", "electric", "heat"
    )

    private val intenseKeywords = setOf(
        "rock", "hard", "metal", "fire", "bass", "trap", "drill", "rage",
        "drop", "blast", "heavy", "power", "wild", "fast", "speed", "dark",
        "storm", "thunder", "scream", "war", "beast", "monster", "cyber",
        "overdrive", "raw", "hyper", "clash", "strike"
    )

    private val soulfulKeywords = setOf(
        "sad", "tears", "alone", "blue", "cry", "heart", "break", "hurt",
        "lost", "memory", "shadow", "goodbye", "pain", "hollow", "soul",
        "blues", "slowly", "miss", "regret", "sorrow", "bleeding", "distant",
        "lonely", "broken", "empty", "distance", "deep", "midnight"
    )

    private val euphoricKeywords = setOf(
        "love", "light", "dream", "sky", "fly", "alive", "magic", "paradise",
        "golden", "forever", "miracle", "wonder", "soar", "heaven", "higher",
        "bliss", "infinity", "wings", "glory", "angel", "celestial", "eternal",
        "glow", "radiance", "sweet", "hope", "horizon"
    )

    fun analyzeMood(song: Song): SongMoodProfile {
        val text = "${song.title} ${song.album} ${song.artist}".lowercase()

        val tokens = text
            .split(Regex("[\\s,_\\-\\(\\)\\[\\]\\.]+"))
            .filter { it.isNotBlank() }

        var chillScore = tokens.count { it in chillKeywords } * 1.5f
        var upbeatScore = tokens.count { it in upbeatKeywords } * 1.5f
        var intenseScore = tokens.count { it in intenseKeywords } * 1.6f
        var soulfulScore = tokens.count { it in soulfulKeywords } * 1.5f
        var euphoricScore = tokens.count { it in euphoricKeywords } * 1.5f

        val hash = abs((song.id + song.title).hashCode())

        val hashFactor1 = (hash % 100) / 100f
        val hashFactor2 = ((hash / 100) % 100) / 100f
        val hashFactor3 = ((hash / 10000) % 100) / 100f

        val duration = song.durationSeconds

        if (duration in 120..195) {
            upbeatScore += 0.8f
        } else if (duration > 260) {
            chillScore += 0.6f
            soulfulScore += 0.6f
        }

        chillScore += hashFactor1 * 0.9f
        upbeatScore += hashFactor2 * 0.9f
        intenseScore += (hashFactor1 * hashFactor2) * 0.9f
        soulfulScore += hashFactor3 * 0.9f
        euphoricScore += ((hashFactor2 + hashFactor3) / 2f) * 0.9f

        val scores = listOf(
            MoodCategory.CHILL to chillScore,
            MoodCategory.UPBEAT to upbeatScore,
            MoodCategory.INTENSE to intenseScore,
            MoodCategory.SOULFUL to soulfulScore,
            MoodCategory.EUPHORIC to euphoricScore
        )

        val primaryMood =
            scores.maxByOrNull { it.second }?.first ?: MoodCategory.CHILL

        val energy = when (primaryMood) {
            MoodCategory.CHILL -> 0.18f + hashFactor1 * 0.22f
            MoodCategory.SOULFUL -> 0.28f + hashFactor2 * 0.25f
            MoodCategory.EUPHORIC -> 0.65f + hashFactor3 * 0.25f
            MoodCategory.UPBEAT -> 0.72f + hashFactor1 * 0.24f
            MoodCategory.INTENSE -> 0.85f + hashFactor2 * 0.15f
        }.coerceIn(0.05f, 0.98f)

        val valence = when (primaryMood) {
            MoodCategory.SOULFUL -> 0.15f + hashFactor3 * 0.25f
            MoodCategory.INTENSE -> 0.40f + hashFactor1 * 0.30f
            MoodCategory.CHILL -> 0.50f + hashFactor2 * 0.25f
            MoodCategory.UPBEAT -> 0.75f + hashFactor3 * 0.22f
            MoodCategory.EUPHORIC -> 0.85f + hashFactor1 * 0.15f
        }.coerceIn(0.05f, 0.98f)

        val danceability = when (primaryMood) {
            MoodCategory.CHILL -> 0.30f + hashFactor3 * 0.20f
            MoodCategory.SOULFUL -> 0.25f + hashFactor1 * 0.20f
            MoodCategory.EUPHORIC -> 0.65f + hashFactor2 * 0.25f
            MoodCategory.INTENSE -> 0.70f + hashFactor3 * 0.25f
            MoodCategory.UPBEAT -> 0.82f + hashFactor1 * 0.18f
        }.coerceIn(0.10f, 0.98f)

        val acousticness = when (primaryMood) {
            MoodCategory.CHILL -> 0.65f + hashFactor2 * 0.30f
            MoodCategory.SOULFUL -> 0.55f + hashFactor3 * 0.30f
            MoodCategory.EUPHORIC -> 0.25f + hashFactor1 * 0.25f
            MoodCategory.UPBEAT -> 0.15f + hashFactor2 * 0.20f
            MoodCategory.INTENSE -> 0.05f + hashFactor3 * 0.15f
        }.coerceIn(0.02f, 0.98f)

        val estimatedBpm =
            (70 + (energy * 50f) + (danceability * 25f))
                .toInt()
                .coerceIn(68, 155)

        return SongMoodProfile(
            song = song,
            energy = energy,
            valence = valence,
            danceability = danceability,
            estimatedBpm = estimatedBpm,
            acousticness = acousticness,
            primaryMood = primaryMood
        )
    }

    fun generateCohesiveSequence(
        songs: List<Song>,
        startingSongId: String? = null,
        preferredMood: MoodCategory? = null
    ): List<Song> {

        if (songs.size <= 2) return songs

        val profiles = songs.map { analyzeMood(it) }

        val candidateProfiles = if (preferredMood != null) {
            val matching = profiles.filter {
                it.primaryMood == preferredMood
            }

            val others = profiles.filter {
                it.primaryMood != preferredMood
            }

            if (matching.isNotEmpty()) {
                matching + others
            } else {
                profiles
            }
        } else {
            profiles
        }

        val unvisited = candidateProfiles.toMutableList()
        val result = mutableListOf<SongMoodProfile>()

        val anchor = if (!startingSongId.isNullOrBlank()) {
            unvisited.firstOrNull {
                it.song.id == startingSongId
            } ?: unvisited.first()
        } else if (preferredMood != null) {
            unvisited.firstOrNull {
                it.primaryMood == preferredMood
            } ?: unvisited.first()
        } else {
            unvisited.minByOrNull {
                abs(it.energy - 0.5f)
            } ?: unvisited.first()
        }

        result.add(anchor)
        unvisited.remove(anchor)

        while (unvisited.isNotEmpty()) {
            val current = result.last()

            val scoredCandidates = unvisited
                .map { candidate ->
                    val distance = calculateTransitionCost(
                        current,
                        candidate
                    )
                    candidate to distance
                }
                .sortedBy { it.second }

            val topPool = scoredCandidates.take(
                2.coerceAtMost(scoredCandidates.size)
            )

            val selected = topPool.random().first

            result.add(selected)
            unvisited.remove(selected)
        }

        return result.map { it.song }
    }

    private fun calculateTransitionCost(
        a: SongMoodProfile,
        b: SongMoodProfile
    ): Float {

        val energyDelta =
            abs(a.energy - b.energy) * 2.5f

        val valenceDelta =
            abs(a.valence - b.valence) * 1.8f

        // FIX: SongMoodProfile uses estimatedBpm
        val bpmDelta =
            (abs(a.estimatedBpm - b.estimatedBpm) / 45f) * 1.4f

        val acousticDelta =
            abs(a.acousticness - b.acousticness)

        val sameArtistPenalty =
            if (
                a.song.artist.equals(
                    b.song.artist,
                    ignoreCase = true
                )
            ) {
                3.0f
            } else {
                0f
            }

        val moodBonus =
            if (a.primaryMood == b.primaryMood) {
                -0.4f
            } else {
                0f
            }

        return energyDelta +
                valenceDelta +
                bpmDelta +
                acousticDelta +
                sameArtistPenalty +
                moodBonus
    }

    fun getMoodBreakdown(
        songs: List<Song>
    ): Map<MoodCategory, Int> {

        val counts = mutableMapOf<MoodCategory, Int>()

        for (song in songs) {
            val profile = analyzeMood(song)

            counts[profile.primaryMood] =
                counts.getOrDefault(
                    profile.primaryMood,
                    0
                ) + 1
        }

        return counts
    }
}
