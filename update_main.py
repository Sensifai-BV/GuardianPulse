import sys
import re

with open("app/src/main/java/com/guardianpulse/prototype/MainActivity.kt", "r") as f:
    content = f.read()

# Add imports
imports_to_add = """
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BatteryFull
"""
content = content.replace("import androidx.compose.ui.unit.sp\n", "import androidx.compose.ui.unit.sp\n" + imports_to_add)

# Change BatteryOptimizationFlow to call AppNavigator
content = content.replace("MainDashboardScreen(context)", "AppNavigator(context)")

# Replace MainDashboardScreen
main_dashboard_start = content.find("@Composable\nfun MainDashboardScreen(context: Context) {")

new_ui = """@Composable
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
            imageVector = Icons.Filled.Security,
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
"""

with open("app/src/main/java/com/guardianpulse/prototype/MainActivity.kt", "w") as f:
    f.write(content[:main_dashboard_start] + new_ui)

print("Updated MainActivity.kt successfully!")
