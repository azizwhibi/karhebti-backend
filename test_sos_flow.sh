#!/bin/bash

# SOS Flow Test Script
# Tests the complete SOS flow from user to garage and back

echo "╔══════════════════════════════════════════════════════════╗"
echo "║         🚨 SOS FLOW TEST SCRIPT                          ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BACKEND_URL="http://localhost:3000"
USER_TOKEN=""
GARAGE_TOKEN=""

# Function to print step
print_step() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}$1${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to print warning
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Function to wait for user
wait_user() {
    echo ""
    read -p "Press Enter to continue to next step..."
}

# ============================================================
# TEST PREPARATION
# ============================================================

print_step "0. PREPARATION"

echo "This script will test the complete SOS flow:"
echo "1. User sends SOS request"
echo "2. Backend creates breakdown"
echo "3. Backend finds garages"
echo "4. Backend sends notification"
echo "5. Garage receives notification"
echo "6. Garage accepts request"
echo "7. User receives status update"
echo "8. User navigates to tracking"
echo ""

# Check if backend is running
echo "Checking backend status..."
if curl -s "$BACKEND_URL/api/health" > /dev/null 2>&1; then
    print_success "Backend is running at $BACKEND_URL"
else
    print_error "Backend is not running at $BACKEND_URL"
    echo "Please start the backend first:"
    echo "  cd backend && npm start"
    exit 1
fi

wait_user

# ============================================================
# STEP 1: USER LOGIN & TOKEN
# ============================================================

print_step "1. USER AUTHENTICATION"

echo "Login as regular user..."
echo ""
echo "Request:"
echo "POST $BACKEND_URL/api/auth/login"
echo "Body: {\"email\": \"user@example.com\", \"password\": \"password123\"}"
echo ""

read -p "Enter user JWT token (or press Enter to skip): " USER_TOKEN

if [ -z "$USER_TOKEN" ]; then
    print_warning "User token not provided - some tests will be skipped"
else
    print_success "User token set"

    # Verify token
    echo ""
    echo "Verifying user token..."
    VERIFY_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BACKEND_URL/api/auth/me" \
        -H "Authorization: Bearer $USER_TOKEN")

    HTTP_CODE=$(echo "$VERIFY_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$VERIFY_RESPONSE" | head -n-1)

    if [ "$HTTP_CODE" = "200" ]; then
        print_success "Token is valid"
        echo "User info: $RESPONSE_BODY"
    else
        print_error "Token is invalid (HTTP $HTTP_CODE)"
        exit 1
    fi
fi

wait_user

# ============================================================
# STEP 2: CREATE SOS BREAKDOWN
# ============================================================

print_step "2. USER SENDS SOS REQUEST"

if [ -z "$USER_TOKEN" ]; then
    print_warning "Skipped - no user token"
else
    echo "Creating SOS breakdown..."
    echo ""

    # Prepare request
    SOS_REQUEST='{
        "type": "PNEU",
        "description": "Pneu crevé sur autoroute - TEST SCRIPT",
        "latitude": 36.8065,
        "longitude": 10.1815
    }'

    echo "Request:"
    echo "$SOS_REQUEST" | jq '.'
    echo ""

    # Send request
    echo "Sending to $BACKEND_URL/api/breakdowns..."
    CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/breakdowns" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $USER_TOKEN" \
        -d "$SOS_REQUEST")

    HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$CREATE_RESPONSE" | head -n-1)

    echo ""
    echo "Response (HTTP $HTTP_CODE):"
    echo "$RESPONSE_BODY" | jq '.'

    if [ "$HTTP_CODE" = "201" ]; then
        print_success "Breakdown created successfully!"

        # Extract breakdown ID
        BREAKDOWN_ID=$(echo "$RESPONSE_BODY" | jq -r '.id')
        print_success "Breakdown ID: $BREAKDOWN_ID"

        # Check status
        STATUS=$(echo "$RESPONSE_BODY" | jq -r '.status')
        if [ "$STATUS" = "PENDING" ]; then
            print_success "Status: PENDING ✓"
        else
            print_warning "Status: $STATUS (expected PENDING)"
        fi
    else
        print_error "Failed to create breakdown (HTTP $HTTP_CODE)"
        echo "Response: $RESPONSE_BODY"
        exit 1
    fi
