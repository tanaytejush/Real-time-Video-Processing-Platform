#include "GLRenderer.h"
#include <GLES3/gl3.h>
#include <android/log.h>

#define LOG_TAG "GLRenderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

GLRenderer::GLRenderer() {
    LOGI("GLRenderer created");
}

GLRenderer::~GLRenderer() {
    LOGI("GLRenderer destroyed");
}

void GLRenderer::init() {
    LOGI("Initializing OpenGL ES 3.0");

    // Set clear color
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    // Enable depth testing
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LESS);
}

void GLRenderer::render() {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    // Rendering logic goes here
}

void GLRenderer::resize(int width, int height) {
    LOGI("Viewport resized to %dx%d", width, height);
    glViewport(0, 0, width, height);
}
