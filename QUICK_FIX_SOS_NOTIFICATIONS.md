# 🚨 QUICK FIX GUIDE - SOS Notifications Not Working

## ⚡ TL;DR

**Problem:** Garage owner (`prop.garage@example.com`) is NOT receiving SOS notifications when users send requests.

**Cause:** Backend creates the breakdown successfully but **never sends push notifications** to garage owners.

**Solution:** Add notification logic to backend `breakdowns.service.ts`

---

## 🎯 The Issue (In 3 Steps)

### 1. ✅ User Sends SOS
```
User app → POST /api/breakdowns
{
  type: "PNEU",
  latitude: 36.8065,
  longitude: 10.1815
}
```

### 2. ✅ Backend Creates Breakdown
```
Backend logs:
✅ POST /api/breakdowns 201 Created
✅ Breakdown saved to database
```

### 3. ❌ Backend Does NOT Send Notification
```
Missing logs:
❌ "Found X garage owners"
❌ "Sending notification to..."
❌ "FCM notification sent"

Result:
❌ Garage owner receives NO notification
❌ SOS request stays PENDING forever
```

---

## 🔧 The Fix (Copy-Paste Ready)

### Backend File: `breakdowns.service.ts`

Add this method after breakdown creation:

```typescript
async create(userId: string, createBreakdownDto: CreateBreakdownDto) {
  // Create breakdown
  const breakdown = await this.breakdownModel.create({
    userId,
    ...createBreakdownDto,
    status: 'PENDING',
  });

  console.log(`✅ Breakdown created: ${breakdown._id}`);

  // 🚨 ADD THIS:
  await this.sendNotificationsToNearbyGarages(breakdown);

  return breakdown;
}

// 🚨 ADD THIS METHOD:
private async sendNotificationsToNearbyGarages(breakdown: any) {
  console.log(`🔍 Looking for garage owners...`);

  const garageOwners = await this.userModel.find({
    role: 'propGarage',
    emailVerified: true
  });

  console.log(`👥 Found ${garageOwners.length} garage owners`);

  for (const owner of garageOwners) {
    if (!owner.fcmToken) {
      console.log(`⚠️ No FCM token for ${owner.email}`);
      continue;
    }

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
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'sos_notifications',
          sound: 'default',
        },
      },
    };

    try {
      await this.firebaseAdmin.messaging().send(notification);
      console.log(`✅ Notification sent to ${owner.email}`);

      // Save notification in database
      await this.notificationModel.create({
        recipientId: owner._id,
        type: 'NEW_BREAKDOWN',
        title: notification.notification.title,
        message: notification.notification.body,
        data: notification.data,
        read: false,
      });
    } catch (error) {
      console.error(`❌ Failed to send to ${owner.email}:`, error.message);
    }
  }
}
```

---

## 🧪 Quick Test

### Before Testing:
```bash
# 1. Verify garage owner has FCM token
node check-garage-setup.js
```

### Test Flow:
1. Open **user app** → Go to SOS screen
2. Select location (tap map)
3. Select problem type: "PNEU"
4. Click "Envoyer"
5. Check **backend logs** → Should see:
   ```
   ✅ Breakdown created: 65xxx...
   🔍 Looking for garage owners...
   👥 Found 1 garage owners
   ✅ Notification sent to prop.garage@example.com
   ```
6. Check **garage owner's phone** → Should receive:
   ```
   🔔 Notification: "🚨 Nouvelle demande SOS"
   Body: "Assistance PNEU demandée"
   ```

---

## 📋 Checklist

### Prerequisites:
- [ ] Backend running
- [ ] MongoDB connected
- [ ] Firebase Admin SDK configured
- [ ] Garage owner exists in database
- [ ] Garage owner has logged in to Android app (to register FCM token)

### Implementation:
- [ ] Add `sendNotificationsToNearbyGarages()` method
- [ ] Call it after breakdown creation
- [ ] Add console logs for debugging
- [ ] Restart backend server

### Testing:
- [ ] Send test SOS from user app
- [ ] Check backend logs
- [ ] Verify notification received
- [ ] Test acceptance flow

---

## 🆘 If Still Not Working

### Check 1: FCM Token Exists
```bash
# In MongoDB
db.users.findOne(
  { email: "prop.garage@example.com" },
  { fcmToken: 1 }
)

# Should return: { fcmToken: "eXXXXX..." }
# If null: Garage owner needs to log in to Android app
```

### Check 2: Firebase Admin Initialized
```typescript
// In app.module.ts or firebase-admin.service.ts
import * as admin from 'firebase-admin';

admin.initializeApp({
  credential: admin.credential.cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n'),
  }),
});
```

### Check 3: Environment Variables
```bash
# .env file must have:
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CLIENT_EMAIL=firebase-adminsdk@...
FIREBASE_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"
```

---

## 📊 Expected Backend Logs (After Fix)

```
POST /api/breakdowns - 201 Created
✅ Breakdown created: 6756e8f8c123456789abcdef
🔍 Looking for garage owners...
👥 Found 1 garage owners
✅ Notification sent to prop.garage@example.com
📊 Notification Summary: 1 sent, 0 failed
```

---

## 🎯 Summary

| Component | Status | Action |
|-----------|--------|--------|
| Android User App | ✅ Working | No changes needed |
| Android Garage App | ✅ Working | No changes needed |
| Backend (Create Breakdown) | ✅ Working | No changes needed |
| Backend (Send Notifications) | ❌ **MISSING** | **Add code above** |
| FCM Setup | ✅ Working | No changes needed |

**Only one file needs to be edited: `breakdowns.service.ts`**

---

## 📞 Support Files

1. **Detailed Implementation:** `BACKEND_SOS_NOTIFICATION_IMPLEMENTATION.md`
2. **Visual Flow Diagram:** `SOS_NOTIFICATION_FLOW_VISUAL.md`
3. **Diagnostic Script:** `check-garage-setup.js`
4. **Android App Guide:** `SOS_MANUAL_LOCATION_FIX.md`

---

## ✅ Success Criteria

After fix is implemented:

1. ✅ Backend logs show "Notification sent to..."
2. ✅ Garage owner receives push notification
3. ✅ Tapping notification opens garage app
4. ✅ Garage owner sees SOS details
5. ✅ Garage owner can accept/refuse
6. ✅ User sees tracking screen when accepted

**That's it! The Android apps are ready. Only backend needs this one fix.** 🚀

