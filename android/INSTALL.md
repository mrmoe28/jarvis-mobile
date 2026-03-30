# JARVIS Mobile -- Installation Guide

JARVIS Mobile connects to your JARVIS desktop via USB.

## Prerequisites

- Android phone with USB debugging enabled
- Android SDK / ADB installed (comes with Android Studio)
- Java 17+ in PATH
- JARVIS desktop running: `cd ui && node start-dev.js`

## Setup (first time only)

### 1. Get the Gradle wrapper

**Option A -- Android Studio (easiest)**
Open the `android/` folder in Android Studio. It will auto-download everything.

**Option B -- PowerShell CLI**
```powershell
cd android
.\bootstrap-gradle.ps1
```

**Option C -- If Gradle is installed**
```bash
cd android
gradle wrapper --gradle-version=8.5
```

## Install & Run

```powershell
# 1. Start JARVIS desktop (from the ui/ directory)
cd ..\ui && node start-dev.js

# 2. In a new terminal -- connect USB and set up tunnel
adb reverse tcp:3001 tcp:3001

# 3. Build and install
cd ..\android
.\gradlew.bat installDebug

# 4. Launch JARVIS on your phone
adb shell am start -n com.jarvis.mobile/.MainActivity
```

## WiFi Connection (optional)

If connecting over WiFi instead of USB:
1. Find your PC's local IP: `ipconfig | findstr IPv4`
2. Open JARVIS on your phone -> Settings (gear icon)
3. Change Server URL to `http://192.168.x.x:3001`
4. Make sure Windows Firewall allows port 3001

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `JARVIS is offline` | Run `adb reverse tcp:3001 tcp:3001` and ensure desktop is running |
| `Microphone not working` | Grant microphone permission in phone settings |
| `Build fails: JDK not found` | Set JAVA_HOME to JDK 17 path |
| `adb not found` | Add Android SDK platform-tools to PATH |
