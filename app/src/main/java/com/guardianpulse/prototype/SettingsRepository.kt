package com.guardianpulse.prototype

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("guardian_pulse_settings", Context.MODE_PRIVATE)

    fun getBaselineHR(): Float? {
        return if (prefs.contains("baseline_hr")) prefs.getFloat("baseline_hr", 75f) else null
    }

    fun setBaselineHR(value: Float) {
        prefs.edit().putFloat("baseline_hr", value).apply()
    }

    fun getBaselineAudio(): Float? {
        return if (prefs.contains("baseline_audio")) prefs.getFloat("baseline_audio", 45f) else null
    }

    fun setBaselineAudio(value: Float) {
        prefs.edit().putFloat("baseline_audio", value).apply()
    }
    
    fun clearBaselines() {
        prefs.edit().remove("baseline_hr").remove("baseline_audio").apply()
    }

    fun getThresholdSettings(): ThresholdSettings {
        return ThresholdSettings(
            hrThreshold = prefs.getFloat("hr_threshold", 0.3f),
            hrSustainSeconds = prefs.getInt("hr_sustain", 5),
            audioThreshold = prefs.getFloat("audio_threshold", 80f),
            audioSustainSeconds = prefs.getInt("audio_sustain", 3),
            fusionWindowSeconds = prefs.getInt("fusion_window", 10)
        )
    }
    
    fun saveThresholdSettings(settings: ThresholdSettings) {
        prefs.edit()
            .putFloat("hr_threshold", settings.hrThreshold)
            .putInt("hr_sustain", settings.hrSustainSeconds)
            .putFloat("audio_threshold", settings.audioThreshold)
            .putInt("audio_sustain", settings.audioSustainSeconds)
            .putInt("fusion_window", settings.fusionWindowSeconds)
            .apply()
    }
}
