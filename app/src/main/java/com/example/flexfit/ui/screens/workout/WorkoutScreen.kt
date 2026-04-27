package com.example.flexfit.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flexfit.data.model.ExerciseType
import com.example.flexfit.ui.theme.AccentPurple
import com.example.flexfit.ui.theme.DeepPurple
import com.example.flexfit.ui.theme.LightBackground
import com.example.flexfit.ui.theme.LightPurple
import com.example.flexfit.ui.theme.SurfaceLight
import com.example.flexfit.ui.theme.TextPrimary
import com.example.flexfit.ui.theme.TextSecondary
import com.example.flexfit.ui.theme.TextTertiary

@Composable
fun WorkoutScreen(
    onOpenPullUpSelection: () -> Unit,
    onStartShoulderPress: (String) -> Unit
) {
    var selectedExercise by remember { mutableStateOf<ExerciseType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Workout",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose an exercise and training mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        ExerciseType.entries.forEach { exercise ->
            ExerciseSelectionCard(
                exercise = exercise,
                isSelected = selectedExercise == exercise,
                onClick = {
                    when (exercise) {
                        ExerciseType.PULL_UP -> onOpenPullUpSelection()
                        ExerciseType.SHOULDER_PRESS -> {
                            selectedExercise = if (selectedExercise == exercise) null else exercise
                        }
                        else -> Unit
                    }
                },
                onStartCamera = { onStartShoulderPress("camera") },
                onStartVideo = { onStartShoulderPress("video") }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExerciseSelectionCard(
    exercise: ExerciseType,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStartCamera: () -> Unit,
    onStartVideo: () -> Unit
) {
    val isAvailable = exercise.isImplemented
    val cardColor = if (isAvailable) SurfaceLight else SurfaceLight.copy(alpha = 0.68f)
    val accentColor = when {
        !isAvailable -> TextTertiary
        isSelected -> AccentPurple
        else -> DeepPurple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAvailable, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = if (isAvailable) LightPurple else LightPurple.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAvailable) Icons.Default.FitnessCenter else Icons.Default.Lock,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = exercise.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAvailable) TextPrimary else TextTertiary
                        )
                        StatusLabel(isAvailable = isAvailable)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = exercise.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAvailable) TextSecondary else TextTertiary
                    )
                }
            }

            if (exercise == ExerciseType.PULL_UP) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select grip width before choosing camera or video mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (exercise == ExerciseType.SHOULDER_PRESS && isSelected) {
                Spacer(modifier = Modifier.height(16.dp))
                TrainingModeButtons(
                    onStartCamera = onStartCamera,
                    onStartVideo = onStartVideo
                )
            }
        }
    }
}

@Composable
private fun StatusLabel(isAvailable: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isAvailable) AccentPurple.copy(alpha = 0.12f) else TextTertiary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isAvailable) "Available" else "Coming soon",
            style = MaterialTheme.typography.labelSmall,
            color = if (isAvailable) AccentPurple else TextTertiary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrainingModeButtons(
    onStartCamera: () -> Unit,
    onStartVideo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartCamera,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Camera")
        }

        OutlinedButton(
            onClick = onStartVideo,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Video")
        }
    }
}
