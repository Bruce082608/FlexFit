package com.example.flexfit.ml

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

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

class PoseDetectorWrapper {
    
    private var poseDetector: PoseDetector? = null
    private var isInitialized = false
    
    fun initialize() {
        try {
            val options = AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
            
            poseDetector = PoseDetection.getClient(options)
            isInitialized = true
        } catch (e: Exception) {
            isInitialized = false
            throw e
        }
    }
    
    @OptIn(ExperimentalGetImage::class)
    fun processFrame(
        imageProxy: ImageProxy,
        callback: PoseDetectorCallback
    ) {
        if (!isInitialized || poseDetector == null) {
            callback.onError("PoseDetector not initialized")
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            callback.onError("No image available")
            imageProxy.close()
            return
        }
        
        try {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            poseDetector!!.process(inputImage)
                .addOnSuccessListener { pose ->
                    if (pose.allPoseLandmarks.isNotEmpty()) {
                        val poseFrame = convertPoseToKeypoints(
                            pose = pose,
                            imageWidth = imageProxy.width,
                            imageHeight = imageProxy.height
                        )
                        val confidence = calculateAverageConfidence(pose)
                        callback.onPoseDetected(
                            keypoints = poseFrame.keypoints,
                            landmarkConfidences = poseFrame.landmarkConfidences,
                            confidence = confidence
                        )
                    } else {
                        callback.onPoseNotDetected()
                    }
                }
                .addOnFailureListener { e ->
                    callback.onError(e.message ?: "Unknown error")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            callback.onError(e.message ?: "Unknown error")
            imageProxy.close()
        }
    }
    
    private data class PoseFrame(
        val keypoints: FloatArray,
        val landmarkConfidences: FloatArray
    )

    private fun convertPoseToKeypoints(
        pose: Pose,
        imageWidth: Int,
        imageHeight: Int
    ): PoseFrame {
        val keypoints = PoseKeypoints.empty()
        val landmarkConfidences = PoseKeypoints.emptyConfidences()

        pose.allPoseLandmarks.forEach { landmark ->
            val index = landmark.landmarkType
            if (index in 0 until PoseKeypoints.LANDMARK_COUNT) {
                val x = landmark.position.x / imageWidth - 0.5f
                val y = 0.5f - landmark.position.y / imageHeight
                val z = landmark.position3D.z / maxOf(imageWidth, imageHeight)

                PoseKeypoints.set(keypoints, index, x, y, z)
                landmarkConfidences[index] = landmark.inFrameLikelihood
            }
        }

        return PoseFrame(
            keypoints = keypoints,
            landmarkConfidences = landmarkConfidences
        )
    }
    
    private fun calculateAverageConfidence(pose: Pose): Float {
        if (pose.allPoseLandmarks.isEmpty()) return 0f

        val totalConfidence = pose.allPoseLandmarks.sumOf { it.inFrameLikelihood.toDouble() }
        return (totalConfidence / pose.allPoseLandmarks.size).toFloat()
    }
    
    fun close() {
        poseDetector?.close()
        poseDetector = null
        isInitialized = false
    }
}
