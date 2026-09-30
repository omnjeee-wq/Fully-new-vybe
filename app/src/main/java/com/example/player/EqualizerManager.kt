package com.example.player

import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.common.C

object EqualizerManager {
    private const val TAG = "EqualizerManager"

    private var equalizer: Equalizer? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var isEnabled: Boolean = false
    private val bandGains = IntArray(5) { 0 }

    suspend fun restoreFromDatabase(songDao: com.example.data.local.SongDao) {
        val eqEnabled = songDao.getPreference("pref_equalizer_enabled")?.toBooleanStrictOrNull() ?: false
        val b0 = songDao.getPreference("pref_eq_band_0")?.toIntOrNull() ?: 0
        val b1 = songDao.getPreference("pref_eq_band_1")?.toIntOrNull() ?: 0
        val b2 = songDao.getPreference("pref_eq_band_2")?.toIntOrNull() ?: 0
        val b3 = songDao.getPreference("pref_eq_band_3")?.toIntOrNull() ?: 0
        val b4 = songDao.getPreference("pref_eq_band_4")?.toIntOrNull() ?: 0
        synchronized(this) {
            isEnabled = eqEnabled
            bandGains[0] = b0
            bandGains[1] = b1
            bandGains[2] = b2
            bandGains[3] = b3
            bandGains[4] = b4
            applyAll()
        }
    }

    @Synchronized
    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return
        if (equalizer != null && currentSessionId == audioSessionId) return

        release()
        try {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            currentSessionId = audioSessionId
            applyAll()
            Log.d(TAG, "Equalizer successfully attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Equalizer for session $audioSessionId: ${e.message}")
        }
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error setting equalizer enabled: ${e.message}")
        }
    }

    @Synchronized
    fun setBandGain(bandIndex: Int, gainDb: Int) {
        if (bandIndex in bandGains.indices) {
            bandGains[bandIndex] = gainDb
        }
        applyBandGain(bandIndex, gainDb)
    }

    @Synchronized
    fun setAllBands(bands: List<Int>) {
        bands.forEachIndexed { index, gain ->
            if (index in bandGains.indices) {
                bandGains[index] = gain
                applyBandGain(index, gain)
            }
        }
    }

    private fun applyBandGain(bandIndex: Int, gainDb: Int) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            if (bandIndex < numBands) {
                val range = eq.bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()
                // Convert gainDb (-10 to +10 dB) to millibels (1 dB = 100 mB)
                val targetMb = (gainDb * 100).coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(bandIndex.toShort(), targetMb)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying band gain for band $bandIndex: ${e.message}")
        }
    }

    private fun applyAll() {
        val eq = equalizer ?: return
        try {
            eq.enabled = isEnabled
            for (i in bandGains.indices) {
                applyBandGain(i, bandGains[i])
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying equalizer settings: ${e.message}")
        }
    }

    @Synchronized
    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
    }
}
