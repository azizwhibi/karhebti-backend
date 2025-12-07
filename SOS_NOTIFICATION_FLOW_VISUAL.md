# 🔄 SOS Notification Flow - Complete Visual Guide

## 📱 Current vs Required Flow

### ❌ CURRENT FLOW (NOT WORKING)
```
┌─────────────────┐
│   User Phone    │
│   (SOS Screen)  │
└────────┬────────┘
         │ 1. Select location on map
         │ 2. Select problem type: PNEU
         │ 3. Click "Envoyer"
         ▼
┌─────────────────────────────────────┐
│     POST /api/breakdowns            │
│  {                                  │
│    type: "PNEU",                    │
│    latitude: 36.8065,               │
│    longitude: 10.1815,              │
│    description: "Pneu crevé"        │
│  }                                  │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│        Backend Server               │
│  ✅ JWT Auth Successful             │
│  ✅ User Validated                  │
│  ✅ Breakdown Created in DB         │
│  ❌ NO NOTIFICATION SENT ← PROBLEM  │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  User Phone     │
│  Waiting Screen │
│  ⏳ Polling...  │
│  (Forever...)   │
└─────────────────┘

┌─────────────────┐
│  Garage Owner   │
│  📱 Phone       │
│  ❌ No notif    │  ← GARAGE OWNER NEVER KNOWS!
│  ❌ Silent      │
└─────────────────┘
```

---

### ✅ REQUIRED FLOW (AFTER FIX)
```
┌─────────────────┐
│   User Phone    │
│   (SOS Screen)  │
└────────┬────────┘
         │ 1. Select location on map
         │ 2. Select problem type: PNEU
         │ 3. Click "Envoyer"
         ▼
┌─────────────────────────────────────┐
│     POST /api/breakdowns            │
│  {                                  │
│    type: "PNEU",                    │
│    latitude: 36.8065,               │
│    longitude: 10.1815               │
│  }                                  │
└────────┬────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────┐
│              Backend Server                      │
│  ✅ JWT Auth Successful                          │
│  ✅ User Validated                               │
│  ✅ Breakdown Created in DB (ID: 65xxx)          │
│  ✅ Find garage owners (role: propGarage)        │
│  ✅ Found: prop.garage@example.com               │
│  ✅ Has FCM token: eXXXXX...                     │
│  ✅ Send FCM notification                        │
│  ✅ Save notification in DB                      │
└────────┬─────────────────────┬────────────────────┘
         │                     │
         │                     └─────────────────────┐
         │                                           │
         ▼                                           ▼
┌─────────────────┐                    ┌─────────────────────┐
│  User Phone     │                    │  Firebase Cloud     │
│  Waiting Screen │                    │  Messaging (FCM)    │
│  ⏳ Polling...  │                    └──────────┬──────────┘
└─────────────────┘                               │
                                                  │
                                                  ▼
                                    ┌──────────────────────────┐
                                    │   Garage Owner Phone     │
                                    │   🔔 NOTIFICATION!       │
                                    │                          │
                                    │   🚨 Nouvelle demande    │
                                    │      SOS                 │
                                    │   Assistance PNEU        │
                                    │   demandée               │
                                    └──────────┬───────────────┘
                                               │
                                               │ User taps notification
                                               ▼
                                    ┌──────────────────────────┐
                                    │   Garage App Opens       │
                                    │   📍 SOS Details Screen  │
                                    │                          │
                                    │   Type: PNEU             │
                                    │   Location: 36.8, 10.1   │
                                    │   Description: ...       │
                                    │                          │
                                    │   [Accepter] [Refuser]   │
                                    └──────────┬───────────────┘
                                               │
                                               │ Owner clicks "Accepter"
                                               ▼
                                    ┌──────────────────────────┐
                                    │   PUT /api/breakdowns/   │
                                    │        65xxx/accept      │
                                    └──────────┬───────────────┘
                                               │
                                               ▼
┌──────────────────────────────────────────────────────────┐
│              Backend Server                              │
│  ✅ Update breakdown status → ACCEPTED                   │
│  ✅ Assign garage to breakdown                           │
│  ✅ Send notification to user (optional)                 │
└────────┬─────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│       User Phone                    │
│  ⏳ Polling detects status change   │
│  ✅ Status: ACCEPTED                │
│  ✅ Navigate to Tracking Screen     │
│                                     │
│  🗺️ Map showing:                   │
│     - Garage location               │
│     - User location                 │
│     - Route between them            │
│     - ETA: 15 minutes               │
└─────────────────────────────────────┘
```

