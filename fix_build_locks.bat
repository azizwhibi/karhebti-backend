

@echo off
echo ========================================
echo Fixing Build File Locks
echo ========================================
echo.

REM Stop Gradle Daemon
echo Step 1: Stopping Gradle Daemons...
call gradlew --stop
timeout /t 2 /nobreak >nul 2>&1

REM Kill Java processes
echo Step 2: Terminating Java processes...
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM javaw.exe /T >nul 2>&1
timeout /t 2 /nobreak >nul 2>&1

REM Clean build directories
echo Step 3: Cleaning build directories...
call gradlew clean
timeout /t 2 /nobreak >nul 2>&1

REM Force delete build directory
echo Step 4: Force deleting app build directory...
if exist app\build (
    rmdir /s /q app\build >nul 2>&1
    echo App build directory deleted.
) else (
    echo App build directory doesn't exist.
)

REM Force delete root build directory
if exist build (
    rmdir /s /q build >nul 2>&1
    echo Root build directory deleted.
)

timeout /t 2 /nobreak >nul 2>&1

REM Clean Gradle cache
echo Step 5: Cleaning Gradle cache...
if exist .gradle (
    rmdir /s /q .gradle >nul 2>&1
    echo Gradle cache deleted.
)

timeout /t 2 /nobreak >nul 2>&1

echo.
echo ========================================
echo File locks cleared! Ready to build.
echo ========================================
echo.
pause

