package com.example.flexfit.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit Client Configuration
 *
 * This object provides the Retrofit instance for API calls.
 * Configure the BASE_URL with your actual LLM API endpoint.
 *
 * TODO: Replace BASE_URL with your actual LLM API endpoint
 */
object RetrofitClient {

    // TODO: Replace with your actual LLM API base URL
    private const val BASE_URL = "https://api.your-llm-service.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                // TODO: Add your API authentication header here
                // .header("Authorization", "Bearer YOUR_API_KEY")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val llmApiService: LlmApiService = retrofit.create(LlmApiService::class.java)
}

/**
 * API Result wrapper
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

/**
 * Repository for LLM API calls
 */
class LlmRepository {

    private val apiService = RetrofitClient.llmApiService

    suspend fun analyzeTraining(
        session: com.example.flexfit.data.model.TrainingSession,
        repEvaluations: List<com.example.flexfit.data.model.RepEvaluation>,
        userProfile: com.example.flexfit.data.model.UserProfile
    ): ApiResult<TrainingAnalysisResponse> {
        return try {
            val request = TrainingAnalysisRequest(
                sessionData = SessionData(
                    sessionId = session.id,
                    exerciseType = session.exerciseType.name,
                    startTime = session.startTime,
                    endTime = session.endTime ?: System.currentTimeMillis(),
                    totalReps = session.totalReps,
                    goodReps = session.goodReps,
                    durationMinutes = session.durationMinutes.toInt(),
                    averageAccuracy = session.averageAccuracy
                ),
                repEvaluations = repEvaluations.map { rep ->
                    RepEvaluationData(
                        repNumber = rep.repNumber,
                        accuracy = rep.accuracy,
                        depthScore = rep.depth,
                        alignmentScore = rep.alignment,
                        stabilityScore = rep.stability,
                        feedback = rep.feedback.map { it.message }
                    )
                },
                userProfile = UserProfileData(
                    name = userProfile.name,
                    height = userProfile.height,
                    weight = userProfile.weight,
                    fitnessGoal = userProfile.fitnessGoal
                )
            )

            val response = apiService.getTrainingAnalysis(request)

            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(
                    message = response.message() ?: "Unknown error",
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Network error")
        }
    }

    /**
     * Placeholder method for realtime suggestions
     * TODO: Implement with actual LLM API call
     */
    suspend fun getRealtimeSuggestion(
        poseData: PoseData,
        exerciseType: String,
        repCount: Int,
        currentAccuracy: Float
    ): ApiResult<RealtimeSuggestionResponse> {
        // Placeholder: Return simulated response
        // TODO: Implement actual API call when LLM endpoint is ready
        return ApiResult.Success(
            RealtimeSuggestionResponse(
                suggestion = "Keep your form steady",
                priority = "medium",
                focusArea = "stability"
            )
        )
    }
}
