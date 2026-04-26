package com.example.flexfit.data.repository

import com.example.flexfit.data.model.PerformanceLevel
import com.example.flexfit.data.model.WorkoutRecord
import com.example.flexfit.data.model.WorkoutResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing workout records.
 * Uses in-memory storage for now, can be extended to use Room/SQLite for persistence.
 */
object WorkoutRecordRepository {

    private val _workoutRecords = MutableStateFlow<List<WorkoutRecord>>(emptyList())
    val workoutRecords: StateFlow<List<WorkoutRecord>> = _workoutRecords.asStateFlow()

    // Statistics
    private val _totalWorkouts = MutableStateFlow(0)
    val totalWorkouts: StateFlow<Int> = _totalWorkouts.asStateFlow()

    private val _totalMinutes = MutableStateFlow(0L)
    val totalMinutes: StateFlow<Long> = _totalMinutes.asStateFlow()

    private val _averageAccuracy = MutableStateFlow(0f)
    val averageAccuracy: StateFlow<Float> = _averageAccuracy.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    init {
        // Load sample data for demonstration
        loadSampleData()
    }

    fun addWorkoutResult(result: WorkoutResult) {
        val record = WorkoutRecord(
            id = result.id,
            exerciseType = result.exerciseType,
            date = result.date,
            durationSeconds = result.durationSeconds,
            totalReps = result.totalReps,
            completedReps = result.completedReps,
            averageAccuracy = result.averageAccuracy,
            depthScore = result.depthScore,
            alignmentScore = result.alignmentScore,
            stabilityScore = result.stabilityScore,
            caloriesBurned = result.caloriesBurned,
            errorsCount = result.errorsCount,
            warningsCount = result.warningsCount
        )

        val currentList = _workoutRecords.value.toMutableList()
        currentList.add(0, record) // Add to beginning (newest first)
        _workoutRecords.value = currentList

        updateStatistics()
    }

    fun getWorkoutRecords(): List<WorkoutRecord> = _workoutRecords.value

    fun getRecentRecords(count: Int = 7): List<WorkoutRecord> {
        return _workoutRecords.value.take(count)
    }

    fun getRecordsByExercise(exerciseType: String): List<WorkoutRecord> {
        return _workoutRecords.value.filter { it.exerciseType == exerciseType }
    }

    fun clearAllRecords() {
        _workoutRecords.value = emptyList()
        updateStatistics()
    }

    private fun updateStatistics() {
        val records = _workoutRecords.value

        _totalWorkouts.value = records.size
        _totalMinutes.value = records.sumOf { it.durationSeconds } / 60

        _averageAccuracy.value = if (records.isNotEmpty()) {
            records.map { it.averageAccuracy }.average().toFloat()
        } else 0f

        // Calculate streak (simplified - counts consecutive days with workouts)
        _currentStreak.value = calculateStreak(records)
    }

    private fun calculateStreak(records: List<WorkoutRecord>): Int {
        if (records.isEmpty()) return 0

        val sortedRecords = records.sortedByDescending { it.date }
        val today = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        var streak = 0
        var expectedDay = today

        for (record in sortedRecords) {
            val recordDay = record.date / oneDayMs
            val expectedDayNum = expectedDay / oneDayMs

            when {
                recordDay == expectedDayNum -> {
                    streak++
                    expectedDay -= oneDayMs
                }
                recordDay == expectedDayNum - 1 -> {
                    expectedDay = record.date
                    streak++
                    expectedDay -= oneDayMs
                }
                else -> break
            }
        }

        return streak
    }

    // Sample data for demonstration
    private fun loadSampleData() {
        val sampleRecords = listOf(
            WorkoutRecord(
                exerciseType = "Pull Up",
                date = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000,
                durationSeconds = 1200,
                totalReps = 15,
                completedReps = 12,
                averageAccuracy = 85f,
                caloriesBurned = 120f,
                errorsCount = 3,
                warningsCount = 5
            ),
            WorkoutRecord(
                exerciseType = "Pull Up",
                date = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                durationSeconds = 1080,
                totalReps = 12,
                completedReps = 10,
                averageAccuracy = 82f,
                caloriesBurned = 105f,
                errorsCount = 2,
                warningsCount = 4
            ),
            WorkoutRecord(
                exerciseType = "Shoulder Press",
                date = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000,
                durationSeconds = 900,
                totalReps = 20,
                completedReps = 18,
                averageAccuracy = 88f,
                caloriesBurned = 95f,
                errorsCount = 2,
                warningsCount = 3
            )
        )

        _workoutRecords.value = sampleRecords
        updateStatistics()
    }
}
