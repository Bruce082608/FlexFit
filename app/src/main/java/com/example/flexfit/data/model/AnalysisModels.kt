package com.example.flexfit.data.model

/**
 * Evaluation result for pose detection.
 * This is a placeholder interface that will be implemented by the core algorithm.
 */
data class PoseEvaluationResult(
    val accuracy: Float,
    val depthScore: Float,
    val alignmentScore: Float,
    val stabilityScore: Float,
    val feedback: List<FeedbackMessage>,
    val isGoodRep: Boolean,
    val repCount: Int
)

/**
 * Angle analysis result for specific joints.
 */
data class JointAngleAnalysis(
    val jointName: String,
    val currentAngle: Float,
    val targetAngle: Float,
    val isCorrect: Boolean,
    val suggestion: String? = null
)

/**
 * Camera state enum.
 */
enum class CameraState {
    IDLE,
    STARTING,
    ACTIVE,
    PAUSED,
    STOPPING,
    ERROR
}

/**
 * Workout state enum.
 */
enum class WorkoutState {
    IDLE,
    COUNTDOWN,
    ACTIVE,
    PAUSED,
    COMPLETED
}
