package com.example.flexfit.ui.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flexfit.data.repository.UserProfileRepository
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ui.navigation.Screen
import com.example.flexfit.ui.theme.AccentPurple
import com.example.flexfit.ui.theme.DeepPurple
import com.example.flexfit.ui.theme.LightPurple
import com.example.flexfit.ui.theme.SuccessGreen
import com.example.flexfit.ui.theme.TextPrimary
import com.example.flexfit.ui.theme.TextSecondary
import com.example.flexfit.ui.theme.WarningOrange

private val HomeDeep = Color(0xFF3B2473)
private val HomeInk = Color(0xFF241346)

@Composable
fun HomeScreen(navController: NavController) {
    val profile by UserProfileRepository.profile.collectAsState()
    val totalWorkouts by WorkoutRecordRepository.totalWorkouts.collectAsState()
    val totalMinutes by WorkoutRecordRepository.totalMinutes.collectAsState()
    val averageAccuracy by WorkoutRecordRepository.averageAccuracy.collectAsState()
    val currentStreak by WorkoutRecordRepository.currentStreak.collectAsState()
    val nickname = profile.name.trim().ifBlank { "FlexFit User" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(HomeInk, DeepPurple, LightPurple.copy(alpha = 0.86f))
                )
            )
    ) {
        MotionLineBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(670.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "${getGreeting()} $nickname",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Today's goal: complete one focused Pull Up or Shoulder Press session with clean form and stable local scores.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.76f),
                        lineHeight = MaterialTheme.typography.titleMedium.lineHeight
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.34f),
                                        AccentPurple.copy(alpha = 0.30f)
                                    )
                                )
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(76.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            navController.navigate(Screen.Workout.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = HomeDeep
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Text(
                            text = "Start",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            HomeProgressSection(
                totalWorkouts = totalWorkouts,
                totalMinutes = totalMinutes,
                averageAccuracy = averageAccuracy,
                currentStreak = currentStreak
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun HomeProgressSection(
    totalWorkouts: Int,
    totalMinutes: Long,
    averageAccuracy: Float,
    currentStreak: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeStatCard(
                icon = Icons.Default.SportsScore,
                title = "Total Workouts",
                value = totalWorkouts.toString(),
                color = DeepPurple,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
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
            HomeStatCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "Avg Accuracy",
                value = "${averageAccuracy.toInt()}%",
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                icon = Icons.Default.LocalFireDepartment,
                title = "Streak Days",
                value = currentStreak.toString(),
                color = WarningOrange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeStatCard(
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
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
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MotionLineBackdrop() {
    val transition = rememberInfiniteTransition(label = "home_motion_lines")
    val flow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200),
            repeatMode = RepeatMode.Restart
        ),
        label = "line_offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val diagonalTravel = w * 0.42f
        val offset = diagonalTravel * flow

        repeat(8) { index ->
            val baseX = w * (0.08f + index * 0.14f) - offset
            val startX = (baseX + diagonalTravel) % (w + diagonalTravel) - diagonalTravel * 0.5f
            val startY = h * (0.14f + (index % 4) * 0.18f)
            val lineLength = w * (0.22f + (index % 3) * 0.04f)

            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(startX + lineLength, startY + h * 0.08f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
        }

        repeat(6) { index ->
            val baseX = w * (0.02f + index * 0.20f) + offset * 0.58f
            val startX = baseX % (w + diagonalTravel) - diagonalTravel * 0.2f
            val startY = h * (0.22f + index * 0.11f)

            drawLine(
                color = LightPurple.copy(alpha = 0.26f),
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(startX + w * 0.16f, startY - h * 0.05f),
                strokeWidth = 7.5f,
                cap = StrokeCap.Round
            )
        }

        val pulseX = w * (0.12f + flow * 0.72f)
        drawLine(
            color = AccentPurple.copy(alpha = 0.44f),
            start = androidx.compose.ui.geometry.Offset(pulseX - w * 0.28f, h * 0.62f),
            end = androidx.compose.ui.geometry.Offset(pulseX + w * 0.22f, h * 0.70f),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.28f),
            start = androidx.compose.ui.geometry.Offset(pulseX - w * 0.18f, h * 0.65f),
            end = androidx.compose.ui.geometry.Offset(pulseX + w * 0.14f, h * 0.70f),
            strokeWidth = 2.6f,
            cap = StrokeCap.Round
        )
    }
}

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }
}
