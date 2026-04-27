package com.example.flexfit.ui.screens.training

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.flexfit.ml.ExercisePhaseTone
import com.example.flexfit.ml.FeedbackType
import com.example.flexfit.ml.PoseKeypoints
import com.example.flexfit.ui.theme.ErrorRed
import com.example.flexfit.ui.theme.SuccessGreen
import com.example.flexfit.ui.theme.WarningOrange

@Composable
fun PoseOverlay(
    keypoints: FloatArray?,
    landmarkConfidences: FloatArray? = null,
    phaseTone: ExercisePhaseTone = ExercisePhaseTone.NEUTRAL,
    feedbackType: FeedbackType? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val kp = keypoints ?: return@Canvas
        if (!PoseKeypoints.isValid(kp)) return@Canvas

        val strokeWidth = 4.dp.toPx()
        val pointRadius = 8.dp.toPx()
        val skeletonColor = skeletonColor(feedbackType, phaseTone)

        POSE_CONNECTIONS.forEach { (start, end) ->
            if (!PoseKeypoints.hasPoint(kp, start) || !PoseKeypoints.hasPoint(kp, end)) {
                return@forEach
            }

            val confidence = minOf(
                confidenceAt(landmarkConfidences, start),
                confidenceAt(landmarkConfidences, end)
            )
            if (confidence < MIN_VISIBLE_CONFIDENCE) return@forEach

            drawLine(
                color = skeletonColor.copy(alpha = confidenceAlpha(confidence, line = true)),
                start = pointOffset(kp, start, size.width, size.height),
                end = pointOffset(kp, end, size.width, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        for (index in 0 until PoseKeypoints.LANDMARK_COUNT) {
            if (!PoseKeypoints.hasPoint(kp, index)) continue

            val confidence = confidenceAt(landmarkConfidences, index)
            if (confidence < MIN_VISIBLE_CONFIDENCE) continue

            val center = pointOffset(kp, index, size.width, size.height)
            drawCircle(
                color = skeletonColor.copy(alpha = confidenceAlpha(confidence, line = false)),
                radius = pointRadius * (0.75f + confidence * 0.25f),
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = confidenceAlpha(confidence, line = false)),
                radius = pointRadius * 0.5f,
                center = center
            )
        }
    }
}

private fun pointOffset(
    keypoints: FloatArray,
    index: Int,
    width: Float,
    height: Float
): Offset {
    return Offset(
        x = width * (0.5f + keypoints[index * PoseKeypoints.VALUES_PER_LANDMARK]),
        y = height * (0.5f - keypoints[index * PoseKeypoints.VALUES_PER_LANDMARK + 1])
    )
}

private fun confidenceAt(confidences: FloatArray?, index: Int): Float {
    if (confidences == null || index !in confidences.indices) return 1f
    val confidence = confidences[index]
    return if (confidence <= 0f) 0.35f else confidence.coerceIn(0f, 1f)
}

private fun confidenceAlpha(confidence: Float, line: Boolean): Float {
    val minAlpha = if (line) 0.35f else 0.45f
    return (minAlpha + confidence.coerceIn(0f, 1f) * (1f - minAlpha)).coerceIn(minAlpha, 1f)
}

private fun skeletonColor(
    feedbackType: FeedbackType?,
    phaseTone: ExercisePhaseTone
): Color {
    return when (feedbackType) {
        FeedbackType.ERROR -> ErrorRed
        FeedbackType.WARNING -> WarningOrange
        FeedbackType.SUCCESS -> SuccessGreen
        FeedbackType.INFO, null -> when (phaseTone) {
            ExercisePhaseTone.SUCCESS -> SuccessGreen
            ExercisePhaseTone.WARNING -> WarningOrange
            ExercisePhaseTone.ACTIVE -> SuccessGreen
            ExercisePhaseTone.NEUTRAL -> Color.White
        }
    }
}

private val POSE_CONNECTIONS = listOf(
    11 to 13,
    13 to 15,
    12 to 14,
    14 to 16,
    11 to 12,
    11 to 23,
    12 to 24,
    23 to 24,
    23 to 25,
    25 to 27,
    24 to 26,
    26 to 28,
    0 to 11,
    0 to 12
)

private const val MIN_VISIBLE_CONFIDENCE = 0.12f
