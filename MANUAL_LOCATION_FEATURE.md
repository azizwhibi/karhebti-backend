# Manual Location Selection Feature

## Overview
The SOS screen now supports **manual location selection** in addition to automatic GPS positioning. Users can tap anywhere on the map to choose their exact location for the breakdown assistance request.

## Features Implemented

### 1. Interactive Map
- **Tap to Select**: Users can tap anywhere on the OpenStreetMap to manually select their position
- **Real-time Updates**: The marker moves instantly to the tapped location
- **Coordinates Update**: Latitude and longitude display updates immediately

### 2. Location Mode Indicators

#### GPS Mode (Default)
- **Blue indicator card**: Shows "Position GPS actuelle" (Current GPS position)
- **GPS icon**: Displays GPS fixed icon
- **Marker title**: "Votre position GPS"
- **Refresh button**: Updates location using GPS

#### Manual Mode
- **Purple indicator card**: Shows "Appuyez sur la carte pour choisir votre position" (Tap the map to choose your position)
- **Touch icon**: Displays touch/tap icon
- **Marker title**: "Position choisie" (Chosen position)
- **Refresh button**: Returns to GPS mode and fetches current GPS position

### 3. Seamless Mode Switching
- **Automatic to Manual**: Simply tap on the map to switch to manual mode
- **Manual to Automatic**: Click the refresh button (🔄) to return to GPS mode

## How to Use

### For Users

1. **Open SOS Screen**
   - The app will automatically request location permission
   - If granted, it will fetch your current GPS position
   - The map displays centered on your location

2. **Manual Location Selection**
   - **Tap anywhere on the map** to select a different position
   - The indicator changes from blue (GPS) to purple (Manual)
   - The marker moves to your tapped location
   - Coordinates update automatically

3. **Return to GPS Mode**
   - Click the **refresh button** (🔄) at the bottom of the map
   - The app will fetch your current GPS position again
   - The indicator returns to blue (GPS mode)

4. **Submit Request**
   - Fill in the breakdown type and optional description
   - Click "Envoyer la demande SOS" to submit
   - The selected position (GPS or manual) will be sent

### Use Cases

1. **Different Location**: You're calling for assistance for someone at a different location
2. **Poor GPS Signal**: GPS is inaccurate, so you manually correct the position
3. **Indoor Location**: GPS doesn't work well indoors, select location on map
4. **Future Location**: You know you'll be at a specific location soon
5. **Landmark Selection**: Choose a nearby landmark for easier navigation

## Technical Details

### Modified Files

1. **OpenStreetMapView.kt**
   - Added `onLocationSelected` callback parameter
   - Implemented `onSingleTapConfirmed` overlay for tap detection
   - Marker position updates on tap

2. **BreakdownSOSScreen.kt**
   - Added `isManualLocation` state to track location mode
   - Added location mode indicator UI (GPS vs Manual)
   - Pass `onLocationSelected` callback to map component
   - Refresh button resets to GPS mode

### Code Changes

#### OpenStreetMapView Component
```kotlin
@Composable
fun OpenStreetMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoom: Double = 15.0,
    markerTitle: String = "Votre position",
    onLocationSelected: ((Double, Double) -> Unit)? = null  // NEW parameter
)
```

#### Map Tap Handler
```kotlin
overlays.add(object : org.osmdroid.views.overlay.Overlay() {
    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val projection = mapView.projection
        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
        
        // Update marker position
        marker?.position = geoPoint
        mapView.invalidate()
        
        // Notify callback with new coordinates
        callback(geoPoint.latitude, geoPoint.longitude)
        
        return true
    }
})
```

#### Location State Management
```kotlin
// In BreakdownSOSScreen
var isManualLocation by remember { mutableStateOf(false) }

// On map tap
onLocationSelected = { lat, lon ->
    latitude = lat
    longitude = lon
    isManualLocation = true
}

// On refresh
onRefreshLocation = {
    isManualLocation = false
    fetchLocation(...)  // Fetch GPS position
}
```

### UI Components

1. **Mode Indicator Card**
   - Blue background for GPS mode
   - Purple background for Manual mode
   - Clear text explaining current mode
   - Appropriate icons (GPS/Touch)

2. **Map Card**
   - 300dp height
   - Rounded corners
   - Full-width responsive
   - Touch-enabled for selection

3. **Coordinates Display**
   - Shows current lat/lon with 4 decimal precision
   - Location pin icon
   - Refresh button for GPS update

## Benefits

1. **Flexibility**: Users can choose between automatic GPS and manual selection
2. **Accuracy**: Can correct inaccurate GPS readings
3. **Convenience**: Easy to select locations for others
4. **User-Friendly**: Clear visual indicators of location mode
5. **No Extra Steps**: Just tap the map, no menus or buttons needed

## Testing

### Test Scenarios

