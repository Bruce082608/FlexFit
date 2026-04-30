package com.example.flexfit.ml

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

object BodyProportionAnalyzer {
    fun analyze(keypoints: FloatArray, landmarkConfidences: FloatArray? = null): BodyProportionAnalysis {
        if (!PoseKeypoints.isValid(keypoints) || !hasRequiredPoints(keypoints, landmarkConfidences)) {
            return BodyProportionAnalysis.Failure("Please keep your full body visible in the photo.")
        }

        val points = StandardLandmark.entries.associateWith { keypoints.imagePoint(it.mediaPipeIndex) }

        val nose = points.getValue(StandardLandmark.NOSE)
        val leftEye = points.getValue(StandardLandmark.LEFT_EYE)
        val rightEye = points.getValue(StandardLandmark.RIGHT_EYE)
        val leftEar = points.getValue(StandardLandmark.LEFT_EAR)
        val rightEar = points.getValue(StandardLandmark.RIGHT_EAR)
        val leftShoulder = points.getValue(StandardLandmark.LEFT_SHOULDER)
        val rightShoulder = points.getValue(StandardLandmark.RIGHT_SHOULDER)
        val leftElbow = points.getValue(StandardLandmark.LEFT_ELBOW)
        val rightElbow = points.getValue(StandardLandmark.RIGHT_ELBOW)
        val leftWrist = points.getValue(StandardLandmark.LEFT_WRIST)
        val rightWrist = points.getValue(StandardLandmark.RIGHT_WRIST)
        val leftHip = points.getValue(StandardLandmark.LEFT_HIP)
        val rightHip = points.getValue(StandardLandmark.RIGHT_HIP)
        val leftKnee = points.getValue(StandardLandmark.LEFT_KNEE)
        val rightKnee = points.getValue(StandardLandmark.RIGHT_KNEE)
        val leftAnkle = points.getValue(StandardLandmark.LEFT_ANKLE)
        val rightAnkle = points.getValue(StandardLandmark.RIGHT_ANKLE)

        val earAngle = angle(leftEar, rightEar)
        val shoulderAngle = angle(leftShoulder, rightShoulder)
        val hipAngle = angle(leftHip, rightHip)
        val earDiffShoulder = lineDiff(earAngle, shoulderAngle)
        val earDiffHip = lineDiff(earAngle, hipAngle)
        val hipDiffShoulder = lineDiff(hipAngle, shoulderAngle)
        val hipMidX = (leftHip.x + rightHip.x) / 2f
        val vertical = abs(nose.x - hipMidX)

        val leftEyeEar = distance(leftEye, leftEar)
        val rightEyeEar = distance(rightEye, rightEar)
        val eyeDistanceEar = abs(leftEyeEar - rightEyeEar)
        val earDistance = distance(leftEar, rightEar).coerceAtLeast(EPSILON)
        val leftEyeNose = distance(leftEye, nose)
        val rightEyeNose = distance(rightEye, nose)
        val eyeDistanceNose = abs(leftEyeNose - rightEyeNose)
        val leftEarNose = distance(leftEar, nose)
        val rightEarNose = distance(rightEar, nose)
        val earDistanceNose = abs(leftEarNose - rightEarNose)

        val standingStraight = earDiffHip < 10f &&
            earDiffShoulder < 10f &&
            hipDiffShoulder < 10f &&
            vertical < hipMidX / 8f
        val facingFront = eyeDistanceEar < earDistance / 4f &&
            eyeDistanceNose < earDistance / 4f &&
            earDistanceNose < earDistance / 4f

        if (!standingStraight && !facingFront) {
            return BodyProportionAnalysis.Failure("Please retake the photo while standing straight and facing the camera.")
        }
        if (!standingStraight) {
            return BodyProportionAnalysis.Failure("Please stand straight before recording body data.")
        }
        if (!facingFront) {
            return BodyProportionAnalysis.Failure("Please face the camera directly before recording body data.")
        }

        val leftEarShoulder = distance(leftEar, leftShoulder)
        val rightEarShoulder = distance(rightEar, rightShoulder)
        val leftShoulderNose = distance(leftShoulder, nose)
        val rightShoulderNose = distance(rightShoulder, nose)
        val leftShoulderHip = distance(leftShoulder, leftHip)
        val rightShoulderHip = distance(rightShoulder, rightHip)
        val leftUpperArm = distance(leftShoulder, leftElbow)
        val rightUpperArm = distance(rightShoulder, rightElbow)
        val leftLowerArm = distance(leftWrist, leftElbow)
        val rightLowerArm = distance(rightWrist, rightElbow)
        val leftThigh = distance(leftHip, leftKnee)
        val rightThigh = distance(rightHip, rightKnee)
        val leftShank = distance(leftAnkle, leftKnee)
        val rightShank = distance(rightAnkle, rightKnee)
        val leftWristShoulder = distance(leftWrist, leftShoulder)
        val rightWristShoulder = distance(rightWrist, rightShoulder)
        val leftShoulder3d = distance3d(nose, leftShoulder)
        val rightShoulder3d = distance3d(nose, rightShoulder)
        val hipDistance3d = distance3d(leftHip, rightHip).coerceAtLeast(EPSILON)

        val coefficients = listOf(
            leftEyeEar / earDistance,
            rightEyeEar / earDistance,
            leftEyeNose / earDistance,
            rightEyeNose / earDistance,
            leftShoulderNose / earDistance,
            rightShoulderNose / earDistance,
            leftShoulderHip / earDistance,
            rightShoulderHip / earDistance,
            leftUpperArm / earDistance,
            rightUpperArm / earDistance,
            leftLowerArm / earDistance,
            rightLowerArm / earDistance,
            leftThigh / earDistance,
            rightThigh / earDistance,
            leftShank / earDistance,
            rightShank / earDistance,
            leftEarShoulder / earDistance,
            rightEarShoulder / earDistance,
            leftWristShoulder / earDistance,
            rightWristShoulder / earDistance,
            leftShoulder3d / hipDistance3d,
            rightShoulder3d / hipDistance3d
        )

        return BodyProportionAnalysis.Success(coefficients)
    }

