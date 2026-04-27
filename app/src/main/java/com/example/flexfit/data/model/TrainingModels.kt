package com.example.flexfit.data.model

/**
 * Represents a single pose landmark with position and visibility.
 */
data class PoseLandmark(
    val index: Int,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val visibility: Float
) {
    val isVisible: Boolean get() = visibility > 0.5f
}

/**
 * Represents detected pose with all landmarks.
 */
data class DetectedPose(
    val landmarks: List<PoseLandmark>,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getLandmark(index: Int): PoseLandmark? = landmarks.getOrNull(index)

    fun getLeftShoulder(): PoseLandmark? = getLandmark(PoseLandmarkMapping.LEFT_SHOULDER)
    fun getRightShoulder(): PoseLandmark? = getLandmark(PoseLandmarkMapping.RIGHT_SHOULDER)
    fun getLeftElbow(): PoseLandmark? = getLandmark(PoseLandmarkMapping.LEFT_ELBOW)
    fun getRightElbow(): PoseLandmark? = getLandmark(PoseLandmarkMapping.RIGHT_ELBOW)
    fun getLeftWrist(): PoseLandmark? = getLandmark(PoseLandmarkMapping.LEFT_WRIST)
    fun getRightWrist(): PoseLandmark? = getLandmark(PoseLandmarkMapping.RIGHT_WRIST)
    fun getLeftHip(): PoseLandmark? = getLandmark(PoseLandmarkMapping.LEFT_HIP)
    fun getRightHip(): PoseLandmark? = getLandmark(PoseLandmarkMapping.RIGHT_HIP)
    fun getLeftKnee(): PoseLandmark? = getLandmark(PoseLandmarkMapping.LEFT_KNEE)
    fun getRightKnee(): PoseLandmark? = getLandmark(PoseLandmarkMapping.RIGHT_KNEE)
    fun getLeftAnkle(): PoseLandmark? = getLandmark(PoseLandmarkMapping.LEFT_ANKLE)
    fun getRightAnkle(): PoseLandmark? = getLandmark(PoseLandmarkMapping.RIGHT_ANKLE)
}

/**
 * Represents a feedback message for the user.
 */
data class FeedbackMessage(
    val type: FeedbackType,
    val message: String
)

enum class FeedbackType {
    CORRECT,   // Green - Correct form
    WARNING,   // Orange - Needs attention
    ERROR      // Red - Incorrect form
}

/**
 * Represents a single rep's evaluation.
 */
data class RepEvaluation(
    val repNumber: Int,
    val accuracy: Float,
    val depth: Float,
    val alignment: Float,
    val stability: Float,
    val feedback: List<FeedbackMessage>,
    val isGoodRep: Boolean
)

/**
 * Training session data.
 */
data class TrainingSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseType: ExerciseType,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val totalReps: Int = 0,
    val goodReps: Int = 0,
    val averageAccuracy: Float = 0f,
    val repEvaluations: List<RepEvaluation> = emptyList(),
    val caloriesBurned: Float = 0f
) {
    val durationMinutes: Long
        get() = ((endTime ?: System.currentTimeMillis()) - startTime) / 60000

    val successRate: Float
        get() = if (totalReps > 0) goodReps.toFloat() / totalReps else 0f
}

/**
 * User profile data.
 */
data class UserProfile(
    val name: String = "FlexFit User",
    val email: String = "user@flexfit.com",
    val avatarStyle: Int = 0,
    val avatarUri: String? = null,
    val height: Float = 170f,  // cm
    val weight: Float = 70f,   // kg
    val fitnessGoal: String = "Build Strength",
    val streakDays: Int = 0,
    val totalWorkouts: Int = 0,
    val totalMinutes: Long = 0
)
