# 🧪 Complete End-to-End Testing Guide

## 📋 Overview

This guide walks you through testing the **complete SOS flow** from start to finish, ensuring all components work together perfectly.

**Total Test Time:** ~20 minutes  
**Prerequisites:** Backend running, 2 devices/emulators (user + garage owner)

---

## 🎯 Test Objectives

✅ User can send SOS request  
✅ Backend creates breakdown and finds garages  
✅ Garage owner receives notification  
✅ Garage owner can view details  
✅ Garage owner can accept/refuse  
✅ User app auto-navigates to tracking  
✅ Tracking screen shows correct information  

---

## 🛠️ Prerequisites

### 1. Backend Setup
```bash
# Start backend server
cd backend
npm install
npm start

# Expected output:
# Server running on port 3000
# Database connected
# FCM initialized
```

### 2. Database Setup
```sql
-- Verify tables exist
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM garages;
SELECT COUNT(*) FROM devices;
SELECT COUNT(*) FROM breakdowns;

-- Create test users
INSERT INTO users (email, password, role) VALUES
  ('user@test.com', 'hashed_password', 'client'),
  ('garage@test.com', 'hashed_password', 'garage_owner');

-- Create test garage
INSERT INTO garages (name, latitude, longitude, verified, userId) VALUES
  ('Test Garage', 36.8565, 10.2315, true, 2);

-- Verify garage
SELECT * FROM garages WHERE verified = true;
```

### 3. Android App Setup
```bash
# Device 1: User
adb -s emulator-5554 install app-debug.apk

# Device 2: Garage Owner
adb -s emulator-5556 install app-debug.apk

# Check installations
adb devices
```

### 4. Firebase Configuration
- Verify `google-services.json` is present
- Check FCM project configuration
- Verify server key in backend `.env`

---

## 📱 Test Scenario 1: Complete Happy Path

### Setup (2 minutes)

**Device 1 - User:**
1. Open app
2. Login as `user@test.com`
3. Grant location permissions
4. Enable GPS

**Device 2 - Garage Owner:**
1. Open app
2. Login as `garage@test.com`
3. Grant notification permissions
4. Keep app in background

### Execution (15 minutes)

#### Step 1: User Sends SOS (2 min)

**Device 1 Actions:**
1. Navigate to SOS screen
2. Select breakdown type: `PNEU`
3. Enter description: "Pneu crevé sur autoroute A1"
4. Verify location on map (36.8065, 10.1815)
5. Click `📤 Envoyer`
6. Confirm in dialog

**Expected Results:**
```
✅ Loading overlay appears
✅ Request sent successfully
✅ Navigate to waiting screen
✅ Shows "En attente de réponse"
✅ Polling starts (every 5 seconds)
```

**Backend Logs to Verify:**
```
📥 POST /api/breakdowns
✅ Status: 201 Created
✅ Breakdown ID: 123
✅ Status: PENDING
🔍 Looking for nearby garages...
✓ Found 1 verified garage owner
📤 Sending FCM notification...
✅ Notification sent successfully!
```

**Screenshots:** 
- [ ] SOS form filled
- [ ] Waiting screen

---

#### Step 2: Garage Receives Notification (1 min)

**Device 2 Verification:**

**Expected Results:**
```
🔔 Notification appears on screen
   Title: "🚨 Nouvelle demande SOS"
   Body: "Assistance PNEU demandée"
   
🔊 Sound plays
📳 Phone vibrates (pattern)
```

**Check Logcat:**
```bash
adb -s emulator-5556 logcat | grep "KarhebtiMessaging"

# Expected:
# ✅ MESSAGE REÇU!
# Type: BREAKDOWN_REQUEST
# ✅✅✅ NOTIFICATION AFFICHÉE
```

**Screenshots:**
- [ ] Notification in status bar
- [ ] Notification expanded view

**✅ Test Checkpoint 1:** Notification received and displayed

---

#### Step 3: Garage Opens Notification (2 min)

**Device 2 Actions:**
1. Tap the notification
2. App opens

**Expected Navigation:**
```
MainActivity.onCreate()
  ↓
handleNotificationIntent()
  ↓
Navigate to GarageBreakdownDetailsScreen
  ↓
Load breakdown details
```

**Expected Results:**
```
✅ App opens (not on login screen)
✅ GarageBreakdownDetailsScreen displays
✅ Shows breakdown type: PNEU
✅ Shows description
✅ Shows map with client location
✅ Shows distance: ~5.2 km
✅ Shows ETA: ~15 minutes
✅ Shows Accept/Refuse buttons
```