---

## 🔑 Key Points

### 1. Garage Owner FCM Token Registration
```
┌─────────────────────┐
│  Garage Owner       │
│  First Login        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Android App (Garage)              │
│   FirebaseMessaging.getInstance()   │
│        .token.await()               │
│   → Token: eXXXXXXXXXXXX...        │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   POST /api/auth/update-fcm-token   │
│   {                                 │
│     fcmToken: "eXXXXXXX..."         │
│   }                                 │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│   Backend saves to User document    │
│   users.update({                    │
│     _id: 6932f6f96551fb27afecc516   │
│   }, {                              │
│     fcmToken: "eXXXXXX..."          │
│   })                                │
└─────────────────────────────────────┘
```

### 2. Backend Notification Logic
```typescript
// After creating breakdown
async create(userId, createBreakdownDto) {
  // 1. Create breakdown
  const breakdown = await this.breakdownModel.create({...});
  
  // 2. 🚨 CRITICAL: Send notifications
  await this.sendNotificationsToNearbyGarages(breakdown);
  
  return breakdown;
}

async sendNotificationsToNearbyGarages(breakdown) {
  // Find garage owners
  const garageOwners = await this.userModel.find({
    role: 'propGarage',
    emailVerified: true
  });
  
  // Send FCM to each
  for (const owner of garageOwners) {
    if (!owner.fcmToken) continue;
    
    await this.firebaseAdmin.messaging().send({
      token: owner.fcmToken,
      notification: {
        title: '🚨 Nouvelle demande SOS',
        body: `Assistance ${breakdown.type} demandée`
      },
      data: {
        type: 'NEW_BREAKDOWN',
        breakdownId: breakdown._id.toString(),
        latitude: breakdown.latitude.toString(),
        longitude: breakdown.longitude.toString()
      }
    });
  }
}
```

### 3. Android App Notification Handling
```kotlin
// In FirebaseMessagingService
override fun onMessageReceived(remoteMessage: RemoteMessage) {
  val data = remoteMessage.data
  
  when (data["type"]) {
    "NEW_BREAKDOWN" -> {
      val breakdownId = data["breakdownId"]
      val latitude = data["latitude"]?.toDoubleOrNull()
      val longitude = data["longitude"]?.toDoubleOrNull()
      
      // Show notification
      showNotification(
        title = "🚨 Nouvelle demande SOS",
        message = "Assistance demandée à proximité",
        data = data
      )
      
      // Navigate to SOS details when tapped
      val intent = Intent(this, SOSDetailsActivity::class.java).apply {
        putExtra("breakdownId", breakdownId)
        putExtra("latitude", latitude)
        putExtra("longitude", longitude)
      }
    }
  }
}
```

---

## 🧪 Testing Checklist

### Pre-Test Setup:
- [ ] Backend running on port 3000
- [ ] MongoDB connected and accessible
- [ ] Firebase Admin SDK initialized
- [ ] Garage owner exists: `prop.garage@example.com`
- [ ] Garage owner has logged in to Android app
- [ ] FCM token saved in user document
- [ ] User app and garage app both installed

### Test Steps:

#### 1. Verify Garage Owner Setup
```bash
# Run diagnostic script
node check-garage-setup.js

Expected Output:
✅ Garage Owner Found: prop.garage@example.com
✅ FCM Token: EXISTS
```

#### 2. Send SOS from User App
```
1. Open user app
2. Go to SOS screen
3. Select location (tap map or use GPS)
4. Select problem type: PNEU
5. Add description: "Pneu crevé"
6. Click "Envoyer"
7. Wait for loading to complete
```

#### 3. Check Backend Logs
```
Expected logs:
✅ POST /api/breakdowns 201
✅ Breakdown created: 65xxx...
✅ Looking for nearby garages...
✅ Found 1 verified garage owners
✅ Notification sent to prop.garage@example.com
✅ FCM Response: projects/.../messages/0:xxx
```

