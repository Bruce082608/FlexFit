package com.example.flexfit.data.llm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flexfit.BuildConfig
import com.example.flexfit.data.model.AnalysisSource
import com.example.flexfit.data.model.WorkoutAnalysisResult
import com.example.flexfit.data.remote.LlmApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private val Context.llmCacheDataStore by preferencesDataStore(name = "llm_cache")

sealed class LlmAnalysisState {
    data object Idle : LlmAnalysisState()
    data object Loading : LlmAnalysisState()
    data class Success(val result: WorkoutAnalysisResult) : LlmAnalysisState()
    data class Error(val message: String, val fallbackResult: WorkoutAnalysisResult?) : LlmAnalysisState()
}

class LlmAnalysisRepository private constructor(private val context: Context) {

    private val apiService = LlmApiServiceHolder.service
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _analysisState = MutableStateFlow<LlmAnalysisState>(LlmAnalysisState.Idle)
    val analysisState: StateFlow<LlmAnalysisState> = _analysisState.asStateFlow()

    private val cacheDataStore: DataStore<Preferences> by lazy {
        context.applicationContext.llmCacheDataStore
    }

    companion object {
        @Volatile
        private var instance: LlmAnalysisRepository? = null

        fun getInstance(context: Context): LlmAnalysisRepository {
            return instance ?: synchronized(this) {
                instance ?: LlmAnalysisRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun requestAnalysis(
        stats: LlmWorkoutStats,
        responseLanguage: LlmResponseLanguage = LlmResponseLanguage.ENGLISH
    ) {
        _analysisState.value = LlmAnalysisState.Loading

        cacheScope.launch {
            val apiKey = BuildConfig.LLM_API_KEY

            if (apiKey.isBlank() || apiKey in placeholderApiKeys) {
                val fallback = generateLocalAnalysis(stats, responseLanguage)
                _analysisState.value = LlmAnalysisState.Success(fallback)
                return@launch
            }

            try {
                val request = LlmPromptBuilder.buildRequest(stats, responseLanguage)
                val auth = "Bearer $apiKey"
                val response = apiService.chatCompletion(auth, request)

                if (response.isSuccessful && response.body() != null) {
                    val content = response.body()!!.choices.firstOrNull()?.message?.content
                    if (content != null) {
                        val parsed = LlmAnalysisInterpreter.parseStructuredResponse(content)
                        if (parsed != null) {
                            cacheResult(stats.exerciseType, responseLanguage, content)
                            _analysisState.value = LlmAnalysisState.Success(
                                parsed.copy(source = AnalysisSource.LLM)
                            )
                        } else {
                            val fallback = generateLocalAnalysis(stats, responseLanguage)
                            _analysisState.value = LlmAnalysisState.Error(
                                "Failed to parse LLM response",
                                fallback
                            )
                        }
                    } else {
                        val fallback = generateLocalAnalysis(stats, responseLanguage)
                        _analysisState.value = LlmAnalysisState.Error(
                            "Empty response from LLM",
                            fallback
                        )
                    }
                } else {
                    val fallback = generateLocalAnalysis(stats, responseLanguage)
                    val errorMsg = "API error: ${response.code()} ${response.message()}"
                    _analysisState.value = LlmAnalysisState.Error(errorMsg, fallback)
                }
            } catch (e: Exception) {
                val fallback = generateLocalAnalysis(stats, responseLanguage)
                _analysisState.value = LlmAnalysisState.Error(
                    "Network error: ${e.localizedMessage ?: "Unknown error"}",
                    fallback
                )
            }
        }
    }

    fun retryLastAnalysis(
        stats: LlmWorkoutStats,
        responseLanguage: LlmResponseLanguage = LlmResponseLanguage.ENGLISH
    ) {
        requestAnalysis(stats, responseLanguage)
    }

    fun clearState() {
        _analysisState.value = LlmAnalysisState.Idle
    }

    private suspend fun generateLocalAnalysis(
        stats: LlmWorkoutStats,
        responseLanguage: LlmResponseLanguage
    ): WorkoutAnalysisResult {
        val cached = loadCachedAnalysis(stats.exerciseType, responseLanguage)
        if (cached != null) {
            return cached
        }

        return LlmAnalysisInterpreter.localFallback(stats, responseLanguage)
    }

    private suspend fun cacheResult(
        exerciseType: String,
        responseLanguage: LlmResponseLanguage,
        jsonContent: String
    ) {
        cacheScope.launch {
            cacheDataStore.edit { prefs ->
                prefs[stringPreferencesKey("analysis_${exerciseType}_${responseLanguage.cacheKey}")] = jsonContent
                prefs[longPreferencesKey("analysis_${exerciseType}_${responseLanguage.cacheKey}_time")] = System.currentTimeMillis()
            }
        }
    }

    private suspend fun loadCachedAnalysis(
        exerciseType: String,
        responseLanguage: LlmResponseLanguage
    ): WorkoutAnalysisResult? {
        val prefs = cacheDataStore.data.first()
        val cached = prefs[stringPreferencesKey("analysis_${exerciseType}_${responseLanguage.cacheKey}")]
        val cachedTime = prefs[longPreferencesKey("analysis_${exerciseType}_${responseLanguage.cacheKey}_time")] ?: 0L

        if (cached == null) return null

        val oneDayMs = 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - cachedTime > oneDayMs) return null

        return LlmAnalysisInterpreter.parseStructuredResponse(cached)
            ?.copy(source = AnalysisSource.LOCAL_FALLBACK)
    }
}

private object LlmApiServiceHolder {
    private const val DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com/"

    private fun ensureTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
    val service: LlmApiService by lazy {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(BuildConfig.LLM_BASE_URL.ifBlank { DEFAULT_DEEPSEEK_BASE_URL }))
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        retrofit.create(LlmApiService::class.java)
    }
}

private val placeholderApiKeys = setOf(
    "your_api_key_here",
    "your_openai_api_key_here",
    "your_deepseek_api_key_here"
)
