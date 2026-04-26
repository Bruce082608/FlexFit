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
                        val keypoints = convertPoseToKeypoints(pose)
                        val confidence = calculateAverageConfidence(pose)
                        callback.onPoseDetected(keypoints, confidence)
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
    
    private fun convertPoseToKeypoints(pose: Pose): FloatArray {
        val keypoints = FloatArray(19 * 3) // 19 keypoints * 3 (x, y, z)
        
        // ML Kit landmark indices
        val mlKitLandmarks = mapOf(
            // Our index -> ML Kit landmark type
            0 to PoseLandmark.NOSE,
            1 to PoseLandmark.RIGHT_HIP,
            2 to PoseLandmark.RIGHT_KNEE,
            3 to PoseLandmark.RIGHT_ANKLE,
            4 to PoseLandmark.LEFT_HIP,
            5 to PoseLandmark.LEFT_KNEE,
            6 to PoseLandmark.LEFT_ANKLE,
            7 to PoseLandmark.RIGHT_SHOULDER, // spine approximation
            8 to PoseLandmark.RIGHT_SHOULDER, // thorax approximation
            9 to PoseLandmark.RIGHT_SHOULDER, // neck approximation
            10 to PoseLandmark.NOSE,
            11 to PoseLandmark.LEFT_SHOULDER,
            12 to PoseLandmark.LEFT_ELBOW,
            13 to PoseLandmark.LEFT_WRIST,
            14 to PoseLandmark.RIGHT_SHOULDER,
            15 to PoseLandmark.RIGHT_ELBOW,
            16 to PoseLandmark.RIGHT_WRIST,
            17 to PoseLandmark.LEFT_EAR,
            18 to PoseLandmark.RIGHT_EAR
        )
        
        pose.allPoseLandmarks.forEach { landmark ->
            val ourIndex = mlKitLandmarks.entries.find { it.value == landmark.landmarkType }?.key
            if (ourIndex != null) {
                keypoints[ourIndex * 3] = landmark.position.x.toFloat()
                keypoints[ourIndex * 3 + 1] = landmark.position.y.toFloat()
                keypoints[ourIndex * 3 + 2] = 0f
            }
        }
        
        // Estimate missing keypoints
        estimateMissingKeypoints(keypoints, pose)
        
        return keypoints
    }
    
    private fun estimateMissingKeypoints(keypoints: FloatArray, pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        
        // Estimate spine (index 7)
        if (keypoints[7 * 3] == 0f && leftShoulder != null && leftHip != null) {
            keypoints[7 * 3] = ((leftShoulder.position.x + leftHip.position.x) / 2).toFloat()
            keypoints[7 * 3 + 1] = ((leftShoulder.position.y + leftHip.position.y) / 2).toFloat()
            keypoints[7 * 3 + 2] = 0f
        }

        // Estimate thorax (index 8) - shoulder center
        if (keypoints[8 * 3] == 0f && leftShoulder != null && rightShoulder != null) {
            keypoints[8 * 3] = ((leftShoulder.position.x + rightShoulder.position.x) / 2).toFloat()
            keypoints[8 * 3 + 1] = ((leftShoulder.position.y + rightShoulder.position.y) / 2).toFloat()
            keypoints[8 * 3 + 2] = 0f
        }

        // Estimate neck (index 9) - same as thorax
        if (keypoints[9 * 3] == 0f && leftShoulder != null && rightShoulder != null) {
            keypoints[9 * 3] = ((leftShoulder.position.x + rightShoulder.position.x) / 2).toFloat()
            keypoints[9 * 3 + 1] = ((leftShoulder.position.y + rightShoulder.position.y) / 2).toFloat()
            keypoints[9 * 3 + 2] = 0f
        }

        // Estimate pelvis (index 0) - hip center
        if (keypoints[0 * 3] == 0f && leftHip != null && rightHip != null) {
            keypoints[0] = ((leftHip.position.x + rightHip.position.x) / 2).toFloat()
            keypoints[1] = ((leftHip.position.y + rightHip.position.y) / 2).toFloat()
            keypoints[2] = 0f
        }

        // Estimate head (index 10) - from nose
        if (keypoints[10 * 3] == 0f && nose != null) {
            keypoints[10 * 3] = nose.position.x.toFloat()
            keypoints[10 * 3 + 1] = (nose.position.y - 50).toFloat()
            keypoints[10 * 3 + 2] = 0f
        }
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
