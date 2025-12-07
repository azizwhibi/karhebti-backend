# 🚨 SOS Complete Flow Guide - Expected Behavior

## 📋 Table of Contents
1. [Flow Overview](#flow-overview)
2. [Step-by-Step Timeline](#step-by-step-timeline)
3. [User Journey](#user-journey)
4. [Garage Owner Journey](#garage-owner-journey)
5. [Backend Processing](#backend-processing)
6. [Technical Implementation](#technical-implementation)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Flow Overview

```
USER                    BACKEND                 GARAGE OWNER
  │                        │                         │
  ├─1. Send SOS Request──►│                         │
  │                        │                         │
  │                        ├─2. Create Breakdown    │
  │                        │   (status: PENDING)     │
  │                        │                         │
  │                        ├─3. Find Nearby Garages │
  │                        │                         │
  │                        ├─4. Send FCM Notification►
  │                        │                         │
  │                        │                    5. Receive Notification
  │                        │                         │
  │                        │                    6. Tap & View Details
  │                        │                         │
  │                        │                    7. Accept Request
  │                        │                         │
  │                        │◄────8. Update Status────┤
  │                        │   (status: ACCEPTED)    │
  │                        │                         │
  ├─9. Poll & Detect─────►│                         │
  │   Status Change        │                         │
  │                        │                         │
  10. Navigate to Tracking│                         │
  │                        │                         │
  11. See Garage Location  │                         │
      & ETA                │                         │
```

---

## ⏱️ Step-by-Step Timeline (0-12 seconds)

### **0:00 - User Sends SOS**

```
┌────────────────────────────────┐
│   📱 User's Phone              │
│                                │
│   🚨 Demande SOS              │
│                                │
│   Type: ● PNEU ▼              │
│   ○ BATTERIE                   │
│   ○ MOTEUR                     │
│   ○ CARBURANT                  │
│                                │
│   📝 Description:              │
│   ┌──────────────────────────┐│
│   │ Pneu crevé sur autoroute ││
│   │                          ││
│   └──────────────────────────┘│
│                                │
│   📍 Votre position:           │
│   [🗺️ Interactive Map]         │
│   📌 36.8065, 10.1815          │
│                                │
│   ┌──────────────────────────┐│
│   │  📤 Envoyer              ││ ← User clicks
│   └──────────────────────────┘│
└────────────────────────────────┘
```

**Action:** User fills form and clicks "📤 Envoyer"

---

### **0:01 - Backend Creates Breakdown**

```
Backend Terminal:
───────────────────────────────────
✅ POST /api/breakdowns
   Status: 201 Created
   Time: 203ms

✅ JWT Auth Successful
   User: user@example.com

✅ Breakdown created successfully
   ID: 6756e8f8...
   Type: PNEU
   Status: PENDING
   Location: 36.8065, 10.1815
```

**Database State:**
```json
{
  "id": "6756e8f8",
  "userId": 123,
  "type": "PNEU",
  "description": "Pneu crevé sur autoroute",
  "latitude": 36.8065,
  "longitude": 10.1815,
  "status": "PENDING",
  "createdAt": "2025-12-05T10:00:01Z"
}
```

---

### **0:02-0:03 - Backend Finds & Notifies Garages**

```
Backend Terminal:
───────────────────────────────────
🔍 Looking for nearby garages...
   Search radius: 10 km
   Breakdown location: 36.8065, 10.1815

📊 Search results:
   ✓ Found 1 verified garage owner(s)
   
👥 Garage owners to notify:
   1. prop.garage@example.com
      - Distance: 5.2 km
      - FCM Token: eYxRk7F_Sa2...
      - Status: Verified ✓

📤 Sending notification to prop.garage@example.com...
🔐 FCM Token: eYxRk7F_Sa2...

✅ Notification sent successfully!
   Response: projects/karhebti/messages/0:1234567890
   Message ID: 0:1234567890

💾 Notification saved to database
   Type: BREAKDOWN_REQUEST
   RecipientId: 456
   BreakdownId: 6756e8f8

📊 Summary:
   ✅ Sent: 1
   ❌ Failed: 0
```

---

### **0:04 - Garage Owner Receives Notification**

```
┌────────────────────────────────┐
│   📱 Garage Owner's Phone      │
│   (prop.garage@example.com)    │
│                                │
│   🔔 NOTIFICATION APPEARS!     │
│                                │
│   ╔══════════════════════════╗ │
│   ║ 🚨 Nouvelle demande SOS  ║ │
│   ║ Assistance PNEU demandée ║ │
│   ║ Tap to view details      ║ │
│   ╚══════════════════════════╝ │
│                                │
│   [Sound plays] 🔊             │
│   [Phone vibrates] 📳          │
│   Pattern: 0.5s - 0.25s - 0.5s │
└────────────────────────────────┘
```

**Technical Details:**
- **Channel:** `sos_breakdown_requests`
- **Priority:** HIGH
- **Sound:** Default notification sound
- **Vibration:** Custom pattern [0, 500, 250, 500, 250, 500]
- **Auto-cancel:** true (dismisses when tapped)

---

### **0:05-0:06 - Garage Owner Taps Notification**

```
┌────────────────────────────────┐
│   📱 Garage App Opens          │
│   → NotificationActivity       │
│   → Parses notification data   │
│   → Navigates to SOS details   │
│                                │
│   🚨 Demande SOS              │
│                                │
│   📋 DÉTAILS:                  │
│   ──────────────────────────  │
│   Type: PNEU 🛞               │
│   Description:                 │
│   "Pneu crevé sur autoroute"  │
│                                │
│   📍 LOCALISATION:             │
│   [Interactive Map]            │
│   📌 36.8065, 10.1815          │
│   📏 Distance: 5.2 km          │
│   ⏱️ Temps trajet: ~15 min    │
│                                │
│   👤 CLIENT:                   │
│   Jean Dupont                  │
│   📞 +216 XX XXX XXX           │
│                                │
│   ┌──────────────────────────┐│
│   │  ✅ Accepter             ││
│   └──────────────────────────┘│
│   ┌──────────────────────────┐│
│   │  ❌ Refuser              ││
│   └──────────────────────────┘│
└────────────────────────────────┘
```

---

### **0:07 - Garage Owner Accepts**

```
┌────────────────────────────────┐
│   ⚠️ Confirmation Dialog       │
│                                │
│   Accepter cette demande SOS?  │
│                                │
│   📋 Vous vous engagez à:      │
│   ✓ Vous rendre sur place      │
│   ✓ Arriver dans 15-20 min     │
│   ✓ Apporter le matériel       │
│     nécessaire (PNEU)          │
│                                │
│   [Annuler]    [Confirmer] ◄── Click
└────────────────────────────────┘
          │
          │ User confirms
          ▼
┌────────────────────────────────┐
│   ✅ Demande acceptée!         │
│                                │
│   🗺️ Navigation démarrée      │
│   Direction: Client            │
│   Distance: 5.2 km             │
│   ETA: 15 minutes              │
│                                │
│   [🎯 Voir l'itinéraire]       │
└────────────────────────────────┘
```

---

### **0:08 - Backend Updates Status**

```
Backend Terminal:
───────────────────────────────────
✅ PUT /api/breakdowns/6756e8f8/accept
   Status: 200 OK
   Time: 156ms

✅ JWT Auth Successful
   Garage: prop.garage@example.com

✅ Status updated successfully
   Breakdown ID: 6756e8f8
   New Status: ACCEPTED
   Assigned to: Garage #456
   Updated at: 2025-12-05T10:00:08Z

📊 Database updated:
   ✓ breakdown.status = "ACCEPTED"
   ✓ breakdown.acceptedBy = 456
   ✓ breakdown.acceptedAt = now()
```

**Database State (Updated):**
```json
{
  "id": "6756e8f8",
  "userId": 123,
  "type": "PNEU",
  "status": "ACCEPTED",  ← Changed
  "acceptedBy": 456,      ← New
  "acceptedAt": "2025-12-05T10:00:08Z"  ← New
}
```

---

### **0:10 - User App Polls & Detects Change**

```
User App Logs:
───────────────────────────────────
📡 Polling breakdown status...
   Breakdown ID: 6756e8f8
   Interval: 5 seconds

GET /api/breakdowns/6756e8f8
✅ Response: 200 OK

📊 Status check:
   Previous: PENDING
   Current: ACCEPTED ◄── Change detected!

🎉 Status changed to ACCEPTED!
   Preparing navigation...
```

**SOSWaitingScreen Logic:**
```kotlin
LaunchedEffect(breakdownId) {
    while (true) {
        val result = onGetBreakdownStatus(breakdownId)
        result.onSuccess { breakdown ->
            when (breakdown.status.uppercase()) {
                "ACCEPTED" -> {
                    delay(1000) // Show success message
                    onGarageAccepted(breakdown) // Navigate!
                    return@LaunchedEffect
                }
            }
        }
        delay(5000) // Poll every 5 seconds
    }
}
```

---

### **0:11-0:12 - Auto-Navigation to Tracking**

```
┌────────────────────────────────┐
│   📱 User's Phone              │
│   [Automatically navigates]    │
│                                │
│   ✅ Status: ACCEPTED          │
│   Auto-navigating...           │
└────────────────────────────────┘
          │
          │ Automatic transition
          ▼
┌────────────────────────────────┐
│   🎉 Garage trouvé!            │
│                                │
│   📍 Tracking Screen           │
│                                │
│   [Interactive Map]            │
│   ┌──────────────────────────┐│
│   │  🏢 Garage               ││
│   │   ↓                      ││
│   │   └─── 5.2 km ───┐       ││
│   │                   ↓       ││
│   │                  📌 You   ││
│   └──────────────────────────┘│
│                                │
│   🚗 Garage: Auto Service Pro  │
│   📞 +216 XX XXX XXX           │
│   ⏱️ Arrivée estimée: 15 min  │
│   📏 Distance: 5.2 km          │
│                                │
│   ┌──────────────────────────┐│
│   │  📞 Appeler le garage    ││
│   └──────────────────────────┘│
│   ┌──────────────────────────┐│
│   │  🗺️ Voir l'itinéraire   ││
│   └──────────────────────────┘│
└────────────────────────────────┘
```

**Features on Tracking Screen:**
- ✅ Real-time garage location (simulated movement)
- ✅ User's breakdown location
- ✅ Distance calculation
- ✅ ETA (Estimated Time of Arrival)
- ✅ Call button
- ✅ Route visualization
- ✅ Periodic updates (every 3 seconds)

---

## 👤 User Journey (Complete)

### 1. Opening SOS Screen

**Screen:** `BreakdownSOSScreen`

**Initial Checks:**
1. ✅ Check location permission
2. ✅ Check GPS enabled
3. ✅ Fetch current location
4. ✅ Display map with marker

**User Actions:**
- Select breakdown type (PNEU, BATTERIE, etc.)
- Enter description
- Confirm location on map
- (Optional) Choose manual location if GPS fails
- Click "📤 Envoyer"

---

### 2. Confirmation Dialog

**Displays:**
- ✓ Breakdown type
- ✓ Description
- ✓ Location coordinates
- ✓ Warning about commitment

**User Confirms:** Sends request to backend

---

### 3. Waiting Screen

**Screen:** `SOSWaitingScreen`

**Features:**
- 🔄 Animated loading indicator
- 📡 Polls backend every 5 seconds
- ⏳ Shows "PENDING" status
- ✅ Auto-navigates when "ACCEPTED"

**Display:**
```
⏳ En attente de réponse

Le garage examine votre demande...
Vous serez notifié dès qu'il répond.

Vérification: Toutes les 5 secondes
```

---

### 4. Tracking Screen

**Screen:** `GarageTrackingScreen`

**Real-time Features:**
- 🗺️ Live map with two markers
- 🚗 Garage position (green)
- 📌 User position (red)
- 📏 Distance updates
- ⏱️ ETA countdown
- 📞 Quick call button

**Simulation:**
- Garage moves closer every 3 seconds
- Distance decreases progressively
- ETA updates: `distance * 3 minutes per km`

---

## 🏪 Garage Owner Journey

### 1. Receiving Notification

**Service:** `KarhebtiMessagingService`

**Notification Details:**
```kotlin
Channel: "sos_breakdown_requests"
Priority: HIGH
Title: "🚨 Nouvelle demande SOS"
Body: "Assistance PNEU demandée"
Sound: ✓
Vibration: ✓ (Custom pattern)
Auto-cancel: true
```

**Data Payload:**
```json
{
  "type": "BREAKDOWN_REQUEST",
  "breakdownId": "6756e8f8",
  "breakdownType": "PNEU",
  "latitude": "36.8065",
  "longitude": "10.1815"
}
```

---

### 2. Opening Notification

**Flow:**
1. User taps notification
2. Opens `MainActivity`
3. Receives intent extras:
   ```kotlin
   intent.putExtra("from_notification", true)
   intent.putExtra("notification_type", "BREAKDOWN_REQUEST")
   intent.putExtra("breakdownId", "6756e8f8")
   ```
4. Navigates to breakdown details screen

---

### 3. Viewing SOS Details

**Expected Screen:** (To be implemented)

**Should Display:**
- 🚨 Breakdown type
- 📝 Description
- 📍 Location on map
- 📏 Distance from garage
- ⏱️ Estimated travel time
- 👤 Client info (name, phone)
- ✅ Accept button
- ❌ Refuse button

---

### 4. Accepting Request

**API Call:**
```http
PUT /api/breakdowns/6756e8f8/accept
Authorization: Bearer {garage_token}
```

**Backend Response:**
```json
{
  "id": "6756e8f8",
  "status": "ACCEPTED",
  "acceptedBy": 456,
  "message": "Breakdown accepted successfully"
}
```

---

### 5. Navigation to Client

**Expected Features:**
- 🗺️ Google Maps or OpenStreetMap integration
- 📍 Client's exact location
- 🧭 Turn-by-turn directions
- 📞 Quick call to client
- ✅ "I've arrived" button

---

## 🖥️ Backend Processing

### 1. Receiving SOS Request

**Endpoint:** `POST /api/breakdowns`

**Request Body:**
```json
{
  "vehicleId": null,
  "type": "PNEU",
  "description": "Pneu crevé sur autoroute",
  "latitude": 36.8065,
  "longitude": 10.1815,
  "photo": null
}
```

**Backend Actions:**
```javascript
1. Authenticate user (JWT)
2. Validate request data
3. Create breakdown in database
4. Set status = "PENDING"
5. Return breakdown object with ID
```

---

### 2. Finding Nearby Garages

**Logic:**
```javascript
// Find verified garage owners within radius
const radius = 10; // km

const garages = await Garage.findAll({
  where: {
    verified: true,
    // Haversine formula for distance
    [Op.and]: [
      sequelize.literal(`
        (6371 * acos(cos(radians(${lat})) 
        * cos(radians(latitude)) 
        * cos(radians(longitude) - radians(${lon})) 
        + sin(radians(${lat})) 
        * sin(radians(latitude)))) < ${radius}
      `)
    ]
  },
  include: [{ model: User, where: { role: 'garage_owner' } }]
});
```

---

### 3. Sending FCM Notifications

**Service:** Firebase Cloud Messaging

**Notification Payload:**
```json
{
  "notification": {
    "title": "🚨 Nouvelle demande SOS",
    "body": "Assistance PNEU demandée"
  },
  "data": {
    "type": "BREAKDOWN_REQUEST",
    "breakdownId": "6756e8f8",
    "breakdownType": "PNEU",
    "latitude": "36.8065",
    "longitude": "10.1815"
  },
  "token": "eYxRk7F_Sa2...",
  "android": {
    "priority": "high",
    "notification": {
      "channel_id": "sos_breakdown_requests",
      "sound": "default",
      "vibrate_timings": ["0.5s", "0.25s", "0.5s"]
    }
  }
}
```

**Success Response:**
```json
{
  "name": "projects/karhebti/messages/0:1234567890",
  "success": true
}
```

---

### 4. Updating Status on Accept

**Endpoint:** `PUT /api/breakdowns/:id/accept`

**Backend Actions:**
```javascript
1. Authenticate garage owner (JWT)
2. Verify garage is verified
3. Check breakdown still PENDING
4. Update breakdown:
   - status = "ACCEPTED"
   - acceptedBy = garageId
   - acceptedAt = now()
5. (Optional) Send push to user
6. Return updated breakdown
```

---

## 🔧 Technical Implementation

### Key Files

#### 1. **User App - SOS Flow**
```
BreakdownSOSScreen.kt
├─ Location permission handling
├─ GPS error handling with manual fallback
├─ Form validation
├─ API call to create breakdown
└─ Navigate to waiting screen

SOSWaitingScreen.kt
├─ Polling mechanism (every 5s)
├─ Status detection (PENDING → ACCEPTED)
├─ Auto-navigation on ACCEPTED
└─ Error handling

GarageTrackingScreen.kt
├─ Real-time map display
├─ Garage location simulation
├─ Distance & ETA calculation
└─ Call button integration
```

#### 2. **Notification Service**
```
KarhebtiMessagingService.kt
├─ onMessageReceived handler
├─ Notification channel creation
├─ High-priority notification display
└─ Intent extras for navigation

FCMTokenService.kt
├─ Token registration
├─ Auto-update on new token
└─ Backend sync
```

#### 3. **Backend (Expected)**
```
BreakdownController.js
├─ POST /api/breakdowns (create)
├─ GET /api/breakdowns/:id (get status)
├─ PUT /api/breakdowns/:id/accept
└─ PUT /api/breakdowns/:id/refuse

NotificationService.js
├─ findNearbyGarages(lat, lon, radius)
├─ sendFCMNotification(token, data)
└─ saveNotificationLog()
```

---

### Critical Code Snippets

#### User App - Polling Logic
```kotlin
// SOSWaitingScreen.kt
LaunchedEffect(breakdownId) {
    while (true) {
        val result = onGetBreakdownStatus(breakdownId)
        result.onSuccess { breakdown ->
            breakdownStatus = breakdown
            
            when (breakdown.status.uppercase()) {
                "ACCEPTED" -> {
                    delay(1000)
                    onGarageAccepted(breakdown)
                    return@LaunchedEffect
                }
                "REFUSED", "CANCELLED" -> {
                    onGarageRefused()
                    return@LaunchedEffect
                }
            }
        }
        delay(5000) // Poll every 5 seconds
    }
}
```

#### Notification Handling
```kotlin
// KarhebtiMessagingService.kt
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    val notificationType = remoteMessage.data["type"]
    
    val channelId = if (notificationType == "BREAKDOWN_REQUEST") {
        CHANNEL_ID_SOS // High priority channel
    } else {
        CHANNEL_ID_DOCUMENT
    }
    
    showNotification(title, body, data, channelId, notificationType)
}
```

---

## 🐛 Troubleshooting

### Common Issues & Solutions

#### 1. **Notification Not Received**

**Symptoms:**
- Backend logs show "Notification sent successfully"
- Garage owner's phone doesn't show notification

**Possible Causes:**
- ❌ FCM token not registered
- ❌ App not running in background
- ❌ Notification channel not created
- ❌ Do Not Disturb mode enabled

**Solutions:**
```bash
# Check FCM token registration
adb logcat | findstr "FCM Token"

# Verify notification channels
adb shell dumpsys notification | findstr "sos_breakdown"

# Test notification manually
curl -X POST https://fcm.googleapis.com/v1/projects/karhebti/messages:send \
  -H "Authorization: Bearer {SERVER_KEY}" \
  -d '{"message": {...}}'
```

---

#### 2. **Polling Not Working**

**Symptoms:**
- User stuck on "En attente de réponse"
- Status doesn't update even after garage accepts

**Possible Causes:**
- ❌ Network error
- ❌ Backend endpoint down
- ❌ JWT token expired

**Solutions:**
```kotlin
// Add better error handling in SOSWaitingScreen
result.onFailure { error ->
    Log.e(TAG, "Polling failed: ${error.message}")
    errorMessage = error.message
    
    // Show retry button
    if (error is IOException) {
        // Network error - show offline message
    } else if (error.message?.contains("401") == true) {
        // Token expired - redirect to login
    }
}
```

---

#### 3. **Navigation Not Triggered**

**Symptoms:**
- Status changes to ACCEPTED
- Screen doesn't navigate to tracking

**Debug Steps:**
```kotlin
// Add logging in SOSWaitingScreen
when (breakdown.status.uppercase()) {
    "ACCEPTED" -> {
        Log.d(TAG, "✅ Status is ACCEPTED!")
        Log.d(TAG, "Calling onGarageAccepted...")
        delay(1000)
        onGarageAccepted(breakdown)
        Log.d(TAG, "Navigation callback completed")
        return@LaunchedEffect
    }
}
```

**Check NavGraph:**
```kotlin
// Ensure route is properly configured
Screen.SOSWaiting.route -> "sos/waiting/{breakdownId}"
Screen.GarageTracking.route -> "sos/tracking/{breakdownId}"
```

---

#### 4. **Backend Not Finding Garages**

**Symptoms:**
- Backend logs: "Found 0 garage owners"
- No notifications sent

**Possible Causes:**
- ❌ No garages within radius
- ❌ Garages not verified
- ❌ No FCM tokens registered

**Debug Queries:**
```sql
-- Check garage data
SELECT id, name, latitude, longitude, verified 
FROM garages 
WHERE verified = true;

-- Check FCM tokens
SELECT u.email, u.role, d.fcm_token 
FROM users u 
JOIN devices d ON u.id = d.user_id 
WHERE u.role = 'garage_owner';

-- Calculate distance
SELECT 
  id, 
  name,
  (6371 * acos(cos(radians(36.8065)) 
  * cos(radians(latitude)) 
  * cos(radians(longitude) - radians(10.1815)) 
  + sin(radians(36.8065)) 
  * sin(radians(latitude)))) AS distance
FROM garages 
WHERE verified = true
HAVING distance < 10
ORDER BY distance;
```

---

## ✅ Success Checklist

### User App
- [ ] Location permissions granted
- [ ] GPS enabled (or manual location selected)
- [ ] Breakdown type selected
- [ ] API call succeeds (201 Created)
- [ ] Navigate to waiting screen
- [ ] Polling starts successfully
- [ ] Status changes detected
- [ ] Navigate to tracking screen
- [ ] Map displays correctly
- [ ] Garage location visible

### Garage Owner App
- [ ] FCM token registered in backend
- [ ] App has notification permission
- [ ] Notification channel created
- [ ] Notification received & displayed
- [ ] Tap opens app correctly
- [ ] Breakdown details displayed
- [ ] Accept button functional
- [ ] API call succeeds (200 OK)
- [ ] Navigation to client works

### Backend
- [ ] JWT authentication working
- [ ] Breakdown created with PENDING status
- [ ] Nearby garages found
- [ ] FCM notifications sent
- [ ] Accept endpoint working
- [ ] Status updated to ACCEPTED
- [ ] Logs show full flow

---

## 📊 Expected Logs (Complete Session)

### Backend Terminal
```
═══════════════════════════════════
📥 POST /api/breakdowns
   Time: 2025-12-05 10:00:01
   Status: 201 Created
   Duration: 203ms
───────────────────────────────────
✅ JWT Auth Successful
   User: user@example.com (ID: 123)

✅ Breakdown created successfully
   ID: 6756e8f8
   Type: PNEU
   Description: Pneu crevé sur autoroute
   Location: 36.8065, 10.1815
   Status: PENDING

🔍 Looking for nearby garages...
   Search radius: 10 km
   Breakdown location: 36.8065, 10.1815

📊 Search results:
   ✓ Found 1 verified garage owner(s)

👥 Garage owners to notify:
   1. prop.garage@example.com
      - Garage ID: 456
      - Distance: 5.2 km
      - FCM Token: eYxRk7F_Sa2...
      - Status: Verified ✓

📤 Sending notification to prop.garage@example.com...
   Token: eYxRk7F_Sa2...
   Type: BREAKDOWN_REQUEST
   Breakdown ID: 6756e8f8

✅ Notification sent successfully!
   Response: projects/karhebti/messages/0:1234567890
   Message ID: 0:1234567890

💾 Notification saved to database
   Type: BREAKDOWN_REQUEST
   RecipientId: 456
   BreakdownId: 6756e8f8
   SentAt: 2025-12-05T10:00:03Z

📊 Summary:
   ✅ Sent: 1
   ❌ Failed: 0
───────────────────────────────────

═══════════════════════════════════
📥 PUT /api/breakdowns/6756e8f8/accept
   Time: 2025-12-05 10:00:08
   Status: 200 OK
   Duration: 156ms
───────────────────────────────────
✅ JWT Auth Successful
   Garage: prop.garage@example.com (ID: 456)

✅ Status updated successfully
   Breakdown ID: 6756e8f8
   Previous Status: PENDING
   New Status: ACCEPTED
   Accepted By: Garage #456
   Accepted At: 2025-12-05T10:00:08Z

📊 Database updated:
   ✓ breakdown.status = "ACCEPTED"
   ✓ breakdown.acceptedBy = 456
   ✓ breakdown.acceptedAt = now()
───────────────────────────────────
```

### User App Logs
```
D/BreakdownSOSScreen: ✅ Location fetched: 36.8065, 10.1815
D/BreakdownSOSScreen: 📤 Sending SOS request...
D/BreakdownSOSScreen: Request: {"type":"PNEU","latitude":36.8065,...}
D/BreakdownViewModel: ✅ Breakdown created: 6756e8f8
D/NavGraph: → Navigating to SOSWaitingScreen
D/SOSWaitingScreen: 📡 Starting polling for breakdown: 6756e8f8
D/SOSWaitingScreen: 📊 Status: PENDING
D/SOSWaitingScreen: 📊 Status: PENDING
D/SOSWaitingScreen: ✅ Status changed: ACCEPTED!
D/SOSWaitingScreen: 🎉 Garage accepted! Navigating...
D/NavGraph: → Navigating to GarageTrackingScreen
D/GarageTrackingScreen: 🗺️ Map initialized
D/GarageTrackingScreen: 📍 User: 36.8065, 10.1815
D/GarageTrackingScreen: 🏢 Garage: 36.7565, 10.1315
D/GarageTrackingScreen: 📏 Distance: 5.2 km
D/GarageTrackingScreen: ⏱️ ETA: 15 minutes
```

### Garage Owner App Logs
```
D/KarhebtiMessaging: ✅ MESSAGE REÇU!
D/KarhebtiMessaging: De: FCM
D/KarhebtiMessaging: Type: BREAKDOWN_REQUEST
D/KarhebtiMessaging: Data: {breakdownId=6756e8f8, type=PNEU, ...}
D/KarhebtiMessaging: 🔔 Création de la notification...
D/KarhebtiMessaging: ✅✅✅ NOTIFICATION AFFICHÉE
D/MainActivity: 📱 Launched from notification
D/MainActivity: Extras: {breakdownId=6756e8f8, type=BREAKDOWN_REQUEST}
D/NavGraph: → Navigating to BreakdownDetailsScreen
```

---

## 🎯 Next Steps

### Immediate Actions Required

1. **Garage Owner - SOS Details Screen** ❗
   - Create `GarageBreakdownDetailsScreen.kt`
   - Display breakdown info from notification
   - Add Accept/Refuse buttons
   - Integrate with backend API

2. **Backend - Accept/Refuse Endpoints** ✅ (Assumed working)
   - `PUT /api/breakdowns/:id/accept`
   - `PUT /api/breakdowns/:id/refuse`
   - Add validation and authorization

3. **Testing**
   - Test complete flow end-to-end
   - Verify notifications appear
   - Check polling mechanism
   - Test navigation transitions

---

## 📖 Related Documentation

- [SOS_QUICK_TEST_GUIDE.md](SOS_QUICK_TEST_GUIDE.md) - Quick testing steps
- [NOTIFICATIONS_GUIDE.md](NOTIFICATIONS_GUIDE.md) - FCM setup
- [MANUAL_LOCATION_COMPLETE.md](MANUAL_LOCATION_COMPLETE.md) - Location handling

---

**Last Updated:** December 5, 2025  
**Status:** ✅ Flow documented, ready for implementation testing

