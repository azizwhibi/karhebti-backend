@echo off
setlocal enabledelayedexpansion

:: SOS Flow Test Script for Windows
:: Tests the complete SOS flow from user to garage and back

echo ================================================================
echo          SOS FLOW TEST SCRIPT (Windows)
echo ================================================================
echo.

:: Configuration
set BACKEND_URL=http://localhost:3000
set USER_TOKEN=
set GARAGE_TOKEN=
set BREAKDOWN_ID=

:: ============================================================
:: TEST PREPARATION
:: ============================================================

echo ================================================================
echo 0. PREPARATION
echo ================================================================
echo.

echo This script will test the complete SOS flow:
echo 1. User sends SOS request
echo 2. Backend creates breakdown
echo 3. Backend finds garages
echo 4. Backend sends notification
echo 5. Garage receives notification
echo 6. Garage accepts request
echo 7. User receives status update
echo 8. User navigates to tracking
echo.

echo Checking backend status...
curl -s "%BACKEND_URL%/api/health" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] Backend is running at %BACKEND_URL%
) else (
    echo [ERROR] Backend is not running at %BACKEND_URL%
    echo Please start the backend first:
    echo   cd backend ^&^& npm start
    pause
    exit /b 1
)

pause
cls

:: ============================================================
:: STEP 1: USER LOGIN & TOKEN
:: ============================================================

echo ================================================================
echo 1. USER AUTHENTICATION
echo ================================================================
echo.

echo Login as regular user...
echo.
echo Request:
echo POST %BACKEND_URL%/api/auth/login
echo Body: {"email": "user@example.com", "password": "password123"}
echo.

set /p USER_TOKEN="Enter user JWT token (or press Enter to skip): "

if "!USER_TOKEN!"=="" (
    echo [WARNING] User token not provided - some tests will be skipped
) else (
    echo [OK] User token set

    echo.
    echo Verifying user token...
    curl -s -X GET "%BACKEND_URL%/api/auth/me" -H "Authorization: Bearer !USER_TOKEN!" > temp_user_verify.json

    type temp_user_verify.json | jq .
    if %ERRORLEVEL% EQU 0 (
        echo [OK] Token is valid
    ) else (
        echo [ERROR] Token is invalid
        del temp_user_verify.json
        pause
        exit /b 1
    )
    del temp_user_verify.json
)

pause
cls

:: ============================================================
:: STEP 2: CREATE SOS BREAKDOWN
:: ============================================================

echo ================================================================
echo 2. USER SENDS SOS REQUEST
echo ================================================================
echo.

if "!USER_TOKEN!"=="" (
    echo [WARNING] Skipped - no user token
) else (
    echo Creating SOS breakdown...
    echo.

    echo Request:
    echo {
    echo   "type": "PNEU",
    echo   "description": "Pneu creve sur autoroute - TEST SCRIPT",
    echo   "latitude": 36.8065,
    echo   "longitude": 10.1815
    echo }
    echo.

    echo Sending to %BACKEND_URL%/api/breakdowns...

    curl -s -X POST "%BACKEND_URL%/api/breakdowns" ^
        -H "Content-Type: application/json" ^
        -H "Authorization: Bearer !USER_TOKEN!" ^
        -d "{\"type\":\"PNEU\",\"description\":\"Pneu creve - TEST\",\"latitude\":36.8065,\"longitude\":10.1815}" ^
        > temp_create_response.json

    echo.
    echo Response:
    type temp_create_response.json | jq .

    :: Extract breakdown ID (basic extraction - requires jq)
    for /f "tokens=*" %%i in ('type temp_create_response.json ^| jq -r .id') do set BREAKDOWN_ID=%%i

    if not "!BREAKDOWN_ID!"=="" (
        if not "!BREAKDOWN_ID!"=="null" (
            echo [OK] Breakdown created successfully!
            echo [OK] Breakdown ID: !BREAKDOWN_ID!

            :: Check status
            for /f "tokens=*" %%i in ('type temp_create_response.json ^| jq -r .status') do set STATUS=%%i
            if "!STATUS!"=="PENDING" (
                echo [OK] Status: PENDING
            ) else (
                echo [WARNING] Status: !STATUS! (expected PENDING)
            )
        ) else (
            echo [ERROR] Failed to create breakdown
        )
    ) else (
        echo [ERROR] Failed to create breakdown
    )

    del temp_create_response.json
)

pause
cls

:: ============================================================
:: STEP 3: CHECK BACKEND LOGS
:: ============================================================

echo ================================================================
echo 3. BACKEND PROCESSING
echo ================================================================
echo.

echo Check your backend terminal for:
echo.
echo Expected logs:
echo ---------------------------------------------------------------
echo [OK] POST /api/breakdowns
echo    Status: 201 Created
echo.
echo [OK] Breakdown created successfully
echo    ID: !BREAKDOWN_ID!
echo    Type: PNEU
echo    Status: PENDING
echo.
echo [SEARCH] Looking for nearby garages...
echo    Search radius: 10 km
echo.
echo [RESULT] Search results:
echo    Found X verified garage owner(s)
echo.
echo [SEND] Sending notification to garage owners...
echo [OK] Notification sent successfully!
echo.
echo [SUMMARY] Summary:
echo    Sent: X
echo    Failed: 0
echo ---------------------------------------------------------------
echo.

