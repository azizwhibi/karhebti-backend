@echo off
echo ================================
echo Backend Connection Test
echo ================================
echo.

echo Testing localhost:3000...
curl -s http://localhost:3000/ 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Backend is running on localhost:3000
) else (
    echo [FAIL] Backend is NOT running on localhost:3000
    echo Please start your backend server with: npm start
)

echo.
echo Testing 10.0.2.2:3000 (emulator view)...
curl -s http://10.0.2.2:3000/ 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Backend accessible from emulator IP
) else (
    echo [INFO] This will work from emulator if backend is running on localhost
)

echo.
echo ================================
echo Test Complete
echo ================================
echo.
echo If backend is NOT running:
echo   1. Open terminal in backend folder
echo   2. Run: npm start
echo   3. Wait for "Server listening on port 3000" message
echo   4. Then try login in the app
echo.
pause
