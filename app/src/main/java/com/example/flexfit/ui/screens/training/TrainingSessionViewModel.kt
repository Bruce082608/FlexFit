package com.example.flexfit.ui.screens.training

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.flexfit.data.llm.LlmAnalysisRepository
import com.example.flexfit.data.llm.LlmAnalysisState
import com.example.flexfit.data.llm.LlmResponseLanguage
import com.example.flexfit.data.llm.LlmWorkoutStats
import com.example.flexfit.data.model.LlmAnalysisData
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.data.repository.AppPreferencesRepository
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ml.ExerciseAnalysisResult
import com.example.flexfit.ml.ExerciseIssue
import com.example.flexfit.ml.FeedbackType
import com.example.flexfit.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingSessionUiState(
    val result: ExerciseAnalysisResult = ExerciseAnalysisResult(),
    val isWorkoutActive: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTime: Long = 0L,
    val lastKeypoints: FloatArray? = null,
    val lastLandmarkConfidences: FloatArray? = null,
    val feedbackMessage: String? = null,
    val feedbackType: FeedbackType = FeedbackType.INFO,
    val showResultDialog: Boolean = false,
    val workoutResult: WorkoutResult? = null,
    val llmAnalysisState: LlmAnalysisState = LlmAnalysisState.Idle,
    val totalErrors: Int = 0,
    val totalWarnings: Int = 0,
    val accuracySum: Float = 0f,
    val depthScoreSum: Float = 0f,
    val alignmentScoreSum: Float = 0f,
    val stabilityScoreSum: Float = 0f,
    val accuracyCount: Int = 0,
    val issueCounts: Map<String, Int> = emptyMap(),
    val issueLabels: Map<String, String> = emptyMap(),
    val issueSuggestions: Map<String, String> = emptyMap(),
    // Video analysis state
    val isVideoMode: Boolean = false,
    val videoAnalysisProgress: Float = 0f,     // 0..1
    val videoFramesAnalyzed: Int = 0,
    val videoTotalFrames: Int = 0,
    val videoCurrentTimeMs: Long = 0L,
    val videoAnalysisStatus: String = "Ready",
    val isAnalyzingVideo: Boolean = false,
    val videoMetadata: VideoAnalysisMetadata? = null
)

data class VideoAnalysisMetadata(
    val durationMs: Long,
    val frameRate: Float,
    val videoWidth: Int,
    val videoHeight: Int,
    val estimatedFrames: Long
)

