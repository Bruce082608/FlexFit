package com.example.flexfit.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents the result of a completed workout session.
 */
data class WorkoutResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseType: String,
    val date: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val totalReps: Int = 0,
    val completedReps: Int = 0,
    val averageAccuracy: Float = 0f,
    val caloriesBurned: Float = 0f,
    val errorsCount: Int = 0,
    val warningsCount: Int = 0,
    val feedbackMessages: List<String> = emptyList()
) {
    val successRate: Float
        get() = if (totalReps > 0) completedReps.toFloat() / totalReps * 100 else 0f

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(date))
        }

    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val performanceLevel: PerformanceLevel
        get() = when {
            averageAccuracy >= 90 && successRate >= 90 -> PerformanceLevel.EXCELLENT
            averageAccuracy >= 75 && successRate >= 75 -> PerformanceLevel.GOOD
            averageAccuracy >= 60 -> PerformanceLevel.AVERAGE
            else -> PerformanceLevel.NEEDS_IMPROVEMENT
        }
}

enum class PerformanceLevel(val displayName: String, val emoji: String) {
    EXCELLENT("Excellent", "🏆"),
    GOOD("Good", "👍"),
    AVERAGE("Average", "💪"),
    NEEDS_IMPROVEMENT("Needs Improvement", "📈")
}

/**
 * Represents a workout record stored for history tracking.
 */
data class WorkoutRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseType: String,
    val date: Long,
    val durationSeconds: Long,
    val totalReps: Int,
    val completedReps: Int,
    val averageAccuracy: Float,
    val caloriesBurned: Float,
    val errorsCount: Int,
    val warningsCount: Int
) {
    fun toWorkoutResult(): WorkoutResult = WorkoutResult(
        id = id,
        exerciseType = exerciseType,
        date = date,
        durationSeconds = durationSeconds,
        totalReps = totalReps,
        completedReps = completedReps,
        averageAccuracy = averageAccuracy,
        caloriesBurned = caloriesBurned,
        errorsCount = errorsCount,
        warningsCount = warningsCount
    )
}
