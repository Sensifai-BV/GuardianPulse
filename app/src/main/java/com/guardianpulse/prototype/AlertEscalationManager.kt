package com.guardianpulse.prototype

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AlertState {
    IDLE, LEVEL_1, LEVEL_2, LEVEL_3, COOLDOWN
}

object AlertEscalationManager {
    
    private val _currentState = MutableStateFlow(AlertState.IDLE)
    val currentState: StateFlow<AlertState> = _currentState.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var escalationJob: Job? = null
    private var pollingJob: Job? = null
    
    // Configurable delays (shortened for prototype testing)
    var escalationDelayMillis = 30_000L // 30s instead of 5m for fast testing
    var cooldownDelayMillis = 60_000L // 1m cooldown instead of 10m

    fun triggerAlert() {
        if (_currentState.value != AlertState.IDLE) {
            Log.d("AlertEscalationManager", "Alert ignored. Current state: ${_currentState.value}")
            return
        }
        
        _currentState.value = AlertState.LEVEL_1
        
        escalationJob = scope.launch {
            // Level 1
            Log.w("AlertEscalationManager", "Starting Level 1 Alert")
            TelegramNotifier.sendAlert(1)
            startPollingForAck()
            
            delay(escalationDelayMillis)
            if (_currentState.value != AlertState.LEVEL_1) return@launch
            
            // Level 2
            _currentState.value = AlertState.LEVEL_2
            Log.w("AlertEscalationManager", "Escalating to Level 2 Alert")
            TelegramNotifier.sendAlert(2)
            
            delay(escalationDelayMillis)
            if (_currentState.value != AlertState.LEVEL_2) return@launch
            
            // Level 3
            _currentState.value = AlertState.LEVEL_3
            Log.w("AlertEscalationManager", "Escalating to Level 3 Alert (PO)")
            TelegramNotifier.sendAlert(3)
        }
    }
    
    fun triggerTamperAlert() {
        scope.launch {
            Log.w("AlertEscalationManager", "Starting Tamper Alert")
            TelegramNotifier.sendTamperAlert()
        }
    }
    
    private fun startPollingForAck() {
        if (pollingJob?.isActive == true) return
        
        pollingJob = scope.launch {
            while (isActive && _currentState.value in listOf(AlertState.LEVEL_1, AlertState.LEVEL_2, AlertState.LEVEL_3)) {
                TelegramNotifier.pollForAck {
                    handleAck()
                }
                delay(1000) // Small delay between long polling reconnects
            }
        }
    }
    
    private fun handleAck() {
        Log.i("AlertEscalationManager", "Alert Acknowledged!")
        escalationJob?.cancel()
        enterCooldown()
    }
    
    private fun enterCooldown() {
        _currentState.value = AlertState.COOLDOWN
        pollingJob?.cancel()
        
        scope.launch {
            Log.i("AlertEscalationManager", "Entering Cooldown for ${cooldownDelayMillis/1000}s")
            delay(cooldownDelayMillis)
            _currentState.value = AlertState.IDLE
            Log.i("AlertEscalationManager", "Cooldown finished. Ready for new alerts.")
        }
    }
}
