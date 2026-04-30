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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flexfit.data.model.ExerciseType
import com.example.flexfit.ui.i18n.LocalAppLanguage
import com.example.flexfit.ui.i18n.exerciseDescription
import com.example.flexfit.ui.i18n.exerciseName
import com.example.flexfit.ui.i18n.l10n
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
    isBodyCalibrated: Boolean = true,
    onOpenBodyCalibration: () -> Unit = {},
    onOpenExerciseSetup: (ExerciseType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 112.dp)
    ) {
        Text(
            text = l10n("Workout"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = l10n("Pick a movement, then choose camera or video analysis."),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (!isBodyCalibrated) {
            BodyCalibrationGuideCard(onClick = onOpenBodyCalibration)
            Spacer(modifier = Modifier.height(14.dp))
        }

        ExerciseType.entries.forEach { exercise ->
            ExerciseSelectionCard(
                exercise = exercise,
                onClick = { onOpenExerciseSetup(exercise) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BodyCalibrationGuideCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = l10n("Custom Body Data Required", "需要先定制化身材数据"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = l10n(
                        "Upload a front full-body photo so FlexFit can generate body ratio coefficients before training.",
                        "请先上传正面全身照，生成身体比例系数后再开始训练。"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ExerciseSelectionCard(
    exercise: ExerciseType,
    onClick: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val isAvailable = exercise.isImplemented
    val accentColor = if (isAvailable) DeepPurple else TextTertiary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAvailable, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) SurfaceLight else SurfaceLight.copy(alpha = 0.68f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    tint = if (isAvailable) AccentPurple else TextTertiary,
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
                        text = appLanguage.exerciseName(exercise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAvailable) TextPrimary else TextTertiary
                    )
                    StatusLabel(isAvailable = isAvailable)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = appLanguage.exerciseDescription(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAvailable) TextSecondary else TextTertiary
                )
            }

            if (isAvailable) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
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
            text = if (isAvailable) l10n("Available") else l10n("Coming soon"),
            style = MaterialTheme.typography.labelSmall,
            color = if (isAvailable) AccentPurple else TextTertiary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}