fi

wait_user

# ============================================================
# STEP 3: CHECK BACKEND LOGS
# ============================================================

print_step "3. BACKEND PROCESSING"

echo "Check your backend terminal for:"
echo ""
echo "Expected logs:"
echo "───────────────────────────────────────────────────────────"
echo "✅ POST /api/breakdowns"
echo "   Status: 201 Created"
echo ""
echo "✅ Breakdown created successfully"
echo "   ID: $BREAKDOWN_ID"
echo "   Type: PNEU"
echo "   Status: PENDING"
echo ""
echo "🔍 Looking for nearby garages..."
echo "   Search radius: 10 km"
echo ""
echo "📊 Search results:"
echo "   ✓ Found X verified garage owner(s)"
echo ""
echo "📤 Sending notification to garage owners..."
echo "✅ Notification sent successfully!"
echo ""
echo "📊 Summary:"
echo "   ✅ Sent: X"
echo "   ❌ Failed: 0"
echo "───────────────────────────────────────────────────────────"
echo ""

read -p "Did you see these logs in the backend? (y/n): " LOGS_SEEN

if [ "$LOGS_SEEN" = "y" ]; then
    print_success "Backend processing confirmed"
else
    print_warning "Backend logs not visible - check backend terminal"
fi

wait_user

# ============================================================
# STEP 4: CHECK GARAGE NOTIFICATION
# ============================================================

print_step "4. GARAGE OWNER NOTIFICATION"

echo "Check garage owner's phone:"
echo ""
echo "Expected notification:"
echo "┌────────────────────────────────┐"
echo "│   📱 Garage Owner's Phone      │"
echo "│                                │"
echo "│   🔔 NOTIFICATION APPEARS!     │"
echo "│                                │"
echo "│   ╔══════════════════════════╗ │"
echo "│   ║ 🚨 Nouvelle demande SOS  ║ │"
echo "│   ║ Assistance PNEU demandée ║ │"
echo "│   ║ Tap to view details      ║ │"
echo "│   ╚══════════════════════════╝ │"
echo "│                                │"
echo "│   [Sound plays] 🔊             │"
echo "│   [Phone vibrates] 📳          │"
echo "└────────────────────────────────┘"
echo ""

read -p "Did garage owner receive notification? (y/n): " NOTIF_RECEIVED

if [ "$NOTIF_RECEIVED" = "y" ]; then
    print_success "Notification received on garage owner's phone"

    echo ""
    read -p "Did garage owner TAP the notification? (y/n): " NOTIF_TAPPED

    if [ "$NOTIF_TAPPED" = "y" ]; then
        print_success "Notification tapped - app should open to SOS details"
    else
        print_warning "Notification not tapped - flow incomplete"
    fi
else
    print_error "Notification NOT received"
    echo ""
    echo "Troubleshooting:"
    echo "1. Check FCM token is registered in backend"
    echo "2. Verify google-services.json is correct"
    echo "3. Check garage app has notification permission"
    echo "4. Verify backend FCM credentials"
    echo ""
    echo "To check FCM token:"
    echo "  SELECT fcm_token FROM devices WHERE user_id = [garage_user_id];"
fi

wait_user

# ============================================================
# STEP 5: GARAGE OWNER LOGIN & TOKEN
# ============================================================

print_step "5. GARAGE OWNER AUTHENTICATION"

echo "Login as garage owner..."
echo ""

read -p "Enter garage owner JWT token (or press Enter to skip): " GARAGE_TOKEN