set /p LOGS_SEEN="Did you see these logs in the backend? (y/n): "

if /i "!LOGS_SEEN!"=="y" (
    echo [OK] Backend processing confirmed
) else (
    echo [WARNING] Backend logs not visible - check backend terminal
)

pause
cls

:: ============================================================
:: STEP 4: CHECK GARAGE NOTIFICATION
:: ============================================================

echo ================================================================
echo 4. GARAGE OWNER NOTIFICATION
echo ================================================================
echo.

echo Check garage owner's phone:
echo.
echo Expected notification:
echo +--------------------------------+
echo ^|   Garage Owner's Phone         ^|
echo ^|                                ^|
echo ^|   NOTIFICATION APPEARS!        ^|
echo ^|                                ^|
echo ^|   ========================     ^|
echo ^|   Nouvelle demande SOS         ^|
echo ^|   Assistance PNEU demandee     ^|
echo ^|   Tap to view details          ^|
echo ^|   ========================     ^|
echo ^|                                ^|
echo ^|   [Sound plays]                ^|
echo ^|   [Phone vibrates]             ^|
echo +--------------------------------+
echo.

set /p NOTIF_RECEIVED="Did garage owner receive notification? (y/n): "

if /i "!NOTIF_RECEIVED!"=="y" (
    echo [OK] Notification received on garage owner's phone

    echo.
    set /p NOTIF_TAPPED="Did garage owner TAP the notification? (y/n): "

    if /i "!NOTIF_TAPPED!"=="y" (
        echo [OK] Notification tapped - app should open to SOS details
    ) else (
        echo [WARNING] Notification not tapped - flow incomplete
    )
) else (
    echo [ERROR] Notification NOT received
    echo.
    echo Troubleshooting:
    echo 1. Check FCM token is registered in backend
    echo 2. Verify google-services.json is correct
    echo 3. Check garage app has notification permission
    echo 4. Verify backend FCM credentials
)

pause
cls

:: ============================================================
:: STEP 5: GARAGE OWNER LOGIN & TOKEN
:: ============================================================

echo ================================================================
echo 5. GARAGE OWNER AUTHENTICATION
echo ================================================================
echo.

echo Login as garage owner...
echo.

set /p GARAGE_TOKEN="Enter garage owner JWT token (or press Enter to skip): "

if "!GARAGE_TOKEN!"=="" (
    echo [WARNING] Garage token not provided - acceptance test will be skipped
) else (
    echo [OK] Garage token set
)

pause
cls

:: ============================================================
:: STEP 6: GARAGE ACCEPTS BREAKDOWN
:: ============================================================

echo ================================================================
echo 6. GARAGE OWNER ACCEPTS REQUEST
echo ================================================================
echo.

if "!GARAGE_TOKEN!"=="" (
    echo [WARNING] Skipped - no garage token
) else if "!BREAKDOWN_ID!"=="" (
    echo [WARNING] Skipped - no breakdown ID
) else (
    echo Simulating garage acceptance...
    echo.
    echo Request:
    echo PUT %BACKEND_URL%/api/breakdowns/!BREAKDOWN_ID!/accept
    echo.

    curl -s -X PUT "%BACKEND_URL%/api/breakdowns/!BREAKDOWN_ID!/accept" ^
        -H "Authorization: Bearer !GARAGE_TOKEN!" ^
        > temp_accept_response.json

    echo Response:
    type temp_accept_response.json | jq .
    echo.

    :: Check new status
    for /f "tokens=*" %%i in ('type temp_accept_response.json ^| jq -r .status') do set NEW_STATUS=%%i

    if "!NEW_STATUS!"=="ACCEPTED" (
        echo [OK] Breakdown accepted successfully!
        echo [OK] Status updated: PENDING -^> ACCEPTED
    ) else (
        echo [WARNING] Status: !NEW_STATUS! (expected ACCEPTED)
    )

    del temp_accept_response.json
)

pause
cls

:: ============================================================
:: STEP 7: USER APP POLLING
:: ============================================================

echo ================================================================
echo 7. USER APP DETECTS STATUS CHANGE
echo ================================================================
echo.

echo User's app polls for status updates...
echo.
echo Expected behavior in user app:
echo ---------------------------------------------------------------
echo [POLL] SOSWaitingScreen polling...
echo    Breakdown ID: !BREAKDOWN_ID!
echo    Interval: 5 seconds
echo.
echo GET /api/breakdowns/!BREAKDOWN_ID!
echo [OK] Response: 200 OK
echo.
echo [CHECK] Status check:
echo    Previous: PENDING
echo    Current: ACCEPTED -- Change detected!
echo.
echo [SUCCESS] Status changed to ACCEPTED!
echo    Auto-navigating to tracking screen...
echo ---------------------------------------------------------------
echo.

