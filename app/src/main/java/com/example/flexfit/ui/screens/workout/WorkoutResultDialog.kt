package com.example.flexfit.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.flexfit.data.llm.LlmAnalysisState
import com.example.flexfit.data.model.AnalysisSource
import com.example.flexfit.data.model.PerformanceLevel
import com.example.flexfit.data.model.WorkoutAnalysisResult
import com.example.flexfit.data.model.WorkoutResult
import com.example.flexfit.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun WorkoutResultDialog(
    result: WorkoutResult,
    llmAnalysisState: LlmAnalysisState = LlmAnalysisState.Idle,
    onDismiss: () -> Unit,
    onSaveAndClose: () -> Unit,
    onRequestLlmAnalysis: () -> Unit = {},
    onRetryLlmAnalysis: () -> Unit = {}
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
                                    PerformanceLevel.NEEDS_IMPROVEMENT -> Icons.AutoMirrored.Filled.TrendingUp
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

                AiAnalysisCard(
                    analysisState = llmAnalysisState,
                    onRequestAnalysis = onRequestLlmAnalysis,
                    onRetry = onRetryLlmAnalysis
                )

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
                text = "Local Scores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Real-time rule-based scores calculated from pose metrics during training.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            ScoreRow(
                label = "Depth",
                value = result.depthScore,
                description = "Range of motion and full rep completion."
            )
            ScoreRow(
                label = "Alignment",
                value = result.alignmentScore,
                description = "Left-right symmetry and joint positioning."
            )
            ScoreRow(
                label = "Stability",
                value = result.stabilityScore,
                description = "Body control, shoulder level, and torso steadiness."
            )
        }
    }
}

@Composable
private fun ScoreRow(
    label: String,
    value: Float,
    description: String
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

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
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
                text = "Rule-Based Issues",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Detected from local pose rules before AI analysis.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            val hasIssues = result.mainIssues.any { it != "No major issues detected" }
            if (!hasIssues) {
                SummaryLine(
                    title = "No major issues detected",
                    detail = "Keep the same tempo and full range of motion.",
                    color = SuccessGreen
                )
            } else {
                result.mainIssues.forEachIndexed { index, issue ->
                    SummaryLine(
                        title = issue,
                        detail = result.improvementSuggestions.getOrNull(index)
                            ?: "Review this part of your form on the next set.",
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(
    title: String,
    detail: String,
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
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
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
                text = "Overall Local Score",
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

@Composable
private fun AiAnalysisCard(
    analysisState: LlmAnalysisState,
    onRequestAnalysis: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                when (analysisState) {
                    is LlmAnalysisState.Idle -> {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is LlmAnalysisState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AccentPurple
                        )
                    }
                    is LlmAnalysisState.Success -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is LlmAnalysisState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Post-workout personalized coaching generated after the local scores are ready.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = analysisState) {
                is LlmAnalysisState.Idle -> {
                    Text(
                        text = "Get personalized insights and recommendations powered by AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestAnalysis,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze with AI")
                    }
                }

                is LlmAnalysisState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = AccentPurple
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Analyzing your workout...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                is LlmAnalysisState.Success -> {
                    AnalysisResultContent(result = state.result)
                }

                is LlmAnalysisState.Error -> {
                    if (state.fallbackResult != null) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnalysisResultContent(result = state.fallbackResult)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry AI Analysis")
                        }
                    } else {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisResultContent(result: WorkoutAnalysisResult) {
    val sourceLabel = when (result.source) {
        AnalysisSource.LLM -> "AI-powered"
        AnalysisSource.LOCAL_FALLBACK -> "Local analysis"
    }
    val sourceColor = when (result.source) {
        AnalysisSource.LLM -> AccentPurple
        AnalysisSource.LOCAL_FALLBACK -> TextSecondary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = sourceLabel,
            style = MaterialTheme.typography.labelSmall,
            color = sourceColor
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = result.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Medium
    )

    if (result.strengths.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Strengths",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SuccessGreen
        )
        Spacer(modifier = Modifier.height(6.dp))
        result.strengths.forEach { strength ->
            AnalysisBulletPoint(text = strength, color = SuccessGreen)
        }
    }

    if (result.weaknesses.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Areas for Improvement",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = WarningOrange
        )
        Spacer(modifier = Modifier.height(6.dp))
        result.weaknesses.forEach { weakness ->
            AnalysisBulletPoint(text = weakness, color = WarningOrange)
        }
    }

    if (result.recommendations.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Recommendations",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AccentPurple
        )
        Spacer(modifier = Modifier.height(6.dp))
        result.recommendations.forEach { recommendation ->
            AnalysisBulletPoint(text = recommendation, color = AccentPurple)
        }
    }
}

@Composable
private fun AnalysisBulletPoint(
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
