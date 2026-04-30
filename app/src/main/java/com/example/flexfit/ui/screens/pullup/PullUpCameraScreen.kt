package com.example.flexfit.ui.screens.pullup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.flexfit.data.repository.BodyCalibrationRepository
import com.example.flexfit.ml.PullUpAnalyzer
import com.example.flexfit.ml.PullUpType
import com.example.flexfit.ui.screens.training.TrainingMode
import com.example.flexfit.ui.screens.training.TrainingScreen

@Composable
fun PullUpCameraScreen(
    exerciseType: String,
    mode: String,
    onNavigateBack: () -> Unit
) {
    val pullUpType = remember(exerciseType) {
        when (exerciseType.lowercase()) {
            "wide" -> PullUpType.WIDE
            "narrow" -> PullUpType.NARROW
            else -> PullUpType.NORMAL
        }
    }
    val bodyProportions by BodyCalibrationRepository.bodyProportions.collectAsState()
    val analyzer = remember(pullUpType, bodyProportions) {
        PullUpAnalyzer(pullUpType, bodyProportions)
    }

    TrainingScreen(
        analyzer = analyzer,
        initialMode = TrainingMode.fromRouteValue(mode),
        onNavigateBack = onNavigateBack
    )
}
