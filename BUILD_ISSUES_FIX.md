# Build Issues - Quick Fix Guide

## ✅ Issue: "The process cannot access the file because it is being used by another process"

### Problem
The Gradle build process or Android Studio is holding onto build artifact files (like R.jar), preventing the build from continuing.

### Solution Applied ✅

1. **Stop Gradle Daemon**
   ```bash
   .\gradlew.bat --stop
   ```

2. **Kill Java Processes (if needed)**
   ```powershell
   Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
   ```

3. **Clean Build Directory**
   ```bash
   .\gradlew.bat clean
   ```

4. **Rebuild**
   ```bash
   .\gradlew.bat assembleDebug
   ```

---

## 🛠️ Common Build Commands

### Clean Build
```bash
# Stop all Gradle processes
.\gradlew.bat --stop

# Clean build artifacts
.\gradlew.bat clean

# Build fresh
.\gradlew.bat assembleDebug
```

### Build and Install
```bash
# Build and install to connected device/emulator
.\gradlew.bat installDebug
```

### Run Tests
```bash
# Run unit tests
.\gradlew.bat test

# Run instrumented tests
.\gradlew.bat connectedAndroidTest
```

---

## ⚠️ Warnings You Can Ignore

### 1. Path Check Warning (Expected)
```
WARNING: The option setting 'android.overridePathCheck=true' is experimental.
```
**Status:** ✅ Safe to ignore - This is needed for your non-ASCII path

### 2. Deprecation Warnings
```
Deprecated Gradle features were used in this build
```
**Status:** ✅ Safe to ignore - Your project will work fine

---

## 🔧 If Build Still Fails

### Option 1: Close Android Studio and Rebuild
1. Close Android Studio completely
2. Run in terminal:
   ```bash
   .\gradlew.bat --stop
   .\gradlew.bat clean
   .\gradlew.bat assembleDebug
   ```

### Option 2: Delete Build Folders Manually
```bash
# Delete build folders (PowerShell)
Remove-Item -Recurse -Force .\build, .\app\build -ErrorAction SilentlyContinue
.\gradlew.bat assembleDebug
```

### Option 3: Invalidate Caches (Android Studio)
1. Open Android Studio
2. File → Invalidate Caches / Restart
3. Choose "Invalidate and Restart"

### Option 4: Check for File Locks
```powershell
# Find what process is locking a file
# Install handle.exe from Sysinternals if needed
handle.exe R.jar
```

---

## 📊 Build Status Checklist

- [x] Gradle daemon stopped
- [x] Build directory cleaned
- [x] Build started
- [ ] Build completed successfully ← Waiting...

---

## 🎯 Expected Output

When build succeeds, you should see:
```
BUILD SUCCESSFUL in XXs
XX actionable tasks: XX executed
```

The APK will be located at:
```
app\build\outputs\apk\debug\app-debug.apk
```

---

## 💡 Pro Tips

1. **Always stop Gradle daemon before cleaning:**
   ```bash
   .\gradlew.bat --stop && .\gradlew.bat clean
   ```

2. **Build from terminal instead of Android Studio** if you keep getting locks

3. **Close Android Studio when running terminal builds** to avoid conflicts

4. **Use single build command:**
   ```bash
   .\gradlew.bat clean assembleDebug
   ```

---

## 🚨 If All Else Fails

1. Restart your computer (releases all file locks)
2. Run:
   ```bash
   .\gradlew.bat clean build
   ```

---

**Last Updated:** December 4, 2025
**Status:** Build in progress...

