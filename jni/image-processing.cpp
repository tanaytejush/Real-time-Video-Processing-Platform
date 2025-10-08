#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <cstring>
#include <memory>

#define LOG_TAG "ImageProcessing"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global cache for processed frames to manage memory lifecycle
static std::unique_ptr<cv::Mat> g_processedFrame = nullptr;
static std::mutex g_frameMutex;

extern "C" {

/**
 * Converts RGBA frame to grayscale, applies Canny edge detection
 * Returns pointer to processed frame data
 *
 * @param env JNI environment
 * @param thiz Object reference
 * @param width Frame width
 * @param height Frame height
 * @param rgbaData Input RGBA frame data as byte array
 * @param lowThreshold Canny low threshold (default: 50)
 * @param highThreshold Canny high threshold (default: 150)
 * @return Pointer to processed frame data (jlong), or 0 on failure
 */
JNIEXPORT jlong JNICALL
Java_com_flam_ImageProcessor_processFrameNative(
        JNIEnv* env,
        jobject thiz,
        jint width,
        jint height,
        jbyteArray rgbaData,
        jint lowThreshold,
        jint highThreshold) {

    if (rgbaData == nullptr) {
        LOGE("Input RGBA data is null");
        return 0;
    }

    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return 0;
    }

    LOGD("Processing frame: %dx%d, thresholds: [%d, %d]", width, height, lowThreshold, highThreshold);

    try {
        // Get input data from Java byte array
        jbyte* rgbaBytes = env->GetByteArrayElements(rgbaData, nullptr);
        if (rgbaBytes == nullptr) {
            LOGE("Failed to get RGBA byte array elements");
            return 0;
        }

        // Create OpenCV Mat from RGBA data (no copy, just wrapper)
        cv::Mat rgbaMat(height, width, CV_8UC4, reinterpret_cast<unsigned char*>(rgbaBytes));

        // Convert RGBA to Grayscale
        cv::Mat grayMat;
        cv::cvtColor(rgbaMat, grayMat, cv::COLOR_RGBA2GRAY);

        // Apply Gaussian blur to reduce noise before edge detection
        cv::Mat blurredMat;
        cv::GaussianBlur(grayMat, blurredMat, cv::Size(5, 5), 1.5);

        // Apply Canny edge detection
        cv::Mat edgesMat;
        cv::Canny(blurredMat, edgesMat, lowThreshold, highThreshold);

        // Convert back to RGBA for display (edges will be white on black)
        cv::Mat outputRgba;
        cv::cvtColor(edgesMat, outputRgba, cv::COLOR_GRAY2RGBA);

        // Thread-safe frame management
        {
            std::lock_guard<std::mutex> lock(g_frameMutex);

            // Clean up previous frame if it exists
            if (g_processedFrame != nullptr) {
                g_processedFrame.reset();
            }

            // Allocate new frame and copy data
            g_processedFrame = std::make_unique<cv::Mat>(outputRgba.clone());
        }

        // Release Java byte array
        env->ReleaseByteArrayElements(rgbaData, rgbaBytes, JNI_ABORT);

        LOGD("Frame processed successfully, returning pointer");
        return reinterpret_cast<jlong>(g_processedFrame->data);

    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception: %s", e.what());
        return 0;
    } catch (const std::exception& e) {
        LOGE("Standard exception: %s", e.what());
        return 0;
    } catch (...) {
        LOGE("Unknown exception occurred");
        return 0;
    }
}

/**
 * Get processed frame data and copy to Java byte array
 *
 * @param env JNI environment
 * @param thiz Object reference
 * @param outputBuffer Output byte array to receive processed frame
 * @return true if successful, false otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_flam_ImageProcessor_getProcessedFrame(
        JNIEnv* env,
        jobject thiz,
        jbyteArray outputBuffer) {

    if (outputBuffer == nullptr) {
        LOGE("Output buffer is null");
        return JNI_FALSE;
    }

    try {
        std::lock_guard<std::mutex> lock(g_frameMutex);

        if (g_processedFrame == nullptr || g_processedFrame->empty()) {
            LOGE("No processed frame available");
            return JNI_FALSE;
        }

        jsize bufferSize = env->GetArrayLength(outputBuffer);
        jsize requiredSize = g_processedFrame->total() * g_processedFrame->elemSize();

        if (bufferSize < requiredSize) {
            LOGE("Buffer too small: %d < %d", bufferSize, requiredSize);
            return JNI_FALSE;
        }

        // Copy processed frame data to Java array
        jbyte* outputBytes = env->GetByteArrayElements(outputBuffer, nullptr);
        if (outputBytes == nullptr) {
            LOGE("Failed to get output buffer");
            return JNI_FALSE;
        }

        std::memcpy(outputBytes, g_processedFrame->data, requiredSize);

        env->ReleaseByteArrayElements(outputBuffer, outputBytes, 0);

        LOGD("Processed frame copied to buffer");
        return JNI_TRUE;

    } catch (const std::exception& e) {
        LOGE("Exception in getProcessedFrame: %s", e.what());
        return JNI_FALSE;
    }
}

/**
 * Release processed frame memory
 * Should be called when frame is no longer needed
 *
 * @param env JNI environment
 * @param thiz Object reference
 */
