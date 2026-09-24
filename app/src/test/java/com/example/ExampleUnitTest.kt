package com.example

import com.example.data.model.ApiDownloadItem
import com.example.data.model.ApiImageItem
import com.example.util.MusicUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUnescapeHtml() {
        val raw = "&quot;Hero&quot; &amp; &apos;Vibe&#039;"
        val decoded = MusicUtils.unescapeHtml(raw)
        assertEquals("\"Hero\" & 'Vibe'", decoded)
    }

    @Test
    fun testFormatDuration() {
        assertEquals("0:00", MusicUtils.formatDuration(0))
        assertEquals("3:11", MusicUtils.formatDuration(191))
        assertEquals("1:05", MusicUtils.formatDuration(65))
    }

    @Test
    fun testSelectBestImage() {
        val images = listOf(
            ApiImageItem(quality = "50x50", url = "https://img.com/50.jpg"),
            ApiImageItem(quality = "500x500", url = "https://img.com/500.jpg"),
            ApiImageItem(quality = "150x150", url = "https://img.com/150.jpg")
        )
        val best = MusicUtils.selectBestImage(images)
        assertEquals("https://img.com/500.jpg", best)
    }

    @Test
    fun testSelectBestAudioUrl() {
        val audio = listOf(
            ApiDownloadItem(quality = "96kbps", url = "https://cdn.com/96.mp4"),
            ApiDownloadItem(quality = "320kbps", url = "https://cdn.com/320.mp4"),
            ApiDownloadItem(quality = "160kbps", url = "https://cdn.com/160.mp4")
        )
        val best = MusicUtils.selectBestAudioUrl(audio)
        assertEquals("https://cdn.com/320.mp4", best)
    }
}
