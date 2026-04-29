package com.example.flexfit.ml

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Extracts frames from a video file at a controlled frequency for pose analysis.
 *
 * Uses [MediaMetadataRetriever] to pull individual frames. Frame rate is controlled
 * via [frameIntervalMs] to avoid overwhelming the pose detector.
 *
 * Usage:
 * ```
 * val extractor = VideoFrameExtractor(videoUri, context)
 * extractor.videoMetadata { durationMs, frameRate, totalFrames ->
 *     // show metadata to user
 * }
 * extractor.extractFrames(intervalMs = 200) { frame ->
 *     val keypoints = poseDetector.processBitmap(frame)
 *     analyzer.analyze(keypoints, timestamp)
 *     frame.recycle()
 * }
 * ```
 *
 * @param videoUri Content URI of the video file (from GetContent or file:// URI)
 * @param context Android context for content resolver access
 */
class VideoFrameExtractor(
    private val videoUri: Uri,
    private val context: android.content.Context
) {
    private var retriever: MediaMetadataRetriever? = null

    /**
     * Video metadata discovered during initialization.
     */
    data class VideoMetadata(
        val durationMs: Long,
        val frameRate: Float,
        val videoWidth: Int,
        val videoHeight: Int,
        val estimatedTotalFrames: Long
    )

    /**
     * Result of a single frame extraction.
     */
    data class VideoFrame(
        val bitmap: Bitmap,
        val timestampMs: Long,
        val frameIndex: Int
    )

    /**
     * Opens the video file and discovers metadata. Must be called before extractFrames().
     * Safe to call multiple times — re-uses the existing retriever.
     */
    suspend fun initialize(): VideoMetadata = withContext(Dispatchers.IO) {
        ensureRetriever()
        val retriever = this@VideoFrameExtractor.retriever!!

        val durationStr = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        ) ?: "0"
        val frameRateStr = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
        ) ?: "30"
        val widthStr = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
        ) ?: "0"
        val heightStr = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
        ) ?: "0"

        val durationMs = durationStr.toLongOrNull() ?: 0L
        val frameRate = frameRateStr.toFloatOrNull() ?: 30f
        val width = widthStr.toIntOrNull() ?: 0
        val height = heightStr.toIntOrNull() ?: 0
        if (durationMs <= 0L) {
            close()
            throw IllegalArgumentException("Video has no valid duration.")
        }

        val estimatedTotal = estimateFrameCount(
            durationMs = durationMs,
            intervalMs = DEFAULT_FRAME_INTERVAL_MS
        ).toLong()

        VideoMetadata(
            durationMs = durationMs,
            frameRate = frameRate,
            videoWidth = width,
            videoHeight = height,
            estimatedTotalFrames = estimatedTotal
        )
    }

    /**
     * Emits frames from the video at the specified interval.
     *
     * @param intervalMs Minimum milliseconds between frames (default 200ms = 5 fps).
     *                   Using 100ms (10fps) is recommended for smoothest analysis.
     * @param targetHeight Target height in pixels for extracted frames. Frames are
     *                     scaled down to this height to reduce memory usage. Default 480.
     *                     Use null to keep original size.
     * @param emitFirstFrame Whether to emit the frame at position 0. Default true.
     * @param onFrame Called on the IO dispatcher with each extracted frame bitmap.
     *                The caller is responsible for recycling the bitmap after use.
     * @return Flow that completes when all frames have been emitted or on error.
     *         The flow emits the 0-based frame index after each successful extraction.
     */
    fun extractFrames(
        intervalMs: Long = DEFAULT_FRAME_INTERVAL_MS,
        targetHeight: Int = DEFAULT_TARGET_HEIGHT,
        emitFirstFrame: Boolean = true,
        onFrame: suspend (VideoFrame) -> Unit
    ): Flow<Int> = flow {
        ensureRetriever()
        val retriever = this@VideoFrameExtractor.retriever!!

        val durationMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: 0L

        if (durationMs <= 0L) {
            close()
            throw IllegalArgumentException("Video has no valid duration.")
        }

        var frameIndex = 0
        var currentPositionMs = 0L

        // Optionally emit the very first frame at position 0
        if (emitFirstFrame) {
            val frame = extractFrameAt(retriever, 0L, targetHeight)
            if (frame != null) {
                onFrame(VideoFrame(frame, 0L, frameIndex))
                emit(frameIndex)
                frameIndex++
                currentPositionMs = intervalMs.coerceAtMost(durationMs)
            }
        }

        try {
            while (coroutineContext.isActive && currentPositionMs < durationMs) {
                val frame = extractFrameAt(retriever, currentPositionMs, targetHeight)
                if (frame != null) {
                    onFrame(VideoFrame(frame, currentPositionMs, frameIndex))
                    emit(frameIndex)
                    frameIndex++
                }
                currentPositionMs += intervalMs
            }
        } finally {
            close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Counts how many frames will be requested for a duration/interval pair.
     * This intentionally tracks extraction timestamps, not the source video's native FPS.
     */
    fun estimatedFrameCount(intervalMs: Long = DEFAULT_FRAME_INTERVAL_MS): Int {
        val durationMs = retriever?.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: 0L
        return estimateFrameCount(durationMs, intervalMs)
    }

    /**
     * Extracts a single frame at the given timestamp using MediaMetadataRetriever.
     * Returns null if extraction fails.
     */
    private fun extractFrameAt(
        retriever: MediaMetadataRetriever,
        positionMs: Long,
        targetHeight: Int
    ): Bitmap? {
        return try {
            // METHOD_FRAME: decodes the closest frame to the given time.
            // This is more reliable than METHOD_CLOSEST for compressed video.
            val frame = retriever.getFrameAtTime(
                positionMs * 1000L, // microseconds
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: return null

            if (targetHeight > 0 && frame.height > targetHeight) {
                val ratio = targetHeight.toFloat() / frame.height
                val scaledWidth = (frame.width * ratio).toInt()
                val scaled = Bitmap.createScaledBitmap(frame, scaledWidth, targetHeight, true)
                if (scaled != frame) {
                    frame.recycle()
                }
                scaled
            } else {
                frame
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convenience: run a suspend block with the retriever open, then close it.
     */
    suspend fun <T> withRetriever(block: suspend (MediaMetadataRetriever) -> T): T {
        ensureRetriever()
        return try {
            block(this.retriever!!)
        } finally {
            close()
        }
    }

    private fun ensureRetriever() {
        if (retriever == null) {
            retriever = MediaMetadataRetriever().apply {
                try {
                    // Handle content:// URIs from GetContent picker
                    setDataSource(context, videoUri)
                } catch (e: Exception) {
                    // Fallback: try as file path
                    try {
                        val path = videoUri.path ?: throw e
                        setDataSource(path)
                    } catch (e2: Exception) {
                        close()
                        throw IllegalArgumentException("Selected video file cannot be opened.", e2)
                    }
                }
            }
        }
    }

    fun close() {
        try {
            retriever?.release()
        } catch (_: Exception) { }
        retriever = null
    }

    companion object {
        /**
         * Default interval between extracted frames in milliseconds.
         * 200ms = 5 fps — good balance between analysis quality and performance.
         */
        const val DEFAULT_FRAME_INTERVAL_MS = 200L

        /**
         * Default target height for extracted frames.
     * 480px is sufficient for MediaPipe pose detection while keeping memory usage low.
         */
        const val DEFAULT_TARGET_HEIGHT = 480

        fun estimateFrameCount(durationMs: Long, intervalMs: Long): Int {
            if (durationMs <= 0L || intervalMs <= 0L) return 0
            return (((durationMs - 1L) / intervalMs) + 1L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        }
    }
}
