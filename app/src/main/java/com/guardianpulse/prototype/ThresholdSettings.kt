package com.guardianpulse.prototype

data class ThresholdSettings(
    val hrThreshold: Float = 0.3f, // 30% above baseline
    val hrSustainSeconds: Int = 5,
    val audioThreshold: Float = 80f,
    val audioSustainSeconds: Int = 3,
    val fusionWindowSeconds: Int = 10
)
