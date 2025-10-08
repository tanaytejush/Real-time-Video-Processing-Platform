cmake_minimum_required(VERSION 3.22.1)

project("flam")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti")

# Android architecture configuration
set(CMAKE_ANDROID_ARCH_ABI ${ANDROID_ABI})
message(STATUS "Building for architecture: ${ANDROID_ABI}")

# OpenCV SDK path - update this to match your OpenCV SDK location
set(OPENCV_ANDROID_SDK "$ENV{OPENCV_ANDROID_SDK}")

if(NOT OPENCV_ANDROID_SDK)
    message(FATAL_ERROR "OPENCV_ANDROID_SDK environment variable not set. Please set it to your OpenCV Android SDK path.")
endif()

# Set OpenCV directory based on architecture
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-arm64-v8a")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(OpenCV_DIR "${OPENCV_ANDROID_SDK}/sdk/native/jni/abi-armeabi-v7a")
else()
    message(FATAL_ERROR "Unsupported Android ABI: ${ANDROID_ABI}")
endif()

message(STATUS "OpenCV Directory: ${OpenCV_DIR}")

# Find OpenCV package
find_package(OpenCV REQUIRED PATHS ${OpenCV_DIR} NO_DEFAULT_PATH)

if(OpenCV_FOUND)
    message(STATUS "OpenCV found: ${OpenCV_VERSION}")
    message(STATUS "OpenCV libraries: ${OpenCV_LIBS}")
    message(STATUS "OpenCV include dirs: ${OpenCV_INCLUDE_DIRS}")
else()
    message(FATAL_ERROR "OpenCV not found at ${OpenCV_DIR}")
endif()

# Include directories
include_directories(
    ${OpenCV_INCLUDE_DIRS}
    ${CMAKE_SOURCE_DIR}/jni
    ${CMAKE_SOURCE_DIR}/gl
)

# Collect source files
file(GLOB JNI_SOURCES "${CMAKE_SOURCE_DIR}/jni/*.cpp")
file(GLOB GL_SOURCES "${CMAKE_SOURCE_DIR}/gl/*.cpp")

# Create shared library
add_library(
    flam
    SHARED
    ${JNI_SOURCES}
    ${GL_SOURCES}
)

# Find required Android libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(EGL-lib EGL)
find_library(GLESv2-lib GLESv2)

# Link libraries - order matters for proper dependency resolution
target_link_libraries(
    flam
    ${OpenCV_LIBS}
    ${log-lib}
    ${android-lib}
    ${EGL-lib}
    ${GLESv2-lib}
)

# Set compiler flags for optimization
target_compile_options(flam PRIVATE
    -O3
    -ffast-math
    -fvisibility=hidden
    -Wall
    -Wextra
)

# Strip symbols in release builds
if(CMAKE_BUILD_TYPE MATCHES Release)
    add_custom_command(TARGET flam POST_BUILD
        COMMAND ${CMAKE_STRIP} --strip-unneeded $<TARGET_FILE:flam>
    )
endif()
