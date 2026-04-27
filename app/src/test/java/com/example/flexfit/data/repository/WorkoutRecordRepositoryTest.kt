package com.example.flexfit.data.repository

import com.example.flexfit.data.model.AnalysisSource
import com.example.flexfit.data.model.LlmAnalysisData
import com.example.flexfit.data.model.WorkoutAnalysisResult
import com.example.flexfit.data.model.WorkoutRecord
import com.example.flexfit.data.model.WorkoutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutRecordRepositoryTest {

    @Test
    fun decodeRecordsJson_returnsEmptyListForMissingOrBrokenJson() {
        assertTrue(WorkoutRecordRepository.decodeRecordsJson(null).isEmpty())
        assertTrue(WorkoutRecordRepository.decodeRecordsJson("").isEmpty())
        assertTrue(WorkoutRecordRepository.decodeRecordsJson("{broken").isEmpty())
    }

    @Test
    fun encodeAndDecodeRecordsJson_preservesWorkoutRecordFields() {
        val record = workoutRecord(
            id = "record-1",
            date = ONE_DAY_MS * 5,
            depthScore = 91f,
            alignmentScore = 82f,
            stabilityScore = 73f,
            mainIssues = listOf("Body swinging detected"),
            improvementSuggestions = listOf("Brace your core before pulling."),
            llmAnalysis = llmAnalysis()
        )

        val decoded = WorkoutRecordRepository.decodeRecordsJson(
            WorkoutRecordRepository.encodeRecordsJson(listOf(record))
        )

        assertEquals(1, decoded.size)
        assertEquals(record.id, decoded.first().id)
        assertEquals(record.depthScore, decoded.first().depthScore, 0.001f)
        assertEquals(record.alignmentScore, decoded.first().alignmentScore, 0.001f)
        assertEquals(record.stabilityScore, decoded.first().stabilityScore, 0.001f)
        assertEquals(record.mainIssues, decoded.first().mainIssues)
        assertEquals(record.improvementSuggestions, decoded.first().improvementSuggestions)
        assertEquals(record.llmAnalysis?.summary, decoded.first().llmAnalysis?.summary)
        assertEquals(record.llmAnalysis?.recommendations, decoded.first().llmAnalysis?.recommendations)
    }

    @Test
    fun calculateStats_handlesEmptyRecords() {
        val stats = WorkoutRecordRepository.calculateStats(emptyList(), nowMillis = ONE_DAY_MS * 10)

        assertEquals(0, stats.totalWorkouts)
        assertEquals(0L, stats.totalMinutes)
        assertEquals(0f, stats.averageAccuracy, 0.001f)
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun calculateStats_countsSameDayOnceForStreakAndKeepsWorkoutTotals() {
        val now = ONE_DAY_MS * 10 + 12_000L
        val records = listOf(
            workoutRecord(id = "today-1", date = ONE_DAY_MS * 10 + 1_000L, durationSeconds = 120, averageAccuracy = 80f),
            workoutRecord(id = "today-2", date = ONE_DAY_MS * 10 + 2_000L, durationSeconds = 180, averageAccuracy = 90f),
            workoutRecord(id = "yesterday", date = ONE_DAY_MS * 9 + 1_000L, durationSeconds = 60, averageAccuracy = 70f),
            workoutRecord(id = "two-days-ago", date = ONE_DAY_MS * 8 + 1_000L, durationSeconds = 240, averageAccuracy = 100f),
            workoutRecord(id = "gap", date = ONE_DAY_MS * 6 + 1_000L, durationSeconds = 60, averageAccuracy = 60f)
        )

        val stats = WorkoutRecordRepository.calculateStats(records, nowMillis = now)

        assertEquals(5, stats.totalWorkouts)
        assertEquals(11L, stats.totalMinutes)
        assertEquals(80f, stats.averageAccuracy, 0.001f)
        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun calculateStats_returnsZeroStreakWhenLatestWorkoutIsOlderThanYesterday() {
        val now = ONE_DAY_MS * 10 + 12_000L
        val records = listOf(
            workoutRecord(id = "old", date = ONE_DAY_MS * 8 + 1_000L)
        )

        val stats = WorkoutRecordRepository.calculateStats(records, nowMillis = now)

        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun workoutRecordToWorkoutResult_preservesScoreBreakdownAndAdvice() {
        val record = workoutRecord(
            depthScore = 88f,
            alignmentScore = 77f,
            stabilityScore = 66f,
            mainIssues = listOf("Pull higher before counting"),
            improvementSuggestions = listOf("Pull until your chin clearly passes the bar line.")
        )

        val result = record.toWorkoutResult()

        assertEquals(88f, result.depthScore, 0.001f)
        assertEquals(77f, result.alignmentScore, 0.001f)
        assertEquals(66f, result.stabilityScore, 0.001f)
        assertEquals(record.mainIssues, result.mainIssues)
        assertEquals(record.improvementSuggestions, result.improvementSuggestions)
    }

    @Test
    fun updateWorkoutWithLlmAnalysis_updatesSavedRecord() {
        WorkoutRecordRepository.clearAllRecords()
        val result = WorkoutResult(
            id = "saved-record",
            exerciseType = "Normal Grip Pull-up",
            durationSeconds = 90,
            totalReps = 3,
            completedReps = 2,
            averageAccuracy = 72f,
            caloriesBurned = 12f
        )

        WorkoutRecordRepository.addWorkoutResult(result)
        WorkoutRecordRepository.updateWorkoutWithLlmAnalysis(
            workoutId = result.id,
            llmAnalysis = llmAnalysis(summary = "Updated AI analysis")
        )

        val updated = WorkoutRecordRepository.getWorkoutById(result.id)
        assertEquals("Updated AI analysis", updated?.llmAnalysis?.summary)
    }

    private fun workoutRecord(
        id: String = "record",
        exerciseType: String = "Normal Grip Pull-up",
        date: Long = ONE_DAY_MS,
        durationSeconds: Long = 120,
        totalReps: Int = 1,
        completedReps: Int = 1,
        averageAccuracy: Float = 85f,
        depthScore: Float = averageAccuracy,
        alignmentScore: Float = averageAccuracy,
        stabilityScore: Float = averageAccuracy,
        caloriesBurned: Float = 16f,
        errorsCount: Int = 0,
        warningsCount: Int = 0,
        mainIssues: List<String> = emptyList(),
        improvementSuggestions: List<String> = emptyList(),
        llmAnalysis: LlmAnalysisData? = null
    ): WorkoutRecord {
        return WorkoutRecord(
            id = id,
            exerciseType = exerciseType,
            date = date,
            durationSeconds = durationSeconds,
            totalReps = totalReps,
            completedReps = completedReps,
            averageAccuracy = averageAccuracy,
            depthScore = depthScore,
            alignmentScore = alignmentScore,
            stabilityScore = stabilityScore,
            caloriesBurned = caloriesBurned,
            errorsCount = errorsCount,
            warningsCount = warningsCount,
            mainIssues = mainIssues,
            improvementSuggestions = improvementSuggestions,
            llmAnalysis = llmAnalysis
        )
    }

    private fun llmAnalysis(summary: String = "Personalized AI analysis"): LlmAnalysisData {
        return LlmAnalysisData.fromWorkoutAnalysisResult(
            WorkoutAnalysisResult(
                summary = summary,
                strengths = listOf("Stable tempo"),
                weaknesses = listOf("Limited range"),
                recommendations = listOf("Use full range of motion"),
                source = AnalysisSource.LLM
            )
        )
    }

    private companion object {
        const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    }
}
