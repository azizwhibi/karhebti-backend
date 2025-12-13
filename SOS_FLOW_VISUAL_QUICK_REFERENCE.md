# 🚨 SOS Flow - Visual Quick Reference

## 🎯 The Complete Journey (12 seconds)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         🚨 SOS FLOW TIMELINE                        │
└─────────────────────────────────────────────────────────────────────┘

0:00  👤 USER                    🖥️ BACKEND                🏪 GARAGE
      │                          │                          │
      📱 Fills SOS form          │                          │
      Type: PNEU                 │                          │
      Location: 36.8065, 10.1815 │                          │
      Clicks [📤 Envoyer] ───────►│                          │
                                  │                          │
0:01                              ✅ Creates Breakdown       │
                                  ID: 6756e8f8               │
                                  Status: PENDING            │
                                  │                          │
0:02                              🔍 Finds nearby garages    │
                                  Found: 1 garage            │
                                  │                          │
0:03                              📤 Sends FCM ──────────────►│
                                  Notification sent!         🔔 NOTIFICATION!
                                  │                          "🚨 Nouvelle SOS"
                                  │                          "PNEU demandée"
                                  │                          │
0:05                              │                          📱 Taps notification
                                  │                          Opens app
                                  │                          │
0:06                              │                          👀 Views details:
                                  │                          - Type: PNEU
                                  │                          - Distance: 5.2km
                                  │                          - Client info
                                  │                          │
0:07                              │                          ✅ Clicks [Accepter]
                                  │                          Confirms action
                                  │                          │
0:08                              ◄──────────────────────────│ Sends accept
                                  ✅ Updates status          │
                                  PENDING → ACCEPTED         │
                                  │                          │
0:10  📡 Polling detects change   │                          │
      Status: ACCEPTED ◄──────────│                          │
      │                          │                          │
0:11  🎉 Auto-navigates          │                          │
      To tracking screen         │                          │
      │                          │                          │
0:12  🗺️ TRACKING SCREEN         │                          🗺️ Navigation to client
      Shows:                     │                          Started!
      - Garage location          │                          │
      - Your location            │                          │
      - Distance: 5.2 km         │                          │
      - ETA: 15 minutes          │                          │
      - [📞 Call button]         │                          │
```

---

## 📱 User Interface Flow

```
┌────────────────┐
│ 1. SOS Screen  │
└────────────────┘
        │
        │ User fills form
        │ Clicks [📤 Envoyer]
        ▼
┌──────────────────────┐
│ 2. Confirmation      │
│    Dialog            │
│                      │
│ Type: PNEU          │
│ Location: 36.80...  │
│                      │
│ [Annuler] [Confirmer]│
└──────────────────────┘
        │
        │ Confirms
        ▼
┌──────────────────────┐
│ 3. Loading Overlay   │
│                      │
│    ⏳ Sending...     │
└──────────────────────┘
        │
        │ Success (201)
        ▼
┌──────────────────────┐
│ 4. SOSWaitingScreen  │
│                      │
│    ⏳ PENDING        │
│                      │
│ "Le garage examine   │
│  votre demande..."   │
│                      │
│ [Polling every 5s]   │
└──────────────────────┘
        │
        │ Status → ACCEPTED
        │ (Auto-navigate)
        ▼
┌──────────────────────┐
│ 5. Tracking Screen   │
│                      │
│  🎉 Garage trouvé!   │
│                      │
│  [🗺️ Interactive Map]│
│  🏢 ────5.2km──── 📌 │
│                      │
│  ⏱️ ETA: 15 minutes  │
│  📞 [Call Garage]    │
└──────────────────────┘
```

---

## 🏪 Garage Owner Interface Flow

```
┌──────────────────────┐
│ 1. NOTIFICATION      │
│    APPEARS           │
│                      │
│ ╔════════════════╗  │
│ ║ 🚨 Nouvelle    ║  │
│ ║    demande SOS ║  │
│ ║ PNEU demandée  ║  │
│ ╚════════════════╝  │
│                      │
│ 🔊 Sound + 📳 Vibrate│
└──────────────────────┘
        │
        │ Taps notification
        ▼
┌──────────────────────┐
│ 2. App Opens         │
│                      │
│ MainActivity         │
│ ↓                    │
│ Parse intent extras  │
│ ↓                    │
│ Navigate to details  │
└──────────────────────┘
        │
        ▼