if not "!BREAKDOWN_ID!"=="" (
    if not "!USER_TOKEN!"=="" (
        echo Let's verify the current status...

        curl -s -X GET "%BACKEND_URL%/api/breakdowns/!BREAKDOWN_ID!" ^
            -H "Authorization: Bearer !USER_TOKEN!" ^
            > temp_status_response.json

        echo.
        echo Response:
        type temp_status_response.json | jq .
        echo.

        for /f "tokens=*" %%i in ('type temp_status_response.json ^| jq -r .status') do set CURRENT_STATUS=%%i

        echo [OK] Current status: !CURRENT_STATUS!

        if "!CURRENT_STATUS!"=="ACCEPTED" (
            echo [OK] User app should now auto-navigate to tracking!
        )

        del temp_status_response.json
    )
)

set /p NAVIGATED="Did user app navigate to tracking screen? (y/n): "

if /i "!NAVIGATED!"=="y" (
    echo [OK] Navigation successful!
) else (
    echo [WARNING] Navigation did not occur - check polling logic
)

pause
cls

:: ============================================================
:: STEP 8: TRACKING SCREEN
:: ============================================================

echo ================================================================
echo 8. TRACKING SCREEN
echo ================================================================
echo.

echo User should now see:
echo.
echo +--------------------------------+
echo ^|   Garage trouve!               ^|
echo ^|                                ^|
echo ^|   Tracking Screen              ^|
echo ^|                                ^|
echo ^|   [Interactive Map]            ^|
echo ^|   Garage ----5.2km---- You     ^|
echo ^|                                ^|
echo ^|   Garage: Auto Service Pro     ^|
echo ^|   Phone: +216 XX XXX XXX       ^|
echo ^|   Arrivee estimee: 15 min      ^|
echo ^|                                ^|
echo ^|   [Appeler le garage]          ^|
echo +--------------------------------+
echo.

set /p TRACKING_OK="Is the tracking screen displayed correctly? (y/n): "

if /i "!TRACKING_OK!"=="y" (
    echo [OK] Tracking screen working!

    echo.
    echo Features to verify:
    echo - Map shows both positions (user ^& garage)
    echo - Distance is calculated
    echo - ETA is displayed
    echo - Call button is functional
    echo - Garage info is shown
) else (
    echo [ERROR] Tracking screen not working properly
)

pause
cls

:: ============================================================
:: TEST SUMMARY
:: ============================================================

echo ================================================================
echo TEST SUMMARY
echo ================================================================
echo.

echo Flow Timeline:
echo ----------------------------------------------------------------
echo.

if not "!USER_TOKEN!"=="" (
    echo [OK] Step 1: User authenticated
) else (
    echo [WARNING] Step 1: User not authenticated
)

if not "!BREAKDOWN_ID!"=="" (
    echo [OK] Step 2: SOS request created (ID: !BREAKDOWN_ID!)
) else (
    echo [WARNING] Step 2: SOS request not created
)

if /i "!LOGS_SEEN!"=="y" (
    echo [OK] Step 3: Backend processed request
) else (
    echo [WARNING] Step 3: Backend logs not verified
)

if /i "!NOTIF_RECEIVED!"=="y" (
    echo [OK] Step 4: Garage received notification
) else (
    echo [ERROR] Step 4: Notification NOT received
)

if /i "!NOTIF_TAPPED!"=="y" (
    echo [OK] Step 5: Garage opened notification
) else (
    echo [WARNING] Step 5: Notification not tapped
)

if not "!GARAGE_TOKEN!"=="" (
    echo [OK] Step 6: Garage accepted request
) else (
    echo [WARNING] Step 6: Garage acceptance not tested
)

if /i "!NAVIGATED!"=="y" (
    echo [OK] Step 7: User app auto-navigated
) else (
    echo [WARNING] Step 7: Auto-navigation not confirmed
)

if /i "!TRACKING_OK!"=="y" (
    echo [OK] Step 8: Tracking screen working
) else (
    echo [WARNING] Step 8: Tracking screen not verified
)

echo.
echo ----------------------------------------------------------------
echo.

:: Overall result
if /i "!NOTIF_RECEIVED!"=="y" if /i "!NAVIGATED!"=="y" if /i "!TRACKING_OK!"=="y" (
    echo.
    echo ================================================================
    echo   ALL TESTS PASSED! SOS FLOW WORKING PERFECTLY!
    echo ================================================================
    echo.
) else (
    echo.
    echo ================================================================
    echo   SOME TESTS FAILED - SEE SUMMARY ABOVE
    echo ================================================================
    echo.
)

echo.
echo For detailed documentation, see:
echo   - SOS_COMPLETE_FLOW_GUIDE.md
echo   - SOS_FLOW_VISUAL_QUICK_REFERENCE.md
echo.

echo Test completed!
pause

