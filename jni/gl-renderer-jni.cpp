#include <jni.h>
#include <android/log.h>
#include "../gl/TextureRenderer.h"
#include <map>
#include <memory>
#include <mutex>

#define LOG_TAG "GLRendererJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global renderer instance management
static std::map<jlong, std::unique_ptr<flam::TextureRenderer>> g_renderers;
static std::mutex g_renderersMutex;
static jlong g_nextRendererId = 1;

extern "C" {

/**
 * Create a new TextureRenderer instance
 * @return Renderer ID (handle), or 0 on failure
 */
JNIEXPORT jlong JNICALL
Java_com_flam_GLTextureRenderer_nativeCreate(JNIEnv* env, jobject thiz) {
    try {
        std::lock_guard<std::mutex> lock(g_renderersMutex);

        jlong rendererId = g_nextRendererId++;
        auto renderer = std::make_unique<flam::TextureRenderer>();

        g_renderers[rendererId] = std::move(renderer);

        LOGI("Created renderer with ID: %lld", (long long)rendererId);
        return rendererId;

    } catch (const std::exception& e) {
        LOGE("Exception in nativeCreate: %s", e.what());
        return 0;
    }
}

/**
 * Initialize the renderer (must be called from GL thread)
 * @param rendererId Renderer handle
 * @return true if successful
 */
JNIEXPORT jboolean JNICALL
Java_com_flam_GLTextureRenderer_nativeInit(JNIEnv* env, jobject thiz, jlong rendererId) {
    try {
        std::lock_guard<std::mutex> lock(g_renderersMutex);

        auto it = g_renderers.find(rendererId);
        if (it == g_renderers.end()) {
            LOGE("Invalid renderer ID: %lld", (long long)rendererId);
            return JNI_FALSE;
        }

        bool success = it->second->initialize();
        LOGI("Renderer %lld initialized: %s", (long long)rendererId, success ? "success" : "failed");
        return success ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        LOGE("Exception in nativeInit: %s", e.what());
        return JNI_FALSE;
    }
}

/**
 * Update texture from byte array
 * @param rendererId Renderer handle
 * @param data RGBA byte array
 * @param width Texture width
 * @param height Texture height
 * @return true if successful
 */
JNIEXPORT jboolean JNICALL
Java_com_flam_GLTextureRenderer_nativeUpdateTexture(
        JNIEnv* env,
        jobject thiz,
        jlong rendererId,
        jbyteArray data,
        jint width,
        jint height) {

    if (data == nullptr) {
        LOGE("Texture data is null");
        return JNI_FALSE;
    }

    try {
        std::lock_guard<std::mutex> lock(g_renderersMutex);

        auto it = g_renderers.find(rendererId);
        if (it == g_renderers.end()) {
            LOGE("Invalid renderer ID: %lld", (long long)rendererId);
            return JNI_FALSE;
        }

        // Get byte array data
        jbyte* bytes = env->GetByteArrayElements(data, nullptr);
        if (bytes == nullptr) {
            LOGE("Failed to get byte array elements");
            return JNI_FALSE;
        }

        // Update texture
        bool success = it->second->updateTexture(
            reinterpret_cast<const unsigned char*>(bytes),
            width,
            height
        );

        // Release byte array
        env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

        return success ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        LOGE("Exception in nativeUpdateTexture: %s", e.what());
        return JNI_FALSE;
    }
}

/**
 * Render the current texture
 * @param rendererId Renderer handle
 */
JNIEXPORT void JNICALL
Java_com_flam_GLTextureRenderer_nativeRender(JNIEnv* env, jobject thiz, jlong rendererId) {
    try {
        std::lock_guard<std::mutex> lock(g_renderersMutex);

        auto it = g_renderers.find(rendererId);
        if (it == g_renderers.end()) {
            LOGE("Invalid renderer ID: %lld", (long long)rendererId);
            return;
        }

        it->second->render();

    } catch (const std::exception& e) {
        LOGE("Exception in nativeRender: %s", e.what());
    }
}

/**
 * Handle viewport resize
 * @param rendererId Renderer handle
 * @param width New viewport width
 * @param height New viewport height
 */
JNIEXPORT void JNICALL
Java_com_flam_GLTextureRenderer_nativeResize(
        JNIEnv* env,
        jobject thiz,
        jlong rendererId,
        jint width,
        jint height) {

    try {
        std::lock_guard<std::mutex> lock(g_renderersMutex);

        auto it = g_renderers.find(rendererId);
        if (it == g_renderers.end()) {
            LOGE("Invalid renderer ID: %lld", (long long)rendererId);
            return;
        }

        it->second->resize(width, height);

    } catch (const std::exception& e) {
        LOGE("Exception in nativeResize: %s", e.what());
    }
}

/**
 * Set brightness multiplier
 * @param rendererId Renderer handle
 * @param brightness Brightness factor (1.0 = normal)
 */
JNIEXPORT void JNICALL
Java_com_flam_GLTextureRenderer_nativeSetBrightness(
        JNIEnv* env,
        jobject thiz,
        jlong rendererId,
        jfloat brightness) {

    try {
        std::lock_guard<std::mutex> lock(g_renderersMutex);

        auto it = g_renderers.find(rendererId);
        if (it == g_renderers.end()) {
            LOGE("Invalid renderer ID: %lld", (long long)rendererId);
            return;
        }

        it->second->setBrightness(brightness);

    } catch (const std::exception& e) {
        LOGE("Exception in nativeSetBrightness: %s", e.what());
    }
}

/**
 * Destroy the renderer and clean up resources
 * @param rendererId Renderer handle
 */
JNIEXPORT void JNICALL
Java_com_flam_GLTextureRenderer_nativeDestroy(JNIEnv* env, jobject thiz, jlong rendererId) {
    try {
        std::lock_guard<std::mutex> lock(g_renderersMutex);

        auto it = g_renderers.find(rendererId);
        if (it == g_renderers.end()) {
            LOGE("Invalid renderer ID: %lld", (long long)rendererId);
            return;
        }

        it->second->cleanup();
        g_renderers.erase(it);

        LOGI("Destroyed renderer with ID: %lld", (long long)rendererId);

    } catch (const std::exception& e) {
        LOGE("Exception in nativeDestroy: %s", e.what());
    }
}

} // extern "C"
