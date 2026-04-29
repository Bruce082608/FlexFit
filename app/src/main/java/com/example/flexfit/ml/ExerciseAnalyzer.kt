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

data class ExerciseIssue(
    val key: String,
    val label: String,
    val severity: FeedbackType,
    val suggestion: String
)

data class ExerciseScoreBreakdown(
    val depth: Float = 0f,
    val alignment: Float = 0f,
    val stability: Float = 0f
) {
    val total: Float
        get() = (depth * 0.45f + alignment * 0.30f + stability * 0.25f).coerceIn(0f, 100f)
}

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
    NOT_HIGH,
    SHP_ADJUST_GRIP,
    SHP_ARMS_BALANCE,
    SHP_BODY_UPRIGHT,
    SHP_START_POSITION,
    SHP_START,
    SHP_BRACE_CORE,
    SHP_SHRUGGING,
    SHP_NOT_HIGH,
    SHP_BODY_LEAN,
    SHP_ELBOW_FLARE,
    SHP_BAD_WRIST,
    SHP_SUCCESS,
    SHP_FAIL
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
    val scores: ExerciseScoreBreakdown = ExerciseScoreBreakdown(),
    val issues: List<ExerciseIssue> = emptyList(),
    val attemptedReps: Int = count,
    val elapsedTime: Long = 0L,
    val voiceAction: VoiceAction? = null
) {
    init {
        // Validation guard — no-op for out-of-range values
    }
}

interface ExerciseAnalyzer {
    val exerciseName: String

    fun analyze(keypoints: FloatArray, timestamp: Long): ExerciseAnalysisResult

    fun mockFrame(frameCount: Long): FloatArray = PoseKeypoints.empty()

    fun reset()
}
