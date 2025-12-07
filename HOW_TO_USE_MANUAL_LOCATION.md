# How to Use Manual Location Selection in SOS Screen

## 📍 Overview

Your SOS assistance screen now has **manual location selection** functionality! You can choose your exact position by tapping anywhere on the map, in addition to the automatic GPS positioning.

## ✅ Feature Status

**✓ FULLY IMPLEMENTED AND WORKING**

The feature is already coded and ready to use. No additional changes are needed!

---

## 🎯 How to Use It

### Step 1: Open SOS Screen
1. Launch the app
2. Navigate to the SOS assistance screen
3. The app will request location permission if not already granted
4. Once permission is granted, your current GPS position will be displayed on the map

### Step 2: View Your Current Position
- You'll see a **blue indicator card** at the top saying "Position GPS actuelle"
- A **red marker** shows your current GPS location on the map
- The coordinates are displayed below the map

### Step 3: Manually Select a Different Position
**Simply tap anywhere on the map!**

When you tap on the map:
- ✅ The marker **immediately moves** to where you tapped
- ✅ The indicator card changes from **blue to purple**
- ✅ The text changes to "Appuyez sur la carte pour choisir votre position"
- ✅ The marker title changes from "Votre position GPS" to "Position choisie"
- ✅ The coordinates **update automatically**

### Step 4: Return to GPS Mode (Optional)
If you want to go back to your real GPS position:
- Click the **refresh button** (🔄) next to the coordinates
- The app will fetch your current GPS position again
- The indicator returns to blue "Position GPS actuelle"

### Step 5: Submit Your Request
- Fill in the breakdown type (required)
- Optionally add a description
- Click "Envoyer la demande SOS"
- The selected position (whether GPS or manually chosen) will be sent

---

## 🎨 Visual Indicators

### GPS Mode (Default)
```
┌────────────────────────────────────┐
│ 📍 Position GPS actuelle           │  ← Blue background
└────────────────────────────────────┘
```
- **Icon:** GPS Fixed (📍)
- **Marker Title:** "Votre position GPS"
- **Color:** Blue

### Manual Mode (After Tapping Map)
```
┌────────────────────────────────────┐
│ 👆 Appuyez sur la carte pour       │  ← Purple background
│    choisir votre position          │
└────────────────────────────────────┘
```
- **Icon:** Touch/Tap (👆)
- **Marker Title:** "Position choisie"
- **Color:** Purple

---

## 💡 Use Cases

### 1. **Someone Else Needs Help**
You're calling assistance for a friend or family member who is at a different location.
- Open SOS screen
- Tap on the map where they are located
- Send the request

### 2. **Poor GPS Signal**
Your GPS is showing an inaccurate position (e.g., wrong side of the road, off by several meters).
- Wait for initial GPS position
- Tap on the map to correct the position
- Send the accurate request

### 3. **Indoor Location**
GPS doesn't work well inside buildings or parking structures.
- Manually select your location on the map
- Choose the correct building or entrance
- Send the request

### 4. **Future Location**
You know you'll arrive at a specific location soon.
- Tap on the map where you'll be
- Continue to that location
- The assistance will meet you there

### 5. **Landmark Selection**
You want to meet at a nearby landmark for easier navigation.
- Tap on a gas station, parking lot, or other landmark
- This makes it easier for the technician to find you
- Send the request

---

## 🔧 Technical Details

### How It Works

1. **Map Component**: Uses OpenStreetMap (osmdroid library)
2. **Tap Detection**: Overlay on the map detects single tap events
3. **Marker Update**: Marker position updates immediately on tap
4. **State Management**: `isManualLocation` boolean tracks current mode
5. **Coordinates Update**: Latitude/longitude variables update in real-time

### Code Flow

```
User taps on map
    ↓
onSingleTapConfirmed triggered
    ↓
Convert pixel coordinates to lat/lon
    ↓
Update marker position on map
    ↓
Call onLocationSelected(lat, lon)
    ↓
Update latitude & longitude variables
    ↓
Set isManualLocation = true
    ↓
UI updates (blue → purple indicator)
```

### Files Modified

1. **OpenStreetMapView.kt**
   - Added `onLocationSelected` callback parameter
   - Implemented tap overlay with `onSingleTapConfirmed`
   - Updates marker position on tap

2. **BreakdownSOSScreen.kt**
   - Added `isManualLocation` state variable
   - Created location mode indicator UI
   - Wired up `onLocationSelected` callback
   - Refresh button resets to GPS mode

---

## 🧪 Testing Guide

### Test on Android Emulator

1. **Start Emulator**
   ```
   Open Android Studio → Device Manager → Start Emulator
   ```

2. **Set GPS Location**
   - Open Extended Controls (⋮ button)
   - Go to Location tab
   - Enter coordinates (e.g., 37.4220, -122.0840)
   - Click "Send"

3. **Launch App**
   - Install and run the app
   - Navigate to SOS screen
   - Should show the emulator's set GPS position

4. **Test Manual Selection**
   - Tap anywhere on the map
   - Observe:
     - ✅ Marker moves
     - ✅ Blue → Purple indicator
     - ✅ Coordinates update
     - ✅ "Position choisie" title

5. **Test GPS Return**
   - Click refresh button (🔄)
   - Should return to emulator GPS position
   - Purple → Blue indicator

### Test on Real Device

1. **Enable GPS**
   - Settings → Location → On
   - Allow high accuracy mode

2. **Grant Permission**
   - App will request location permission
   - Grant "While using the app"

3. **Go Outdoors** (for best GPS signal)
   - Wait 10-30 seconds for GPS fix
   - Should see blue indicator

4. **Test Manual Selection**
   - Tap a nearby street or location
   - Marker should move immediately
   - Indicator changes to purple

