package com.example.flexfit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flexfit.data.model.LlmAnalysisData
import com.example.flexfit.data.model.WorkoutRecord
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.data.model.WorkoutStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.workoutRecordDataStore by preferencesDataStore(name = "workout_records")

/**
 * Repository for persisted workout records.
 *
 * DataStore keeps the MVP storage small and dependency-light while still
 * surviving app restarts.
 */
object WorkoutRecordRepository {
    private val gson = Gson()
    private val recordListType: Type = object : TypeToken<List<WorkoutRecord>>() {}.type
    private val recordsJsonKey = stringPreferencesKey("records_json")
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var dataStore: DataStore<Preferences>? = null
    private var initialized = false

    private val _workoutRecords = MutableStateFlow<List<WorkoutRecord>>(emptyList())
    val workoutRecords: StateFlow<List<WorkoutRecord>> = _workoutRecords.asStateFlow()

    private val _totalWorkouts = MutableStateFlow(0)
    val totalWorkouts: StateFlow<Int> = _totalWorkouts.asStateFlow()

    private val _totalMinutes = MutableStateFlow(0L)
    val totalMinutes: StateFlow<Long> = _totalMinutes.asStateFlow()

    private val _averageAccuracy = MutableStateFlow(0f)
    val averageAccuracy: StateFlow<Float> = _averageAccuracy.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    fun initialize(context: Context) {
        if (initialized) return

        initialized = true
        val store = context.applicationContext.workoutRecordDataStore
        dataStore = store

        repositoryScope.launch {
            store.data
                .map { preferences -> decodeRecordsJson(preferences[recordsJsonKey]) }
                .catch { emit(emptyList()) }
                .collect { records -> replaceRecords(records) }
        }
    }

    /**
     * Add a new workout result to the repository.
     * @param result The workout result to save
     * @param llmAnalysis Optional LLM analysis data to include with the record
     */
    fun addWorkoutResult(result: WorkoutResult, llmAnalysis: LlmAnalysisData? = null) {
        val record = result.toRecord(llmAnalysis)
        replaceRecords(upsertRecord(_workoutRecords.value, record))

        repositoryScope.launch {
            dataStore?.edit { preferences ->
                val storedRecords = decodeRecordsJson(preferences[recordsJsonKey])
                preferences[recordsJsonKey] = encodeRecordsJson(upsertRecord(storedRecords, record))
            }
        }
    }

    /**
     * Update an existing workout record with LLM analysis.
     * Call this after the user requests LLM analysis on a saved workout.
     */
    fun updateWorkoutWithLlmAnalysis(workoutId: String, llmAnalysis: LlmAnalysisData) {
        val currentRecords = _workoutRecords.value
        val existingRecord = currentRecords.find { it.id == workoutId }

        if (existingRecord != null) {
            val updatedRecord = existingRecord.copy(llmAnalysis = llmAnalysis)
            replaceRecords(updateRecord(currentRecords, updatedRecord))

            repositoryScope.launch {
                dataStore?.edit { preferences ->
                    val storedRecords = decodeRecordsJson(preferences[recordsJsonKey])
                    val storedRecord = storedRecords.find { it.id == workoutId }
                    if (storedRecord != null) {
                        val storedUpdated = storedRecord.copy(llmAnalysis = llmAnalysis)
                        preferences[recordsJsonKey] = encodeRecordsJson(
                            upsertRecord(storedRecords, storedUpdated)
                        )
                    }
                }
            }
        }
    }

    /**
     * Get a workout record by its ID.
     */
    fun getWorkoutById(id: String): WorkoutRecord? {
        return _workoutRecords.value.find { it.id == id }
    }

    fun getWorkoutRecords(): List<WorkoutRecord> = _workoutRecords.value

    fun getRecentRecords(count: Int = 7): List<WorkoutRecord> {
        return _workoutRecords.value.take(count)
    }

    fun getRecordsByExercise(exerciseType: String): List<WorkoutRecord> {
        return _workoutRecords.value.filter { it.exerciseType == exerciseType }
    }

    /**
     * Get workout records that have LLM analysis.
     */
    fun getRecordsWithLlmAnalysis(): List<WorkoutRecord> {
        return _workoutRecords.value.filter { it.llmAnalysis != null }
    }

    /**
     * Get workout records that do not have LLM analysis yet.
     */
    fun getRecordsWithoutLlmAnalysis(): List<WorkoutRecord> {
        return _workoutRecords.value.filter { it.llmAnalysis == null }
    }

    fun clearAllRecords() {
        replaceRecords(emptyList())

        repositoryScope.launch {
            dataStore?.edit { preferences ->
                preferences.remove(recordsJsonKey)
            }
        }
    }

    fun calculateStats(
        records: List<WorkoutRecord>,
        nowMillis: Long = System.currentTimeMillis()
    ): WorkoutStats {
        if (records.isEmpty()) return WorkoutStats()

        return WorkoutStats(
            totalWorkouts = records.size,
            totalMinutes = records.sumOf { it.durationSeconds } / 60L,
            averageAccuracy = records.map { it.averageAccuracy }.average().toFloat(),
            currentStreak = calculateStreak(records, nowMillis)
        )
    }

    internal fun encodeRecordsJson(records: List<WorkoutRecord>): String {
        return gson.toJson(records.sortedByDescending { it.date })
    }

    internal fun decodeRecordsJson(json: String?): List<WorkoutRecord> {
        if (json.isNullOrBlank()) return emptyList()

        return runCatching {
            val decoded = gson.fromJson<List<WorkoutRecord>>(json, recordListType)
            decoded.orEmpty().sortedByDescending { it.date }
        }.getOrElse { emptyList() }
    }

    private fun replaceRecords(records: List<WorkoutRecord>) {
        val sortedRecords = records.sortedByDescending { it.date }
        val stats = calculateStats(sortedRecords)

        _workoutRecords.value = sortedRecords
        _totalWorkouts.value = stats.totalWorkouts
        _totalMinutes.value = stats.totalMinutes
        _averageAccuracy.value = stats.averageAccuracy
        _currentStreak.value = stats.currentStreak
    }

    private fun upsertRecord(
        records: List<WorkoutRecord>,
        record: WorkoutRecord
    ): List<WorkoutRecord> {
        return (listOf(record) + records.filterNot { it.id == record.id })
            .sortedByDescending { it.date }
    }

    private fun updateRecord(
        records: List<WorkoutRecord>,
        updatedRecord: WorkoutRecord
    ): List<WorkoutRecord> {
        return records.map { if (it.id == updatedRecord.id) updatedRecord else it }
            .sortedByDescending { it.date }
    }

    private fun WorkoutResult.toRecord(llmAnalysis: LlmAnalysisData? = null): WorkoutRecord {
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

    private fun calculateStreak(
        records: List<WorkoutRecord>,
        nowMillis: Long
    ): Int {
        val workoutDays = records.map { it.date / ONE_DAY_MS }.toSet()
        if (workoutDays.isEmpty()) return 0

        val today = nowMillis / ONE_DAY_MS
        var cursor = when {
            today in workoutDays -> today
            today - 1 in workoutDays -> today - 1
            else -> return 0
        }

        var streak = 0
        while (cursor in workoutDays) {
            streak++
            cursor--
        }
        return streak
    }

    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
}