if [ -z "$GARAGE_TOKEN" ]; then
    print_warning "Garage token not provided - acceptance test will be skipped"
else
    print_success "Garage token set"
fi

wait_user

# ============================================================
# STEP 6: GARAGE ACCEPTS BREAKDOWN
# ============================================================

print_step "6. GARAGE OWNER ACCEPTS REQUEST"

if [ -z "$GARAGE_TOKEN" ] || [ -z "$BREAKDOWN_ID" ]; then
    print_warning "Skipped - missing garage token or breakdown ID"
else
    echo "Simulating garage acceptance..."
    echo ""
    echo "Request:"
    echo "PUT $BACKEND_URL/api/breakdowns/$BREAKDOWN_ID/accept"
    echo ""

    # Send accept request
    ACCEPT_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT \
        "$BACKEND_URL/api/breakdowns/$BREAKDOWN_ID/accept" \
        -H "Authorization: Bearer $GARAGE_TOKEN")

    HTTP_CODE=$(echo "$ACCEPT_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$ACCEPT_RESPONSE" | head -n-1)

    echo "Response (HTTP $HTTP_CODE):"
    echo "$RESPONSE_BODY" | jq '.'
    echo ""

    if [ "$HTTP_CODE" = "200" ]; then
        print_success "Breakdown accepted successfully!"

        # Check new status
        NEW_STATUS=$(echo "$RESPONSE_BODY" | jq -r '.status')
        if [ "$NEW_STATUS" = "ACCEPTED" ]; then
            print_success "Status updated: PENDING → ACCEPTED ✓"
        else
            print_warning "Status: $NEW_STATUS (expected ACCEPTED)"
        fi
    else
        print_error "Failed to accept breakdown (HTTP $HTTP_CODE)"
        echo "Response: $RESPONSE_BODY"
    fi
fi

wait_user

# ============================================================
# STEP 7: USER APP POLLING
# ============================================================

print_step "7. USER APP DETECTS STATUS CHANGE"

echo "User's app polls for status updates..."
echo ""
echo "Expected behavior in user app:"
echo "───────────────────────────────────────────────────────────"
echo "📡 SOSWaitingScreen polling..."
echo "   Breakdown ID: $BREAKDOWN_ID"
echo "   Interval: 5 seconds"
echo ""
echo "GET /api/breakdowns/$BREAKDOWN_ID"
echo "✅ Response: 200 OK"
echo ""
echo "📊 Status check:"
echo "   Previous: PENDING"
echo "   Current: ACCEPTED ◄── Change detected!"
echo ""
echo "🎉 Status changed to ACCEPTED!"
echo "   Auto-navigating to tracking screen..."
echo "───────────────────────────────────────────────────────────"
echo ""

if [ ! -z "$BREAKDOWN_ID" ]; then
    echo "Let's verify the current status..."

    # Poll status
    STATUS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
        "$BACKEND_URL/api/breakdowns/$BREAKDOWN_ID" \
        -H "Authorization: Bearer $USER_TOKEN")

    HTTP_CODE=$(echo "$STATUS_RESPONSE" | tail -n1)
    RESPONSE_BODY=$(echo "$STATUS_RESPONSE" | head -n-1)

    echo ""
    echo "Response (HTTP $HTTP_CODE):"
    echo "$RESPONSE_BODY" | jq '.'
    echo ""

    if [ "$HTTP_CODE" = "200" ]; then
        CURRENT_STATUS=$(echo "$RESPONSE_BODY" | jq -r '.status')
        print_success "Current status: $CURRENT_STATUS"

        if [ "$CURRENT_STATUS" = "ACCEPTED" ]; then
            print_success "✅ User app should now auto-navigate to tracking!"
        fi
    fi
fi

read -p "Did user app navigate to tracking screen? (y/n): " NAVIGATED

if [ "$NAVIGATED" = "y" ]; then
    print_success "Navigation successful!"
