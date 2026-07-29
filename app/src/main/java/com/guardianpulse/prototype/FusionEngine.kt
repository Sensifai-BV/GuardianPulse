package com.guardianpulse.prototype

import kotlin.math.abs

class FusionEngine {
    
    private val hrHistory = mutableListOf<Pair<Long, Float>>()
    private val audioHistory = mutableListOf<Pair<Long, Float>>()
    
    var lastHrFlagTimestamp: Long = 0
    var lastAudioFlagTimestamp: Long = 0

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

    fun processAudio(timestamp: Long, db: Float, settings: ThresholdSettings): Boolean {
        audioHistory.add(Pair(timestamp, db))
        
        val cutoff = timestamp - (settings.audioSustainSeconds * 1000L)
        audioHistory.removeAll { it.first < cutoff }
        
        if (audioHistory.isEmpty()) return false
        
        val duration = timestamp - audioHistory.first().first
        val minSamples = maxOf(1, settings.audioSustainSeconds - 1)
        
        val allExceed = audioHistory.all { it.second >= settings.audioThreshold }
        
        if (allExceed && duration >= (settings.audioSustainSeconds - 1) * 1000L && audioHistory.size >= minSamples) {
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
        lastHrFlagTimestamp = 0
        lastAudioFlagTimestamp = 0
    }
}
