# bootstrap-gradle.ps1 - Downloads gradle-wrapper.jar if not present
# Run this once before first build: .\bootstrap-gradle.ps1

$wrapperDir = "$PSScriptRoot\gradle\wrapper"
$jarPath = "$wrapperDir\gradle-wrapper.jar"

if (Test-Path $jarPath) {
    Write-Host "gradle-wrapper.jar already exists, skipping download." -ForegroundColor Green
} else {
    Write-Host "Downloading gradle-wrapper.jar..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null

    # Download from official Gradle GitHub releases
    $url = "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $url -OutFile $jarPath -UseBasicParsing
        Write-Host "Downloaded gradle-wrapper.jar successfully." -ForegroundColor Green
    } catch {
        Write-Host "Download failed. Trying alternate approach..." -ForegroundColor Yellow
        # Fallback: use gradle to generate the wrapper if gradle is in PATH
        if (Get-Command gradle -ErrorAction SilentlyContinue) {
            Push-Location $PSScriptRoot
            gradle wrapper --gradle-version=8.5
            Pop-Location
            Write-Host "Generated wrapper using local Gradle." -ForegroundColor Green
        } else {
            Write-Host "ERROR: Could not download wrapper. Options:" -ForegroundColor Red
            Write-Host "  1. Install Gradle: winget install Gradle.Gradle" -ForegroundColor Yellow
            Write-Host "  2. Open this project in Android Studio (auto-syncs)" -ForegroundColor Yellow
            exit 1
        }
    }
}

Write-Host ""
Write-Host "Setup complete! Run the app:" -ForegroundColor Green
Write-Host "  adb reverse tcp:3001 tcp:3001" -ForegroundColor Cyan
Write-Host "  .\gradlew.bat installDebug" -ForegroundColor Cyan
