package com.example.flexfit.api

import com.example.flexfit.data.model.TrainingSession
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * LLM API Service Interface
 *
 * This interface defines the API endpoints for LLM-based training analysis.
 * The actual implementation will connect to your LLM service provider.
 *
 * TODO: Configure the base URL and authentication for your LLM API
 */
interface LlmApiService {

    /**
     * Get personalized training analysis from LLM
     *
     * @param session The training session data to analyze
     * @param userId The user ID for personalized analysis
     * @return LLM-generated analysis response
     */
    @POST("api/v1/training/analyze")
    suspend fun getTrainingAnalysis(
        @Body request: TrainingAnalysisRequest
    ): Response<TrainingAnalysisResponse>

    /**
     * Get real-time suggestions during workout
     *
     * @param poseData Current pose data for real-time feedback
     * @param exerciseType The type of exercise being performed
     * @return Real-time suggestion from LLM
     */
    @POST("api/v1/training/suggestion")
    suspend fun getRealtimeSuggestion(
        @Body request: RealtimeSuggestionRequest
    ): Response<RealtimeSuggestionResponse>

    /**
     * Get exercise recommendations based on user history
     *
     * @param userId The user ID
     * @param targetMuscles Target muscle groups
     * @return Recommended exercises
     */
    @GET("api/v1/training/recommendations")
    suspend fun getExerciseRecommendations(
        @Query("user_id") userId: String,
        @Query("target_muscles") targetMuscles: String
    ): Response<ExerciseRecommendationsResponse>
}

/**
 * Request body for training analysis
 */
data class TrainingAnalysisRequest(
    val sessionData: SessionData,
    val repEvaluations: List<RepEvaluationData>,
    val userProfile: UserProfileData
)

data class SessionData(
    val sessionId: String,
    val exerciseType: String,
    val startTime: Long,
    val endTime: Long,
    val totalReps: Int,
    val goodReps: Int,
    val durationMinutes: Int,
    val averageAccuracy: Float
)

data class RepEvaluationData(
    val repNumber: Int,
    val accuracy: Float,
    val depthScore: Float,
    val alignmentScore: Float,
    val stabilityScore: Float,
    val feedback: List<String>
)

data class UserProfileData(
    val name: String,
    val height: Float,
    val weight: Float,
    val fitnessGoal: String,
    val experienceLevel: String = "intermediate"
)

/**
 * Response body for training analysis
 */
data class TrainingAnalysisResponse(
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>,
    val improvementTips: List<String>
)

/**
 * Request body for realtime suggestions
 */
data class RealtimeSuggestionRequest(
    val currentPose: PoseData,
    val exerciseType: String,
    val repCount: Int,
    val currentAccuracy: Float
)

data class PoseData(
    val landmarks: List<LandmarkData>,
    val timestamp: Long
)

data class LandmarkData(
    val index: Int,
    val name: String,
    val x: Float,
    val y: Float,
    val visibility: Float
)

/**
 * Response body for realtime suggestions
 */
data class RealtimeSuggestionResponse(
    val suggestion: String,
    val priority: String,  // "high", "medium", "low"
    val focusArea: String
)

/**
 * Response body for exercise recommendations
 */
data class ExerciseRecommendationsResponse(
    val recommendations: List<ExerciseRecommendation>
)

data class ExerciseRecommendation(
    val exerciseName: String,
    val targetMuscles: List<String>,
    val difficulty: String,
    val reason: String
)
