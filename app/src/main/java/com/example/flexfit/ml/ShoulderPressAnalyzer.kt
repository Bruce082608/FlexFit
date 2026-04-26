package com.example.flexfit.ml

class ShoulderPressAnalyzer : ExerciseAnalyzer {
    override val exerciseName: String = "Shoulder Press"

    private enum class ShoulderPressState {
        PREPARING,
        RACK,
        PRESSING,
        LOCKOUT
    }

    private var state = ShoulderPressState.PREPARING
    private var count = 0
    private var isReady = false

    override fun analyze(keypoints: FloatArray, timestamp: Long): ExerciseAnalysisResult {
        if (!PoseKeypoints.isValid(keypoints)) {
            return buildResult(
                timestamp = timestamp,
                feedback = ExerciseFeedback("Pose data incomplete", FeedbackType.ERROR),
                accuracy = 0f
            )
        }

        val feedback = if (!isReady) {
            handlePreparation(keypoints)
        } else {
            handleStateTransitions(keypoints)
        }

        return buildResult(
            timestamp = timestamp,
            feedback = feedback,
            accuracy = if (isReady) 80f else 0f
        )
    }

    override fun mockFrame(frameCount: Long): FloatArray {
        return MockPoseSequence.shoulderPressFrame(frameCount)
    }

    override fun reset() {
        state = ShoulderPressState.PREPARING
        count = 0
        isReady = false
    }

    private fun handlePreparation(keypoints: FloatArray): ExerciseFeedback? {
        return if (isRackPosition(keypoints)) {
            isReady = true
            state = ShoulderPressState.RACK
            ExerciseFeedback("Starting position confirmed. Ready to press!", FeedbackType.SUCCESS)
        } else {
            ExerciseFeedback("Bring hands to shoulder height", FeedbackType.WARNING)
        }
    }

    private fun handleStateTransitions(keypoints: FloatArray): ExerciseFeedback? {
        return when {
            state == ShoulderPressState.RACK && isPressing(keypoints) -> {
                state = ShoulderPressState.PRESSING
                null
            }

            state == ShoulderPressState.PRESSING && isLockout(keypoints) -> {
                state = ShoulderPressState.LOCKOUT
                null
            }

            state == ShoulderPressState.LOCKOUT && isRackPosition(keypoints) -> {
                count++
                state = ShoulderPressState.RACK
                ExerciseFeedback("Shoulder press completed! Count: $count", FeedbackType.SUCCESS)
            }

            else -> null
        }
    }

    private fun isRackPosition(keypoints: FloatArray): Boolean {
        val shoulderY = averageY(keypoints, LEFT_SHOULDER, RIGHT_SHOULDER)
        val wristY = averageY(keypoints, LEFT_WRIST, RIGHT_WRIST)
        return wristY in (shoulderY - 0.05f)..(shoulderY + 0.25f)
    }

    private fun isPressing(keypoints: FloatArray): Boolean {
        val shoulderY = averageY(keypoints, LEFT_SHOULDER, RIGHT_SHOULDER)
        val wristY = averageY(keypoints, LEFT_WRIST, RIGHT_WRIST)
        return wristY > shoulderY + 0.25f
    }

    private fun isLockout(keypoints: FloatArray): Boolean {
        val noseY = keypoints[NOSE * 3 + 1]
        val wristY = averageY(keypoints, LEFT_WRIST, RIGHT_WRIST)
        return wristY > noseY + 0.05f
    }

    private fun averageY(keypoints: FloatArray, first: Int, second: Int): Float {
        return (keypoints[first * 3 + 1] + keypoints[second * 3 + 1]) / 2f
    }

    private fun buildResult(
        timestamp: Long,
        feedback: ExerciseFeedback?,
        accuracy: Float
    ): ExerciseAnalysisResult {
        return ExerciseAnalysisResult(
            count = count,
            phase = state.toExercisePhase(),
            isReady = isReady,
            feedback = feedback,
            accuracy = accuracy,
            elapsedTime = timestamp
        )
    }

    private fun ShoulderPressState.toExercisePhase(): ExercisePhase {
        return when (this) {
            ShoulderPressState.PREPARING -> ExercisePhase("shoulder_press_preparing", "Ready", ExercisePhaseTone.NEUTRAL)
            ShoulderPressState.RACK -> ExercisePhase("shoulder_press_rack", "Rack Position", ExercisePhaseTone.WARNING)
            ShoulderPressState.PRESSING -> ExercisePhase("shoulder_press_pressing", "Pressing", ExercisePhaseTone.ACTIVE)
            ShoulderPressState.LOCKOUT -> ExercisePhase("shoulder_press_lockout", "Lockout", ExercisePhaseTone.SUCCESS)
        }
    }

    private companion object {
        const val NOSE = 0
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_WRIST = 15
        const val RIGHT_WRIST = 16
    }
}
