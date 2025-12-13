# 🔧 Backend Notification Fix Required

## 🎯 Problem
When a user sends an SOS request, the backend receives it successfully but **does NOT send push notifications to nearby garage owners (propGarage role)**.

### Current Situation
✅ Backend receives SOS request  
✅ Backend authenticates user correctly  
✅ Breakdown is created in database  
❌ **NO notification sent to garage owners**  
❌ Garage owners don't see the SOS request  

---

## 📊 Backend Logs Analysis

```
POST /api/breakdowns 201 - - 203.479 ms
✅ Authentication successful
✅ User found in database
✅ Breakdown created successfully

❌ NO logs showing:
- "Sending notification to garage..."
- "Push notification sent..."
- "FCM token found..."
```

---

## 🛠️ Required Backend Fixes

### 1. **breakdowns.service.ts** - Add Notification Logic

After creating a breakdown, the backend MUST:

```typescript
async create(userId: string, createBreakdownDto: CreateBreakdownDto) {
  // 1. Create breakdown
  const breakdown = await this.breakdownModel.create({
    userId,
    ...createBreakdownDto,
    status: 'PENDING',
  });

  // 2. 🚨 CRITICAL: Find nearby garages and send notifications
  await this.sendNotificationsToNearbyGarages(breakdown);

  return breakdown;
}

private async sendNotificationsToNearbyGarages(breakdown: Breakdown) {
  try {
    // Find garages within 50km radius
    const nearbyGarages = await this.garageModel.find({
      location: {
        $near: {
          $geometry: {
            type: 'Point',
            coordinates: [breakdown.longitude, breakdown.latitude]
          },
          $maxDistance: 50000 // 50km in meters
        }
      }
    });

    console.log(`📍 Found ${nearbyGarages.length} nearby garages for breakdown ${breakdown._id}`);

    // Send notification to each garage owner
    for (const garage of nearbyGarages) {
      // Find garage owner
      const owner = await this.userModel.findById(garage.ownerId);
      
      if (!owner || !owner.fcmToken) {
        console.log(`⚠️ No FCM token for garage ${garage.name}`);
        continue;
      }

      // Send push notification
      const notification = {
        token: owner.fcmToken,
        notification: {
          title: '🚨 Nouvelle demande SOS',
          body: `Assistance ${breakdown.type} demandée à proximité`,
        },
        data: {
          type: 'NEW_BREAKDOWN',
          breakdownId: breakdown._id.toString(),
          latitude: breakdown.latitude.toString(),
          longitude: breakdown.longitude.toString(),
          breakdownType: breakdown.type,
        },
      };

      await this.firebaseAdmin.messaging().send(notification);
      console.log(`✅ Notification sent to garage: ${garage.name}`);

      // Also save notification in database
      await this.notificationModel.create({
        recipientId: owner._id,
        type: 'NEW_BREAKDOWN',
        title: '🚨 Nouvelle demande SOS',
        message: `Assistance ${breakdown.type} demandée`,
        data: {
          breakdownId: breakdown._id,
          latitude: breakdown.latitude,
          longitude: breakdown.longitude,
        },
      });
    }
  } catch (error) {
    console.error('❌ Error sending notifications:', error);
  }
}
```

### 2. **garages.schema.ts** - Add Geospatial Index

Ensure garage schema has location with 2dsphere index:

```typescript
@Schema()
export class Garage {
  @Prop({
    type: {
      type: String,
      enum: ['Point'],
      default: 'Point',
    },
    coordinates: {
      type: [Number], // [longitude, latitude]
      required: true,
    },
  })
  location: {
    type: string;
    coordinates: number[];
  };

  // ... other fields
}

// Create index
GarageSchema.index({ location: '2dsphere' });
```

### 3. **users.schema.ts** - Ensure FCM Token Field

```typescript
@Schema()
export class User {
  @Prop({ required: false })
  fcmToken?: string; // Firebase Cloud Messaging token

  // ... other fields
}
```

### 4. **firebase-admin.service.ts** - Initialize Firebase Admin

```typescript
import * as admin from 'firebase-admin';

@Injectable()
export class FirebaseAdminService {
  private firebaseApp: admin.app.App;

  constructor() {
    // Initialize with service account
    this.firebaseApp = admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
      }),
    });
  }

  messaging() {
    return admin.messaging();
  }
}
```

