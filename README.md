# Real-time Edge Detection

Android application for real-time edge detection using OpenCV, OpenGL ES 2.0, and native C++ processing.

## What is this?

This is an Android app that captures camera frames and applies Canny edge detection in real-time using OpenCV's C++ library. The processed frames are rendered using OpenGL ES for smooth 60 FPS display. There's also a web viewer built with TypeScript to display saved frames.

The main goal was to learn how to integrate OpenCV with Android NDK and achieve decent performance (targeting 15+ FPS) for real-time video processing.

## Features

### Android App
- Camera2 API for 1280x720 @ 30 FPS capture
- OpenCV C++ for Canny edge detection (runs at 12-15ms per frame)
- OpenGL ES 2.0 for hardware-accelerated rendering
- Toggle between raw and edge-detected view
- Live FPS counter and performance metrics
- Save frames as PNG with metadata

### Web Viewer
- Display exported frames from Android app
- Shows FPS, resolution, and processing stats
- Supports JSON file loading with drag-and-drop
- Optional HTTP/WebSocket connections (see integration docs)

## How it works

```
Camera (YUV) -> Convert to RGBA -> JNI -> C++ OpenCV -> Edge Detection -> OpenGL Texture -> Display
```

The pipeline runs on multiple threads to avoid blocking the UI. Camera operations run on a background thread, OpenCV processing happens via JNI calls to native C++, and OpenGL rendering runs on the GL thread.

## Building

You'll need:
- Android Studio (latest version)
- Android NDK r25c or later
- CMake 3.22.1+
- OpenCV 4.8.0 Android SDK

### Setup OpenCV

Download the OpenCV Android SDK from [opencv.org/releases](https://opencv.org/releases/) and extract it:

```bash
cd ~/Downloads
curl -L https://github.com/opencv/opencv/releases/download/4.8.0/opencv-4.8.0-android-sdk.zip -o opencv.zip
unzip opencv.zip
mv opencv-android-sdk ~/opencv-android-sdk
```

Set the environment variable:

```bash
export OPENCV_ANDROID_SDK=~/opencv-android-sdk
```

Add it to your shell config (.bashrc or .zshrc) to make it permanent.

### Build the app

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and click Run.

## Project Structure

```
app/                # Android app (Kotlin)
  src/main/
    java/com/flam/
      MainActivity.kt         # Main activity
      CameraManager.kt        # Camera2 wrapper
      ImageProcessor.kt       # JNI wrapper for OpenCV
      GLTextureRenderer.kt    # OpenGL renderer wrapper
      PerformanceMonitor.kt   # FPS tracking
jni/                # C++ native code
  image-processing.cpp        # OpenCV edge detection
  gl-renderer-jni.cpp         # OpenGL rendering
gl/                 # OpenGL ES renderer
  TextureRenderer.cpp         # Main renderer
  shaders.h                   # Vertex/fragment shaders
web/                # TypeScript web viewer
  src/
    FrameViewer.ts            # Canvas rendering
    app.ts                    # Main app
```

## Dependencies

### Android
- Kotlin 1.9.20
- NDK 25.2.9519653
- OpenCV 4.8.0 (must be installed separately)
- AndroidX Core, AppCompat, Material Design

### Web
- TypeScript 5.3
- Webpack 5

## Configuration

The app uses these settings by default:
- Camera: 1280x720 @ 30 FPS
- Canny thresholds: Low=50, High=150
- Gaussian blur: 5x5 kernel with sigma=1.5

You can adjust the thresholds in real-time using the sliders in the app.

## Performance

On a typical mid-range device:
- Camera capture: 30 FPS
- YUV to RGBA conversion: ~8ms
- OpenCV processing: 12-15ms
- OpenGL rendering: 3-5ms
- End-to-end: 15-20 FPS

The bottleneck is usually the OpenCV processing. Reducing resolution or using simpler filters can improve FPS.

## Web Viewer

The web viewer is a separate TypeScript project in the `web/` folder. It displays frames exported from the Android app.

To run it:

```bash
cd web
npm install
npm run dev
```

Then open http://localhost:8080

Export a frame from the Android app (tap the export button), pull it via ADB, and load it in the browser:

```bash
adb pull /sdcard/Download/Flam/WebExport/latest_frame.json ./
```

Then drag-drop the JSON file onto the web viewer.

## Known Issues

- First frame sometimes takes longer to process (OpenCV initialization)
- Performance varies significantly between devices
- Camera2 API requires Android 7.0+ (API 24)

## Future Ideas

- Add more filters (Sobel, Laplacian, etc.)
- Implement frame buffering for smoother playback
- Add video file processing mode
- HTTP/WebSocket server on Android for live streaming (code exists but not integrated yet)

## Why OpenCV via JNI?

I wanted to learn how native libraries integrate with Android. OpenCV has good Java/Kotlin bindings, but using C++ directly via JNI gives better performance and lets you use the full OpenCV API.

The trade-off is more complex build setup (CMake, NDK) and manual memory management in C++.

## License

This is a learning project. Feel free to use it however you want.

## Acknowledgments

- OpenCV team for the awesome computer vision library
- Android Camera2 API documentation and samples
- Various StackOverflow answers for JNI memory management tips
