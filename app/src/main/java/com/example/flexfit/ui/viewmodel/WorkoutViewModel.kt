package com.example.flexfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexfit.algorithm.ExerciseAnalyzerFactory
import com.example.flexfit.data.model.DetectedPose
import com.example.flexfit.data.model.ExerciseType
import com.example.flexfit.data.model.FeedbackMessage
import com.example.flexfit.data.model.FeedbackType
import com.example.flexfit.data.model.PoseEvaluationResult
import com.example.flexfit.data.model.RepEvaluation
import com.example.flexfit.data.model.TrainingSession
import com.example.flexfit.data.model.WorkoutState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class WorkoutUiState(
    val workoutState: WorkoutState = WorkoutState.IDLE,
    val selectedExercise: ExerciseType = ExerciseType.PULL_UP,
    val currentPose: DetectedPose? = null,
    val currentAccuracy: Float = 0f,
    val repCount: Int = 0,
    val feedback: List<FeedbackMessage> = emptyList(),
    val currentSession: TrainingSession? = null,
    val repEvaluations: List<RepEvaluation> = emptyList()
)

class WorkoutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var exerciseAnalyzer = ExerciseAnalyzerFactory.getAnalyzer(ExerciseType.PULL_UP)

    fun selectExercise(exercise: ExerciseType) {
        if (exercise.isImplemented) {
            _uiState.value = _uiState.value.copy(selectedExercise = exercise)
            exerciseAnalyzer = ExerciseAnalyzerFactory.getAnalyzer(exercise)
                ?: throw IllegalArgumentException("Analyzer not implemented")
        }
    }

    fun startWorkout() {
        val session = TrainingSession(
            exerciseType = _uiState.value.selectedExercise
        )
        _uiState.value = _uiState.value.copy(
            workoutState = WorkoutState.ACTIVE,
            currentSession = session,
            repCount = 0,
            repEvaluations = emptyList()
        )
    }

    fun pauseWorkout() {
        _uiState.value = _uiState.value.copy(workoutState = WorkoutState.PAUSED)
    }

    fun resumeWorkout() {
        _uiState.value = _uiState.value.copy(workoutState = WorkoutState.ACTIVE)
    }

    fun endWorkout() {
        val currentSession = _uiState.value.currentSession?.copy(
            endTime = System.currentTimeMillis(),
            totalReps = _uiState.value.repCount,
            goodReps = _uiState.value.repEvaluations.count { it.isGoodRep },
            averageAccuracy = calculateAverageAccuracy(),
            repEvaluations = _uiState.value.repEvaluations
        )

        _uiState.value = _uiState.value.copy(
            workoutState = WorkoutState.COMPLETED,
            currentSession = currentSession
        )
    }

    fun processPoseUpdate(pose: DetectedPose) {
        if (_uiState.value.workoutState != WorkoutState.ACTIVE) return

        _uiState.value = _uiState.value.copy(currentPose = pose)

        // Analyze pose using the exercise analyzer
        // TODO: Replace with actual algorithm when available
        analyzeCurrentPose(pose)
    }

    private fun analyzeCurrentPose(pose: DetectedPose) {
        // Placeholder analysis - to be replaced with actual algorithm
        // TODO: Integrate with MediaPipePoseDetector and ExerciseAnalyzer

        val result = exerciseAnalyzer?.analyze(pose)

        result?.let {
            _uiState.value = _uiState.value.copy(
                currentAccuracy = it.accuracy,
                feedback = it.feedback
            )

            // Check for rep completion (placeholder logic)
            checkRepCompletion(it)
        }
    }

    private fun checkRepCompletion(evaluation: PoseEvaluationResult) {
        // TODO: Implement actual rep detection logic
        // This is a placeholder that simulates rep counting

        if (evaluation.isGoodRep && _uiState.value.repCount < 100) {
            // Simulate rep detection
            // In real implementation, this would be based on pose phase detection
        }
    }

    private fun calculateAverageAccuracy(): Float {
        val evaluations = _uiState.value.repEvaluations
        return if (evaluations.isEmpty()) {
            _uiState.value.currentAccuracy
        } else {
            evaluations.map { it.accuracy }.average().toFloat()
        }
    }

    fun resetWorkout() {
        _uiState.value = WorkoutUiState()
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up resources
    }
}
