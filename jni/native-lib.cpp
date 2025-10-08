#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/opencv.hpp>

#define LOG_TAG "FlamNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_flam_MainActivity_processImage(
        JNIEnv* env,
        jobject /* this */,
        jstring inputPath,
        jstring outputPath) {

    const char* inputPathStr = env->GetStringUTFChars(inputPath, nullptr);
    const char* outputPathStr = env->GetStringUTFChars(outputPath, nullptr);

    LOGI("Processing image: %s -> %s", inputPathStr, outputPathStr);

    // Example OpenCV processing
    cv::Mat image = cv::imread(inputPathStr);
    if (image.empty()) {
        LOGE("Failed to load image");
        env->ReleaseStringUTFChars(inputPath, inputPathStr);
        env->ReleaseStringUTFChars(outputPath, outputPathStr);
        return JNI_FALSE;
    }

    // Process image (example: convert to grayscale)
    cv::Mat grayImage;
    cv::cvtColor(image, grayImage, cv::COLOR_BGR2GRAY);

    // Save result
    bool success = cv::imwrite(outputPathStr, grayImage);

    env->ReleaseStringUTFChars(inputPath, inputPathStr);
    env->ReleaseStringUTFChars(outputPath, outputPathStr);

    return success ? JNI_TRUE : JNI_FALSE;
}