1. **Default GPS Mode**
   - ✅ App opens with GPS position
   - ✅ Blue indicator shows "GPS actuelle"
   - ✅ Marker shows "Votre position GPS"

2. **Manual Selection**
   - ✅ Tap anywhere on map
   - ✅ Marker moves to tapped location
   - ✅ Purple indicator shows "Appuyez sur la carte"
   - ✅ Coordinates update immediately

3. **Mode Switching**
   - ✅ Tap map → switches to manual mode
   - ✅ Click refresh → returns to GPS mode
   - ✅ Multiple taps → each updates position

4. **SOS Submission**
   - ✅ Manual position is sent with request
   - ✅ GPS position is sent with request
   - ✅ Confirmation shows correct coordinates

### Emulator Testing

In Android Emulator:
1. Use Extended Controls → Location
2. Set initial GPS position
3. Open SOS screen (will use set position)
4. Tap on map to manually select different position
5. Click refresh to return to emulator GPS position

### Real Device Testing

On physical device:
1. Enable GPS/Location services
2. Open SOS screen outdoors for good GPS signal
3. Wait for GPS fix (blue indicator)
4. Tap map to manually adjust position (purple indicator)
5. Click refresh to return to GPS position

## Known Limitations

1. **No Search**: Cannot search for addresses (future enhancement)
2. **No Route**: No routing to selected location (future enhancement)
3. **Single Marker**: Only one position marker (by design)

## Future Enhancements

Possible improvements for future versions:

1. **Address Search**: Search for locations by address or name
2. **Recent Locations**: Quick access to recently used positions
3. **Favorite Locations**: Save frequently used locations
4. **Share Location**: Share selected position via SMS/WhatsApp
5. **Nearby Landmarks**: Show nearby gas stations, repair shops, etc.
6. **Distance Indicator**: Show distance from current GPS to selected position
7. **Confirmation Dialog**: Show map preview in confirmation dialog
8. **Undo Selection**: Quick button to undo last manual selection

## Latest Updates (December 5, 2025)

### GPS Detection Improvements

#### Multiple Location Strategies
The app now uses **3 simultaneous strategies** to get your location:

1. **Last Known Location** - Fastest, uses cached position if recent (< 2 minutes)
2. **Location Updates** - Requests fresh GPS updates with balanced power accuracy
3. **Current Location** - Modern API for devices with latest Google Play Services

#### Smart Timeout (15 seconds)
- App waits up to 15 seconds for fresh GPS fix
- Falls back to last known location if timeout occurs
- Prevents infinite loading states

#### Better Emulator Support
- Works with `PRIORITY_BALANCED_POWER_ACCURACY` (better for emulators)
- Automatic fallback to last known location
- Clear error messages with instructions for emulator users

#### Improved Error Messages
- **Emulator users**: Get instructions to use Extended Controls > Location
- **Real device users**: Guidance to go outside and enable GPS
- **Timeout**: Uses last known location automatically

### Location Detection Flow

```
1. Check Permissions → ✅ Granted
2. Check GPS Enabled → ✅ Active
3. Start 3 Parallel Strategies:
   
   Strategy 1: Get Last Known Location
   ├─ If recent (< 2 min) → Use immediately ✅
   └─ If old → Keep as fallback
   
   Strategy 2: Request Location Updates
   ├─ Wait for fresh GPS fix
   └─ Use first accurate result ✅
   
   Strategy 3: Get Current Location
   ├─ Modern API call
   └─ Use if available ✅
   
4. Timeout Handler (15s):
   └─ If no fresh location → Use last known ✅

5. Success! → Show map with position
```

### How to Set Location in Emulator

**Quick Method:**
1. Open emulator
2. Click **⋮** (3 dots) → **Location**
3. Search **"Tunis"** or enter coordinates:
   - Latitude: `36.8065`
   - Longitude: `10.1815`
4. Click **"SET LOCATION"**

**Command Line Method:**
```bash
adb emu geo fix 10.1815 36.8065
```
⚠️ Note: Longitude before Latitude!

**See full guide:** [EMULATOR_LOCATION_QUICK_GUIDE.md](./EMULATOR_LOCATION_QUICK_GUIDE.md)

## Build Status

✅ **Build Successful** (December 5, 2025)  
✅ **All Compilation Errors Resolved**  
✅ **Manual Location Feature Implemented**  
✅ **GPS Detection Improved with Multiple Strategies**  
✅ **Better Emulator Support Added**  
✅ **Smart Timeout & Fallback Implemented**  
✅ **UI/UX Enhancements Applied**

## Summary

The manual location selection feature empowers users to take control of their SOS request location, whether they need to correct GPS inaccuracies, select a location for someone else, or choose a more precise position. The intuitive tap-to-select interface combined with clear visual indicators makes the feature easy to discover and use.

