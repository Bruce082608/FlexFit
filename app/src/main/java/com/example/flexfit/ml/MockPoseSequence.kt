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
        val keypoints = PoseKeypoints.empty()
        val cyclePhase = (frameCount % 180) / 180f * 2 * Math.PI
        val pressLift = (kotlin.math.sin(cyclePhase).toFloat() + 1f) * 0.35f

        PoseKeypoints.set(keypoints, 0, 0f, 0.75f)
        PoseKeypoints.set(keypoints, 11, -0.18f, 0.35f)
        PoseKeypoints.set(keypoints, 12, 0.18f, 0.35f)
        PoseKeypoints.set(keypoints, 13, -0.28f, 0.45f + pressLift * 0.4f)
        PoseKeypoints.set(keypoints, 14, 0.28f, 0.45f + pressLift * 0.4f)
        PoseKeypoints.set(keypoints, 15, -0.24f, 0.42f + pressLift)
        PoseKeypoints.set(keypoints, 16, 0.24f, 0.42f + pressLift)
        PoseKeypoints.set(keypoints, 23, -0.12f, -0.10f)
        PoseKeypoints.set(keypoints, 24, 0.12f, -0.10f)

        return keypoints
    }
}
