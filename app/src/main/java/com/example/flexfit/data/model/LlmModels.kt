package com.example.flexfit.data.model

data class LlmAnalysisRequest(
    val model: String = "deepseek-chat",
    val messages: List<LlmMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1200,
    val response_format: LlmResponseFormat? = null
)

data class LlmMessage(
    val role: String,
    val content: String
)

data class LlmResponseFormat(
    val type: String = "json_object",
    val json_schema: LlmJsonSchema? = null
)

data class LlmJsonSchema(
    val name: String,
    val strict: Boolean = true,
    val schema: LlmSchema
)

data class LlmSchema(
    val type: String = "object",
    val required: List<String> = listOf("summary", "strengths", "weaknesses", "recommendations"),
    val properties: Map<String, LlmProperty>
)

data class LlmProperty(
    val type: String,
    val description: String,
    val items: LlmItems? = null,
    val enum: List<String>? = null
)

data class LlmItems(
    val type: String = "string",
    val description: String? = null
)
