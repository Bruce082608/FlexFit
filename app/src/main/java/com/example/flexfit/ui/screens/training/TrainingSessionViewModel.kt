package com.example.flexfit.ui.screens.training

import androidx.lifecycle.ViewModel
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.ml.ExerciseAnalysisResult
import com.example.flexfit.ml.ExerciseIssue
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
    val depthScoreSum: Float = 0f,
    val alignmentScoreSum: Float = 0f,
    val stabilityScoreSum: Float = 0f,
    val accuracyCount: Int = 0,
    val issueCounts: Map<String, Int> = emptyMap(),
    val issueLabels: Map<String, String> = emptyMap(),
    val issueSuggestions: Map<String, String> = emptyMap()
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
                depthScoreSum = 0f,
                alignmentScoreSum = 0f,
                stabilityScoreSum = 0f,
                accuracyCount = 0,
                issueCounts = emptyMap(),
                issueLabels = emptyMap(),
                issueSuggestions = emptyMap()
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
            val shouldTrack = state.isWorkoutActive && result.isReady && result.accuracy > 0f
            val feedback = result.feedback
            val issueSummary = mergeIssues(
                issueCounts = state.issueCounts,
                issueLabels = state.issueLabels,
                issueSuggestions = state.issueSuggestions,
                issues = if (state.isWorkoutActive) result.issues else emptyList()
            )

            state.copy(
                result = result,
                lastKeypoints = keypoints,
                feedbackMessage = feedback?.message ?: state.feedbackMessage,
                feedbackType = feedback?.type ?: state.feedbackType,
                totalErrors = state.totalErrors + if (state.isWorkoutActive) {
                    result.issues.count { it.severity == FeedbackType.ERROR }
                } else {
                    0
                },
                totalWarnings = state.totalWarnings + if (state.isWorkoutActive) {
                    result.issues.count { it.severity == FeedbackType.WARNING }
                } else {
                    0
                },
                accuracySum = state.accuracySum + if (shouldTrack) result.accuracy else 0f,
                depthScoreSum = state.depthScoreSum + if (shouldTrack) result.scores.depth else 0f,
                alignmentScoreSum = state.alignmentScoreSum + if (shouldTrack) result.scores.alignment else 0f,
                stabilityScoreSum = state.stabilityScoreSum + if (shouldTrack) result.scores.stability else 0f,
                accuracyCount = state.accuracyCount + if (shouldTrack) 1 else 0,
                issueCounts = issueSummary.counts,
                issueLabels = issueSummary.labels,
                issueSuggestions = issueSummary.suggestions
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
            val averageDepth = averageScore(state.depthScoreSum, state.accuracyCount)
            val averageAlignment = averageScore(state.alignmentScoreSum, state.accuracyCount)
            val averageStability = averageScore(state.stabilityScoreSum, state.accuracyCount)

            val completedReps = state.result.count
            val totalReps = state.result.attemptedReps.coerceAtLeast(completedReps)
            val rankedIssues = state.issueCounts.entries.sortedByDescending { it.value }.take(3)
            val mainIssues = rankedIssues.map { state.issueLabels[it.key] ?: it.key }
            val suggestions = rankedIssues.mapNotNull { state.issueSuggestions[it.key] }.distinct()

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
                    depthScore = averageDepth,
                    alignmentScore = averageAlignment,
                    stabilityScore = averageStability,
                    caloriesBurned = state.elapsedTime / 60f * 8f,
                    errorsCount = state.totalErrors,
                    warningsCount = state.totalWarnings,
                    mainIssues = mainIssues.ifEmpty { listOf("No major issues detected") },
                    improvementSuggestions = suggestions.ifEmpty {
                        listOf("Keep the same tempo and full range of motion.")
                    },
                    feedbackMessages = buildFeedbackMessages(
                        averageAccuracy,
                        state.totalErrors,
                        state.totalWarnings,
                        mainIssues,
                        suggestions
                    )
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
        totalWarnings: Int,
        mainIssues: List<String>,
        suggestions: List<String>
    ): List<String> {
        val summary = if (averageAccuracy >= 85f) "Great form!" else "Keep practicing!"
        val issueSummary = when {
            totalErrors == 0 && totalWarnings == 0 -> "No major form issues detected."
            mainIssues.isNotEmpty() -> "Main focus: ${mainIssues.joinToString(", ")}."
            else -> "Watch your form on the next set."
        }
        val suggestionSummary = suggestions.firstOrNull() ?: "Keep the same tempo and full range of motion."
        return listOf(summary, issueSummary, suggestionSummary)
    }

    private fun averageScore(sum: Float, count: Int): Float {
        return if (count > 0) sum / count else 0f
    }

    private fun mergeIssues(
        issueCounts: Map<String, Int>,
        issueLabels: Map<String, String>,
        issueSuggestions: Map<String, String>,
        issues: List<ExerciseIssue>
    ): IssueSummary {
        if (issues.isEmpty()) {
            return IssueSummary(issueCounts, issueLabels, issueSuggestions)
        }

        val counts = issueCounts.toMutableMap()
        val labels = issueLabels.toMutableMap()
        val suggestions = issueSuggestions.toMutableMap()

        issues.forEach { issue ->
            counts[issue.key] = (counts[issue.key] ?: 0) + 1
            labels[issue.key] = issue.label
            suggestions[issue.key] = issue.suggestion
        }

        return IssueSummary(counts, labels, suggestions)
    }

    private data class IssueSummary(
        val counts: Map<String, Int>,
        val labels: Map<String, String>,
        val suggestions: Map<String, String>
    )
}