class TrainingSessionViewModel(
    private val llmRepository: LlmAnalysisRepository
) : ViewModel() {
    private var pendingSavedWorkoutId: String? = null

    private val _uiState = MutableStateFlow(TrainingSessionUiState())
    val uiState: StateFlow<TrainingSessionUiState> = _uiState.asStateFlow()

    // Individual StateFlows to avoid stale closure in CameraTrainingView
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActiveFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isWorkoutActive

    private val _isPaused = MutableStateFlow(false)
    val isPausedFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPaused

    init {
        viewModelScope.launch {
            llmRepository.analysisState.collect { state ->
                syncSavedWorkoutAnalysis(state)
                _uiState.update { it.copy(llmAnalysisState = state) }
            }
        }
    }

    fun startWorkout() {
        _isWorkoutActive.value = true
        _isPaused.value = false
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
        val newPaused = !_isPaused.value
        _isPaused.value = newPaused
        _uiState.update { it.copy(isPaused = newPaused) }
    }

    fun setPlaybackActive(playing: Boolean) {
        val isActive = playing || _uiState.value.isWorkoutActive
        val isPaused = !playing
        _isWorkoutActive.value = isActive
        _isPaused.value = isPaused
        _uiState.update {
            it.copy(
                isWorkoutActive = isActive,
                isPaused = isPaused
            )
        }
    }

    fun setVideoMode(enabled: Boolean) {
        _uiState.update { it.copy(isVideoMode = enabled) }
    }

    fun setVideoMetadata(
        durationMs: Long,
        frameRate: Float,
        width: Int,
        height: Int,
        estimatedFrames: Long
    ) {
        _uiState.update {
            it.copy(
                videoMetadata = VideoAnalysisMetadata(
                    durationMs = durationMs,
                    frameRate = frameRate,
                    videoWidth = width,
                    videoHeight = height,
                    estimatedFrames = estimatedFrames
                )
            )
        }
    }

    fun updateVideoAnalysisProgress(
        framesAnalyzed: Int,
        totalFrames: Int,
        currentTimeMs: Long,
        progress: Float = -1f,
        status: String = "Analyzing video"
    ) {
        val safeTotalFrames = totalFrames.coerceAtLeast(framesAnalyzed).coerceAtLeast(1)
        val safeFramesAnalyzed = framesAnalyzed.coerceIn(0, safeTotalFrames)
        val safeProgress = if (progress >= 0f) {
            progress
        } else {
            safeFramesAnalyzed / safeTotalFrames.toFloat()
        }.coerceIn(0f, 1f)

        _uiState.update {
            it.copy(
                isAnalyzingVideo = true,
                videoFramesAnalyzed = safeFramesAnalyzed,
                videoTotalFrames = safeTotalFrames,
                videoCurrentTimeMs = currentTimeMs.coerceAtLeast(0L),
                videoAnalysisProgress = safeProgress,
                videoAnalysisStatus = status
            )
        }
    }

    fun setAnalyzingVideo(analyzing: Boolean) {
        _uiState.update { it.copy(isAnalyzingVideo = analyzing) }
    }

    fun resetVideoState() {
        _uiState.update {
            it.copy(
                isVideoMode = false,
                isAnalyzingVideo = false,
                videoAnalysisProgress = 0f,
                videoFramesAnalyzed = 0,
                videoTotalFrames = 0,
                videoCurrentTimeMs = 0L,
                videoAnalysisStatus = "Ready",
                videoMetadata = null
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

    fun recordAnalysis(
        result: ExerciseAnalysisResult,
        keypoints: FloatArray?,
        landmarkConfidences: FloatArray? = null
    ) {
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
                lastLandmarkConfidences = landmarkConfidences,
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

    fun showFeedback(message: String, type: FeedbackType) {
        _uiState.update { it.copy(feedbackMessage = message, feedbackType = type) }
    }

    fun resetSession() {
        _isWorkoutActive.value = false
        _isPaused.value = false
        _uiState.value = TrainingSessionUiState()
    }

    fun resetSessionKeepsVideo() {
        _isWorkoutActive.value = false
        _isPaused.value = false
        _uiState.update {
            it.copy(
                result = ExerciseAnalysisResult(),
                isWorkoutActive = false,
                isPaused = false,
                elapsedTime = 0L,
                lastKeypoints = null,
                lastLandmarkConfidences = null,
                feedbackMessage = null,
                feedbackType = FeedbackType.INFO,
                showResultDialog = false,
                workoutResult = null,
                llmAnalysisState = LlmAnalysisState.Idle,
                totalErrors = 0,
                totalWarnings = 0,
                accuracySum = 0f,
                depthScoreSum = 0f,
                alignmentScoreSum = 0f,
                stabilityScoreSum = 0f,
                accuracyCount = 0,
                issueCounts = emptyMap(),
                issueLabels = emptyMap(),
                issueSuggestions = emptyMap(),
                isAnalyzingVideo = false,
                videoAnalysisProgress = 0f,
                videoFramesAnalyzed = 0,
                videoTotalFrames = 0,
                videoCurrentTimeMs = 0L,
                videoAnalysisStatus = "Ready"
            )
        }
    }

    fun endWorkout(exerciseName: String) {
        endWorkoutInternal(exerciseName, null)
    }

    fun endWorkoutWithVideoDuration(exerciseName: String, videoDurationMs: Long) {
        endWorkoutInternal(exerciseName, videoDurationMs / 1000L)
    }

    private fun endWorkoutInternal(exerciseName: String, overrideDurationSeconds: Long?) {
        _isWorkoutActive.value = false
        _isPaused.value = false
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

            // Use video duration in seconds if provided (video analysis),
            // otherwise use elapsed time (live camera recording)
            val durationSeconds = overrideDurationSeconds ?: state.elapsedTime

            state.copy(
                isWorkoutActive = false,
                isPaused = false,
                showResultDialog = true,
                workoutResult = WorkoutResult(
                    exerciseType = exerciseName,
                    durationSeconds = durationSeconds,
                    totalReps = totalReps,
                    completedReps = completedReps,
                    averageAccuracy = averageAccuracy,
                    depthScore = averageDepth,
                    alignmentScore = averageAlignment,
                    stabilityScore = averageStability,
                    caloriesBurned = durationSeconds / 60f * 8f,
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
        _isWorkoutActive.value = false
        _isPaused.value = false
        _uiState.value = TrainingSessionUiState()
    }

    fun requestLlmAnalysis(
        result: WorkoutResult,
        appLanguage: AppLanguage = AppPreferencesRepository.language.value
    ) {
        val stats = LlmWorkoutStats(
            exerciseType = result.exerciseType,
            durationSeconds = result.durationSeconds,
            totalReps = result.totalReps,
            completedReps = result.completedReps,
            successRate = result.successRate,
            averageAccuracy = result.averageAccuracy,
            depthScore = result.depthScore,
            alignmentScore = result.alignmentScore,
            stabilityScore = result.stabilityScore,
            caloriesBurned = result.caloriesBurned,
            errorsCount = result.errorsCount,
            warningsCount = result.warningsCount,
            keyErrors = result.mainIssues,
            topIssues = result.improvementSuggestions
        )
        llmRepository.requestAnalysis(stats, appLanguage.toLlmResponseLanguage())
    }

    fun retryLlmAnalysis(
        result: WorkoutResult,
        appLanguage: AppLanguage = AppPreferencesRepository.language.value
    ) {
        requestLlmAnalysis(result, appLanguage)
    }

    fun saveWorkoutResult(result: WorkoutResult) {
        val analysisData = _uiState.value.llmAnalysisState.toLlmAnalysisDataOrNull()
        WorkoutRecordRepository.addWorkoutResult(result, analysisData)

        if (analysisData == null && _uiState.value.llmAnalysisState is LlmAnalysisState.Loading) {
            pendingSavedWorkoutId = result.id
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmRepository.clearState()
    }

    private fun syncSavedWorkoutAnalysis(state: LlmAnalysisState) {
        val analysisData = state.toLlmAnalysisDataOrNull() ?: return
        val targetId = _uiState.value.workoutResult
            ?.id
            ?.takeIf { WorkoutRecordRepository.getWorkoutById(it) != null }
            ?: pendingSavedWorkoutId
            ?: return

        if (WorkoutRecordRepository.getWorkoutById(targetId) != null) {
            WorkoutRecordRepository.updateWorkoutWithLlmAnalysis(targetId, analysisData)
            if (pendingSavedWorkoutId == targetId) {
                pendingSavedWorkoutId = null
            }
        }
    }

    private fun LlmAnalysisState.toLlmAnalysisDataOrNull(): LlmAnalysisData? {
        return when (this) {
            is LlmAnalysisState.Success -> LlmAnalysisData.fromWorkoutAnalysisResult(result)
            is LlmAnalysisState.Error -> fallbackResult?.let {
                LlmAnalysisData.fromWorkoutAnalysisResult(it)
            }
            LlmAnalysisState.Idle,
            LlmAnalysisState.Loading -> null
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = LlmAnalysisRepository.getInstance(context)
            return TrainingSessionViewModel(repository) as T
        }
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

private fun AppLanguage.toLlmResponseLanguage(): LlmResponseLanguage {
    return when (this) {
        AppLanguage.ENGLISH -> LlmResponseLanguage.ENGLISH
        AppLanguage.CHINESE -> LlmResponseLanguage.CHINESE
    }
}
