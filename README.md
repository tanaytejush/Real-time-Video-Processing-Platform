# 🔥 Flam

**Real-time Video Processing Android Application**

High-performance mobile computer vision platform combining Camera2 API, OpenCV C++, and OpenGL ES 2.0 for real-time edge detection and frame processing at 15+ FPS.

[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com/about/versions/nougat)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.8.0-blue.svg)](https://opencv.org/)
[![OpenGL ES](https://img.shields.io/badge/OpenGL%20ES-2.0-orange.svg)](https://www.khronos.org/opengles/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-purple.svg)](https://kotlinlang.org/)

## ✨ Features

- 📹 **Real-time Camera Processing**: 1280x720 @ 30 FPS camera capture
- 🎨 **Canny Edge Detection**: Hardware-accelerated OpenCV processing
- ⚡ **OpenGL Rendering**: 60 FPS texture rendering with GLES 2.0
- 📊 **Performance Monitoring**: Live FPS counter with detailed timing metrics
- 💾 **Frame Export**: Save processed frames as PNG with metadata to Downloads
- 🌐 **Web Viewer**: TypeScript web interface for viewing saved frames
- 🎯 **Adjustable Parameters**: Real-time threshold adjustment for edge detection

## 🏗️ Architecture

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         FLAM ANDROID APP                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────┐      ┌──────────────────┐                  │
│  │  MainActivity  │◄─────┤  CameraManager   │                  │
│  │   (Kotlin)     │      │   (Camera2 API)  │                  │
│  └────────┬───────┘      └────────┬─────────┘                  │
│           │                       │                             │
│           │ ┌─────────────────────▼──────────────┐             │
│           │ │   YUV_420_888 ImageReader          │             │
│           │ │   (1280x720 @ 30 FPS)              │             │
│           │ └─────────────┬──────────────────────┘             │
│           │               │                                     │
│           │ ┌─────────────▼──────────────┐                     │
│           │ │  YuvConverter (Kotlin)     │                     │
│           │ │  Fast Integer YUV→RGBA     │                     │
│           │ └─────────────┬──────────────┘                     │
│           │               │                                     │
│  ┌────────▼───────────────▼───────────────┐                    │
│  │      ImageProcessor (Kotlin)           │                    │
│  │           JNI Wrapper                  │                    │
│  └────────────────┬───────────────────────┘                    │
│                   │ JNI Call                                    │
├───────────────────┼─────────────────────────────────────────────┤
│                   │                                             │
│  ┌────────────────▼────────────────────────────┐               │
│  │   image-processing.cpp (C++)               │               │
│  │   ┌──────────────────────────────────┐    │               │
│  │   │  OpenCV Pipeline:                │    │               │
│  │   │  1. RGBA → Grayscale            │    │               │
│  │   │  2. Gaussian Blur (5x5)         │    │               │
│  │   │  3. Canny Edge Detection        │    │               │
│  │   │  4. Grayscale → RGBA            │    │               │
│  │   └──────────────────────────────────┘    │               │
│  └────────────────┬────────────────────────────┘               │
│                   │ Return RGBA                                 │
├───────────────────┼─────────────────────────────────────────────┤
│                   │                                             │
│  ┌────────────────▼───────────────────────────┐                │
│  │  GLTextureRenderer (Kotlin)                │                │
│  │           JNI Wrapper                      │                │
│  └────────────────┬───────────────────────────┘                │
│                   │ JNI Call                                    │
├───────────────────┼─────────────────────────────────────────────┤
│                   │                                             │
│  ┌────────────────▼────────────────────────────┐               │
│  │   gl-renderer-jni.cpp (C++)                │               │
│  │   TextureRenderer.cpp                      │               │
│  │   ┌──────────────────────────────────┐    │               │
│  │   │  OpenGL ES 2.0 Pipeline:        │    │               │
│  │   │  1. Vertex Shader               │    │               │
│  │   │  2. Fragment Shader             │    │               │
│  │   │  3. Texture Upload (VBO/IBO)    │    │               │
│  │   │  4. Render to Screen            │    │               │
│  │   └──────────────────────────────────┘    │               │
│  └────────────────┬────────────────────────────┘               │
│                   │                                             │
├───────────────────┼─────────────────────────────────────────────┤
│                   │                                             │
│  ┌────────────────▼───────────────────────────┐                │
│  │  TextureView (Kotlin)                      │                │
│  │  GLSurfaceView - Display @60 FPS          │                │
│  └────────────────────────────────────────────┘                │
│                                                                 │
│  ┌──────────────────────────────────────────┐                  │
│  │  PerformanceMonitor (Kotlin)             │                  │
│  │  - FPS Tracking                          │                  │
│  │  - Stage Timing (YUV/OpenCV/GL)         │                  │
│  │  - Logcat Output                         │                  │
│  └──────────────────────────────────────────┘                  │
│                                                                 │
│  ┌──────────────────────────────────────────┐                  │
│  │  FrameSaver (Kotlin)                     │                  │
│  │  - Save PNG to Downloads/Flam            │                  │
│  │  - Export JSON Metadata                  │                  │
│  └──────────────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      WEB VIEWER (TypeScript)                     │
├─────────────────────────────────────────────────────────────────┤
│  FrameViewer.ts → Canvas Rendering → HTML5 Display              │
│  - Display saved PNG frames                                      │
│  - Show metadata (FPS, resolution, timings)                      │
└─────────────────────────────────────────────────────────────────┘
```

### Processing Pipeline Flow (15-20 FPS End-to-End)

```
Camera2 API (1280x720 @ 30 FPS)
    ↓ [8ms]
YUV_420_888 ImageReader → YUV to RGBA Conversion
    ↓ [12-15ms]
JNI → OpenCV C++ → Gaussian Blur → Canny Edge Detection
    ↓ [3-5ms]
OpenGL ES 2.0 Texture Upload & Rendering
    ↓ [<1ms]
GLSurfaceView Display @ 60 FPS
    ↓ [Optional]
Save to Downloads/Flam/*.png + metadata.json
```

## 📁 Project Structure

```
Flam/
├── app/src/main/
│   ├── java/com/flam/
│   │   ├── MainActivity.kt              # Main activity with integrated pipeline
│   │   ├── CameraManager.kt             # Camera2 API wrapper (30 FPS capture)
│   │   ├── YuvConverter.kt              # YUV→RGBA conversion (fast integer)
│   │   ├── ImageProcessor.kt            # JNI wrapper for OpenCV
│   │   ├── PerformanceMonitor.kt        # FPS & timing tracker
│   │   ├── PerformanceOverlay.kt        # Custom view for FPS graph
│   │   ├── FrameSaver.kt                # PNG export to Downloads
│   │   ├── GLTextureRenderer.kt         # OpenGL ES 2.0 renderer
│   │   ├── TextureView.kt               # GLSurfaceView wrapper
│   │   ├── CameraPreviewActivity.kt     # Camera preview demo
│   │   └── TextureDemoActivity.kt       # Texture rendering demo
│   ├── res/
│   │   └── layout/
│   │       ├── activity_main.xml        # Main UI with controls
│   │       ├── activity_camera_preview.xml
│   │       └── activity_texture_demo.xml
│   └── AndroidManifest.xml              # App manifest & permissions
│
├── jni/                                 # C++ JNI Native Code
│   ├── image-processing.cpp             # OpenCV Canny edge detection
│   ├── gl-renderer-jni.cpp              # OpenGL renderer JNI bindings
│   └── native-lib.cpp                   # Legacy native methods
│
├── gl/                                  # OpenGL ES 2.0 Renderer
│   ├── shaders.h                        # GLSL vertex/fragment shaders
│   ├── TextureRenderer.h                # Texture renderer interface
│   ├── TextureRenderer.cpp              # High-performance renderer
│   ├── GLRenderer.h                     # Legacy renderer interface
│   └── GLRenderer.cpp                   # Legacy renderer
│
├── web/                                 # TypeScript Web Viewer
│   ├── src/
│   │   ├── index.ts                     # Public API exports
│   │   ├── app.ts                       # Main application
│   │   ├── FrameViewer.ts               # Canvas frame display
│   │   ├── styles.css                   # Web UI styles
│   │   └── index.html                   # HTML template
│   ├── package.json                     # NPM dependencies
│   ├── tsconfig.json                    # TypeScript config
│   ├── webpack.config.js                # Webpack bundler config
│   └── README.md                        # Web viewer docs
│
├── build.gradle                         # Project-level Gradle config
├── app/build.gradle                     # App-level Gradle config
├── CMakeLists.txt                       # CMake build for C++/OpenCV
├── gradle.properties                    # Gradle properties
├── settings.gradle                      # Project settings
├── local.properties.sample              # SDK paths template
├── .gitignore                           # Git ignore rules
├── README.md                            # This file
└── SETUP.md                             # Detailed setup instructions
```

## 🚀 Quick Start

### Prerequisites

- **Android Studio** Hedgehog 2023.1.1+ or later
- **Android SDK** API 34 (Android 14)
- **Android NDK** 25.2.9519653
- **CMake** 3.22.1
- **OpenCV Android SDK** 4.8.0 ([Download](https://opencv.org/releases/))
- **JDK** 17 (bundled with Android Studio)

### Setup Steps

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd Flam
   ```

2. **Download OpenCV Android SDK**
   ```bash
   # Download from https://opencv.org/releases/
   unzip opencv-4.8.0-android-sdk.zip
   mv opencv-android-sdk ~/opencv-android-sdk
   ```

3. **Set OpenCV Path**
   ```bash
   # Add to ~/.bashrc or ~/.zshrc
   export OPENCV_ANDROID_SDK=~/opencv-android-sdk
   source ~/.bashrc
   ```

4. **Install NDK & CMake**
   - Open Android Studio → SDK Manager → SDK Tools
   - Check: NDK (25.2.9519653) and CMake (3.22.1)
   - Click Apply

5. **Build Project**
   ```bash
   ./gradlew assembleDebug
   ```

6. **Run on Device**
   - Connect Android device (API 24+)
   - Enable USB debugging
   - Run from Android Studio or:
   ```bash
   ./gradlew installDebug
   ```

**For detailed setup instructions, see [SETUP.md](SETUP.md)**

## 📋 Dependencies

### Android Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9.20 | Primary language |
| Gradle | 8.2.0 | Build system |
| Android SDK | 34 | Compile SDK |
| Min SDK | 24 | Android 7.0+ |
| NDK | 25.2.9519653 | Native C++ support |
| CMake | 3.22.1 | C++ build system |
| **OpenCV SDK** | **4.8.0** | **Computer vision (REQUIRED)** |

### OpenCV SDK Configuration

**Environment Variable (Recommended):**
```bash
export OPENCV_ANDROID_SDK=/path/to/opencv-android-sdk
```

**Or in `gradle.properties`:**
```properties
OPENCV_ANDROID_SDK=/path/to/opencv-android-sdk
```

**Verify Setup:**
```bash
echo $OPENCV_ANDROID_SDK
ls $OPENCV_ANDROID_SDK/sdk/native/jni/
# Should show: abi-arm64-v8a, abi-armeabi-v7a
```

### Kotlin Libraries

```gradle
// AndroidX Core
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1

// Material Design
com.google.android.material:material:1.11.0

// Lifecycle & Coroutines
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
kotlinx.coroutines:kotlinx-coroutines-android:1.7.3
```

### Native Libraries (C++)

```cmake
# OpenCV (architecture-specific)
${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-arm64-v8a/
${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-armeabi-v7a/

# Android NDK
log, android, EGL, GLESv2
```

### Web Viewer Dependencies

```json
{
  "typescript": "^5.3.3",
  "webpack": "^5.89.0",
  "ts-loader": "^9.5.1",
  "http-server": "^14.1.1"
}
```

## 🔧 Build Configuration

### Gradle Build

**Project-level `build.gradle`:**
```gradle
ext {
    kotlin_version = '1.9.20'
    gradle_version = '8.2.0'
    min_sdk_version = 24
    target_sdk_version = 34
    ndk_version = '25.2.9519653'
    cmake_version = '3.22.1'
}
```

**App-level `build.gradle`:**
```gradle
android {
    compileSdk 34
    ndkVersion '25.2.9519653'

    defaultConfig {
        minSdk 24
        targetSdk 34
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a'
        }
    }

    externalNativeBuild {
        cmake {
            path '../CMakeLists.txt'
            version '3.22.1'
        }
    }
}
```

### CMake Configuration

**CMakeLists.txt:**
```cmake
# OpenCV SDK detection
set(OPENCV_ANDROID_SDK "$ENV{OPENCV_ANDROID_SDK}")

# Architecture-specific OpenCV paths
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-arm64-v8a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-armeabi-v7a")
endif()

# Find and link OpenCV
find_package(OpenCV REQUIRED)
target_link_libraries(flam ${OpenCV_LIBS})
```

## 📱 Usage

### Main Features

1. **Real-time Camera Processing**
   - Open app → Grant camera permission
   - View live camera feed at 1280x720 @ 30 FPS

2. **Edge Detection**
   - Toggle "Edge Detection" switch
   - Adjust low/high thresholds with sliders
   - See real-time Canny edge detection

3. **Performance Monitoring**
   - View FPS counter (Camera FPS | Pipeline FPS)
   - See per-frame timing (avg/min/max)
   - Check OpenCV and GL stage timings
   - Monitor dropped/slow frames

4. **Save Frames**
   - Tap "💾 Save Frame to Downloads"
   - Frame saved to `Downloads/Flam/`
   - PNG image + JSON metadata exported

5. **Web Viewer**
   - Build web viewer: `cd web && npm install && npm run dev`
   - Open browser to `http://localhost:8080`
   - View saved frames with metadata

### Performance Metrics

**Expected Performance:**
- Camera capture: 30 FPS
- YUV conversion: ~8ms
- OpenCV processing: 12-15ms
- GL rendering: 3-5ms
- **End-to-end: 15-20 FPS**

**Logcat Output:**
```
I/MainActivity: === Performance Metrics ===
I/MainActivity: Camera FPS: 29.8
I/MainActivity: Pipeline FPS: 18.2
I/MainActivity: Frame time: Avg=25.3ms, Min=18ms, Max=42ms
I/MainActivity: Stage timings: OpenCV=12ms, GL=3ms
I/MainActivity: ==========================
```

## 🎯 Key Features

### Camera2 API Integration
- **1280x720 @ 30 FPS** YUV_420_888 capture
- Background thread for camera operations
- Double buffering (`MAX_IMAGES=2`)
- Fast autofocus and auto-exposure
- Thread-safe with semaphore locking

### YUV to RGBA Conversion
- **Fast integer-only conversion** (no floating-point math)
- ITU-R BT.601 color standard
- Zero-copy direct buffer access
- Reusable buffers to avoid allocations

### OpenCV Processing (C++)
- Grayscale conversion
- Gaussian blur (5x5 kernel)
- Canny edge detection (configurable thresholds)
- Direct I/O processing (zero intermediate copies)
- Smart pointer memory management

### OpenGL ES 2.0 Rendering
- **60 FPS target** with continuous rendering
- Static VBO/IBO (no per-frame uploads)
- `glTexSubImage2D` for texture reuse (faster updates)
- Mutex-protected thread safety
- Brightness control shader

## Performance Optimizations

1. **Zero-Copy Pipeline**
   - Direct buffer access in YUV conversion
   - JNI uses direct byte arrays
   - OpenGL maps buffers without copies

2. **Reusable Buffers**
   - Pre-allocated RGBA buffers
   - Static vertex/index buffers
   - Texture reuse with `glTexSubImage2D`

3. **Thread Management**
   - Dedicated camera background thread
   - GL operations on GL thread via `queueEvent()`
   - Main thread for UI updates only

4. **Frame Management**
   - `acquireLatestImage()` drops old frames
   - Double buffering in ImageReader
   - No frame queue backlog

## Building

### Prerequisites

1. **OpenCV Android SDK**
   ```bash
   # Download OpenCV Android SDK
   # Set environment variable
   export OPENCV_ANDROID_SDK=/path/to/opencv-android-sdk
   ```

2. **Android NDK**
   - NDK version: r25c or later
   - CMake version: 3.22.1

3. **Gradle**
   - Gradle plugin: 8.2.0
   - Kotlin: 1.9.20

### Build Commands

```bash
# Build project
./gradlew assembleDebug

# Install on device
./gradlew installDebug

# Build TypeScript module
cd web
npm install
npm run build
```

## Runtime Requirements

- Android 7.0+ (API 24+)
- Camera2 API support
- OpenGL ES 2.0 support
- Camera permission

## Lifecycle Management

### Activity Lifecycle
```
onCreate()
  ├── initializeViews()
  ├── initializeProcessingPipeline()
  │   ├── CameraManager.initialize()
  │   └── ImageProcessor initialization
  ├── setupControls()
  └── startPipeline()
      ├── Allocate buffers
      └── CameraManager.startCamera()

onResume()
  └── TextureView.onResume()

onPause()
  ├── TextureView.onPause()
  └── CameraManager.stopCamera()

onDestroy()
  ├── CameraManager.stopCamera()
  ├── ImageProcessor.cleanup()
  ├── TextureView.cleanup()
  └── Clear buffers
```

## Usage

### Basic Camera Preview
```kotlin
val cameraManager = CameraManager(context)
cameraManager.initialize()
cameraManager.startCamera { rgbaData, width, height ->
    // Process frame
    textureView.updateTexture(rgbaData, width, height)
}
```

### Edge Detection Processing
```kotlin
val processor = ImageProcessor()
val outputBuffer = ByteArray(width * height * 4)

processor.processFrameDirect(
    width, height,
    inputRgba, outputBuffer,
    lowThreshold = 50,
    highThreshold = 150
)
```

### OpenGL Rendering
```kotlin
val textureView = TextureView(context)
textureView.setContinuousRendering()  // 60 FPS
textureView.updateTexture(rgbaData, width, height)
```

## Performance Metrics

- **Camera Capture**: 30 FPS @ 1280x720
- **YUV Conversion**: ~8ms per frame
- **OpenCV Processing**: ~15ms per frame (Canny edge detection)
- **OpenGL Rendering**: 60 FPS target
- **Total Pipeline**: 15-20 FPS end-to-end

## License

Copyright © 2025 Flam Project