#### 4. Check Garage Owner's Phone
```
Expected:
🔔 Push notification appears in status bar
   Title: "🚨 Nouvelle demande SOS"
   Body: "Assistance PNEU demandée"
   
When tapped:
✅ Opens garage app
✅ Shows SOS details screen
✅ Shows map with user location
✅ Shows [Accepter] [Refuser] buttons
```

#### 5. Garage Owner Accepts
```
1. Tap "Accepter" button
2. Confirmation dialog appears
3. Click "Confirmer"
4. Backend receives acceptance
5. Status updated to ACCEPTED
```

#### 6. User App Updates
```
Expected:
✅ Polling detects status = ACCEPTED
✅ Auto-navigates to tracking screen
✅ Shows garage location on map
✅ Shows route and ETA
✅ Shows garage details (name, phone)
```

---

## 🚨 Common Issues & Solutions

### Issue 1: "No FCM token for garage owner"
**Cause:** Garage owner hasn't logged in to Android app
**Solution:** 
1. Open garage Android app
2. Log in with `prop.garage@example.com`
3. App will automatically register token
4. Verify token saved: `node check-garage-setup.js`

---

### Issue 2: "Notification sent but not received"
**Possible Causes:**
- Phone has no internet connection
- Google Play Services not installed (emulator issue)
- Notification permissions denied
- App force-stopped in Android settings
- FCM channel not created in app

**Solutions:**
1. Check internet connection
2. Enable notification permissions in Settings
3. Verify FCM channel created:
```kotlin
val channel = NotificationChannel(
  "sos_notifications",
  "SOS Notifications", 
  NotificationManager.IMPORTANCE_HIGH
)
notificationManager.createNotificationChannel(channel)
```

---

### Issue 3: "Firebase Admin not initialized"
**Cause:** Missing Firebase credentials in `.env`
**Solution:**
```bash
# Add to backend .env file
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@...
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
```

---

### Issue 4: "Found 0 garage owners"
**Cause:** No users with `role: propGarage` in database
**Solution:**
```javascript
// In MongoDB or using Postman
db.users.updateOne(
  { email: "prop.garage@example.com" },
  { $set: { role: "propGarage", emailVerified: true } }
)
```

---

## 📊 Success Metrics

After implementing the fix, you should see:

| Metric | Before | After |
|--------|--------|-------|
| Notifications sent | 0 | 1+ per SOS |
| Backend logs "Found X garages" | ❌ No | ✅ Yes |
| Backend logs "Notification sent" | ❌ No | ✅ Yes |
| Garage owner receives push | ❌ No | ✅ Yes |
| SOS status changes to ACCEPTED | ❌ Stuck PENDING | ✅ Yes |
| User sees tracking screen | ❌ No | ✅ Yes |

---

## 🎯 Implementation Checklist

### Backend Changes:
- [ ] Add `sendNotificationsToNearbyGarages()` method
- [ ] Call it after breakdown creation
- [ ] Import Firebase Admin SDK
- [ ] Add FCM credentials to `.env`
- [ ] Add logging for notification sending
- [ ] Test with Postman or cURL

### Testing:
- [ ] Run `check-garage-setup.js`
- [ ] Verify garage owner has FCM token
- [ ] Send test SOS from user app
- [ ] Verify backend logs show notification sent
- [ ] Verify garage owner receives push notification
- [ ] Test acceptance flow end-to-end

### Monitoring:
- [ ] Monitor backend logs for errors
- [ ] Monitor FCM send success rate
- [ ] Track notification delivery time
- [ ] Track SOS acceptance rate

---

## 🎉 Expected Result

After implementing the backend fix:

1. ✅ User sends SOS → Backend creates breakdown
2. ✅ Backend finds garage owners automatically
3. ✅ Backend sends FCM push notification
4. ✅ Garage owner receives notification immediately
5. ✅ Garage owner taps notification → Opens app
6. ✅ Garage owner sees SOS details with map
7. ✅ Garage owner clicks "Accepter"
8. ✅ User app detects acceptance
9. ✅ User navigates to tracking screen
10. ✅ Complete SOS flow works end-to-end!

**The Android app is 100% ready. Only backend notification logic needs to be implemented!** 🚀

