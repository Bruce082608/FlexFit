package com.example.flexfit.ml

enum class ExercisePhaseTone {
    NEUTRAL,
    WARNING,
    ACTIVE,
    SUCCESS
}

data class ExercisePhase(
    val key: String,
    val label: String,
    val tone: ExercisePhaseTone = ExercisePhaseTone.NEUTRAL
)

data class ExerciseFeedback(
    val message: String,
    val type: FeedbackType
)

enum class FeedbackType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

enum class VoiceAction {
    START,
    SUCCESS,
    FAIL,
    SWINGING,
    SHRUGGING,
    NOT_HIGH
}

data class ExerciseAnalysisResult(
    val count: Int = 0,
    val phase: ExercisePhase = ExercisePhase(
        key = "preparing",
        label = "Ready",
        tone = ExercisePhaseTone.NEUTRAL
    ),
    val isReady: Boolean = false,
    val feedback: ExerciseFeedback? = null,
    val accuracy: Float = 0f,
    val elapsedTime: Long = 0L,
    val voiceAction: VoiceAction? = null
)

interface ExerciseAnalyzer {
    val exerciseName: String

    fun analyze(keypoints: FloatArray, timestamp: Long): ExerciseAnalysisResult

    fun mockFrame(frameCount: Long): FloatArray = PoseKeypoints.empty()

    fun reset()
}
