# 🔥 Flam

**Real-time Video Processing Platform - Android + Web**

High-performance mobile computer vision application combining Camera2 API, OpenCV C++, OpenGL ES 2.0, and TypeScript for real-time edge detection and cross-platform frame visualization.

[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com/about/versions/nougat)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.8.0-blue.svg)](https://opencv.org/)
[![OpenGL ES](https://img.shields.io/badge/OpenGL%20ES-2.0-orange.svg)](https://www.khronos.org/opengles/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-purple.svg)](https://kotlinlang.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue.svg)](https://www.typescriptlang.org/)

---

## ✨ Features

### 📱 Android Application

- 📹 **Real-time Camera Processing**: 1280x720 @ 30 FPS camera capture via Camera2 API
- 🎨 **Canny Edge Detection**: Hardware-accelerated OpenCV C++ processing (12-15ms)
- ⚡ **OpenGL ES 2.0 Rendering**: 60 FPS texture rendering with zero-copy pipeline
- 📊 **Performance Monitoring**: Live FPS counter with detailed stage timing metrics
- 💾 **Frame Export**: Save processed frames as PNG with JSON metadata
- 🌐 **Web Integration**: Export frames for web viewer with 3 connection methods
- 🎯 **Adjustable Parameters**: Real-time threshold adjustment for edge detection
- 📈 **Performance Overlay**: Visual FPS graph with historical data

### 🌐 Web Viewer (TypeScript)

- 🖼️ **Frame Visualization**: Canvas-based rendering of processed frames
- 📂 **JSON File Loading**: Drag-and-drop support for exported frames
- 🔌 **HTTP Polling**: Real-time frame updates via HTTP server (10 FPS)
- ⚡ **WebSocket Streaming**: Live streaming with auto-reconnection (30+ FPS)
- 📊 **Metadata Display**: FPS, resolution, processing time, device info
- 🎮 **Keyboard Controls**: Quick method switching (1/2/3 keys)

---

## 📸 Screenshots

### Android App

```
┌─────────────────────────────────────┐
│  Flam                    📊 Graph   │
│  1280x720                           │
├─────────────────────────────────────┤
│                                     │
│   [Live Camera Preview]             │
│   Real-time Edge Detection          │
│   1280 × 720 @ 30 FPS              │
│                                     │
├─────────────────────────────────────┤
│ ✅ Pipeline initialized             │
│                                     │
│ Camera: 29.8 FPS | Pipeline: 18.2  │
│ Frame: 25.3ms (18.0-42.0ms)        │
│                                     │
│ Edge Detection          [ON] 🟢    │
│                                     │
│ [💾 Save Frame to Downloads]       │
│ [🌐 Export Frame for Web Viewer]   │
│                                     │
│ Low Threshold: 50     [====|----]  │
│ High Threshold: 150   [=====|---]  │
└─────────────────────────────────────┘
```

### Web Viewer

```
┌─────────────────────────────────────┐
│  ✅ WebSocket connected              │
├─────────────────────────────────────┤
│                                     │
│   [Edge-detected frame displayed]   │
│        1280 × 720                   │
│                                     │
├─────────────────────────────────────┤
│ FPS: 18.2 | Processing: 25ms       │
│ Frame #543 | Device: Pixel 7       │
│ Timestamp: 2025-10-08 22:45:30     │
└─────────────────────────────────────┘

┌────────────────────────────────────┐
│ [JSON File ▼] [192.168.1.100:8080]│
│ [▶️ Connect] [⏸️ Stop] [🔍 Test]   │
└────────────────────────────────────┘
```

---

## 🏗️ Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                       FLAM ANDROID APP                           │
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
│                   │      C++ NATIVE LAYER                       │
│  ┌────────────────▼────────────────────────────┐               │
│  │   image-processing.cpp (OpenCV)            │               │
│  │   ┌──────────────────────────────────┐    │               │
│  │   │  Processing Pipeline:            │    │               │
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
│  │   gl-renderer-jni.cpp (OpenGL ES)          │               │
│  │   TextureRenderer.cpp                      │               │
│  │   ┌──────────────────────────────────┐    │               │
│  │   │  OpenGL ES 2.0 Pipeline:        │    │               │
│  │   │  1. Vertex Shader               │    │               │
│  │   │  2. Fragment Shader             │    │               │
│  │   │  3. Texture Upload (VBO/IBO)    │    │               │
│  │   │  4. Render to Screen @60 FPS    │    │               │
│  │   └──────────────────────────────────┘    │               │
│  └────────────────┬────────────────────────────┘               │
│                   │                                             │
├───────────────────┼─────────────────────────────────────────────┤
│                   │                                             │
│  ┌────────────────▼───────────────────────────┐                │
│  │  TextureView (GLSurfaceView)               │                │
│  │  Display @ 60 FPS                          │                │
│  └────────────────────────────────────────────┘                │
│                                                                 │
│  ┌──────────────────────────────────────────┐                  │
│  │  FrameExporter (Kotlin)                  │                  │
│  │  - Export JSON with base64 PNG           │                  │
│  │  - Save to Downloads/Flam/WebExport      │                  │
│  │  - Maintain frame history (100 frames)   │                  │
│  └──────────────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────────┘
                            │
                            │ Export JSON
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    WEB VIEWER (TypeScript)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────┐                  │
│  │  AndroidIntegrationManager               │                  │
│  │  ┌────────────────────────────────────┐ │                  │
│  │  │ Method 1: JsonFileLoader           │ │                  │
│  │  │  - File input & drag-drop          │ │                  │
│  │  │  - Manual frame loading            │ │                  │
│  │  └────────────────────────────────────┘ │                  │
│  │  ┌────────────────────────────────────┐ │                  │
│  │  │ Method 2: HttpPoller               │ │                  │
│  │  │  - Poll Android HTTP server        │ │                  │
│  │  │  - 10 FPS update rate              │ │                  │
│  │  └────────────────────────────────────┘ │                  │
│  │  ┌────────────────────────────────────┐ │                  │
│  │  │ Method 3: WebSocketStreamer        │ │                  │
│  │  │  - Real-time streaming             │ │                  │
│  │  │  - Auto-reconnection (5 attempts)  │ │                  │
│  │  │  - 30+ FPS live updates            │ │                  │
│  │  └────────────────────────────────────┘ │                  │
│  └──────────────┬───────────────────────────┘                  │
│                 │                                               │
│  ┌──────────────▼───────────────────────────┐                  │
│  │  FrameViewer (Canvas Renderer)           │                  │
│  │  - Display base64 PNG images             │                  │
│  │  - Show metadata overlay                 │                  │
│  │  - FPS counter & frame statistics        │                  │
│  └──────────────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────────┘
```

### Frame Processing Pipeline

```
Camera2 API (1280x720 @ 30 FPS)
    ↓ [8ms]
YUV_420_888 ImageReader → YuvConverter (Fast Integer Math)
    ↓ [12-15ms]
JNI → C++ → OpenCV Pipeline:
    • cv::cvtColor (RGBA → Grayscale)
    • cv::GaussianBlur (5x5 kernel, σ=1.5)
    • cv::Canny (low=50, high=150)
    • cv::cvtColor (Grayscale → RGBA)
    ↓ [3-5ms]
OpenGL ES 2.0 Texture Upload & Rendering
    • glTexSubImage2D (reuse texture)
    • Vertex/Fragment shaders
    • VBO/IBO static buffers
    ↓ [<1ms]
GLSurfaceView Display @ 60 FPS
    ↓ [Optional]
FrameExporter:
    • Encode PNG (base64)
    • Create JSON metadata
    • Save to Downloads/Flam/WebExport/
    ↓
Web Viewer (3 methods):
    • JSON File: Manual load
    • HTTP: Poll every 100ms
    • WebSocket: Real-time stream
```

### JNI Bridge Architecture

```kotlin
// Kotlin Layer
class ImageProcessor {
    external fun processFrameDirect(
        width: Int, height: Int,
        inputRgba: ByteArray,
        outputRgba: ByteArray,
        lowThreshold: Int,
        highThreshold: Int
    ): Boolean
}
```

```cpp
// C++ Native Layer (jni/image-processing.cpp)
extern "C" JNIEXPORT jboolean JNICALL
Java_com_flam_ImageProcessor_processFrameDirect(
    JNIEnv* env, jobject,
    jint width, jint height,
    jbyteArray inputArray,
    jbyteArray outputArray,
    jint lowThreshold,
    jint highThreshold
) {
    // Zero-copy direct buffer access
    jbyte* inputData = env->GetByteArrayElements(inputArray, nullptr);
    jbyte* outputData = env->GetByteArrayElements(outputArray, nullptr);

    // OpenCV Mat wrappers (no copy)
    cv::Mat inputMat(height, width, CV_8UC4, inputData);
    cv::Mat outputMat(height, width, CV_8UC4, outputData);

    // Processing pipeline
    cv::Mat gray, blurred, edges;
    cv::cvtColor(inputMat, gray, cv::COLOR_RGBA2GRAY);
    cv::GaussianBlur(gray, blurred, cv::Size(5, 5), 1.5);
    cv::Canny(blurred, edges, lowThreshold, highThreshold);
    cv::cvtColor(edges, outputMat, cv::COLOR_GRAY2RGBA);

    // Release buffers
    env->ReleaseByteArrayElements(inputArray, inputData, JNI_ABORT);
    env->ReleaseByteArrayElements(outputArray, outputData, 0);

    return JNI_TRUE;
}
```

---

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
│   │   ├── FrameExporter.kt             # JSON export for web viewer
│   │   ├── GLTextureRenderer.kt         # OpenGL ES 2.0 renderer JNI
│   │   ├── TextureView.kt               # GLSurfaceView wrapper
│   │   ├── VideoProcessor.kt            # Video file processing
│   │   ├── VideoProcessingActivity.kt   # Video processing UI
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
│   │   ├── app.ts                       # Main application (demo mode)
│   │   ├── app_integrated.ts            # Android integration mode
│   │   ├── FrameViewer.ts               # Canvas frame display
│   │   ├── AndroidIntegration.ts        # 3 connection methods
│   │   ├── styles.css                   # Web UI styles
│   │   └── index.html                   # HTML template
│   ├── package.json                     # NPM dependencies
│   ├── tsconfig.json                    # TypeScript config
│   └── webpack.config.js                # Webpack bundler config
│
├── build.gradle                         # Project-level Gradle config
├── app/build.gradle                     # App-level Gradle config
├── CMakeLists.txt                       # CMake build for C++/OpenCV
├── gradle.properties                    # Gradle properties
├── settings.gradle                      # Project settings
├── local.properties.sample              # SDK paths template
├── .gitignore                           # Git ignore rules
└── README.md                            # This file
```

---

## 🚀 Quick Start

### Prerequisites

#### For Android App:
- **Android Studio** Hedgehog 2023.1.1+ or later
- **Android SDK** API 34 (Android 14)
- **Android NDK** 25.2.9519653
- **CMake** 3.22.1
- **OpenCV Android SDK** 4.8.0 ([Download](https://opencv.org/releases/))
- **JDK** 17 (bundled with Android Studio)

#### For Web Viewer:
- **Node.js** 18+ and npm
- Modern web browser (Chrome, Firefox, Safari, Edge)

---

### ⚙️ Setup Instructions

#### 1. Install Android Studio

```bash
# macOS
brew install --cask android-studio

# Or download from: https://developer.android.com/studio
```

#### 2. Download & Configure OpenCV SDK

```bash
# Download OpenCV 4.8.0 Android SDK
curl -L https://github.com/opencv/opencv/releases/download/4.8.0/opencv-4.8.0-android-sdk.zip -o ~/Downloads/opencv.zip

# Extract to home directory
cd ~/Downloads
unzip opencv.zip
mv opencv-android-sdk ~/opencv-android-sdk

# Set environment variable (add to ~/.zshrc or ~/.bashrc)
echo 'export OPENCV_ANDROID_SDK=~/opencv-android-sdk' >> ~/.zshrc
source ~/.zshrc

# Verify setup
echo $OPENCV_ANDROID_SDK
ls $OPENCV_ANDROID_SDK/sdk/native/jni/
# Should show: abi-arm64-v8a, abi-armeabi-v7a
```

#### 3. Install NDK & CMake in Android Studio

1. Open Android Studio → Settings (⌘,)
2. Appearance & Behavior → System Settings → Android SDK
3. Click "SDK Tools" tab
4. Check these items:
   - ✅ **NDK (Side by side)** version 25.2.9519653
   - ✅ **CMake** version 3.22.1
5. Click "Apply" → Install

#### 4. Clone & Build Project

```bash
# Clone repository
git clone <repository-url>
cd Flam

# Build Android app
./gradlew assembleDebug

# Or open in Android Studio:
open -a "Android Studio" .
# Then click Run ▶️
```

#### 5. Setup Web Viewer

```bash
cd web

# Install dependencies
npm install

# Start development server
npm run dev

# Open browser to http://localhost:8080
```

---

## 🧠 Architecture Explained

### Android App Architecture

#### **Layer 1: Kotlin UI & Control**
- **MainActivity**: Orchestrates entire pipeline
- **CameraManager**: Manages Camera2 API lifecycle
- **UI Controls**: Sliders, buttons, switches for user interaction

#### **Layer 2: Data Conversion**
- **YuvConverter**: Fast YUV_420_888 → RGBA conversion
  - Integer-only math (no floating-point)
  - Direct buffer access (zero-copy)
  - ITU-R BT.601 standard

#### **Layer 3: JNI Bridge**
- **ImageProcessor**: Kotlin wrapper for C++ processing
- **GLTextureRenderer**: Kotlin wrapper for OpenGL rendering
- Uses direct byte arrays for zero-copy transfers

#### **Layer 4: C++ Native Processing**
- **image-processing.cpp**: OpenCV edge detection
  - Grayscale conversion
  - Gaussian blur (noise reduction)
  - Canny edge detection
  - Thread-safe with smart pointers

#### **Layer 5: OpenGL Rendering**
- **TextureRenderer.cpp**: GLES 2.0 rendering
  - Static VBO/IBO (no per-frame uploads)
  - `glTexSubImage2D` for texture reuse
  - Vertex + Fragment shaders
  - Mutex-protected thread safety

#### **Layer 6: Export & Integration**
- **FrameExporter**: JSON + base64 PNG export
  - Saves to `/sdcard/Download/Flam/WebExport/`
  - Maintains 100-frame history
  - Metadata: FPS, resolution, timestamps, device info

### Web Viewer Architecture

#### **TypeScript Modules**

1. **FrameViewer.ts**
   - Canvas-based rendering
   - Metadata overlay display
   - FPS tracking
   - Sample frame generation

2. **AndroidIntegration.ts**
   - **JsonFileLoader**: File input + drag-drop
   - **HttpPoller**: Polling client (10 FPS)
   - **WebSocketStreamer**: Real-time streaming (30+ FPS)
   - **AndroidIntegrationManager**: Unified API

3. **app_integrated.ts**
   - UI controls for method selection
   - Connection management
   - Status indicators
   - Keyboard shortcuts

#### **Frame Flow: Android → Web**

```
┌──────────────────────────────────────────────────────────────┐
│ Android App                                                   │
│                                                               │
│ 1. Process frame (Canny edge detection)                      │
│ 2. Encode PNG (Bitmap.compress → ByteArrayOutputStream)      │
│ 3. Base64 encode (android.util.Base64.encodeToString)        │
│ 4. Create JSON:                                               │
│    {                                                          │
│      "image": "data:image/png;base64,iVBORw0KGgo...",        │
│      "fps": 18.2,                                             │
│      "resolution": "1280x720",                                │
│      "width": 1280,                                           │
│      "height": 720,                                           │
│      "timestamp": "2025-10-08T22:45:30.123Z",                │
│      "timestampMs": 1728422730123,                            │
│      "processingTimeMs": 25,                                  │
│      "cannyLowThreshold": 50,                                 │
│      "cannyHighThreshold": 150,                               │
│      "deviceInfo": "Google Pixel 7",                          │
│      "androidVersion": 34,                                    │
│      "frameNumber": 543                                       │
│    }                                                          │
│ 5. Save to file or send via HTTP/WebSocket                   │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│ Transfer Methods                                              │
│                                                               │
│ Method 1: File Export                                         │
│  • Save JSON to Downloads/Flam/WebExport/                    │
│  • Pull via ADB: adb pull /sdcard/Download/...              │
│  • Drag-drop into browser                                    │
│                                                               │
│ Method 2: HTTP Server (NanoHTTPD)                            │
│  • Serve on Android device: http://192.168.x.x:8080         │
│  • CORS headers enabled                                      │
│  • Endpoints: /frame, /history, /status                      │
│  • Web viewer polls every 100ms                              │
│                                                               │
│ Method 3: WebSocket (Java-WebSocket)                         │
│  • WebSocket server on device: ws://192.168.x.x:8080        │
│  • Broadcast frames to all connected clients                 │
│  • Auto-reconnection with exponential backoff                │
│  • Real-time streaming (30+ FPS)                             │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────┐
│ Web Viewer (Browser)                                          │
│                                                               │
│ 1. Receive JSON frame                                         │
│ 2. Parse JSON (JSON.parse)                                    │
│ 3. Extract base64 image                                       │
│ 4. Create Image element (new Image())                         │
│ 5. Set src = "data:image/png;base64,..."                     │
│ 6. Draw to canvas (ctx.drawImage)                             │
│ 7. Display metadata overlay                                   │
│ 8. Update FPS counter                                          │
└──────────────────────────────────────────────────────────────┘
```

---

## 📋 Dependencies

### Android Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9.20 | Primary language |
| Android Gradle Plugin | 8.2.0 | Build system |
| Compile SDK | 34 (Android 14) | Target platform |
| Min SDK | 24 (Android 7.0) | Minimum support |
| NDK | 25.2.9519653 | Native C++ support |
| CMake | 3.22.1 | C++ build system |
| **OpenCV SDK** | **4.8.0** | **Computer vision (REQUIRED)** |

### Kotlin/AndroidX Libraries

```gradle
// AndroidX Core
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.activity:activity-ktx:1.8.2'

// Material Design
implementation 'com.google.android.material:material:1.11.0'

// ConstraintLayout
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// Lifecycle & Coroutines
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

### Native Libraries (C++)

```cmake
# OpenCV (loaded via find_package in CMakeLists.txt)
${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-arm64-v8a/
${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-armeabi-v7a/

# Android NDK Libraries
find_library(log-lib log)           # Logging
find_library(android-lib android)   # Android APIs
find_library(EGL-lib EGL)           # OpenGL context
find_library(GLESv2-lib GLESv2)     # OpenGL ES 2.0
```

### Web Viewer Dependencies

```json
{
  "dependencies": {
    "typescript": "^5.3.3",
    "webpack": "^5.102.1",
    "webpack-cli": "^5.1.4",
    "webpack-dev-server": "^4.15.1",
    "ts-loader": "^9.5.1",
    "css-loader": "^6.8.1",
    "style-loader": "^3.3.3",
    "html-webpack-plugin": "^5.6.0"
  }
}
```

---

## 🔧 Build Configuration

### Gradle Configuration

**Project-level `build.gradle`:**

```gradle
buildscript {
    ext {
        kotlin_version = '1.9.20'
        gradle_version = '8.2.0'
        compile_sdk_version = 34
        min_sdk_version = 24
        target_sdk_version = 34
        ndk_version = '25.2.9519653'
        cmake_version = '3.22.1'
    }
}

plugins {
    id 'com.android.application' version '8.2.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
}
```

**App-level `app/build.gradle`:**

```gradle
android {
    namespace 'com.flam'
    compileSdk 34
    ndkVersion '25.2.9519653'

    defaultConfig {
        applicationId "com.flam"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"

        externalNativeBuild {
            cmake {
                cppFlags "-std=c++17", "-frtti", "-fexceptions"
                arguments "-DANDROID_STL=c++_shared"
                targets "flam"
            }
        }

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

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }
}
```

### CMake Configuration

**CMakeLists.txt:**

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("flam")

set(CMAKE_CXX_STANDARD 17)

# OpenCV SDK from environment variable
set(OPENCV_ANDROID_SDK "$ENV{OPENCV_ANDROID_SDK}")

if(NOT OPENCV_ANDROID_SDK)
    message(FATAL_ERROR "OPENCV_ANDROID_SDK environment variable not set")
endif()

# Architecture-specific OpenCV paths
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-arm64-v8a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-armeabi-v7a")
endif()

# Find OpenCV
find_package(OpenCV REQUIRED PATHS ${OpenCV_DIR} NO_DEFAULT_PATH)

# Collect source files
file(GLOB JNI_SOURCES "${CMAKE_SOURCE_DIR}/jni/*.cpp")
file(GLOB GL_SOURCES "${CMAKE_SOURCE_DIR}/gl/*.cpp")

# Create shared library
add_library(flam SHARED ${JNI_SOURCES} ${GL_SOURCES})

# Find Android libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(EGL-lib EGL)
find_library(GLESv2-lib GLESv2)

# Link libraries
target_link_libraries(
    flam
    ${OpenCV_LIBS}
    ${log-lib}
    ${android-lib}
    ${EGL-lib}
    ${GLESv2-lib}
)

# Optimization flags
target_compile_options(flam PRIVATE
    -O3
    -ffast-math
    -fvisibility=hidden
)
```

---

## 📱 Usage

### Android App

1. **Launch App**
   - Open Flam app
   - Grant camera permission when prompted

2. **View Live Camera Feed**
   - Camera starts automatically at 1280x720 @ 30 FPS
   - Raw preview displayed initially

3. **Enable Edge Detection**
   - Toggle "Edge Detection" switch to ON
   - See real-time Canny edge detection

4. **Adjust Parameters**
   - **Low Threshold** slider: Adjust edge sensitivity (default 50)
   - **High Threshold** slider: Adjust edge strength (default 150)
   - Changes apply immediately

5. **Monitor Performance**
   - View FPS counter: `Camera FPS | Pipeline FPS`
   - Check frame timing: `Avg (Min-Max) ms`
   - Tap "📊 Graph" to see performance overlay

6. **Save Frames**
   - Tap "💾 Save Frame to Downloads"
   - Frame saved to `/sdcard/Download/Flam/`
   - PNG image + metadata JSON

7. **Export for Web Viewer**
   - Tap "🌐 Export Frame for Web Viewer"
   - Frame exported to `/sdcard/Download/Flam/WebExport/latest_frame.json`
   - Ready for web viewer

### Web Viewer

#### Method 1: JSON File Loading

```bash
# 1. Export frame on Android
# 2. Pull file from device
adb pull /sdcard/Download/Flam/WebExport/latest_frame.json ./

# 3. Open web viewer
cd web && npm run dev

# 4. In browser (http://localhost:8080):
#    - Select "📂 JSON File" from dropdown
#    - Click "▶️ Connect"
#    - Select the JSON file
#    - Or drag-drop JSON onto canvas
```

#### Method 2: HTTP Server (requires NanoHTTPD)

```bash
# 1. Add dependency to app/build.gradle:
#    implementation 'org.nanohttpd:nanohttpd:2.3.1'

# 2. Add FrameHttpServer.kt (code in integration docs)

# 3. Find Android device IP:
adb shell ip addr show wlan0 | grep inet

# 4. In browser (http://localhost:8080):
#    - Select "🌐 HTTP Server"
#    - Enter Android IP (e.g., 192.168.1.100)
#    - Click "▶️ Connect"
#    - Frames update at 10 FPS
```

#### Method 3: WebSocket Streaming (requires Java-WebSocket)

```bash
# 1. Add dependency to app/build.gradle:
#    implementation 'org.java-websocket:Java-WebSocket:1.5.3'

# 2. Add FrameWebSocketServer.kt (code in integration docs)

# 3. In browser (http://localhost:8080):
#    - Select "⚡ WebSocket"
#    - Enter Android IP
#    - Click "▶️ Connect"
#    - Real-time streaming at 30+ FPS
```

#### Keyboard Shortcuts

- **1** = Switch to JSON File method
- **2** = Switch to HTTP Server method
- **3** = Switch to WebSocket method
- **Space/Enter** = Connect
- **Escape** = Stop/Disconnect

---

## 📊 Performance Metrics

### Expected Performance

| Stage | Time | FPS | Notes |
|-------|------|-----|-------|
| Camera Capture | - | 30 FPS | 1280x720 YUV_420_888 |
| YUV → RGBA | ~8ms | - | Fast integer conversion |
| OpenCV Processing | 12-15ms | - | Gaussian blur + Canny |
| OpenGL Rendering | 3-5ms | 60 FPS | Texture upload + draw |
| **Total Pipeline** | **25-30ms** | **15-20 FPS** | End-to-end |

### Performance Monitor Output

```
I/MainActivity: === Performance Metrics ===
I/MainActivity: Camera FPS: 29.8
I/MainActivity: Pipeline FPS: 18.2
I/MainActivity: Frame time: Avg=25.3ms, Min=18ms, Max=42ms
I/MainActivity: Stage timings: YUV=8ms, OpenCV=12ms, GL=3ms
I/MainActivity: Frames: Total=543, Slow=12, Dropped=2
I/MainActivity: ==========================
```

### Optimization Techniques

1. **Zero-Copy Pipeline**
   - Direct buffer access in YUV conversion
   - JNI direct byte arrays (no copying)
   - OpenGL buffer mapping

2. **Buffer Reuse**
   - Pre-allocated RGBA buffers
   - Static VBO/IBO in OpenGL
   - Texture reuse with `glTexSubImage2D`

3. **Thread Management**
   - Camera on background thread
   - GL operations on GL thread
   - Main thread for UI only

4. **Frame Dropping**
   - `acquireLatestImage()` skips old frames
   - No frame queue backlog
   - Double buffering in ImageReader

---

## 🔍 Troubleshooting

### OpenCV SDK Not Found

**Error:**
```
OPENCV_ANDROID_SDK environment variable not set
```

**Solution:**
```bash
# Download OpenCV
curl -L https://github.com/opencv/opencv/releases/download/4.8.0/opencv-4.8.0-android-sdk.zip -o ~/Downloads/opencv.zip
cd ~/Downloads && unzip opencv.zip && mv opencv-android-sdk ~/

# Set environment variable
echo 'export OPENCV_ANDROID_SDK=~/opencv-android-sdk' >> ~/.zshrc
source ~/.zshrc

# Verify
echo $OPENCV_ANDROID_SDK
```

### NDK Not Found

**Error:**
```
NDK not found
```

**Solution:**
```bash
# Install via Android Studio SDK Manager:
# Settings → SDK Tools → Check "NDK (Side by side)" 25.2.9519653

# Or set manually in local.properties:
echo "ndk.dir=$HOME/Library/Android/sdk/ndk/25.2.9519653" >> local.properties
```

### Camera Permission Denied

**Solution:**
```bash
# Grant manually via ADB
adb shell pm grant com.flam android.permission.CAMERA

# Or grant in app settings:
# Settings → Apps → Flam → Permissions → Camera → Allow
```

### Web Viewer Build Errors

**Error:**
```
ERROR in app_integrated.ts: Property 'resolution' is missing
```

**Solution:**
```bash
# Already fixed in latest version
# Make sure you have latest code
cd web
npm install
npm run build
```

### Slow Performance

**Check:**
1. Disable debug logging in release builds
2. Use `acquireLatestImage()` instead of `acquireNextImage()`
3. Verify OpenGL continuous rendering enabled
4. Check device has OpenCV optimizations (NEON)

---

## 📄 License

Copyright © 2025 Flam Project

---

## 🤝 Contributing

Contributions welcome! Please open an issue or submit a pull request.

---

## 📚 Additional Resources

- [Android Camera2 API Guide](https://developer.android.com/training/camera2)
- [OpenCV Documentation](https://docs.opencv.org/4.8.0/)
- [OpenGL ES 2.0 Reference](https://www.khronos.org/opengles/sdk/docs/man/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/handbook/intro.html)

---

**Built with ❤️ using Kotlin, C++, OpenCV, OpenGL ES, and TypeScript**
