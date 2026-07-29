# PROJECT SKILL / AGENT INSTRUCTIONS
# Guardian Pulse — Child Safety Early-Warning Wearable App (Android Prototype)
# Target: AI coding agent (e.g., in VS Code / Claude Code) — follow step by step

## 0. PROJECT CONTEXT (read first, do not skip)

You are building an **Android application prototype** called **Guardian Pulse** for a
child-safety early-warning system. The target hardware for this phase is a **fully Android
device** (e.g., Telzel TC42 or similar rugged Android smartwatch/handheld with SIM slot,
though SIM/telephony features are NOT used in this phase). The app must also be
installable/testable on a regular Android phone during development.

**Core concept:** A child wears the device. The app passively monitors (1) heart rate and
(2) ambient sound level/pattern. If both signals cross defined thresholds together within a
time window, the app sends a tiered alert via Telegram to a designated "safe adult," and
escalates if unacknowledged.

**Non-negotiable constraints (do not violate these in any implementation choice):**
1. NEVER record, store, or transmit raw audio content. Only numeric levels/derived flags
   (e.g., dB level, pattern classification) may be processed and logged. No audio files,
   no audio buffers persisted to disk, no audio sent over network.
2. All audio/HR analysis must happen **on-device**. Do not stream raw sensor data to any
   external server.
3. Every alert threshold must be **configurable and visible in the UI** — nothing hardcoded
   and hidden. This is a scoring requirement (transparent thresholds).
4. The app must survive being backgrounded/killed by Android's battery optimizer — use a
   proper Foreground Service, not a background thread that dies.
5. This is a PROTOTYPE for a grant submission demo — prioritize a working, demonstrable
   end-to-end flow over polish. But code must still be clean and modular, since it will be
   extended later (Wear OS port, non-Android wearables, real backend, WhatsApp API).

**Definition of Done for the whole project:** A person wearing/holding the Android device can
trigger a simulated distress pattern (elevated HR + loud sound), and within seconds a message
arrives in a Telegram chat, with escalation if unacknowledged within a configurable timeout.

---

## 1. WORKING METHOD (the agent must follow this process for every stage)

For **each stage** listed in Section 3:
1. State clearly which stage you are starting.
2. Implement only what that stage asks for — do not jump ahead to later stages.
3. Build and run/compile-check the code (or explain exactly how to build it if you cannot run
   an emulator/device yourself).
4. Write a short **Stage Report** in this exact format:

   ```
   ## Stage Report: <stage name>
   - What was implemented:
   - Files created/modified:
   - How to test this stage manually:
   - Known limitations / TODO for later stages:
   - Blockers or questions for the user (if any):
   ```
5. STOP after the report and wait for the user's confirmation ("ok next stage") before moving
   to the next stage. Do not silently continue to the next stage.
6. If a stage depends on something only the user can provide (e.g., a Telegram Bot Token, a
   physical device, a permission granted manually on-device), explicitly ask for it in the
   Stage Report and pause.

---

## 2. TECH STACK DECISIONS (fixed — do not deviate without asking)

- Project name: **Guardian Pulse**
- Package name: `com.guardianpulse.prototype`
- Language: **Kotlin**
- UI: **Jetpack Compose** (simple screens, not fancy — function over form)
- Min SDK: 26 (Android 8.0) — adjust only if TC42 requires higher/lower, ask user to confirm
  device's Android version if unknown
- Architecture: simple **MVVM**, one `ForegroundService` for continuous monitoring, ViewModel
  for UI state, no unnecessary abstraction layers (no full Clean Architecture — keep it light)
- Local storage: **Room** database for event logs (timestamp, signal values, alert level) —
  never store raw audio
