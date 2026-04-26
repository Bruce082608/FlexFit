package com.example.flexfit.ui.screens.training

enum class TrainingMode {
    CAMERA,
    VIDEO;

    companion object {
        fun fromRouteValue(value: String): TrainingMode {
            return when (value.lowercase()) {
                "video" -> VIDEO
                else -> CAMERA
            }
        }
    }
}