    private fun hasRequiredPoints(keypoints: FloatArray, landmarkConfidences: FloatArray?): Boolean {
        return StandardLandmark.entries.all { landmark ->
            PoseKeypoints.hasPoint(keypoints, landmark.mediaPipeIndex) &&
                ((landmarkConfidences?.getOrNull(landmark.mediaPipeIndex) ?: 1f) >= VISIBILITY_THRESHOLD)
        }
    }

    private fun FloatArray.imagePoint(index: Int): Point {
        return Point(
            x = this[index.x] + 0.5f,
            y = 0.5f - this[index.y],
            z = this[index.z]
        )
    }

    private fun angle(first: Point, second: Point): Float {
        return Math.toDegrees(atan2((second.y - first.y).toDouble(), (second.x - first.x).toDouble())).toFloat()
    }

    private fun lineDiff(first: Float, second: Float): Float {
        val diff = abs(first - second)
        return min(diff, 180f - diff)
    }

    private fun distance(first: Point, second: Point): Float {
        val dx = first.x - second.x
        val dy = first.y - second.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun distance3d(first: Point, second: Point): Float {
        val dx = first.x - second.x
        val dy = first.y - second.y
        val dz = first.z - second.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private enum class StandardLandmark(val mediaPipeIndex: Int) {
        NOSE(0),
        LEFT_EYE(2),
        RIGHT_EYE(5),
        LEFT_EAR(7),
        RIGHT_EAR(8),
        LEFT_SHOULDER(11),
        RIGHT_SHOULDER(12),
        LEFT_ELBOW(13),
        RIGHT_ELBOW(14),
        LEFT_WRIST(15),
        RIGHT_WRIST(16),
        LEFT_HIP(23),
        RIGHT_HIP(24),
        LEFT_KNEE(25),
        RIGHT_KNEE(26),
        LEFT_ANKLE(27),
        RIGHT_ANKLE(28)
    }

    private data class Point(val x: Float, val y: Float, val z: Float)

    private val Int.x: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK
    private val Int.y: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK + 1
    private val Int.z: Int get() = this * PoseKeypoints.VALUES_PER_LANDMARK + 2

    private const val VISIBILITY_THRESHOLD = 0.5f
    private const val EPSILON = 1e-6f
}

sealed interface BodyProportionAnalysis {
    data class Success(val coefficients: List<Float>) : BodyProportionAnalysis
    data class Failure(val reason: String) : BodyProportionAnalysis
}
