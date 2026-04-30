package com.example.flexfit.ui.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.flexfit.data.repository.UserProfileRepository
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ui.i18n.LocalAppLanguage
import com.example.flexfit.ui.i18n.l10n
import com.example.flexfit.ui.navigation.Screen
import com.example.flexfit.ui.theme.AccentPurple
import com.example.flexfit.ui.theme.DeepPurple
import com.example.flexfit.ui.theme.LightPurple
import com.example.flexfit.ui.theme.SuccessGreen
import com.example.flexfit.ui.theme.TextPrimary
import com.example.flexfit.ui.theme.TextSecondary
import com.example.flexfit.ui.theme.WarningOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    val appLanguage = LocalAppLanguage.current

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
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 112.dp)
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
                        text = "${getGreeting(appLanguage)} $nickname",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = l10n("Today's goal: complete one focused Pull Up or Shoulder Press session with clean form and stable local scores."),
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
                            text = l10n("Start"),
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
                title = l10n("Total Workouts"),
                value = totalWorkouts.toString(),
                color = DeepPurple,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                icon = Icons.Default.Schedule,
                title = l10n("Total Minutes"),
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
                title = l10n("Avg Accuracy"),
                value = "${averageAccuracy.toInt()}%",
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                icon = Icons.Default.LocalFireDepartment,
                title = l10n("Streak Days"),
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
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "line_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val left = -w * 0.14f
        val right = w * 1.14f
        val steps = 72

        val layers = listOf(
            LineLayer(y = 0.12f, amplitude = 0.022f, frequency = 1.45f, width = 1.4f, alpha = 0.22f, speed = 1.0f),
            LineLayer(y = 0.21f, amplitude = 0.030f, frequency = 1.10f, width = 3.2f, alpha = 0.18f, speed = -0.72f),
            LineLayer(y = 0.34f, amplitude = 0.024f, frequency = 1.70f, width = 6.4f, alpha = 0.14f, speed = 0.58f),
            LineLayer(y = 0.49f, amplitude = 0.036f, frequency = 1.24f, width = 2.1f, alpha = 0.26f, speed = 0.86f),
            LineLayer(y = 0.64f, amplitude = 0.028f, frequency = 1.58f, width = 8.2f, alpha = 0.12f, speed = -0.48f),
            LineLayer(y = 0.78f, amplitude = 0.020f, frequency = 1.34f, width = 2.8f, alpha = 0.20f, speed = 0.68f)
        )

        layers.forEachIndexed { index, layer ->
            val path = Path()
            for (step in 0..steps) {
                val progress = step / steps.toFloat()
                val x = left + (right - left) * progress
                val wave = sin(progress * layer.frequency * PI.toFloat() * 2f + phase * layer.speed + index * 0.82f)
                val crossWave = cos(progress * PI.toFloat() * 3f + phase * layer.speed * 0.42f)
                val y = h * layer.y + h * layer.amplitude * wave + h * layer.amplitude * 0.34f * crossWave
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.02f),
                        LightPurple.copy(alpha = layer.alpha),
                        Color.White.copy(alpha = layer.alpha * 0.78f),
                        AccentPurple.copy(alpha = layer.alpha * 0.92f),
                        Color.White.copy(alpha = 0.02f)
                    ),
                    startX = left,
                    endX = right
                ),
                style = Stroke(
                    width = layer.width,
                    cap = StrokeCap.Round
                )
            )
        }

        repeat(9) { index ->
            val orbit = phase * (if (index % 2 == 0) 1f else -1f) + index * 0.71f
            val centerX = w * (0.50f + 0.42f * sin(orbit * 0.42f + index))
            val centerY = h * (0.16f + 0.70f * ((index % 7) / 6f)) +
                h * 0.020f * cos(orbit)
            val length = w * (0.12f + (index % 4) * 0.035f)
            val tilt = h * (0.020f + (index % 3) * 0.010f)
            val thick = when (index % 4) {
                0 -> 1.7f
                1 -> 3.8f
                2 -> 6.0f
                else -> 9.0f
            }
            val alpha = 0.16f + (index % 3) * 0.055f

            drawLine(
                color = if (index % 2 == 0) Color.White.copy(alpha = alpha) else LightPurple.copy(alpha = alpha),
                start = Offset(centerX - length, centerY - tilt),
                end = Offset(centerX + length, centerY + tilt),
                strokeWidth = thick,
                cap = StrokeCap.Round
            )
        }

        drawLine(
            color = AccentPurple.copy(alpha = 0.30f + 0.08f * sin(phase)),
            start = Offset(w * (0.08f + 0.035f * sin(phase * 0.7f)), h * 0.58f),
            end = Offset(w * (0.92f + 0.035f * cos(phase * 0.7f)), h * 0.68f),
            strokeWidth = 11f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.24f + 0.08f * cos(phase)),
            start = Offset(w * (0.16f + 0.024f * cos(phase * 0.9f)), h * 0.61f),
            end = Offset(w * (0.84f + 0.024f * sin(phase * 0.9f)), h * 0.67f),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
    }
}

private data class LineLayer(
    val y: Float,
    val amplitude: Float,
    val frequency: Float,
    val width: Float,
    val alpha: Float,
    val speed: Float
)

private fun getGreeting(language: com.example.flexfit.ui.i18n.AppLanguage): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> language.text("Good morning,", "早上好，")
        hour < 17 -> language.text("Good afternoon,", "下午好，")
        else -> language.text("Good evening,", "晚上好，")
    }
}
