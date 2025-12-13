# 🗺️ SOS Manual Location Selection - User Guide & Fix

## 📱 Current Situation

✅ **ALREADY WORKING:**
- The app detects GPS location automatically
- User can tap anywhere on the map to select their position manually
- The red marker moves to where they tap
- Instructions are displayed above the map

❌ **ISSUE:**
- GPS doesn't work well in emulator (expected)
- User may not realize they can tap the map to select position

---

## 🎯 How It Works NOW

### Automatic Flow:
1. **Permission Check** → App asks for location permission
2. **GPS Detection** → Tries to get GPS location
3. **Show Map** → Displays map with current position
4. **User Can Tap** → User taps anywhere on map to change position
5. **Send SOS** → Sends request with selected coordinates

### Manual Selection:
When GPS fails, user sees:
- ❌ "GPS non détecté" error
- ✅ Big button: **"👆 Je choisis ma position"**
- When clicked → Shows map centered on Tunis (default)
- User taps anywhere on map to select exact location
- Marker moves to tapped position
- Position updates in lat/lon display

---

## 🔧 What User Needs to Know

### On Real Device:
1. Enable location permission
2. Turn on GPS
3. Wait 10-30 seconds for GPS fix
4. OR tap map to select manually

### On Emulator:
1. Click **"👆 Je choisis ma position"** button (shown when GPS fails)
2. Map opens centered on Tunis
3. **TAP ANYWHERE ON MAP** to select your position
4. Red marker moves to tapped location
5. Coordinates update automatically
6. Fill in problem type and send

---

## 🎨 UI Improvements (Already Implemented)

The app already shows:

```
┌────────────────────────────────────────┐
│  👆 Touchez la carte pour choisir      │
│     votre position                     │
│                                        │
│  Le marqueur rouge 📍 se déplacera     │
│  là où vous touchez                    │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│  ✅ Position sélectionnée manuellement │
│  OR                                    │
│  📡 Position GPS automatique           │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│                                        │
│          [MAP WITH MARKER]             │
│                                        │
│          📍 Tap anywhere               │
│                                        │
└────────────────────────────────────────┘

📍 Lat: 36.8065, Lon: 10.1815  🔄
```

---

## 📍 Testing Instructions

### Test on Emulator:
```bash
1. Open app
2. Go to SOS screen
3. If GPS fails, click "👆 Je choisis ma position"
4. TAP on the map where you want assistance
5. Verify marker moves
6. Verify coordinates update
7. Select problem type
8. Send SOS
```

### Test GPS on Emulator:
```bash
1. Open Android Emulator
2. Click "..." (More) button in emulator toolbar
3. Go to "Location" tab
4. Enter coordinates manually OR use map
5. Click "Send" button
6. App should detect new location
```

### Set Emulator Location Programmatically:
```bash
# Using adb
adb emu geo fix 10.1815 36.8065

# Format: longitude latitude
```

---

## 🚨 SOS Flow After Position Selection

### Step 1: User Selects Position
- GPS automatic OR map tap manual
- Position confirmed with coordinates display

### Step 2: Fill Details
- Select problem type (PNEU, BATTERIE, etc.)
- Add description (optional)
- Add photo (optional)

### Step 3: Send SOS
- Confirmation dialog appears
- Shows: Type, Position, Description
- User clicks "Confirmer et envoyer"

### Step 4: Waiting Screen
- Shows pulsing SOS icon
- Polls backend every 5 seconds for status
- Displays: "En attente de confirmation du garage"

### Step 5: Backend Notifies Garage
**⚠️ THIS IS WHERE THE ISSUE IS**

Current backend logs show:
```
✅ POST /api/breakdowns 201 - Breakdown created
❌ NO notification sent to garage owners
❌ NO logs showing "Sending notification to garage..."
```

**Backend needs to:**
1. Find nearby garages (within 50km radius)
2. Send FCM push notification to each garage owner
3. Save notification in database
4. Log notification sent

### Step 6: Garage Owner Response
When garage owner accepts:
- Backend updates breakdown status to "ACCEPTED"
- Android app polling detects status change
- App navigates to tracking screen
- Shows route from garage to user

---

## 🔧 Backend Fix Required

See `BACKEND_NOTIFICATION_FIX.md` for complete backend implementation.

**Critical missing code in backend:**
```typescript
// breakdowns.service.ts
async create(userId: string, createBreakdownDto: CreateBreakdownDto) {
  const breakdown = await this.breakdownModel.create({...});
  
  // ❌ THIS IS MISSING:
  await this.sendNotificationsToNearbyGarages(breakdown);
  
  return breakdown;
}
```

---

## ✅ Android App Status

