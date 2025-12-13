# 🚀 Quick Start: Manual Location Selection

## What You Can Do Now

### ✅ Your SOS Screen Already Has This Feature!

The manual location selection is **already working** in your app. Here's how to use it:

---

## 🎯 3 Simple Steps

### 1️⃣ Open SOS Screen
- Go to the SOS assistance screen
- Wait for GPS to load (or grant location permission)

### 2️⃣ Tap Anywhere on the Map
- Simply **tap on the map** where you want to select your position
- The red marker will jump to that location
- Coordinates will update automatically

### 3️⃣ Send Your Request
- Fill in the breakdown type
- Click "Envoyer la demande SOS"
- Your selected position is sent!

---

## 🎨 Visual Guide

### Before Tapping (GPS Mode)

```
┌─────────────────────────────────┐
│  📍 Position GPS actuelle       │  ← BLUE CARD
└─────────────────────────────────┘

┌─────────────────────────────────┐
│                                 │
│     [   MAP WITH YOUR GPS   ]   │
│              🔴                 │  ← Red marker at your GPS location
│                                 │
└─────────────────────────────────┘
```

**Status:**
- Blue indicator = Using GPS
- Marker title = "Votre position GPS"

---

### After Tapping (Manual Mode)

```
┌─────────────────────────────────┐
│  👆 Appuyez sur la carte pour   │  ← PURPLE CARD
│     choisir votre position      │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│                                 │
│     [   MAP WITH YOUR TAP   ]   │
│                    🔴           │  ← Red marker at tapped location
│                                 │
└─────────────────────────────────┘
```

**Status:**
- Purple indicator = Manual selection
- Marker title = "Position choisie"
- Coordinates = Updated to tapped location

---

## 🔄 How to Return to GPS

Click the **refresh button** (🔄) next to the coordinates:

```
📍 Lat: 37.4220, Lon: -122.0840  [🔄]
                                   ↑
                              Click here
```

This will:
- ✅ Fetch your current GPS position again
- ✅ Change back to blue indicator
- ✅ Update marker to GPS location

---

## 💡 When to Use Manual Selection

| Situation | Solution |
|-----------|----------|
| **GPS shows wrong location** | Tap on the correct spot on map |
| **Calling for someone else** | Tap where they are located |
| **Want to meet at landmark** | Tap on gas station, parking lot, etc. |
| **GPS not working well (indoors)** | Tap on map to choose your building |
| **Need precise position** | Zoom in and tap exactly where you are |

---

## 🎬 Example Scenario

**You're on Highway A1, but GPS shows you on a parallel street:**

1. You see the map with blue indicator
2. You zoom in on the map to see Highway A1
3. You tap directly on the highway where you are
4. Marker moves to Highway A1 ✅
5. Purple indicator appears
6. You fill in "PNEU" as breakdown type
7. You click "Envoyer la demande SOS"
8. Done! Technician gets the correct highway location 🎉

---

## ⚡ Pro Tips

### Tip 1: Zoom First
```
Use two fingers to zoom in before tapping
→ More accurate selection
→ Can see street names and landmarks
```

### Tip 2: Multiple Taps OK
```
Tap anywhere multiple times
→ Each tap updates the position
→ Last tap is the one that counts
```

### Tip 3: Use Landmarks
```
Tap on recognizable spots:
- Gas stations ⛽
- Shopping centers 🏪
- Highway exits 🛣️
- Intersections 🚦
```

### Tip 4: Refresh Anytime
```
Made a mistake?
→ Click refresh button (🔄)
→ Returns to GPS position
→ Start over
```

---

## 📱 What You See vs What Happens

### What You See:
1. Blue card → "Position GPS actuelle"
2. Tap on map → **Immediate response**
3. Purple card → "Appuyez sur la carte..."
4. Marker moves → New position
5. Coordinates update → New lat/lon

### What Happens in the Background:
1. Map detects your tap
2. Converts screen coordinates to latitude/longitude
3. Updates marker position
4. Triggers callback to update state
5. Changes `isManualLocation` to `true`
6. UI reflects the change (blue → purple)
7. Ready to send SOS with selected position!

---

## 🎯 Color Coding

| Color | Mode | Icon | Meaning |
|-------|------|------|---------|
| 🔵 Blue | GPS | 📍 GPS Fixed | Using automatic GPS position |
| 🟣 Purple | Manual | 👆 Touch | Using manually selected position |

---

## ✅ Checklist: Is It Working?

Test the feature:

- [ ] Open SOS screen
- [ ] See blue indicator "Position GPS actuelle"
- [ ] See map with red marker
- [ ] Tap somewhere else on the map
- [ ] Marker moves to tapped location? ✅
- [ ] Indicator changes to purple? ✅
- [ ] Text changes to "Appuyez sur la carte..."? ✅
- [ ] Coordinates update? ✅
- [ ] Click refresh button (🔄)
- [ ] Returns to blue indicator? ✅
- [ ] Marker returns to GPS position? ✅

**If all checkboxes are ✅, the feature is working perfectly!**

---

## 🆘 Troubleshooting

### Issue: "I tap but nothing happens"

**Solutions:**
1. Make sure you're tapping on the map area (the gray/green area with streets)
2. Don't tap on the indicator card at the top
3. Don't tap on the coordinates at the bottom
4. Tap directly on the map view itself

### Issue: "The marker doesn't move"

**Solutions:**
1. Restart the app
2. Check if the map has fully loaded (can you see streets?)
3. Try tapping a different spot
4. Make sure you're not zoomed out too far

### Issue: "I can't find the refresh button"

**Look here:**
```
📍 Lat: 37.4220, Lon: -122.0840  [🔄]
                                   ↑
                            Right here!
```

---

## 📝 Summary

**The feature is ready and working!**

Just remember:
- 👆 **Tap map** = Manual selection (purple)
- 🔄 **Click refresh** = Back to GPS (blue)
- 📍 **Current position** = Always shown in coordinates

**No configuration needed. No settings to change. Just tap and go!**

---

## 📖 Need More Info?

See the complete guide: [HOW_TO_USE_MANUAL_LOCATION.md](./HOW_TO_USE_MANUAL_LOCATION.md)

---

*Feature Status: ✅ Working*  
*Last Updated: December 5, 2025*
