package com.example.data.update

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppUpdateConfig(
    @Json(name = "schema_version") val schemaVersion: Int = 1,
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "update_mode") val updateMode: String = "none",
    @Json(name = "latest_release_version") val latestReleaseVersion: String = "1.0.0",
    @Json(name = "minimum_supported_version") val minimumSupportedVersion: String = "1.0.0",
    @Json(name = "config_version") val configVersion: Int = 1,
    @Json(name = "release_notes") val releaseNotes: String? = null,
    @Json(name = "config_url") val configUrl: String? = null,
    @Json(name = "remote_config") val remoteConfig: RemoteConfigPayload? = null
) {
    companion object {
        const val MODE_NONE = "none"
        const val MODE_SILENT_CONFIG = "silent_config"
        const val MODE_MANDATORY = "mandatory"
    }
}

@JsonClass(generateAdapter = true)
data class RemoteConfigPayload(
    @Json(name = "custom_api_base_url") val customApiBaseUrl: String? = null,
    @Json(name = "announcement_banner") val announcementBanner: AnnouncementBannerConfig? = null,
    @Json(name = "feature_flags") val featureFlags: Map<String, Boolean>? = null
)

@JsonClass(generateAdapter = true)
data class AnnouncementBannerConfig(
    @Json(name = "id") val id: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "type") val type: String = "info",
    @Json(name = "action_url") val actionUrl: String? = null,
    @Json(name = "enabled") val enabled: Boolean = false
)
