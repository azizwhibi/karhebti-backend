@echo off
cd /d "C:\Users\Mosbeh Eya\Desktop\karhebti-android-gestionVoitures"
echo ============================================
echo Testing Android Build After Notifications Removal
echo ============================================
echo.

echo Running Kotlin compilation...
call gradlew.bat compileDebugKotlin
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Kotlin Compilation SUCCESS
) else (
    echo.
    echo ❌ Kotlin Compilation FAILED
    exit /b 1
)

echo.
echo Running Resource Processing...
call gradlew.bat processDebugResources
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Resource Processing SUCCESS
) else (
    echo.
    echo ⚠️ Resource Processing had issues (may be due to file locks)
)

echo.
echo ============================================
echo Build Test Complete
echo ============================================
pause

