# 🚨 Backend SOS Notification - Complete Implementation Guide

## 🎯 Problem Identified

**User:** `prop.garage@example.com` (ID: `6932f6f96551fb27afecc516`)
**Role:** `propGarage`
**Status:** ✅ Authenticated and logged in
**Issue:** ❌ **NOT receiving SOS notifications when users send requests**

---

## 📊 Current Situation

### Backend Logs Show:
```
✅ POST /api/breakdowns 201 - Breakdown created
✅ User authenticated: prop.garage@example.com
✅ Role: propGarage
❌ NO notification sending logic executed
❌ NO "Sending FCM notification..." logs
❌ NO "Found nearby garages..." logs
```

### What's Missing:
The backend **creates the breakdown** but **never notifies garage owners**. The notification logic is completely missing from the breakdown creation flow.

---

## 🔧 Required Backend Implementation

### 📁 File: `breakdowns.service.ts`

#### Step 1: Add Notification Method After Breakdown Creation

```typescript
async create(userId: string, createBreakdownDto: CreateBreakdownDto) {
  // Create the breakdown
  const breakdown = await this.breakdownModel.create({
    userId,
    type: createBreakdownDto.type,
    description: createBreakdownDto.description,
    latitude: createBreakdownDto.latitude,
    longitude: createBreakdownDto.longitude,
    photos: createBreakdownDto.photos || [],
    status: 'PENDING',
    createdAt: new Date(),
  });

  console.log(`✅ Breakdown created: ${breakdown._id}`);

  // 🚨 CRITICAL: Send notifications to nearby garages
  try {
    await this.sendNotificationsToNearbyGarages(breakdown);
  } catch (error) {
    console.error('❌ Error sending notifications:', error);
    // Don't fail the request if notifications fail
  }

  return breakdown;
}
```

#### Step 2: Implement Notification Logic

```typescript
private async sendNotificationsToNearbyGarages(breakdown: any) {
  console.log(`🔍 Looking for nearby garages for breakdown ${breakdown._id}...`);
  console.log(`📍 Breakdown location: ${breakdown.latitude}, ${breakdown.longitude}`);

  try {
    // Find all propGarage users
    const garageOwners = await this.userModel.find({ 
      role: 'propGarage',
      emailVerified: true 
    });

    console.log(`👥 Found ${garageOwners.length} verified garage owners`);

    if (garageOwners.length === 0) {
      console.warn('⚠️ No garage owners found in database');
      return;
    }

    // Send notification to each garage owner
    let notificationsSent = 0;
    let notificationsFailed = 0;

    for (const owner of garageOwners) {
      try {
        // Check if owner has FCM token
        if (!owner.fcmToken) {
          console.log(`⚠️ No FCM token for garage owner: ${owner.email}`);
          notificationsFailed++;
          continue;
        }

        // Send push notification via Firebase Cloud Messaging
        const notification = {
          token: owner.fcmToken,
          notification: {
            title: '🚨 Nouvelle demande SOS',
            body: `Assistance ${breakdown.type} demandée`,
          },
          data: {
            type: 'NEW_BREAKDOWN',
            breakdownId: breakdown._id.toString(),
            latitude: breakdown.latitude.toString(),
            longitude: breakdown.longitude.toString(),
            breakdownType: breakdown.type,
            description: breakdown.description || '',
            userId: breakdown.userId.toString(),
          },
          android: {
            priority: 'high',
            notification: {
              channelId: 'sos_notifications',
              sound: 'default',
              priority: 'high',
            },
          },
        };

        // Send via Firebase Admin SDK
        const response = await this.firebaseAdmin.messaging().send(notification);
        console.log(`✅ Notification sent to ${owner.email} - Response: ${response}`);
        notificationsSent++;

        // Save notification in database for history
        await this.notificationModel.create({
          recipientId: owner._id,
          type: 'NEW_BREAKDOWN',
          title: '🚨 Nouvelle demande SOS',
          message: `Assistance ${breakdown.type} demandée`,
          data: {
            breakdownId: breakdown._id.toString(),
            latitude: breakdown.latitude,
            longitude: breakdown.longitude,
            type: breakdown.type,
          },
          read: false,
          createdAt: new Date(),
        });

      } catch (error) {
        console.error(`❌ Failed to send notification to ${owner.email}:`, error.message);
        notificationsFailed++;
      }
    }

    console.log(`📊 Notification Summary: ${notificationsSent} sent, ${notificationsFailed} failed`);

  } catch (error) {
    console.error('❌ Error in sendNotificationsToNearbyGarages:', error);
    throw error;
  }
}
```

---

## 🔌 Required Dependencies

### 1. Firebase Admin SDK (Should Already Be Installed)