5. **Test GPS Refresh**
   - Click refresh button
   - Should fetch current GPS again
   - Returns to blue indicator

---

## 📱 User Interface

### Complete Screen Layout

```
┌─────────────────────────────────────┐
│  ← SOS - Assistance routière     ⏱ │
├─────────────────────────────────────┤
│                                     │
│          ┌─────────┐                │
│          │   ⚠️    │                │  SOS Button
│          │         │                │
│          └─────────┘                │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ 📍 Position GPS actuelle      │ │  Mode Indicator
│  └───────────────────────────────┘ │
│                                     │
│  ┌───────────────────────────────┐ │
│  │                               │ │
│  │         [MAP VIEW]            │ │  OpenStreetMap
│  │           🔴                  │ │  (Tap to select)
│  │                               │ │
│  └───────────────────────────────┘ │
│                                     │
│  📍 Lat: 37.4220, Lon: -122.0840   │  Coordinates
│                                  🔄 │  Refresh button
│                                     │
│  ┌───────────────────────────────┐ │
│  │ Type de problème *            │ │  Dropdown
│  └───────────────────────────────┘ │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ Description (optionnel)       │ │  Text field
│  └───────────────────────────────┘ │
│                                     │
│  [📷 Ajouter une photo]            │  Photo button
│                                     │
│  [✉️ Envoyer la demande SOS]       │  Submit button
│                                     │
└─────────────────────────────────────┘
```

---

## 🎬 Step-by-Step Visual Guide

### Scenario: Correcting GPS Position

**1. Initial GPS Position (Blue)**
```
The app shows your GPS position, but it's on the wrong side of the street.
→ Blue indicator shows "Position GPS actuelle"
→ Marker shows "Votre position GPS"
```

**2. Tap on Correct Location**
```
You tap on the correct position on the other side of the street.
→ Marker immediately jumps to tapped location
→ Indicator changes to purple
```

**3. Manual Position Selected (Purple)**
```
The selected position is now active.
→ Purple indicator shows "Appuyez sur la carte..."
→ Marker shows "Position choisie"
→ Coordinates updated
```

**4. Submit Request**
```
Fill in breakdown type and send.
→ The manually selected position is sent to the server
→ Technician will receive accurate location
```

---

## ⚡ Quick Reference

| Action | Result |
|--------|--------|
| **Tap anywhere on map** | Selects that position (blue → purple) |
| **Click refresh button (🔄)** | Returns to GPS position (purple → blue) |
| **Submit SOS request** | Sends currently selected position (GPS or manual) |
| **Tap multiple times** | Each tap updates to new position |

---

## 🚀 Benefits

1. **Flexibility**: Choose between automatic GPS and manual selection
2. **Accuracy**: Correct inaccurate GPS readings easily
3. **Convenience**: Select locations for others without being there
4. **User-Friendly**: Intuitive tap-to-select, no complex menus
5. **Visual Feedback**: Clear color-coded indicators (blue/purple)
6. **Real-Time Updates**: Immediate coordinate updates on selection
7. **Easy Reset**: One-tap return to GPS mode

---

## 🎓 Best Practices

### For Best Results:

1. **Wait for GPS first** (if possible)
   - Let the app get your GPS position initially
   - Then adjust if needed

2. **Zoom in for precision**
   - Use pinch gesture to zoom in on the map
   - Tap more precisely on your exact location

3. **Use landmarks**
   - Select recognizable points (gas stations, intersections)
   - Easier for technicians to find you

4. **Verify coordinates**
   - Check that the displayed lat/lon looks reasonable
   - Coordinates should match the visible map area

5. **Test before emergency**
   - Try the feature beforehand to familiarize yourself
   - Know how to use it when you really need it

---

## ❓ FAQ

### Q: Do I need to tap the map every time?
**A:** No! By default, the app uses your GPS position automatically. Only tap the map if you want to manually select a different position.

### Q: Can I tap multiple times?
**A:** Yes! Each tap updates to the new position. The most recent tap is used.

### Q: How do I go back to GPS mode?
**A:** Click the refresh button (🔄) next to the coordinates display.

### Q: Will this work without GPS enabled?
**A:** The app requires GPS permission to launch, but once you're on the map screen, you can manually select any position even if GPS signal is weak or unavailable.

### Q: Is the manual position saved?
**A:** The position is used for the current SOS request only. Next time you open the SOS screen, it will use GPS again by default.

### Q: Can I search for addresses?
**A:** Not yet - this is a planned future enhancement. Currently, you can only tap on the visible map area.

### Q: What if I accidentally tap the map?
**A:** No problem! Just click the refresh button to return to GPS mode, or tap again on the correct position.

---

## 🔮 Future Enhancements

Planned improvements for future versions:

- 🔍 **Address Search**: Search for locations by address or name
- 📍 **Recent Locations**: Quick access to recently used positions
- ⭐ **Favorite Locations**: Save frequently used positions (home, work, etc.)
- 📤 **Share Location**: Share selected position via SMS/WhatsApp
- 🏪 **Nearby POIs**: Show nearby gas stations, repair shops, hospitals
- 📏 **Distance Indicator**: Show distance from GPS to selected position
- ↩️ **Undo Selection**: Quick undo button for last manual selection
- 🗺️ **Map Preview**: Show map in confirmation dialog

---

## 🎉 Summary

**The manual location selection feature is fully functional and ready to use!**

Simply:
1. Open the SOS screen
2. Tap anywhere on the map to select your position
3. Fill in the breakdown details
4. Send your request

The feature makes it easy to choose your exact location, whether you need to correct GPS inaccuracies, select a location for someone else, or pick a more convenient meeting point.

**No additional setup required - just tap and go!** 🚗💨

---

*Last Updated: December 5, 2025*  
*Feature Status: ✅ Production Ready*