┌──────────────────────┐
│ 3. SOS Details       │
│                      │
│ 🚨 Demande SOS      │
│                      │
│ Type: PNEU 🛞       │
│ Description: ...     │
│                      │
│ [🗺️ Map]            │
│ 📍 36.8065, 10.1815  │
│ 📏 Distance: 5.2 km  │
│                      │
│ 👤 Jean Dupont       │
│ 📞 +216 XX XXX XXX   │
│                      │
│ [✅ Accepter]        │
│ [❌ Refuser]         │
└──────────────────────┘
        │
        │ Clicks [✅ Accepter]
        ▼
┌──────────────────────┐
│ 4. Confirmation      │
│                      │
│ Accepter cette       │
│ demande SOS?         │
│                      │
│ Vous vous engagez à: │
│ ✓ Vous rendre        │
│ ✓ Arriver 15-20 min  │
│                      │
│ [Annuler] [Confirmer]│
└──────────────────────┘
        │
        │ Confirms
        ▼
┌──────────────────────┐
│ 5. Success + Nav     │
│                      │
│ ✅ Demande acceptée! │
│                      │
│ 🗺️ Navigation       │
│    démarrée          │
│                      │
│ Direction: Client    │
│ Distance: 5.2 km     │
│ ETA: 15 minutes      │
│                      │
│ [🎯 Voir itinéraire] │
└──────────────────────┘
```

---

## 🔄 Status Transitions

```
┌──────────────────────────────────────────────────────────┐
│                   Breakdown Status Flow                  │
└──────────────────────────────────────────────────────────┘

    CREATED                User creates SOS request
       │
       ▼
  ┌─────────┐
  │ PENDING │◄──────── Initial status
  └─────────┘          Waiting for garage response
       │
       ├──────────────┐
       │              │
       ▼              ▼
  ┌─────────┐    ┌─────────┐
  │ACCEPTED │    │ REFUSED │
  └─────────┘    └────��────┘
       │              │
       │              └────► User returns to home
       │
       ▼
  ┌─────────┐
  │IN_PROGRESS│      Garage on the way
  └─────────┘
       │
       ▼
  ┌─────────┐
  │COMPLETED│       Garage arrived & fixed
  └─────────┘

Cancellation path:
  PENDING/ACCEPTED ──► CANCELLED (by user or timeout)
```

---

## 🎭 Three Perspectives

### 👤 USER'S VIEW

```
1. 📝 Form Filling
   ├─ Select type
   ├─ Enter description
   └─ Confirm location

2. ⏳ Waiting
   ├─ See "PENDING" status
   ├─ Wait for garage response
   └─ App polls every 5s

3. 🎉 Success!
   ├─ Status changes to "ACCEPTED"
   ├─ Auto-navigate to tracking
   └─ See garage approaching

4. 🚗 Tracking
   ├─ Watch garage location
   ├─ See ETA countdown
   └─ Can call garage
```

---

### 🖥️ BACKEND'S VIEW

```
1. 📥 Receive Request
   ├─ Validate JWT token
   ├─ Validate data
   └─ Create breakdown (PENDING)

2. 🔍 Find Garages
   ├─ Query nearby garages
   ├─ Filter by verified status
   └─ Check FCM tokens exist

3. 📤 Send Notifications
   ├─ Build FCM payload
   ├─ Send to each garage
   └─ Log results

4. 🔄 Status Updates
   ├─ Receive accept/refuse
   ├─ Update database
   └─ Return new status
```

---

### 🏪 GARAGE'S VIEW

```
1. 🔔 Notification
   ├─ Phone receives FCM
   ├─ Display notification
   └─ Play sound + vibrate

2. 👀 View Details
   ├─ Tap notification
   ├─ Open app to details
   └─ See breakdown info

3. ✅ Decision
   ├─ Review distance/type
   ├─ Accept or refuse
   └─ Confirm action

4. 🗺️ Navigation
   ├─ Get directions
   ├─ Drive to client
   └─ Complete service
```

---

## 🔥 Critical Success Points

### ✅ Must Work

1. **Location** ✓
   - GPS permission granted
   - Location fetched or manual selected
   - Coordinates sent to backend

2. **Notification** ✓
   - FCM token registered
   - Notification received
   - Tap opens app correctly

3. **Polling** ✓
   - Every 5 seconds
   - Detects status change
   - Auto-navigates on ACCEPTED

4. **Backend** ✓
   - Creates breakdown
   - Finds garages
   - Sends notifications
   - Updates status

---

## 🐛 Common Failures

### ❌ No Notification

```
Possible causes:
├─ FCM token not registered
├─ Backend FCM key invalid
├─ Notification permission denied
├─ Do Not Disturb enabled
└─ Network error