**Check Logcat:**
```bash
adb -s emulator-5556 logcat | grep "MainActivity\|GarageBreakdown"

# Expected:
# 📱 Opened from notification: type=BREAKDOWN_REQUEST
# 🚨 Navigating to breakdown details: 123
# ✅ Breakdown details loaded
```

**Screenshots:**
- [ ] Breakdown details screen
- [ ] Map with marker
- [ ] Distance and ETA
- [ ] Action buttons

**✅ Test Checkpoint 2:** Details screen opens correctly

---

#### Step 4: Garage Accepts Request (3 min)

**Device 2 Actions:**
1. Review breakdown details
2. Click `✅ Accepter` button
3. Read confirmation dialog
4. Click `Confirmer`

**Expected Confirmation Dialog:**
```
╔══════════════════════════════════╗
║ ✅ Accepter cette demande SOS?   ║
╠══════════════════════════════════╣
║ En acceptant, vous vous engagez: ║
║ ✓ Vous rendre sur place          ║
║ ✓ Apporter le matériel (PNEU)    ║
║ ✓ Contacter le client si besoin  ║
║                                  ║
║ ⏱️ Temps estimé: 15 minutes      ║
║                                  ║
║ [Annuler]        [Confirmer]     ║
╚══════════════════════════════════╝
```

**Expected Results After Confirm:**
```
✅ API call: PUT /api/breakdowns/123/accept
✅ Status: 200 OK
✅ Snackbar: "Demande acceptée avec succès!"
✅ Navigate back to home screen
```

**Backend Logs:**
```
📥 PUT /api/breakdowns/123/accept
✅ User: garage@test.com
✅ Breakdown accepted successfully
   Status: PENDING → ACCEPTED
   Accepted by: Garage #1
📤 Sending acceptance notification to client...
✅ Acceptance notification sent
```

**Check Logcat Device 2:**
```bash
# Expected:
# ✅ Breakdown accepted: 123
# 🎉 Navigation to home
```

**Screenshots:**
- [ ] Confirmation dialog
- [ ] Success snackbar
- [ ] Home screen after accept

**✅ Test Checkpoint 3:** Accept works and status updated

---

#### Step 5: User Detects Status Change (5 min)

**Device 1 Automatic Behavior:**

The waiting screen polls every 5 seconds. Monitor it.

**Expected Timeline:**
```
0:00  Waiting screen shows "PENDING"
0:05  Poll 1: Still PENDING
0:10  Poll 2: Status changed to ACCEPTED! ✓
0:11  Auto-navigate to tracking screen
```

**Expected Results:**
```
✅ Polling detects status change
✅ Shows "✅ Garage trouvé!" briefly
✅ Auto-navigates to GarageTrackingScreen
✅ No manual action needed
```

**Check Logcat Device 1:**
```bash
adb -s emulator-5554 logcat | grep "SOSWaiting\|Tracking"

# Expected:
# 📡 Polling breakdown status: 123
# 📊 Status: PENDING
# 📊 Status: PENDING
# ✅ Status changed: ACCEPTED!
# 🎉 Garage accepted! Navigating...
# 🗺️ GarageTrackingScreen initialized
```

**Screenshots:**
- [ ] Waiting screen (PENDING)
- [ ] Transition animation
- [ ] Tracking screen appears

**✅ Test Checkpoint 4:** Auto-navigation works

---

#### Step 6: Verify Tracking Screen (2 min)

**Device 1 Verification:**

**Expected Screen Layout:**
```
╔════════════════════════════════════╗
║ 🎉 Garage trouvé!                  ║
╠════════════════════════════════════╣
║                                    ║
║   [Interactive Map]                ║
║   🏢 Garage ──5.2km──► 📌 You     ║
║                                    ║
║ 🚗 Garage: Test Garage             ║
║ 📞 +216 XX XXX XXX                 ║
║ ⏱️ Arrivée estimée: 15 min        ║
║ 📏 Distance: 5.2 km                ║
║                                    ║
║ ┌────────────────────────────────┐║
║ │  📞 Appeler le garage          │║
║ └────────────────────────────────┘║
╚════════════════════════════════════╝
```

