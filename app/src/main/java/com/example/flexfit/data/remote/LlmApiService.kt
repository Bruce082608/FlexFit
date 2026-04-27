package com.example.flexfit.data.remote

import com.example.flexfit.data.model.LlmAnalysisRequest
import com.example.flexfit.data.model.LlmAnalysisResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LlmApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: LlmAnalysisRequest
    ): Response<LlmAnalysisResponse>
}