JNIEXPORT void JNICALL
Java_com_flam_ImageProcessor_releaseFrame(
        JNIEnv* env,
        jobject thiz) {

    try {
        std::lock_guard<std::mutex> lock(g_frameMutex);

        if (g_processedFrame != nullptr) {
            g_processedFrame.reset();
            LOGD("Processed frame released");
        }
    } catch (const std::exception& e) {
        LOGE("Exception in releaseFrame: %s", e.what());
    }
}

/**
 * Get processed frame dimensions
 *
 * @param env JNI environment
 * @param thiz Object reference
 * @return int array [width, height], or null if no frame available
 */
JNIEXPORT jintArray JNICALL
Java_com_flam_ImageProcessor_getFrameDimensions(
        JNIEnv* env,
        jobject thiz) {

    try {
        std::lock_guard<std::mutex> lock(g_frameMutex);

        if (g_processedFrame == nullptr || g_processedFrame->empty()) {
            LOGE("No processed frame available");
            return nullptr;
        }

        jintArray result = env->NewIntArray(2);
        if (result == nullptr) {
            LOGE("Failed to create int array");
            return nullptr;
        }

        jint dimensions[2] = {g_processedFrame->cols, g_processedFrame->rows};
        env->SetIntArrayRegion(result, 0, 2, dimensions);

        return result;

    } catch (const std::exception& e) {
        LOGE("Exception in getFrameDimensions: %s", e.what());
        return nullptr;
    }
}

/**
 * Direct processing function that returns data in output buffer
 * More efficient for real-time processing
 *
 * @param env JNI environment
 * @param thiz Object reference
 * @param width Frame width
 * @param height Frame height
 * @param inputRgba Input RGBA data
 * @param outputRgba Output RGBA data
 * @param lowThreshold Canny low threshold
 * @param highThreshold Canny high threshold
 * @return true if successful, false otherwise
 */
JNIEXPORT jboolean JNICALL
Java_com_flam_ImageProcessor_processFrameDirect(
        JNIEnv* env,
        jobject thiz,
        jint width,
        jint height,
        jbyteArray inputRgba,
        jbyteArray outputRgba,
        jint lowThreshold,
        jint highThreshold) {

    if (inputRgba == nullptr || outputRgba == nullptr) {
        LOGE("Input or output buffer is null");
        return JNI_FALSE;
    }

    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return JNI_FALSE;
    }

    try {
        jsize inputSize = env->GetArrayLength(inputRgba);
        jsize outputSize = env->GetArrayLength(outputRgba);
        jsize requiredSize = width * height * 4; // RGBA = 4 bytes per pixel

        if (inputSize < requiredSize || outputSize < requiredSize) {
            LOGE("Buffer size mismatch: input=%d, output=%d, required=%d",
                 inputSize, outputSize, requiredSize);
            return JNI_FALSE;
        }

        // Get input data
        jbyte* inputBytes = env->GetByteArrayElements(inputRgba, nullptr);
        jbyte* outputBytes = env->GetByteArrayElements(outputRgba, nullptr);

        if (inputBytes == nullptr || outputBytes == nullptr) {
            LOGE("Failed to get byte array elements");
            if (inputBytes) env->ReleaseByteArrayElements(inputRgba, inputBytes, JNI_ABORT);
            if (outputBytes) env->ReleaseByteArrayElements(outputRgba, outputBytes, JNI_ABORT);
            return JNI_FALSE;
        }

        // Create Mat wrappers (no copy)
        cv::Mat inputMat(height, width, CV_8UC4, reinterpret_cast<unsigned char*>(inputBytes));
        cv::Mat outputMat(height, width, CV_8UC4, reinterpret_cast<unsigned char*>(outputBytes));

        // Processing pipeline
        cv::Mat grayMat, blurredMat, edgesMat;
        cv::cvtColor(inputMat, grayMat, cv::COLOR_RGBA2GRAY);
        cv::GaussianBlur(grayMat, blurredMat, cv::Size(5, 5), 1.5);
        cv::Canny(blurredMat, edgesMat, lowThreshold, highThreshold);
        cv::cvtColor(edgesMat, outputMat, cv::COLOR_GRAY2RGBA);

        // Release arrays
        env->ReleaseByteArrayElements(inputRgba, inputBytes, JNI_ABORT);
        env->ReleaseByteArrayElements(outputRgba, outputBytes, 0);

        LOGD("Direct processing completed");
        return JNI_TRUE;

    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception: %s", e.what());
        return JNI_FALSE;
    } catch (const std::exception& e) {
        LOGE("Exception: %s", e.what());
        return JNI_FALSE;
    }
}

} // extern "C"
