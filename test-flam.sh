#!/bin/bash

# Flam Android OpenCV Project - Automated Testing Script
# Usage: ./test-flam.sh [phase]
#   phase: build, install, test, perf, clean, all

set -e  # Exit on error

PACKAGE="com.flam"
ACTIVITY="MainActivity"
SAVE_DIR="/sdcard/Download/Flam"

COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_NC='\033[0m' # No Color

log_info() {
    echo -e "${COLOR_BLUE}[INFO]${COLOR_NC} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[SUCCESS]${COLOR_NC} $1"
}

log_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_NC} $1"
}

log_warn() {
    echo -e "${COLOR_YELLOW}[WARN]${COLOR_NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check adb
    if ! command -v adb &> /dev/null; then
        log_error "adb not found. Install Android SDK platform-tools"
        exit 1
    fi

    # Check OpenCV SDK
    if [ -z "$OPENCV_ANDROID_SDK" ]; then
        log_warn "OPENCV_ANDROID_SDK environment variable not set"
        log_warn "Set it with: export OPENCV_ANDROID_SDK=/path/to/opencv-android-sdk"
    else
        log_success "OpenCV SDK: $OPENCV_ANDROID_SDK"
    fi

    # Check device
    DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        log_error "No Android device connected"
        exit 1
    fi

    log_success "Prerequisites OK"
}

# Phase 1: Build
phase_build() {
    log_info "=== PHASE 1: BUILD ==="

    log_info "Cleaning build..."
    ./gradlew clean > /dev/null 2>&1 || true

    log_info "Building debug APK..."
    ./gradlew assembleDebug

    APK_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    log_success "APK built successfully ($APK_SIZE)"

    log_info "Verifying native libraries..."
    unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libflam.so
    log_success "Native libraries found"
}

# Phase 2: Install
phase_install() {
    log_info "=== PHASE 2: INSTALL ==="

    log_info "Installing APK..."
    ./gradlew installDebug

    log_info "Granting permissions..."
    adb shell pm grant $PACKAGE android.permission.CAMERA 2>/dev/null || log_warn "Camera permission already granted"

    log_success "App installed successfully"
}

# Phase 3: Functional Tests
phase_test() {
    log_info "=== PHASE 3: FUNCTIONAL TESTS ==="

    # Clear logcat
    adb logcat -c

    log_info "Launching app..."
    adb shell am start -n $PACKAGE/.$ACTIVITY
    sleep 3

    # Check initialization
    log_info "Checking initialization..."
    INIT_LOG=$(adb logcat -d MainActivity:I *:S | grep "initialized" | wc -l)
    if [ "$INIT_LOG" -gt 0 ]; then
        log_success "App initialized correctly"
    else
        log_error "App initialization failed"
        adb logcat -d MainActivity:E *:S
        return 1
    fi

    # Wait for camera to start
    sleep 2

    # Check camera started
    log_info "Checking camera..."
    CAMERA_LOG=$(adb logcat -d CameraManager:I *:S | grep "Camera started" | wc -l)
    if [ "$CAMERA_LOG" -gt 0 ]; then
        log_success "Camera started"
    else
        log_error "Camera failed to start"
        return 1
    fi

    # Wait for FPS metrics
    sleep 5

    # Check FPS
    log_info "Checking FPS metrics..."
    FPS_LOG=$(adb logcat -d MainActivity:I *:S | grep "Pipeline FPS")
    if [ -n "$FPS_LOG" ]; then
        log_success "FPS tracking working"
        echo "$FPS_LOG" | tail -1
    else
        log_warn "No FPS data yet"
    fi

    # Test save frame
    log_info "Testing save frame..."
    adb shell input tap 360 1300  # Save button coordinates
    sleep 2

    SAVED_FILES=$(adb shell ls $SAVE_DIR 2>/dev/null | wc -l)
    if [ "$SAVED_FILES" -gt 0 ]; then
        log_success "Frame saved successfully ($SAVED_FILES files)"
        adb shell ls -lh $SAVE_DIR | head -5
    else
        log_warn "No saved frames found"
    fi

    log_success "Functional tests completed"
}

# Phase 4: Performance Tests
phase_perf() {
    log_info "=== PHASE 4: PERFORMANCE TESTS ==="

    log_info "Monitoring performance for 10 seconds..."

    # Clear logcat
    adb logcat -c

    # Monitor for 10 seconds
    timeout 10 adb logcat MainActivity:I *:S | grep "Performance Metrics" &
    PID=$!
    sleep 10
    kill $PID 2>/dev/null || true

    # Get latest metrics
    log_info "Latest performance metrics:"
    adb logcat -d MainActivity:I *:S | grep -A 6 "Performance Metrics" | tail -7

    # Memory usage
    log_info "Checking memory usage..."
    MEMORY=$(adb shell dumpsys meminfo $PACKAGE | grep "TOTAL PSS" | awk '{print $3}')
    log_info "Total PSS: ${MEMORY} KB"

    # CPU usage
    log_info "Checking CPU usage..."
    CPU=$(adb shell top -n 1 | grep $PACKAGE | head -1 | awk '{print $9}')
    log_info "CPU usage: ${CPU}%"

    log_success "Performance tests completed"
}

# Phase 5: Clean
phase_clean() {
    log_info "=== PHASE 5: CLEAN ==="

    log_info "Stopping app..."
    adb shell am force-stop $PACKAGE

    log_info "Clearing app data..."
    adb shell pm clear $PACKAGE > /dev/null 2>&1 || true

    log_info "Cleaning saved frames..."
    adb shell rm -rf $SAVE_DIR/* 2>/dev/null || true

    log_success "Clean completed"
}

# Main
main() {
    PHASE=${1:-all}

    echo ""
    log_info "Flam Testing Script - Phase: $PHASE"
    echo ""

    check_prerequisites

    case $PHASE in
        build)
            phase_build
            ;;
        install)
            phase_install
            ;;
        test)
            phase_test
            ;;
        perf)
            phase_perf
            ;;
        clean)
            phase_clean
            ;;
        all)
            phase_build
            phase_install
            phase_test
            phase_perf
            log_info "All phases completed successfully!"
            ;;
        *)
            log_error "Unknown phase: $PHASE"
            echo "Usage: $0 [build|install|test|perf|clean|all]"
            exit 1
            ;;
    esac

    echo ""
    log_success "Done!"
    echo ""
}

main "$@"