---

## 🔍 Debug Steps

### 1. Check if garages have location data
```typescript
const garages = await this.garageModel.find({ ownerId: 'propGarageUserId' });
console.log('Garage locations:', garages.map(g => g.location));
```

### 2. Check if garage owner has FCM token
```typescript
const propGarage = await this.userModel.findOne({ role: 'propGarage' });
console.log('FCM Token:', propGarage?.fcmToken);
```

### 3. Test nearby search manually
```typescript
const test = await this.garageModel.find({
  location: {
    $near: {
      $geometry: {
        type: 'Point',
        coordinates: [10.1815, 36.8065] // Example: Tunis
      },
      $maxDistance: 50000
    }
  }
});
console.log('Nearby garages:', test.length);
```

---

## 📱 Android App - Already Implemented ✅

The Android app already:
- ✅ Sends FCM token to backend on login
- ✅ Handles push notifications
- ✅ Polls for breakdown status updates
- ✅ Shows waiting screen after SOS sent
- ✅ Navigates to tracking screen when accepted

---

## 🧪 Testing Flow

### Step 1: Create Test Garage
```bash
POST /api/garages
{
  "name": "Test Garage",
  "address": "Tunis, Tunisia",
  "latitude": 36.8065,
  "longitude": 10.1815,
  "phone": "123456789"
}
```

### Step 2: Send SOS Request
```bash
POST /api/breakdowns
{
  "type": "PNEU",
  "description": "Flat tire",
  "latitude": 36.8070,
  "longitude": 10.1820
}
```

### Step 3: Check Backend Logs
Should see:
```
📍 Found 1 nearby garages for breakdown 65xxx
✅ Notification sent to garage: Test Garage
```

### Step 4: Garage Owner Should Receive
- Push notification on Android
- In-app notification visible

---

## ⚡ Quick Fix Priority

1. **HIGH**: Add `sendNotificationsToNearbyGarages()` method
2. **HIGH**: Ensure garages have location data with 2dsphere index
3. **MEDIUM**: Verify FCM tokens are saved correctly
4. **LOW**: Add notification database logging

---

## 📍 Location Data Format

Garage location should be stored as:
```json
{
  "location": {
    "type": "Point",
    "coordinates": [10.1815, 36.8065]  // [longitude, latitude]
  }
}
```

⚠️ **Important**: MongoDB uses **[longitude, latitude]** order!

---

## 🎯 Expected Result

After fix, when user sends SOS:
1. ✅ Backend creates breakdown
2. ✅ Backend finds nearby garages
3. ✅ Backend sends FCM notification to garage owners
4. ✅ Backend saves notification in database
5. ✅ Garage owner receives push notification
6. ✅ Garage owner can accept/reject request
7. ✅ User is notified of acceptance
8. ✅ Tracking screen shows garage location

---

## 📝 Additional Notes

### Notification Payload Example
```json
{
  "token": "user_fcm_token_here",
  "notification": {
    "title": "🚨 Nouvelle demande SOS",
    "body": "Assistance PNEU demandée à proximité"
  },
  "data": {
    "type": "NEW_BREAKDOWN",
    "breakdownId": "65xxx",
    "latitude": "36.8070",
    "longitude": "10.1820",
    "breakdownType": "PNEU"
  }
}
```

### Firebase Console Check
Verify in Firebase Console:
1. Cloud Messaging enabled
2. Server key configured
3. Test notification working

---

## 🔗 Related Files

### Backend (needs update)
- `src/breakdowns/breakdowns.service.ts` - Add notification logic
- `src/garages/garage.schema.ts` - Ensure geospatial index
- `src/firebase/firebase-admin.service.ts` - Firebase integration

### Android (already working)
- ✅ `MyFirebaseMessagingService.kt` - Handles notifications
- ✅ `SOSWaitingScreen.kt` - Polls for status
- ✅ `BreakdownSOSScreen.kt` - Sends SOS with location

---

## 🚀 Action Required

**Backend developer must:**
1. Add notification sending logic after breakdown creation
2. Ensure garages have proper location data
3. Verify FCM tokens are stored for users
4. Test notification delivery
5. Add logging for debugging

**No Android changes needed** - app is ready!

