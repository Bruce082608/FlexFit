package com.example.flexfit.ui.screens.training

import androidx.compose.runtime.Composable
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.ui.screens.workout.WorkoutResultDialog

@Composable
fun TrainingResultDialog(
    result: WorkoutResult,
    onDismiss: () -> Unit,
    onSaveAndClose: () -> Unit
) {
    WorkoutResultDialog(
        result = result,
        onDismiss = onDismiss,
        onSaveAndClose = onSaveAndClose
    )
}

