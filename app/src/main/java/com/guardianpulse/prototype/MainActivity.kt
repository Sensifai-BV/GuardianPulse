package com.guardianpulse.prototype

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.core.app.NotificationCompat
import androidx.compose.ui.unit.em
import com.guardianpulse.prototype.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings

val RauschCoral = Color(0xFFFF5A5F)
val KazanTeal = Color(0xFF00A699)
val HofDark = Color(0xFF222222)
val FoggyGray = Color(0xFF767676)
val ErrorRed = Color(0xFFC13515)
val SurfaceGray = Color(0xFFF7F7F7)

val CoralStayColorScheme = lightColorScheme(
    primary = RauschCoral,
    secondary = KazanTeal,
    background = Color.White,
    surface = SurfaceGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = HofDark,
    onSurface = HofDark,
    error = ErrorRed,
    onError = Color.White
)

val CoralStayTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).em
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)

val CoralStayShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun CoralStayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoralStayColorScheme,
        typography = CoralStayTypography,
        shapes = CoralStayShapes,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoralStayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionFlowScreen()
                }
            }
        }
    }
}

@Composable
fun PermissionFlowScreen() {
    val context = LocalContext.current
    
    val requiredPermissions = mutableListOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    var allPermissionsGranted by remember {
        mutableStateOf(requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            allPermissionsGranted = true
            showRationale = false
        } else {
            showRationale = true
        }
    }

    if (allPermissionsGranted) {
        BatteryOptimizationFlow()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Guardian Pulse requires sensors, microphone, and notification permissions to function correctly.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(onClick = { 
                permissionLauncher.launch(requiredPermissions.toTypedArray()) 
            }) {
                Text("Grant Permissions")
            }
            
            if (showRationale) {
                Text(
                    text = "Permissions were denied. Please grant them in app settings to proceed.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun BatteryOptimizationFlow() {
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val packageName = context.packageName
    
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(packageName)
            } else true
        )
    }

    if (!isIgnoringBatteryOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "To run continuously in the background, Guardian Pulse must be exempted from battery optimizations.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(onClick = { 
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        isIgnoringBatteryOptimizations = true // Force skip if OS is broken
                    }
                }
            }) {
                Text("Disable Battery Optimization")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(onClick = { 
                isIgnoringBatteryOptimizations = true // Allow forced bypass for older devices
            }) {
                Text("Skip / I have done this")
            }
        }
    } else {
        AppNavigator(context)
    }
}

@Composable
fun AppNavigator(context: Context) {
    var showParentSettings by remember { mutableStateOf(false) }
    
    if (showParentSettings) {
        ParentSettingsScreen(context = context, onBack = { showParentSettings = false })
    } else {
        ChildDashboardScreen(context = context, onSettingsClick = { showParentSettings = true })
    }
}

@Composable
fun ChildDashboardScreen(context: Context, onSettingsClick: () -> Unit) {
    val hrValue by PrototypeState.currentHR.collectAsState()
    val isTampered by PrototypeState.isTampered.collectAsState()
    
    var isServiceRunning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Parent Settings", tint = Color.LightGray)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = if (isTampered) Icons.Filled.Warning else Icons.Filled.Lock,
            contentDescription = "Shield",
            modifier = Modifier.size(120.dp),
            tint = if (isTampered) ErrorRed else KazanTeal
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isTampered) "Alert! Device Removed" else "Everything is Safe!",
            style = MaterialTheme.typography.headlineLarge,
            color = if (isTampered) ErrorRed else KazanTeal,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceGray),
            modifier = Modifier.fillMaxWidth().height(120.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Heart",
                    modifier = Modifier.size(48.dp),
                    tint = RauschCoral
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = hrValue?.let { "${it.toInt()} BPM" } ?: "-- BPM",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HofDark
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                val intent = Intent(context, MonitoringService::class.java).apply {
                    action = if (isServiceRunning) MonitoringService.ACTION_STOP else MonitoringService.ACTION_START
                }
                if (!isServiceRunning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isServiceRunning = !isServiceRunning
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isServiceRunning) FoggyGray else RauschCoral)
        ) {
            Text(if (isServiceRunning) "Stop System" else "Start System", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ParentSettingsScreen(context: Context, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Button(onClick = onBack, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Back to Child View")
        }
        
        Text("Parent & Developer Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 24.dp))
        
        // --- HR Section ---
        val isMockHR by PrototypeState.mockMode.collectAsState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mock HR Generator")
            Switch(
                checked = isMockHR,
                onCheckedChange = { PrototypeState.setMockMode(it) },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isMockHR) {
            Button(onClick = { PrototypeState.triggerHRSpike = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Simulate HR Spike")
            }
        }

        // --- Audio Section ---
        val isMockAudio by PrototypeState.mockAudioMode.collectAsState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mock Audio Generator")
            Switch(
                checked = isMockAudio,
                onCheckedChange = { PrototypeState.setMockAudioMode(it) },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isMockAudio) {
            Button(onClick = { PrototypeState.triggerAudioSpike = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Simulate Loud Noise")
            }
        }
        
        // --- Tamper Section ---
        val isMockTamper by PrototypeState.mockTamperMode.collectAsState()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mock Tamper Generator")
            Switch(
                checked = isMockTamper,
                onCheckedChange = { PrototypeState.setMockTamperMode(it) },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isMockTamper) {
            Button(onClick = { PrototypeState.triggerTamperRemoval = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Simulate Removal")
            }
        }
        
        // --- Calibration Section ---
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        val isCalibrating by PrototypeState.isCalibrating.collectAsState()
        val baseHR by PrototypeState.baselineHR.collectAsState()
        val baseAudio by PrototypeState.baselineAudio.collectAsState()

        Text("Baselines & Calibration", style = MaterialTheme.typography.titleLarge)
        
        if (isCalibrating) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            Text("Calibrating... Please wait (30s).", color = MaterialTheme.colorScheme.secondary)
        } else {
            Text(text = "Baseline HR: ${baseHR?.toInt() ?: "Not Set"}")
            Text(text = "Baseline Audio: ${baseAudio?.toInt() ?: "Not Set"}", modifier = Modifier.padding(bottom = 16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        PrototypeState.clearBaselines()
                        val intent = Intent(context, MonitoringService::class.java).apply {
                            action = MonitoringService.ACTION_CALIBRATE
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                ) {
                    Text("Auto Calibrate")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        PrototypeState.setBaselineHR(75f)
                        PrototypeState.setBaselineAudio(45f)
                    }
                ) {
                    Text("Set Defaults")
                }
            }
        }

        // --- Thresholds Section ---
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Alert Thresholds", style = MaterialTheme.typography.titleLarge)
        val settings by PrototypeState.thresholdSettings.collectAsState()
        
        Text("HR Deviation Trigger: ${(settings.hrThreshold * 100).toInt()}%")
        Slider(
            value = settings.hrThreshold,
            onValueChange = { PrototypeState.updateThresholdSettings(settings.copy(hrThreshold = it)) },
            valueRange = 0.1f..1.0f,
            steps = 9
        )
        Text("Audio Trigger Level: ${settings.audioThreshold.toInt()} dB")
        Slider(
            value = settings.audioThreshold,
            onValueChange = { PrototypeState.updateThresholdSettings(settings.copy(audioThreshold = it)) },
            valueRange = 50f..120f,
            steps = 14
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
