package com.guardianpulse.prototype

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.BatteryManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.guardianpulse.prototype.data.AppDatabase
import com.guardianpulse.prototype.data.EventLog
import kotlinx.coroutines.*
import kotlin.math.log10
import kotlin.math.sqrt

class MonitoringService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "monitoring_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CALIBRATE = "ACTION_CALIBRATE"
    }

    private lateinit var sensorManager: SensorManager
    private var hrSensor: Sensor? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: AppDatabase
    
    private var mockHrJob: Job? = null
    private var realAudioJob: Job? = null
    private var mockAudioJob: Job? = null
    private var mockTamperJob: Job? = null
    
    private var audioRecord: AudioRecord? = null
    private var calibrationJob: Job? = null
    
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null
    
    private var isMonitoring = false
    
    private var tamperStartTime = 0L
    private var isCurrentlyRemoved = false
    
    private var hasSentLowBatteryAlert = false
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val batteryPct = (level * 100) / scale
                    PrototypeState.updateBatteryLevel(batteryPct)
                    
                    if (batteryPct <= 15 && !hasSentLowBatteryAlert) {
                        hasSentLowBatteryAlert = true
                        serviceScope.launch {
                            Log.w("MonitoringService", "Low Battery Alert Triggered ($batteryPct%)")
                            TelegramNotifier.sendLowBatteryAlert(batteryPct)
                        }
                    } else if (batteryPct > 20) {
                        hasSentLowBatteryAlert = false
                    }
                }
            }
        }
    }
    
    private var hrCalibrationBuffer = mutableListOf<Float>()
    private var audioCalibrationBuffer = mutableListOf<Float>()
    
    private var alarmPlayer: MediaPlayer? = null
    
    private val fusionEngine = FusionEngine()

    override fun onCreate() {
        super.onCreate()
        PrototypeState.init(applicationContext)
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        
        database = AppDatabase.getDatabase(this)
        
        serviceScope.launch {
            PrototypeState.mockMode.collect { isMock ->
                if (isMonitoring) {
                    if (isMock) { stopRealHRSensor(); startMockHRSensor() }
                    else { stopMockHRSensor(); startRealHRSensor() }
                }
            }
        }
        
        serviceScope.launch {
            PrototypeState.mockAudioMode.collect { isMock ->
                if (isMonitoring) {
                    if (isMock) { stopRealAudioSensor(); startMockAudioSensor() }
                    else { stopMockAudioSensor(); startRealAudioSensor() }
                }
            }
        }
        serviceScope.launch {
            PrototypeState.mockTamperMode.collect { isMock ->
                if (isMonitoring) {
                    if (isMock) { stopRealTamperSensors(); startMockTamperSensors() }
                    else { stopMockTamperSensors(); startRealTamperSensors() }
                }
            }
        }
        
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isMonitoring = false
                stopRealHRSensor()
                stopMockHRSensor()
                stopRealAudioSensor()
                stopMockAudioSensor()
                stopRealTamperSensors()
                stopMockTamperSensors()
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_CALIBRATE -> {
                startCalibration()
                return START_STICKY
            }
            ACTION_START -> {
                if (!isMonitoring) {
                    isMonitoring = true
                    if (PrototypeState.mockMode.value) startMockHRSensor() else startRealHRSensor()
                    if (PrototypeState.mockAudioMode.value) startMockAudioSensor() else startRealAudioSensor()
                    if (PrototypeState.mockTamperMode.value) startMockTamperSensors() else startRealTamperSensors()
                    
                    // Auto-start calibration if baselines are missing
                    if (PrototypeState.baselineHR.value == null || PrototypeState.baselineAudio.value == null) {
                        startCalibration()
                    }
                }
            }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Guardian Pulse")
            .setContentText("Guardian Pulse monitoring active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (Build.VERSION.SDK_INT >= 34) {
                    types = types or 256 // FOREGROUND_SERVICE_TYPE_HEALTH
                }
                startForeground(NOTIFICATION_ID, notification, types)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error starting foreground service", e)
        }

        return START_STICKY
    }
    
    private fun startCalibration() {
        if (calibrationJob?.isActive == true) return
        
        PrototypeState.setIsCalibrating(true)
        hrCalibrationBuffer.clear()
        audioCalibrationBuffer.clear()
        
        calibrationJob = serviceScope.launch {
            Log.d("MonitoringService", "Starting calibration for 30s")
            delay(30_000) // 30 seconds for prototype testing speed
            
            if (hrCalibrationBuffer.isNotEmpty()) {
                PrototypeState.setBaselineHR(hrCalibrationBuffer.average().toFloat())
            } else {
                PrototypeState.setBaselineHR(75f) // safe fallback
            }
            
            if (audioCalibrationBuffer.isNotEmpty()) {
                PrototypeState.setBaselineAudio(audioCalibrationBuffer.average().toFloat())
            } else {
                PrototypeState.setBaselineAudio(45f) // safe fallback
            }
            
            Log.d("MonitoringService", "Calibration finished. HR: ${PrototypeState.baselineHR.value}, Audio: ${PrototypeState.baselineAudio.value}")
            PrototypeState.setIsCalibrating(false)
        }
    }

    // --- HR Sensors ---
    private fun startRealHRSensor() {
        hrSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    private fun stopRealHRSensor() {
        hrSensor?.let { sensorManager.unregisterListener(this, it) }
    }
    
    private fun startMockHRSensor() {
        if (mockHrJob?.isActive == true) return
        mockHrJob = serviceScope.launch {
            var baseHr = 75f
            while (isActive) {
                if (PrototypeState.triggerHRSpike) {
                    baseHr = 150f
                    PrototypeState.triggerHRSpike = false
                } else {
                    baseHr += (-2..2).random()
                    if (baseHr < 60) baseHr = 60f
                    if (baseHr > 100 && !PrototypeState.triggerHRSpike) baseHr -= 2f
                }
                onHrReading(baseHr)
                delay(1000)
            }
        }
    }
    
    private fun stopMockHRSensor() {
        mockHrJob?.cancel()
        mockHrJob = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> onHrReading(event.values[0])
            Sensor.TYPE_PROXIMITY -> processTamper(proximity = event.values[0], light = PrototypeState.currentLight.value)
            Sensor.TYPE_LIGHT -> processTamper(proximity = PrototypeState.currentProximity.value, light = event.values[0])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- Audio Sensors ---
    private fun startRealAudioSensor() {
        if (realAudioJob?.isActive == true) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MonitoringService", "RECORD_AUDIO permission missing")
            return
        }
        
        realAudioJob = serviceScope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, sampleRate * 2) 

            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("MonitoringService", "AudioRecord initialization failed")
                return@launch
            }

            audioRecord?.startRecording()
            val buffer = ShortArray(sampleRate) 
            
            try {
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sumSquare = 0.0
                        for (i in 0 until read) {
                            sumSquare += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sumSquare / read)
                        val db = if (rms > 0) 20 * log10(rms) else 0.0
                        
                        onAudioReading(db.toFloat())
                    }
                    delay(100)
                }
            } finally {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            }
        }
    }

    private fun stopRealAudioSensor() {
        realAudioJob?.cancel()
        realAudioJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    private fun startMockAudioSensor() {
        if (mockAudioJob?.isActive == true) return
        mockAudioJob = serviceScope.launch {
            var baseDb = 45f
            while (isActive) {
                if (PrototypeState.triggerAudioSpike) {
                    baseDb = 95f
                    PrototypeState.triggerAudioSpike = false
                } else {
                    baseDb += (-3..3).random()
                    if (baseDb < 30) baseDb = 30f
                    if (baseDb > 60 && !PrototypeState.triggerAudioSpike) baseDb -= 5f
                }
                onAudioReading(baseDb)
                delay(1000)
            }
        }
    }
    
    private fun stopMockAudioSensor() {
        mockAudioJob?.cancel()
        mockAudioJob = null
    }

    // --- Data Handlers ---
    private fun onHrReading(hr: Float) {
        PrototypeState.updateHR(hr)
        
        if (PrototypeState.isCalibrating.value) {
            hrCalibrationBuffer.add(hr)
        }
        
        val baseline = PrototypeState.baselineHR.value
        val settings = PrototypeState.thresholdSettings.value
        var alertTriggered = false
        
        if (!PrototypeState.isCalibrating.value && baseline != null) {
            alertTriggered = fusionEngine.processHR(System.currentTimeMillis(), hr, baseline, settings)
        }
        
        if (alertTriggered) {
            Log.w("MonitoringService", "FUSION ALERT LEVEL 1 TRIGGERED by HR")
            AlertEscalationManager.triggerAlert()
        }
        
        logEvent(hrValue = hr, audioLevel = null, alertLevel = if (alertTriggered) 1 else 0)
    }

    private fun onAudioReading(db: Float) {
        PrototypeState.updateAudio(db)
        
        if (PrototypeState.isCalibrating.value) {
            audioCalibrationBuffer.add(db)
        }
        
        val settings = PrototypeState.thresholdSettings.value
        var alertTriggered = false
        
        if (!PrototypeState.isCalibrating.value) {
            alertTriggered = fusionEngine.processAudio(System.currentTimeMillis(), db, settings)
        }
        
        if (alertTriggered) {
            Log.w("MonitoringService", "FUSION ALERT LEVEL 1 TRIGGERED by Audio")
            AlertEscalationManager.triggerAlert()
        }
        
        logEvent(hrValue = null, audioLevel = db, alertLevel = if (alertTriggered) 1 else 0)
    }
    
    // --- Tamper Logic ---
    private fun startRealTamperSensors() {
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }
    
    private fun stopRealTamperSensors() {
        proximitySensor?.let { sensorManager.unregisterListener(this, it) }
        lightSensor?.let { sensorManager.unregisterListener(this, it) }
    }
    
    private fun startMockTamperSensors() {
        if (mockTamperJob?.isActive == true) return
        mockTamperJob = serviceScope.launch {
            while (isActive) {
                if (PrototypeState.triggerTamperRemoval) {
                    processTamper(proximity = 5.0f, light = 1000f) // Far / Bright
                } else {
                    processTamper(proximity = 0.0f, light = 10f) // Near / Dark
                }
                delay(1000)
            }
        }
    }
    
    private fun stopMockTamperSensors() {
        mockTamperJob?.cancel()
        mockTamperJob = null
    }

    private fun processTamper(proximity: Float?, light: Float?) {
        proximity?.let { PrototypeState.updateProximity(it) }
        light?.let { PrototypeState.updateLight(it) }
        
        // Define removal: Proximity > 3.0 (Far)
        // Reduced sensitivity for phone testing, requiring 10 seconds of removal
        val isRemoved = (proximity != null && proximity > 3.0f)
        
        if (isRemoved) {
            if (!isCurrentlyRemoved) {
                isCurrentlyRemoved = true
                tamperStartTime = System.currentTimeMillis()
            } else {
                val duration = System.currentTimeMillis() - tamperStartTime
                if (duration > 10000L && !PrototypeState.isTampered.value) { // 10 seconds sustained
                    PrototypeState.setTampered(true)
                    Log.w("MonitoringService", "TAMPER ALERT TRIGGERED")
                    AlertEscalationManager.triggerTamperAlert()
                    startAlarm()
                }
            }
        } else {
            isCurrentlyRemoved = false
            tamperStartTime = 0L
            if (PrototypeState.isTampered.value) {
                PrototypeState.setTampered(false)
                stopAlarm()
            }
        }
    }
    
    private fun startAlarm() {
        if (alarmPlayer?.isPlaying == true) return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            alarmPlayer = MediaPlayer.create(this, uri)
            alarmPlayer?.isLooping = true
            alarmPlayer?.start()
        } catch (e: Exception) {
            Log.e("MonitoringService", "Failed to play alarm", e)
        }
    }

    private fun stopAlarm() {
        alarmPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        alarmPlayer = null
    }
    
    private fun logEvent(hrValue: Float?, audioLevel: Float?, alertLevel: Int = 0) {
        serviceScope.launch {
            val log = EventLog(
                timestamp = System.currentTimeMillis(),
                hrValue = hrValue,
                audioLevel = audioLevel,
                hrFlag = fusionEngine.lastHrFlagTimestamp != 0L,
                audioFlag = fusionEngine.lastAudioFlagTimestamp != 0L,
                alertLevel = alertLevel
            )
            database.eventLogDao().insert(log)
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            database.eventLogDao().deleteOlderThan(oneDayAgo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        calibrationJob?.cancel()
        stopRealHRSensor()
        stopMockHRSensor()
        stopRealAudioSensor()
        stopMockAudioSensor()
        stopRealTamperSensors()
        stopMockTamperSensors()
        unregisterReceiver(batteryReceiver)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Guardian Pulse Monitoring Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
