package com.flam

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Process video files frame-by-frame with OpenCV edge detection
 * Supports MP4, AVI, MOV formats
 */
class VideoProcessor(private val context: Context) {

    companion object {
        private const val TAG = "VideoProcessor"
        private const val TIMEOUT_US = 10000L
    }

    private val imageProcessor = ImageProcessor()
    private val frameSaver = FrameSaver(context)

    data class VideoInfo(
        val width: Int,
        val height: Int,
        val duration: Long,
        val frameRate: Float,
        val totalFrames: Int,
        val mimeType: String
    )

    data class ProcessingProgress(
        val currentFrame: Int,
        val totalFrames: Int,
        val progressPercent: Float,
        val fps: Float
    )

    /**
     * Get video information
     */
    fun getVideoInfo(videoUri: Uri): VideoInfo? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            val frameRate = frameRateStr?.toFloatOrNull() ?: 30f
            val totalFrames = ((duration / 1000.0) * frameRate).toInt()
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            Log.i(TAG, "Video info: ${width}x${height}, $duration ms, $frameRate fps, $totalFrames frames")

            VideoInfo(width, height, duration, frameRate, totalFrames, mimeType)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video info: ${e.message}", e)
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Process video frame-by-frame and save processed frames
     *
     * @param videoUri Input video URI
     * @param outputDir Output directory for processed frames
     * @param lowThreshold Canny low threshold
     * @param highThreshold Canny high threshold
     * @param frameSkip Process every Nth frame (1 = all frames, 2 = every other frame)
     * @param progressCallback Called with progress updates
     * @return Number of frames processed
     */
    fun processVideo(
        videoUri: Uri,
        outputDir: File,
        lowThreshold: Int = 50,
        highThreshold: Int = 150,
        frameSkip: Int = 1,
        progressCallback: ((ProcessingProgress) -> Unit)? = null
    ): Int {
        val extractor = MediaExtractor()
        var processedCount = 0

        try {
            // Open video file
            extractor.setDataSource(context, videoUri, null)

            // Find video track
            val videoTrackIndex = findVideoTrack(extractor)
            if (videoTrackIndex < 0) {
                Log.e(TAG, "No video track found")
                return 0
            }

            extractor.selectTrack(videoTrackIndex)
            val format = extractor.getTrackFormat(videoTrackIndex)

            val width = format.getInteger(MediaFormat.KEY_WIDTH)
            val height = format.getInteger(MediaFormat.KEY_HEIGHT)
            val mimeType = format.getString(MediaFormat.KEY_MIME) ?: "video/avc"

            Log.i(TAG, "Video format: ${width}x${height}, mime: $mimeType")

            // Create codec
            val codec = MediaCodec.createDecoderByType(mimeType)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var frameIndex = 0
            val startTime = System.currentTimeMillis()

            // Create output directory
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Prepare processing buffers
            val inputBuffer = ByteArray(width * height * 4)
            val outputBuffer = ByteArray(width * height * 4)

            var isEOS = false

            while (!isEOS) {
                // Feed input to codec
                val inputBufferId = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferId >= 0) {
                    val inputCodecBuffer = codec.getInputBuffer(inputBufferId)
                    val sampleSize = extractor.readSampleData(inputCodecBuffer!!, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }

                // Get output from codec
                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferId >= 0) {
                    // Process frame if it matches skip pattern
                    if (frameIndex % frameSkip == 0) {
                        val image = codec.getOutputImage(outputBufferId)

                        if (image != null) {
                            // Convert YUV image to RGBA
                            val yuvConverter = YuvConverter()
                            val success = yuvConverter.imageToRgba(image, inputBuffer)

                            if (success) {
                                // Apply edge detection
                                val processed = imageProcessor.processFrameDirect(
                                    width, height,
                                    inputBuffer, outputBuffer,
                                    lowThreshold, highThreshold
                                )

                                if (processed) {
                                    // Save processed frame
                                    val filename = "frame_${String.format("%06d", frameIndex)}.png"
                                    val outputFile = File(outputDir, filename)
                                    saveFrameToFile(outputBuffer, width, height, outputFile)
                                    processedCount++

                                    Log.d(TAG, "Processed frame $frameIndex -> $filename")
                                }
                            }

                            image.close()
                        }
                    }

                    codec.releaseOutputBuffer(outputBufferId, false)
                    frameIndex++

                    // Calculate progress
                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                    val currentFps = if (elapsedTime > 0) frameIndex / elapsedTime else 0.0
                    val videoInfo = getVideoInfo(videoUri)
                    val totalFrames = videoInfo?.totalFrames ?: frameIndex
                    val progress = (frameIndex.toFloat() / totalFrames.toFloat() * 100f).coerceIn(0f, 100f)

                    // Report progress
                    progressCallback?.invoke(
                        ProcessingProgress(
                            currentFrame = frameIndex,
                            totalFrames = totalFrames,
                            progressPercent = progress,
                            fps = currentFps.toFloat()
                        )
                    )

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEOS = true
                    }
                }
            }

            codec.stop()
            codec.release()

            val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
            Log.i(TAG, "Processed $processedCount frames in ${totalTime}s (${processedCount / totalTime} fps)")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing video: ${e.message}", e)
        } finally {
            extractor.release()
        }

        return processedCount
    }

    /**
     * Extract single frame at specific timestamp
     */
    fun extractFrame(videoUri: Uri, timeMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)

            Log.i(TAG, "Extracted frame at ${timeMs}ms: ${bitmap?.width}x${bitmap?.height}")
            bitmap

        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract frame: ${e.message}", e)
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Process single frame from video and apply edge detection
     */
    fun processSingleFrame(
        videoUri: Uri,
        timeMs: Long,
        lowThreshold: Int = 50,
        highThreshold: Int = 150
    ): Bitmap? {
        val bitmap = extractFrame(videoUri, timeMs) ?: return null

        return try {
            // Convert bitmap to RGBA byte array
            val width = bitmap.width
            val height = bitmap.height
            val inputBuffer = ByteArray(width * height * 4)
            val outputBuffer = ByteArray(width * height * 4)

            // Copy bitmap pixels to buffer
            val pixelBuffer = ByteBuffer.allocate(width * height * 4)
            bitmap.copyPixelsToBuffer(pixelBuffer)
            System.arraycopy(pixelBuffer.array(), 0, inputBuffer, 0, inputBuffer.size)

            // Process with edge detection
            val success = imageProcessor.processFrameDirect(
                width, height,
                inputBuffer, outputBuffer,
                lowThreshold, highThreshold
            )

            if (success) {
                // Convert back to bitmap
                val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                outputBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(outputBuffer))
                outputBitmap
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process frame: ${e.message}", e)
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun findVideoTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                return i
            }
        }
        return -1
    }

    private fun saveFrameToFile(rgbaData: ByteArray, width: Int, height: Int, file: File) {
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgbaData))

            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save frame: ${e.message}", e)
        }
    }

    fun cleanup() {
        imageProcessor.cleanup()
    }
}
