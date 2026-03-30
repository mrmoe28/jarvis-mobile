$ADB = "C:\Users\Dell\scoop\apps\android-clt\current\platform-tools\adb.exe"
$DEVICE_IP = "100.79.152.12"
$REPO = "C:\Users\Dell\Desktop\jarvis-mobile\android"

Set-Location $REPO

Write-Host "=== Connecting to device ===" -ForegroundColor Cyan
& $ADB connect "${DEVICE_IP}:5555"

Write-Host "`n=== Building debug APK ===" -ForegroundColor Cyan
& ".\gradlew.bat" assembleDebug
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD FAILED" -ForegroundColor Red; exit 1 }

Write-Host "`n=== Setting up port reverse ===" -ForegroundColor Cyan
& $ADB -s "${DEVICE_IP}:5555" reverse tcp:3001 tcp:3001

Write-Host "`n=== Installing APK ===" -ForegroundColor Cyan
& $ADB -s "${DEVICE_IP}:5555" install -r "app\build\outputs\apk\debug\app-debug.apk"
if ($LASTEXITCODE -ne 0) { Write-Host "INSTALL FAILED" -ForegroundColor Red; exit 1 }

Write-Host "`n=== Launching app ===" -ForegroundColor Cyan
& $ADB -s "${DEVICE_IP}:5555" shell am start -n com.jarvis.mobile/.MainActivity

Write-Host "`nDone." -ForegroundColor Green
