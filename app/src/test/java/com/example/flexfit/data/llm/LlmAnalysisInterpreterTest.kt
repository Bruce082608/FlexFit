package com.example.flexfit.data.llm

import com.example.flexfit.data.model.AnalysisSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmAnalysisInterpreterTest {

    @Test
    fun parseStructuredResponse_parsesDeepSeekJsonObject() {
        val result = LlmAnalysisInterpreter.parseStructuredResponse(
            """
            {
              "summary": "Solid session with controlled reps.",
              "strengths": ["Stable torso"],
              "weaknesses": ["Pull height faded late"],
              "recommendations": ["Rest longer between sets"]
            }
            """.trimIndent()
        )

        assertNotNull(result)
        requireNotNull(result)
        assertEquals("Solid session with controlled reps.", result.summary)
        assertEquals(listOf("Stable torso"), result.strengths)
        assertEquals(listOf("Pull height faded late"), result.weaknesses)
        assertEquals(listOf("Rest longer between sets"), result.recommendations)
        assertEquals(AnalysisSource.LLM, result.source)
    }

    @Test
    fun parseStructuredResponse_acceptsMarkdownFencedJson() {
        val result = LlmAnalysisInterpreter.parseStructuredResponse(
            """
            ```json
            {
              "summary": "Good effort with minor stability issues.",
              "strengths": ["Consistent tempo"],
              "weaknesses": ["Body swing"],
              "recommendations": ["Brace before each rep"]
            }
            ```
            """.trimIndent()
        )

        assertNotNull(result)
        requireNotNull(result)
        assertEquals("Good effort with minor stability issues.", result.summary)
        assertEquals("Consistent tempo", result.strengths.first())
        assertEquals("Body swing", result.weaknesses.first())
        assertEquals("Brace before each rep", result.recommendations.first())
    }

    @Test
    fun emptyResponse_canFallBackToLocalAnalysis() {
        assertNull(LlmAnalysisInterpreter.parseStructuredResponse(""))

        val fallback = LlmAnalysisInterpreter.localFallback(sampleStats())

        assertEquals(AnalysisSource.LOCAL_FALLBACK, fallback.source)
        assertTrue(fallback.summary.isNotBlank())
        assertTrue(fallback.recommendations.isNotEmpty())
    }

    @Test
    fun localFallback_hasNonEmptyCoachContent() {
        val fallback = LlmAnalysisInterpreter.localFallback(
            sampleStats(
                averageAccuracy = 92f,
                successRate = 100f,
                errorsCount = 0,
                warningsCount = 0
            )
        )

        assertTrue(fallback.summary.isNotBlank())
        assertTrue(fallback.strengths.isNotEmpty())
        assertTrue(fallback.weaknesses.isNotEmpty())
        assertTrue(fallback.recommendations.isNotEmpty())
    }

    private fun sampleStats(
        averageAccuracy: Float = 62f,
        successRate: Float = 50f,
        errorsCount: Int = 4,
        warningsCount: Int = 8
    ): LlmWorkoutStats {
        return LlmWorkoutStats(
            exerciseType = "Normal Grip Pull-up",
            durationSeconds = 90,
            totalReps = 4,
            completedReps = 2,
            successRate = successRate,
            averageAccuracy = averageAccuracy,
            depthScore = 58f,
            alignmentScore = 64f,
            stabilityScore = 61f,
            caloriesBurned = 12f,
            errorsCount = errorsCount,
            warningsCount = warningsCount,
            keyErrors = listOf("Pull higher before counting"),
            topIssues = listOf("Use full range of motion")
        )
    }
}
