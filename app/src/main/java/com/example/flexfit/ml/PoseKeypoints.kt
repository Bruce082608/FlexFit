package com.example.flexfit.ml

/**
 * Shared pose keypoint protocol for analyzer and UI paths.
 *
 * The index order follows the 33-point MediaPipe/ML Kit pose landmark layout.
 * Each landmark occupies three Float slots: x, y, z. The x/y values are
 * normalized around the frame center for camera input and mock data.
 */
object PoseKeypoints {
    const val LANDMARK_COUNT = 33
    const val VALUES_PER_LANDMARK = 3
    const val FLOAT_COUNT = LANDMARK_COUNT * VALUES_PER_LANDMARK

    fun empty(): FloatArray = FloatArray(FLOAT_COUNT)

    fun emptyConfidences(): FloatArray = FloatArray(LANDMARK_COUNT)

    fun isValid(keypoints: FloatArray): Boolean = keypoints.size >= FLOAT_COUNT

    fun set(
        keypoints: FloatArray,
        index: Int,
        x: Float,
        y: Float,
        z: Float = 0f
    ) {
        require(index in 0 until LANDMARK_COUNT) { "Invalid pose landmark index: $index" }
        val offset = index * VALUES_PER_LANDMARK
        keypoints[offset] = x
        keypoints[offset + 1] = y
        keypoints[offset + 2] = z
    }

    fun hasPoint(keypoints: FloatArray, index: Int): Boolean {
        if (!isValid(keypoints) || index !in 0 until LANDMARK_COUNT) return false

        val offset = index * VALUES_PER_LANDMARK
        return keypoints[offset] != 0f || keypoints[offset + 1] != 0f || keypoints[offset + 2] != 0f
    }
}
