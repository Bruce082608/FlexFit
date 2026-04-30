package com.example.flexfit.data.model

data class BodyProportions(
    val coefficients: List<Float>,
    val sourceUri: String?,
    val updatedAtMillis: Long
) {
    val leftEarShoulderCoefficient: Float?
        get() = coefficients.getOrNull(16)

    val rightEarShoulderCoefficient: Float?
        get() = coefficients.getOrNull(17)

    val isComplete: Boolean
        get() = coefficients.size == COEFFICIENT_COUNT

    companion object {
        const val COEFFICIENT_COUNT = 22
    }
}
