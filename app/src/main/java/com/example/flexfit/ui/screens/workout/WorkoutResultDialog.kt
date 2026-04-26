package com.example.flexfit.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.flexfit.data.model.PerformanceLevel
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun WorkoutResultDialog(
    result: WorkoutResult,
    onDismiss: () -> Unit,
    onSaveAndClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(DeepPurple, AccentPurple)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Performance icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (result.performanceLevel) {
                                    PerformanceLevel.EXCELLENT -> Icons.Default.EmojiEvents
                                    PerformanceLevel.GOOD -> Icons.Default.ThumbUp
                                    PerformanceLevel.AVERAGE -> Icons.Default.FitnessCenter
                                    PerformanceLevel.NEEDS_IMPROVEMENT -> Icons.Default.TrendingUp
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = result.performanceLevel.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Workout Complete!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Exercise type
                Text(
                    text = result.exerciseType,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = result.formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Main stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Reps completed
                    StatItem(
                        value = "${result.completedReps}/${result.totalReps}",
                        label = "Reps",
                        icon = Icons.Default.Repeat,
                        color = DeepPurple
                    )

                    // Duration
                    StatItem(
                        value = result.formattedDuration,
                        label = "Duration",
                        icon = Icons.Default.Timer,
                        color = AccentPurple
                    )

                    // Accuracy
                    StatItem(
                        value = "${result.averageAccuracy.roundToInt()}%",
                        label = "Score",
                        icon = Icons.Default.Speed,
                        color = SuccessGreen
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Accuracy progress bar
                AccuracyProgressBar(accuracy = result.averageAccuracy)

                Spacer(modifier = Modifier.height(24.dp))

                ScoreBreakdownCard(result = result)

                Spacer(modifier = Modifier.height(24.dp))

                // Detailed stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = LightBackground
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(
                            icon = Icons.Default.CheckCircle,
                            label = "Success Rate",
                            value = "${result.successRate.roundToInt()}%",
                            valueColor = if (result.successRate >= 75) SuccessGreen else WarningOrange
                        )

                        DetailRow(
                            icon = Icons.Default.LocalFireDepartment,
                            label = "Calories Burned",
                            value = "${result.caloriesBurned.roundToInt()} kcal",
                            valueColor = WarningOrange
                        )

                        DetailRow(
                            icon = Icons.Default.Error,
                            label = "Errors",
                            value = "${result.errorsCount}",
                            valueColor = if (result.errorsCount <= 3) SuccessGreen else ErrorRed
                        )

                        DetailRow(
                            icon = Icons.Default.Warning,
                            label = "Warnings",
                            value = "${result.warningsCount}",
                            valueColor = if (result.warningsCount <= 5) WarningOrange else ErrorRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                IssueSummaryCard(result = result)

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Discard")
                    }

                    Button(
                        onClick = onSaveAndClose,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBreakdownCard(result: WorkoutResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Score Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScoreRow(label = "Depth", value = result.depthScore)
            ScoreRow(label = "Alignment", value = result.alignmentScore)
            ScoreRow(label = "Stability", value = result.stabilityScore)
        }
    }
}

@Composable
private fun ScoreRow(
    label: String,
    value: Float
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "${value.roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = scoreColor(value)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = scoreColor(value),
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun IssueSummaryCard(result: WorkoutResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Main Issues",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            result.mainIssues.forEach { issue ->
                SummaryLine(text = issue, color = if (issue == "No major issues detected") SuccessGreen else ErrorRed)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            result.improvementSuggestions.forEach { suggestion ->
                SummaryLine(text = suggestion, color = AccentPurple)
            }
        }
    }
}

@Composable
private fun SummaryLine(
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun scoreColor(value: Float): Color {
    return when {
        value >= 85f -> SuccessGreen
        value >= 65f -> AccentPurple
        value >= 45f -> WarningOrange
        else -> ErrorRed
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun AccuracyProgressBar(accuracy: Float) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overall Accuracy",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "${accuracy.roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    accuracy >= 90 -> SuccessGreen
                    accuracy >= 70 -> AccentPurple
                    accuracy >= 50 -> WarningOrange
                    else -> ErrorRed
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { accuracy / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = when {
                accuracy >= 90 -> SuccessGreen
                accuracy >= 70 -> AccentPurple
                accuracy >= 50 -> WarningOrange
                else -> ErrorRed
            },
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