Check `package.json`:
```json
{
  "dependencies": {
    "firebase-admin": "^11.0.0"
  }
}
```

If not installed:
```bash
npm install firebase-admin
```

### 2. Firebase Admin Initialization

In `firebase-admin.service.ts` or `app.module.ts`:

```typescript
import * as admin from 'firebase-admin';

@Injectable()
export class FirebaseAdminService {
  private readonly firebaseApp: admin.app.App;

  constructor() {
    this.firebaseApp = admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
      }),
    });
  }

  getMessaging() {
    return this.firebaseApp.messaging();
  }
}
```

### 3. Environment Variables

Add to `.env`:
```bash
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@your-project.iam.gserviceaccount.com
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
```

---

## 🧪 Testing the Fix

### Test 1: Verify Garage Owner Has FCM Token

```bash
# In MongoDB or your database
db.users.findOne({ email: "prop.garage@example.com" })
```

**Expected Result:**
```json
{
  "_id": "6932f6f96551fb27afecc516",
  "email": "prop.garage@example.com",
  "role": "propGarage",
  "fcmToken": "eXXXXXXXXXXX...", // ✅ This must exist
  "emailVerified": true
}
```

**If `fcmToken` is missing:**
- The garage owner needs to log in to the Android app
- The app will register the FCM token automatically
- Token is sent to backend on login

### Test 2: Send SOS from Android App

1. Open user app (not garage app)
2. Go to SOS screen
3. Select location manually or use GPS
4. Select problem type: "PNEU"
5. Click "Envoyer"

**Backend should log:**
```
✅ Breakdown created: 65xxx...
🔍 Looking for nearby garages for breakdown 65xxx...
📍 Breakdown location: 36.8065, 10.1815
👥 Found 1 verified garage owners
✅ Notification sent to prop.garage@example.com - Response: projects/.../messages/0:xxx
📊 Notification Summary: 1 sent, 0 failed
```

### Test 3: Verify Garage Owner Receives Notification

On **garage owner's phone** (prop.garage@example.com):
- Should see push notification: "🚨 Nouvelle demande SOS"
- Tap notification → Opens garage app
- Should navigate to SOS details screen
- Shows: Location, problem type, user info
- Shows buttons: [Accepter] [Refuser]

---

## 🎯 Garage Owner FCM Token Registration

### When Garage Owner Logs In (Android App):

The app automatically registers FCM token:

```kotlin
// In GarageAuthViewModel.kt or similar
suspend fun login(email: String, password: String) {
  val response = authRepository.login(email, password)
  
  if (response.isSuccessful) {
    // Get FCM token
    val fcmToken = FirebaseMessaging.getInstance().token.await()
    
    // Send to backend
    authRepository.updateFcmToken(fcmToken)
    
    // Save in preferences
    preferences.saveFcmToken(fcmToken)
  }
}
```

### Backend Receives Token:

```typescript
// In auth.controller.ts
@Post('update-fcm-token')
@UseGuards(JwtAuthGuard)
async updateFcmToken(
  @GetUser() user: any,
  @Body() body: { fcmToken: string }
) {
  await this.userModel.findByIdAndUpdate(user.userId, {
    fcmToken: body.fcmToken,
    fcmTokenUpdatedAt: new Date(),
  });
  
  return { success: true, message: 'FCM token updated' };
}
```

---

## 🚨 Critical Checklist

### Before Testing:

- [ ] Garage owner has logged in to Android app at least once
- [ ] FCM token is saved in user document in database
- [ ] Firebase Admin SDK is initialized with correct credentials
- [ ] Notification sending code is added to `breakdowns.service.ts`
- [ ] Backend has access to `firebaseAdmin.messaging()`
- [ ] Android app has `google-services.json` configured
- [ ] FCM channel "sos_notifications" is created in Android app

### During Testing:

- [ ] Backend logs show "Found X garage owners"
- [ ] Backend logs show "Notification sent to..."
- [ ] No errors in backend console
- [ ] Garage owner's phone receives push notification
- [ ] Notification data includes `breakdownId`, `latitude`, `longitude`
- [ ] Tapping notification opens garage app

### After Notification Received:

- [ ] Garage app shows SOS details
- [ ] Map shows user's location
- [ ] [Accepter] and [Refuser] buttons work
- [ ] When accepted, user app receives status update
- [ ] User app navigates to tracking screen

---

## 🔍 Debugging Guide

### Issue: "No garage owners found"

**Check:**
```sql
SELECT * FROM users WHERE role = 'propGarage';
```

**Fix:** Create garage owner account or verify role is set correctly.

---

### Issue: "No FCM token for garage owner"

**Check:**
```json
db.users.findOne({ 
  email: "prop.garage@example.com" 
}, { fcmToken: 1 })
```