**Verify Features:**
- [ ] Map displays correctly
- [ ] Two markers visible (garage + user)
- [ ] Distance shown: ~5.2 km
- [ ] ETA shown: ~15 minutes
- [ ] Garage name displayed
- [ ] Phone number shown
- [ ] Call button functional

**Test Call Button:**
1. Click `📞 Appeler le garage`
2. Verify dialer opens with number

**Check Logcat:**
```bash
# Expected:
# 🗺️ Map initialized
# 📍 User: 36.8065, 10.1815
# 🏢 Garage: 36.8565, 10.2315
# 📏 Distance: 5.2 km
# ⏱️ ETA: 15 minutes
```

**Screenshots:**
- [ ] Full tracking screen
- [ ] Map with both markers
- [ ] Garage info card
- [ ] Dialer opened (after tap)

**✅ Test Checkpoint 5:** Tracking screen fully functional

---

## ✅ Success Criteria

All checkpoints must pass:

- [x] **Checkpoint 1:** Notification received and displayed
- [x] **Checkpoint 2:** Details screen opens correctly  
- [x] **Checkpoint 3:** Accept works and status updated
- [x] **Checkpoint 4:** Auto-navigation works
- [x] **Checkpoint 5:** Tracking screen fully functional

**Total Time:** ~15 minutes  
**Result:** ✅ **COMPLETE FLOW WORKING**

---

## 🧪 Test Scenario 2: Refuse Flow

**Duration:** 5 minutes

### Steps:

1. **User sends SOS** (same as Scenario 1)
2. **Garage receives notification** (same as Scenario 1)
3. **Garage opens details** (same as Scenario 1)
4. **Garage clicks `❌ Refuser`**
5. **Confirm in dialog**

### Expected Results:

```
✅ API call: PUT /api/breakdowns/123/refuse
✅ Status: 200 OK
✅ Snackbar: "Demande refusée"
✅ Navigate back to home
✅ User app stays on waiting screen
✅ Backend could notify other garages (optional)
```

### Backend Logs:
```
📥 PUT /api/breakdowns/123/refuse
ℹ️ Garage Test Garage refused breakdown 123
✅ Refusal logged
```

---

## 🧪 Test Scenario 3: No Garages Found

**Duration:** 3 minutes

### Setup:
```sql
-- Temporarily mark all garages as not verified
UPDATE garages SET verified = false;
```

### Steps:
1. User sends SOS
2. Check backend logs

### Expected Results:
```
📥 POST /api/breakdowns
✅ Breakdown created: 123
🔍 Looking for nearby garages...
⚠️ Found 0 verified garage owners
📊 Summary: Sent: 0, Failed: 0
```

### User Experience:
```
✅ Request sent successfully
✅ Waiting screen shows "PENDING"
⏳ Stays in PENDING state
❌ No notification sent (no garages)
```

### Cleanup:
```sql
UPDATE garages SET verified = true;
```

---

## 🐛 Common Issues & Solutions

### Issue 1: Notification Not Received

**Symptoms:**
- Backend logs show "Notification sent successfully"
- Garage owner's device shows nothing

**Debug Steps:**
```bash
# 1. Check FCM token
adb shell "run-as com.example.karhebti_android cat /data/data/com.example.karhebti_android/shared_prefs/FCMTokenPrefs.xml"

# 2. Check notification permissions
adb shell dumpsys notification | findstr "karhebti"

# 3. Check Do Not Disturb
adb shell settings get global zen_mode
# Should return: 0 (DND off)

# 4. Test notification manually
# Use Firebase Console to send test notification
```

**Solutions:**
- Re-register FCM token: Logout and login again
- Grant notification permission manually
- Disable Do Not Disturb
- Verify google-services.json

---

### Issue 2: Auto-Navigation Not Triggered

**Symptoms:**
- Status changes to ACCEPTED in backend
- User app stays on waiting screen

**Debug Steps:**
```bash
# Check polling logs
adb -s emulator-5554 logcat | grep "SOSWaiting"

# Expected every 5 seconds:
# 📡 Polling breakdown status: 123
# 📊 Status: ACCEPTED ← Should trigger navigation
```

**Solutions:**
- Check network connectivity
- Verify JWT token not expired
- Check polling interval (should be 5s)
- Restart app

---

### Issue 3: Backend Can't Find Garages

**Symptoms:**
- Backend logs: "Found 0 garage owners"

