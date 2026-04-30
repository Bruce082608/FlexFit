package com.example.flexfit.data.llm

import com.example.flexfit.BuildConfig
import com.example.flexfit.data.model.LlmAnalysisRequest
import com.example.flexfit.data.model.LlmMessage
import com.example.flexfit.data.model.LlmResponseFormat

/**
 * Builds the prompt for post-workout LLM analysis.
 * Only sends training statistics and key errors — no video data.
 */
object LlmPromptBuilder {

    fun buildRequest(
        workoutStats: LlmWorkoutStats,
        responseLanguage: LlmResponseLanguage = LlmResponseLanguage.ENGLISH
    ): LlmAnalysisRequest {
        return LlmAnalysisRequest(
            model = BuildConfig.LLM_MODEL.ifBlank { "deepseek-chat" },
            messages = listOf(
                LlmMessage(
                    role = "system",
                    content = buildSystemPrompt(responseLanguage)
                ),
                LlmMessage(
                    role = "user",
                    content = buildUserContent(workoutStats, responseLanguage)
                )
            ),
            temperature = 0.7,
            max_tokens = 1200,
            response_format = buildResponseFormat()
        )
    }

    private fun buildUserContent(
        stats: LlmWorkoutStats,
        responseLanguage: LlmResponseLanguage
    ): String {
        return """
You are a professional fitness coach analyzing a workout session.
Write the entire analysis in ${responseLanguage.promptName}. You may translate exercise names naturally.

Workout Summary:
- Exercise: ${stats.exerciseType}
- Duration: ${formatDuration(stats.durationSeconds)}
- Total Reps: ${stats.totalReps}
- Completed Reps: ${stats.completedReps}
- Success Rate: ${stats.successRate}%
- Average Accuracy: ${stats.averageAccuracy}%
- Depth Score: ${stats.depthScore}%
- Alignment Score: ${stats.alignmentScore}%
- Stability Score: ${stats.stabilityScore}%
- Calories Burned: ${stats.caloriesBurned}
- Errors: ${stats.errorsCount}
- Warnings: ${stats.warningsCount}
${stats.keyErrors.takeIf { it.isNotEmpty() }?.let { "Key Form Errors Detected:\n${it.joinToString("\n") { e -> "  - $e" }}"} ?: ""}
${stats.topIssues.takeIf { it.isNotEmpty() }?.let { "\nTop Issues to Address:\n${it.joinToString("\n") { i -> "  - $i" }}"} ?: ""}
"""
    }

    private fun formatDuration(seconds: Long): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "${min}m ${sec}s"
    }

    private fun buildResponseFormat() = LlmResponseFormat(type = "json_object")

    private fun buildSystemPrompt(responseLanguage: LlmResponseLanguage) = """
You are a certified fitness coach providing post-workout analysis.
Be encouraging but specific. Focus on actionable advice.
Language requirement:
- Write every JSON string value in ${responseLanguage.promptName}.
- Keep the JSON keys exactly as shown below.
Respond ONLY with valid JSON matching this exact shape:
{
  "summary": "Overall performance evaluation in 1-2 sentences",
  "strengths": ["What the user did well"],
  "weaknesses": ["Areas that need improvement"],
  "recommendations": ["Actionable suggestions for next workout"]
}
Do not include any text outside the JSON response.
"""
}

enum class LlmResponseLanguage(
    val cacheKey: String,
    val promptName: String
) {
    ENGLISH("en", "English"),
    CHINESE("zh", "Simplified Chinese")
}

data class LlmWorkoutStats(
    val exerciseType: String,
    val durationSeconds: Long,
    val totalReps: Int,
    val completedReps: Int,
    val successRate: Float,
    val averageAccuracy: Float,
    val depthScore: Float,
    val alignmentScore: Float,
    val stabilityScore: Float,
    val caloriesBurned: Float,
    val errorsCount: Int,
    val warningsCount: Int,
    val keyErrors: List<String> = emptyList(),
    val topIssues: List<String> = emptyList()
)