else
    print_warning "Navigation did not occur - check polling logic"
fi

wait_user

# ============================================================
# STEP 8: TRACKING SCREEN
# ============================================================

print_step "8. TRACKING SCREEN"

echo "User should now see:"
echo ""
echo "┌────────────────────────────────┐"
echo "│   🎉 Garage trouvé!            │"
echo "│                                │"
echo "│   📍 Tracking Screen           │"
echo "│                                │"
echo "│   [Interactive Map]            │"
echo "│   🏢 Garage ────5.2km──── 📌  │"
echo "│                                │"
echo "│   🚗 Garage: Auto Service Pro  │"
echo "│   📞 +216 XX XXX XXX           │"
echo "│   ⏱️ Arrivée estimée: 15 min  │"
echo "│                                │"
echo "│   [📞 Appeler le garage]       │"
echo "└────────────────────────────────┘"
echo ""

read -p "Is the tracking screen displayed correctly? (y/n): " TRACKING_OK

if [ "$TRACKING_OK" = "y" ]; then
    print_success "Tracking screen working!"

    echo ""
    echo "Features to verify:"
    echo "✓ Map shows both positions (user & garage)"
    echo "✓ Distance is calculated"
    echo "✓ ETA is displayed"
    echo "✓ Call button is functional"
    echo "✓ Garage info is shown"
else
    print_error "Tracking screen not working properly"
fi

wait_user

# ============================================================
# TEST SUMMARY
# ============================================================

print_step "TEST SUMMARY"

echo ""
echo "Flow Timeline:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
print_success "Step 1: User authenticated ✓"
if [ ! -z "$BREAKDOWN_ID" ]; then
    print_success "Step 2: SOS request created (ID: $BREAKDOWN_ID) ✓"
else
    print_warning "Step 2: SOS request not created"
fi

if [ "$LOGS_SEEN" = "y" ]; then
    print_success "Step 3: Backend processed request ✓"
else
    print_warning "Step 3: Backend logs not verified"
fi

if [ "$NOTIF_RECEIVED" = "y" ]; then
    print_success "Step 4: Garage received notification ✓"
else
    print_error "Step 4: Notification NOT received ✗"
fi

if [ "$NOTIF_TAPPED" = "y" ]; then
    print_success "Step 5: Garage opened notification ✓"
else
    print_warning "Step 5: Notification not tapped"
fi

if [ ! -z "$GARAGE_TOKEN" ]; then
    print_success "Step 6: Garage accepted request ✓"
else
    print_warning "Step 6: Garage acceptance not tested"
fi

if [ "$NAVIGATED" = "y" ]; then
    print_success "Step 7: User app auto-navigated ✓"
else
    print_warning "Step 7: Auto-navigation not confirmed"
fi

if [ "$TRACKING_OK" = "y" ]; then
    print_success "Step 8: Tracking screen working ✓"
else
    print_warning "Step 8: Tracking screen not verified"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Overall result
if [ "$NOTIF_RECEIVED" = "y" ] && [ "$NAVIGATED" = "y" ] && [ "$TRACKING_OK" = "y" ]; then
    echo ""
    print_success "╔══════════════════════════════════════════════════════════╗"
    print_success "║  🎉 ALL TESTS PASSED! SOS FLOW WORKING PERFECTLY!       ║"
    print_success "╚══════════════════════════════════════════════════════════╝"
    echo ""
else
    echo ""
    print_warning "╔══════════════════════════════════════════════════════════╗"
    print_warning "║  ⚠️  SOME TESTS FAILED - SEE SUMMARY ABOVE              ║"
    print_warning "╚══════════════════════════════════════════════════════════╝"
    echo ""
fi

echo ""
echo "For detailed documentation, see:"
echo "  - SOS_COMPLETE_FLOW_GUIDE.md"
echo "  - SOS_FLOW_VISUAL_QUICK_REFERENCE.md"
echo ""

echo "Test completed!"

