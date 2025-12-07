@echo off
echo ====================================
echo Karhebti App Logs Viewer
echo ====================================
echo.
echo Filtering logs for: com.example.karhebti_android
echo Press Ctrl+C to stop viewing logs
echo.
echo ====================================
echo.

adb logcat | findstr "karhebti_android"

