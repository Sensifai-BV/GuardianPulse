package com.guardianpulse.prototype

data class ThresholdSettings(
    val hrThreshold: Float = 0.3f, // 30% above baseline
    val hrSustainSeconds: Int = 5,
    val audioThreshold: Float = 80f,
    val audioSustainSeconds: Int = 3,
    val fusionWindowSeconds: Int = 10,
    // POINT 2: Siren is disabled by default. Only enabled when MSF explicitly approves it.
    val sirenOnTamperEnabled: Boolean = false,
    // POINT 3: Escalation timeout is configurable, agreed with MSF per case risk.
    // Default is 3 minutes, but can be adjusted per operational protocol.
    val escalationTimeoutMinutes: Int = 3
)
