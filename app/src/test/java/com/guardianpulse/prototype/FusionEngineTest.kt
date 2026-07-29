package com.guardianpulse.prototype

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FusionEngineTest {

    private lateinit var fusionEngine: FusionEngine
    private val defaultSettings = ThresholdSettings()

    @Before
    fun setup() {
        fusionEngine = FusionEngine()
    }

    @Test
    fun testHRFlagTriggered() {
        val baseline = 75f
        val spike = 110f // > 30% deviation
        
        var fused = false
        var time = 1000L
        for (i in 1..6) {
            fused = fusionEngine.processHR(time, spike, baseline, defaultSettings)
            time += 1000L
        }
        
        // HR sustained for > 5 seconds, so HR flag should be set
        assertTrue(fusionEngine.lastHrFlagTimestamp > 0L)
        assertFalse(fused) // No audio, so no fusion
    }

    @Test
    fun testAudioFlagTriggered() {
        val loudDb = 90f // > 80f
        
        var fused = false
        var time = 1000L
        for (i in 1..4) {
            fused = fusionEngine.processAudio(time, loudDb, defaultSettings)
            time += 1000L
        }
        
        // Audio sustained for > 3 seconds, so Audio flag should be set
        assertTrue(fusionEngine.lastAudioFlagTimestamp > 0L)
        assertFalse(fused)
    }

    @Test
    fun testFusionTriggered() {
        val baseline = 75f
        val spike = 110f
        val loudDb = 90f
        
        var fused = false
        var time = 1000L
        
        // Trigger HR flag
        for (i in 1..6) {
            fusionEngine.processHR(time, spike, baseline, defaultSettings)
            time += 1000L
        }
        
        // Trigger Audio flag right after
        for (i in 1..4) {
            fused = fusionEngine.processAudio(time, loudDb, defaultSettings)
            time += 1000L
        }
        
        // Since both triggered within 10 seconds, fusion must be true
        assertTrue(fused)
        // Flags should be reset after fusion
        assertEquals(0L, fusionEngine.lastHrFlagTimestamp)
        assertEquals(0L, fusionEngine.lastAudioFlagTimestamp)
    }
}