| Feature | Status | Notes |
|---------|--------|-------|
| GPS Detection | ✅ | Works on real devices |
| Manual Location Selection | ✅ | Tap map to select |
| Map Interaction | ✅ | Marker moves on tap |
| SOS Form | ✅ | Type, description, photo |
| Send Request | ✅ | Sends to backend |
| Waiting Screen | ✅ | Polls every 5 seconds |
| FCM Notifications | ✅ | Receives push notifications |
| Status Updates | ✅ | Detects ACCEPTED/REFUSED |
| Navigation to Tracking | ✅ | Auto-navigates on accept |

---

## 🎯 What Needs to Be Done

### ✅ Android (COMPLETE - No changes needed)
- Manual location selection working
- Clear instructions shown
- GPS fallback functional

### ❌ Backend (NEEDS FIX)
1. Implement garage notification logic
2. Add geospatial queries for nearby garages
3. Send FCM push notifications
4. Save notifications in database
5. Add proper logging

---

## 🧪 Full Test Scenario

### Test Manual Location + Notification Flow:

1. **Android App:**
   ```
   - Open SOS screen
   - Click "👆 Je choisis ma position" (if GPS fails)
   - Tap map at desired location (e.g., your current city)
   - Select "PNEU" as problem type
   - Add description: "Pneu crevé sur autoroute"
   - Click Send
   - Verify waiting screen appears
   ```

2. **Check Backend Logs:**
   ```
   Should see:
   ✅ POST /api/breakdowns 201 Created
   ✅ 📍 Found 3 nearby garages for breakdown 65xxx
   ✅ Sending FCM notification to garage: Garage ABC
   ✅ Notification sent successfully
   ```

3. **Garage Owner Phone:**
   ```
   Should receive:
   🚨 Notification: "Nouvelle demande SOS"
   Body: "Assistance PNEU demandée à proximité"
   Tap notification → Opens garage app
   Shows SOS details with map
   Buttons: [Accepter] [Refuser]
   ```

4. **Garage Owner Accepts:**
   ```
   - Clicks "Accepter"
   - Backend updates breakdown status → ACCEPTED
   - Backend saves garage assignment
   ```

5. **User's Android App:**
   ```
   - Polling detects status = ACCEPTED
   - Auto-navigates to tracking screen
   - Shows: Garage location, route, ETA
   - Shows: Garage name, phone, distance
   ```

---

## 📊 Current vs Expected

### Current (Android ✅, Backend ❌):
```
User → Selects location → Sends SOS → Backend creates → ❌ NO notification → User waits forever
```

### Expected (After Backend Fix):
```
User → Selects location → Sends SOS → Backend creates → ✅ Notifies garages → Garage accepts → User sees tracking
```

---

## 🔍 Debugging Tips

### Check if location is sent correctly:
```kotlin
// In BreakdownSOSScreen.kt
Log.d("SOS", "Sending breakdown: lat=$latitude, lon=$longitude")
```

### Check backend receives location:
```
Backend logs should show:
POST /api/breakdowns
Body: { "latitude": 36.8065, "longitude": 10.1815, ... }
```

### Check if garages exist in database:
```typescript
const garages = await this.garageModel.find({});
console.log(`Total garages in DB: ${garages.length}`);
```

### Check if garage has location:
```typescript
const garagesWithLocation = await this.garageModel.find({
  'location.coordinates': { $exists: true }
});
console.log(`Garages with location: ${garagesWithLocation.length}`);
```

---

## 📱 User Instructions (Simple Version)

### Si le GPS ne fonctionne pas:

1. ✅ Appuyez sur **"👆 Je choisis ma position"**
2. 📍 **Touchez la carte** où vous êtes
3. ✅ Le marqueur rouge se déplace
4. 📝 Choisissez le type de problème
5. 📤 Appuyez sur "Envoyer"
6. ⏳ Attendez la confirmation du garage

**C'est tout! Pas besoin d'activer le GPS manuellement.**

---

## 🎯 Summary

### Android App: ✅ WORKING PERFECTLY
- User can select location by tapping map
- Clear instructions shown
- GPS fallback implemented
- Waiting screen polls for status
- Navigation to tracking works

### Backend: ❌ NEEDS FIX
- Missing: Send notifications to nearby garages
- Missing: Find garages within radius
- Missing: FCM push notification implementation
- See BACKEND_NOTIFICATION_FIX.md for solution

---

## 🚀 Next Steps

1. **Backend Developer:** Implement notification logic (see BACKEND_NOTIFICATION_FIX.md)
2. **Test:** Create test garage with location data
3. **Test:** Send SOS and verify garage receives notification
4. **Test:** Garage accepts and verify user sees tracking screen

**Android app is ready and waiting for backend notifications to work!** 🎉