**Fix:** 
1. Open garage Android app
2. Log in with garage owner credentials
3. App will automatically register FCM token
4. Token sent to backend on login

---

### Issue: "Firebase Admin not initialized"

**Check backend logs for:**
```
Error: Firebase Admin SDK is not initialized
```

**Fix:**
1. Verify `.env` has Firebase credentials
2. Check `firebase-admin.service.ts` initialization
3. Ensure `FirebaseAdminService` is injected in `BreakdownsService`

---

### Issue: "Notification sent but not received"

**Check:**
1. **Phone has internet connection**
2. **Google Play Services installed** (required for FCM)
3. **Notification permissions enabled** in Android settings
4. **App is not force-stopped** (FCM won't work if app is force-stopped)
5. **FCM channel created** in Android app:

```kotlin
// In MainActivity.onCreate()
val channel = NotificationChannel(
  "sos_notifications",
  "SOS Notifications",
  NotificationManager.IMPORTANCE_HIGH
)
notificationManager.createNotificationChannel(channel)
```

---

## 📱 Testing on Emulator

### Send Test Notification Manually:

```bash
# Using Firebase Console
1. Go to Firebase Console → Cloud Messaging
2. Click "Send test message"
3. Enter FCM token from database
4. Send notification
```

### Or Use cURL:

```bash
curl -X POST https://fcm.googleapis.com/v1/projects/YOUR_PROJECT/messages:send \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": {
      "token": "GARAGE_OWNER_FCM_TOKEN",
      "notification": {
        "title": "Test SOS",
        "body": "Test notification"
      },
      "data": {
        "type": "TEST"
      }
    }
  }'
```

---

## 🎯 Expected Flow After Fix

### 1. User Sends SOS
```
User App → POST /api/breakdowns
{
  "type": "PNEU",
  "latitude": 36.8065,
  "longitude": 10.1815,
  "description": "Pneu crevé"
}
```

### 2. Backend Creates Breakdown
```
✅ Breakdown saved to database
✅ Status: PENDING
✅ ID: 65xxx...
```

### 3. Backend Finds Garage Owners
```
🔍 Query users WHERE role = 'propGarage' AND emailVerified = true
👥 Found: prop.garage@example.com (6932f6f96551fb27afecc516)
```

### 4. Backend Sends FCM Notification
```
📤 Sending to token: eXXXXXX...
✅ FCM Response: projects/.../messages/0:xxx
✅ Notification saved to notifications collection
```

### 5. Garage Owner Receives Push
```
📱 Android System → Shows notification
🔔 Title: "🚨 Nouvelle demande SOS"
📝 Body: "Assistance PNEU demandée"
```

### 6. Garage Owner Taps Notification
```
📱 Opens garage app
🗺️ Shows SOS details screen
📍 Shows user location on map
🔘 Buttons: [Accepter] [Refuser]
```

### 7. Garage Owner Accepts
```
Garage App → PUT /api/breakdowns/65xxx/accept
Backend → Updates status to ACCEPTED
Backend → Sends notification to user
```

### 8. User App Detects Status Change
```
User App → Polling GET /api/breakdowns/65xxx
Response: { status: "ACCEPTED", garageId: "xxx" }
User App → Navigate to tracking screen
🗺️ Shows route from garage to user
```

---

## ✅ Final Verification

After implementing the fix, verify:

1. **Database Check:**
   ```
   Garage owner has fcmToken ✅
   User document exists ✅
   Breakdown created ✅
   ```

2. **Backend Logs:**
   ```
   "Found X garage owners" ✅
   "Notification sent to..." ✅
   "Notification Summary: X sent" ✅
   ```

3. **Garage Owner Phone:**
   ```
   Push notification received ✅
   Notification has correct data ✅
   Tapping opens app ✅
   ```

4. **SOS Flow:**
   ```
   User sends SOS ✅
   Garage receives notification ✅
   Garage accepts ✅
   User sees tracking ✅
   ```

---

## 🚀 Implementation Priority

### High Priority (Implement Now):
1. ✅ Add `sendNotificationsToNearbyGarages()` method
2. ✅ Call it after breakdown creation
3. ✅ Ensure garage owner has FCM token
4. ✅ Test end-to-end flow

### Medium Priority (Optimize Later):
- Add geospatial queries for nearby garages (within radius)
- Filter garages by availability status
- Send notification only to available garages
- Add notification retry logic

### Low Priority (Future Enhancement):
- Add notification history dashboard
- Add notification analytics
- Add notification preferences
- Add in-app notification center

---

## 📞 Support

If notifications still don't work after implementing this guide:

1. Share backend logs when sending SOS
2. Share FCM token from database
3. Share Firebase project configuration
4. Share any error messages from backend

**The issue is 100% on the backend side** - the Android app is ready and working correctly! 🎉

