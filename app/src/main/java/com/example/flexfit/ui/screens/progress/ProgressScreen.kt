package com.example.flexfit.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProgressScreen() {
    val records by WorkoutRecordRepository.workoutRecords.collectAsState()
    val totalWorkouts by WorkoutRecordRepository.totalWorkouts.collectAsState()
    val totalMinutes by WorkoutRecordRepository.totalMinutes.collectAsState()
    val averageAccuracy by WorkoutRecordRepository.averageAccuracy.collectAsState()
    val currentStreak by WorkoutRecordRepository.currentStreak.collectAsState()

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
            totalWorkouts = totalWorkouts,
            totalMinutes = totalMinutes,
            averageAccuracy = averageAccuracy,
            currentStreak = currentStreak
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Workout Records Section
        WorkoutRecordsSection(records = records)

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
        TipsSection()
    }
}

@Composable
private fun WorkoutRecordsSection(records: List<WorkoutRecord>) {
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
            Text(
                text = "${records.size} sessions",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                        text = "No workouts yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = "Complete a workout to see your history",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        } else {
            records.take(5).forEach { record ->
                WorkoutRecordItem(record = record)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
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
    totalWorkouts: Int,
    totalMinutes: Long,
    averageAccuracy: Float,
    currentStreak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.SportsScore,
            title = "Total Workouts",
            value = totalWorkouts.toString(),
            color = DeepPurple,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Schedule,
            title = "Total Minutes",
            value = totalMinutes.toString(),
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
            value = "${averageAccuracy.toInt()}%",
            color = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.LocalFireDepartment,
            title = "Streak Days",
            value = currentStreak.toString(),
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
    // Get accuracy data from recent records
    val accuracyData = records.take(7).map { it.averageAccuracy }.reversed()
    val displayData = if (accuracyData.isEmpty()) listOf(75f, 82f, 78f, 88f, 85f, 92f, 89f) else accuracyData
    val avgAccuracy = if (displayData.isNotEmpty()) displayData.average().toFloat() else 0f

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

            // Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val stepX = if (displayData.size > 1) width / (displayData.size - 1) else width
                    val maxY = 100f

                    // Draw grid lines
                    for (i in 0..4) {
                        val y = height - (height * i / 4)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Draw line chart
                    val path = Path()
                    displayData.forEachIndexed { index, value ->
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

                    // Draw points
                    displayData.forEachIndexed { index, value ->
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

            // Legend
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

@Composable
private fun ExerciseDistribution(records: List<WorkoutRecord>) {
    // Calculate distribution from records
    val exerciseCounts = records.groupBy { it.exerciseType }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    val displayData = if (exerciseCounts.isEmpty()) {
        listOf("Pull Up" to 10, "Shoulder Press" to 8, "Squat" to 4, "Others" to 2)
    } else {
        exerciseCounts
    }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val total = displayData.sumOf { it.second }.toFloat()

                        if (total > 0) {
                            var startAngle = -90f
                            displayData.forEachIndexed { index, (_, count) ->
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

                // Legend
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    displayData.forEachIndexed { index, (name, count) ->
                        val percentage = if (records.isNotEmpty()) {
                            (count.toFloat() / records.size * 100).toInt()
                        } else {
                            0
                        }
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
private fun TipsSection() {
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
                text = "Based on your recent training data, here are some personalized suggestions:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            TipItem(
                tip = "Try to shrug less during pull-ups. Focus on engaging your back muscles.",
                icon = "1"
            )
            TipItem(
                tip = "Your shoulder press form is improving! Keep your core engaged.",
                icon = "2"
            )
            TipItem(
                tip = "Consider adding more rest days between intense sessions.",
                icon = "3"
            )
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
