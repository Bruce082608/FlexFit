package com.example.flexfit.ui.screens.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.flexfit.ml.PullUpAnalyzer
import com.example.flexfit.ml.PullUpType
import com.example.flexfit.ui.screens.training.TrainingMode
import com.example.flexfit.ui.screens.training.TrainingScreen

@Composable
fun WorkoutScreen() {
    val analyzer = remember { PullUpAnalyzer(PullUpType.NORMAL) }

    TrainingScreen(
        analyzer = analyzer,
        initialMode = TrainingMode.CAMERA,
        onNavigateBack = {}
    )
}
