package com.example.flexfit.data.llm

import com.example.flexfit.data.model.AnalysisSource
import com.example.flexfit.data.model.WorkoutAnalysisResult
import com.google.gson.Gson
import com.google.gson.JsonObject

internal object LlmAnalysisInterpreter {
    private val gson = Gson()

    fun parseStructuredResponse(content: String): WorkoutAnalysisResult? {
        return runCatching {
            val json = gson.fromJson(extractJsonObject(content), JsonObject::class.java)
            val summary = json.get("summary")?.asString.orEmpty()
            WorkoutAnalysisResult(
                summary = summary,
                strengths = json.getAsJsonArray("strengths")?.map { it.asString } ?: emptyList(),
                weaknesses = json.getAsJsonArray("weaknesses")?.map { it.asString } ?: emptyList(),
                recommendations = json.getAsJsonArray("recommendations")?.map { it.asString } ?: emptyList(),
                source = AnalysisSource.LLM
            ).takeIf {
                it.summary.isNotBlank() ||
                    it.strengths.isNotEmpty() ||
                    it.weaknesses.isNotEmpty() ||
                    it.recommendations.isNotEmpty()
            }
        }.getOrNull()
    }

    fun localFallback(stats: LlmWorkoutStats): WorkoutAnalysisResult {
        val accuracy = stats.averageAccuracy
        val successRate = stats.successRate
        val errors = stats.errorsCount
        val warnings = stats.warningsCount

        val summary = when {
            accuracy >= 85 && errors <= 2 -> "Great workout! Your form was consistent throughout."
            accuracy >= 70 -> "Good effort! There is room to improve on your form."
            accuracy >= 50 -> "Keep practicing! Focus on the key areas identified below."
            else -> "Every rep counts. Build up your strength and form gradually."
        }

        val strengths = buildList {
            if (successRate >= 85) add("High rep completion rate at $successRate%")
            if (errors <= 2) add("Minimal form errors detected")
            if (stats.stabilityScore >= 80) add("Strong core stability throughout the movement")
            if (accuracy >= 75) add("Consistent movement quality")
            if (isEmpty()) add("Determination and effort throughout the session")
        }

        val weaknesses = buildList {
            if (accuracy < 75) add("Movement accuracy below target range")
            if (errors > 5) add("Multiple form errors were detected - focus on control")
            if (stats.alignmentScore < 70) add("Body alignment needs improvement")
            if (stats.stabilityScore < 70) add("Stability could be improved with core work")
            if (warnings > 10) add("Too many compensatory movements detected")
            if (isEmpty()) add("Minor adjustments needed for optimal form")
        }

        val recommendations = buildList {
            add("Warm up for 5 minutes before starting")
            if (accuracy < 75) add("Focus on controlled, full-range repetitions")
            if (stats.alignmentScore < 70) add("Practice in front of a mirror to check alignment")
            if (stats.stabilityScore < 70) add("Add planks and core exercises to your routine")
            if (errors > 5) add("Reduce speed to prioritize form over reps")
            add("Stay consistent - regular practice leads to improvement")
        }

        return WorkoutAnalysisResult(
            summary = summary,
            strengths = strengths,
            weaknesses = weaknesses,
            recommendations = recommendations,
            source = AnalysisSource.LOCAL_FALLBACK
        )
    }

    private fun extractJsonObject(content: String): String {
        val trimmed = content.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }
}
