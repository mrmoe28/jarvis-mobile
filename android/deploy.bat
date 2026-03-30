@echo off
echo Building JARVIS Mobile...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo BUILD FAILED
    exit /b 1
)
echo.
echo Installing on phone...
adb reverse tcp:3001 tcp:3001
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo INSTALL FAILED - is USB debugging enabled?
    exit /b 1
)
echo.
echo Launching JARVIS Mobile...
adb shell am start -n com.jarvis.mobile/.MainActivity
echo Done.