Solution:
1. Check FCM token in backend DB
2. Verify google-services.json
3. Test notification manually
4. Check phone settings
```

---

### ❌ Stuck on Waiting

```
Possible causes:
├─ Polling stopped
├─ Network error
├─ JWT token expired
└─ Backend not updating status

Solution:
1. Check network logs
2. Verify polling interval
3. Test status endpoint directly
4. Check backend logs
```

---

### ❌ No Garages Found

```
Possible causes:
├─ No garages in radius
├─ Garages not verified
├─ No FCM tokens
└─ Location too remote

Solution:
1. Check garage table in DB
2. Verify verified = true
3. Check FCM tokens exist
4. Increase search radius
```

---

## 📊 Key Metrics

### ⚡ Expected Timings

```
Action                    Time      Status
─────────────────────────────────────────────
User clicks [Envoyer]     0:00      ⏱️
Backend creates           0:01      ✅ 201 Created
Backend finds garages     0:02      🔍
Backend sends FCM         0:03      📤 Sent
Garage receives notify    0:04      🔔 Delivered
Garage taps & views       0:05-06   👀
Garage accepts            0:07-08   ✅ Confirmed
Backend updates status    0:08      💾 Updated
User polls & detects      0:10      📡 Detected
User auto-navigates       0:11      🎯 Navigated
───────────────────────────────────────────
Total Time: 11 seconds ✅
```

---

## 🎯 Next Implementation Steps

### 1. Garage Owner SOS Details Screen (Missing)

**File:** `GarageBreakdownDetailsScreen.kt`

**Must Display:**
- ✅ Breakdown type
- ✅ Description
- ✅ Location on map
- ✅ Distance from garage
- ✅ Client info
- ✅ Accept button
- ✅ Refuse button

**Navigation:**
```kotlin
// In NavGraph.kt
composable("garage/sos/{breakdownId}") { 
    GarageBreakdownDetailsScreen(...)
}

// From MainActivity when notification tapped
if (intent.getStringExtra("notification_type") == "BREAKDOWN_REQUEST") {
    val breakdownId = intent.getStringExtra("breakdownId")
    navController.navigate("garage/sos/$breakdownId")
}
```

---

### 2. Backend Accept/Refuse Endpoints

**Expected:**
```javascript
// PUT /api/breakdowns/:id/accept
router.put('/:id/accept', authenticateToken, async (req, res) => {
    // Verify user is garage owner
    // Update breakdown status to ACCEPTED
    // Save acceptedBy and acceptedAt
    // Return updated breakdown
});

// PUT /api/breakdowns/:id/refuse
router.put('/:id/refuse', authenticateToken, async (req, res) => {
    // Update status to REFUSED
    // Save refusedBy and refusedAt
    // Return updated breakdown
});
```

---

### 3. Testing Checklist

**User Flow:**
- [ ] Open SOS screen
- [ ] Fill form completely
- [ ] Click [📤 Envoyer]
- [ ] See loading overlay
- [ ] Navigate to waiting screen
- [ ] See "PENDING" status
- [ ] Wait for garage accept
- [ ] Auto-navigate to tracking
- [ ] See garage on map
- [ ] See ETA updating

**Garage Flow:**
- [ ] Receive notification
- [ ] Notification displays correctly
- [ ] Tap notification
- [ ] App opens to details
- [ ] See breakdown info
- [ ] Click [✅ Accepter]
- [ ] Confirm action
- [ ] See success message
- [ ] Navigate to client

**Backend:**
- [ ] Breakdown created (201)
- [ ] Garages found
- [ ] FCM sent successfully
- [ ] Accept endpoint works (200)
- [ ] Status updated in DB
- [ ] Logs show full flow

---

## 📖 Related Documents

- **[SOS_COMPLETE_FLOW_GUIDE.md](SOS_COMPLETE_FLOW_GUIDE.md)** - Detailed documentation
- **[SOS_QUICK_TEST_GUIDE.md](SOS_QUICK_TEST_GUIDE.md)** - Quick testing steps
- **[NOTIFICATIONS_GUIDE.md](NOTIFICATIONS_GUIDE.md)** - FCM setup
- **[BACKEND_SOS_NOTIFICATION_IMPLEMENTATION.md](BACKEND_SOS_NOTIFICATION_IMPLEMENTATION.md)** - Backend code

---

**Last Updated:** December 5, 2025  
**Version:** 1.0  
**Status:** ✅ Ready for implementation & testing

