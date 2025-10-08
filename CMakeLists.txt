cmake_minimum_required(VERSION 3.22.1)

project("flam")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include OpenCV
find_package(OpenCV REQUIRED)
include_directories(${OpenCV_INCLUDE_DIRS})

# Include directories
include_directories(
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

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(EGL-lib EGL)
find_library(GLESv2-lib GLESv2)
find_library(GLESv3-lib GLESv3)

# Link libraries
target_link_libraries(
    flam
    ${OpenCV_LIBS}
    ${log-lib}
    ${android-lib}
    ${EGL-lib}
    ${GLESv2-lib}
    ${GLESv3-lib}
)