**Debug Steps:**
```sql
-- Check garages exist and are verified
SELECT id, name, verified, latitude, longitude 
FROM garages 
WHERE verified = true;

-- Check distance calculation
SELECT 
  id, 
  name,
  (6371 * acos(
    cos(radians(36.8065)) * cos(radians(latitude)) * 
    cos(radians(longitude) - radians(10.1815)) + 
    sin(radians(36.8065)) * sin(radians(latitude))
  )) AS distance
FROM garages 
WHERE verified = true
HAVING distance < 10
ORDER BY distance;

-- Check FCM tokens
SELECT u.email, d.fcmToken 
FROM users u
JOIN devices d ON u.id = d.userId
WHERE u.role = 'garage_owner';
```

**Solutions:**
- Create verified garage: `UPDATE garages SET verified = true`
- Check garage location is within 10km
- Register FCM token for garage owner
- Increase search radius if needed

---

## 📊 Test Results Template

```
═══════════════════════════════════════════════════════
          SOS FLOW TEST RESULTS
═══════════════════════════════════════════════════════

Date: _______________
Tester: _____________
Build: ______________

SCENARIO 1: COMPLETE HAPPY PATH
├─ User sends SOS              [ ] PASS [ ] FAIL
├─ Garage receives notification [ ] PASS [ ] FAIL
├─ Garage opens details        [ ] PASS [ ] FAIL
├─ Garage accepts request      [ ] PASS [ ] FAIL
├─ User auto-navigates         [ ] PASS [ ] FAIL
└─ Tracking screen displays    [ ] PASS [ ] FAIL

SCENARIO 2: REFUSE FLOW
├─ Garage can refuse           [ ] PASS [ ] FAIL
└─ Refusal logged              [ ] PASS [ ] FAIL

SCENARIO 3: NO GARAGES
├─ Request still succeeds      [ ] PASS [ ] FAIL
└─ User stays on waiting       [ ] PASS [ ] FAIL

PERFORMANCE
├─ Notification delay          ____ seconds
├─ Status update delay         ____ seconds
├─ Auto-navigation delay       ____ seconds
└─ Total flow time             ____ seconds

ISSUES FOUND:
_________________________________________________
_________________________________________________
_________________________________________________

NOTES:
_________________________________________________
_________________________________________________
_________________________________________________

Overall Result: [ ] ✅ PASS [ ] ❌ FAIL
═══════════════════════════════════════════════════════
```

---

## 🎯 Automated Test Checklist

Use `test_sos_flow.bat` or `test_sos_flow.sh` for automated testing:

```bash
# Windows
test_sos_flow.bat

# Linux/Mac
bash test_sos_flow.sh
```

**Automated Checks:**
- [ ] Backend health check
- [ ] User authentication
- [ ] SOS request creation
- [ ] Backend processing logs
- [ ] API response validation
- [ ] Status polling
- [ ] Final state verification

---

## 📸 Required Screenshots

For complete documentation, capture:

1. **User Flow:**
   - [ ] SOS form filled
   - [ ] Confirmation dialog
   - [ ] Waiting screen (PENDING)
   - [ ] Waiting screen (ACCEPTED message)
   - [ ] Tracking screen with map

2. **Garage Flow:**
   - [ ] Notification in status bar
   - [ ] Notification expanded
   - [ ] Breakdown details screen
   - [ ] Accept confirmation dialog
   - [ ] Success message

3. **Backend:**
   - [ ] Terminal logs showing full flow
   - [ ] Database state before/after
   - [ ] FCM response logs

---

## 🔄 Regression Testing

After code changes, re-run:

**Quick Test (5 min):**
- Scenario 1 only
- Verify notifications work
- Verify auto-navigation works

**Full Test (20 min):**
- All 3 scenarios
- Performance measurements
- Complete documentation

---

## ✅ Final Verification

Before marking as complete:

- [ ] All 3 scenarios pass
- [ ] No console errors
- [ ] No crashes
- [ ] Performance acceptable (<30s total)
- [ ] Screenshots captured
- [ ] Issues documented
- [ ] Test results saved

---

## 📝 Next Steps After Testing

### If All Tests Pass:
1. Mark feature as **COMPLETE** ✅
2. Update documentation
3. Create release notes
4. Prepare for production deployment

### If Tests Fail:
1. Document exact failure point
2. Check relevant logs
3. Review troubleshooting section
4. Fix issues
5. Re-test

---

**Test Guide Version:** 1.0  
**Last Updated:** December 6, 2025  
**Status:** ✅ Ready for Use

