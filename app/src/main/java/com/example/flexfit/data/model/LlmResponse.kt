package com.example.flexfit.data.model

data class LlmAnalysisResponse(
    val id: String,
    val choices: List<LlmChoice>
)

data class LlmChoice(
    val message: LlmResponseMessage
)

data class LlmResponseMessage(
    val content: String
)

/**
 * Structured result from LLM analysis.
 * Always returned so callers can use it whether the source is LLM or local fallback.
 */
data class WorkoutAnalysisResult(
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>,
    val source: AnalysisSource
)

enum class AnalysisSource {
    LLM,
    LOCAL_FALLBACK
}
