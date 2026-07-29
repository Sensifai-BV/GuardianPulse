package com.guardianpulse.prototype

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PrototypeState {
    private var settingsRepo: SettingsRepository? = null

    private val _currentHR = MutableStateFlow<Float?>(null)
    val currentHR: StateFlow<Float?> = _currentHR.asStateFlow()

    private val _currentAudio = MutableStateFlow<Float?>(null)
    val currentAudio: StateFlow<Float?> = _currentAudio.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _currentProximity = MutableStateFlow<Float?>(null)
    val currentProximity: StateFlow<Float?> = _currentProximity.asStateFlow()

    private val _currentLight = MutableStateFlow<Float?>(null)
    val currentLight: StateFlow<Float?> = _currentLight.asStateFlow()

    private val _isTampered = MutableStateFlow(false)
    val isTampered: StateFlow<Boolean> = _isTampered.asStateFlow()

    private val _mockMode = MutableStateFlow(false)
    val mockMode: StateFlow<Boolean> = _mockMode.asStateFlow()
    
    private val _mockAudioMode = MutableStateFlow(false)
    val mockAudioMode: StateFlow<Boolean> = _mockAudioMode.asStateFlow()

    private val _mockTamperMode = MutableStateFlow(false)
    val mockTamperMode: StateFlow<Boolean> = _mockTamperMode.asStateFlow()

    private val _baselineHR = MutableStateFlow<Float?>(null)
    val baselineHR: StateFlow<Float?> = _baselineHR.asStateFlow()

    private val _baselineAudio = MutableStateFlow<Float?>(null)
    val baselineAudio: StateFlow<Float?> = _baselineAudio.asStateFlow()

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()
    
    private val _thresholdSettings = MutableStateFlow(ThresholdSettings())
    val thresholdSettings: StateFlow<ThresholdSettings> = _thresholdSettings.asStateFlow()
    
    var triggerHRSpike = false
    var triggerAudioSpike = false
    var triggerTamperRemoval = false

    fun init(context: Context) {
        if (settingsRepo == null) {
            settingsRepo = SettingsRepository(context)
            _baselineHR.value = settingsRepo?.getBaselineHR()
            _baselineAudio.value = settingsRepo?.getBaselineAudio()
            _thresholdSettings.value = settingsRepo?.getThresholdSettings() ?: ThresholdSettings()
        }
    }

    fun updateHR(hr: Float) {
        _currentHR.value = hr
    }

    fun updateAudio(db: Float) {
        _currentAudio.value = db
    }
    
    fun updateBatteryLevel(level: Int) {
        _batteryLevel.value = level
    }
    
    fun updateProximity(prox: Float) {
        _currentProximity.value = prox
    }
    
    fun updateLight(light: Float) {
        _currentLight.value = light
    }
    
    fun setTampered(tampered: Boolean) {
        _isTampered.value = tampered
    }

    fun setMockMode(enabled: Boolean) {
        _mockMode.value = enabled
    }

    fun setMockAudioMode(enabled: Boolean) {
        _mockAudioMode.value = enabled
    }

    fun setMockTamperMode(enabled: Boolean) {
        _mockTamperMode.value = enabled
    }

    fun setBaselineHR(hr: Float) {
        settingsRepo?.setBaselineHR(hr)
        _baselineHR.value = hr
    }

    fun setBaselineAudio(db: Float) {
        settingsRepo?.setBaselineAudio(db)
        _baselineAudio.value = db
    }

    fun clearBaselines() {
        settingsRepo?.clearBaselines()
        _baselineHR.value = null
        _baselineAudio.value = null
    }

    fun setIsCalibrating(calibrating: Boolean) {
        _isCalibrating.value = calibrating
    }
    
    fun updateThresholdSettings(settings: ThresholdSettings) {
        settingsRepo?.saveThresholdSettings(settings)
        _thresholdSettings.value = settings
    }
}
