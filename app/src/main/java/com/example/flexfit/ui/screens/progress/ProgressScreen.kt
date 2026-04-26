package com.example.flexfit.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexfit.data.model.WorkoutRecord
import com.example.flexfit.data.model.WorkoutStats
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ui.theme.*

@Composable
fun ProgressScreen() {
    val records by WorkoutRecordRepository.workoutRecords.collectAsState()
    var selectedExercise by remember { mutableStateOf(ALL_EXERCISES_FILTER) }
    val exerciseFilters = remember(records) {
        listOf(ALL_EXERCISES_FILTER) + records.map { it.exerciseType }.distinct().sorted()
    }
    LaunchedEffect(exerciseFilters) {
        if (selectedExercise !in exerciseFilters) {
            selectedExercise = ALL_EXERCISES_FILTER
        }
    }
    val filteredRecords = remember(records, selectedExercise) {
        if (selectedExercise == ALL_EXERCISES_FILTER) {
            records
        } else {
            records.filter { it.exerciseType == selectedExercise }
        }
    }
    val filteredStats = remember(filteredRecords) {
        WorkoutRecordRepository.calculateStats(filteredRecords)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Progress",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Cards Row
        StatsCardsRow(
            stats = filteredStats
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Workout Records Section
        WorkoutRecordsSection(
            allRecords = records,
            records = filteredRecords,
            filters = exerciseFilters,
            selectedExercise = selectedExercise,
            onFilterSelected = { selectedExercise = it },
            onClearAll = WorkoutRecordRepository::clearAllRecords
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Training Chart
        WeeklyTrainingChart(records = records)

        Spacer(modifier = Modifier.height(24.dp))

        // Accuracy Trend Chart
        AccuracyTrendChart(records = records)

        Spacer(modifier = Modifier.height(24.dp))

        // Exercise Distribution
        ExerciseDistribution(records = records)

        Spacer(modifier = Modifier.height(24.dp))

        // Tips Section
        TipsSection(records = filteredRecords)
    }
}

private const val ALL_EXERCISES_FILTER = "All"

@Composable
private fun WorkoutRecordsSection(
    allRecords: List<WorkoutRecord>,
    records: List<WorkoutRecord>,
    filters: List<String>,
    selectedExercise: String,
    onFilterSelected: (String) -> Unit,
    onClearAll: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Training History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${records.size} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (allRecords.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showClearDialog = true }) {
                        Text("Clear", color = ErrorRed)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filters.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedExercise == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (records.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (allRecords.isEmpty()) "No workouts yet" else "No matching workouts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = if (allRecords.isEmpty()) {
                            "Complete a workout to see your history"
                        } else {
                            "Choose another exercise filter"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        } else {
            records.forEach { record ->
                WorkoutRecordItem(record = record)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear training history?") },
            text = { Text("This removes all saved workout records from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    }
                ) {
                    Text("Clear", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WorkoutRecordItem(record: WorkoutRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(DeepPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = DeepPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.exerciseType,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = record.toWorkoutResult().formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${record.completedReps} reps",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = record.toWorkoutResult().formattedDuration,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Accuracy badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                record.averageAccuracy >= 85 -> SuccessGreen.copy(alpha = 0.1f)
                                record.averageAccuracy >= 70 -> WarningOrange.copy(alpha = 0.1f)
                                else -> ErrorRed.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${record.averageAccuracy.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            record.averageAccuracy >= 85 -> SuccessGreen
                            record.averageAccuracy >= 70 -> WarningOrange
                            else -> ErrorRed
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCardsRow(
    stats: WorkoutStats
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.SportsScore,
            title = "Total Workouts",
            value = stats.totalWorkouts.toString(),
            color = DeepPurple,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Schedule,
            title = "Total Minutes",
            value = stats.totalMinutes.toString(),
            color = AccentPurple,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.TrendingUp,
            title = "Avg Accuracy",
            value = "${stats.averageAccuracy.toInt()}%",
            color = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.LocalFireDepartment,
            title = "Streak Days",
            value = stats.currentStreak.toString(),
            color = WarningOrange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun WeeklyTrainingChart(records: List<WorkoutRecord>) {
    // Calculate workouts per day for the last 7 days
    val oneDayMs = 24 * 60 * 60 * 1000L
    val today = System.currentTimeMillis()
    val weekData = mutableListOf<Int>()
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    for (i in 6 downTo 0) {
        val dayStart = (today - i * oneDayMs) / oneDayMs
        val dayEnd = dayStart + 1
        val count = records.count { record ->
            val recordDay = record.date / oneDayMs
            recordDay in dayStart until dayEnd
        }
        weekData.add(count)
    }

    val maxValue = (weekData.maxOrNull() ?: 1).coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Weekly Training",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Number of workouts per day",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weekData.forEachIndexed { index, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height((value * 30 / maxValue + if (value > 0) 10 else 0).dp)
                                .align(Alignment.CenterHorizontally)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(DeepPurple, AccentPurple)
                                    ),
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = days[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccuracyTrendChart(records: List<WorkoutRecord>) {
    val accuracyData = records.take(7).map { it.averageAccuracy }.reversed()
    val avgAccuracy = if (accuracyData.isNotEmpty()) accuracyData.average().toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Accuracy Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Your accuracy over the past 7 sessions",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (accuracyData.isEmpty()) {
                EmptyCardMessage(text = "Complete workouts to see your accuracy trend.")
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val stepX = if (accuracyData.size > 1) width / (accuracyData.size - 1) else width
                        val maxY = 100f

                        for (i in 0..4) {
                            val y = height - (height * i / 4)
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        val path = Path()
                        accuracyData.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - (value / maxY * height)

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = DeepPurple,
                            style = Stroke(width = 3f)
                        )

                        accuracyData.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - (value / maxY * height)

                            drawCircle(
                                color = DeepPurple,
                                radius = 6f,
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Avg: ${avgAccuracy.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseDistribution(records: List<WorkoutRecord>) {
    val exerciseCounts = records.groupBy { it.exerciseType }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    val colors = listOf(DeepPurple, AccentPurple, LightPurple, SuccessGreen, WarningOrange)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Exercise Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Time spent on each exercise type",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (exerciseCounts.isEmpty()) {
                EmptyCardMessage(text = "Complete workouts to see exercise distribution.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val total = exerciseCounts.sumOf { it.second }.toFloat()

                            if (total > 0) {
                                var startAngle = -90f
                                exerciseCounts.forEachIndexed { index, (_, count) ->
                                    val sweepAngle = (count / total) * 360f
                                    drawArc(
                                        color = colors[index % colors.size],
                                        startAngle = startAngle,
                                        sweepAngle = sweepAngle,
                                        useCenter = true,
                                        size = Size(size.width, size.height)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                        }

                        Text(
                            text = "${records.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        exerciseCounts.forEachIndexed { index, (name, count) ->
                            val percentage = (count.toFloat() / records.size * 100).toInt()
                            LegendItem(
                                color = colors[index % colors.size],
                                label = name,
                                percentage = "$percentage%"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCardMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    percentage: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = percentage,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun TipsSection(records: List<WorkoutRecord>) {
    val tips = remember(records) {
        val suggestions = records
            .flatMap { it.improvementSuggestions }
            .filter { it.isNotBlank() }
        val issueTips = records
            .flatMap { it.mainIssues }
            .filter { it.isNotBlank() && it != "No major issues detected" }
            .map { "Focus on: $it." }

        (suggestions + issueTips).distinct().take(3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightPurple.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "AI Training Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeepPurple
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (records.isEmpty()) {
                    "Complete a workout to generate personalized suggestions."
                } else {
                    "Based on your recent training data, here are some personalized suggestions:"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (records.isEmpty()) {
                EmptyCardMessage(text = "Complete a workout to generate personalized suggestions.")
            } else if (tips.isEmpty()) {
                TipItem(
                    tip = "No major issues detected. Keep your current form.",
                    icon = "1"
                )
            } else {
                tips.forEachIndexed { index, tip ->
                    TipItem(
                        tip = tip,
                        icon = "${index + 1}"
                    )
                }
            }
        }
    }
}

@Composable
private fun TipItem(tip: String, icon: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(DeepPurple, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = tip,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
