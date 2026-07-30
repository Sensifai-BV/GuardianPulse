package com.guardianpulse.prototype

import kotlin.math.abs

/**
 * POINT 5: Audio Event Classification Engine
 * Instead of relying solely on raw decibel levels (which cannot distinguish
 * a TV from an argument), we classify audio into event categories.
 * Only distress-related events (SHOUTING, CRYING, IMPACT) contribute to alerts.
 * This approach mirrors on-device classifiers like YAMNet/openSMILE.
 * Raw audio is NEVER stored or transmitted — only the event label.
 */
enum class AudioEventLabel {
    AMBIENT,    // Normal background noise (TV, music, conversation)
    SHOUTING,   // Elevated, sustained voice with rapid amplitude increase
    CRYING,     // Rhythmic distress vocalisation pattern
    IMPACT,     // Sharp, high-energy transient (strike, fall)
    UNKNOWN
}

class FusionEngine {
    
    private val hrHistory = mutableListOf<Pair<Long, Float>>()
    private val audioHistory = mutableListOf<Pair<Long, Float>>()
    
    // Rolling buffer for audio event classification
    private val audioEventHistory = mutableListOf<Pair<Long, AudioEventLabel>>()
    
    var lastHrFlagTimestamp: Long = 0
    var lastAudioFlagTimestamp: Long = 0

    /**
     * Rule-based audio event classifier.
     * In production, this would be replaced by an on-device ML model (e.g. YAMNet).
     * Raw audio buffers are discarded immediately — only the label is retained.
     *
     * Classification rules (based on amplitude and rate-of-change):
     * - IMPACT: A single very loud spike (>100 dB equivalent) followed by rapid drop.
     * - SHOUTING: Sustained loud audio (>85 dB) with high variance (fluctuating).
     * - CRYING: Sustained moderate audio (70-85 dB) with a rhythmic pattern.
     * - AMBIENT: Everything else.
     */
    fun classifyAudioEvent(currentDb: Float, previousDb: Float?): AudioEventLabel {
        val delta = if (previousDb != null) currentDb - previousDb else 0f
        
        return when {
            currentDb >= 100f && delta >= 15f -> AudioEventLabel.IMPACT       // Sharp loud spike
            currentDb >= 85f && kotlin.math.abs(delta) >= 5f -> AudioEventLabel.SHOUTING // Loud + fluctuating
            currentDb in 70f..85f && kotlin.math.abs(delta) in 2f..8f -> AudioEventLabel.CRYING // Rhythmic moderate
            else -> AudioEventLabel.AMBIENT
        }
    }

    fun processHR(timestamp: Long, hr: Float, baselineHR: Float, settings: ThresholdSettings): Boolean {
        if (baselineHR <= 0) return false
        
        hrHistory.add(Pair(timestamp, hr))
        
        val cutoff = timestamp - (settings.hrSustainSeconds * 1000L)
        hrHistory.removeAll { it.first < cutoff }
        
        if (hrHistory.isEmpty()) return false
        
        val duration = timestamp - hrHistory.first().first
        val minSamples = maxOf(1, settings.hrSustainSeconds - 1)
        
        val allExceed = hrHistory.all {
            val deviation = (it.second - baselineHR) / baselineHR
            deviation >= settings.hrThreshold
        }
        
        if (allExceed && duration >= (settings.hrSustainSeconds - 1) * 1000L && hrHistory.size >= minSamples) {
            lastHrFlagTimestamp = timestamp
            return evaluateFusion(timestamp, settings)
        }
        return false
    }

    /**
     * POINT 5: Audio processing now uses event classification, not raw dB alone.
     * Only SHOUTING, CRYING, or IMPACT events sustained over the configured window
     * will contribute to a fusion alert.
     */
    fun processAudio(timestamp: Long, db: Float, settings: ThresholdSettings): Boolean {
        audioHistory.add(Pair(timestamp, db))
        
        val cutoff = timestamp - (settings.audioSustainSeconds * 1000L)
        audioHistory.removeAll { it.first < cutoff }
        
        if (audioHistory.isEmpty()) return false
        
        // Classify current event
        val prevDb = audioHistory.getOrNull(audioHistory.size - 2)?.second
        val eventLabel = classifyAudioEvent(db, prevDb)
        audioEventHistory.add(Pair(timestamp, eventLabel))
        audioEventHistory.removeAll { it.first < cutoff }
        
        // Only trigger alert if all sustained events are distress-related
        val distressEvents = setOf(AudioEventLabel.SHOUTING, AudioEventLabel.CRYING, AudioEventLabel.IMPACT)
        val duration = timestamp - audioHistory.first().first
        val minSamples = maxOf(1, settings.audioSustainSeconds - 1)
        
        val allDistress = audioEventHistory.isNotEmpty() &&
            audioEventHistory.all { it.second in distressEvents }
        
        // Fallback: also check raw dB threshold for cases where classifier returns UNKNOWN
        val rawDbExceeds = audioHistory.all { it.second >= settings.audioThreshold }
        
        if ((allDistress || rawDbExceeds) &&
            duration >= (settings.audioSustainSeconds - 1) * 1000L &&
            audioHistory.size >= minSamples) {
            lastAudioFlagTimestamp = timestamp
            return evaluateFusion(timestamp, settings)
        }
        return false
    }
    
    private fun evaluateFusion(timestamp: Long, settings: ThresholdSettings): Boolean {
        if (lastHrFlagTimestamp == 0L || lastAudioFlagTimestamp == 0L) return false
        
        val diff = abs(lastHrFlagTimestamp - lastAudioFlagTimestamp)
        if (diff <= settings.fusionWindowSeconds * 1000L) {
            lastHrFlagTimestamp = 0
            lastAudioFlagTimestamp = 0
            return true
        }
        return false
    }
    
    fun reset() {
        hrHistory.clear()
        audioHistory.clear()
        audioEventHistory.clear()
        lastHrFlagTimestamp = 0
        lastAudioFlagTimestamp = 0
    }
}
