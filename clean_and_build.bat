@echo off
setlocal enabledelayedexpansion
echo ========================================
echo Complete Build Fix and Rebuild
echo ========================================
echo.

REM Change to project directory
cd /d "%~dp0"

echo Step 1: Stopping all Gradle daemons...
call gradlew --stop 2>nul
timeout /t 2 /nobreak >nul 2>&1

echo Step 2: Terminating Java and Android Studio processes...
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM javaw.exe /T >nul 2>&1
taskkill /F /IM studio64.exe /T >nul 2>&1
taskkill /F /IM idea64.exe /T >nul 2>&1
timeout /t 3 /nobreak >nul 2>&1

echo Step 3: Removing build directories...
if exist app\build (
    echo   - Deleting app\build...
    rmdir /s /q app\build 2>nul
)
if exist build (
    echo   - Deleting root build...
    rmdir /s /q build 2>nul
)
if exist .gradle (
    echo   - Deleting .gradle cache...
    rmdir /s /q .gradle 2>nul
)
timeout /t 2 /nobreak >nul 2>&1

echo Step 4: Removing local Gradle cache (if locked)...
if exist "%USERPROFILE%\.gradle\caches" (
    echo   - Cleaning user Gradle caches...
    rmdir /s /q "%USERPROFILE%\.gradle\caches" 2>nul
)
timeout /t 2 /nobreak >nul 2>&1

echo.
echo Step 5: Starting clean build (no daemon)...
call gradlew clean --no-daemon
echo.

echo Step 6: Building project (no daemon)...
call gradlew assembleDebug --no-daemon --stacktrace

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo ========================================
    echo BUILD FAILED - Check errors above
    echo ========================================
)

echo.
pause

