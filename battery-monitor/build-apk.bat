@echo off
setlocal
where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install Android Studio, then run this script again.
  exit /b 1
)
if not exist gradlew.bat (
  echo Gradle wrapper launcher is missing. Open this folder in Android Studio to build.
  exit /b 1
)
call gradlew.bat --no-daemon assembleDebug
if errorlevel 1 exit /b %errorlevel%
echo.
echo APK: app\build\outputs\apk\debug\app-debug.apk
endlocal
