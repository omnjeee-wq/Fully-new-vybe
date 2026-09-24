package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T?,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiSearchResult<T>(
    @Json(name = "total") val total: Int? = null,
    @Json(name = "start") val start: Int? = null,
    @Json(name = "results") val results: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ApiImageItem(
    @Json(name = "quality") val quality: String?,
    @Json(name = "url") val url: String?
)

@JsonClass(generateAdapter = true)
data class ApiDownloadItem(
    @Json(name = "quality") val quality: String?,
    @Json(name = "url") val url: String?
)

@JsonClass(generateAdapter = true)
data class ApiAlbumRef(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiArtistRef(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "image") val image: List<ApiImageItem>? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiArtistsGroup(
    @Json(name = "primary") val primary: List<ApiArtistRef>? = null,
    @Json(name = "featured") val featured: List<ApiArtistRef>? = null,
    @Json(name = "all") val all: List<ApiArtistRef>? = null
)

@JsonClass(generateAdapter = true)
data class ApiSong(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "explicitContent") val explicitContent: Boolean? = null,
    @Json(name = "playCount") val playCount: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "hasLyrics") val hasLyrics: Boolean? = null,
    @Json(name = "lyricsId") val lyricsId: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "copyright") val copyright: String? = null,
    @Json(name = "album") val album: ApiAlbumRef? = null,
    @Json(name = "artists") val artists: ApiArtistsGroup? = null,
    @Json(name = "image") val image: List<ApiImageItem>? = null,
    @Json(name = "downloadUrl") val downloadUrl: List<ApiDownloadItem>? = null
)

@JsonClass(generateAdapter = true)
data class ApiPlaylist(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "image") val image: List<ApiImageItem>? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "songCount") val songCount: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "explicitContent") val explicitContent: Boolean? = null,
    @Json(name = "songs") val songs: List<ApiSong>? = null
)

@JsonClass(generateAdapter = true)
data class ApiAlbum(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "year") val year: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "image") val image: List<ApiImageItem>? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "songCount") val songCount: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "artists") val artists: ApiArtistsGroup? = null,
    @Json(name = "songs") val songs: List<ApiSong>? = null
)

@JsonClass(generateAdapter = true)
data class ApiArtistDetails(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "image") val image: List<ApiImageItem>? = null,
    @Json(name = "topSongs") val topSongs: List<ApiSong>? = null,
    @Json(name = "topAlbums") val topAlbums: List<ApiAlbum>? = null
)
