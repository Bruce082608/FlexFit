package com.example.flexfit.data.model

/**
 * Pose landmark mapping based on standard pose detection format.
 * These indices follow the industry-standard 33-point pose landmark format.
 *
 * Reference: https://google.github.io/mediapipe/solutions/pose
 *
 * TODO: When integrating MediaPipe, add dependency:
 *       implementation("com.google.mediapipe:tasks-vision")
 */
object PoseLandmarkMapping {
    const val NOSE = 0
    const val LEFT_EYE_INNER = 1
    const val LEFT_EYE = 2
    const val LEFT_EYE_OUTER = 3
    const val RIGHT_EYE_INNER = 4
    const val RIGHT_EYE = 5
    const val RIGHT_EYE_OUTER = 6
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_MOUTH = 9
    const val RIGHT_MOUTH = 10
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_PINKY = 17
    const val RIGHT_PINKY = 18
    const val LEFT_INDEX = 19
    const val RIGHT_INDEX = 20
    const val LEFT_THUMB = 21
    const val RIGHT_THUMB = 22
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32

    val landmarkNames = mapOf(
        NOSE to "Nose",
        LEFT_EYE_INNER to "Left Eye Inner",
        LEFT_EYE to "Left Eye",
        LEFT_EYE_OUTER to "Left Eye Outer",
        RIGHT_EYE_INNER to "Right Eye Inner",
        RIGHT_EYE to "Right Eye",
        RIGHT_EYE_OUTER to "Right Eye Outer",
        LEFT_EAR to "Left Ear",
        RIGHT_EAR to "Right Ear",
        LEFT_SHOULDER to "Left Shoulder",
        RIGHT_SHOULDER to "Right Shoulder",
        LEFT_ELBOW to "Left Elbow",
        RIGHT_ELBOW to "Right Elbow",
        LEFT_WRIST to "Left Wrist",
        RIGHT_WRIST to "Right Wrist",
        LEFT_HIP to "Left Hip",
        RIGHT_HIP to "Right Hip",
        LEFT_KNEE to "Left Knee",
        RIGHT_KNEE to "Right Knee",
        LEFT_ANKLE to "Left Ankle",
        RIGHT_ANKLE to "Right Ankle"
    )
}

/**
 * Skeleton connection pairs for drawing the pose.
 */
object SkeletonConnections {
    val connections = listOf(
        // Face
        PoseLandmarkMapping.LEFT_EAR to PoseLandmarkMapping.LEFT_EYE,
        PoseLandmarkMapping.LEFT_EYE to PoseLandmarkMapping.NOSE,
        PoseLandmarkMapping.NOSE to PoseLandmarkMapping.RIGHT_EYE,
        PoseLandmarkMapping.RIGHT_EYE to PoseLandmarkMapping.RIGHT_EAR,

        // Upper body
        PoseLandmarkMapping.LEFT_SHOULDER to PoseLandmarkMapping.RIGHT_SHOULDER,
        PoseLandmarkMapping.LEFT_SHOULDER to PoseLandmarkMapping.LEFT_ELBOW,
        PoseLandmarkMapping.LEFT_ELBOW to PoseLandmarkMapping.LEFT_WRIST,
        PoseLandmarkMapping.RIGHT_SHOULDER to PoseLandmarkMapping.RIGHT_ELBOW,
        PoseLandmarkMapping.RIGHT_ELBOW to PoseLandmarkMapping.RIGHT_WRIST,

        // Torso
        PoseLandmarkMapping.LEFT_SHOULDER to PoseLandmarkMapping.LEFT_HIP,
        PoseLandmarkMapping.RIGHT_SHOULDER to PoseLandmarkMapping.RIGHT_HIP,
        PoseLandmarkMapping.LEFT_HIP to PoseLandmarkMapping.RIGHT_HIP,

        // Lower body
        PoseLandmarkMapping.LEFT_HIP to PoseLandmarkMapping.LEFT_KNEE,
        PoseLandmarkMapping.LEFT_KNEE to PoseLandmarkMapping.LEFT_ANKLE,
        PoseLandmarkMapping.RIGHT_HIP to PoseLandmarkMapping.RIGHT_KNEE,
        PoseLandmarkMapping.RIGHT_KNEE to PoseLandmarkMapping.RIGHT_ANKLE
    )
}
