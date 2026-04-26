package com.example.flexfit.ui.screens.training

import androidx.lifecycle.ViewModel
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.ml.ExerciseAnalysisResult
import com.example.flexfit.ml.FeedbackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TrainingSessionUiState(
    val result: ExerciseAnalysisResult = ExerciseAnalysisResult(),
    val isWorkoutActive: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTime: Long = 0L,
    val lastKeypoints: FloatArray? = null,
    val feedbackMessage: String? = null,
    val feedbackType: FeedbackType = FeedbackType.INFO,
    val showResultDialog: Boolean = false,
    val workoutResult: WorkoutResult? = null,
    val totalErrors: Int = 0,
    val totalWarnings: Int = 0,
    val accuracySum: Float = 0f,
    val accuracyCount: Int = 0
)

class TrainingSessionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingSessionUiState())
    val uiState: StateFlow<TrainingSessionUiState> = _uiState.asStateFlow()

    fun startWorkout() {
        _uiState.update {
            it.copy(
                isWorkoutActive = true,
                isPaused = false,
                totalErrors = 0,
                totalWarnings = 0,
                accuracySum = 0f,
                accuracyCount = 0
            )
        }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun setPlaybackActive(playing: Boolean) {
        _uiState.update {
            it.copy(
                isWorkoutActive = playing || it.isWorkoutActive,
                isPaused = !playing
            )
        }
    }

    fun tickElapsedSecond() {
        _uiState.update {
            if (it.isWorkoutActive && !it.isPaused) {
                it.copy(elapsedTime = it.elapsedTime + 1)
            } else {
                it
            }
        }
    }

    fun recordAnalysis(result: ExerciseAnalysisResult, keypoints: FloatArray?) {
        _uiState.update { state ->
            val shouldTrack = state.isWorkoutActive && result.accuracy > 0f
            val feedback = result.feedback

            state.copy(
                result = result,
                lastKeypoints = keypoints,
                feedbackMessage = feedback?.message ?: state.feedbackMessage,
                feedbackType = feedback?.type ?: state.feedbackType,
                totalErrors = state.totalErrors + if (shouldTrack && feedback?.type == FeedbackType.ERROR) 1 else 0,
                totalWarnings = state.totalWarnings + if (shouldTrack && feedback?.type == FeedbackType.WARNING) 1 else 0,
                accuracySum = state.accuracySum + if (shouldTrack) result.accuracy else 0f,
                accuracyCount = state.accuracyCount + if (shouldTrack) 1 else 0
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun resetSession() {
        _uiState.value = TrainingSessionUiState()
    }

    fun endWorkout(exerciseName: String) {
        _uiState.update { state ->
            val averageAccuracy = if (state.accuracyCount > 0) {
                state.accuracySum / state.accuracyCount
            } else {
                0f
            }

            val completedReps = state.result.count
            val totalReps = completedReps.coerceAtLeast(0)

            state.copy(
                isWorkoutActive = false,
                isPaused = false,
                showResultDialog = true,
                workoutResult = WorkoutResult(
                    exerciseType = exerciseName,
                    durationSeconds = state.elapsedTime,
                    totalReps = totalReps,
                    completedReps = completedReps,
                    averageAccuracy = averageAccuracy,
                    caloriesBurned = state.elapsedTime / 60f * 8f,
                    errorsCount = state.totalErrors,
                    warningsCount = state.totalWarnings,
                    feedbackMessages = buildFeedbackMessages(averageAccuracy, state.totalErrors, state.totalWarnings)
                )
            )
        }
    }

    fun closeResultAndReset() {
        _uiState.value = TrainingSessionUiState()
    }

    private fun buildFeedbackMessages(
        averageAccuracy: Float,
        totalErrors: Int,
        totalWarnings: Int
    ): List<String> {
        return listOf(
            if (averageAccuracy >= 85f) "Great form!" else "Keep practicing!",
            if (totalErrors == 0) "No errors!" else "Watch your form",
            if (totalWarnings <= 3) "Good stability!" else "Try to stay more stable"
        )
    }
}
