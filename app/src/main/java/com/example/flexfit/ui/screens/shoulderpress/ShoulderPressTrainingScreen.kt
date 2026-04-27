package com.example.flexfit.ui.screens.shoulderpress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.flexfit.ml.ShoulderPressAnalyzer
import com.example.flexfit.ui.screens.training.TrainingMode
import com.example.flexfit.ui.screens.training.TrainingScreen

@Composable
fun ShoulderPressTrainingScreen(
    mode: String,
    onNavigateBack: () -> Unit
) {
    val analyzer = remember { ShoulderPressAnalyzer() }

    TrainingScreen(
        analyzer = analyzer,
        initialMode = TrainingMode.fromRouteValue(mode),
        onNavigateBack = onNavigateBack
    )
}
