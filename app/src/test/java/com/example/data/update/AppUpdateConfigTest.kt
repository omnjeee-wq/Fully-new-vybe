package com.example.data.update

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateConfigTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(AppUpdateConfig::class.java)

    @Test
    fun testParseNoneModeConfig() {
        val json = """
        {
            "schema_version": 1,
            "enabled": true,
            "update_mode": "none",
            "latest_release_version": "1.0.0",
            "minimum_supported_version": "1.0.0",
            "config_version": 1,
            "release_notes": "Initial release"
        }
        """.trimIndent()

        val config = adapter.fromJson(json)
        assertNotNull(config)
        assertEquals("none", config?.updateMode)
        assertEquals("1.0.0", config?.latestReleaseVersion)
        assertEquals("1.0.0", config?.minimumSupportedVersion)
        assertTrue(config?.enabled == true)
    }

    @Test
    fun testParseSilentConfigWithPayload() {
        val json = """
        {
            "schema_version": 1,
            "enabled": true,
            "update_mode": "silent_config",
            "latest_release_version": "1.0.5",
            "minimum_supported_version": "1.0.0",
            "config_version": 3,
            "release_notes": "Silent performance flags update",
            "remote_config": {
                "custom_api_base_url": "https://api.vybe-music.internal/",
                "announcement_banner": {
                    "id": "notice_42",
                    "message": "Special Weekend Live Stream Available!",
                    "type": "promo",
                    "action_url": "https://t.me/VyomaOfficial",
                    "enabled": true
                },
                "feature_flags": {
                    "enable_high_res_audio": true,
                    "enable_lyrics": false
                }
            }
        }
        """.trimIndent()

        val config = adapter.fromJson(json)
        assertNotNull(config)
        assertEquals("silent_config", config?.updateMode)
        assertNotNull(config?.remoteConfig)
        assertEquals("https://api.vybe-music.internal/", config?.remoteConfig?.customApiBaseUrl)

        val banner = config?.remoteConfig?.announcementBanner
        assertNotNull(banner)
        assertEquals("notice_42", banner?.id)
        assertTrue(banner?.enabled == true)
        assertEquals("promo", banner?.type)

        val flags = config?.remoteConfig?.featureFlags
        assertNotNull(flags)
        assertEquals(true, flags?.get("enable_high_res_audio"))
        assertEquals(false, flags?.get("enable_lyrics"))
    }

    @Test
    fun testParseMandatoryUpdateConfig() {
        val json = """
        {
            "schema_version": 1,
            "enabled": true,
            "update_mode": "mandatory",
            "latest_release_version": "2.0.0",
            "minimum_supported_version": "2.0.0",
            "config_version": 5,
            "release_notes": "Complete engine overhaul with new audio pipeline."
        }
        """.trimIndent()

        val config = adapter.fromJson(json)
        assertNotNull(config)
        assertEquals("mandatory", config?.updateMode)
        assertEquals("2.0.0", config?.minimumSupportedVersion)

        // Verify that installed version 1.0.0 is detected as outdated
        assertTrue(SemVer.isLessThan("1.0.0", config?.minimumSupportedVersion))
        // Verify that updated version 2.0.0 is valid
        assertFalse(SemVer.isLessThan("2.0.0", config?.minimumSupportedVersion))
    }
}
