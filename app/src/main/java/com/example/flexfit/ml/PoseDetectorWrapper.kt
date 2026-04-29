package com.example.flexfit.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

interface PoseDetectorCallback {
    fun onPoseDetected(keypoints: FloatArray, confidence: Float)

    fun onPoseDetected(
        keypoints: FloatArray,
        landmarkConfidences: FloatArray,
        confidence: Float
    ) {
        onPoseDetected(keypoints, confidence)
    }

    fun onPoseNotDetected()
    fun onError(error: String)
}

class PoseDetectorWrapper(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    private val smoother = OneEuroPoseSmoother(PoseKeypoints.LANDMARK_COUNT, PoseKeypoints.VALUES_PER_LANDMARK)

    fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET_NAME)
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(VISIBILITY_THRESHOLD)
                .setMinPosePresenceConfidence(VISIBILITY_THRESHOLD)
                .setMinTrackingConfidence(VISIBILITY_THRESHOLD)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context.applicationContext, options)
            smoother.reset()
            isInitialized = true
        } catch (e: Exception) {
            isInitialized = false
            throw e
        }
    }

    fun processFrame(
        imageProxy: ImageProxy,
        callback: PoseDetectorCallback
    ) {
        if (!isInitialized || poseLandmarker == null) {
            callback.onError("MediaPipe PoseLandmarker not initialized")
            imageProxy.close()
            return
        }

        try {
            val bitmap = imageProxy.toBitmap()
            val rotatedBitmap = bitmap.rotate(imageProxy.imageInfo.rotationDegrees)
            val frame = detectBitmap(rotatedBitmap, System.currentTimeMillis())

            if (rotatedBitmap !== bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap.recycle()

            if (frame != null) {
                callback.onPoseDetected(
                    keypoints = frame.keypoints,
                    landmarkConfidences = frame.landmarkConfidences,
                    confidence = frame.confidence
                )
            } else {
                callback.onPoseNotDetected()
            }
        } catch (e: Exception) {
            callback.onError(e.message ?: "Unknown MediaPipe error")
        } finally {
            imageProxy.close()
        }
    }

    fun processBitmap(
        bitmap: Bitmap,
        callback: PoseDetectorCallback
    ) {
        if (!isInitialized || poseLandmarker == null) {
            callback.onError("MediaPipe PoseLandmarker not initialized")
            return
        }

        try {
            val frame = detectBitmap(bitmap, System.currentTimeMillis())
            if (frame != null) {
                callback.onPoseDetected(
                    keypoints = frame.keypoints,
                    landmarkConfidences = frame.landmarkConfidences,
                    confidence = frame.confidence
                )
            } else {
                callback.onPoseNotDetected()
            }
        } catch (e: Exception) {
            callback.onError(e.message ?: "Unknown MediaPipe error")
        }
    }

    private fun detectBitmap(bitmap: Bitmap, timestampMs: Long): PoseFrame? {
        val argbBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        val result = poseLandmarker?.detect(BitmapImageBuilder(argbBitmap).build())
        if (argbBitmap !== bitmap) {
            argbBitmap.recycle()
        }

        return result?.toPoseFrame(timestampMs)
    }

    private fun PoseLandmarkerResult.toPoseFrame(timestampMs: Long): PoseFrame? {
        val landmarks = landmarks().firstOrNull() ?: return null
        if (landmarks.size < PoseKeypoints.LANDMARK_COUNT) return null

        val landmarkConfidences = PoseKeypoints.emptyConfidences()
        landmarks.forEachIndexed { index, landmark ->
            if (index < landmarkConfidences.size) {
                landmarkConfidences[index] = landmarkConfidence(landmark)
            }
        }

        val minCoreVisibility = CORE_LANDMARKS.minOf { landmarkConfidences[it] }
        if (minCoreVisibility < VISIBILITY_THRESHOLD) return null

        val keypoints = PoseKeypoints.empty()
        landmarks.take(PoseKeypoints.LANDMARK_COUNT).forEachIndexed { index, landmark ->
            PoseKeypoints.set(
                keypoints = keypoints,
                index = index,
                x = landmark.x() - 0.5f,
                y = 0.5f - landmark.y(),
                z = landmark.z()
            )
        }

        val smoothedKeypoints = smoother.smooth(keypoints, timestampMs)
        val confidence = CORE_LANDMARKS.map { landmarkConfidences[it] }.average().toFloat()
        return PoseFrame(smoothedKeypoints, landmarkConfidences, confidence)
    }

    private fun landmarkConfidence(
        landmark: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): Float {
        val visibility = landmark.visibility()
        if (visibility.isPresent) return visibility.get()

        val presence = landmark.presence()
        if (presence.isPresent) return presence.get()

        return 1f
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        smoother.reset()
        isInitialized = false
    }

    private data class PoseFrame(
        val keypoints: FloatArray,
        val landmarkConfidences: FloatArray,
        val confidence: Float
    )

    private fun Bitmap.rotate(rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return this

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private class OneEuroPoseSmoother(
        landmarkCount: Int,
        valuesPerLandmark: Int
    ) {
        private val filters = Array(landmarkCount * valuesPerLandmark) { OneEuroFilter() }

        fun smooth(keypoints: FloatArray, timestampMs: Long): FloatArray {
            val timestampSeconds = timestampMs / 1000.0
            return FloatArray(keypoints.size) { index ->
                filters[index].filter(keypoints[index], timestampSeconds)
            }
        }

        fun reset() {
            filters.forEach { it.reset() }
        }
    }

    private class OneEuroFilter(
        private val minCutoff: Double = 1.0,
        private val beta: Double = 0.01,
        private val dCutoff: Double = 1.0
    ) {
        private var previousValue: Double? = null
        private var previousDerivative = 0.0
        private var previousTimestamp: Double? = null

        fun filter(value: Float, timestamp: Double): Float {
            val previous = previousValue
            val previousTime = previousTimestamp
            if (previous == null || previousTime == null) {
                previousValue = value.toDouble()
                previousTimestamp = timestamp
                return value
            }

            val dt = max(timestamp - previousTime, 1e-3)
            val derivative = (value - previous) / dt
            val derivativeAlpha = alpha(dCutoff, dt)
            val filteredDerivative = derivativeAlpha * derivative + (1.0 - derivativeAlpha) * previousDerivative
            val cutoff = minCutoff + beta * abs(filteredDerivative)
            val valueAlpha = alpha(cutoff, dt)
            val filtered = valueAlpha * value + (1.0 - valueAlpha) * previous

            previousValue = filtered
            previousDerivative = filteredDerivative
            previousTimestamp = timestamp
            return filtered.toFloat()
        }

        fun reset() {
            previousValue = null
            previousDerivative = 0.0
            previousTimestamp = null
        }

        private fun alpha(cutoff: Double, dt: Double): Double {
            val tau = 1.0 / (2.0 * PI * cutoff)
            return 1.0 / (1.0 + tau / dt)
        }
    }

    private companion object {
        const val MODEL_ASSET_NAME = "pose_landmarker_lite.task"
        const val VISIBILITY_THRESHOLD = 0.5f

        val CORE_LANDMARKS = intArrayOf(
            0,
            7,
            8,
            11,
            12,
            13,
            14,
            15,
            16,
            23,
            24
        )
    }
}
