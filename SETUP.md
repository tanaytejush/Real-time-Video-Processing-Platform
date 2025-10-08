# Flam Setup Guide

Complete setup instructions for building the Flam Android project with OpenCV.

## Prerequisites

### 1. Development Environment

- **Android Studio**: Arctic Fox or later (recommended: Hedgehog 2023.1.1+)
- **JDK**: Java 17 (included with Android Studio)
- **Android SDK**: API 34 (Android 14)
- **Android NDK**: Version 25.2.9519653
- **CMake**: Version 3.22.1

### 2. Install NDK and CMake

In Android Studio:
1. Go to `Tools > SDK Manager`
2. Select `SDK Tools` tab
3. Check:
   - ✅ NDK (Side by side) - Version 25.2.9519653
   - ✅ CMake - Version 3.22.1
4. Click `Apply` to install

## OpenCV Android SDK Setup

### Download OpenCV

1. Visit [OpenCV Releases](https://opencv.org/releases/)
2. Download **OpenCV Android SDK 4.8.0** (or later)
   - File: `opencv-4.8.0-android-sdk.zip`
3. Extract to a permanent location:
   ```bash
   unzip opencv-4.8.0-android-sdk.zip
   mv opencv-android-sdk ~/opencv-android-sdk
   ```

### Configure OpenCV Path

**Option 1: Environment Variable (Recommended)**

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, or `~/.bash_profile`):

```bash
export OPENCV_ANDROID_SDK=~/opencv-android-sdk
```

Reload your shell:
```bash
source ~/.bashrc  # or ~/.zshrc
```

**Option 2: Gradle Properties**

Edit `gradle.properties` and set:
```properties
OPENCV_ANDROID_SDK=/path/to/opencv-android-sdk
```

**Option 3: Local AAR (Alternative)**

1. Copy `opencv-release.aar` from SDK to `libs/` directory:
   ```bash
   cp ~/opencv-android-sdk/sdk/native/libs/opencv-release.aar libs/
   ```

2. In `app/build.gradle`, uncomment:
   ```gradle
   implementation(name: 'opencv-release', ext: 'aar')
   ```

## Build Configuration

### Project Structure

```
Flam/
├── build.gradle              # Project-level build config
├── settings.gradle           # Project settings
├── gradle.properties         # Gradle properties
├── app/
│   ├── build.gradle          # App-level build config
│   └── proguard-rules.pro    # ProGuard rules
├── CMakeLists.txt            # CMake configuration
├── jni/                      # C++ JNI source files
├── gl/                       # OpenGL C++ source files
└── web/                      # TypeScript web viewer
```

### Gradle Configuration

**Project-level `build.gradle`:**
- Gradle version: 8.2.0
- Kotlin version: 1.9.20
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- NDK version: 25.2.9519653
- CMake version: 3.22.1

**App-level `build.gradle`:**
- OpenCV SDK dependency configuration
- NDK ABI filters: arm64-v8a, armeabi-v7a
- CMake external build setup
- C++17 standard with RTTI and exceptions

### CMakeLists.txt Configuration

The CMakeLists.txt file:
- Detects OpenCV SDK from `OPENCV_ANDROID_SDK` environment variable
- Links architecture-specific OpenCV libraries
- Configures C++ compiler flags for optimization
- Links OpenGL ES 2.0 libraries

## Building the Project

### Using Android Studio

1. **Open Project**
   ```
   File > Open > Select "Flam" directory
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync
   - Check for errors in "Build" tab

3. **Build Project**
   ```
   Build > Make Project
   ```

4. **Run on Device**
   - Connect Android device (API 24+)
   - Enable USB debugging
   - Click ▶️ Run button

### Using Command Line

```bash
# Navigate to project directory
cd "/Users/tejushtanay/Projects /Flam"

# Check OpenCV SDK
echo $OPENCV_ANDROID_SDK

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

## Troubleshooting

### OpenCV SDK Not Found

**Error:**
```
OPENCV_ANDROID_SDK environment variable not set
```

**Solution:**
- Set the environment variable (see setup instructions above)
- Or specify path in `gradle.properties`
- Verify path is correct: `ls $OPENCV_ANDROID_SDK`

### CMake Configuration Failed

**Error:**
```
OpenCV not found at /path/to/opencv
```

**Solution:**
1. Check OpenCV SDK directory structure:
   ```bash
   ls $OPENCV_ANDROID_SDK/sdk/native/jni/
   # Should show: abi-arm64-v8a, abi-armeabi-v7a
   ```

2. Verify CMake version:
   ```bash
   cmake --version  # Should be 3.22.1
   ```

### NDK Not Found

**Error:**
```
NDK not configured
```

**Solution:**
- Install NDK via Android Studio SDK Manager
- Verify NDK path: `echo $ANDROID_NDK_HOME`
- Check `local.properties` has correct NDK path

### Build ABI Mismatch

**Error:**
```
No native library found for target architecture
```

**Solution:**
- Clean and rebuild: `./gradlew clean build`
- Check ABI filters in `app/build.gradle`:
  ```gradle
  ndk {
      abiFilters 'arm64-v8a', 'armeabi-v7a'
  }
  ```

### Gradle Sync Failed

**Error:**
```
Could not resolve dependencies
```

**Solution:**
1. Check internet connection
2. Clear Gradle cache:
   ```bash
   rm -rf ~/.gradle/caches/
   ./gradlew --refresh-dependencies
   ```

## Verification

After successful build, verify:

1. **APK generated:**
   ```bash
   ls app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Native libraries included:**
   ```bash
   unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libflam.so
   # Should show: lib/arm64-v8a/libflam.so, lib/armeabi-v7a/libflam.so
   ```

3. **OpenCV libraries included:**
   ```bash
   unzip -l app/build/outputs/apk/debug/app-debug.apk | grep opencv
   ```

## Build Variants

### Debug Build
- Includes debug symbols
- JNI debugging enabled
- No code obfuscation
- Larger APK size

```bash
./gradlew assembleDebug
```

### Release Build
- Optimized code
- ProGuard enabled
- Stripped symbols
- Smaller APK size

```bash
./gradlew assembleRelease
```

## Next Steps

1. **Run the app** on a physical device (API 24+)
2. **Grant camera permission** when prompted
3. **Test edge detection** toggle in UI
4. **Check Logcat** for performance metrics
5. **Build web viewer** (see `web/README.md`)

## Additional Resources

- [Android NDK Guide](https://developer.android.com/ndk/guides)
- [CMake Documentation](https://cmake.org/documentation/)
- [OpenCV Android Tutorial](https://docs.opencv.org/4.x/d5/df8/tutorial_dev_with_OCV_on_Android.html)
- [Flam README](README.md)
