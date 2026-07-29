# Guardian Pulse - Demo Script

This script outlines exactly how to demonstrate the **Guardian Pulse** prototype during a live pitch or validation session without needing real physiological stress or loud physical noises.

## Prerequisites
1. Ensure the app is installed and permissions are granted.
2. In `local.properties`, ensure your `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` are configured correctly.
3. Open the app and toggle **"Mock HR Generator"**, **"Mock Audio Generator"**, and **"Mock Tamper Generator"** to ON.

---

## Scenario A: Marcus's Case (Dual Distress - HR + Audio)
**Goal:** Demonstrate the core Fusion Engine where an isolated spike is ignored, but a combined distress signature triggers an escalation alert.

### Steps:
1. **Calibrate:** Press `Auto Calibrate` or `Set Defaults` (HR=75, Audio=45).
2. **Start:** Press `Start Monitoring`. Explain that Guardian Pulse is now continuously monitoring in the background.
3. **False Alarm (HR Only):**
   - Press **`Simulate HR Spike`**.
   - *Result:* HR shoots up to 150 BPM. Notice how NO alert is triggered immediately. The system is waiting to confirm a distress signature to prevent false positives.
4. **Distress Confirmation (Fusion):**
   - Wait 3 seconds, then press **`Simulate Loud Noise`**.
   - *Result:* Audio spikes to 95 dB.
   - Look at the **System State**: it changes from `IDLE` to `LEVEL_1`.
5. **Alert Reception:**
   - Open Telegram. You should immediately receive a Level 1 Distress Alert.
6. **Resolution:**
   - Tap the **`✅ I've checked, all good`** inline button in Telegram.
   - The app's System State will instantly move to `COOLDOWN`.

---

## Scenario B: Sarah's Case (Device Tampering)
**Goal:** Demonstrate the hardware removal/tamper detection to ensure an aggressor cannot simply disable the device silently.

### Steps:
1. **Preparation:** Ensure the app is running in `IDLE` mode.
2. **Tamper Event:**
   - Press **`Simulate Removal`** (under Mock Tamper Generator).
   - *Result:* The UI status changes from "Device Attached" to a flashing red "DEVICE REMOVED!".
3. **Sustained Timeout:**
   - Wait 5 seconds. The system requires a sustained removal state to avoid false alarms (e.g., adjusting clothing).
4. **Alert Reception:**
   - After 5 seconds, open Telegram. You will receive a high-priority `⚠️ TAMPER ALERT ⚠️`.

---

## Final Transparency Walkthrough
1. Scroll down the app to the **Alert Thresholds** section to show the judges/evaluators that the system is not a black box—thresholds are entirely transparent and adjustable.
2. Scroll to the bottom **Event Log (Transparency)** to prove that all underlying raw values, sensor flags, and network triggers are being securely logged to a local, encrypted database in real-time.