- Networking: **Ktor Client** or **OkHttp** (agent's choice) for Telegram Bot API HTTP calls
- No cloud backend in this phase — Telegram Bot API is the only external service

---

## 3. STAGE-BY-STAGE PLAN

### STAGE 1 — Project Scaffolding
- Create new Android Studio/Gradle project (Kotlin, Compose, min SDK per Section 2).
- Package name: `com.guardianpulse.prototype`.
- Add required permissions to `AndroidManifest.xml` (declare now, request at runtime later):
  - `BODY_SENSORS`
  - `RECORD_AUDIO`
  - `INTERNET`
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_MICROPHONE`
  - `FOREGROUND_SERVICE_HEALTH` (if targeting SDK 34+)
  - `POST_NOTIFICATIONS` (Android 13+, needed for foreground service notification)
- Set up a minimal Compose UI with a single screen showing "Guardian Pulse — Running"
  placeholder.
- Verify project builds successfully.

### STAGE 2 — Runtime Permissions Flow
- Implement a permissions request screen shown on first launch.
- Request `BODY_SENSORS`, `RECORD_AUDIO`, `POST_NOTIFICATIONS` at runtime.
- Handle denial gracefully (show explanation, allow retry — do not crash).
- Add battery optimization exemption request (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).
- Report which permissions were successfully tested on an emulator vs. require a real device
  (BODY_SENSORS typically requires real hardware).

### STAGE 3 — Foreground Service Skeleton
- Create `MonitoringService : Service()` running as a foreground service with a persistent
  notification ("Guardian Pulse monitoring active").
- Implement start/stop controls from the main UI (a toggle button).
- No sensor logic yet — just prove the service starts, survives app backgrounding, and stops
  cleanly.

### STAGE 4 — Heart Rate Reading
- Inside `MonitoringService`, register a `SensorEventListener` for `Sensor.TYPE_HEART_RATE`.
- If the test device has no HR sensor, implement a **Mock HR Generator** mode (a toggle in
  settings) that produces a simulated HR value (random walk around a baseline, with a manual
  "spike" trigger button for testing) — this is essential since HR sensors are often
  unavailable on dev devices/emulators.
- Display live HR value on the UI screen.
- Log every HR reading to Room (timestamp + value) — cap log retention (e.g., last 24h) so DB
  doesn't grow unbounded.

### STAGE 5 — Audio Level Analysis
- Implement `AudioRecord`-based capture (NOT `MediaRecorder` — we must never write an audio
  file).
- Process audio in short buffers (e.g., 1-second windows), compute RMS/dB level only.
- Discard/overwrite the raw audio buffer immediately after computing the level — never persist
  audio samples.
- Display live dB level on the UI screen.
- Add a manual "simulate loud noise" test button (for use when a real microphone environment
  isn't practical during dev) as an alternative/supplement to real mic input — controllable via
  settings.
- Log only the computed level (not audio) to Room, same retention policy as HR.

### STAGE 6 — Baseline Calibration Logic
- On service start, calibrate a personal HR baseline over the first N minutes (configurable,
  default suggestion: 3 minutes) by averaging readings, OR allow manual baseline entry in
  settings for faster testing.
- Similarly calibrate an ambient sound baseline (average dB over first N minutes) OR allow
  manual entry.
- Store baseline values persistently (SharedPreferences or Room) so they don't reset on every
  app restart unless the user explicitly recalibrates.

### STAGE 7 — Fusion & Threshold Engine
- Implement a `FusionEngine` class (pure Kotlin, unit-testable, no Android dependencies where
  possible) that:
  - Computes `hrDeviation = (currentHR - baselineHR) / baselineHR`
  - Flags `hrFlag = true` if `hrDeviation > HR_THRESHOLD` sustained for `HR_SUSTAIN_SECONDS`
  - Flags `audioFlag = true` if dB level exceeds `AUDIO_THRESHOLD` sustained for
    `AUDIO_SUSTAIN_SECONDS`
  - Fusion rule: if `hrFlag AND audioFlag` occur within a shared `FUSION_WINDOW_SECONDS` window
    → raise `AlertLevel.LEVEL_1`
  - All thresholds/constants above must be exposed in a Settings screen, with sane defaults,
    and stored so they're user-adjustable (this satisfies the "transparent thresholds"
    requirement).
- Write basic unit tests for the fusion logic using fixed input sequences (no device needed).
- No alert-sending yet — just log when a fusion event would trigger, show it in the UI/log
  screen.

### STAGE 8 — Telegram Bot Integration
- User must create a bot via @BotFather and provide the Bot Token and a target Chat ID (agent
  should ask for these and explain how to obtain them if the user doesn't have them yet).
- Store the token securely (not hardcoded in source — use local encrypted storage or
  `local.properties` / `BuildConfig` for prototype purposes; note this is NOT production-safe
  and flag it as a known limitation).
- Implement `TelegramNotifier` that sends a text message via
  `https://api.telegram.org/bot<TOKEN>/sendMessage` when `AlertLevel.LEVEL_1` fires.
- Test: manually trigger a fusion event (via the Stage 4/5 simulate buttons) and confirm a
  Telegram message arrives.

### STAGE 9 — Tiered Escalation Logic
- After Level 1 alert sent, start a countdown (configurable, e.g., 5 minutes).
- Implement acknowledgment: use a Telegram **inline keyboard button** ("I've checked, all
  good") attached to the alert message. Requires either:
  - Long polling (`getUpdates`) from the app/service to check for button presses, or
  - A simple webhook (out of scope for local prototype — prefer long polling for simplicity).
- If no acknowledgment within the countdown → send **Level 2** message (repeat/escalate
  wording, optionally to a second chat ID representing a secondary contact).
- If still no acknowledgment after a second countdown → send **Level 3** message simulating PO
  escalation (to a third chat ID representing "PO").
- Implement a cooldown period after any resolved/acknowledged alert (default suggestion: 10
  minutes) before a new Level 1 can fire again, to prevent alert spam.

### STAGE 10 — Tamper Detection
- Detect if the HR sensor stops reporting values for longer than a configurable timeout while
  the service is supposed to be running → treat as possible tamper/removal, send a distinct
  "device tamper suspected" Telegram alert (separate from Level 1-3 distress alerts).
- Detect if the foreground service is killed/stopped unexpectedly (e.g., via a
  `BroadcastReceiver` on `ACTION_MY_PACKAGE_REPLACED` or a watchdog check) and log/report this.

### STAGE 11 — Event Log / Transparency Screen
- Build a simple UI screen listing recent events from Room: timestamp, HR value, audio level,
  fusion flags, alert level, whether acknowledged.
- This screen exists specifically to demonstrate "transparent thresholds and validation" for
  the grant evaluation — make sure current threshold values are also visible here or in
  Settings.

### STAGE 12 — Demo Polish
- Add a single "Demo Mode" toggle that enables the simulate-HR-spike and simulate-loud-noise
  buttons prominently on the main screen (for live demonstration without needing real
  physiological stress).
- Write a short `DEMO_SCRIPT.md` describing exactly what buttons to press, in what order, to
  reproduce the two example use cases from the challenge brief (Sarah's case / Marcus's case)
  during a live pitch.
- Final full Stage Report summarizing the whole system, known limitations, and a list of what
  would be needed to port this to a real Wear OS device or non-Android wearable (next phase).

---

## 4. THINGS THE AGENT MUST NEVER DO

- Never use `MediaRecorder` or any API that writes audio to a file.
- Never send raw audio or raw sensor streams over the network — only derived flags/values.
- Never hardcode Telegram tokens directly committed in a way implying it's production-safe;
  always flag this as a prototype-only shortcut.
- Never skip the Stage Report step, even for small stages.
- Never merge multiple stages into one giant commit/response — one stage at a time.
- Never silently change the tech stack decisions in Section 2 without asking the user first.

---

## 5. OPEN QUESTIONS THE AGENT SHOULD ASK THE USER BEFORE/DURING RELEVANT STAGES

- Exact Android version running on the TC42 device (affects min SDK).
- Whether the TC42 has a working HR sensor accessible via standard Android Sensor API, or
  needs a vendor-specific SDK.
- Telegram Bot Token + Chat ID(s) for safe adult / secondary contact / PO simulation (needed
  before Stage 8).
- Preferred default threshold values, if the user has any domain input (otherwise agent
  proposes reasonable defaults and marks them as adjustable).
