package com.example.flexfit.ml

object MockPoseSequence {
    fun pullUpFrame(frameCount: Long): FloatArray {
        val keypoints = PoseKeypoints.empty()
        val cyclePhase = (frameCount % 180) / 180f * 2 * Math.PI

        val shoulderY = 0.25f + 0.05f * kotlin.math.sin(cyclePhase).toFloat()
        PoseKeypoints.set(keypoints, 11, -0.15f, shoulderY)
        PoseKeypoints.set(keypoints, 12, 0.15f, shoulderY)

        val elbowBend = kotlin.math.cos(cyclePhase).toFloat() * 0.15f + 0.3f
        PoseKeypoints.set(keypoints, 13, -0.2f - elbowBend * 0.3f, 0.15f + elbowBend * 0.1f, 0.1f)
        PoseKeypoints.set(keypoints, 14, 0.2f + elbowBend * 0.3f, 0.15f + elbowBend * 0.1f, -0.1f)

        val wristLift = (kotlin.math.cos(cyclePhase).toFloat() + 1f) * 0.2f
        PoseKeypoints.set(keypoints, 15, -0.25f - elbowBend * 0.4f, 0.1f + wristLift, 0.15f)
        PoseKeypoints.set(keypoints, 16, 0.25f + elbowBend * 0.4f, 0.1f + wristLift, -0.15f)

        PoseKeypoints.set(keypoints, 23, -0.08f, -0.15f)
        PoseKeypoints.set(keypoints, 24, 0.08f, -0.15f)

        val chinUp = (kotlin.math.cos(cyclePhase).toFloat() + 1f) * 0.1f
        PoseKeypoints.set(keypoints, 0, 0f, 0.38f + chinUp)
        PoseKeypoints.set(keypoints, 7, -0.06f, 0.36f + chinUp, 0.05f)
        PoseKeypoints.set(keypoints, 8, 0.06f, 0.36f + chinUp, -0.05f)

        return keypoints
    }

    fun shoulderPressFrame(frameCount: Long): FloatArray {
        val phase = (frameCount % 160).toInt()
        val progress = when {
            phase < 30 -> 0f
            phase < 70 -> (phase - 30) / 40f
            phase < 100 -> 1f
            phase < 140 -> 1f - (phase - 100) / 40f
            else -> 0f
        }.coerceIn(0f, 1f)

        return shoulderPressPose(progress)
    }

    private fun shoulderPressPose(progress: Float): FloatArray {
        val keypoints = PoseKeypoints.empty()
        val shoulderY = 0.28f
        val elbowY = lerp(0.22f, 0.62f, progress)
        val wristY = lerp(0.43f, 0.96f, progress)
        val elbowX = lerp(0.36f, 0.22f, progress)
        val wristX = lerp(0.28f, 0.24f, progress)

        PoseKeypoints.set(keypoints, 0, 0f, 0.78f)
        PoseKeypoints.set(keypoints, 11, -0.18f, shoulderY)
        PoseKeypoints.set(keypoints, 12, 0.18f, shoulderY)
        PoseKeypoints.set(keypoints, 13, -elbowX, elbowY)
        PoseKeypoints.set(keypoints, 14, elbowX, elbowY)
        PoseKeypoints.set(keypoints, 15, -wristX, wristY)
        PoseKeypoints.set(keypoints, 16, wristX, wristY)
        PoseKeypoints.set(keypoints, 23, -0.13f, -0.16f)
        PoseKeypoints.set(keypoints, 24, 0.13f, -0.16f)

        return keypoints
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float {
        return start + (end - start) * progress
    }
}
