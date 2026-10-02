package com.example.util

import android.os.Build
import android.text.Html
import com.example.data.model.ApiAlbum
import com.example.data.model.ApiDownloadItem
import com.example.data.model.ApiImageItem
import com.example.data.model.ApiPlaylist
import com.example.data.model.ApiSong
import com.example.domain.model.Album
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import java.util.Locale

object MusicUtils {

    fun unescapeHtml(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        var result = text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&apos;", "'")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#x27;", "'")

        // Handle generic numeric character references &#123;
        val regex = Regex("&#(\\d+);")
        result = regex.replace(result) { matchResult ->
            val code = matchResult.groupValues[1].toIntOrNull()
            if (code != null) code.toChar().toString() else matchResult.value
        }
        return result.trim()
    }

    fun formatDuration(seconds: Int): String {
        val totalSecs = if (seconds < 0) 0 else seconds
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    fun formatDurationMs(millis: Long): String {
        val totalSecs = (millis / 1000).toInt()
        return formatDuration(totalSecs)
    }

    fun selectBestImage(images: List<ApiImageItem>?): String {
        if (images.isNullOrEmpty()) return ""
        // Priority: 500x500 -> 150x150 -> 50x50 -> last -> first
        val best = images.find { it.quality == "500x500" }
            ?: images.find { it.quality == "150x150" }
            ?: images.lastOrNull { !it.url.isNullOrBlank() }
        return best?.url.orEmpty()
    }

    fun selectBestAudioUrl(downloads: List<ApiDownloadItem>?, preferredQuality: String? = null): String {
        if (downloads.isNullOrEmpty()) return ""

        val available = downloads
            .filter { !it.url.isNullOrBlank() && !it.quality.isNullOrBlank() }
            .mapNotNull { item ->
                val bitrate = Regex("\\d+").find(item.quality!!)?.value?.toIntOrNull()
                if (bitrate != null) bitrate to item else null
            }
            .sortedBy { it.first }

        if (available.isEmpty()) {
            return downloads.lastOrNull { !it.url.isNullOrBlank() }?.url.orEmpty()
        }

        val pref = preferredQuality?.lowercase(Locale.US).orEmpty()
        val target = Regex("\\d+").find(pref)?.value?.toIntOrNull()

        val selected = when {
            target != null -> {
                // Prefer the exact API-provided bitrate. If it is unavailable,
                // use the nearest higher bitrate, then the highest available.
                available.firstOrNull { it.first == target }
                    ?: available.firstOrNull { it.first > target }
                    ?: available.lastOrNull()
            }
            pref.contains("auto") || pref.isBlank() -> available.lastOrNull()
            else -> available.lastOrNull()
        }

        return selected?.second?.url.orEmpty()
    }

    fun applyQualityToUrl(url: String, preferredQuality: String?): String {
        if (url.isBlank() || url.startsWith("file://") || url.startsWith("/")) return url

        val pref = preferredQuality?.lowercase(Locale.US).orEmpty()
        val targetBitrate = Regex("\\d+").find(pref)?.value
            ?: if (pref.contains("auto")) "320" else return url

        val regex = Regex("_(320|160|96|48|12)\\.mp4(\\?.*)?$")
        return if (regex.containsMatchIn(url)) {
            regex.replace(url) { matchResult ->
                val query = matchResult.groupValues[2]
                "_${targetBitrate}.mp4$query"
            }
        } else {
            url
        }
    }

    fun toDomainSong(apiSong: ApiSong, isLiked: Boolean = false, preferredQuality: String? = null): Song {
        val primaryArtists = apiSong.artists?.primary?.mapNotNull { it.name } ?: emptyList()
        val allArtists = if (primaryArtists.isNotEmpty()) {
            primaryArtists.joinToString(", ") { unescapeHtml(it) }
        } else {
            apiSong.artists?.all?.mapNotNull { it.name }?.joinToString(", ") { unescapeHtml(it) }
                ?: "Unknown Artist"
        }

        val firstArtistId = apiSong.artists?.primary?.firstOrNull()?.id
            ?: apiSong.artists?.all?.firstOrNull()?.id

        val durationInt = try {
            apiSong.duration?.toIntOrNull() ?: 0
        } catch (_: Exception) {
            0
        }

        return Song(
            id = apiSong.id,
            title = unescapeHtml(apiSong.name),
            artist = allArtists,
            artistId = firstArtistId,
            album = unescapeHtml(apiSong.album?.name.orEmpty()),
            albumId = apiSong.album?.id,
            durationSeconds = durationInt,
            year = apiSong.year.orEmpty(),
            language = apiSong.language.orEmpty(),
            artworkUrl = selectBestImage(apiSong.image),
            streamUrl = selectBestAudioUrl(apiSong.downloadUrl, preferredQuality),
            hasLyrics = apiSong.hasLyrics == true,
            lyricsId = apiSong.lyricsId,
            isLiked = isLiked
        )
    }

    fun toDomainAlbum(apiAlbum: ApiAlbum, preferredQuality: String? = null): Album {
        val primaryArtists = apiAlbum.artists?.primary?.mapNotNull { it.name } ?: emptyList()
        val artistName = if (primaryArtists.isNotEmpty()) {
            primaryArtists.joinToString(", ") { unescapeHtml(it) }
        } else {
            apiAlbum.artists?.all?.mapNotNull { it.name }?.joinToString(", ") { unescapeHtml(it) }
                ?: ""
        }
        val domainSongs = apiAlbum.songs?.map { toDomainSong(it, preferredQuality = preferredQuality) } ?: emptyList()
        val count = apiAlbum.songCount?.toIntOrNull() ?: domainSongs.size
        return Album(
            id = apiAlbum.id,
            title = unescapeHtml(apiAlbum.name),
            artist = artistName,
            artworkUrl = selectBestImage(apiAlbum.image),
            year = apiAlbum.year.orEmpty(),
            songCount = if (count > 0) count else domainSongs.size,
            language = apiAlbum.language.orEmpty(),
            songs = domainSongs
        )
    }

    fun toDomainPlaylist(apiPlaylist: ApiPlaylist, preferredQuality: String? = null): Playlist {
        val domainSongs = apiPlaylist.songs?.map { toDomainSong(it, preferredQuality = preferredQuality) } ?: emptyList()
        val count = apiPlaylist.songCount?.toIntOrNull() ?: domainSongs.size
        return Playlist(
            id = apiPlaylist.id,
            title = unescapeHtml(apiPlaylist.name),
            artworkUrl = selectBestImage(apiPlaylist.image),
            songCount = if (count > 0) count else domainSongs.size,
            language = apiPlaylist.language.orEmpty(),
            songs = domainSongs
        )
    }
}
